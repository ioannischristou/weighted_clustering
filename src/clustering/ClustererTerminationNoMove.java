package clustering;

public class ClustererTerminationNoMove implements ClustererTermination {
  private Clusterer _cl;
  private int _oldasgns[];
  private int _totIters = 0;

  public ClustererTerminationNoMove() {
  }


  public void registerClustering(Clusterer cl) {
    _cl = cl;
    _oldasgns = null;
    try {
      if (_cl.getClusteringIndices()!=null) {
        _oldasgns = new int[_cl.getClusteringIndices().length];
        for (int i = 0; i < _oldasgns.length; i++)
          _oldasgns[i] = _cl.getClusteringIndices()[i];
      }
    }
    catch (Exception e) {
      _oldasgns = null;
    }
    _totIters = 0;
  }


  public boolean isDone() {
    _totIters++;
    boolean result = true;
    // figure out if _oldasgns is the same as new clustering indices
    try {
      if (_oldasgns != null) {
        for (int i = 0; i < _oldasgns.length && result; i++) {
          if (_oldasgns[i] != _cl.getClusteringIndices()[i]) result = false;
        }
      }
      if (_oldasgns==null) {
        result = false;
        if (_cl.getClusteringIndices()!=null)
          _oldasgns = new int[_cl.getClusteringIndices().length];
      }
      if (_cl.getClusteringIndices()!=null) {
        for (int i = 0; i < _oldasgns.length; i++) {
          _oldasgns[i] = _cl.getClusteringIndices()[i];
        }
      }
      if (result)
        System.err.println("ClustererTerminationNoMove: done after "+
                           _totIters+" iterations.");
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}
