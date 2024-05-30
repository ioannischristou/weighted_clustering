package onedclustering;

import java.util.*;

public class Solver {
  private ClusterSet _allclusters;
  private Params _params;
  private double _incV = Double.MAX_VALUE;
  private ClusterSet _incumbent = null;

  int _numiters = 0;

  public Solver(Params p) {
    _params = p;
  }


  public ClusterSet makeAllClusters() throws CException {
    ClusterSet result = new ClusterSet(new HashSet());
    for (int cs = 1; cs<=_params.getSequenceLength(); cs++) {
      for (int i=0; i<_params.getSequenceLength(); i++) {
        if (i+cs>_params.getSequenceLength()) break;
        Cluster s = new Cluster();
        for (int j=i; j<i+cs; j++) {
          // if (j>=Params.getSequenceLength()) break;
          s.add(new Integer(j));
        }
        result.addClusterNoCheck(s);
      }
    }
    return result;
  }


  public double solveDP() throws CException {
    _allclusters = makeAllClusters();

    final int n = _params.getSequenceLength();
    final int M = _params.getM();
    double part[][] = new double[n][M];
    ClusterSet partcs[][] = new ClusterSet[n][M];
    // n = Params.getSequenceLength(), M = Params.getM()
    // set values for P[n-1,j] j=0...M-1
    part[n-1][0] = 0;
    partcs[n-1][0] = new ClusterSet(new Integer(n-1));
    for (int i=1; i<M-1; i++) {
      part[n - 1][i] = Double.POSITIVE_INFINITY;
    }
    // set values for P[i,0] i=0...n-1
    for (int i=0; i<=n-2; i++) {
      part[i][0] = _allclusters.getCluster(i, n-1).evaluate(_params);
      partcs[i][0] = new ClusterSet(i, n-1);
    }
    // recursive relationship is
    // P[i,k] = min_{j=i,...,n-1}(c[i,j]+P[j+1,k-1])
    // with java indices it becomes
    // part[i][k] = min_{j=i,...,n-2}(cost(i,j)+part[j+1][k-1]
    _numiters = 0;
    for (int i=n-2; i>=0; i--) {
      for (int k=1; k<M; k++) {
        if (i+k==n-1) {
          part[i][k] = 0;
          // store the ClusterSet {{i}, {i+1},...,{n-1}}
          ClusterSet cs = new ClusterSet(new Integer(i));
          for (int ii=i+1; ii<=n-1; ii++) {
            Cluster c = new Cluster();
            c.add(new Integer(ii));
            cs.addCluster(c);
          }
          partcs[i][k] = cs;
        }
        else if (i+k>n-1) part[i][k] = Double.POSITIVE_INFINITY;
        else {
          // i+k<n-1
          double bv = Double.MAX_VALUE;
          int bind = -1;
          for (int j = i; j <= n - 2; j++) {
            ++_numiters;
            double costij = _allclusters.getCluster(i, j).evaluate(_params);
            if (bv > costij + part[j+1][k-1]) {
              bv = costij + part[j+1][k-1];
              bind = j;
            }
          }
          part[i][k] = bv;
          // store partition cluster
          ClusterSet csij = new ClusterSet(i,bind);
          ClusterSet pj1k1 = partcs[bind+1][k-1];
          if (pj1k1==null) {
            System.err.println("i="+i+" k="+k+" bind+1="+(bind+1)+" k-1="+(k-1));
            System.err.println("csij="+csij);
            throw new CException("null pj1k1...");
          }
          csij.addClustersRight(pj1k1);
          partcs[i][k] = csij;
        }
      }
    }
    _incV = part[0][M-1];
    _incumbent = partcs[0][M-1];
    return _incV;
  }


