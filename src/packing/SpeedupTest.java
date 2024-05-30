package packing;

/* The commented code works and shows nice speedups on multi-core processors

public class SpeedupTest {
  public SpeedupTest() {
  }

  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      int upto = Integer.parseInt(args[0]);
      int numthreads = Integer.parseInt(args[1]);
      BBThreadPool.setNumthreads(numthreads);
      BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
      BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
      int p = upto / numthreads;
      int l = 1;
      int u = p;
      Tsum[] tis = new Tsum[numthreads];
      for (int i = 0; i < numthreads; i++) {
        tis[i] = new Tsum(l, u);
        BBThreadPool.getPool().execute(tis[i]);
        l = u + 1;
        u += p;
      }
      int res = 0;
      for (int i = 0; i < numthreads; i++) {
        // tis[i].join();
        while (tis[i].isDone()==false) {
          try {
            Thread.currentThread().sleep(100);
          }
          catch (InterruptedException e) {
            // noop
          }
        }
        res += tis[i].numprimes();
      }
      BBThreadPool.getPool().shutdownNow();
      long time = System.currentTimeMillis()-start;
      System.out.println("Res="+res+" (in time="+time+" msecs.)");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}


class Tsum implements Runnable {
  long _l, _u;
  long _numprimes=0;
  boolean _isDone = false;
  public Tsum(long low, long high) {
    super();
    _l = low;
    _u = high;
  }

  public void run() {
    for (long i=_l; i<_u; i++) {
      if (isprime(i)) _numprimes++;
    }
    _isDone = true;
  }


  public long numprimes() { return _numprimes; }

  public boolean isDone() { return _isDone; }

  private boolean isprime(long n) {
    if (n<2) return false;
    for (long i=2; i<n; i++) {
      if (n % i == 0) return false;
    }
    return true;
  }
}

*/

public class SpeedupTest {
  public SpeedupTest() {
  }

  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      int upto = Integer.parseInt(args[0]);
      int numthreads = Integer.parseInt(args[1]);
      BBThreadPool.setNumthreads(numthreads);
      BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
      BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
      Tsum root = new Tsum(1,upto, null);
      BBThreadPool.getPool().execute(root);
      while (!root.isDone()) {
        Thread.currentThread().sleep(100);
      }
      long res = root.numprimes();
      BBThreadPool.getPool().shutdownNow();
      long time = System.currentTimeMillis()-start;
      System.out.println("Res="+res+" (in time="+time+" msecs.)");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}


class Tsum implements Runnable {
  long _l, _u;
  long _numprimes=0;
  boolean _isDone = false;
  Tsum _parent = null;
  int _numChildren;
  public Tsum(long low, long high, Tsum parent) {
    super();
    _l = low;
    _u = high;
    _parent = parent;
    _numChildren = 0;
  }

  public void run() {
    if (_u - _l <= 5) {
      for (long i = _l; i < _u; i++) {
        if (isprime(i)) _numprimes++;
      }
      _isDone = true;
      if (_parent!=null) _parent.notifyDone(this);
    }
    else {
      long mid = (_l+_u)/2;
      Tsum left = new Tsum(_l, mid, this);
      Tsum right = new Tsum(mid+1, _u, this);
      _numChildren = 2;
      try {
        BBThreadPool.getPool().execute(left);
        BBThreadPool.getPool().execute(right);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
        System.exit(-1);  // oops...
      }
    }
  }


  public long numprimes() { return _numprimes; }

  public boolean isDone() { return _isDone; }

  private void notifyDone(Tsum child) {
    if (_isDone) return;
    synchronized(this) {
      --_numChildren;
      _numprimes += child._numprimes;
      if (_numChildren == 0) {
        _isDone = true;
      }
    }
    if (_isDone && _parent!=null) _parent.notifyDone(this);
  }

  private boolean isprime(long n) {
    if (n<2) return false;
    for (long i=2; i<n; i++) {
      if (n % i == 0) return false;
    }
    return true;
  }
}
