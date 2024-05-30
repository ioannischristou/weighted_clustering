package packing;

import coarsening.*;
import utils.*;
import java.util.*;

public class GRASPPacker {
  private Graph _g;
  private Node[] _nodes;  // Pair<Integer id, Node n>

  public GRASPPacker(Graph g) {
    _g = g;
    // 1. create a sorted array of the nodes in order of connectedness
    // so as to apply the GRASP heuristic then
    final int gsz = _g.getNumNodes();
    _nodes = new Node[gsz];
    for (int i=0; i<gsz; i++) {
      _nodes[i] = _g.getNode(i);
    }
    _g.makeNNbors();
    //System.err.println("done making dist-2 nbors");  // itc: HERE rm asap
    Comparator comp = new NodeComparator();
    Arrays.sort(_nodes, comp);
    //System.err.println("done sorting");  // itc: HERE rm asap
  }


  /**
   * create a dist-2 packing for Graph _g via a GRASP method and return it
   * as a Set<Integer posid> indicating the nodeids of the active nodes.
   * @param init Set<Node n>
   * @param addfirstfrom Set<Node n>
   * @return Set<Node n>
   */
  public Set pack(Set init, Set addfirstfrom) {
    Set res = new HashSet(init);
    if (addfirstfrom!=null) {
      Iterator it = addfirstfrom.iterator();
      while (it.hasNext()) {
        Node n = (Node) it.next();
        if (isFree2Cover(n, res)) {
          res.add(n);
        }
      }
    }
    final int gsz = _g.getNumNodes();
    // 2. create roullete wheel
    double[] nodewheelpos = new double[gsz];
    nodewheelpos[0] = 1.0 / (double) (_nodes[0].getNNbors().size()+1);
    double totfitness = nodewheelpos[0];
    for (int i=1; i<gsz; i++) {
      nodewheelpos[i] = nodewheelpos[i-1] +
                        1.0 / (double) (_nodes[i].getNNbors().size()+1);
      totfitness += nodewheelpos[i];
    }
    for (int i=0; i<gsz; i++)
      nodewheelpos[i] /= totfitness;
    // 3. now start running the roullette to select nodes to activate
    boolean cont = true;
    while (cont) {
      double pos = RndUtil.getInstance().getRandom().nextDouble();
      // which node has the pos?
      for (int i=0; i<gsz; i++) {
        if (pos <= nodewheelpos[i]) {
          // ok, find the first node nearest to i that is ok to cover
          boolean ok = false;
          for (int j=i; j>=0; j--) {
            Node nj = _nodes[j];
            if (isFree2Cover(nj, res)) {
              res.add(nj);
              ok = true;
              break;
            }
          }
          if (!ok) {
            for (int j=i+1; j<gsz; j++) {
              Node nj = _nodes[j];
              if (isFree2Cover(nj, res)) {
                res.add(nj);
                ok = true;
                break;
              }
            }
          }
          if (!ok) cont = false;
          break;
        }
      }
    }
    return res;
  }


  public Set packAndRelink(int num_tries, int num_combs) {
    int best = 0;
    double avg = 0;
    Vector sols = new Vector();  // store sols here
    Set best_set=null;
    for (int i=0; i<num_tries; i++) {
      Set init = new TreeSet();
      Set active = pack(init,null);
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
   * @return Set a Set<Node n>
   */
  private Set restore(Set invalid, Set addfirst) {
    //System.err.println("invalid.size()="+invalid.size());
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
    Set fin = pack(result, addfirst);  // now pack as much as possible
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


  public static void main(String[] args) {
    try {
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      int num_tries = Integer.parseInt(args[1]);
      int num_combs = Integer.parseInt(args[2]);
      int best = 0;
      double avg = 0;
      Vector sols = new Vector();  // store sols here
      GRASPPacker p = new GRASPPacker(g);
      for (int i=0; i<num_tries; i++) {
        Set init = new TreeSet();
        Set active = p.pack(init,null);
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
        int r1 = RndUtil.getInstance().getRandom().nextInt(sols.size());
        int r2 = RndUtil.getInstance().getRandom().nextInt(sols.size());
        Set a1 = new HashSet((Set) sols.elementAt(r1));
        Set a2 = (Set) sols.elementAt(r2);
        Set a21 = new HashSet(a2);
        a21.removeAll(a1);
        a1.addAll(a2);
        Set res = p.restore(a1, a21);
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
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

class NodeComparator implements Comparator {
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
      else if (n1s==n2s) return 0;
      else return 1;
    }
    else return 1;
  }
}

