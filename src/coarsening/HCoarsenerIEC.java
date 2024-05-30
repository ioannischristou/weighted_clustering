package coarsening;

import java.util.*;
import clustering.Document;

public class HCoarsenerIEC extends HCoarsener {

  public HCoarsenerIEC(HGraph g, int[] partition, Hashtable props) {
    super(g, partition, props);
  }


  public HCoarsener newInstance(HGraph g, int[] partition, Hashtable properties) {
    return new HCoarsenerIEC(g, partition, properties);
  }


  /**
   * coarsen() performs the following loop until the number of unmatched
   * nodes is less than a ratio of the orinigal nodes:
   * we visit an unvisited node randomly;
   * we match it with the neighbor that yields the maximum value of
   * arcs connecting weight * lamda + (1-lamda)*sum_of_weights_of_arcs_conn_common_nbors
   * we mark as visited both nodes that were matched of course
   * @throws GraphException
   * @throws CoarsenerException if coarsening couldn't proceed satisfactorily
   */
  public void coarsen() throws GraphException, CoarsenerException {
    System.err.println("CoarsenerIEC.coarsen() entered");
    // System.err.println("coarsen(): _g.numnodes="+getOriginalGraph().getNumNodes());
    reset();  // remove old data
    Vector l = new Vector();
    for (int i=0; i<getOriginalHGraph().getNumNodes(); i++)
      l.addElement(new Integer(i));
    Collections.shuffle(l, utils.RndUtil.getInstance().getRandom());  // get a random permutation
    // main loop
    double ratio = ((Double) getProperty("coarsening.HCoarsener.HCoarsenerIEC.ratio")).doubleValue();
    final int min_allowed_nodes = (int) (ratio*_g.getNumNodes());
    int num_seen = 0;
    int new_nodes = 0;
    int j;
    for (j=0; j<l.size(); j++) {
      // check to see if coarsening has exceeded thresholds
      if (new_nodes+_g.getNumNodes()-num_seen<=min_allowed_nodes)
        break;
      Integer pos = (Integer) l.elementAt(j);
      if (_map.get(pos)==null) {  // OK, not mapped yet
        // find best nbor
        HNode best = getBestNbor(pos);
        if (best==null) {
          continue;  // node cannot be matched at this point
                     // e.g. may be isolated
        }
        // System.err.println("merging nodeid="+pos.intValue()+" with nodeid="+best.getId());  // itc: HERE rm asap
        ++num_seen;
        Integer best_id = new Integer(best.getId());
        // match node at pos with best
        // see if best is matched already
        Integer best_new = (Integer) _map.get(best_id);
        if (best_new!=null) {
          // put pos-th node at best_new
          _map.put(pos, best_new);  // map<oldid, newid>
          Set nids = (Set) _rmap.get(best_new);
          nids.add(pos);
        }
        else {
          // create new merger node
          Integer new_pos = new Integer(new_nodes);
          Set new_set = new HashSet();
          new_set.add(pos);
          new_set.add(best_id);
          _map.put(pos, new_pos);
          _map.put(best_id, new_pos);
          _rmap.put(new_pos, new_set);
          ++num_seen;
          ++new_nodes;
        }
      }
    }
    // check if coarsening reached desired level
    if (new_nodes+_g.getNumNodes()-num_seen>min_allowed_nodes) {
      throw new CoarsenerException("CoarsenerIEC.coarsen(): cannot proceed further");
    }
    // System.err.println("_map.size()="+_map.size()+" num_seen="+num_seen+" new_nodes="+new_nodes+" j="+j);
    // OK, go on
    // finally put remaining unmatched nodes as they are in the new graph
    for (int i=0; i<_g.getNumNodes(); i++) {
      Integer pi = new Integer(i);
      if (_map.get(pi)==null) {
        // unmatched, put it in on itself
        Integer npos = new Integer(new_nodes++);
        _map.put(pi, npos);
        Set oset = new HashSet();
        oset.add(pi);
        _rmap.put(npos, oset);
      }
    }

    // and finally create the arcs for the new graph
    // for each old arc, if it connects two new nodes, connect the new nodes.
    // if there's already an arc, add the weight of this one to the new one.
    Hashtable new_arcs_table = new Hashtable();  // map<Integer new_startid, Set<HLinkPair> >
    int new_arcs=0;
    for (int i=0; i<_g.getNumArcs(); i++) {
      HLink ll = _g.getHLink(i);
      Set llnodeids = new HashSet();  // Set<Integer oldId>
      Iterator llniter = ll.getHNodeIds();
      while (llniter.hasNext()) {
        llnodeids.add(llniter.next());
      }
      // create set of the new nodes corresponding to the old nodes
      TreeSet new_llnodeids = new TreeSet();
      Iterator llniter2 = llnodeids.iterator();
      while (llniter2.hasNext()) {
        new_llnodeids.add(_map.get(llniter2.next()));
      }
      if (new_llnodeids.size()==1) continue;  // the arc is hidden
      else {
        HLinkPair hlp = new HLinkPair(new_llnodeids, ll.getWeight());
        Integer nllnid = (Integer) new_llnodeids.first();
        Set hlps = (Set) new_arcs_table.get(nllnid); // Set<HLinkPair>
        if (hlps!=null && hlps.contains(hlp)) {
          // find existing net and increase weight
          Iterator iter = hlps.iterator();
          while (iter.hasNext()) {
            HLinkPair hlp_ex = (HLinkPair) iter.next();
            if (hlp_ex.equals(hlp)) {
              hlp_ex.addWeight(ll.getWeight());
              break;
            }
          }
        }
        else {
          // new arc found
          ++new_arcs;
          if (hlps==null) hlps = new HashSet();
          hlps.add(hlp);
          new_arcs_table.put(nllnid, hlps);
        }
      }
    }

    _coarseG = new HGraph(new_nodes, new_arcs);
    Enumeration new_arcs_table_iter = new_arcs_table.keys();
    while (new_arcs_table_iter.hasMoreElements()) {
      Integer new_start = (Integer) new_arcs_table_iter.nextElement();
      Set linkpairs = (Set) new_arcs_table.get(new_start);
      Iterator lpairs_iter = linkpairs.iterator();
      while (lpairs_iter.hasNext()) {
        HLinkPair lp = (HLinkPair) lpairs_iter.next();
        _coarseG.addHLink(lp.getNodeIds(), lp.getWeight());
      }
    }

    // set the right "cardinality" values for the _coarseG nodes
    for (int i=0; i<_coarseG.getNumNodes(); i++) {
      HNode ni = _coarseG.getHNode(i);
      Set si = (Set) _rmap.get(new Integer(i));
      double nw = 0.0;
      Iterator it = si.iterator();
      while (it.hasNext()) {
        Integer oid = (Integer) it.next();
        Double val = _g.getHNode(oid.intValue()).getWeightValue("cardinality");
        nw += val.doubleValue();
      }
      ni.setWeight("cardinality", new Double(nw));  // full deep value of "cardinality"
      // ni.setWeight("cardinality", new Double(si.size()));  // shallow value of "cardinality"
    }

    // if there is a mapping for each node in _g to a Document, compute
    // the Document that represents the centroid for each of the new
    // coarse nodes and put it into _properties under the name
    // "coarseNodeDocumentArray"
    Document[] node_document_array = (Document[])
        getProperty("coarsening.HCoarsener.HCoarsenerIEC.nodeDocumentArray");
    if (node_document_array != null) {
      Document[] coarse_doc_array = new Document[new_nodes];
      for (int i=0; i<new_nodes; i++) {
        Vector c_i = new Vector();  // Vector<Document>
        Set ri = (Set) _rmap.get(new Integer(i));
        Iterator riter = ri.iterator();
        while (riter.hasNext()) {
          Integer rin = (Integer) riter.next();
          Document drin = node_document_array[rin.intValue()];
          c_i.addElement(drin);
        }
        try {
          Document c_i_new = Document.getCenter(c_i, null);  // itc-20220223: HERE
          coarse_doc_array[i] = c_i_new;
        }
        catch (clustering.ClustererException ce) {
          ce.printStackTrace();
          throw new CoarsenerException("failed to create coarseNodeDocumentArray");
        }
      }
      setProperty("coarsening.HCoarsener.HCoarsenerIEC.coarseNodeDocumentArray", coarse_doc_array);
    }
    // else System.err.println("coarsen(): nodeDocumentArray was null or not found...");

    if (_graphPartition!=null) {
      // finally create a coarse_graph_partition array and put it into the
      // _properties under the name "coarsePartition".
      int[] coarse_graph_partition = new int[_coarseG.getNumNodes()];
      for (int i=0; i<coarse_graph_partition.length-1; i++) {
        Set xi = (Set) _rmap.get(new Integer(i));
        Integer xi_first_id = (Integer) xi.iterator().next();
        coarse_graph_partition[i] = _graphPartition[xi_first_id.intValue()];
      }
      setProperty("coarsening.HCoarsener.HCoarsenerIEC.coarsePartition", coarse_graph_partition);
    }

    // System.err.println("coarsen(): _coarseG.numnodes="+getCoarseGraph().getNumNodes());
    System.err.println("CoarsenerIEC.coarsen() finished");
  }


