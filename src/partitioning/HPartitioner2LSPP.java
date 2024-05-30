package partitioning;

import coarsening.*;
import utils.DataMgr;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.*;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * this class implements a 2-level (high-level) partitioning scheme:
 * First, the graph is partitioned among a small number of components
 * (using hMeTiS or any other partitioner), and each component is
 * then partitioned among a corresponding number of components.
 * Finally, the low-level partitions form the partition of the entire graph
 * which is refined using a partitioner such as HPartitionerGKW.
 * The main reasoning behind the two-level approach is to reduce memory
 * and speed requirements; if HPartitionerMLSPP worked better than HMeTiS
 * there would not be much need for it -except for memory issues.
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HPartitioner2LSPP implements HPartitioner {
  private final double _eps = 1.e-6;
  private HObjFncIntf _obj=null;

  public HPartitioner2LSPP() {
  }


  public void setObjectiveFunction(HObjFncIntf f) {
    _obj = f;
  }


  public int[] partition(HGraph g, int k, Hashtable props) throws PartitioningException {
    int k1 = ((Integer) props.get("partitioning.HPartitioner.HPartitioner2LSPP.k1")).intValue();
    // 1. partition at high-level, among k1 components the graph g
    HPartitioner parter1 = (HPartitioner) props.get("partitioning.HPartitioner.HPartitioner2LSPP.Parter1");
    int[] partition1 = parter1.partition(g, k1, props);
    // 2. create the k1 graphs and partition them among k/k1 components
    try {
      HGraph[] graphs = getHGraphs(g, k1, partition1);
      HPartitioner parter2 = (HPartitioner) props.get(
          "partitioning.HPartitioner.HPartitioner2LSPP.Parter2");
      int k2 = k / k1;
      int[] partition = new int[g.getNumNodes()];
      int pcount = 0; // partition number counter
      for (int i = 0; i < k1; i++) {
        HGraph gi = graphs[i];
        int[] parti = parter2.partition(gi, k2, props);
        int gi_nodes = gi.getNumNodes();
        for (int j = 0; j < gi_nodes; j++) {
          Integer origidj = (Integer) gi.getHNodeLabel(j);
          partition[origidj.intValue()] = parti[j] + pcount;
        }
        pcount += k2;
      }
      // 3. improve the partition with another partitioner
      HPartitioner parter3 = (HPartitioner) props.get(
          "partitioning.HPartitioner.HPartitioner2LSPP.Parter3");
      props.put("initialpartition", partition);
      int[] finalpartition = parter3.partition(g, k, props);
      return finalpartition;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("partition(): failed");
    }
  }


  /**
   * create the HGraph objects that comprise the sub-graphs induced by the
   * partition given as the third argument. Each sub-graph's nodes will have
   * the labels that correspond to the node in the original graph g passed as
   * the first argument. The parameter k1 is auxiliary (could be computed, but
   * it saves time to have it handy).
   * @param g HGraph
   * @param k1 int
   * @param partition int[]
   * @return HGraph[]
   */
  private HGraph[] getHGraphs(HGraph g, int k1, int[] partition) throws GraphException {
    final int num_nodes = g.getNumNodes();
    final int num_arcs = g.getNumArcs();
    HGraph[] graphs = new HGraph[k1];
    Set[] nodesi = new TreeSet[k1];  // nodesi[i] are the Set<Integer nid> of nodes
                                     // belonging in partition block i.
    Set[] arcsi = new HashSet[k1];  // acrsi[i] are the Set<Integer lid> of arcs
                                    // contained completely in partition block i
    for (int i=0; i<k1; i++) {
      nodesi[i] = new TreeSet();
      arcsi[i] = new HashSet();
    }
    for (int i=0; i<num_nodes; i++) {
      nodesi[partition[i]-1].add(new Integer(i));
    }
    for (int i=0; i<num_arcs; i++) {
      HLink li = g.getHLink(i);
      for (int j=0; j<k1; j++) {
        if (nodesi[j].containsAll(li.getHNodeIdsAsSet())) arcsi[j].add(new Integer(i));
      }
    }
    // now create each graph separately
    for (int i=0; i<k1; i++) {
      graphs[i] = new HGraph(nodesi[i].size(), arcsi[i].size());
      // first add the right label for each node
      HGraph gi = graphs[i];
      Iterator niter = nodesi[i].iterator();
      for (int j=0; j<gi.getNumNodes(); j++) {
        gi.setHNodeLabel(j, niter.next());
      }
      // set cardinality values for gi
      for (int j = 0; j < gi.getNumNodes(); j++) {
        HNode nj = gi.getHNode(j);
        nj.setWeight("cardinality", new Double(1.0));
      }
      // now add the arcs
      Iterator aiter = arcsi[i].iterator();
      for (int j=0; j<gi.getNumArcs(); j++) {
        Integer lid = (Integer) aiter.next();
        Iterator lnodesiter = g.getHLink(lid.intValue()).getHNodeIds();
        Set newlnodes = new HashSet();
        while (lnodesiter.hasNext()) {
          Integer oid = (Integer) lnodesiter.next();
          int nid = gi.getHNodeIdByLabel(oid);
          newlnodes.add(new Integer(nid));
        }
        gi.addHLink(newlnodes, g.getHLink(lid.intValue()).getWeight());
      }
    }
    return graphs;
  }

}
