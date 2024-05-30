package packing;

import coarsening.*;
import java.util.*;

public class BBNode3 implements Runnable, Comparable {
  // static private int _counter=0;
  private int _r;  // this node has assignments from [0...r-1] through its ancestors
           // and specifies whether node[r] has value 0 or 1 in _cindex;
  private int _cindex;  // specified above
  private int _numChildren;
  private BBNode3 _parent;  // node's parent
  private double _cost;  // this node's total cost
  private double _bound;  // this node's lower bound for the solution
  private int _nndegree;  // number of dist-2 neighbors
  private BBTree _master;  // pointer to book-keeping structure
  private boolean _isDone;  // indicates if node is fathomed
  private int _id;
  private int _counter2LP=0;


  public BBNode3(BBTree master, int r, int cindex, BBNode3 parent, double cost)
      throws ExactPackingException {
    if (master==null)
      throw new ExactPackingException("null args");
    _master = master;
    _isDone = false;
    _r = r;
    _cindex = cindex;
    _parent = parent;
    _numChildren = 0;
    if (_master.getRNP()!=null && _parent!=null)
      _counter2LP = _parent._counter2LP+1;
    if (parent==null) {  // i am the root
      if (r!=-1) {
        throw new ExactPackingException("root has _r!=-1");
      }
      _nndegree = 0;
      _cindex = cindex;
      _cost = _master.getGraphSize();
      _id = _master.incrementCounter();
      // _bound = _master.getGraphSize();
      _bound = 0;
      RelaxedNodePacker rnp = _master.getRNP();
      if (rnp!=null) {  // do LP for bounding
        if (rnp.isRunning()==false) {
          _counter2LP=0;  // reset
          Set setnodeids = new HashSet();
          try {
            double b = rnp.getPackingLPBound(setnodeids);
            _bound = _master.getGraphSize() - Math.floor(b);
            System.err.println("At root, bound = "+_bound);
          }
          catch (Exception e) {
            e.printStackTrace();
            // no-op
          }
        }
      }
      // System.err.println("Created Node w/ id="+_id+" for node "+r+ " set to "+cindex+" w/ cost="+cost);  // itc: HERE rm asap
    }
    else {  // i am not the root
      _cost = cost;
      _nndegree = _master.getGraph().getNode(_r).getNNbors().size();
      // get nbors with index > _r
      int nbig = 0;
      final int gsz = _master.getGraphSize();
      RelaxedNodePacker rnp = _master.getRNP();
      if (rnp!=null && _counter2LP>=_master.getSteps2LP() && rnp.isRunning()==false) {
        // do LP for bounding
        _counter2LP=0;  // reset
        BBNode3 p = this;
        Set setnodeids = new HashSet();
        while (p != null) {
          int pr = p._r;
          if (pr >= 0) setnodeids.add(new Integer(pr));
          p = p._parent;
        }
        if (_r < gsz -1) {
          try {
            double b = rnp.getPackingLPBound(setnodeids);
            _bound = gsz - Math.floor(b);
          }
          catch (Exception e) {
            e.printStackTrace();
            System.err.println("BBNode3.<init>: reverting to standard bounding scheme");
            // do standard bounding
            p = this;
            Set forbidden = new HashSet();
            Node curp = _master.getGraph().getNode(_r);
            while (p != null) {
              int pr = p._r;
              if (pr >= 0) {
                Node pn = _master.getGraph().getNode(pr);
                TreeSet nnbors = (TreeSet) pn.getNNbors();
                forbidden.addAll(nnbors.headSet(curp));
              }
              p = p._parent;
            }
            nbig = forbidden.size();
            _bound = _cost - (gsz - _r - 1 - nbig);
          }
        }
      }
      else {  // didn't solve LP
        if (_r >= _master.getTightenBoundLvl() && _r < gsz - 1) {
          BBNode3 p = this;
          Set forbidden = new HashSet();
          Node curp = _master.getGraph().getNode(_r);
          while (p != null) {
            int pr = p._r;
            if (pr >= 0) {
              Node pn = _master.getGraph().getNode(pr);
              TreeSet nnbors = (TreeSet) pn.getNNbors();
              forbidden.addAll(nnbors.headSet(curp));
            }
            p = p._parent;
          }
          nbig = forbidden.size();
        }
        _bound = _cost - (gsz - _r - 1 - nbig);
      }
      // compare with parent's bound for LP cases
      if (_parent._bound > _bound) _bound = _parent._bound;
      _id = _master.incrementCounter();
      if (_id % 100000 == 0)
        System.err.println("Created node w/ id="+_id+" _bound="+_bound+" _cost="+_cost+" _counter2LP="+_counter2LP);  // itc: HERE rm asap
      //System.err.println("Created Node w/ id="+_id+" from parent "+_parent._id+
      //                   " for node "+r+ " set to "+cindex+" w/ cost="+cost);  // itc: HERE rm asap
    }
  }


