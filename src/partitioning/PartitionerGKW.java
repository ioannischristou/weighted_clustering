package partitioning;

import coarsening.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;


public class PartitionerGKW implements Partitioner {
  private double _gain = -1.0;
  private double _devPerc = 0.3;  // default allow 30% deviation in block sizes
  private int[] _blockSizes = null;
  private int _min_block_size, _max_block_size;

  public PartitionerGKW() {
  }


  public void setObjectiveFunction(ObjFnc f) {
    // no-op
  }


  /**
   * returns an array of size g.getNumNodes() with values in [1,...,k]
   * implements the Greedy K-way partitioning scheme (simplification over
   * F-M style algorithms since it visits nodes randomly and makes the best
   * move iff it leads to a gain>0, in terms of weight of cut-edges.)
   * @param g Graph
   * @param k int
   * @param props Hashtable
   * @return int[]
   */
  synchronized public int[] partition(Graph g, int k, Hashtable props) {
    final int num_nodes = g.getNumNodes();
    // try to find an initial partition to work with
    int[] init_partition = (int[]) props.get("initialpartition");
    int[] partition = new int[num_nodes];
    // modify _devPerc if specified in props
    Number dp = (Number) props.get("partitiondevperc");
    if (dp!=null) _devPerc = dp.doubleValue();
    if (init_partition==null) {
      // no, no initial partition
      // create initial partition ensuring no empty block
      Vector v = new Vector();
      int j=0;
      for (int i=0; i<num_nodes; i++) {
        v.addElement(new Integer(++j));
        if (j==k) j=0;
      }
      Collections.shuffle(v);
      for (int i=0; i<num_nodes; i++) {
        partition[i] = ((Integer) v.elementAt(i)).intValue();
      }
    }
    else {  // yes, there is an initial partition
      for (int i=0; i<num_nodes; i++) partition[i] = init_partition[i];
    }

    if (num_nodes==k) {
      // no need for further partitioning
      for (int i=0; i<k; i++)
        partition[i] = i+1;
      return partition;
    }

    if (_devPerc<=0) {
      _min_block_size = 1;
      _max_block_size = g.getNumNodes();
    }
    else {
      _min_block_size = (int) ( (1 - _devPerc) * g.getNumNodes() / (double) k);
      if (_min_block_size == 0) _min_block_size = 1; // set lower limit
      _max_block_size = (int) ( (1 + _devPerc) * g.getNumNodes() / (double) k);
      if (_max_block_size > g.getNumNodes()) _max_block_size = g.getNumNodes();
    }
    _blockSizes = new int[k];
    for (int i=0; i<k; i++) _blockSizes[i]=0;
    for (int i=0; i<num_nodes; i++) _blockSizes[partition[i]-1]++;

    Vector l = new Vector();
    for (int i=0; i<num_nodes; i++)
      l.addElement(new Integer(i));
    boolean cont=true;
    while (cont) {
      cont=false;  // turn into true only if there is an improvement in the loop
      Collections.shuffle(l, utils.RndUtil.getInstance().getRandom());  // get a random permutation
      for (int i=0; i<num_nodes; i++) {
        Integer nId = (Integer) l.elementAt(i);
        Node ni = g.getNode(nId.intValue());
        int best_part = bestPart(g, k, partition, ni, props);
        // method bestPart updates correctly _gain
        if (_gain>0) {
          // do the move and break
          _blockSizes[best_part-1]++;
          _blockSizes[partition[nId.intValue()]-1]--;
          partition[nId.intValue()] = best_part;
          cont = true;
        }
      }
    }
    return partition;
  }


  /**
   * find the best partition that node ni can go to, or -1 if constraints are
   * violated by this move. Also sets the best gain found in _gain.
   * @param g Graph
   * @param k int
   * @param partition int[]
   * @param ni Node
   * @param props Hashtable
   * @return int the partition block to move ni, or -1 else
   */
  private int bestPart(Graph g, int k, int[] partition, Node ni,
                                    Hashtable props) {
    _gain = Double.NEGATIVE_INFINITY;
    final int ni_part = partition[ni.getId()];  // 1...k
    // can we move node ni?
    if (_blockSizes[partition[ni.getId()]-1] <= _min_block_size) return -1;
    double res = 0.0;
    int best = 0;  // best part to move to
    double[] weights = new double[k+1];
    // weights[i] is the weight with which ni connects to partition block i
    // i=1...k
    for (int i=0; i<=k; i++) weights[i] = 0.0;
    Set inarcsnbors = ni.getInLinks();
    Iterator iter = inarcsnbors.iterator();
    while (iter.hasNext()) {
      Integer inlinkid = (Integer) iter.next();
      Link inlink = g.getLink(inlinkid.intValue());
      int nborid = inlink.getStart();
      Node nbor = g.getNode(nborid);
      weights[partition[nbor.getId()]] += inlink.getWeight();
    }
    Set outarcsnbors = ni.getOutLinks();
    iter = outarcsnbors.iterator();
    while (iter.hasNext()) {
      Integer outlinkid = (Integer) iter.next();
      Link outlink = g.getLink(outlinkid.intValue());
      int nborid = outlink.getEnd();
      Node nbor = g.getNode(nborid);
      weights[partition[nbor.getId()]] += outlink.getWeight();
    }

    for (int i=1; i<=k; i++) {
      if (i==ni_part) continue;
      // can we add ni to block i?
      if (_blockSizes[i-1]>=_max_block_size) continue;
      if (_gain < weights[i] - weights[ni_part]) {
        _gain = weights[i] - weights[ni_part];
        best = i;
      }
    }
    return best;
  }
}
