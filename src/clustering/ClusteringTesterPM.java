package clustering;

import utils.*;
import java.util.*;

public class ClusteringTesterPM {
  public ClusteringTesterPM() {
  }


  /**
   * args[0]: docs_file
   * args[1]: options file
   * args[2]: trueclusters file [optional]
   * args[3]: improvingClustererClassName [optional]
   * args[4]: overriding value for k [optional]
   * args[5]: output index file [optional]
   * @param args String[]
   */
  public static void main(String args[]) {
    if (args.length<2) {
      System.err.println("usage: java ClusteringTester2 <docs_file> <options_file> [trueclustersfile] [improveClustererName] [k_override] [outfile]");
      System.exit(-1);
    }
    long st = System.currentTimeMillis();
    try {
      Hashtable props = DataMgr.readPropsFromFile(args[1]);
      Vector docs=null;
      if (props.get("normalizedocs")!=null)
        docs = DataMgr.readDocumentsFromFileAndNormalize(args[0]);
      else docs = DataMgr.readDocumentsFromFile(args[0]);
      final int docs_size = docs.size();
      int trueclusters[] = null;
      try {
        if (args.length > 2) {
          trueclusters = DataMgr.readNumericLabelsFromFile(args[2]);
          props.put("trueclusters", trueclusters);
        }
      }
      catch (Exception e) {
        // no-op
      }
      Clusterer improver = null;
      if (args.length>3) {
        try {
          improver = (Clusterer) Class.forName(args[3]).newInstance();
        }
        catch (Exception e) {
          // no-op
          System.err.println("improver will not be used in this run");
          improver=null;
        }
      }
      if (args.length>4) {
        props.put("num_clusters", new Integer(args[4]));
        props.put("k", new Integer(args[4]));
      }
      final int k = ((Integer) props.get("num_clusters")).intValue();
      String out_file=null;
      if (args.length>5) {
        out_file=args[5];
      }
      else out_file="center_inds.txt";
      Clusterer cl = (Clusterer) props.get("clusterer");
      Evaluator eval = (Evaluator) props.get("evaluator");
      if (eval instanceof EnhancedEvaluator) {
        ((EnhancedEvaluator) eval).setMasterDocSet(docs);
        ((EnhancedEvaluator) eval).setParams(props);
      }
      Integer percent_to_useI = (Integer) props.get("percent_docs_to_use");
      Vector udocs = null;
      int[] dmap = null;
      if (percent_to_useI==null || percent_to_useI.intValue()==100) {
        udocs = new Vector(docs);
        dmap = new int[udocs.size()];
        for (int j=0; j<dmap.length; j++) dmap[j]=j;
      } else {
        // add randomly the percentage provided
        int usize = percent_to_useI.intValue()*docs.size()/100;  // integer arithmetic
        dmap = new int[usize];
        boolean used[] = new boolean[docs.size()];
        udocs = new Vector();
        for (int i=0; i<docs.size(); i++) used[i]=false;
        for (int i=0; i<usize; i++) {
          while (true) {
            int r = (int) Math.floor(utils.RndUtil.getInstance().getRandom().nextDouble()*docs_size);
            // r in [0...udocs_size-1]
            if (used[r]==false) {
              dmap[i]=r;
              udocs.addElement(docs.elementAt(r));
              used[r]=true;
              break;
            }
          }
        }
      }
      System.err.println("ClusteringTesterPM: done creating udocs set.");  // itc: HERE rm asap
      cl.addAllDocuments(udocs);
      cl.setParams(props);
      DocumentDistIntf m = (DocumentDistIntf) props.get("metric");
      if (m!=null) Document.setMetric(m);
      // set initial clustering -required for KMeansClusterers family
      int udocs_size = udocs.size();
      Vector init_centers = new Vector();  // Vector<Document>
      for (int i=0; i<k; i++) {
        int r = (int) Math.floor(utils.RndUtil.getInstance().getRandom().nextDouble()*udocs_size);
        // r in [0...udocs_size-1]
        init_centers.addElement(udocs.elementAt(r));
      }
      cl.setInitialClustering(init_centers);
      cl.clusterDocs();
      double val = eval.eval(cl);
      System.out.println("best value = "+val);
      if (improver!=null) {
        improver.addAllDocuments(udocs);
        improver.setParams(props);
        Vector centers = cl.getCurrentCenters();
        improver.setInitialClustering(centers);
        improver.setClusteringIndices(cl.getClusteringIndices());
        improver.clusterDocs();
      }
      else improver = cl;

      Evaluator feval = (Evaluator) props.get("finalevaluator");
      // get the final evaluator, which is an AdjRandIndexEvaluator in case of
      // UCI data tests, and of course it also requires having passed in
      // correctly the truelabelsfile argument in the program's cmd line
      if (feval==null) feval = eval;
      else {
        val = feval.eval(improver);
        System.out.println("final best value = " + val);
      }
      // DataMgr.writeLabelsToFile(improver, args[0]+"_asgns.txt");
      if (out_file!=null) {  // write the index of each center in out_file
        int[] center_inds = getIndices(udocs, improver, dmap);
        DataMgr.writeLabelsToFile(center_inds, out_file);
        // write a new labels file that assigns to each p-median the label of
        // the majority of its assigned points
        int num_classes = ((Integer) props.get("num_classes")).intValue();
        int[] new_labels = getNewLabels(docs, center_inds, trueclusters, num_classes, m);
        DataMgr.writeLabelsToFile(new_labels, args[2]+"_asgns.txt");
        System.err.println("wrote new labels file: "+args[2]+"_asgns.txt");
      }
      System.out.println("total time="+(System.currentTimeMillis()-st));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  private static int[] getNewLabels(Vector docs, int[] center_inds, int[] truelabels,
                                    int num_classes,
                                    DocumentDistIntf metric) throws ClustererException {
    final int n = docs.size();
    final int p = center_inds.length;
    int[] nlabels = new int[truelabels.length];
    for (int i=0; i<n; i++) nlabels[i]=truelabels[i];  // init
    Vector centers = new Vector();
    for (int i=0; i<center_inds.length; i++) {
      centers.add(docs.elementAt(center_inds[i]));
    }
    // figure out how many votes each center has for each label
    int[][] votes = new int[num_classes][p];
    for (int i=0; i<p; i++) {
      for (int l = 0; l < num_classes; l++) votes[l][i] = 0; // init
      votes[truelabels[i]-1][i] = 1;  // increase the votes of each center for its true label
    }
    for (int j=0; j<n; j++) {
      Document dj = (Document) docs.elementAt(j);
      Vector docj = new Vector(); docj.addElement(dj);
      Document bestmedian = Document.getDocumentCenter(docj, centers, metric, null);  // itc: HERE 20220223
      for (int k=0; k<p; k++) {
        if (bestmedian.equals(centers.elementAt(k))) {
          // dj goes to k-th median
          votes[truelabels[j]-1][k]++;
          break;
        }
      }
    }
    // figure out for each center, how to relabel it
    for (int i=0; i<p; i++) {
      int maxv = -1;
      for (int j=0; j<num_classes; j++) {
        if (votes[j][i]>maxv) {
          maxv = votes[j][i];
          truelabels[center_inds[i]] = j+1;
        }
      }
    }
    return nlabels;
  }


  private static int[] getIndices(Vector docs, Clusterer cl, int[] dmap) {
    final int p = cl.getCurrentCenters().size();
    final int n = docs.size();
    int[] inds = new int[p];
    for (int i=0; i<p; i++) {
      Document ci = (Document) cl.getCurrentCenters().elementAt(i);
      for (int j=0; j<n; j++) {
        Document dj = (Document) docs.elementAt(j);
        if (ci.equals(dj)) {
          inds[i]=dmap[j];
          break;
        }
      }
    }
    return inds;
  }
}
