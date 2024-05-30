package packing;

import java.util.*;
import utils.*;
import coarsening.*;

public class EPTester {
  public EPTester() {
  }


  /**
   * args[0]: graph file name
   * args[1]: params file name
   * args[2]: [optional] max num nodes to create in B&B Clustering
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      final int n = g.getNumNodes();
      Hashtable params = DataMgr.readPropsFromFile(args[1]);
      // first run a heuristic to get a good lower bound on the opt. solution
      GRASPPacker gp = new GRASPPacker(g);
      int num_tries = 10;
      Integer ntI = (Integer) params.get("grasp.num_tries");
      if (ntI!=null) num_tries = ntI.intValue();
      int num_combs = 50;
      Integer ncI = (Integer) params.get("grasp.num_combs");
      if (ncI!=null) num_combs = ncI.intValue();
      Set bestgp = gp.packAndRelink(num_tries, num_combs);
      System.err.println("GRASP heuristic found a solution w/ value = "+bestgp.size());
      int maxnodes = -1;
      if (args.length>2)
        maxnodes = Integer.parseInt(args[2]);

      // do LP bounding?
      RelaxedNodePacker rnp = null;
      Integer steps2LP = (Integer) params.get("lp.steps2lp");
      Integer spa = (Integer) params.get("lp.setsperarc");
      if (steps2LP!=null)
        rnp = new RelaxedNodePacker(g, spa);

      // now run the B&B algorithm
      int nt = 1;
      Integer num_threadsI = (Integer) params.get("numthreads");
      if (num_threadsI!=null)
        nt = num_threadsI.intValue();
      if (nt>1) BBThreadPool.setNumthreads(nt);
      int initsoln[] = null;

      BBTree t = null;
      // double bound = Double.MAX_VALUE;
      double bound = g.getNumNodes()-bestgp.size();
      if (steps2LP!=null) t = new BBTree(g, initsoln, bound, rnp, steps2LP.intValue());
      else t = new BBTree(g, initsoln, bound);
      t.setSolution(bestgp);
      if (maxnodes>0) t.setMaxNodesAllowed(maxnodes);
      Integer num_nodesI = (Integer) params.get("numnodes");
      if (num_nodesI!=null) t.setJoinNumNodes(num_nodesI.intValue());
      Integer tightenboundI = (Integer) params.get("tightenbound");
      if (tightenboundI!=null && tightenboundI.intValue()>0)
        t.setTightenBoundLvl(tightenboundI.intValue());
      Integer maxQSizeI = (Integer) params.get("maxqsize");
      if (maxQSizeI!=null && maxQSizeI.intValue()>0)
        t.setMaxQSize(maxQSizeI.intValue());
      Boolean cutNodesB = (Boolean) params.get("cutnodes");
      if (cutNodesB!=null) t.setCutNodes(cutNodesB.booleanValue());
      t.run();
/*
      // save reconstructed best soln in args[0]_asgns.txt
      int finalsoln[] = new int[n];
      int orsoln[] = t.getSolution();
      for (int i=0; i<n; i++) {
        // orsoln[i] is the cluster index of the document originally found in position
        finalsoln[rearrange_map[i]] = orsoln[i];
      }
      StringBuffer soln_fileb = new StringBuffer(args[0]);
      soln_fileb.append("_asgns.txt");
      String soln_file = soln_fileb.toString();
      DataMgr.writeLabelsToFile(finalsoln, soln_file);
*/
      long time = (System.currentTimeMillis()-start)/1000;
      int orsoln[] = t.getSolution();
      for (int i=0; i<orsoln.length; i++) {
        if (orsoln[i]==1) System.out.print((i+1)+" ");
      }
      System.err.println("Total nodes="+t.getCounter());
      /*
      System.err.println("Pruned Nodes by level: ");
      int[] pruned = t.getPrunedNodes();
      for (int i=0; i<g.getNumNodes(); i++) {
        System.err.print(pruned[i]+"\t");
        if (i % 10 == 0) System.err.println("");
      }
      */
      System.out.println("\nWall-clock Time (secs): "+time);

      System.out.println("Done.");
      System.exit(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

}

/*
class PairComp implements java.io.Serializable, Comparator {
  public int compare(Object pair1, Object pair2) {
    utils.Pair p1 = (utils.Pair) pair1;
    utils.Pair p2 = (utils.Pair) pair2;
    Integer cind1 = (Integer) p1.getFirst();
    Integer cind2 = (Integer) p2.getFirst();
    if (cind1.intValue()<cind2.intValue()) return -1;
    else if (cind1.intValue()==cind2.intValue()) return 0;
    else return 1;
  }
}
*/
