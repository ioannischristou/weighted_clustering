package clustering;

import java.util.*;

public class PMedianEvaluator extends EnhancedEvaluator {
  private static DocumentDistIntf _m = new DocumentDistL2();

  public PMedianEvaluator() {
  }


  /**
   * return the sum of the distances of each document from the document that
   * minimizes this sum.
   * @param cl Clusterer
   * @return double
   * @throws ClustererException
   */
  public double eval(Clusterer cl) throws ClustererException {
    double ret = 0.0;
    Vector orig_centers = cl.getCurrentCenters();  // to get k
    final int k = orig_centers.size();
    Vector docs = cl.getCurrentDocs();
    int[] asgnms = cl.getClusteringIndices();
    if (asgnms==null) {
      // no clustering has occured, or failed
      return Double.POSITIVE_INFINITY;
    }
    Vector centers = getCenters(docs, asgnms, k);  // find best document to serve
                                                // as center for each cluster
    final int n = docs.size();
    for (int i=0; i<n; i++) {
      Document di = (Document) docs.elementAt(i);
      Document ci = (Document) centers.elementAt(asgnms[i]);
      ret += _m.dist(di, ci);
    }
    return ret;
  }


  public double evalCluster(Vector docs) throws ClustererException {
    throw new ClustererException("method not supported");
  }


  public double eval(Vector docs, int[] arr) throws ClustererException {
    throw new ClustererException("method not supported");
  }


  public void setParams(Hashtable params) {
    DocumentDistIntf m = (DocumentDistIntf) params.get("metric");
    if (m!=null) _m = m;
  }


  public double bestOf(double v1, double v2) {
    // smaller is better
    return Math.min(v1, v2);
  }
  public double getWorstValue() {
    return Double.MAX_VALUE;  // smaller is better
  }


  private static Vector getCenters(Vector docs, int[] asngs, int p) throws ClustererException {
    Vector centers = new Vector();
    final int n = docs.size();
    double dists[][] = new double[p][n];  // the sum of distances of group i when doc j becomes the center of the group
    // by default zero?
    for (int j=0; j<n; j++) {
      Document dj = (Document) docs.elementAt(j);
      for (int r=0; r<n; r++) {
        int i = asngs[r];
        Document dr = (Document) docs.elementAt(r);
        dists[i][j] += _m.dist(dj, dr);
      }
    }
    // find the best j for each group
    for (int i=0; i<p; i++) {
      double best_val = Double.MAX_VALUE;
      int best_ind = -1;
      for (int j=0; j<n; j++) {
        if (dists[i][j] < best_val) {
          best_val = dists[i][j];
          best_ind = j;
        }
      }
      centers.addElement(new Document((Document) docs.elementAt(best_ind)));
    }
    return centers;
  }
}

