package packing;

import EDU.oswego.cs.dl.util.concurrent.*;

public class BBThreadPool {
  static private int _numThreads = 1;  // default
  static PooledExecutor _pool = new PooledExecutor(_numThreads);

  public static void setNumthreads(int n) {
    _numThreads = n;
    _pool = new PooledExecutor(_numThreads);
  }
  public static int getNumThreads() { return _numThreads; }
  public static PooledExecutor getPool() { return _pool; }

}
