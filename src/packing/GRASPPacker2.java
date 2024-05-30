package packing;

import coarsening.*;
import utils.*;
import java.util.*;

public class GRASPPacker2 {
  private Graph _g;
  private TreeSet _nodesq=null;  // TreeSet<Node>

  private GRASPPacker2(Graph g) {
    _g = g;
  }


  /**
   * create a dist-2 packing for Graph _g via a GRASP method and return it
   * as a Set<Node n> indicating the nodeids of the active nodes.
   * The nodes are chosen with probability inversely proportional to their
   * "connectivity" on the network. The higher the 3rd argument (power), the
   * more likely it is for less connected nodes to be selected than highly
   * connected nodes. (when power=0, all nodes are equally likely to be selected)
   * @param init Set<Node n>
   * @param addfirstfrom Set<Node n>
   * @param power double
   * @return Set<Node n>
   */
  private Set pack(Set init, Set addfirstfrom, double power) {
    // init
    _g.makeNNbors(true);  // force recomputation of NNbors for each node
    // create a priority queue of the nodes in order of connectedness
    // so as to apply the GRASP heuristic then
    final int gsz = _g.getNumNodes();
    NodeComparator2 comp = new NodeComparator2();
    //System.err.println("done making dist-2 nbors");  // itc: HERE rm asap
    _nodesq = new TreeSet(comp);
    for (int i=0; i<gsz; i++) {
      _nodesq.add(_g.getNode(i));
    }
    //System.err.println("done sorting");  // itc: HERE rm asap

    Set res = new HashSet();
    Iterator it = init.iterator();
    while (it.hasNext()) {  // we assume that init does not have conflicts
      Node n = (Node) it.next();
      updateQueue(n);
      res.add(n);
    }
    if (addfirstfrom!=null) {
      it = addfirstfrom.iterator();
      while (it.hasNext()) {
        Node n = (Node) it.next();
        if (isFree2Cover(n, res)) {
          updateQueue(n);
          res.add(n);
        }
      }
    }

    // create roullete wheel
    double[] nodewheelpos = new double[gsz];

    boolean cont = true;
    while (cont) {
      cont = false;
      // 1. get an element from the queue at random according to the
      // roulette wheel
      Iterator iter=_nodesq.iterator();
      nodewheelpos[0] = 1.0 / Math.pow((double) (((Node) iter.next()).getNNbors().size()+1), power);
      double totfitness = nodewheelpos[0];
      int ii=1;
      while (iter.hasNext()) {
        Node cur = (Node) iter.next();
        nodewheelpos[ii] = nodewheelpos[ii-1] +
                          1.0 / Math.pow((double) (cur.getNNbors().size()+1), power);
        totfitness += nodewheelpos[ii];
        ++ii;
      }
      for (int i=0; i<ii; i++) nodewheelpos[i] /= totfitness;
      int ntries=ii/10+1;
      for (int nt=0; nt<ntries; nt++) {
        double r = RndUtil.getInstance().getRandom().nextDouble();
        int pos = 0;
        for (int i = 0; i < ii; i++) {
          if (r <= nodewheelpos[i]) {
            pos = i;
            break;
          }
        }
        // get the element at pos-th position
        it = _nodesq.iterator();
        while (pos > 0) {
          it.next();
          --pos;
        }
        Node n = (Node) it.next();
        if (isFree2Cover(n, res)) {
          // 2. update the queue
          updateQueue(n);
          // 3. add the element to the result set
          res.add(n);
          cont = true;
          break;  // break the for-loop searching for a valid node
        }
      }
      if (cont==false) {  // didn't find a valid node through chance
        // resort to standard greedy approach
        it = _nodesq.iterator();
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
    }
    return res;
  }


  /**
   *
   * @param num_tries int
   * @param num_combs int
   * @return Set<Integer nid>
   */
  private Set packAndRelink(int num_tries, int num_combs) {
    int best = 0;
    double avg = 0;
    Vector sols = new Vector();  // store sols here
    Set best_set=null;
    for (int i=0; i<num_tries; i++) {
      Set init = new TreeSet();
      Set active = pack(init,null,1);
      sols.addElement(active);
      System.err.println("Soln found w/ value="+active.size());
      if (active.size()>best) {
        best = active.size();
        best_set = active;
      }
      avg += active.size();
    }
    avg /= num_tries;
    int n = num_tries;
    System.err.println("Best soln found in initial pool="+best);
    // now combine sols
    System.err.println("combining sols");
    for (int i=0; i<num_combs; i++) {
      //int r1 = RndUtil.getInstance().getRandom().nextInt(sols.size());
      //int r2 = RndUtil.getInstance().getRandom().nextInt(sols.size());
      int r1 = sols.size()/2 + (int) Math.round(sols.size()*RndUtil.getInstance().getRandom().nextGaussian()/6.0);
      if (r1<0) r1 = 0;
      else if (r1>=sols.size()) r1 = sols.size()-1;
      int r2 = sols.size()/2 + (int) Math.round(sols.size()*RndUtil.getInstance().getRandom().nextGaussian()/6.0);
      if (r2<0) r2 = 0;
      else if (r2>=sols.size()) r2 = sols.size()-1;
      Set a1 = new HashSet((Set) sols.elementAt(r1));
      Set a2 = (Set) sols.elementAt(r2);
      Set a21 = new HashSet(a2);
      a21.removeAll(a1);
      a1.addAll(a2);
      Set res1 = restore(a1, a21);
      if (res1.size()>best) {
        best = res1.size();
        System.err.println("found better soln="+best);
        best_set = res1;
      }
      int sz = sols.size();
      avg = avg*n+sols.size();
      ++n;
      avg /= n;
      boolean addres1 = res1.size()>avg;
      // now go the other way around
      a1 = (Set) sols.elementAt(r1);
      a2 = new HashSet((Set) sols.elementAt(r2));
      Set a12 = new HashSet(a1);
      a12.removeAll(a2);
      a2.addAll(a1);
      Set res2 = restore(a2, a12);
      if (res2.size()>best) {
        best = res2.size();
        System.err.println("found better soln="+best);
        best_set = res2;
      }
      sz = sols.size();
      avg = avg*n+sols.size();
      ++n;
      avg /= n;
      boolean addres2 = res2.size()>avg;
      if (addres1)  // only add above average solutions
        sols.insertElementAt(res1, sz/2);  // insert element in the middle
      if (addres2)  // only add above average solutions
        sols.insertElementAt(res2, sz/2);  // insert element in the middle
    }
    // return a set of nodeids
    Set res = new HashSet();
    Iterator it = best_set.iterator();
    while (it.hasNext()) {
      Node nit = (Node) it.next();
      res.add(new Integer(nit.getId()));
    }
    return res;
  }


  /**
   * return true iff adding nj to active is not infeasible
   * @param nj Node
   * @param active Set  // Set<Node>
   * @return boolean
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


  /**
   * removes nodes from the arg set until the remaining nodes are a valid
   * packing using a greedy approach
   * @param invalid Set Set<Node n>
   * @param addfirst Set Set<Node n>
   * @return Set  // Set<Node n>
   */
  private Set restore(Set invalid, Set addfirst) {
    //System.err.println("invalid.size()="+invalid.size());
    _g.makeNNbors(true);
    Set result = new HashSet();
    Iterator it = invalid.iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      result.add(n);
    }
    // now start removing nodes
    boolean cont = true;
    while (cont) {
      cont = false;
      Node remove = null;
      int besthits = 0;
      it = result.iterator();
      while (it.hasNext()) {
        Node n = (Node) it.next();
        int hits = conflicts(n, result);
        if (hits>besthits) {
          remove = n;
          besthits = hits;
        }
      }
      if (remove!=null) {
        result.remove(remove);
        cont = true;
      }
    }
    Set fin = pack(result, addfirst, 1);  // now pack as much as possible
    return fin;
  }


