package packing;

import coarsening.*;
import java.util.*;

public class BBNode implements Runnable {
  // static private int _counter=0;
  private int _r;  // this node has assignments from [0...r-1] through its ancestors
           // and specifies whether node[r] has value 0 or 1 in _cindex;
  private int _cindex;  // specified above
  // private BBNode _children[];  // the node's children
  private int _numChildren;
  private BBNode _parent;  // node's parent
  private double _cost;  // this node's total cost
  private BBTree _master;  // pointer to book-keeping structure
  private boolean _isDone;  // indicates if node is fathomed
  private int _parlvl;
  private boolean _isLeftMost;
  private int _id;

  public BBNode(BBTree master, int r, int cindex, BBNode parent, double cost)
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
      _cindex = cindex;
      _cost = _master.getGraphSize();
      _isLeftMost = true;  // the root is a left-most node
      _id = _master.incrementCounter();
      // System.err.println("Created Node w/ id="+_id+" for node "+r+ " set to "+cindex+" w/ cost="+cost);  // itc: HERE rm asap
    }
    else {  // i am not the root
      _cost = cost;
      _id = _master.incrementCounter();
      // System.err.println("Created Node w/ id="+_id+" from parent "+_parent._id+
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


  public int getPos() { return _r; }
  public int getClusterIndex() { return _cindex; }
  public BBNode getParent() { return _parent; }


/*
  public void run() {
    if (_id % 1000000 == 0)
      System.err.println("running node id="+_id);  // itc: HERE rm asap
    // see if limit has been reached
    if (_master.getCounter()>_master.getMaxNodesAllowed()) {
      setDone();
      return;  // stop computations
    }
    double curbound = _master.getBound();
    if (_cost-(_master.getGraphSize()-_r-1) >= curbound) {
      setDone();
      return; // node is fathomed
    }
    else if (_cost <= curbound) {
      _master.setIncumbent(this);  // sets bound on master as well
    }
    if (_r==_master.getGraphSize()-1) {
      setDone();
      return;
    }
    if (_r < _master.getGraphSize()-1) {
      // compute cost of assigning node _r+1 to zero or one -if possible.
      // the cost of assigning node _r+1 to one when not possible is infinite
      // check if assigning to 1 is possible
      try {
        if (isFree2Cover()) {
          _children = new BBNode[2];
          _children[0] = new BBNode(_master, _r + 1, 1, this, _cost - 1);
          _children[1] = new BBNode(_master, _r + 1, 0, this, _cost);
        } else {
          _children = new BBNode[1];
          _children[0] = new BBNode(_master, _r + 1, 0, this, _cost);
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);  // oops...
      }
      // no need to sort _children
      // Arrays.sort(_children, new BBNodeComp());
      // figure out if _children[0] is a leftmost node
      if (_isLeftMost) {
        _children[0]._isLeftMost = true;
      }

      // now solve for each child
      int n = _children.length;
      for (int i=0; i<n; i++) {
        try {
          if (_r >= _parlvl && BBThreadPool.getNumThreads()>1) {
            BBThreadPool.getPool().execute(_children[i]);
            // run in parallel iff there are available threads AND
            // do that every 10 nodes, so that several nodes run on same thread
          }
          else {  // don't start parallel execution yet
            _children[i].run2();  // run in the current thread
          }
        }
        catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(-1);  // oops...
        }
      }
    }
  }


  void setDone() {
    // System.err.println("Closing Node "+_id+".");  // itc: HERE rm asap
    boolean go_up = false;
    synchronized(this) {
      if (_isDone==false) {
        // now I'm done
        _isDone = true;
        _children = null; // allow garbage collection
        // check who else in the ancestors is done also
        if (_parent != null) {
          BBNode siblings[] = _parent._children;
          if (siblings != null) {
            int i = 0;
            for (; i < siblings.length; i++) {
              if (siblings[i]._isDone == false)break; // cannot use the synch method here
            }
            if (i == siblings.length) go_up = true;
          }
        }
      }
    }
    if (go_up) _parent.setDone(); // recursively go up
    // System.err.println("Node "+_id+" closed.");
  }

   boolean isDone() { return _isDone; }  // intentionally left unsyncrhonized
*/


  boolean isDone() { return _isDone; }  // intentionally left unsynchronized
  public void setDone() {
    _numChildren = 0;
    _isDone = true;
    _parent.notifyDone();
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
      BBNode p = this;
      while (p!=null) {
        int pr = p._r;
        if (pr >= 0 && p._cindex == 1) {
          Set nbors = _master.getGraph().getNode(pr).getNNbors();
          Iterator it = nbors.iterator();
          while (it.hasNext()) {
            Node nn = (Node) it.next();
            if (nn.getId() > _r)++nbig;
          }
        }
        p = p._parent;
      }
    }
    if (_cost-(gsz-_r-1+nbig) >= _master.getBound()) {
      setDone();
      return; // node is fathomed
    }
    else if (_cost < _master.getBound()) {
      // _master.setIncumbent(this);  // sets bound on master as well
    }
    if (_r==gsz-1) {
      setDone();
      return;
    }
    if (_r < gsz-1) {
      // compute cost of assigning node _r+1 to zero or one -if possible.
      // the cost of assigning node _r+1 to one when not possible is infinite
      // check if assigning to 1 is possible
      try {
        if (isFree2Cover()) {
          _numChildren = 2;
          BBNode child0 = new BBNode(_master, _r + 1, 1, this, _cost - 1);
          BBNode child1 = new BBNode(_master, _r + 1, 0, this, _cost);
          int numnodes = _master.getJoinNumNodes();
          if (_r >= _parlvl && _id % numnodes == 0 &&
              BBThreadPool.getNumThreads()>1) {
            BBThreadPool.getPool().execute(child0);
            BBThreadPool.getPool().execute(child1);
            // run in parallel iff there are available threads AND
            // do that every numnodes, so that several nodes run on same thread
          }
          else {  // don't start parallel execution yet
            child0.run();  // run in the current thread
            child1.run();
          }
        } else {
          _numChildren = 1;
          BBNode child0 = new BBNode(_master, _r + 1, 0, this, _cost);
          int numnodes = _master.getJoinNumNodes();
          if (_r >= _parlvl && _id % numnodes == 0 &&
              BBThreadPool.getNumThreads()>1) {
            BBThreadPool.getPool().execute(child0);
            // run in parallel iff there are available threads AND
            // do that every 10 nodes, so that several nodes run on same thread
          }
          else {  // don't start parallel execution yet
            child0.run();  // run in the current thread
          }
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);  // oops...
      }
    }
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
  }


  /**
   * check if node with id _r in _g of the master, can be set to 1 or not for
   * this node.
   * @return boolean
   */
  private boolean isFree2Cover() {
    if (_r==-1) return true;  // root node must create two children for node 0.
    Graph g = _master.getGraph();
    Node n = g.getNode(_r);
    Node nxt = g.getNode(_r+1);
    Set nbors = nxt.getNNbors();
    if (_cindex==1 && nbors.contains(n)) return false;
    BBNode p = _parent;
    while (p!=null) {
      int rp = p._r;
      if (rp==-1) return true;  // reached root
      int indp = p._cindex;
      if (indp==1 && nbors.contains(g.getNode(rp)))
        return false;
      p = p._parent;
    }
    return true;
  }
}


class BBNodeComp implements Comparator, java.io.Serializable {
  public int compare(Object o1, Object o2) {
    BBNode n1 = (BBNode) o1;
    BBNode n2 = (BBNode) o2;
    if (n1.getCostAux() < n2.getCostAux()) return -1;
    else if (n1.getCostAux()==n2.getCostAux()) return 0;
    else return 1;
  }
}