  /**
   * this method is the same as solveDP() except the (i,k) loop runs
   * vertically instead of horizontally so that memory is saved
   * Thus the outer loop is k, the inner loop is i
   * @throws CException
   * @return double
   */
  public double solveDP2() throws CException {
    _allclusters = makeAllClusters();

    final int n = _params.getSequenceLength();
    final int M = _params.getM();
    double part[][] = new double[n][M];
    ClusterSet partcs[][] = new ClusterSet[n][M];
    // n = Params.getSequenceLength(), M = Params.getM()
    // set values for P[n-1,j] j=0...M-1
    part[n-1][0] = 0;
    partcs[n-1][0] = new ClusterSet(new Integer(n-1));
    for (int i=1; i<M-1; i++) {
      part[n - 1][i] = Double.POSITIVE_INFINITY;
    }
    // set values for P[i,0] i=0...n-1
    for (int i=0; i<=n-2; i++) {
      part[i][0] = _allclusters.getCluster(i, n-1).evaluateWF(_params);
      partcs[i][0] = new ClusterSet(i, n-1);
    }
    // recursive relationship is
    // P[i,k] = min_{j=i,...,n-1}(c[i,j]+P[j+1,k-1])
    // with java indices it becomes
    // part[i][k] = min_{j=i,...,n-2}(cost(i,j)+part[j+1][k-1]
    _numiters = 0;
    for (int k=1; k<M; k++) {
      // garbage collect k-2
      if (k-2>=0) {
        for (int i=0; i<n; i++)
          partcs[i][k-2] = null;
      }
      // the main DP loops
      for (int i=n-2; i>=0; i--) {
        if (i+k==n-1) {
          part[i][k] = 0;
          // store the ClusterSet {{i}, {i+1},...,{n-1}}
          ClusterSet cs = new ClusterSet(new Integer(i));
          for (int ii=i+1; ii<=n-1; ii++) {
            Cluster c = new Cluster();
            c.add(new Integer(ii));
            cs.addCluster(c);
          }
          partcs[i][k] = cs;
        }
        else if (i+k>n-1) part[i][k] = Double.POSITIVE_INFINITY;
        else {
          // i+k<n-1
          double bv = Double.POSITIVE_INFINITY;
          int bind = -1;
          for (int j = i; j <= n - 2; j++) {
            ++_numiters;
            double costij = _allclusters.getCluster(i, j).evaluateWF(_params);
            if (bv > costij + part[j+1][k-1]) {
              bv = costij + part[j+1][k-1];
              bind = j;
            }
          }
          part[i][k] = bv;
          // store partition cluster
          ClusterSet pj1k1 = partcs[bind+1][k-1];
          if (bind==-1) {
            // infeasible
            partcs[i][k] = null;
          }
          else if (pj1k1==null) {
            // sanity check
            System.err.println("i="+i+" k="+k+" bind+1="+(bind+1)+" k-1="+(k-1));
            // System.err.println("csij="+csij);
            throw new CException("Solver.solveDP2() internal error...");
          }
          else {
            ClusterSet csij = new ClusterSet(i,bind);
            csij.addClustersRight(pj1k1);
            partcs[i][k] = csij;
          }
        }
      }
    }
    _incV = part[0][M-1];
    _incumbent = partcs[0][M-1];
    return _incV;
  }


