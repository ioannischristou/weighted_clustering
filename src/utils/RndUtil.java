package utils;

import java.util.*;

/**
 * Singleton class controlling random number generation
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RndUtil {
  private static RndUtil _instance = null;
  private static RndUtil _extras[] = null;
  private Random _random = null;
  private long _seed = 0;  // default seed


  public static RndUtil getInstance() {
    if (_instance==null) {
      _instance = new RndUtil();
    }
    return _instance;
  }


  public static RndUtil getInstance(int id) {
    return _extras[id];
  }

  public static void setExtraInstances(int num) {
    _extras = new RndUtil[num];
    for (int i=0; i<num; i++) {
      _extras[i] = new RndUtil();
      _extras[i]._random = new Random(_instance._seed+i+1);
    }
  }

  public void setSeed(long s) {
    _seed = s;
    _random = new Random(_seed);
    if (_extras!=null) {
      for (int i=0; i<_extras.length; i++) {
        _extras[i]._random = new Random(_instance._seed+i+1);
      }
    }
  }


  public Random getRandom() {
    return _random;
  }


  private RndUtil() {
    _random = new Random();  // maintain default unrepeatable random behaviour
  }
}
