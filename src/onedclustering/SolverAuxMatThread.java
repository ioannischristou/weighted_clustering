package onedclustering;

public class SolverAuxMatThread extends Thread {
  private SolverAuxMat _r = null;

  public SolverAuxMatThread(SolverAuxMat r) {
    _r = r;
  }

  public void run() {
    _r.go();
  }

  public SolverAuxMat getSolverAuxMat() {
    return _r;
  }

}