  /**
   * this method is the same as solveDP2() except we use numthreads threads to
   * run the inner vertical loop for the i=n-2...0 computing the k-th column
   * values
   * @param int numthreads
   * @throws CException
   * @return double
   */
  public double solveDP2Parallel(int numthreads) throws CException {
    _allclusters = makeAllClusters();
    final int n = _params.getSequenceLength();
    final int M = _params.getM();
    double part[][] = new double[n][M];
    ClusterSet partcs[][] = new ClusterSet[n][M];
    final int interval = (n-1)/numthreads;
    // n = Params.getSequenceLength(), M = Params.getM()
    // set values for P[n-1,j] j=0...M-1
    part[n-1][0] = 0;
    partcs[n-1][0] = new ClusterSet(new Integer(n-1));
    for (int i=1; i<M-1; i++) {
      part[n - 1][i] = Double.POSITIVE_INFINITY;
    }
    // set values for P[i,0] i=0...n-1
    for (int i=0; i<=n-2; i++) {
      part[i][0] = _allclusters.getCluster(i, n-1).evaluateWF(_params);
      partcs[i][0] = new ClusterSet(i, n-1);
    }

    // Create the threads and start them
    SolverAuxThread[] threads = new SolverAuxThread[numthreads];
    for (int jj=0; jj<numthreads; jj++) {
      SolverAux rjj = new SolverAux(_params, part, partcs, _allclusters);
      threads[jj] = new SolverAuxThread(rjj);
      threads[jj].start();
    }

    // recursive relationship is
    // P[i,k] = min_{j=i,...,n-1}(c[i,j]+P[j+1,k-1])
    // with java indices it becomes
    // part[i][k] = min_{j=i,...,n-2}(cost(i,j)+part[j+1][k-1]
    // ++_numiters;
    for (int k=1; k<M; k++) {
      // garbage collect k-2
      if (k-2>=0) {
        for (int i=0; i<n; i++)
          partcs[i][k-2] = null;
      }
      // all threads except last have floor((n-1)/numthreads) work to do
      int starti = 0; int endi = interval;
      for (int t=0; t<numthreads-1; t++) {
        SolverAux rt = threads[t].getSolverAux();
        rt.runFromTo(starti, endi, k);
        starti = endi+1;
        endi += interval;
      }
      // last thread
      SolverAux last_aux = threads[numthreads-1].getSolverAux();
      last_aux.runFromTo(starti, n-2, k);
      // System.err.println("column k="+k+" done.");
      // wait for threads to finish their current task
      for (int t=0; t<numthreads; t++) {
        SolverAux rt = threads[t].getSolverAux();
        rt.waitForTask();
      }
    }
    // stop threads
    for (int t=0; t<numthreads; t++) {
      threads[t].getSolverAux().setFinish();
    }
/*
    // wait for threads to finish
    for (int t=0; t<numthreads; t++) {
      while (threads[t].isAlive()) {
        try {
          Thread.currentThread().sleep(100);
        }
        catch (InterruptedException e) { }
      }
    }
*/
    _incV = part[0][M-1];
    _incumbent = partcs[0][M-1];
    return _incV;
  }


