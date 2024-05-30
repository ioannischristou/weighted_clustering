package parallel;

import java.util.*;

class A {
  int _i=0;
  public A() { }
  public synchronized void getLock() {
      while (_i!=0) {
        try {
          wait();
        }
        catch (InterruptedException e) { e.printStackTrace(); }
      }
    _i=1;
  }
  public synchronized void releaseLock() {
    _i=0;
    notifyAll();
  }
}


public class atomic {

  private static Hashtable _h = new Hashtable();  // map<Integer i, A a>


  public static void start(int i) {
    A ai = getA(i);
    ai.getLock();
  }


  public static void end(int i) {
    A ai = getA(i);
    ai.releaseLock();
  }


  private static synchronized A getA(int i) {
    Integer ii = new Integer(i);
    A a = (A) _h.get(ii);
    if (a==null) {
      a = new A();
      _h.put(ii, a);
    }
    return a;
  }
}

