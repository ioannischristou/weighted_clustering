package clustering;

import java.util.*;

public abstract class ClustererAux {
  protected Clusterer _master;
  protected int _ind[];
  protected int _numi[];
  protected int _starti=-1;
  protected int _endi=-1;  // the indices to work on [_starti, _endi]
  private boolean _finish = false;


  public ClustererAux(Clusterer master, int[] ind, int[] numi) {
    _master = master;
    _ind = ind;
    _numi = numi;
    /*
     _numi = new int[numi.length];
     for (int i=0; i<numi.length; i++) _numi[i] = numi[i];  // own private copy
     }
    */
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
    while (_starti!=-1 || _endi!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
  }


  public synchronized void runFromTo(int starti, int endi) {
    while (_starti!=-1 || _endi!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // OK, now set values
    _starti = starti; _endi = endi;
    notify();  // itc: HERE do I need notifyAll(); ?
  }


  /**
   * this is the only method that Clusterers wishing to implement multi-threading
   * must implement.
   */
  public abstract void runTask();

  private synchronized void go1() {
    while (_starti==-1 || _endi==-1) {
      if (_finish) return;  // finish
      try {
        wait();  // wait for order
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // run the code
    runTask();
    // finished, reset indices
    _starti=-1; _endi=-1;
    notify();
  }
}
