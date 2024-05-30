package coarsening;

import java.util.*;
import java.io.*;

public class AllCliqueFinder {
  public AllCliqueFinder() {
  }

  public static void main(String[] args) {
/*
    // test
    try {
      Graph g = new Graph(5, 6);
      g.addLink(0, 1, 1);
      g.addLink(0, 2, 2);
      g.addLink(1, 2, 3);
      g.addLink(1, 3, 5);
      g.addLink(2, 3, 4);
      g.addLink(3, 4, 6);
      Set cliques = g.getAllMaximalCliques();
      Iterator iter = cliques.iterator();
      while (iter.hasNext()) {
        Set c = (Set) iter.next();
        Iterator it2 = c.iterator();
        while (it2.hasNext()) {
          Integer id = (Integer) it2.next();
          System.out.print(id.intValue()+" ");
        }
        System.out.println("");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
*/
    try {
      if (args.length==0) {
        System.err.println("usage: java -cp <classpath> coarsener.AllCliqueFinder <graphfile> [mapfile] [mincliquevalue]");
        return;
      }
      String graphfile = args[0];
      String mapfile = null;
      if (args.length>1) mapfile = args[1];

      double minval = Double.NEGATIVE_INFINITY;
      if (args.length>2) minval = Double.parseDouble(args[2]);

      Graph g = utils.DataMgr.readGraphFromhMeTiSFile(graphfile);
      int[] nodemap = new int[g.getNumNodes()];
      for (int i=0; i<nodemap.length; i++) nodemap[i] = i;
      if (mapfile!=null) {
        BufferedReader br = new BufferedReader(new FileReader(mapfile));
        if (br.ready()) {
          int i=0;
          while (true) {
            String line = br.readLine();
            if (line==null) break;
            int orignode = Integer.parseInt(line);
            nodemap[i++] = orignode;
          }
        }
      }
      Set cliques = g.getAllMaximalCliques(minval);
      Iterator iter = cliques.iterator();
      while (iter.hasNext()) {
        Set c = (Set) iter.next();
        double val = evaluate(c, g);
        if (val < minval) continue;  // don't show "light-weight" clique
        Iterator it2 = c.iterator();
        while (it2.hasNext()) {
          Integer id = (Integer) it2.next();
          System.out.print(nodemap[id.intValue()]+" ");
        }
        System.out.println(" val="+val);
      }
      System.out.println("Total number of max. weighted cliques="+cliques.size());
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }

  public static double evaluate(Set c, Graph g) {
    double res = 0;
    Iterator iter = c.iterator();
    while (iter.hasNext()) {
      Integer nid = (Integer) iter.next();
      Node n = g.getNode(nid.intValue());
      Set inarcs = n.getInLinks();
      Iterator itin = inarcs.iterator();
      while (itin.hasNext()) {
        Integer lid = (Integer) itin.next();
        Link l = g.getLink(lid.intValue());
        int inid = l.getStart();
        if (c.contains(new Integer(inid)))
          res += l.getWeight();
      }
      Set outarcs = n.getOutLinks();
      Iterator itout = outarcs.iterator();
      while (itout.hasNext()) {
        Integer lid = (Integer) itout.next();
        Link l = g.getLink(lid.intValue());
        int outid = l.getStart();
        if (c.contains(new Integer(outid)))
          res += l.getWeight();
      }
    }
    return res/2.0;  // divide by 2, as each arc's weight was counted twice
  }



}
