package exactclustering;

import clustering.*;
import java.util.*;


public class BBTree {
  private Vector _docs; // Vector<Document>
  private int _docsz;  // cache
  private int _k;  // #clusters to partition
  private DocumentDistIntf _metric;
  private BBNode _incumbent;
  private double _bound;
  private BBNode _root;
  private int _maxNodesAllowed=Integer.MAX_VALUE;
  private int _initsoln[]=null;  // if available, will guide the search
  private int _parLvl;

  public BBTree(Vector docs, int k, DocumentDistIntf m, double initbound) throws ExactClusteringException {
    _docs = new Vector(docs); // doesn't perform deep copy of the Document
    // elements of the argument
    _k = k;
    _parLvl = _k;
    _docsz = _docs.size();
    _metric = m;  // should be L2Sqr only as for the other metrics there is no
                  // guarantee of the monotone clustering property
    _bound = initbound;
    _root = new BBNode(this, 0, 0, null, 0.0);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public BBTree(Vector docs, int k, DocumentDistIntf m, int[] initsoln, double initbound) throws ExactClusteringException {
    _docs = new Vector(docs); // doesn't perform deep copy of the Document
    // elements of the argument
    _k = k;
    _parLvl = _k;
    _docsz = _docs.size();
    _metric = m;  // should be L2Sqr only as for the other metrics there is no
                  // guarantee of the monotone clustering property
    _bound = initbound;
    _initsoln = initsoln;
    _root = new BBNode(this, 0, 0, null, 0.0);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public BBTree(Vector docs, int k, DocumentDistIntf m, int[] initsoln, double initbound, int parlvl) throws ExactClusteringException {
    _docs = new Vector(docs); // doesn't perform deep copy of the Document
    // elements of the argument
    _k = k;
    _parLvl = parlvl;
    _docsz = _docs.size();
    _metric = m;  // should be L2Sqr only as for the other metrics there is no
                  // guarantee of the monotone clustering property
    _bound = initbound;
    _initsoln = initsoln;
    _root = new BBNode(this, 0, 0, null, 0.0);
    BBThreadPool.getPool().runWhenBlocked();  // run on current thread if no thread available in pool
    BBThreadPool.getPool().setKeepAliveTime(-1);  // keep threads alive for ever
  }


  public void run() throws InterruptedException {
    BBThreadPool.getPool().execute(_root);
    while (!_root.isDone()) {
      Thread.currentThread().sleep(100);
    }
    System.out.println("Opt Soln found: "+_bound);
  }


  public Vector getDocs() { return _docs; }
  public int getDocSize() { return _docsz; }


  public int getK() { return _k; }


  public int getParLvl() { return _parLvl; }


  public int[] getInitSolution() { return _initsoln; }


  public DocumentDistIntf getMetric() { return _metric; }


  public void setMaxNodesAllowed(int n) {
    _maxNodesAllowed = n;
  }
  public int getMaxNodesAllowed() { return _maxNodesAllowed; }


  synchronized public void setIncumbent(BBNode n) {
    _incumbent = n;
    _bound = n.getCost();
    System.err.println("new soln found w/ val="+_bound);
  }


  public int[] getSolution() {
    int inds[] = new int[_docsz];
    BBNode ptr = _incumbent;
    while (ptr!=null) {
      inds[ptr.getPos()] = ptr.getClusterIndex();
      ptr = ptr.getParent();
    }
    return inds;
  }


  synchronized public void setBound(double b) {
    _bound = b;
  }


  synchronized public double getBound() { return _bound; }

}

