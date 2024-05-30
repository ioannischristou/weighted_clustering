package clustering;

import utils.*;
import java.util.*;

public class ClusteringTester2 {
  public ClusteringTester2() {
  }


  /**
   * args[0]: docs_file
   * args[1]: options file
   * args[2]: trueclusters file [optional]
   * args[3]: graph_file [optional]
   * args[4] improvingClustererClassName [optional]
   * args[5] overriding value for k [optional]
   * @param args String[]
   */
  public static void main(String args[]) {
    if (args.length<2) {
      System.err.println("usage: java ClusteringTester2 <docs_file> <options_file> [<trueclustersfile>] [<graph_file>] [improveClustererName] [k_override]");
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
          trueclusters = DataMgr.readLabelsFromFile(args[2], docs_size);
          props.put("trueclusters", trueclusters);
        }
        // read Graph not done yet...
      }
      catch (Exception e) {
        // no-op
      }
      Clusterer improver = null;
      if (args.length>4) {
        improver = (Clusterer) Class.forName(args[4]).newInstance();
      }
      if (args.length>5) {
        props.put("num_clusters", new Integer(args[5]));
        props.put("k", new Integer(args[5]));
      }
      final int k = ((Integer) props.get("num_clusters")).intValue();
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
      Set c_indices = new HashSet();  // structure helps ensure all centers are different
      for (int i=0; i<k; i++) {
        int r=-1;
        while (true) {
          r = (int) Math.floor(utils.RndUtil.getInstance().getRandom().nextDouble()*docs_size);
          Integer rI = new Integer(r);
          if (!c_indices.contains(rI)) {
            c_indices.add(rI);
            break;
          }
        }
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
/*
        // itc: HERE rm asap
        System.err.print("asgns:");
        for (int i=0; i<docs.size(); i++) System.err.print(" c["+i+"]="+improver.getClusteringIndices()[i]);
        System.err.println("");
        // itc: HERE rm up to here
*/
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
