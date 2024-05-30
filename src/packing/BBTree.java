package packing;

import coarsening.*;
import java.util.*;


public class BBTree {

  private Graph _g;
  private int _gsz;  // cache
  // private int _k;  // #clusters to partition
  // private DocumentDistIntf _metric;
  // private BBNode3 _incumbent;
  private Set _incumbent;
  private BBQueue _q;
  private int _maxQSz;
  private boolean _cutNodes=false;
  private double _bound;
  // private BBNode _root;
  // private BBNode2 _root2;
  private BBNode3 _root3;
  private int _maxNodesAllowed=Integer.MAX_VALUE;
  private int _counter=0;
  // private int[] _prunedNodes=null;
  private int _initsoln[]=null;  // if available, will guide the search
  private int _parLvl;
  private int _numnodes=1;  // every this many nodes, the child is run on
  // a thread from the ThreadPool -if available
  private int _tightenBoundLvl=Integer.MAX_VALUE;
  private RelaxedNodePacker _rnp = null;
  private int _steps2lp = 0;


  public BBTree(Graph g, double initbound) throws ExactPackingException {
    _g = g;
    // elements of the argument
    // _k = k;
    _parLvl = 2;
    _gsz = _g.getNumNodes();
    _bound = initbound;
    //_root = new BBNode(this, 0, 0, null, 0.0);
    _root3 = new BBNode3(this, -1, -1, null, _gsz);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public BBTree(Graph g, int[] initsoln, double initbound) throws ExactPackingException {
    _g = g;
    // elements of the argument
    _gsz = _g.getNumNodes();
    //_prunedNodes = new int[_gsz];
    _bound = initbound;
    _initsoln = initsoln;
    // _root = new BBNode(this, 0, 0, null, 0.0);
    _root3 = new BBNode3(this, -1, -1, null, _gsz);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public BBTree(Graph g, int[] initsoln, double initbound, RelaxedNodePacker rnp, int steps2LP)
      throws ExactPackingException {
    _g = g;
    // elements of the argument
    _gsz = _g.getNumNodes();
    //_prunedNodes = new int[_gsz];
    _bound = initbound;
    _initsoln = initsoln;
    _rnp = rnp;
    _steps2lp = steps2LP;
    // _root = new BBNode(this, 0, 0, null, 0.0);
    _root3 = new BBNode3(this, -1, -1, null, _gsz);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public BBTree(Graph g, int[] initsoln, double initbound, int parlvl) throws ExactPackingException {
    _g = g; // doesn't perform deep copy
    _parLvl = parlvl;
    _gsz = _g.getNumNodes();
    //_prunedNodes = new int[_gsz];
    _bound = initbound;
    _initsoln = initsoln;
    //_root = new BBNode(this, -1, -1, null, _gsz);
    _root3 = new BBNode3(this, -1, -1, null, _gsz);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public void run() throws InterruptedException {
    _g.makeNNbors();  // ensure _nnbors cache for every node is ok before starting
    /*
    // work in parallel
    BBThreadPool.getPool().execute(_root2);
    while (!_root2.isDone()) {
      Thread.currentThread().sleep(100);
    }
    */
    // use BBQueue
    _q = new BBQueue(_maxQSz);
    _q.insertNode(_root3);
    _q.start();
    /*synchronized (this) {
      while (_q.isDone() == false) {
        wait();
      }
    }
    */
   while (!_q.isDone()) {
     Thread.currentThread().sleep(100);
   }
    System.out.println("Opt Soln found: "+(_gsz-_bound));
  }


  public Graph getGraph() { return _g; }
  public int getGraphSize() { return _gsz; }

  public int getParLvl() { return _parLvl; }

  public BBQueue getQueue() { return _q; }

  public int[] getInitSolution() { return _initsoln; }


  public void setMaxNodesAllowed(int n) {
    _maxNodesAllowed = n;
  }
  public int getMaxNodesAllowed() { return _maxNodesAllowed; }

  public void setJoinNumNodes(int numnodes) {
    _numnodes = numnodes;
  }
  public int getJoinNumNodes() {
    return _numnodes;
  }
  public void setMaxQSize(int s) {
    _maxQSz = s;
  }
  public void setCutNodes(boolean v) { _cutNodes = v; }
  public boolean getCutNodes() { return _cutNodes; }
  public void setTightenBoundLvl(int level) { _tightenBoundLvl=level; }
  public int getTightenBoundLvl() { return _tightenBoundLvl; }
  public void setLP(RelaxedNodePacker rnp, int s2lp) {
    _rnp = rnp; _steps2lp = s2lp;
  }
  public RelaxedNodePacker getRNP() {
    return _rnp;
  }
  public int getSteps2LP() {
    return _steps2lp;
  }

  synchronized int incrementCounter() { return ++_counter; }
  int getCounter() { return _counter; }  // intentionally unsynchronized

  // synchronized void incPrunedNodes(int lvl) { _prunedNodes[lvl]++; }
  // int[] getPrunedNodes() { return _prunedNodes; }

  synchronized public void setIncumbent(BBNode3 n) {
    if (n.getCost()<_bound) {
      //_incumbent = n;
      _incumbent = new HashSet();
      BBNode3 ptr = n;
      while (ptr!=null) {
        int pos = ptr.getPos();
        if (pos>=0) _incumbent.add(new Integer(ptr.getPos()));
        else break;
        ptr = ptr.getParent();
      }
      _bound = n.getCost();
      System.err.println("new soln found w/ val=" + _bound);
    }
  }


  public void setSolution(Set sol) {
    _incumbent = new HashSet(sol);
    _bound = getGraphSize()-sol.size();
  }
  public int[] getSolution() {
    int inds[] = new int[_gsz];
    Iterator it = _incumbent.iterator();
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      inds[i.intValue()]=1;
    }
    return inds;
  }
/*
  public int[] getSolution() {
    int inds[] = new int[_gsz];
    for (int i=0; i<_gsz; i++) inds[i]=0;
    BBNode3 ptr = _incumbent;
    while (ptr!=null) {
      if (ptr.getPos()==-1) break;  // root node
      inds[ptr.getPos()] = ptr.getClusterIndex();
      ptr = ptr.getParent();
    }
    return inds;
  }
*/

  public double getBound() { return _bound; }  // intentionally unsynchronized

}

