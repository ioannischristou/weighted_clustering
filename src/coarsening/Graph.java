package coarsening;

import java.util.*;
import utils.IntSet;


public class Graph {
  private Node[] _nodes;
  private Link[] _arcs;
  private Object[] _nodeLabels;  // if not null, it contains label for each
                                 // node, which may possible be the node's true
                                 // id in a larger graph, of which this node
                                 // is a sub-graph, and can be used to get the
                                 // support of this graph in other graphs.
  private Hashtable _labelMap;  // map<Object label, Integer nodeid>
  private int _addLinkPos = 0;  // pointer to the last arc set by addLink()
  private int _comps[]=null;  // the array describes the maximal connected
                              // components of this Graph.
                              // _comps.length = _nodes.length
  private Vector _compCards;  // holds each component's cardinality
  private int _compindex=-1;  // holds number of max. connected components of
                              // Graph


  public Graph(int numnodes, int numarcs) {
    _nodes = new Node[numnodes];
    _arcs = new Link[numarcs];
    _nodeLabels = null;
    _labelMap = null;

    for (int i=0; i<numnodes; i++) _nodes[i] = new Node(i);

    for (int i=0; i<numarcs; i++) _arcs[i] = null;
  }


  public Graph(int numnodes, int numarcs, Object[] labels) {
    this(numnodes, numarcs);
    _nodeLabels = labels;
    _labelMap = new Hashtable();
    for (int i=0; i<numnodes; i++)
      _labelMap.put(_nodeLabels[i], new Integer(i));
  }


  /**
   * copy ctor: perform deep copies: each node and graph is copied to new copies
   * so that operations on the new graph or its nodes does not affect the original
   * graph. If the graph's nodes have labels however, being Object instances of
   * unknown class, the new Graph's labels themselves are just refs to the
   * original Graph's labels.
   * @param g Graph
   */
  public Graph(Graph g) {
    // 0. initialization
    _nodeLabels = null;
    _labelMap = null;
    _arcs = new Link[g.getNumArcs()];
    final int numnodes = g.getNumNodes();
    _nodes = new Node[numnodes];
    // 1. copy arcs and make nodes in the process
    for (int i=0; i<numnodes; i++) _nodes[i] = new Node(i);
    for (int i=0; i<_arcs.length; i++) {
      Link ai = g.getLink(i);
      try {
        addLink(ai.getStart(), ai.getEnd(), ai.getWeight());
      }
      catch (GraphException e) {
        e.printStackTrace();  // cannot occur
      }
    }
    // 2. finally, set the _nodeLabels and _labelMap if not null
    if (g._nodeLabels!=null) {
      _nodeLabels = new Object[numnodes];
      for (int i=0; i<numnodes; i++) {
        setNodeLabel(i, g._nodeLabels[i]);
      }
    }
  }


  public void addLink(int starta, int enda, double weight) throws GraphException {
    if (_addLinkPos>=_arcs.length) throw new GraphException("cannot add more arcs.");
    _arcs[_addLinkPos] = new Link(this, _addLinkPos++, starta, enda, weight);
  }


  public Link getLink(int id) {
    return _arcs[id];
  }


  public Node getNode(int id) {
    return _nodes[id];
  }


  public int getNumNodes() { return _nodes.length; }


  public int getNumArcs() { return _arcs.length; }


  public Object getNodeLabel(int i) {
    if (_nodeLabels==null) return null;
    return _nodeLabels[i];
  }


  public int getNodeIdByLabel(Object label) throws GraphException {
    Integer nid = (Integer) _labelMap.get(label);
    if (nid==null) throw new GraphException("no such label");
    return nid.intValue();
  }


  public void setNodeLabel(int i, Object o) {
    if (_nodeLabels==null) _nodeLabels = new Object[_nodes.length];
    if (_nodeLabels[i]!=null) _labelMap.remove(_nodeLabels[i]);
    _nodeLabels[i] = o;
    _labelMap.put(o, new Integer(i));
  }


  public int getNumComponents() {
    if (_compindex==-1) getComponents();
    return _compindex;
  }