  /**
   * this method is the same as solveDP2Parallel() using numthreads threads to
   * run the inner vertical loop for the i=n-2...0 computing the k-th column
   * values
   * It also doesnt' use ClusterSets, and so it's much faster.
   * @param int numthreads
   * @throws CException
   * @return double
   */
  public double solveDP2ParallelMat(int numthreads) throws CException {
    double allclustersMatrix[][] = makeAllClustersMatrix(_params);
    // cost of [i,j] stored in allclustersMatrix
    final int n = _params.getSequenceLength();
    final int M = _params.getM();
    double part[][] = new double[n][M];
    int partcs[][] = new int[n][M];

    final int interval = (n-1)/numthreads;
    // n = Params.getSequenceLength(), M = Params.getM()
    // set values for P[n-1,j] j=0...M-1
    part[n-1][0] = 0;
    // initialize partcs[][]:
    for (int i=0; i<n; i++) {
      for (int j=0; j<M; j++) partcs[i][j] = -1;
    }
    for (int i=1; i<M-1; i++) {
      part[n - 1][i] = Double.POSITIVE_INFINITY;
    }
    // set values for P[i,0] i=0...n-1
    for (int i=0; i<=n-2; i++) {
      part[i][0] = allclustersMatrix[i][n-1];
      partcs[i][0] = n;
    }
    partcs[n-1][0] = n;

    // Create the threads and start them
    SolverAuxMatThread[] threads = new SolverAuxMatThread[numthreads];
    for (int jj=0; jj<numthreads; jj++) {
      SolverAuxMat rjj = new SolverAuxMat(_params, part, partcs, allclustersMatrix);
      threads[jj] = new SolverAuxMatThread(rjj);
      threads[jj].start();
    }

    // recursive relationship is
    // P[i,k] = min_{j=i,...,n-1}(c[i,j]+P[j+1,k-1])
    // with java indices it becomes
    // part[i][k] = min_{j=i,...,n-2}(cost(i,j)+part[j+1][k-1]
    // ++_numiters;
    for (int k=1; k<M; k++) {
      // all threads except last have floor((n-1)/numthreads) work to do
      int starti = 0; int endi = interval;
      for (int t=0; t<numthreads-1; t++) {
        SolverAuxMat rt = threads[t].getSolverAuxMat();
        rt.runFromTo(starti, endi, k);
        starti = endi+1;
        endi += interval;
      }
      // last thread
      SolverAuxMat last_aux = threads[numthreads-1].getSolverAuxMat();
      last_aux.runFromTo(starti, n-2, k);
      // System.err.println("column k="+k+" done.");
      // wait for threads to finish their current task
      for (int t=0; t<numthreads; t++) {
        SolverAuxMat rt = threads[t].getSolverAuxMat();
        rt.waitForTask();
      }
    }
    // stop threads
    for (int t=0; t<numthreads; t++) {
      threads[t].getSolverAuxMat().setFinish();
    }

    _incV = part[0][M-1];
    // compute incumbent using partcs
    _incumbent = new ClusterSet();
    int st = 0;
    int en;
    int k = M;
    while (k>0) {
      en = partcs[st][--k];
      if (en==n) --en;
      Cluster s = new Cluster();
      for (int i = st; i <= en; i++) {
        s.add(new Integer(i));
      }
      _incumbent.addClusterNoCheck(s);
      st = en+1;
    }

    return _incV;
  }


  /**
   * this method is the same as solveDP() except it doesn't use ClusterSets.
   * It is much faster.
   * @throws CException
   * @return double
   */
  public double solveDPMat() throws CException {
    double allclustersMatrix[][] = makeAllClustersMatrix(_params);
    // allclustersMatrix[n][n] has as value [i][j] the value of the
    // partition cluster containing the elements [i, i+1, ..., j-1, j]
    final int n = _params.getSequenceLength();
    final int M = _params.getM();
    double part[][] = new double[n][M];
    int partcs[][] = new int[n][M];  // partcs[i][k] = j where j is the
    // index such that the optimal partition from [i,...n] among k
    // blocks is the partition [i,...,j] and the optimal partition
    // from j+1,...,n among k-1 blocks.
    // initialize partcs[][]:
    for (int i=0; i<n; i++) {
      for (int j=0; j<M; j++) partcs[i][j] = -1;
    }
    // n = Params.getSequenceLength(), M = Params.getM()
    // set values for P[n-1,j] j=0...M-1
    part[n-1][0] = 0;
    partcs[n-1][0] = n;
    for (int i=1; i<M-1; i++) {
      part[n - 1][i] = Double.POSITIVE_INFINITY;
    }
    // set values for P[i,0] i=0...n-1
    for (int i=0; i<=n-2; i++) {
      part[i][0] = allclustersMatrix[i][n-1];
      partcs[i][0] = n;
    }
    // recursive relationship is
    // P[i,k] = min_{j=i,...,n-1}(c[i,j]+P[j+1,k-1])
    // with java indices it becomes
    // part[i][k] = min_{j=i,...,n-2}(cost(i,j)+part[j+1][k-1]
    _numiters = 0;
    for (int i=n-2; i>=0; i--) {
      for (int k=1; k<M; k++) {
        if (i+k==n-1) {
          part[i][k] = 0;
          partcs[i][k] = i;
        }
        else if (i+k>n-1) part[i][k] = Double.POSITIVE_INFINITY;
        else {
          // i+k<n-1
          double bv = Double.MAX_VALUE;
          int bind = -1;
          for (int j = i; j <= n - 2; j++) {
            ++_numiters;
            double costij = allclustersMatrix[i][j];
            if (bv > costij + part[j+1][k-1]) {
              bv = costij + part[j+1][k-1];
              bind = j;
            }
          }
          part[i][k] = bv;
          partcs[i][k] = bind;
        }
      }
    }
    _incV = part[0][M-1];
    _incumbent = null;
    // itc: HERE compute the best partition and create a ClusterSet for it in
    // _incumbent
    _incumbent = new ClusterSet();
    int st = 0;
    int en;
    int k = M;
    while (k>0) {
      en = partcs[st][--k];
      // System.err.println("going from ["+st+"..."+en+"]");  // itc: HERE rm ASAP
      if (en==n) --en;
      Cluster s = new Cluster();
      for (int i = st; i <= en; i++) {
        s.add(new Integer(i));
      }
      _incumbent.addClusterNoCheck(s);
      st = en+1;
    }
    return _incV;
  }


