package utils;

import java.util.*;
import java.io.*;
// import java.text.*;

public class GraphTester {
  public GraphTester() {
  }

  public static void main(String[] args) {
    String graphfile = args[0];

    // first ensure number of nodes read in graphfile are equal to the number
    // of nodes given in first line of file
    try {
      BufferedReader br = new BufferedReader(new FileReader(graphfile));
      Set nodes = new HashSet();
      int maxnodeid = -1;
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line);
        st.nextToken();
        int numnodes = Integer.parseInt(st.nextToken());
        System.out.println("read numnodes="+numnodes);
        while (true) {
          line = br.readLine();
          if (line==null) break;
          st = new StringTokenizer(line, " ");
          st.nextToken();
          int start = Integer.parseInt(st.nextToken());
          if (start > maxnodeid) maxnodeid = start;
          nodes.add(new Integer(start));
          int end = Integer.parseInt(st.nextToken());
          if (end > maxnodeid) maxnodeid = end;
          nodes.add(new Integer(end));
        }
      }
      System.out.println("real total number of nodes="+nodes.size());
      System.out.println("max node id="+maxnodeid);
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
}
