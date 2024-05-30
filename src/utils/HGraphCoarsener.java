package utils;

import coarsening.*;
import java.util.*;

public class HGraphCoarsener {
  public HGraphCoarsener() {
  }

  /**
   * the program accepts an HGraph in hMeTiS format and a properties file
   * and produces a coarse HGraph in hMeTiS format as well. Notice that the
   * coarse graph has weights on the nodes, as they are coarse nodes. The output
   * is written on the file specified as the third argument.
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=3) {
      System.err.println("usage: java -cp <classpath> utils.HGraphCoarsener <inputhgraphfile> <propsfile> <outputcoarsegraphfile>");
      return;
    }
    try {
      final String gfile = args[0];
      final String propsfile = args[1];
      final String cgfile = args[2];
      HGraph g = DataMgr.readHGraphFromhMeTiSFile(gfile);
      Hashtable props = DataMgr.readPropsFromFile(propsfile);
      int max_acc_graph_size = ((Integer) props.get("max_acc_graph_size")).intValue();
      HCoarsener co = new HCoarsenerIEC(g, null, props);
      Vector coarseners = new Vector();  // Vector<Coarsener>, last one is the
      // coarsener used to create last graph
      int num_iters = 0;
      while (g.getNumNodes()>max_acc_graph_size) {
        ++num_iters;
        System.err.println("Entering coarsening level "+num_iters);
        try {
          co.coarsen();
          HGraph coarse_g = co.getCoarseHGraph();
          System.err.println("coarse_g.size="+coarse_g.getNumNodes()+" #arcs="+coarse_g.getNumArcs());
          // Hashtable props = co.getProperties();
          coarseners.addElement(co);

          HCoarsener co1 = co.newInstance(coarse_g, null, props);
          co = co1;
          g = coarse_g;
        }
        catch (CoarsenerException e) {
          // remove this coarsener from the coarseners as it didn't work
          // coarseners.remove(coarseners.size()-1);  // itc: HERE the coarsener didn't enter the vector so it must not be rm'ed
          break;
        }
      }
      // print out coarse graph
      DataMgr.writeWeightedHGraphDirectToHGRFile(g, cgfile);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }
}
