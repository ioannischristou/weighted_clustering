package utils;

import java.io.*;
import java.util.*;

public class GraphPartitionTester {
  public GraphPartitionTester() {
  }

  /**
   * compare the partition of a compacted graph against the "true" partition
   * of that graph via the Adjusted Rand Index criterion
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=4) {
      System.err.println("usage: java -cp <classpath> utils.GraphPartitionTester totnumnodes <mapfile> <partitionfile> <truepartitionfile>");
      return;
    }
    try {
      final int totnumnodes = Integer.parseInt(args[0]);
      // read the map used in the partition file
      final String mapfile = args[1];
      final String partfile = args[2];
      int[] map = new int[totnumnodes];  // init to zero
      int[] revmap = new int[totnumnodes];  // gets zeros everywhere
      // revmap[i-1] is the node id in the new graph cgraph.txt of the node with
      // id i in the original graph
      BufferedReader br1 = new BufferedReader(new FileReader(mapfile));
      int num_newnodes = 0;
      if (br1.ready()) {
        int i=1;
        while (true) {
          String nodeid = br1.readLine();
          if (nodeid==null) break;
          num_newnodes++;
          int nid = Integer.parseInt(nodeid);
          map[i-1] = nid;  // map[i-1] is the node id in the original graph of the
          // node with id i in the new cgraph.txt
          revmap[nid-1] = i++;
        }
      }
      br1.close();
      // read the truepartitionfile and convert into partitionfile for new node ids
      final String truepartitionfile = args[3];
      String tmpfile = "tmp.txt";
      String[] truepartitions = new String[totnumnodes];
      BufferedReader br2 = new BufferedReader(new FileReader(truepartitionfile));
      if (br2.ready()) {
        int old_id=0;
        while (true) {
          String line = br2.readLine();
          if (line==null) break;
          truepartitions[old_id++]=line;
        }
      }
      br2.close();
      // write the true partition in terms of the new ids
      PrintWriter pw = new PrintWriter(new FileWriter(tmpfile));
      for (int i=1; i<=num_newnodes; i++) {
        int orig_id = map[i];
        if (orig_id==0) continue;
        System.err.println("will write partition label for orig node id="+orig_id+" for new node id="+i);
        pw.println(truepartitions[orig_id-1]);
      }
      pw.flush();
      pw.close();

      // finally, compare the two partitions in the new ids
      String [] files = {tmpfile, partfile};
      utils.AdjRandIndexSolnComparator.compare(files);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }
}
