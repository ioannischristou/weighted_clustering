package clustering;

public class ClustererTerminationNoImprL2 implements ClustererTermination {

  private double _oldVal = Double.MAX_VALUE;
  private Clusterer _cl = null;
  private Evaluator _eval = null;
  private int _totIters=0;
  private final static double _c = 1e-12;


  public ClustererTerminationNoImprL2() {
    _totIters=0;
  }


  public void registerClustering(Clusterer cl) {
    _cl = cl;
    _oldVal = Double.MAX_VALUE-1;
    _eval = new KMeansEvaluator();
    _totIters = 0;
  }


  public boolean isDone() {
    _totIters++;
    try {
      double new_val = _cl.eval(_eval);
      // if (Math.abs(new_val-_oldVal) <= _c) {
      if (new_val >= _oldVal - _c) {
        System.err.println("ClustererTerminationNoImprL2: done after "+
                           _totIters+" iterations.");
        return true;
      }
      else _oldVal = new_val;
    }
    catch (Exception e) {
      e.printStackTrace();
      return true;  // leave
    }
    return false;  // still moves are being made
  }
}

