package parallel;

import java.util.Hashtable;

/**
 *
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * The class guarantees that if threads are synchronized via the OrderedBarrier
 * class, any tasks given as args to the orderedBarrier(task) call, will execute
 * in the order with which the threads executing them were first registered with
 * the barrier.
 * Behavior is undefined if after some threads have started using the orderedBarrier()
 * call, another thread calls the addThread() method.
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OrderedBarrier {
  private static OrderedBarrier _instance;
  private Hashtable _threads;  // map<Thread id, Integer priority>
  private int _numThreads2Wait4=0;
  private boolean _onHold[];


  /**
   * method must be called before barrier() can be used. It should be invoked
   * only once by each thread which wishes to coordinate with the barrier()
   * call.
   * @param n int
   */
  public synchronized static void addThread(Thread t) {
    if (_instance==null)
      _instance = new OrderedBarrier();
    _instance._threads.put(t, new Integer(_instance._numThreads2Wait4++));
  }


  public static OrderedBarrier getInstance() {
    return _instance;
  }


  public void orderedBarrier(TaskObject task) {
    synchronized (this) {
      if (_onHold == null) {
        _onHold = new boolean[_threads.size()];
        // _numThreads2Wait4 = _threads.size();
        for (int i = 0; i < _numThreads2Wait4; i++) _onHold[i] = false;
      }
    }
    while (passOrderedBarrier(task)==false) {
      try {
        Thread.currentThread().sleep(10);
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
  }


  private synchronized boolean passOrderedBarrier(TaskObject t) {
    // check if everybody is out, then return true
    if (_numThreads2Wait4<0) {
      return false;  // thread tried to enter loop before having all others exit the previous one
    }
    // set _onHold[i]
    Integer idI = (Integer) _threads.get(Thread.currentThread());
    int id = idI.intValue();
    _onHold[id] = true;
    // ensure all threads enter the barrier
    --_numThreads2Wait4;
    while (_numThreads2Wait4>0) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // at this point all except me are waiting in the wait() above
    boolean hold = true;
    while (hold) {
      boolean notyet = false;  // notyet is false if all threads before me have exited loop
      for (int i=0; i<id && !notyet; i++) {
        if (_onHold[i]) notyet = true;
      }
      if (notyet==false) {
        _onHold[id] = false;
        hold = false;  // it's my time to exit
        break;
      }
      else notifyAll();  // it's not my time to exit yet, but I have to tell others to wake up
      try {
        wait();
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    if (--_numThreads2Wait4==-_threads.size()) {
      // I am the last thread to pass this point, so reset
      _numThreads2Wait4 = _threads.size();  // reset
    }
    notifyAll();
    if (t!=null) t.run();  // guaranteed that the tasks will execute completely
                           // in start-order of the threads, unless they are
                           // threads themselves in which case execution completion
                           // cannot be guaranteed
                           // this is because of the synchronized method.
    return true;
  }


  private OrderedBarrier() {
    // _numThreads2Wait4 = n;
    _threads = new Hashtable();
  }

}

