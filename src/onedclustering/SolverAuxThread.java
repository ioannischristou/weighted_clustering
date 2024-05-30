package onedclustering;

public class SolverAuxThread extends Thread {
  private SolverAux _r = null;

  public SolverAuxThread(SolverAux r) {
    _r = r;
  }

  public void run() {
    _r.go();
  }

  public SolverAux getSolverAux() {
    return _r;
  }

}
