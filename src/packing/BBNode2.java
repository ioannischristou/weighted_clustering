package packing;

import coarsening.*;
import java.util.*;

public class BBNode2 implements Runnable, Comparable {
  // static private int _counter=0;
  private int _r;  // this node has assignments from [0...r-1] through its ancestors
           // and specifies whether node[r] has value 0 or 1 in _cindex;
  private int _cindex;  // specified above
  private int _numChildren;
  private BBNode2 _parent;  // node's parent
  private double _cost;  // this node's total cost
  private long _sumOfDegrees;  // this node's cost for sorting heuristic purposes
  private BBTree _master;  // pointer to book-keeping structure
  private boolean _isDone;  // indicates if node is fathomed
  private int _parlvl;
  private boolean _isLeftMost;
  private int _id;

  public BBNode2(BBTree master, int r, int cindex, BBNode2 parent, double cost)
      throws ExactPackingException {
    if (master==null)
      throw new ExactPackingException("null args");
    _master = master;
    _isDone = false;
    _r = r;
    _cindex = cindex;
    _parent = parent;
    _parlvl = _master.getParLvl();
    _isLeftMost = false;  // default is false
    _numChildren = 0;
    if (parent==null) {  // i am the root
      if (r!=-1) {
        throw new ExactPackingException("root has _r!=-1");
      }
      _sumOfDegrees = 0;
      _cindex = cindex;
      _cost = _master.getGraphSize();
      _isLeftMost = true;  // the root is a left-most node
      _id = _master.incrementCounter();
      //_id=1;
      // System.err.println("Created Node w/ id="+_id+" for node "+r+ " set to "+cindex+" w/ cost="+cost);  // itc: HERE rm asap
    }
    else {  // i am not the root
      _cost = cost;
      _sumOfDegrees = parent._sumOfDegrees +
                      _master.getGraph().getNode(r).getNNbors().size();
      _id = _master.incrementCounter();
      //System.err.println("Created Node w/ id="+_id+" from parent "+_parent._id+
      //                   " for node "+r+ " set to "+cindex+" w/ cost="+cost);  // itc: HERE rm asap
    }
  }


  public double getCost() { return _cost; }
  public double getCostAux() {
    // if there is a solution, use it to guide the search
    if (_master.getInitSolution()!=null) {
      int soln[] = _master.getInitSolution();
      if (_cindex == soln[_r] && _parent._isLeftMost) return 0.0;  // make the child with this asgn first
      else return _cost;
    }
    else return _cost;
  }

  public int getId() { return _id; }
  public int getPos() { return _r; }
  public int getClusterIndex() { return _cindex; }
  public BBNode2 getParent() { return _parent; }




  boolean isDone() { return _isDone; }  // intentionally left unsynchronized
  public void setDone() {
    _numChildren = 0;
    _isDone = true;
    if (_parent!=null) _parent.notifyDone();
    if (_r==-1) _master.getQueue().setDone();  // notify BBTree to finish
  }