  /**
   * return all maximal connected components of this Graph.
   * The array that is returned is of length _nodes.length
   * and the i-th element has a value from [0...#Components-1]
   * indicating to which component the node belongs.
   * @return int[]
   */
  public int[] getComponents() {
    if (_comps!=null) return _comps;
    // use dipth-first strategy
    _comps = new int[_nodes.length];
    _compCards = new Vector();
    final int numnodes= _nodes.length;
    for (int i=0; i<numnodes; i++) _comps[i] = -1;  // unassigned
    _compindex=0;
    for (int i=0; i<numnodes; i++) {
      Set nbors = _nodes[i].getNbors();
      if (_comps[i]==-1) {
        // new component
        int numcomps = labelComponent(i, _compindex);
        _compCards.addElement(new Integer(numcomps));
        _compindex++;
      }
    }
    return _comps;
  }


  /**
   * return the number of nodes in max. connected component i
   * i is in the range [0...#comps-1]
   * @param i int
   * @return int
   */
  public int getComponentCard(int i) {
    if (_comps==null) getComponents();
    Integer card = (Integer) _compCards.elementAt(i);
    return card.intValue();
  }


  /**
   * construct and return the dual of this graph. This is accomplished by
   * creating a node in the dual for each arc in this graph, and connecting
   * nodes in the dual that correspond to neighboring arcs in the original.
   * @return Graph
   * @throws GraphException
   */
  public Graph getDual() throws GraphException {
    // 1. compute numarcs of the dual graph
    int numarcs = 0;
    for (int i=0; i<_arcs.length; i++) {
      Link li = _arcs[i];
      Node s = _nodes[li.getStart()];
      Node e = _nodes[li.getEnd()];
      numarcs += s.getNbors().size()-1;
      numarcs += e.getNbors().size()-1;
    }
    numarcs /= 2;
    Graph dg = new Graph(_arcs.length, numarcs);
    int count=0;
    for (int i=0; i<_nodes.length; i++) {
      Node ni = _nodes[i];
      Set ni_links = new HashSet(ni.getOutLinks());
      ni_links.addAll(ni.getInLinks());
      Iterator it = ni_links.iterator();
      while (it.hasNext()) {
        int lid = ((Integer) it.next()).intValue();
        // all other links lj to node ni, with lj_id > lid form a link in the
        // dual graph
        Iterator it2 = ni_links.iterator();
        while (it2.hasNext()) {
          int ljd = ((Integer) it2.next()).intValue();
          if (ljd>lid) {
            // add link (lid,ljd)
            dg.addLink(lid,ljd,1);
            ++count;
          }
        }
      }
    }
    if (count!=numarcs) {
      throw new GraphException("error counting arcs: numarcs should be "+numarcs+" but has count "+count);
    }
    return dg;
  }


  /**
   * the method computes all maximal cliques in a Graph g so that each node in a
   * resulting clique contributes to the clique with arc weights higher than
   * the specified argument; therefore, it has exponential time and space
   * complexity... Use only on small graphs.
   * The higher the argument, the less cliques it searches for, and the more
   * the computational savings.
   * The method is iterative: for each node, it constructs all the max.
   * cliques the node participates in.
   * Duplicates are removed as they appear.
   * It returns the cliques in a Set<Set<NodeId> >
   * @return Set a Set<Set<Integer nodeId> >
   */
  public Set getAllMaximalCliques(double minaccnodecliqueweight) {
    Set max_cliques = new HashSet();
    double[] nodeweights = new double[_nodes.length];
    for (int i=0; i<_nodes.length; i++) {
      nodeweights[i] = _nodes[i].getArcWeights(this);
    }
    for (int i=0; i<_nodes.length; i++) {
      if (nodeweights[i]<minaccnodecliqueweight) continue;
      Set maxcliquesi = new HashSet();  // Set<Set<Integer id> >
      Set candi = new TreeSet();  // each clique is represented by a TreeSet.
      candi.add(new Integer(i));
      maxcliquesi.add(candi);
      Set newcliquesi = new HashSet();  // Set<Set<Integer id> >
      while (expandCliquesBy1(maxcliquesi,newcliquesi, minaccnodecliqueweight)) {
        maxcliquesi = new HashSet(newcliquesi);
        newcliquesi = new HashSet();
      }
      // now put in max_cliques those members of maxcliquesi that are not already
      // in.
      addUnique(maxcliquesi, max_cliques);
    }
    return max_cliques;
  }


