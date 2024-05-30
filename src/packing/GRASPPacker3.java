package packing;

import coarsening.*;
import utils.*;
import qs.*;
import java.io.*;
import java.util.*;

public class GRASPPacker3 {
  private Graph _g;
  private TreeSet _nodesq=null;  // TreeSet<Node>
  private HashSet _activeNodeIds=null;  // Set<Integer nid>

  private GRASPPacker3(Graph g) {
    _g = g;
  }


  private Set pack(Set init, Set addfirstfrom, double power) throws PackingException {
    return pack(init, addfirstfrom, power, Integer.MAX_VALUE);
  }
  /**
   * create a dist-2 packing for Graph _g via a GRASP method and return it
   * as a Set<Node n> indicating the nodeids of the active nodes.
   * The nodes are chosen with probability inversely proportional to their
   * "connectivity" on the network. The higher the 3rd argument (power), the
   * more likely it is for less connected nodes to be selected than highly
   * connected nodes. (when power=0, all nodes are equally likely to be selected)
   * Finally, the fourth param specifies when to stop the (random) greedy process
   * solve the LP relaxation of the current problem, so as to update the nodes'
   * weights and re-adjust the _nodesQueue.
   * @param init Set<Node n>
   * @param addfirstfrom Set<Node n>
   * @param power double
   * @param count int
   * @return Set<Node n>
   * @throws PackingException
   */
  private Set pack(Set init, Set addfirstfrom, double power, int count)
      throws PackingException {
    // init
    final int gsz = _g.getNumNodes();
    _g.makeNNbors(true);  // force recomputation of NNbors for each node
    // create a priority queue of the nodes in order of connectedness
    // so as to apply the GRASP heuristic then
    NodeComparator2 comp = new NodeComparator2();  // can be NodeComparator4
    _nodesq = new TreeSet(comp);
    _activeNodeIds = new HashSet();
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
    System.err.println("GRASPPAcker3.pack(): done adding nodes from 1st and 2nd args");  // itc: HERE rm asap
    // create roullete wheel
    double[] nodewheelpos = new double[gsz];

    boolean cont = true;
    int cnt=count;
    double[] x = null;
    while (cont) {
      if (--cnt<=0) {
        // solve LP, and use the GASP approach to select the next count nodes
        // 1. solve LP and update node weights
        System.err.println("GRASPPAcker3.pack(): solving LP");  // itc: HERE rm asap
        x = solveLP();
        System.err.println("GRASPPAcker3.pack(): done solving LP");  // itc: HERE rm asap
        cnt=count;
      }
      cont = false;
      // 1. get an element from the queue at random according to the
      // roulette wheel
      Iterator iter=_nodesq.iterator();
      Node n0 = (Node) iter.next();
      double nom = (x!=null) ? x[n0.getId()] : 1.0;
      // double nom = (x!=null && x[n0.getId()]>0.8) ? x[n0.getId()] : 0.0001;
      nodewheelpos[0] = Math.pow(nom / (double) (n0.getNNbors().size()+1), power);
      double totfitness = nodewheelpos[0];
      int ii=1;
      while (iter.hasNext()) {
        Node cur = (Node) iter.next();
        nom = (x!=null) ? x[cur.getId()] : 1.0;
        // nom = (x!=null && x[cur.getId()]>0.8) ? x[cur.getId()] : 0.0001;
        nodewheelpos[ii] = nodewheelpos[ii-1] +
                          Math.pow(nom / (double) (cur.getNNbors().size()+1), power);
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
   * the method sets up the following LP problem defined on a Graph G(V,E):
   *
   * max x1+...+xn
   * s.t.
   * sum_{j \in N(i)} x_j <= 1 forall i \in V
   * x_j >= 0 forall j \in V
   *
   * where the set N(i) is a maximal subset of nodes containing i so that all
   * nodes i, j in N(i) have d(i,j) <= 2.
   */
  private double[] solveLP() throws PackingException {
    try {
      Problem lp = new Problem("lppacker");
      lp.change_objsense(QS.MAX);
      final int n = _g.getNumNodes();
      String[] names = new String[n];
      for (int i=0; i<n; i++) {
        names[i] = "x"+(new Integer(i)).toString();
      }
      double[] lower = new double[n];
      double[] upper = new double[n];
      double[] obj = new double[n];
      for (int i=0; i<n; i++) {
        lower[i]=0;
        if (_activeNodeIds.contains(new Integer(i)))
          lower[i]=1;
        upper[i]=1;
        //double denomi = _g.getNode(i).getNbors().size()+1;  // avoid div by zero
        //obj[i]=1.0/denomi;
        obj[i]=1;
      }
      Set constraintsets = _g.getAllNborSets();
      int rows = constraintsets.size();
      Set[] constraints = new Set[rows];
      Iterator iter = constraintsets.iterator();
      int k=0;
      int l = 0;
      while (iter.hasNext()) {
        Set nodes = (Set) iter.next();
        // nodes defines a constraint on the problem
        constraints[k++] = nodes;
        l+=nodes.size();
      }
      // now the constraints array contains the constraints of the problem
      int cmatcnt[] = new int[n];
      int cmatbeg[] = new int[n];
      int cmatind[] = new int[l];
      double cmatval[] = new double[l];

      k=0;
      int p=0;
      cmatbeg[0]=0;
      for (int j=0; j<n; j++) {
        if (j>0) cmatbeg[j] = cmatbeg[j-1]+p;
        p=0;
        for (int i=0; i<rows; i++) {
          if (constraints[i].contains(new Integer(j))) {  // xj appears in row-i
            cmatcnt[j]++;
            cmatind[k] = i;
            cmatval[k] = 1;
            k++; p++;
          }
        }
      }
      int[] zero = new int[rows];
      char[] sense = new char[rows];
      for (int i=0; i<rows; i++) sense[i]='L';
      double[] rhs = new double[rows];
      String[] rnames = new String[rows];
      for (int i=0; i<rows; i++) {
        rhs[i] = 1;
        rnames[i] = "R"+i;
      }
      lp.add_rows(rows, zero, zero, null, null, rhs, sense, rnames);
      lp.add_cols(n, cmatcnt, cmatbeg, cmatind, cmatval, obj, lower, upper, names);
      // use SCIP to solve the LP
      lp.write_lp("tmp.lp");
      double[] x = new double[n];
      exec("bin\\scip.exe -f tmp.lp", x);
      return x;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PackingException("solveLP() failed...");
    }
  }


  /**
   * return true iff adding nj to active is not infeasible
   * @param nj Node
   * @param active Set  // Set<Node>
   * @return boolean
   */
  static private boolean isFree2Cover(Node nj, Set active) {
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
   * @throws PackingException
   */
  private Set restore(Set invalid, Set addfirst) throws PackingException {
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
   * @throws PackingException
   */
  private Set restore2(Set from, Set to) throws PackingException {
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
    _activeNodeIds.add(new Integer(n.getId()));
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
   * Executes the LP solver as a separate process
   */
  private int exec(String cmdLine, double[] solution) throws IOException {
    // zero-out solution
    for (int i=0; i<solution.length; i++) solution[i] = 0;
    int retCode = 1;              // Process return code
    BufferedReader in = null;
    Runtime rt = null;
    // String cmdLine = null;
    // Get a Runtime instance
    rt = Runtime.getRuntime();
    // Get the child process
    Process child = rt.exec(cmdLine);
    // Get input streams for the child process
    in = new BufferedReader(new InputStreamReader(child.getInputStream()));
    // Loop until the child process is finished.
    boolean finished = false;
    String inString;
    do {
      try {
        // Read any data that the child process has written to stdout.
        // This is necessary to prevent the child process from blocking.
        while (in.ready()) {
          inString = in.readLine();
          System.out.println(inString);
          // check to see if line is part of the solution
          if (inString.startsWith("x")) {
            StringTokenizer st = new StringTokenizer(inString);
            String varname = st.nextToken();
            String varvalue = st.nextToken();
            double val = Double.parseDouble(varvalue);
            int setindex = Integer.parseInt(varname.substring(1));
            solution[setindex] = val;
          }
        }
        // Attempt to get the exit code
        retCode = child.exitValue();
        finished = true;

        // If the process is not finished, an attempt to get the exit code
        // will throw the IllegalThreadStateException. Catch this and sleep for
        // 250 msec before trying again.
      } catch (IllegalThreadStateException e) {
        try {
          java.lang.Thread.currentThread().sleep(250);
        } catch (InterruptedException e1) {}
      }
    } while (!finished);
    return retCode;
  }


  private Set getRandomSubset(int size) {
    Set res = new TreeSet(new NodeComparator2());
    for (int i=0; i<size; i++) {
      int rpos = RndUtil.getInstance().getRandom().nextInt(_g.getNumNodes());
      Node nr = _g.getNode(rpos);
      res.add(nr);
    }
    return res;
  }


  public static void main(String[] args) {
    try {
      long start=System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      final int gsz = g.getNumNodes();
      int num_tries = Integer.parseInt(args[1]);
      int num_combs = Integer.parseInt(args[2]);
      double perc = 1.5;  // every when to switch to using the LP relaxation
                           // to rank nodes
      if (args.length>3)
        perc = Double.parseDouble(args[3]);
      double avg = 0;
      Vector sols = new Vector();  // store sols here
      double[] freqs = new double[gsz];  // store frequency of occurence in sols
      for (int i=0; i<gsz; i++) freqs[i]=0;
      // first run GASPPacker to obtain an estimate of what the max. no active
      // nodes is
      GASPPacker gp = new GASPPacker(g);
      Set est = gp.pack();
      sols.addElement(est);
      int best = est.size();
      Set bs = est;
      Iterator pit = est.iterator();
      while (pit.hasNext()) {
        Node ni = (Node) pit.next();
        freqs[ni.getId()] += est.size();
      }
      final long switchcount=Math.round(est.size()*perc);
      GRASPPacker3 p = new GRASPPacker3(g);
      for (int i=0; i<num_tries; i++) {
        Set init = new TreeSet();
        Set addfirstfrom = p.getRandomSubset(best/100);  // pick 1% of best estimated size randomly
        Set active = p.pack(init, addfirstfrom, (double)2.5*i/num_tries, (int) switchcount);
        pit = active.iterator();
        while (pit.hasNext()) {
          Node ni = (Node) pit.next();
          freqs[ni.getId()] += active.size();
        }
        sols.addElement(active);
        System.err.println("Soln found w/ value="+active.size());
        if (active.size()>best) {
          best = active.size();
          bs = active;
        }
        avg += active.size();
      }
      avg /= num_tries;
      int n = num_tries;
      System.err.println("Best soln found in initial pool="+best);
      // now get a solution based on the frequency of occurence
      g.makeNNbors(true);
      Node[] nodesq2 = new Node[gsz];
      for (int i=0; i<gsz; i++) {
        nodesq2[i] = g.getNode(i);
        nodesq2[i].setWeight("freq",new Double(freqs[i]/(num_tries+1)));
      }
      NodeComparator3 comp3 = new NodeComparator3();
      Arrays.sort(nodesq2, comp3);
      HashSet sol2 = new HashSet();
      for (int i=0; i<gsz; i++) {
        Node ni = nodesq2[i];
        if (isFree2Cover(ni, sol2)) sol2.add(ni);
      }
      sols.add(sol2);
      if (sol2.size()>best) {
        best = sol2.size();
        bs = sol2;
      }
      System.err.println("frequency-based sorting solution: "+sol2.size());
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
        if (res.size()>best) {
          best = res.size();
          bs = res;
        }
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
      PrintWriter pw = new PrintWriter(new FileWriter("sol.out"));
      pw.println(best);
      Iterator it = bs.iterator();
      while (it.hasNext()) {
          Node nn = (Node) it.next();
          pw.println((nn.getId()+1));
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


class NodeComparator3 implements Comparator {
  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
    double w1 = n1.getWeightValue("freq").doubleValue();
    double w2 = n2.getWeightValue("freq").doubleValue();
    if (w1<w2) return 1;
    else if (w1>w2) return -1;
    else return 0;
  }
}


class NodeComparator4 implements Comparator {
  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
    int n1sz = n1.getNbors().size();
    int n2sz = n2.getNbors().size();
    if (n1sz < n2sz) return -1;
    else if (n1sz==n2sz) {
      int n1s = n1.getNNbors().size();
      int n2s = n2.getNNbors().size();
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