  public void run() {
    if (_id % 1000000 == 0)
      System.err.println("running node id="+_id);  // itc: HERE rm asap
    final int gsz = _master.getGraphSize();
    // see if limit has been reached
    if (_master.getCounter()>_master.getMaxNodesAllowed()) {
      setDone();
      return;  // stop computations
    }
    // get nbors with index > _r
    int nbig = 0;
    if (_r >= _master.getTightenBoundLvl() && _r<gsz-1) {
      BBNode2 p = this;
/*
      boolean marked[] = new boolean[gsz];  // initialized to false?
      while (p!=null) {
        int pr = p._r;
        if (pr >= 0 && p._cindex == 1) {
          Set nbors = _master.getGraph().getNode(pr).getNNbors();
          Iterator it = nbors.iterator();
          while (it.hasNext()) {
            Node nn = (Node) it.next();
            if (nn.getId()<=_r) break;  // from now on, the small ids will appear
            if (marked[nn.getId()]==false) {
              ++nbig;
              marked[nn.getId()]=true;
            }
          }
        }
        p = p._parent;
      }
*/
      Set forbidden = new HashSet();
      Node curp = _master.getGraph().getNode(_r);
      while (p!=null) {
        int pr=p._r;
        if (pr>=0) {
          Node pn = _master.getGraph().getNode(pr);
          TreeSet nnbors = (TreeSet) pn.getNNbors();
          forbidden.addAll(nnbors.headSet(curp));
        }
        p = p._parent;
      }
      nbig = forbidden.size();
    }
    // if (_cost-(gsz-_r-1)+nbig >= _master.getBound()) {
    if (_cost-(gsz-_r-1-nbig) >= _master.getBound()) {
      //_master.incPrunedNodes(_r);
      setDone();
      return; // node is fathomed
    }
    else if (_cost < _master.getBound()) {
      //_master.setIncumbent(this);  // sets bound on master as well
    }
    if (_r==gsz-1) {
      setDone();
      return;
    }
    if (_r < gsz-1) {
      try {
        // find all the r values that can be set to one if they exist
        Vector children = new Vector();
        for (int r=_r+1; r<gsz; r++) {
          if (isFree2Cover(r)) {
            BBNode2 child = new BBNode2(_master, r, 1, this, _cost - 1);
            children.add(child);
          }
        }
        _numChildren = children.size();
        int sz = children.size();
        if (sz==0) {
          setDone();
          return;  // no children
        }
         boolean added_nodes = _master.getQueue().insertNodes(children);
         if (added_nodes==false) {  // BBQueue grew too big
           if (_master.getCutNodes() == false) {
             for (int i = 0; i < sz; i++) {
               BBNode2 child = (BBNode2) children.elementAt(i);
               // BBThreadPool.getPool().execute(child);
               child.run(); // run on current thread
             }
             return;
           }
           else {
             // if getCutNodes() is true then drop the nodes, and run only as heuristic
             setDone();
             return;  // no children
           }
         }
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);  // oops
      }
    }
  }


  public int compareTo(Object other) {
    BBNode2 o = (BBNode2) other;
    // double ct = _cost * _sumOfDegrees / (double)(_r+2);
    // double oct = o._cost * o._sumOfDegrees / (double)(o._r+2);
    double ct = _cost;
    double oct = o._cost;
    if (ct < oct) return -1;
    else if (ct == oct) {
      double ct2 = _sumOfDegrees / (double) (_r + 2);
      double oct2 = o._sumOfDegrees / (double) (o._r + 2);
      if (ct2 < oct2) {
        return -1;
      }
      else if (ct2 == oct2) {
        if (_id < o._id)return -1;
        else if (_id == o._id)return 0;
        else return 1;
      }
      else return 1;
    }
    else return 1;
  }


  public boolean equals(Object other) {
    BBNode2 o = (BBNode2) other;
    if (_id==o._id) return true;
    else return false;
  }


  public int hashCode() {
    return _id;
  }


  void notifyDone() {
    boolean go_up = false;
    synchronized(this) {
      --_numChildren;
      if (_numChildren == 0) {
        _isDone = true;
        if (_parent != null) go_up = true;
      }
    }
    if (go_up) _parent.notifyDone();
    else if (_parent==null && _numChildren==0) {
      _master.getQueue().setDone(); // notify BBQueue to finish
      // _master.notify(); // notify master to finish
    }
  }


  /**
   * check if node with id r in _g of the master, can be set to 1 or not for
   * this node.
   * @return boolean
   */
  private boolean isFree2Cover(int r) {
    if (r==-1) return true;  // root node must create two children for node 0.
    Graph g = _master.getGraph();
    Node nxt = g.getNode(r);
    Set nbors = nxt.getNNbors();
    BBNode2 p = this;
    while (p!=null) {
      int rp = p._r;
      if (rp==-1) return true;  // reached root
      if (nbors.contains(g.getNode(rp)))
        return false;
      p = p._parent;
    }
    return true;
  }

/*
  public static void main(String[] args) {
    try {
      TreeSet s = new TreeSet();
      BBNode2 n = new BBNode2(null, -1, 1, null, 1.0);
      s.add(n);
      BBNode2 n2 = (BBNode2) s.first();
      System.out.println("got a node w/ id="+n2._id);
      s.remove(n2);
      if (s.size()>0)
        System.out.println("failed to remove");
    }catch (Exception e) {e.printStackTrace(); }
  }
*/

}

