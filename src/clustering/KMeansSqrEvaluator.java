package clustering;

import java.util.*;

public class KMeansSqrEvaluator implements Evaluator {
  private static DocumentDistIntf _m = new DocumentDistL2Sqr();

  public KMeansSqrEvaluator() {
  }


  /**
   * return the sum of the distances of each document from the cluster center
   * it belongs.
   * @param cl Clusterer
   * @return double
   * @throws Exception
   */
  public double eval(Clusterer cl) throws ClustererException {
    double ret = 0.0;
    Vector centers = cl.getCurrentCenters();
    Vector docs = cl.getCurrentDocs();
    int[] asgnms = cl.getClusteringIndices();
    if (asgnms==null) {
      // no clustering has occured yet, or failed
      // throw new ClustererException("null asgnms");
      return 1.e+30;
    }
    final int n = docs.size();
    // final int k = centers.size();
    final Hashtable params = cl.getParams();
    final double[] wgts = params.containsKey("weights") ? 
                            (double[]) params.get("weights") : null;
    for (int i=0; i<n; i++) {
      Document di = (Document) docs.elementAt(i);
      Document ci = (Document) centers.elementAt(asgnms[i]);
      final double d = _m.dist(di, ci);
      ret += (wgts==null ? d : d*wgts[i]);
    }
    return ret;
  }


  /**
   * this does not take into account any weights on the data points.
   * @param docs
   * @return
   * @throws ClustererException 
   */
  public static double evalCluster(Vector docs) throws ClustererException {
    double ret=0.0;
    Document center = Document.getCenter(docs, null);
    final int docs_size = docs.size();
    for (int i=0; i<docs_size; i++) {
      Document doc_i = (Document) docs.elementAt(i);
      ret += _m.dist(doc_i, center);
    }
    return ret;
  }
}