  /**
   * create a new solution by guiding from Set towards the to Set.
   * @param from Set
   * @param to Set
   * @return Set
   */
  private Set restore2(Set from, Set to) {
    _g.makeNNbors(true);
    Set res = new HashSet(from);
    Set a2 = new TreeSet(new NodeComparator2());
    a2.addAll(to);
    a2.removeAll(from);
    for(int nt=0; nt<10; nt++) {
      if (a2.size()==0) break;
      int r = RndUtil.getInstance().getRandom().nextInt(a2.size());
      Iterator it = a2.iterator();
      while (r-->0) {
        it.next();
      }
      Node n = (Node) it.next();
      Set conflicts = getConflicts(n, res);
      if (conflicts.size()<3) {  // itc: HERE rm asap
        res.removeAll(conflicts);
        res.add(n);
        a2.remove(n);
        nt=0;  // success, let the loop execute another 10 times
      }
    }
    // complete the packing
    Set fin = pack(res, a2, 1);
    return fin;
  }


  private int conflicts(Node n, Set nodes) {
    int res = 0;
    Iterator it = nodes.iterator();
    while (it.hasNext()) {
      Node no = (Node) it.next();
      if (no==n) continue;
      Set nnbors = no.getNNbors();
      if (nnbors.contains(n)) res++;
    }
    return res;
  }


