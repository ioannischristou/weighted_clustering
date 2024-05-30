package utils;

import java.util.*;
import java.io.*;
import clustering.*;

public class LabeledDSEvaluator {
  public LabeledDSEvaluator() {
  }

  /**
   * args[0]: input file of CoDoCUp documents.
   * args[1]: input file of data set labels.
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java LabeledDSEvaluator CoDoCUpInputFile LabelsInputFile Median_or_[Sqr]Means");
      System.exit(-1);
    }
    String infile = args[0];
    String labelsfile = args[1];
    boolean median = "Median".equals(args[2]);
    boolean sqrmeans = "SqrMeans".equals(args[2]);
    int k;
    try {
      // read the docs
      Vector docs = DataMgr.readDocumentsFromFile(infile);  // Vector<Document>
      int[] asgns = new int[docs.size()];
      int[] truelabels = null;
      truelabels = DataMgr.readLabelsFromFile(labelsfile, docs.size());
      Hashtable lup = new Hashtable();
      int cur=0;  // running label index in [0...numlabels-1]
      for (int i=0; i<docs.size(); i++) {
        Integer li = new Integer(truelabels[i]);
        Integer ci = (Integer) lup.get(li);
        if (ci==null) {
          ci = new Integer(cur);
          lup.put(li, ci);
          cur++;
        }
        asgns[i] = ci.intValue();
      }
      k = cur;
      Vector centers = Document.getCenters(docs, asgns, k, null);  // itc-20220223: HERE
      // set the clustering params
      Clusterer cl;
      ClustererTermination ct;
      Evaluator ev;
      Hashtable params = new Hashtable();
      params.put("h", new Double(1.0));
      params.put("k",new Integer(k));
      // if (truelabels!=null) params.put("trueclusters",truelabels);
      // truelabels cannot be null
      params.put("trueclusters",truelabels);
      if (median) {
        ct = new ClustererTerminationNoImpr();
        params.put("TerminationCriteria",ct);
        cl = new KMedianClusterer();
        /*
        if (truelabels==null) ev = new KMedianEvaluator();
        else ev = new AdjRandIndexEvaluator();
        */
        // truelabels cannot be null here
        ev = new AdjRandIndexEvaluator();
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
        // note: truelabels isn't null at this point
        /*
        if (truelabels==null && sqrmeans==false)
          ev = new KMeansEvaluator();
        else if (sqrmeans==true) ev = new clustering.KMeansSqrEvaluator();
        else ev = new AdjRandIndexEvaluator();
        */
        if (sqrmeans==true) ev = new clustering.KMeansSqrEvaluator();
        else ev = new AdjRandIndexEvaluator();
      }
      cl.setParams(params);
      cl.addAllDocuments(docs);
      cl.setInitialClustering(centers);
      cl.setClusteringIndices(asgns);
      // do a first evaluation
      double clustervalue = cl.eval(ev);
      System.out.println("value = "+clustervalue);
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
