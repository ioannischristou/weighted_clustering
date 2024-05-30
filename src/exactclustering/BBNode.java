package exactclustering;

import clustering.*;
import java.util.*;

public class BBNode implements Runnable {
  static private int _counter=0;
  private int _r;  // this node has assignments from [0...r-1] through its ancestors
           // and specifies the cluster to which Doc[r] lies in _cindex;
  private int _cindex;  // specified above
  private BBNode _children[];  // the node's children
  private BBNode _parent;  // node's parent
  private double _cost;  // this node's total clustering cost
  private double _costs[];  // the cost for each cluster for this node (similar to _cards[])
  private Vector _means;  // Vector<Document>
                  // this node's means (can be less than _master._k, and represents
                  // the clustering centers that exist in the current asgn)
  private int _cards[];  // the cardinalities of each cluster (has the same length as _means)
  private BBTree _master;  // pointer to book-keeping structure
  private boolean _isDone;  // indicates if node is fathomed
  private int _parlvl;
  private boolean _isLeftMost;

  public BBNode(BBTree master, int r, int cindex, BBNode parent, double cost)
      throws ExactClusteringException {
    if (master==null)
      throw new ExactClusteringException("null args");

    _master = master;
    _isDone = false;
    _r = r;
    _cindex = cindex;
    _parent = parent;
    _parlvl = _master.getParLvl();
    _isLeftMost = false;  // default is false
    if (parent==null) {  // i am the root
      if (r!=0) {
        throw new ExactClusteringException("root has _r!=0");
      }
      if (cindex!=0) {
        throw new ExactClusteringException("root gets index that's not zero");
      }
      _cards = new int[1];
      _cindex = cindex;
      _cards[0]=1;
      _means = new Vector();
      _means.addElement(new Document((Document) _master.getDocs().elementAt(0)));
      _cost = 0.0;

      _costs = new double[1];
      _costs[0] = 0.0;

      _isLeftMost = true;  // the root is a left-most node
      incrementCounter();
    }
    else {  // i am not the root
      Vector pmeans = _parent._means;
      if (pmeans.size()<cindex)
        throw new ExactClusteringException("bad cindex arg");

      // construct the _cards array for this node
      int num_clusters = pmeans.size()>cindex ? pmeans.size() : cindex+1;
      _cards = new int[num_clusters];
      for (int i=0; i<_parent._cards.length; i++) {
        if (cindex!=i) _cards[i] = _parent._cards[i];
        else _cards[i] = _parent._cards[i]+1;
      }
      if (cindex==pmeans.size()) {  // cindex is a new cluster
        _cards[cindex] = 1;
      }

      // construct _costs array for this node
      _costs = new double[num_clusters];
      for (int i=0; i<_parent._costs.length; i++) {
        if (cindex!=i) _costs[i] = _parent._costs[i];
        else _costs[i] = _parent._costs[i]+(cost - _parent._cost);
      }
      if (cindex==pmeans.size()) {
        _costs[cindex] = 0.0;  // new cluster formation
      }

      // construct the _means centers for this node
      _means = new Vector();
      for (int i = 0; i < pmeans.size(); i++) {
        Document mi = (Document) pmeans.elementAt(i);
        if (cindex!=i) {
          Document new_mi = new Document(mi);
          _means.addElement(new_mi);
        }
        else {  // must update cluster cindex with document r
          Document new_mi = new Document((Document) _master.getDocs().elementAt(_r));
          new_mi.addMul(_parent._cards[cindex], mi);
          try {
            new_mi.div(_parent._cards[cindex] + 1.0);
            _means.addElement(new_mi);
          }
          catch (clustering.ClustererException e) {
            throw new ExactClusteringException("ClustererException was thrown");
          }
        }
      }
      if (cindex==pmeans.size()) {
        _means.addElement(new Document((Document) _master.getDocs().elementAt(_r)));
      }

      // finally, set cost
      _cost = cost;

      incrementCounter();
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


  public void run() {
    // see if limit has been reached
    if (getCounter()>_master.getMaxNodesAllowed()) {
      setDone();
      return;  // stop computations
    }
    if (_cost > _master.getBound()) {
      setDone();
      return; // node is fathomed
    }
    else if (_cost <= _master.getBound() && _r==_master.getDocSize()-1) {
      _master.setIncumbent(this);
      // _master.setBound(_cost);  // found incumbent soln
      setDone();
      return;
    }
    else if (_r < _master.getDocSize()-1) {
      // compute cost of assigning Doc. _r+1 to any cluster in [0...min{_cards.length+1, _k}]
      // the cost of assigning Doc. _r+1 to cluster w/ index _cards.length is zero.
      final int k = _master.getK();
      int num_child_clusters = _cards.length < k ? _cards.length+1 : k;
      _children = new BBNode[num_child_clusters];
      Document xrp1 = (Document) _master.getDocs().elementAt(_r+1);
      for (int i=0; i<num_child_clusters; i++) {
        // figure out cost of assigning _r+1 to i
        Document mi = null;
        if (i<_cards.length) {
          mi = (Document) _means.elementAt(i);
          try {

            double delta_Ji = (_cards[i] / ( (double) _cards[i] + 1.0)) *
                _master.getMetric().dist(xrp1, mi);
            _children[i] = new BBNode(_master, _r + 1, i, this,
                                      delta_Ji + _cost);
            /*
            // try more extensive computation for new total cost
            double new_cost = 0.0;
            for (int j=0; j<_costs.length; j++) {
              if (j!=i) new_cost += _costs[j];
            }
            // deal with the modified cluster now
            Document mi_rp1 = new Document(xrp1);
            mi_rp1.addMul(_cards[i], mi);
            mi_rp1.div(_cards[i]+1.0);
            new_cost += _master.getMetric().dist(mi_rp1, xrp1);
            for (BBNode up=this; up!=null; up=up._parent) {
              Document xup = (Document) _master.getDocs().elementAt(up._r);
              if (up._cindex==i) new_cost += _master.getMetric().dist(mi_rp1, xup);
            }
            _children[i] = new BBNode(_master, _r + 1, i, this, new_cost);
            */
          }
          catch (Exception e) {
            e.printStackTrace();
            System.exit( -1); // oops...
          }
        }
        else {  // doc. _r+1 is assigned to a new cluster on its own
          try {
            _children[i] = new BBNode(_master, _r + 1, i, this, _cost);
          }
          catch (Exception e) {
            e.printStackTrace();
            System.exit( -1); // oops...
          }
        }
      }
      // sort _children
      Arrays.sort(_children, new BBNodeComp());
      // figure out if _children[0] is a leftmost node
      if (_isLeftMost) {
        _children[0]._isLeftMost = true;
      }

      // now solve for each child
      for (int i=0; i<num_child_clusters; i++) {
        try {
          if (_r > _parlvl && BBThreadPool.getNumThreads()>1) {
            BBThreadPool.getPool().execute(_children[i]);
            // run in parallel iff there are available threads
          }
          else {  // don't start parallel execution yet
            _children[i].run();  // run in the current thread
          }
        }
        catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(-1);  // oops...
        }
      }
    }
  }


  synchronized void setDone() {
    if (_isDone==false) {
      _isDone = true;
      _children = null; // allow garbage collection
      // check who else in the ancestors is done also
      if (_parent != null) {
        BBNode siblings[] = _parent._children;
        int i = 0;
        if (siblings != null) {
          for (; i < siblings.length; i++)
            if (siblings[i]._isDone == false) break;  // cannot use the synch method here
          if (i == siblings.length) _parent.setDone(); // recursively go up
        }
      }
    }
  }


  synchronized boolean isDone() { return _isDone; }


  synchronized static void incrementCounter() { _counter++; }
  synchronized static int getCounter() { return _counter; }
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
