package parallel;

import java.util.HashSet;

/**
 * <p>Title: Thread Coordination Management System</p>
 * <p>Description: Singleton class providing proper synchronization between
 * readers and writers. Many readers may concurently execute, but
 * there can be only one writer at a time, and while a writer has
 * control, no reader may gain access (except the writer itself).
 * The clients of this class
 * when wishing to gain read access, simply call
 * DMCoordinator.getInstance().getReadAccess()
 * and when they're done must call
 * DMCoordinator.getInstance().releaseReadAccess()
 * and similarly for writers.
 * It is also possible for a reader to attempt to upgrade to a Write Lock
 * which will throw an exception however if there are more than 1 reader at
 * the time of the attempt.
 * It is the responsibility of the clients
 * to ensure that these methods must always be called in pairs (every getXXX
 * must be followed by a releaseXXX).
 * </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */

public class DMCoordinator {

  // synchronizing variables
  private int _readers;
  private int _writerCalls;
  private Thread _writerThread=null;
  private HashSet _readerThreads=null;
  private HashSet _readers4Upgrade=null;

  static private DMCoordinator _DMCoord=null;


  /**
   * provides, securely, the unique instance of DMCoordinator that is
   * used to coordinate readers and writers. The method needs to be
   * synchronized (at the class level, as it's a static method) so as
   * to avoid the possibility of two different client threads receiving
   * different DMCoordinator objects to coordinate on (which results in no
   * coordination).
   */
  synchronized static public DMCoordinator getInstance() {
    if (_DMCoord==null) _DMCoord = new DMCoordinator();
    return _DMCoord;
  }


  synchronized public void getReadAccess() {
    if (_writerThread == Thread.currentThread()) {  // we initiated locking
      ++_readers;
      _readerThreads.add(Thread.currentThread());
      return;
    }
    try {
      while (_writerCalls>0) {
        wait();  // someone ELSE has a write lock
      }
      ++_readers;
      _readerThreads.add(Thread.currentThread());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();  // this is the recommended action
    }
  }

  /**
   * This method is the only method that might throw and it does so in the
   * event that a deadlock would otherwise occur. This may happen only
   * if many readers try simultaneously to upgrade their locks to Write Lock
   * status. In this case the method will throw.
   * @throws InterruptedException
   */
  synchronized public void getWriteAccess() throws InterruptedException {
    if (_writerThread == Thread.currentThread()) {
      ++_writerCalls;
      return;  // we have the lock
    }
    if (_readers4Upgrade.size()>0 && _readerThreads.contains(Thread.currentThread()) )
      throw new InterruptedException("Illegal Attempt to Upgrade Read Lock");
    if (_readerThreads.contains(Thread.currentThread())) _readers4Upgrade.add(Thread.currentThread());
    try {
      // wait until there are no other writers, and there is no reader except possibly me
      while (_writerCalls>0 ||
             (_readers>0 && ( _readers!=1 || _readerThreads.contains(Thread.currentThread())==false))) {
        wait();
      }
      ++_writerCalls;
      _writerThread = Thread.currentThread();  // get the write lock
      _readers4Upgrade.remove(Thread.currentThread());  // no harm if thread is not in here
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();  // this is the recommended action
    }
  }


  synchronized public void releaseReadAccess() {
    if (_readerThreads.contains(Thread.currentThread())==false) {
      System.err.println("Thread not allowed to call DMCoordinator.releaseReadAccess()");
      return;
    }
    --_readers;
    _readerThreads.remove(Thread.currentThread());
    if (_readers<=1) {
      notifyAll();  // notify in case someone who had a read lock wants to upgrade
    }
  }


  synchronized public void releaseWriteAccess() {
    if (_writerThread!=Thread.currentThread()) {
      System.err.println(
          "Thread not allowed to call DMCoordinator.releaseWriteAccess()");
      return;
    }
    if (--_writerCalls==0) {
      _writerThread = null;
      notifyAll();
    }
  }


  synchronized public int getNumReaders() {
    return _readers;
  }


  synchronized public int getNumWriters() {
    return _writerCalls;
  }


  private DMCoordinator() {
    _readers=0;
    _writerCalls=0;
    _readerThreads = new HashSet();
    _readers4Upgrade = new HashSet();
  }
}