  private Set getConflicts(Node n, Set nodes) {
    Set res = new HashSet();
    Iterator it = nodes.iterator();
    while (it.hasNext()) {
      Node nit = (Node) it.next();
      if (nit.getNNbors().contains(n))
        res.add(nit);
    }
    return res;
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


  public static void main(String[] args) {
    try {
      long start=System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      int num_tries = Integer.parseInt(args[1]);
      int num_combs = Integer.parseInt(args[2]);
      int best = 0;
      double avg = 0;
      Vector sols = new Vector();  // store sols here
      GRASPPacker2 p = new GRASPPacker2(g);
      for (int i=0; i<num_tries; i++) {
        Set init = new TreeSet();
        Set active = p.pack(init,null, (double)2*i/(double)num_tries);
        sols.addElement(active);
        System.err.println("Soln found w/ value="+active.size());
        if (active.size()>best) {
          best = active.size();
        }
        avg += active.size();
      }
      avg /= num_tries;
      int n = num_tries;
      System.err.println("Best soln found in initial pool="+best);
      // now combine sols
      System.err.println("combining sols");
      for (int i=0; i<num_combs; i++) {
        //int r1 = RndUtil.getInstance().getRandom().nextInt(sols.size());
        //int r2 = RndUtil.getInstance().getRandom().nextInt(sols.size());
        int r1 = sols.size()/2 + (int) Math.round(sols.size()*RndUtil.getInstance().getRandom().nextGaussian()/6.0);
        if (r1<0) r1 = 0;
        else if (r1>=sols.size()) r1 = sols.size()-1;
        int r2 = sols.size()/2 + (int) Math.round(sols.size()*RndUtil.getInstance().getRandom().nextGaussian()/6.0);
        if (r2<0) r2 = 0;
        else if (r2>=sols.size()) r2 = sols.size()-1;
        Set a1 = new HashSet((Set) sols.elementAt(r1));
        Set a2 = (Set) sols.elementAt(r2);
        // Set a21 = new HashSet(a2);
        Set a21 = new TreeSet(new NodeComparator2());
        a21.addAll(a2);
        a21.removeAll(a1);
        a1.addAll(a2);
        Set res = p.restore(a1, a21);
/*
        Set a1 = (Set) sols.elementAt(r1);
        Set a2 = (Set) sols.elementAt(r2);
        Set res = p.restore2(a1,a2);
*/
        System.err.println("sol found w/ value="+res.size());
        if (res.size()>best) best = res.size();
        int sz = sols.size();
        avg = avg*n+sols.size();
        ++n;
        avg /= n;
        if (res.size()>avg)  // only add above average solutions
          sols.insertElementAt(res, sz/2);  // insert element in the middle
      }
      System.out.println("Final Best soln found="+best);
      long dur = System.currentTimeMillis()-start;
      System.out.println("total time: "+dur+" msecs.");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

