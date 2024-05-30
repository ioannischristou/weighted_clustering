package parallel;

public class Barrier {
  private static Barrier _instance;
  private static int _origNumThreads;
  private int _numThreads2Wait4;


  /**
   * method must be called before barrier() can be used. It should be invoked
   * only once for each group of threads which to coordinate with the barrier()
   * call.
   * @param n int
   */
  public static void setNumThreads(int n) {
    _instance = new Barrier(n);
    _origNumThreads = n;
  }


  public static Barrier getInstance() {
    return _instance;
  }


  /**
   * main method of the class: it waits until _origNumThreads threads have entered
   * this method, and it guarantees that if a thread enters this method again
   * before all threads have exited the previous barrier() call it will wait
   * first for all the other threads to exit and then will proceed.
   */
  public void barrier() {
    while (passBarrier()==false) {
      // repeat: this is not busy-waiting behavior except for the case
      // when a thread has passed the barrier and is calling again
      // the barrier() method before all other threads exit the previous call
      try {
        Thread.currentThread().sleep(10);
      }
      catch (InterruptedException e) { }
    }
  }


  private Barrier(int n) {
    _numThreads2Wait4 = n;
  }


  private synchronized boolean passBarrier() {
    if (_numThreads2Wait4 < 0) return false;  // thread just ran through to the next barrier point before reseting
    --_numThreads2Wait4;
    while (_numThreads2Wait4>0) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    if (--_numThreads2Wait4==-_origNumThreads) {
      // I am the last thread to pass this point, so reset
      _numThreads2Wait4 = _origNumThreads;  // reset
    }
    notifyAll();  // wake them all up
    return true;
  }

}

