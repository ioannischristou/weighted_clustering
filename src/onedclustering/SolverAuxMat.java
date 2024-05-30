package onedclustering;

public class SolverAuxMat {
  Params _p;
  double _allclusters[][];
  double _part[][];
  int _partcs[][];
  int _starti=-1;
  int _endi=-1;  // the indices to work on [_starti, _endi]
  int _k = -1;
  boolean _finish = false;

  public SolverAuxMat(Params p, double part[][], int partcs[][], double allclusters[][]) {
    _p = p;
    _part = part;
    _partcs = partcs;
    _allclusters = allclusters;
  }


  public void go() {
    while (getFinish()==false) {
      go1();
    }
  }


  public synchronized boolean getFinish() {
    return _finish;
  }


  public synchronized void setFinish() {
    _finish = true;
    notify();
  }


  public synchronized void waitForTask() {
    while (_starti!=-1 || _endi!=-1 || _k!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
  }


  public synchronized void runFromTo(int starti, int endi, int k) {
    while (_starti!=-1 || _endi!=-1 || _k!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // OK, now set values
    _starti = starti; _endi = endi; _k = k;
    notify();  // itc: HERE do I need notifyAll(); ?
  }

  private synchronized void go1() {
    while (_starti==-1 || _endi==-1 || _k==-1) {
      if (_finish) return;  // finish
      try {
        wait();  // wait for order
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // run the code
    final int n = _p.getSequenceLength();
    for (int i = _endi; i >= _starti; i--) {
      if (i + _k == n - 1) {
        _part[i][_k] = 0;
        _partcs[i][_k] = i;
      }
      else if (i + _k > n - 1) _part[i][_k] = Double.POSITIVE_INFINITY;
      else {
        // i+k<n-1
        double bv = Double.POSITIVE_INFINITY;
        int bind = -1;
        for (int j = i; j <= n - 2; j++) {
          // ++_numiters;
          double costij = _allclusters[i][j];
          if (bv > costij + _part[j + 1][_k - 1]) {
            bv = costij + _part[j + 1][_k - 1];
            bind = j;
          }
        }
        _part[i][_k] = bv;
        _partcs[i][_k] = bind;
      }
    }
    // finished, reset indices
    _starti=-1; _endi=-1; _k = -1;
    notify();
  }
}
