package clustering;

import java.util.Vector;
import java.util.Hashtable;

public abstract class EnhancedEvaluator implements Evaluator {
  private Vector _allDocs;

  public synchronized void setMasterDocSet(Vector docs) {
    _allDocs = docs;
  }
  public abstract void setParams(Hashtable params);
  public synchronized Vector getMasterDocSet() { return _allDocs; }
  public abstract double eval(Vector docs, int[] clusterindices) throws ClustererException;
  public abstract double evalCluster(Vector clusterdocs) throws ClustererException;
  public double bestOf(double v1, double v2) {
    // default: bigger is better
    return Math.max(v1, v2);
  }
  public double getWorstValue() {
    return Double.NEGATIVE_INFINITY;  // default: bigger is better
  }

}
