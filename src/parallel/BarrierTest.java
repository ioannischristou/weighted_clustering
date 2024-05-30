package parallel;

public class BarrierTest {
  public BarrierTest() {
  }

  public static void main(String[] args) {
    BThread arr[] = new BThread[10];
    // Barrier.setNumThreads(arr.length);
    for (int i=0; i<arr.length; i++) {
      arr[i] = new BThread(i);
      OrderedBarrier.addThread(arr[i]);
    }
    for (int i=0; i<arr.length; i++) {
      arr[i].start();
    }
  }
}


class BThread extends Thread {
  private int _id;
  public BThread(int id) { _id = id; }
  public void run() {
    for (int i=0; i<10; i++) {
      // System.out.println("t-id="+_id+" i="+i);
      // Barrier.getInstance().barrier();
      OrderedBarrier.getInstance().orderedBarrier(new PrintTask(i, _id));
    }
  }
}


class PrintTask implements TaskObject {
  private int _i, _j;
  public PrintTask(int i, int j) {
    _i = i; _j = j;
  }
  public void run() {
    System.out.println("Task-"+_i+" executes from thread "+_j);
  }
}
