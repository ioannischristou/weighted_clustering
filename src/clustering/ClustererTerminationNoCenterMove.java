package clustering;

import java.util.Vector;

public class ClustererTerminationNoCenterMove implements ClustererTermination {
  private Clusterer _cl;
  private Vector _oldcenters;
  private int _totIters = 0;
  final static private double _c = 1e-7;
  final static private int _maxIters = 50;

  public ClustererTerminationNoCenterMove() {
  }


  public void registerClustering(Clusterer cl) {
    _cl = cl;
    _oldcenters = null;
    _totIters = 0;
  }


  public boolean isDone() {
    _totIters++;
    boolean result = true;
    // figure out if _oldcenters is the same as new centers
    try {
      if (_oldcenters != null) {
        for (int i = 0; i < _oldcenters.size() && result; i++) {
          Document ci = (Document) _cl.getCurrentCenters().elementAt(i);
          Document oci = (Document) _oldcenters.elementAt(i);
          double dist = Document.d(ci, oci);
          if (dist>_c) result = false;
        }
      }
      if (_oldcenters==null) {
        result = false;
      }
      if (_cl.getCurrentCenters()!=null) {
        _oldcenters = new Vector(_cl.getCurrentCenters());
      }
      // stop after max_iters
      if (_totIters>=_maxIters) result = true;
      if (result)
        System.err.println("ClustererTerminationNoCenterMove: done after "+
                           _totIters+" iterations.");
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}
