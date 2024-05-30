package packing;

import coarsening.*;
import utils.*;
import java.util.*;
import java.io.*;

public class GASPPacker {
  private Graph _g;
  private TreeSet _nodesq=null;  // TreeSet<Node>

  public GASPPacker(Graph g) {
    _g = g;
    // 1. create a priority queue of the nodes in order of connectedness
    // so as to apply the GASP heuristic then
    _g.makeNNbors();
    final int gsz = _g.getNumNodes();
    NodeComparator2 comp = new NodeComparator2();
    //System.err.println("done making dist-2 nbors");  // itc: HERE rm asap
    _nodesq = new TreeSet(comp);
    for (int i=0; i<gsz; i++) {
      _nodesq.add(g.getNode(i));
    }
    //System.err.println("done sorting");  // itc: HERE rm asap
  }


  /**
   * create a dist-2 packing for Graph _g via a GASP method and return it
   * as a Set<Node> of the active nodes.
   * @return Set  // Set<Node>
   */
  public Set pack() {
    Set res = new HashSet();  // Set<Node>
    boolean cont = true;
    while (cont) {
      cont = false;
      // 1. get the first valid element from the queue
      Iterator it = _nodesq.iterator();
      while (it.hasNext()) {
        Node n = (Node) it.next();
        if (isFree2Cover(n, res)) {
          // 2. update the queue
          updateQueue(n);
          // 3. add the element to the result set
          res.add(n);
          cont = true;
          break;
        }
      }
    }
    return res;
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set<Node>
   * @return boolean // true iff nj can be added to active
   */
  private boolean isFree2Cover(Node nj, Set active) {
    if (active.contains(nj)) return false;
    Iterator it = active.iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      if (n.getNNbors().contains(nj)) return false;
    }
    return true;
  }


  private void updateQueue(Node n) {
    // 0. remove the node n and the nnbors of n from _nodesq
    _nodesq.remove(n);
    Set nnbors = n.getNNbors();
    _nodesq.removeAll(nnbors);
    // 1. create the nnnbors set of the nbors of _nnbors U n set
    Set nnnbors = new HashSet();  // Set<Node>
    Set nbors = n.getNbors();
    Iterator it = nbors.iterator();
    while (it.hasNext()) {
      Node nbor = (Node) it.next();
      Set nnbors2 = nbor.getNNbors();
      nnnbors.addAll(nnbors2);
    }
    nnnbors.removeAll(nnbors);
    nnnbors.remove(n);
    // 3. remove the nnnbors nodes from the _nodesq set and re-insert them
    // (which updates correctly the _nodesq TreeSet)
    // nnnbors are all the nodes at distance 3 from the node n.
    // Update the _nnbors data member of those nodes.
    _nodesq.removeAll(nnnbors);
    Iterator it2 = nnnbors.iterator();
    while (it2.hasNext()) {
        Node nb = (Node) it2.next();
        nb.getNNbors().removeAll(nnbors);
        nb.getNNbors().remove(n);
    }
    _nodesq.addAll(nnnbors);
  }

  /**
   * return true iff all nodes in active set can be set to one without
   * violating feasibility.
   * @param active Set  // Set<Node>
   * @return boolean
   */
  private boolean isFeasible(Set active) {
    _g.makeNNbors();  // re-establish nnbors
    final int gsz = _g.getNumNodes();
    for (int i=0; i<gsz; i++) {
      Node nn = _g.getNode(i);
      Set nnbors = nn.getNbors();  // Set<Node>
      int count=0;
      if (active.contains(nn)) count=1;
      Iterator it2 = active.iterator();
      while (it2.hasNext()) {
        Node n2 = (Node) it2.next();
        if (nnbors.contains(n2)) {
          ++count;
          if (count>1) return false;
        }
      }
    }
    return true;
  }


  public static void main(String[] args) {
    try {
      long st = System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      int best = 0;
      GASPPacker p = new GASPPacker(g);
      Set s = p.pack();  // Set<Node>
      best = s.size();
      long tot = System.currentTimeMillis()-st;
      System.out.println("Final Best soln found="+best+" total time="+tot+" (msecs)");
      //if (p.isFeasible(s)) System.out.println("feasible soln");
      //else System.err.println("infeasible soln");
      // write solution to file
      PrintWriter pw = new PrintWriter(new FileWriter("sol.out"));
      pw.println(best);
      Iterator it = s.iterator();
      while (it.hasNext()) {
          Node n = (Node) it.next();
          pw.println((n.getId()+1));
      }
      pw.flush();
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

class NodeComparator2 implements Comparator {
  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
    int n1sz = n1.getNNbors().size();
    int n2sz = n2.getNNbors().size();
    if (n1sz < n2sz) return -1;
    else if (n1sz==n2sz) {
      int n1s = n1.getNbors().size();
      int n2s = n2.getNbors().size();
      if (n1s<n2s) return -1;
      else if (n1s==n2s) {
        if (n1.getId()<n2.getId()) return -1;
        else if (n1.getId()==n2.getId()) return 0;
        else return 1;
      }
      else return 1;
    }
    else return 1;
  }
}

