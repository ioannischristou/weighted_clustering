package utils;

import coarsening.*;
import partitioning.*;

public class GraphPartitionEvaluator {
  public GraphPartitionEvaluator() {
  }

  public static void main(String[] args) {
    if (args.length!=2) {
      System.err.println("usage: java -cp <classpath> utils.GraphPartitionEvaluator graphfile partfile");
      return;
    }
    final String graphfile = args[0];
    final String partfile = args[1];
    try {
      Graph g = DataMgr.readGraphFromhMeTiSFile(graphfile);
      int numnodes = g.getNumNodes();
      int partition[] = new int[numnodes];
      DataMgr.readPartitionFromHGROutFile(partfile, partition);
      // evaluate partition
      CutEdgesObjFnc f = new CutEdgesObjFnc();
      double partvals[] = f.values(g, partition);
      double val = f.value(g, null, partition);
      System.out.println("partition value="+val);
      System.out.println("sum of weights of emanating edges=");
      for (int i=0; i<partvals.length; i++)
        System.out.print(partvals[i]+" ");
      System.out.println("");
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }
}

