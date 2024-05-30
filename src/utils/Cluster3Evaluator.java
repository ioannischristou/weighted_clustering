package utils;

import java.util.*;
import java.io.*;
import clustering.*;

public class Cluster3Evaluator {
  public Cluster3Evaluator() {
  }

  /**
   * args[0]: input file of CoDoCUp documents.
   * args[1]: input file of Cluster3 results.
   * Format is like this:
   * uid  GROUP
   * di   clusterno
   * ...
   * i is in [0,NumDocs-1], clusterno is in [0, NumClusters-1]
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java Cluster3Evaluator CoDoCUpInputFile Cluster3OutputFile Median_or_[Sqr]Means [LabelsInputFile]");
      System.exit(-1);
    }
    String infile = args[0];
    String outfile = args[1];
    boolean median = "Median".equals(args[2]);
    boolean sqrmeans = "SqrMeans".equals(args[2]);
    int k=1;
    try {
      // read the docs
      Vector docs = DataMgr.readDocumentsFromFile(infile);  // Vector<Document>
      int[] asgns = new int[docs.size()];
      int[] truelabels = null;
      if (args.length>3) {
        truelabels = DataMgr.readLabelsFromFile(args[3], docs.size());
      }
      // read the asngmnt from Cluster3 outfile
      BufferedReader br = new BufferedReader(new FileReader(outfile));
      for (int i=0; true; i++) {
        String line = br.readLine();
        if (i==0) continue;  // ignore first line
        if (line==null) break;
        StringTokenizer st = new StringTokenizer(line, "\t");
        String di = st.nextToken();
        String cnum = st.nextToken();
        int docind = Integer.parseInt(di.substring(1));  // di is: "d153", "d5" ...
        int clusterind = Integer.parseInt(cnum);
        if (clusterind+1>k) k= clusterind+1;  // compute k as well
        asgns[docind] = clusterind;
      }
      Vector centers = Document.getCenters(docs, asgns, k, null);  // itc-20220223: HERE
      // set the clustering params
      Clusterer cl;
      ClustererTermination ct;
      Evaluator ev;
      Hashtable params = new Hashtable();
      params.put("h", new Double(1.0));
      params.put("k",new Integer(k));
      if (truelabels!=null) params.put("trueclusters",truelabels);
      if (median) {
        ct = new ClustererTerminationNoImpr();
        params.put("TerminationCriteria",ct);
        cl = new KMedianClusterer();
        if (truelabels==null) ev = new KMedianEvaluator();
        else ev = new AdjRandIndexEvaluator();
      }
      else {
        if (sqrmeans) {
          ct = new ClustererTerminationNoImprL2Sqr();
          cl = new KMeansSqrClusterer();
        }
        else {
          ct = new ClustererTerminationNoImprL2();
          cl = new KMeansClusterer();
        }
        params.put("TerminationCriteria",ct);
        if (truelabels==null && sqrmeans==false)
          ev = new KMeansEvaluator();
        else if (sqrmeans==true) ev = new clustering.KMeansSqrEvaluator();
        else ev = new AdjRandIndexEvaluator();
      }
      cl.setParams(params);
      cl.addAllDocuments(docs);
      cl.setInitialClustering(centers);
      cl.setClusteringIndices(asgns);
      // do a first evaluation
      double clustervalue = cl.eval(ev);
      System.out.println("Cluster3 value = "+clustervalue);
      System.out.println("Running CoDoCUp standard Clustering on this clustering...");
      Vector centers_new = cl.clusterDocs();
      double newval = cl.eval(ev);
      System.out.println("CoDoCUp standard Clustering results value = "+newval);
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
