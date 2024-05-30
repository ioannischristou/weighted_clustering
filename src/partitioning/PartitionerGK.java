package partitioning;

import coarsening.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;


public class PartitionerGK implements Partitioner {
  private double _gain = -1.0;
  private double _devPerc = 0.3;  // default allow 30% deviation in block sizes
  private int[] _blockSizes = null;
  private int _min_block_size, _max_block_size;

  public PartitionerGK() {
  }


  public void setObjectiveFunction(ObjFnc f) {
    // no-op
  }


  /**
   * returns an array of size g.getNumNodes() with values in [1,...,k]
   * implements the Greedy K-way partitioning scheme (simplification over
   * F-M style algorithms since it visits nodes randomly and makes the best
   * move iff it leads to a gain>0, in terms of cut-edges.)
   * @param g Graph
   * @param k int
   * @param props Hashtable
   * @return int[]
   */
  synchronized public int[] partition(Graph g, int k, Hashtable props) {
    // try to find an initial partition to work with
    int[] partition = (int[]) props.get("initialpartition");
    final int num_nodes = g.getNumNodes();
    if (partition==null) {
      partition = new int[num_nodes];  // no, no initial partition
      /*
      // random initialization
      for (int i=0; i<num_nodes; i++) {
        partition[i] = (int) Math.ceil(Math.random()*k);
      }
      */
      Vector v = new Vector();
/*
      int msz = (int) Math.round(num_nodes/(double) k);
      System.err.println("num_nodes="+num_nodes+" k="+k+" msz="+msz);
      int j=1;
      for (int i=0; i<num_nodes; i++) {
        v.addElement(new Integer(j));
        if (i%msz==0 && j<k) j++;
      }
      // make sure at least one node per partition
      if (((Integer) v.elementAt(num_nodes-1)).intValue()<k)
        v.setElementAt(new Integer(k), num_nodes-1);
*/
      int j=0;
      for (int i=0; i<num_nodes; i++) {
        v.addElement(new Integer(++j));
        if (j==k) j=0;
      }  // replaces above commented out code
      Collections.shuffle(v);
      for (int i=0; i<num_nodes; i++) {
        partition[i] = ((Integer) v.elementAt(i)).intValue();
      }
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
      _min_block_size = (int) ((1-_devPerc)*g.getNumNodes()/(double) k);
      if (_min_block_size==0) _min_block_size = 1;  // set lower limit
      _max_block_size = (int) ((1+_devPerc)*g.getNumNodes()/(double) k);
      if (_max_block_size>g.getNumNodes()) _max_block_size = g.getNumNodes();
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
          // do the move
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
    int[] nums = new int[k+1];
    // nums[i] is the number of neighbors of ni that belong to partition block i
    // i=1...k
    for (int i=0; i<=k; i++) nums[i] = 0;
    Set nbors = ni.getNbors();
    Iterator iter = nbors.iterator();
    while (iter.hasNext()) {
      Node nbor = (Node) iter.next();
      nums[partition[nbor.getId()]]++;
    }
    for (int i=1; i<=k; i++) {
      if (i==ni_part) continue;
      // can we add ni to block i?
      if (_blockSizes[i-1]>=_max_block_size) continue;
      if (_gain < nums[i] - nums[ni_part]) {
        _gain = nums[i] - nums[ni_part];
        best = i;
      }
    }
    return best;
  }
}
