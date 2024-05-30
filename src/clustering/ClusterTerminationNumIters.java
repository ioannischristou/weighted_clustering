package clustering;

public class ClusterTerminationNumIters implements ClustererTermination {
  private int _numItersLeft;
  private int _initialNumIters;

  public ClusterTerminationNumIters(int totalItersAllowed) {
    _numItersLeft = totalItersAllowed;
    _initialNumIters= totalItersAllowed;
  }

  
  public ClusterTerminationNumIters(Integer totalItersAllowed) {
    _numItersLeft = totalItersAllowed.intValue();
    _initialNumIters= totalItersAllowed.intValue();
  }


  public boolean isDone() {
    if (_numItersLeft-- <= 0) return true;
    return false;
  }


  public void registerClustering(Clusterer p) {
    _numItersLeft = _initialNumIters;  // reset iterations left
  }
}
