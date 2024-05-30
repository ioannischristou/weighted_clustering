package partitioning;

import coarsening.*;
import java.util.*;

public class HObjFncSOED implements HObjFncIntf {
  public HObjFncSOED() {
  }


  /**
   * returns the SOED of the entire partition
   * @param g HGraph
   * @param partition int[]
   * @return double
   */
  public double value(HGraph g, int[] partition) {
    double val = 0.0;
    double vals[] = values(g, partition);
    for (int i=0; i<vals.length; i++)
      val += vals[i];
    return val;
  }


  /**
   * returns the SOED of each partition
   * @param g HGraph
   * @param partition int[]
   * @return double[]
   */
  public double[] values(HGraph g, int[] partition) {
    final int n = g.getNumNodes();
    final int m = g.getNumArcs();

    TreeMap parts = new TreeMap();  // map<Integer partid, Set<Integer nodeid> >
    for (int i=0; i<n; i++) {
      Integer pId = new Integer(partition[i]);
      Set pnodes = (Set) parts.get(pId);
      if (pnodes==null) {
        pnodes = new TreeSet();
      }
      pnodes.add(new Integer(i));
      parts.put(pId, pnodes);
    }
    double[] vals = new double[parts.size()];
    boolean[] marked = new boolean[parts.size()];
    for (int i=0; i<vals.length; i++) vals[i]=0.0;
    for (int j=0; j<m; j++) {
      HLink lj = g.getHLink(j);
      if (isCut(lj, g, partition)) {
        for (int i=0; i<marked.length; i++) marked[i]=false;  // reset marks
        double wj = lj.getWeight();
        Iterator njit = lj.getHNodeIds();
        while (njit.hasNext()) {
          Integer nId = (Integer) njit.next();
          if (marked[partition[nId.intValue()]-1]) continue;
          vals[partition[nId.intValue()]-1]+=wj;
          marked[partition[nId.intValue()]-1]=true;
        }
      }
    }
    return vals;
  }


  private boolean isCut(HLink l, HGraph g, int[] partition) {
    Iterator it = l.getHNodeIds();
    int fp = partition[((Integer) it.next()).intValue()];
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
      if (partition[nid.intValue()]!=fp) return true;
    }
    return false;
  }
}
