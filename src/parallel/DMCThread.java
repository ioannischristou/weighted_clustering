package parallel;

/**
 * <p>Title: parallel </p>
 * <p>Description: Minimal Test Thread</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */

public class DMCThread extends Thread {

  int _i;

  public DMCThread(int i) {
    _i = i;
  }

  public void run() {
    long start_time = System.currentTimeMillis();
    boolean done = false;
    while (!done) {
      done = (System.currentTimeMillis() - start_time)/1000.0 >= 10;  // run for 10 secs
      if (_i==0) {
        // reader
        DMCoordinator.getInstance().getReadAccess();
        try {
          Thread.currentThread().sleep(50);  // sleep
        } catch (InterruptedException e) {}
        System.out.println(
            "Thread id="+Thread.currentThread().toString()+
            " writers="+DMCoordinator.getInstance().getNumWriters()+
            " readers="+DMCoordinator.getInstance().getNumReaders());
        DMCoordinator.getInstance().releaseReadAccess();
        Thread.currentThread().yield();
        try { Thread.currentThread().sleep(30); } // sleep a bit
        catch (InterruptedException e) {}
      }
      else {
        try {
          // writer
          DMCoordinator.getInstance().getWriteAccess();
          Thread.currentThread().sleep(10);  // sleep
        } catch (InterruptedException e) {}
        System.out.println(
            "Thread id="+Thread.currentThread().toString()+
            "writers="+DMCoordinator.getInstance().getNumWriters()+
            " readers="+DMCoordinator.getInstance().getNumReaders());
        DMCoordinator.getInstance().releaseWriteAccess();
        Thread.currentThread().yield();
      }
    }
  }
}
