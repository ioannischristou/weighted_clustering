package utils;

import java.util.*;
import java.io.*;
import coarsening.*;
// import java.text.*;

public class ConnLinksTester {
  public ConnLinksTester() {
  }

/*
  public static void main(String[] args) {
    String graphfile = args[0];
    try {
      Graph g = DataMgr.readGraphFromhMeTiSFile(graphfile);
      Set connlinks = g.getAllConnectedLinks(1);
      System.out.print("{");
      Iterator iter = connlinks.iterator();
      while (iter.hasNext()) {
        Set s = (Set) iter.next();
        System.out.print("[");
        Iterator siter = s.iterator();
        boolean c=false;
        while (siter.hasNext()) {
          if (c) System.out.print(",");
          c=true;
          Integer i = (Integer) siter.next();
          Link li = g.getLink(i.intValue());
          System.out.print("("+(li.getStart()+1)+","+(li.getEnd()+1)+")");
        }
        System.out.print("] ");
      }
      System.out.print("}\n");
      System.out.println("Total number of subsets="+connlinks.size());
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
*/

  public static void main(String[] args) {
    String graphfile = args[0];
    try {
      Graph g = DataMgr.readGraphFromhMeTiSFile(graphfile);
      Set connnodes = g.getAllConnectedBy1Nodes(null);
      System.out.print("{");
      Iterator iter = connnodes.iterator();
      while (iter.hasNext()) {
        Set s = (Set) iter.next();
        System.out.print("[");
        Iterator siter = s.iterator();
        boolean c=false;
        while (siter.hasNext()) {
          if (c) System.out.print(",");
          c=true;
          Integer i = (Integer) siter.next();
          System.out.print((i.intValue()+1));
        }
        System.out.print("] ");
      }
      System.out.print("}\n");
      System.out.println("Total number of subsets="+connnodes.size());
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
