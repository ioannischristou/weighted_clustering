package utils;

import coarsening.*;
import java.util.*;

public class RandomHMeTiSHGraphMaker {
  public RandomHMeTiSHGraphMaker() {
  }

  public static void main(String[] args) {
    if (args.length!=5) {
      System.err.println("usage: java -cp <classpath> utils.RandomHMeTiSHGraphMaker <numnodes> <numarcs> <maxnodesperarc> <maxweight> <filename>");
      return;
    }
    try {
      final int n = Integer.parseInt(args[0]);
      final int m = Integer.parseInt(args[1]);
      final int maxnodesperarc = Integer.parseInt(args[2]);
      final int wmax = Integer.parseInt(args[3]);
      final String graphfile = args[4];
      HGraph g = new HGraph(n, m);
      Random r = new Random();
      Set nodes = new HashSet();
      for (int i = 0; i < m; i++) {
        // create arc
        int arcnodes = r.nextInt(maxnodesperarc-2)+2;
        nodes.clear();
        int nid = r.nextInt(n);
        nodes.add(new Integer(nid));
        for (int j=1; j<arcnodes; j++) {
          // nodes.add(new Integer(r.nextInt(n)));
          // the arc should contain near-by nodes of nid
          double r2 = nid + r.nextGaussian()*10;
          int j2 = (int) Math.floor(r2);
          if (j2<0) j2 = 0;
          else if (j2>=n) j2 = n-1;
          nodes.add(new Integer(j2));
        }
        if (nodes.size()==1) {
          i--;
          continue;  // don't accept single node arcs
        }
        int w = r.nextInt(wmax);
        g.addHLink(nodes, w+1);
      }
      DataMgr.writeHGraphDirectToHGRFile(g, graphfile);
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }
}