  /**
   * get the support of a clique of this node in the other Graph g, in terms of
   * their labels. The method compares how many arcs in the clique also appear
   * in the Graph g, when the labels of the endpoints of the two nodes are used.
   * @param nodes Set<Integer node_id>
   * @param g Graph
   * @return double the percentage of arcs from the clique that are also present
   * in the Graph g
   * @throws GraphException
   */
  public double getCliqueLabelSupport(Set nodes, Graph g) throws GraphException {
    if (nodes==null || nodes.size()==0) throw new GraphException("null of empty clique");
    if (_nodeLabels==null || g._nodeLabels==null)
      throw new GraphException("no node labels in one Graph");
    double res = 0;
    Iterator iter = nodes.iterator();
    while (iter.hasNext()) {
      Integer nid = (Integer) iter.next();
      Node n = _nodes[nid.intValue()];
      Iterator it2 = n.getNbors().iterator();
      while (it2.hasNext()) {
        Node nnbor = (Node) it2.next();
        Integer nnborid = new Integer(nnbor.getId());
        if (nnborid.intValue()>nid.intValue() && nodes.contains(nnborid)) {
          // look for this arc in the Graph g
          Object l1 = _nodeLabels[nid.intValue()];
          Object l2 = _nodeLabels[nnborid.intValue()];
          try {
            int othernid = g.getNodeIdByLabel(l1);
            int othernnborid = g.getNodeIdByLabel(l2);
            Node othernode = g.getNode(othernid);
            Node othernnbor = g.getNode(othernnborid);
            if (othernode.getNbors().contains(othernnbor))
              res += 1;
          }
          catch (GraphException e) {
            // silent continue
          }
        }
      }
    }
    double narcs = nodes.size()*(nodes.size()-1)/2.0;
    return (res / narcs);
  }


  /**
   * computes *all* maximal subsets of arcs in this Graph with the property
   * that any two arcs in a set s that is returned are at a distance one from
   * each other. (For k=1 this should be fast enough method)
   * @param k int the max. number of hops away any two arcs are allowed to be.
   * @throws GraphException
   * @return Set Set<Set<Integer linkid> >
   */
  public Set getAllConnectedLinks(int k) throws GraphException {
    if (k!=1) throw new GraphException("currently, input k must be 1");
    Vector sets=new Vector();
    for (int i=0; i<_arcs.length; i++) {
      Set conn1links = getFullLinkNbors(i);
      sets.addElement(conn1links);
    }
    // remove duplicates and subsets from sets
    int setsize = sets.size();
    boolean todelete[] = new boolean[setsize];
    for (int i=0; i<setsize; i++) todelete[i]=false;
    for (int i=setsize-1; i>=0; i--) {
      Set si = (Set) sets.elementAt(i);
      for (int j=i-1; j>=0; j--) {
        Set sj = (Set) sets.elementAt(j);
        if (si.containsAll(sj)) todelete[j]=true;
        if (sj.containsAll(si) && todelete[j]==false) {
          todelete[i]=true;
          break;  // i is set for deletion, so go on
        }
      }
    }
    for (int i=setsize-1; i>=0; i--) {
      if (todelete[i]) sets.remove(i);
    }
    Set result = new HashSet(sets);
    return result;
  }


