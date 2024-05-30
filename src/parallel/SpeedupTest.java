package parallel;

public class SpeedupTest {
  public SpeedupTest() {
  }

  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      int upto = Integer.parseInt(args[0]);
      int numthreads = Integer.parseInt(args[1]);
      int p = upto / numthreads;
      int l = 1;
      int u = p;
      Tsum[] tis = new Tsum[numthreads];
      for (int i = 0; i < numthreads; i++) {
        tis[i] = new Tsum(l, u);
        tis[i].start();
        l = u + 1;
        u += p;
      }
      int res = 0;
      for (int i = 0; i < numthreads; i++) {
        tis[i].join();
        res += tis[i].numprimes();
      }
      long time = System.currentTimeMillis()-start;
      System.out.println("Res="+res+" (in time="+time+" msecs.");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}


class Tsum extends Thread {
  long _l, _u;
  long _numprimes=0;
  public Tsum(long low, long high) {
    super();
    _l = low;
    _u = high;
  }

  public void run() {
    for (long i=_l; i<_u; i++) {
      if (isprime(i)) _numprimes++;
    }
  }


  public long numprimes() { return _numprimes; }


  private boolean isprime(long n) {
    if (n<2) return false;
    for (long i=2; i<n; i++) {
      if (n % i == 0) return false;
    }
    return true;
  }
}
