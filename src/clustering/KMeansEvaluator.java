package clustering;

import java.util.*;

public class KMeansEvaluator implements Evaluator {
  private static DocumentDistIntf _m = new DocumentDistL2();

  public KMeansEvaluator() {
  }


  /**
   * return the sum of the distances of each document from the cluster center
   * it belongs.
   * @param cl Clusterer
   * @return double
   * @throws ClustererException
   */
  public double eval(Clusterer cl) throws ClustererException {
    double ret = 0.0;
    Vector centers = cl.getCurrentCenters();
    Vector docs = cl.getCurrentDocs();
    int[] asgnms = cl.getClusteringIndices();
    if (asgnms==null) {
      // no clustering has occured, or failed
      return 1e+15;  // itc: HERE this needs to be corrected ASAP ...
    }
    final int n = docs.size();
    // final int k = centers.size();
    for (int i=0; i<n; i++) {
      Document di = (Document) docs.elementAt(i);
      Document ci = (Document) centers.elementAt(asgnms[i]);
      ret += _m.dist(di, ci);
    }
    return ret;
  }


  public static double evalCluster(Vector docs) throws ClustererException {
    double ret=0.0;
    Document center = Document.getCenter(docs, null);  // itc: HERE 20220223
    final int docs_size = docs.size();
    for (int i=0; i<docs_size; i++) {
      Document doc_i = (Document) docs.elementAt(i);
      ret += _m.dist(doc_i, center);
    }
    return ret;
  }
}

