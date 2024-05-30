package coarsening;

import java.util.*;

public class Node implements Comparable {
  private int _id;
  private Set _outlinks;  // Set<Integer linkid>
  private Set _inlinks;  // Set<Integer linkid>
  private Set _nbors;  // Set<Node nbor>
  private Set _nnbors;  // Set<Node nnbor>
  private Set _nnborCache;  // Set<Node nnbor>
  private Hashtable _weights;  // map<String wname, Double wvalue>


  Node(int id) {
    _id=id;
    _nbors = new HashSet();
    _outlinks = new HashSet();
    _inlinks = new HashSet();
    _weights = new Hashtable();
  }


  public int getId() { return _id; }


  public int compareTo(Object other) {
    Node o = (Node) other;
    if (_id < o._id) return 1;  // nodes with large id come first
    else if (_id==o._id) return 0;
    else return -1;
  }


  public boolean equals(Object other) {
    Node o = (Node) other;
    return _id == o._id;
  }


  public int hashCode() {
    return _id;
  }


  void addOutLink(Node t, Integer linkid) {
    _nbors.add(t);
    _outlinks.add(linkid);
    t.addInLink(this, linkid);
  }


  public void setWeight(String name, Double val) {
    _weights.put(name, val);
  }


  public Double getWeightValue(String name) {
    return (Double) _weights.get(name);
  }


  public final Set getNbors() { return _nbors; }


  public Set getNborIndices(Graph g, double val) {
    Set indices = new HashSet();  // Set<Integer nid>
    Iterator it = _inlinks.iterator();
    while (it.hasNext()) {
      Integer lid = (Integer) it.next();
      Link l = g.getLink(lid.intValue());
      if (l.getWeight()>=val) indices.add(new Integer(l.getStart()));
    }
    Iterator it2 = _outlinks.iterator();
    while (it2.hasNext()) {
      Integer lid = (Integer) it2.next();
      Link l = g.getLink(lid.intValue());
      if (l.getWeight()>=val) indices.add(new Integer(l.getEnd()));
    }
    return indices;
  }


  public Set getNNborIndices(Graph g) {
    Set indices = new HashSet();
    Iterator iter = _nbors.iterator();
    while (iter.hasNext()) {
      Node n = (Node) iter.next();
      indices.add(new Integer(n.getId()));
      Set nnbors = n.getNborIndices(g, Double.NEGATIVE_INFINITY);
      indices.addAll(nnbors);
    }
    return indices;
  }


  /**
   * return all neighbors of this node at distance 1 or 2
   * @return Set
   */
  public final Set getNNbors() {
    return getNNbors(false);
  }


  /**
   * return all neighbors of this node at distance 1 or 2.
   * The returned set is a reference to the data member _nnbors so cannot
   * be used to be modified in any way
   * @param force boolean
   * @return Set  // Set<Node>
   */
  public final Set getNNbors(boolean force) {
    if (_nnbors!=null && force==false) return _nnbors;
    else if (_nnborCache==null) {  // force re-computation
      _nnbors = new TreeSet(_nbors);
      Iterator it = _nbors.iterator();
      while (it.hasNext()) {
        Node nbor = (Node) it.next();
        Set nnbors = nbor.getNbors();
        _nnbors.addAll(nnbors);
      }
      _nnbors.remove(this);
      // store in cache
      _nnborCache = new TreeSet(_nnbors);
      return _nnbors;
    }
    else {
      // restore from cache
      _nnbors = new TreeSet(_nnborCache);
      return _nnbors;
    }
  }


  public Set getInLinks() { return _inlinks; }


  public Set getOutLinks() { return _outlinks; }


  public double getArcWeights(Graph g) {
    double res = 0.0;
    Iterator itin = _inlinks.iterator();
    while (itin.hasNext()) {
      Integer lid = (Integer) itin.next();
      Link l = g.getLink(lid.intValue());
      res += l.getWeight();
    }
    Iterator itout = _outlinks.iterator();
    while (itout.hasNext()) {
      Integer lid = (Integer) itout.next();
      Link l = g.getLink(lid.intValue());
      res += l.getWeight();
    }
    return res;
  }

  private void addInLink(Node t, Integer linkid) {
    _nbors.add(t);
    _inlinks.add(linkid);
    // if (t._outlinks.contains(linkid)==false) t.addOutLink(this, linkid);
  }

}
