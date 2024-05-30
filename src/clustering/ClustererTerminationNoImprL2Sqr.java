package clustering;

public class ClustererTerminationNoImprL2Sqr implements ClustererTermination {

  private double _oldVal = Double.MAX_VALUE;
  private Clusterer _cl = null;
  private Evaluator _eval = null;
  private int _totIters=0;
  private final static double _c = 1e-16;
  private boolean _showVal=false;

  public ClustererTerminationNoImprL2Sqr() {
    _totIters=0;
  }


  public void registerClustering(Clusterer cl) {
    _cl = cl;
    _oldVal = Double.MAX_VALUE-1;
    _eval = new KMeansSqrEvaluator();
    _totIters = 0;
    Boolean sv = (Boolean) _cl.getParams().get("showintermedvalsinclusterdocs");
    if (sv!=null) _showVal = sv.booleanValue();
  }


  public boolean isDone() {
    _totIters++;
    try {
      double new_val = _cl.eval(_eval);
      if (_showVal)
        System.err.println("ClustererTerminationNoImprL2Sqr.isDone(): clustervalue="+new_val);
      // if (Math.abs(new_val-_oldVal) <= _c) {
      if (new_val >= _oldVal - _c) {
        System.err.println("ClustererTerminationNoImprL2Sqr: done after "+
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

