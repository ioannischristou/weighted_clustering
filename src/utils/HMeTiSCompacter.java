package utils;

import java.util.*;
import java.io.*;
// import java.text.*;

public class HMeTiSCompacter {
  public HMeTiSCompacter() {
  }

  /**
   * the program removes the unconnected nodes from a partition file of a graph
   * that contains such nodes and writes the result in a new file so that
   * comparisons of the clusterings can take place between different graphs
   * partitioned that contained the same nodes.
   * @param args String[]
   */
  public static void main(String[] args) {
    String graphfile = args[0];
    String partitionfile = args[1];
    String compactpartfile = args[2];
    try {
      // read nodes in graphfile
      BufferedReader br = new BufferedReader(new FileReader(graphfile));
      Set usednodes = new HashSet();
      if (br.ready()) {
        String line = br.readLine();  // ignore this line
        while (true) {
          line = br.readLine();
          if (line==null) break;
          StringTokenizer st = new StringTokenizer(line, " ");
          String weight = st.nextToken();
          String start = st.nextToken();
          usednodes.add(new Integer(Integer.parseInt(start)));
          String end = st.nextToken();
          usednodes.add(new Integer(Integer.parseInt(end)));
        }
      }
      // read partition numbers from graph file and add to compactpartfile
      // iff the number of the line is in the usednodes set
      BufferedReader brp = new BufferedReader(new FileReader(partitionfile));
      PrintWriter pwc = new PrintWriter(new FileWriter(compactpartfile));
      if (brp.ready()) {
        int i=1;
        while (true) {
          String line = brp.readLine();
          if (line==null) break;
          int pnum = Integer.parseInt(line);
          if (usednodes.contains(new Integer(i)))
            pwc.println(pnum);
          i++;
        }
        pwc.flush();
        pwc.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
}
