package clustering;

import utils.*;
import java.io.*;
import java.util.*;

public class ClusteringTester3 {
  public ClusteringTester3() {
  }


  /**
   * args[0]: docs_file
   * args[1]: options file
   * args[2]: trueclusters file [optional]
   * args[3]: alarms_file [optional]
   * args[4] improvingClustererClassName [optional]
   * @param args String[]
   */
  public static void main(String args[]) {
    if (args.length<2) {
      System.err.println("usage: java ClusteringTester3 <docs_file> <options_file> [<trueclustersfile>] [<alarms_file>] [improveClustererName]");
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
      final int k = ((Integer) props.get("num_clusters")).intValue();
      try {
        if (args.length > 2) {
          trueclusters = DataMgr.readLabelsFromFile(args[2], docs_size);
          props.put("trueclusters", trueclusters);
        }
      }
      catch (Exception e) {
        // no-op
      }
      Clusterer improver = null;
      if (args.length>4) {
        improver = (Clusterer) Class.forName(args[4]).newInstance();
      }
      Clusterer cl = (Clusterer) props.get("clusterer");
      Evaluator eval = (Evaluator) props.get("evaluator");
      if (eval instanceof EnhancedEvaluator) {
        ((EnhancedEvaluator) eval).setMasterDocSet(docs);
        ((EnhancedEvaluator) eval).setParams(props);
      }
      cl.addAllDocuments(docs);
      cl.setParams(props);
      // set initial clustering -required for KMeansClusterers family
      Vector init_centers = new Vector();  // Vector<Document>
      for (int i=0; i<k; i++) {
        int r = (int) Math.floor(utils.RndUtil.getInstance().getRandom().nextDouble()*docs_size);
        // r in [0...docs_size-1]
        init_centers.addElement(docs.elementAt(r));
      }
      cl.setInitialClustering(init_centers);
      cl.clusterDocs();
      double val = eval.eval(cl);
      System.out.println("best value = "+val);
      if (improver!=null) {
        DocumentDistIntf m = (DocumentDistIntf) props.get("metric");
        Document.setMetric(m);
        improver.addAllDocuments(docs);
        improver.setParams(props);
        Vector centers = cl.getCurrentCenters();
        improver.setInitialClustering(centers);
        improver.setClusteringIndices(cl.getClusteringIndices());
        improver.clusterDocs();

        System.err.print("sparse clusters:");
        Double fd_factorI = (Double) props.get("fd_factor");
        double fd_factor = 5;
        if (fd_factorI!=null) fd_factor = fd_factorI.doubleValue();
        PrintWriter pw2 = null;
        if (args.length>3)
          pw2 = new PrintWriter(new FileOutputStream(args[3]));
        Set sparse_clusters = new HashSet();
        for (int i=0; i<k; i++) {
          int cardi = improver.getClusterCards()[i];
          if (cardi <= docs.size() / (fd_factor * k)) {
            System.err.println("card[" + i + "]=" + cardi);
            if (pw2!=null) {
              // print out the docs belonging to the sparse cluster
              sparse_clusters.add(new Integer(i));
            }
          }
        }
        if (pw2!=null) {
          for (int i=0; i<docs.size(); i++) {
            Integer ci = new Integer(improver.getClusteringIndices()[i]);
            if (sparse_clusters.contains(ci)) pw2.println("1");
            else pw2.println("0");
          }
          pw2.close();
          pw2 = null;
        }
      }
      else improver = cl;

      Evaluator feval = (Evaluator) props.get("finalevaluator");
      // get the final evaluator, which is an AdjRandIndexEvaluator in case of
      // UCI data tests, and of course it also requires having passed in
      // correctly the truelabelsfile argument in the program's cmd line
      if (feval==null) feval = eval;
      val = feval.eval(improver);
      System.out.println("final best value = " + val);
      DataMgr.writeLabelsToFile(improver, args[0]+"_asgns.txt");
      System.out.println("total time="+(System.currentTimeMillis()-st));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
