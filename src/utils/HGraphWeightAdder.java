package utils;

import coarsening.*;

public class HGraphWeightAdder {
  public HGraphWeightAdder() {
  }

  /**
   * converts an unweighted hMeTiS hyper-graph to a weighted graph
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=3) {
      System.err.println("usage: java -cp <classpath> utils.HGraphWeightAdder <hgraphfile> <max_weight|-1> <outgraphfile");
      return;
    }
    String gfile = args[0];
    int w = Integer.parseInt(args[1]);
    String ofile = args[2];

    try {
      HGraph g = DataMgr.readHGraphFromhMeTiSFile(gfile);
      for (int i=0; i<g.getNumArcs(); i++) {
        HLink l = g.getHLink(i);
        if (w<=-1) l.setWeight(l.getNumNodes()-1);
        else l.setWeight(RndUtil.getInstance().getRandom().nextInt(w));
      }
      DataMgr.writeHGraphDirectToHGRFile(g, ofile);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }
}
