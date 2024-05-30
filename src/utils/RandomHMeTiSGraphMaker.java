package utils;

import coarsening.*;
import java.util.*;

public class RandomHMeTiSGraphMaker {
  public RandomHMeTiSGraphMaker() {
  }

  public static void main(String[] args) {
    if (args.length!=4) {
      System.err.println("usage: java -cp <classpath> utils.RandomHMeTiSGraphMaker <numnodes> <numarcs> <maxweight> <filename>");
      return;
    }
    try {
      final int n = Integer.parseInt(args[0]);
      final int m = Integer.parseInt(args[1]);
      final int wmax = Integer.parseInt(args[2]);
      final String graphfile = args[3];
      Graph g = new Graph(n, m);
      Random r = new Random();
      for (int i = 0; i < m; i++) {
        int s = r.nextInt(n/2);
        int e = -1;
        int count=0;
        boolean ok = false;
        while (count++<100) {
          e = r.nextInt(n);
          if (s>=e) continue;
          Set s_nbors = g.getNode(s).getNbors();
          Node en = g.getNode(e);
          if (s_nbors.contains(en)==false) {
            ok = true;
            break;
          }
        }
        if (!ok) {
          i--;
          continue;
        }
        int w = r.nextInt(wmax);
        g.addLink(s, e, w+1);
      }
      DataMgr.writeGraphDirectToHGRFile(g, graphfile);
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }
}
