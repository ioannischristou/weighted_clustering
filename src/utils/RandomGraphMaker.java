package utils;

import java.util.*;
import coarsening.*;

public class RandomGraphMaker {
  private int _numnodes;
  private double _xlen;
  private double _ylen;
  private double _r;

  public RandomGraphMaker(int numnodes, double xdim, double ydim, double radius) {
    _numnodes = numnodes;
    _xlen=xdim;
    _ylen=ydim;
    _r=radius;
  }

  public Graph buildUniformRandomDualGraph() throws GraphException {
    // 1. set nodes in random places uniformly.
    double[] _x = new double[_numnodes];
    double[] _y = new double[_numnodes];
    for (int i = 0; i < _numnodes; i++) {
      _x[i] = RndUtil.getInstance().getRandom().nextDouble() * _xlen;
      _y[i] = RndUtil.getInstance().getRandom().nextDouble() * _ylen;
    }
    // 2. compute arcs
    Set[] nbors = new Set[_numnodes];
    int numarcs = 0;
    for (int i=0; i<_numnodes; i++) {
      nbors[i] = new TreeSet();
      for (int j=i+1; j<_numnodes; j++) {
        if (Math.pow(_x[i]-_x[j],2.0)+Math.pow(_y[i]-_y[j],2.0) <= _r*_r) { // i and j are nbors
          nbors[i].add(new Integer(j));
          ++numarcs;
        }
      }
    }
    Graph g = new Graph(_numnodes, numarcs);
    for (int i=0; i<_numnodes; i++) {
      Set nborsi = nbors[i];
      Iterator it = nborsi.iterator();
      while (it.hasNext()) {
        Integer j = (Integer) it.next();
        g.addLink(i, j.intValue(), 1);
      }
    }
    // 3. return dual graph
    Graph dg = g.getDual();
    return dg;
  }


  public static void main(String[] args) {
    if (args.length!=5) {
      System.err.println("usage: java -cp <classpath> utils.RandomGraphMaker <numnodes> <xdim> <ydim> <radius> <filename>");
      System.exit(-1);
    }
    try {
      long start_time = System.currentTimeMillis();
      RandomGraphMaker maker = new RandomGraphMaker(Integer.parseInt(args[0]),
                                                    Double.parseDouble(args[1]),
                                                    Double.parseDouble(args[2]),
                                                    Double.parseDouble(args[3]));
      Graph g = maker.buildUniformRandomDualGraph();
      DataMgr.writeGraphToFile2(g, args[4]);
      long duration = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+duration);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