  public double getCost() { return _cost; }
  public int getId() { return _id; }
  public int getPos() { return _r; }
  public int getClusterIndex() { return _cindex; }
  public BBNode3 getParent() { return _parent; }


  boolean isDone() { return _isDone; }  // intentionally left unsynchronized
  public void setDone() {
    _numChildren = 0;
    _isDone = true;
    if (_parent!=null) _parent.notifyDone();
    if (_r==-1) _master.getQueue().setDone();  // notify BBTree to finish
  }


  public void run() {
    if (_id % 100000 == 0)
      System.err.println("running node id="+_id+" _bound="+_bound+" _cost="+_cost+" _counter2LP="+_counter2LP);  // itc: HERE rm asap
    final int gsz = _master.getGraphSize();
    // see if limit has been reached
    if (_master.getCounter()>_master.getMaxNodesAllowed()) {
      setDone();
      return;  // stop computations
    }
    if (_bound >= _master.getBound()) {
      setDone();
      return; // node is fathomed
    }
    else if (_cost < _master.getBound()) {
      _master.setIncumbent(this);  // sets bound on master as well
    }
    if (_bound >= _cost && _r>=0) {
      setDone();
      return;  // node is fathomed
    }
    if (_r==gsz-1) {
      setDone();
      return;
    }
    if (_r < gsz-1) {
      try {
        // find all the r values that can be set to one if they exist
        TreeSet children = new TreeSet();
        for (int r=_r+1; r<gsz; r++) {
          if (isFree2Cover(r)) {
            BBNode3 child = new BBNode3(_master, r, 1, this, _cost - 1);
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
             Iterator it = children.iterator();
             while (it.hasNext()) {
               BBNode3 child = (BBNode3) it.next();
               // BBThreadPool.getPool().execute(child);
               child.run(); // run on current thread
             }
             return;
           }
           else {
             // if getCutNodes() is true then drop all children nodes except run
             // the first child node only as heuristic
             BBNode3 bestchild = (BBNode3) children.first();
             bestchild.run();
             return;
             /*
             setDone();
             return;  // no children
             */
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
    BBNode3 o = (BBNode3) other;
    // best-first strategy: node with smallest bound is first to run
    double ct = _bound;
    double oct = o._bound;
    if (ct < oct) return -1;
    else if (ct == oct) {
      if (_nndegree < o._nndegree)return -1;
      else if (_nndegree > o._nndegree)return 1;
      else {  // agree on _nndegree
        if (_id < o._id)return -1;
        else if (_id == o._id)return 0;
        else return 1;
      }
    }
    else return 1;
  }


  public boolean equals(Object other) {
    BBNode3 o = (BBNode3) other;
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
    BBNode3 p = this;
    while (p!=null) {
      int rp = p._r;
      if (rp==-1) return true;  // reached root
      if (nbors.contains(g.getNode(rp)))
        return false;
      p = p._parent;
    }
    return true;
  }

}