  /**
   * return a Set<Set Integer nodeId> > that is the set of all sets of nodeids
   * in the result set that have the property that are maximal sets of nodes
   * that are connected with each other with at most 1 hop.
   * @param k int
   * @throws GraphException
   * @return Set
   */
  public Set getAllConnectedNodes(int k) throws GraphException {
    if (k!=2) throw new GraphException("currently, input k must be 2");
    Set t=new HashSet();  // Set<Set<Integer nodeid> >
    for (int i=0; i<_nodes.length; i++) {
      Node v = getNode(i);
      // get the set Dv
      Set dv = v.getNNborIndices(this);
      dv.add(new Integer(i));  // not needed, as i is already in dv
      Vector l = new Vector();  // Set<Set<Integer nodeid> >
      Set generated = new HashSet();  // Set<IntSet s>
      l.add(dv);
      for (int j=0; j<l.size(); j++) {
        Set s = (Set) l.elementAt(j);
        l.remove(j);
        if (isConnected1(s)) {
          t.add(s);
          continue;
        }
        j--;  // go back one step
        Iterator siter = s.iterator();
        while (siter.hasNext()) {
          Integer u = (Integer) siter.next();
          if (u.intValue()==i) continue;  // don't consider the current node i
          Set sp = new HashSet(s);
          sp.remove(u);
          IntSet isp = new IntSet(sp);
          if (generated.contains(isp)==false) {
            generated.add(isp);
            l.addElement(sp);
          }
        }
      }
      System.err.println("processed node "+i);  // itc: HERE rm asap
    }
    Vector sets = new Vector();
    Iterator iter = t.iterator();
    while (iter.hasNext()) {
      sets.add(iter.next());
    }
    // remove duplicates and subsets from sets
    int setsize = sets.size();
    boolean todelete[] = new boolean[setsize];
    for (int i=0; i<setsize; i++) todelete[i]=false;
    for (int i=setsize-1; i>=0; i--) {
      Set si = (Set) sets.elementAt(i);
      for (int j=i-1; j>=0; j--) {
        Set sj = (Set) sets.elementAt(j);
        if (si.containsAll(sj)) todelete[j]=true;
        if (sj.containsAll(si) && todelete[j]==false) {
          todelete[i]=true;
          break;  // i is set for deletion, so go on
        }
      }
    }
    for (int i=setsize-1; i>=0; i--) {
      if (todelete[i]) sets.remove(i);
    }
    Set result = new HashSet(sets);
    return result;
  }


  /**
   * return the Set<IntSet nodeids> of all sets of nodeids with the property
   * that they are independent (maximal) and at distance at most 2 from each
   * other within the set they are in.
   * @param maxcount Integer if not null denotes the max number of 5-cycle based
   * sets of nodes to return
   * @return Set
   */
  public Set getAllConnectedBy1Nodes(Integer maxcount) throws GraphException {
    Set t = new TreeSet();
    for (int i=0; i<_nodes.length; i++) {
      // 1. create an IntSet and put in the i-th node and its neihgbors
      IntSet ti = new IntSet();
      ti.add(new Integer(i));
      Node ni = _nodes[i];
      Set inbors = ni.getNborIndices(this, Double.NEGATIVE_INFINITY);
      ti.addAll(inbors);
      // 2. add any neighbor of the neighbors that is connected to all the
      // i-th node's neighbors
      if (inbors.size()>0) {
        Iterator inborit = inbors.iterator();
        Set toadd = new HashSet();
        for (int ii = 0; ii < _nodes.length; ii++) toadd.add(new Integer(ii));
        while (inborit.hasNext()) {
          Integer nnid = (Integer) inborit.next();
          Set nnborids = _nodes[nnid.intValue()].getNborIndices(this,
              Double.NEGATIVE_INFINITY);
          toadd.retainAll(nnborids);
        }
        ti.addAll(toadd);
      }
      t.add(ti);
    }
    System.err.println("computing 5-cycle-based sets");  // itc: HERE rm asap
    Set t2 = new HashSet();
    // 3. add all 5-cycle-based sets: sets of the form {n1...n5} U N2
    // that are connected like (n1,n2),(n2,n3),(n3,n4),(n4,n5),(n5,n1) and each
    // other node in N2 has two neighbors that are in {n1...n5} that are at
    // distance 2 from each other (i.e. are not themselves neighbors)
    for (int i=0; i<_arcs.length; i++) {
      Link li = _arcs[i];
      Set ti2 = get5CycleBasedConnectedNodes(li, maxcount);  // Set<IntSet nodeids>
      t2.addAll(ti2);
    }
    System.err.println("get5CycleBasedConnectedNodes() returned "+t2.size()+" sets.");  // itc: HERE rm asap
    // 4. finally, remove subsets
    Vector sets = new Vector();
    Iterator iter = t.iterator();
    while (iter.hasNext()) {
      sets.add(iter.next());
    }
    iter = t2.iterator();
    /*
    int cnt = 0;  // itc: HERE rm asap
    while (iter.hasNext()) {
      sets.add(iter.next());
      if (++cnt>=5*_nodes.length) break;  // itc: HERE rm asap
    }
    */
    // remove duplicates and subsets from sets
    int setsize = sets.size();
    boolean todelete[] = new boolean[setsize];
    for (int i=0; i<setsize; i++) todelete[i]=false;
    for (int i=setsize-1; i>=0; i--) {
      Set si = (Set) sets.elementAt(i);
      for (int j=i-1; j>=0; j--) {
        Set sj = (Set) sets.elementAt(j);
        if (si.containsAll(sj)) todelete[j]=true;
        if (sj.containsAll(si) && todelete[j]==false) {
          todelete[i]=true;
          break;  // i is set for deletion, so go on
        }
      }
    }
    for (int i=setsize-1; i>=0; i--) {
      if (todelete[i]) sets.remove(i);
    }
    Set result = new HashSet(sets);
    System.err.println("in total, "+result.size()+" sets returned.");  // itc: HERE rm asap
    return result;
  }


