package clustering;

class ClustererThread extends Thread {
  private ClustererAux _r=null;

  public ClustererThread(ClustererAux r) {
    _r = r;
  }

  public void run() {
    _r.go();
  }

  public ClustererAux getClustererAux() {
    return _r;
  }
}

