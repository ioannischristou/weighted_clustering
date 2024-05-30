package packing;

import java.util.*;

public class BBQueue extends Thread {
  private TreeSet _nodes;
  private int _maxSz;
  private boolean _isDone;


  public BBQueue(int max_nodes) {
    _nodes = new TreeSet();
    _isDone = false;
    _maxSz = max_nodes;
  }


  public void run() {
    while (true) {
      if (isDone()) return;  // done
      try {
        BBNode3 best = popNode();
        if (best == null) {
          System.err.println("BBQueue done, exits.");
          return; // done
        }
        BBThreadPool.getPool().execute(best);
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);  // oops...
      }
    }
  }


  synchronized boolean isDone() { return _isDone; }
  public synchronized void setDone() {
    _isDone=true;
    notify();
  }


  synchronized BBNode3 popNode() throws InterruptedException, PackingException {
    while (_nodes.size()==0) {
      if (_isDone) return null;
      wait();
    }
    BBNode3 n = (BBNode3) _nodes.first();
    boolean rm = _nodes.remove(n);
    if (rm==false)
      throw new PackingException("failed to remove node?");
    return n;
  }


  public synchronized boolean insertNode(BBNode2 n) {
    if (_nodes.size()<_maxSz) {
      _nodes.add(n);
      notify();
      return true;
    }
    return false;
  }
  public synchronized boolean insertNode(BBNode3 n) {
    if (_nodes.size()<_maxSz) {
      _nodes.add(n);
      notify();
      return true;
    }
    return false;
  }
  public synchronized boolean insertNodes(Vector children) {
    if (_nodes.size()>=_maxSz)
      return false;  // queue is full. Signal caller
    for (int i=0; i<children.size(); i++) {
      BBNode2 n = (BBNode2) children.elementAt(i);
      _nodes.add(n);
    }
    // System.err.println("BBQueue size="+_nodes.size());  // itc: HERE rm asap
    notify();
    return true;
  }
  public synchronized boolean insertNodes(TreeSet children) {
    if (_nodes.size()>=_maxSz)
      return false;  // queue is full. Signal caller
    Iterator it = children.iterator();
    while (it.hasNext()) {
      BBNode3 n = (BBNode3) it.next();
      _nodes.add(n);
    }
    notify();
    return true;
  }
}
