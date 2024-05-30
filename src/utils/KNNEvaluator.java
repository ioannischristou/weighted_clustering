package utils;

import java.util.*;
import java.io.*;
import utils.*;
import clustering.Document;
import clustering.DocumentDistIntf;
import clustering.ClustererException;

public class KNNEvaluator {

  private static DocDist2[] _sortedDocs=null;

  public KNNEvaluator() {
  }

  public static void main(String[] args) {
    if (args.length < 4) {
      System.err.println("usage: java -cp . utils.KNNEvaluator <docs_file> <pattern_indices_file> <labels_file> <properties_file> [test_docs_file] [test_labels_file]");
      System.exit(-1);
    }
    try {
      String docs_file = args[0];
      Vector docs = DataMgr.readDocumentsFromFile(docs_file);
      System.err.println("read docs file");
      String patterns_file = args[1];
      // read patterns and store them in patterns vector
      Set patterns = new HashSet();  // Set<Integer pos>, docs[pos] is the required pattern
      BufferedReader br = new BufferedReader(new FileReader(patterns_file));
      if (br.ready()) {
        while (true) {
          String line = br.readLine();
          if (line==null) break;
          int pos = Integer.parseInt(line);
          patterns.add(new Integer(pos));
        }
        br.close();
        System.err.println("read patterns file");
      }
      String labels_file = args[2];
      final int docs_size = docs.size();
      int labels[] = DataMgr.readNumericLabelsFromFile(labels_file);
      System.err.println("read labels file");
      int num_classes=0;
      Set classes=new HashSet();
      for (int i=0;i<labels.length;i++) {
        int li=labels[i];
        if (classes.contains(new Integer(li))==false) {
          num_classes++;
          classes.add(new Integer(li));
        }
      }
      num_classes=classes.size();
      Hashtable props = DataMgr.readPropsFromFile(args[3]);
      System.err.println("read props file");
      int k = ((Integer) props.get("k")).intValue();
      DocumentDistIntf metric = (DocumentDistIntf) props.get("metric");
      Document.setMetric(metric);
      // figure out the test docs
      Vector tstdocs = null;
      int[] tstlabels = null;
      if (args.length>4) {
        tstdocs = DataMgr.readDocumentsFromFile(args[4]);
        System.err.println("read test docs file");
        tstlabels = DataMgr.readNumericLabelsFromFile(args[5]);
        System.err.println("read "+tstlabels.length+" labels from test labels file");
      }
      else {  // tst{docs,labels} is the same as docs and labels
        tstdocs = new Vector(docs);
        tstlabels = new int[labels.length];
        for (int i=0; i<tstlabels.length; i++) tstlabels[i]=labels[i];
      }
      final int tstdocs_size = tstdocs.size();
      int[] prod_labels = new int[tstdocs_size];
      if (k>1) sortDocs(tstdocs, docs, patterns);  // figure out the d(di, pj) and sort this array
      // classify the docs according to the k-nn method, for the k nearest
      // patterns that are provided in the patterns vector
      int num_correct=0;
      int num_false=0;
      for (int i=0; i<tstdocs_size; i++) {
        int class_i = -1;
        if (i%1000==0) System.err.println("classified "+i+" docs");  // itc: HERE rm asap
        if (k>1) class_i = getClassOf(i, patterns.size(), labels, k, num_classes);
        else class_i = getNN((Document) tstdocs.elementAt(i), patterns, docs, labels);
        if (class_i == tstlabels[i]) num_correct++;
        else num_false++;
        prod_labels[i] = class_i;
      }
      double acc=(double)num_correct/((double) tstdocs_size);
      System.out.println("accuracy="+acc);
      String tstlabels_file = args.length>5 ? args[5] : args[2];
      String prodlabels_file = tstlabels_file+"_asgns.txt";
      DataMgr.writeLabelsToFile(prod_labels, prodlabels_file);
      System.err.println("wrote "+prodlabels_file);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  private static int getNN(Document d, Set patterns, Vector docs, int[] labels) throws ClustererException {
    final int psize = patterns.size();
    int best_class = -1;
    double best_val = Double.MAX_VALUE;
    Iterator it = patterns.iterator();
    while (it.hasNext()) {
      int pi = ((Integer) it.next()).intValue();
      Document dpi = (Document) docs.elementAt(pi);
      double dv = Document.d(d, dpi);
      if (dv < best_val) {
        best_val = dv;
        best_class = labels[pi];
      }
    }
    return best_class;
  }


  private static int getClassOf(int pos, int p, int[] labels, int k, int num_classes) {
    int[] votes = new int[num_classes];
    double[] dists = new double[num_classes];
    int voted_class=-1;
    for (int i=0; i<num_classes; i++) {
      votes[i] = 0;
      dists[i] = 0.0; // init to zero
    }
    for (int i=0; i<k; i++) {
      int nj = _sortedDocs[p*pos+i]._j;
      votes[labels[nj]-1]++;
      dists[labels[nj]-1] += _sortedDocs[p*pos+i]._dist;
    }
    int max_votes=0;
    for (int i=0; i<num_classes; i++) {
      if (votes[i] > max_votes) {
        max_votes = votes[i];
        voted_class=i;
      } else if (votes[i]==max_votes && max_votes>0) {
        // what happens in tie? check average distance of doc from nbors
        double disti = dists[i]/votes[i];
        double prevdist = dists[voted_class]/max_votes;
        if (disti < prevdist) {
          max_votes = votes[i];
          voted_class=i;
        }
      }
    }
    return voted_class+1;
  }


  private static void sortDocs(Vector testdocs, Vector docs, Set patterns) throws ClustererException {
    final int n = testdocs.size();
    final int p = patterns.size();
    _sortedDocs = new DocDist2[n*p];
    System.err.println("_sortedDocs.length="+_sortedDocs.length);  // itc: HERE rm asap
    int jj=0;
    for (int i=0; i<n; i++) {
      Iterator piter = patterns.iterator();
      for (int j=0; j<p; j++) {
        Integer pI = (Integer) piter.next();
        int jp = pI.intValue();
        // if (i==jp) continue;  // don't include self
        if (i % 1000 == 0 && j % 10 == 0) System.err.println("i="+i+" j="+j);  // itc: HERE rm asap
        double dij = Document.d((Document) testdocs.elementAt(i), (Document) docs.elementAt(jp));
        _sortedDocs[jj++] = new DocDist2(i, jp, dij);
      }
    }
    // breakpoint
    Arrays.sort(_sortedDocs);
  }

}


class DocDist2 implements Comparable {
  int _i, _j;
  double _dist;

  DocDist2(int i, int j, double dist) {
    _i = i; _j = j; _dist = dist;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      DocDist2 d2 = (DocDist2) o;
      if (_i==d2._i && _dist==d2._dist) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return _i;
  }

  public int compareTo(Object o) {
    DocDist2 c = (DocDist2) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_i < c._i) return -1;
    else if (_i > c._i) return 1;
    else {  // _i == c._i
      if (_dist < c._dist)return -1;
      else if (_dist == c._dist)return 0;
      else return 1;
    }
  }
}