  public ClusterSet getSolutionSortedIndices() {
    return _incumbent;
  }


  public ClusterSet getSolutionWOrigIndices() {
    // modify new indices to original indices
    Vector clusters = _incumbent.getClusters();
    ClusterSet result = new ClusterSet();
    for (int i=0; i<clusters.size(); i++) {
      Cluster ci = (Cluster) clusters.elementAt(i);
      Cluster nc = new Cluster();
      Iterator it = ci.iterator();
      while (it.hasNext()) {
        Integer ii = (Integer) it.next();
        int orig_ind = _params.getOriginalIndexAt(ii.intValue());
        nc.add(new Integer(orig_ind));
      }
      result.addClusterNoCheck(nc);
    }
    return result;
  }


  public Vector getSolutionIndices() {
    // result vector is Vector<Integer> where
    // result[i] is a number in [1...k] indicating to which cluster the i-th
    // original element belongs to.
    final int n = _params.getSequenceLength();
    ClusterSet cs = getSolutionWOrigIndices();
    Vector result = new Vector();
    for (int i=0; i<n; i++) result.addElement(null);  // init.
    Vector clusters = cs.getClusters();
    for (int i=0; i<clusters.size(); i++) {
      Cluster ci = (Cluster) clusters.elementAt(i);
      Iterator cit = ci.iterator();
      while (cit.hasNext()) {
        Integer ii = (Integer) cit.next();
        result.set(ii.intValue(), new Integer(i+1));
      }
    }
    return result;
  }


  public double getIncumbentValue() { return _incV; }


  public int getNumIters() { return _numiters; }


  /**
   * return the matrix of dimensions n X n where [i,j] denotes the cost of the
   * cluster containing the contiguous values of indices [i, i+1, ..., j]
   * @param p Params
   * @return double[][]
   */
  private double[][] makeAllClustersMatrix(Params p) {
    final int n = p.getSequenceLength();
    // final int M = p.getM();
    double res[][] = new double[n][n];
    double sum=0.0;
    for (int i=0; i<n; i++) {  // i = row index
      sum = 0.0;
      for (int j=0; j<n; j++) {  // j= col index
        if (j<i) {
          res[i][j] = Double.POSITIVE_INFINITY;
        }
        else {
          // add the numbers from [i,...,j] together and divide by j-i+1
          // to get the ave
          sum += p.getSequenceValueAt(j);
          double ave = sum / (j-i+1.0);
          // now compute sum of distances from ave
          double v = 0.0;
          for (int k=i; k<=j;k++) {
            double dk = p.getMetric()==Params._L1 ?
                Math.abs(p.getSequenceValueAt(k) - ave) :
                (p.getSequenceValueAt(k) - ave)*(p.getSequenceValueAt(k) - ave);
            v += dk;
          }
          if (p.getMetric()==Params._L2) v = Math.sqrt(v);
          if (v > p.getP()) res[i][j] = Double.POSITIVE_INFINITY;
          else res[i][j] = v;
        }
      }
    }
    return res;
  }
}