  /**
   * find the best neighbor that should be matched with the node at position pos.
   * The best neighbor is the one maximizing the value
   * sum_{f(conn_arcs_weight)} as implemented in getCommonWeight()
   * @param pos Integer
   * @return Node
   */
  private HNode getBestNbor(Integer pos) {
    HNode node = _g.getHNode(pos.intValue());
    double nodewgt = node.getWeightValue("cardinality").doubleValue();
    if (nodewgt>((Double)getProperty("coarsening.HCoarsener.HCoarsenerIEC.max_allowed_card")).doubleValue())
      return null;  // node is already over-weight
    int node_part = -1;
    if (_graphPartition != null)
      node_part = _graphPartition[pos.intValue()];
    HNode bestnode = null;
    double bestweight = Double.NEGATIVE_INFINITY;
    Set nbors = node.getNbors(_g);  // Set<HNode>
    Iterator iter = nbors.iterator();
    while (iter.hasNext()) {
      HNode en = (HNode) iter.next();
      if (_graphPartition!=null && _graphPartition[en.getId()]!=node_part)
        continue;  // respect partition
      // respect weights
      double nw = 0.0;
      Integer nenid = (Integer) _map.get(new Integer(en.getId()));
      Set si2=null;
      if (nenid!=null) {
        si2 = (Set) _rmap.get(nenid);
        Iterator it = si2.iterator();
        while (it.hasNext()) {
          Integer oid = (Integer) it.next();
          Double val = _g.getHNode(oid.intValue()).getWeightValue("cardinality");
          nw += val.doubleValue();
        }
      }
      double nodewgt2 = 0.0;
      Integer nid = (Integer) _map.get(new Integer(node.getId()));
      if (nid!=null) {
        Set si = (Set) _rmap.get(nid);
        Iterator it = si.iterator();
        while (it.hasNext()) {
          Integer oid = (Integer) it.next();
          if (si2!=null && si2.contains(oid)) continue;  // don't count oid's weight twice
          Double val = _g.getHNode(oid.intValue()).getWeightValue("cardinality");
          nodewgt2 += val.doubleValue();
        }
      }
      else nodewgt2 = nodewgt;
      // done computing weights
      if (nw+nodewgt2 >
          ((Double)getProperty("coarsening.HCoarsener.HCoarsenerIEC.max_allowed_card")).doubleValue())
        continue;  // node over-weight
      double tot_weight = getCommonWeight(node, en);
      if (tot_weight > bestweight) {
        bestweight = tot_weight;
        bestnode = en;
      }
    }
    return bestnode;
  }


  private double getCommonWeight(HNode n1, HNode n2) {
    double res = 0;
    Set links1 = new HashSet(n1.getHLinkIds());
    links1.retainAll(n2.getHLinkIds());
    Iterator iter = links1.iterator();
    while (iter.hasNext()) {
      Integer lid = (Integer) iter.next();
      HLink l = _g.getHLink(lid.intValue());
      // res += l.getWeight()/((double) l.getNumNodes()-1);
      res += l.getWeight()*l.getWeight();  // w^2
    }
    return res;
  }


  public String toString() {
    Hashtable props = getProperties();
    String ret = "props=";
    if (props==null) ret+="null";
    else ret += props.size();
    return ret;
  }
}

