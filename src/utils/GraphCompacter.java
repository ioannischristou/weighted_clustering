package utils;

import java.util.*;
import java.io.*;
import utils.*;

public class GraphCompacter {
  public GraphCompacter() {
  }

  /**
   * the program removes any unconnected nodes from the graph described in
   * file passed as 1st argument and writes the resultant graph in the file
   * described in the 2nd argument
   * The graph is also stripped from edges with weights less than the third
   * argument (or 1 if no third arg exists).
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<2) {
      System.err.println("usage: java -cp <classpath> utils.GraphCompacter <graphfile> <compactgraphfile> [mapfile] [minweightreq]");
      return;
    }
    try {
      final String graphfile = args[0];
      final String compactgraphfile = args[1];
      PrintWriter pw0 = null;
      if (args.length>2) {
        String mapfile = args[2];
        pw0 = new PrintWriter(new FileWriter(mapfile));
      }
      double maxw = 1.0;
      if (args.length>3) maxw = Double.parseDouble(args[3]);

      // first, count num nodes in the file
      BufferedReader br = new BufferedReader(new FileReader(graphfile));
      Set nodes = new HashSet();
      int maxnodeid = -1;
      int decl_numnodes=0;
      int numarcs=0;
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line);
        numarcs = Integer.parseInt(st.nextToken());
        decl_numnodes = Integer.parseInt(st.nextToken());
        while (true) {
          line = br.readLine();
          if (line==null) break;
          st = new StringTokenizer(line, " ");
          String wstr = st.nextToken();
          double w = Double.parseDouble(wstr);
          if (w<maxw) continue;  // diregard edge and associated nodes
          int start = Integer.parseInt(st.nextToken());
          if (start > maxnodeid) maxnodeid = start;
          nodes.add(new Integer(start));
          int end = Integer.parseInt(st.nextToken());
          if (end > maxnodeid) maxnodeid = end;
          nodes.add(new Integer(end));
        }
      }
      int numnodes = nodes.size();
      br.close();
      // new pass
      int i=1;
      int[] newnodeids = new int[decl_numnodes];
      int[] revnodeids = new int[numnodes];
      Vector newarcs = new Vector();
      for (int j=0; j<decl_numnodes; j++) newnodeids[j]=-1;
      br = new BufferedReader(new FileReader(graphfile));
      if (br.ready()) {
        br.readLine();
        while (true) {
          String line = br.readLine();
          if (line==null) break;
          StringTokenizer st = new StringTokenizer(line, " ");
          int w = Integer.parseInt(st.nextToken());
          if (w<maxw) continue;  // disregard edge
          int s = Integer.parseInt(st.nextToken());
          int e = Integer.parseInt(st.nextToken());
          if (newnodeids[s-1] == -1) {
            newnodeids[s-1] = i;
            revnodeids[i-1] = s;
            ++i;
          }
          if (newnodeids[e-1] == -1) {
            newnodeids[e-1] = i;
            revnodeids[i-1] = e;
            ++i;
          }
          newarcs.addElement(new Arc(newnodeids[s-1], newnodeids[e-1], w));
        }
      }
      br.close();
      PrintWriter pw = new PrintWriter(new FileWriter(compactgraphfile));
      pw.println(newarcs.size()+" "+numnodes+" 1");
      for (i=0; i<newarcs.size(); i++) {
        Arc a = (Arc) newarcs.elementAt(i);
        pw.println(((int) a._w) + " " + a._start + " " + a._end);
      }
      pw.flush();
      pw.close();
      // write the mapfile if it is not null
      if (pw0!=null) {
        for (int j=0; j<revnodeids.length; j++) {
          pw0.println(revnodeids[j]);
        }
        pw0.flush();
        pw0.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }

  }
}

class Arc {
  double _w;
  int _start;
  int _end;
  public Arc(int s, int e, double w) {
    _start = s;
    _end = e;
    _w = w;
  }
}
