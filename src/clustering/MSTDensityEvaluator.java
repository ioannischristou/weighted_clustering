package clustering;

import java.util.*;
import coarsening.*;

public class MSTDensityEvaluator extends EnhancedEvaluator {
  public MSTDensityEvaluator() {
  }


  public void setParams(Hashtable params) {
    // no-op
  }


  public double bestOf(double v1, double v2) {
    return Math.min(v1, v2);
  }


  public double getWorstValue() {
    return Double.MAX_VALUE;
  }


  /**
   * return the total density value of the clusters found by clusterer cl.
   * Smaller is better.
   * @param cl Clusterer
   * @return double
   */
  public double eval(Clusterer cl) throws ClustererException {
    final int[] inds = cl.getClusteringIndices();
    return eval(cl.getCurrentDocs(), inds);
  }


  /**
   *
   * @param docs Vector Vector<Document>
   * @param inds int[] array[0...docs.size()-1] values in [0,...k-1]
   * @throws ClustererException
   * @return double
   */
  public double eval(Vector docs, int[] inds) throws ClustererException {
    double res = 0.0;

    final int n = inds.length;
    // figure out k
    int k=0;
    for (int i=0; i<n; i++)
      if (k<inds[i]+1) k = inds[i]+1;
    // figure out cards[j] j=0...k-1
    Vector cards[] = new Vector[k];
    for (int i=0; i<k; i++) {
      cards[i] = new Vector();
    }
    for (int i=0; i<n; i++) {
      cards[inds[i]].addElement(docs.elementAt(i));
    }
    try {
      for (int i = 0; i < k; i++) {
        if (cards[i].size()<=1) continue;  // skip singletons or empty groups
        EMST emst = new EMST(cards[i]);
        double cost = emst.cost();
        res += cost/cards[i].size();
      }
    }
    catch (GraphException e) {
      throw new ClustererException("emst() threw GraphException");
    }
    return res;
  }


  /**
   *
   * @param docs Vector Vector<Document>
   * @throws ClustererException
   * @return double
   */
  public double evalCluster(Vector docs) throws ClustererException {
    try {
      return MSTDensityEvaluator.evalClusterStatic(docs);
    }
    catch (GraphException e) {
      e.printStackTrace();
      throw new ClustererException("mst threw");
    }
  }



  /**
   *
   * @param docs Vector Vector<Document>
   * @throws ClustererException
   * @return double
   */
  static public double evalClusterStatic(Vector docs) throws ClustererException, GraphException {
    if (docs==null || docs.size()==0)
      throw new ClustererException(
        "MSTDensEvaluator.evalCluster(): empty or null docs passed in");
    else if (docs.size()==1) return 0;
    EMST mst = new EMST(docs);
    return mst.cost()/docs.size();
  }

}