  public Set getAllNborSets() {
    Set nborids = new HashSet();  // Set<Set<Integer nodeid> >
    for (int i=0; i<_nodes.length; i++) {
      Set inborids = _nodes[i].getNborIndices(this, Double.NEGATIVE_INFINITY);
      inborids.add(new Integer(i));  // add self
      if (inborids!=null && inborids.size()>0)  // should better be size() > 1
        nborids.add(inborids);
    }
    return nborids;
  }


  public Set getNborIds(Set nodeids) throws GraphException {
    if (nodeids==null || nodeids.size()==0)
      throw new GraphException("empty parameter");
    Set nborids = new HashSet();
    Iterator it = nodeids.iterator();
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
      Node ni = _nodes[nid.intValue()];
      Set nibors = ni.getNborIndices(this, Double.NEGATIVE_INFINITY);
      nborids.addAll(nibors);
    }
    nborids.removeAll(nodeids);
    return nborids;
  }


  public void makeNNbors() {
    makeNNbors(false);
  }
  public void makeNNbors(boolean force) {
    for (int i=0; i<_nodes.length; i++) {
      _nodes[i].getNNbors(force);  // force computation of all NNbors and storage in _nnbors cache
    }
  }


  public String toString() {
    String ret = "#nodes="+_nodes.length+" #arcs="+_arcs.length;
    for (int i=0; i<_arcs.length; i++)
      ret += " ["+_arcs[i].getStart()+","+_arcs[i].getEnd()+"("+_arcs[i].getWeight()+")]";
    return ret;
  }


  /**
   * nid is a Node id
   * c is the component which this node and all nodes reachable by it
   * belong to.
   * @param nid int
   * @param c int
   * @return int number of nodes labeled
   */
  private int labelComponent(int nid, int c) {
    int res=1;
    _comps[nid] = c;
    Set nbors = new HashSet(_nodes[nid].getNbors());
    while (nbors.size()>0) {
      Iterator it = nbors.iterator();
      Node n = (Node) it.next();
      nbors.remove(n);
      _comps[n.getId()] = c;  // label node
      res++;
      // add non-labeled nbors to nbors
      Set nnbors = n.getNbors();
      it = nnbors.iterator();
      while (it.hasNext()) {
        Node nn = (Node) it.next();
        if (_comps[nn.getId()]==-1) nbors.add(nn);
      }
    }
    return res;
  }


  private boolean expandCliquesBy1(Set cliques, Set newcliques, double minval) {
    Iterator iter = cliques.iterator();
    while (iter.hasNext()) {
      Set clique = (Set) iter.next();
      Vector fullnbors = getFullNbors(clique, minval);
      for (int i=0; i<fullnbors.size(); i++) {
        Integer nid = (Integer) fullnbors.elementAt(i);
        Set nclique = new TreeSet(clique);
        nclique.add(nid);
        newcliques.add(nclique);
      }
    }
    return (newcliques.size()>0);
  }


  private Vector getFullNbors(Set clique, double minval) {
    // System.out.print("clique="); print(clique);  // itc: HERE debug
    Vector res = new Vector();
    Iterator iter = clique.iterator();
    Set nbors = new HashSet();
    for (int i=0; i<_nodes.length; i++) {
      Integer ii = new Integer(i);
      if (clique.contains(ii)==false)
        nbors.add(ii);  // don't add the members of the clique
    }
    while (iter.hasNext()) {
      Integer nid = (Integer) iter.next();
      Set nborsi = getNode(nid.intValue()).getNborIndices(this, minval);  // Set<Node n>
      // remove from nbors the elements not in nborsi
      Iterator i2 = nbors.iterator();
      while (i2.hasNext()) {
        Integer id2 = (Integer) i2.next();
        if (nborsi.contains(id2)==false)
          i2.remove();  // remove id2 from nbors
      }
    }
    // return a Vector
    res.addAll(nbors);
    // System.out.print(" nbors=");print(nbors);System.out.println("");  // itc: HERE debug
    return res;
  }


  private Set getFullLinkNbors(int lid) {
    Set result=new TreeSet();  // Set<Integer linkid>
    result.add(new Integer(lid));
    Link l = getLink(lid);
    int s = l.getStart();
    Node sn = getNode(s);
    result.addAll(sn.getInLinks());
    result.addAll(sn.getOutLinks());
    int e = l.getEnd();
    Node en = getNode(e);
    result.addAll(en.getInLinks());
    result.addAll(en.getOutLinks());
    // also, add all nets from emanating from a neighbor of sn and ending on a
    // neighbor of en
    Set snnbors = sn.getNbors();
    Iterator iter = snnbors.iterator();
    while (iter.hasNext()) {
      Node snn = (Node) iter.next();
      Set snninlinks = snn.getInLinks();
      Iterator i2 = snninlinks.iterator();
      while (i2.hasNext()) {
        Integer inlid = (Integer) i2.next();
        Link inlink = getLink(inlid.intValue());
        Integer sid = new Integer(inlink.getStart());
        if (en.getNborIndices(this, 0).contains(sid))
          result.add(inlid);
      }
      Set snnoutlinks = snn.getOutLinks();
      Iterator i3 = snnoutlinks.iterator();
      while (i3.hasNext()) {
        Integer outlid = (Integer) i3.next();
        Link outlink = getLink(outlid.intValue());
        Integer eid = new Integer(outlink.getEnd());
        if (en.getNborIndices(this, 0).contains(eid))
          result.add(outlid);
      }
    }
    return result;
  }


  /**
   * add all cliques in 1st arg. that are not already in 2nd arg. into 2nd arg.
   * @param cliques2add Set
   * @param cliques Set
   */
  private void addUnique(Set cliques2add, Set cliques) {
    Iterator iter = cliques2add.iterator();
    while (iter.hasNext()) {
      Set clique = (Set) iter.next();
      cliques.add(clique);
    }
  }


  /**
   * returns true iff each node w/ id in the arg. set, can reach any
   * other node whose id is in the arg set by using at most 1 hop that must also
   * be inside the arg. set.
   * @param nodeids Set
   * @throws GraphException if arg set is empty or null
   * @return boolean
   */
  private boolean isConnected1(Set nodeids) throws GraphException {
    if (nodeids==null || nodeids.size()==0)
      throw new GraphException("empty nodeids");
    Object[] array = nodeids.toArray();
    int len = array.length;
    for (int i=0; i<len-1; i++) {
      Integer nid = (Integer) array[i];
      Node ni = getNode(nid.intValue());
      Set nibors = ni.getNbors();
      for (int j=i+1; j<len; j++) {
        Integer njd = (Integer) array[j];
        Node nj = getNode(njd.intValue());
        if (nibors.contains(nj)) continue;
        Set njbors_copy = new HashSet(nj.getNbors());
        njbors_copy.retainAll(nibors);
        if (njbors_copy.size()==0) {
          return false;
        }
        // njbors_copy must have one in nodeids
        Iterator itj = njbors_copy.iterator();
        boolean cont = false;
        while (itj.hasNext()) {
          Node nnj = (Node) itj.next();
          if (nodeids.contains(new Integer(nnj.getId()))) {
            cont = true;
            break;
          }
        }
        if (!cont) return false;
      }
    }
    return true;
  }


  private Set get5CycleBasedConnectedNodes(Link li, Integer maxsetcount)
      throws GraphException {
    int maxcount = Integer.MAX_VALUE;
    if (maxsetcount!=null) maxcount = maxsetcount.intValue();
    Set t = new HashSet();
    Node n1 = _nodes[li.getStart()];
    Node n2 = _nodes[li.getEnd()];
    //System.err.println("Computing 5-cycles for link "+li.getId()+"("+n1.getId()+","+n2.getId()+")");  // itc: HERE rm asap
    Set n1_nbors = n1.getNbors();
    Set n2_nbors = n2.getNbors();
    Iterator n1_iter = n1_nbors.iterator();
    int count=0;
    while (n1_iter.hasNext()) {
      Node n5 = (Node) n1_iter.next();
      if (n5.getId()==n2.getId()) continue;
      Set n5_nborids = n5.getNborIndices(this, Double.NEGATIVE_INFINITY);
      Iterator n2_iter = n2_nbors.iterator();
      while (n2_iter.hasNext()) {
        Node n3 = (Node) n2_iter.next();
        if (n3.getId()==n1.getId()) continue;  // nodes must be different
        Set n3_nborids = n3.getNborIndices(this, Double.NEGATIVE_INFINITY);
        boolean add1235 = false;
        if (n3.getNbors().contains(n5)) add1235 = true;
        n3_nborids.retainAll(n5_nborids);
        // each node id in n3_nborids is a valid n4
        Iterator n4_iter = n3_nborids.iterator();
        boolean added12345 = false;
        while (n4_iter.hasNext()) {
          Integer n4id = (Integer) n4_iter.next();
          int n4idi = n4id.intValue();
          if (n4idi==n1.getId() || n4idi==n2.getId())
            continue;
          /*  condition below is never true
          if (n4idi==n5.getId()) {
            add1235 = true;
            continue;
          }
          */
          // Node n4 = _nodes[n4id.intValue()];
          Set c5 = new IntSet();
          c5.add(new Integer(n1.getId()));
          c5.add(new Integer(n2.getId()));
          c5.add(new Integer(n3.getId()));
          c5.add(n4id);
          c5.add(new Integer(n5.getId()));
          // now add also any node having two nbors in the cycle that are not
          // themselves neighbors
          Set c5nborIds = getNborIds(c5);
          Iterator c5it = c5nborIds.iterator();
          boolean added = false;
          while (c5it.hasNext()) {
            Integer c5n = (Integer) c5it.next();
            Set c5test = new IntSet(c5);
            c5test.add(c5n);
            if (isConnected1(c5test)) {
              added = true;
              t.add(c5test);
              if (++count>=maxcount) return t;
            }
          }
          if (!added) {
            t.add(c5);
            if (++count>=maxcount) return t;
          }
          added12345 = true;
        }
        if (!added12345 && add1235) {
          // create the 1,2,3,5 set and add it in
          Set c4 = new IntSet();
          c4.add(new Integer(n1.getId()));
          c4.add(new Integer(n2.getId()));
          c4.add(new Integer(n3.getId()));
          c4.add(new Integer(n5.getId()));
          t.add(c4);
          if (++count>=maxcount) return t;
        }
      }
    }
    System.err.println(" . Total "+count+" sets added");  // itc: HERE rm asap
    return t;
  }


  private void print(Collection clique) {
    System.out.print("[");
    Iterator it = clique.iterator();
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      System.out.print(i+" ");
    }
    System.out.print("]");
  }

}

