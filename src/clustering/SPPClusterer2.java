package clustering;

import partitioning.*;
import java.util.*;
import java.io.*;
import cern.colt.matrix.*;
//import cern.colt.matrix.impl.*;

/**
 * The class implements MSSC Clustering by Set Partitioning, as described in the 
 * EXAMCE paper in IEEE Transactions on Pattern Analysis and Machine 
 * Intelligence.
 * In particular, for each Document, a set of its closest neighbors is added -in 
 * the form of columns in a DoubleMatrix2D matrix- to the matrix A that will be 
 * given as the constraints set in a Set Covering Problem.
 * For the biggest set (in cardinality) we may also add its complement to ensure
 * covering solutions with only 2 columns.
 * The objective value of each set (column) is basically the sum of (square) 
 * distances of each Document from the center of the set and is used to populate 
 * a vector c -a DoubleMatrix1D object. 
 * After the problem is solved, many heuristic procedures can be used to 
 * translate the problem from Set Covering to Set Partitioning. This is because 
 * every Set Covering Problem solution for the Clustering Problem can be 
 * translated to a solution of the Set Partitioning Problem in many different 
 * ways: one heuristic is to find all multiple appearances of documents x in 
 * columns, and to remove them from all columns that contain Document x except 
 * from the set to which x is closest. For speed of execution, the center of 
 * each column may not be shifted, but instead, we can just remove from the 
 * objective value of the column the contribution of x. The result is a valid 
 * solution to the Set Partitioning Problem.
 * <p>Notes:
 * <ul>
 * <li>20220223: positive weights may be specified which result in a weighted
 * MSSC problem.
 * </ul>
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class SPPClusterer2 implements Clusterer {

  private static final double eps = 1.e-3;
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;

  public SPPClusterer2() {
  }


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   * <ul>
   * <li>&lt;"num_clusters", Integer&gt; number of clusters to create
   * <li>&lt;"theta",double&gt; threshold rate in distances for stopping adding 
   * documents far from the point which we currently consider as center
   * <li>&lt;"maxpointset", Integer&gt; the maximum number of columns to 
   * generate for a given data point -by adding more and more distant neighbors
   * <li>&lt;"TerminationCriteria",ClustererTermination&gt; the object that will
   * decide when to stop the iterations.
   * <li>&lt;"metric", DocumentDistIntf&gt; the metric distance.
   * <li>&lt;"scpmaker",SCPMaker&gt; the object encapsulating the algorithm for 
   * creating and converting the SCP set into eventually an SPP soln. 
   * Essentially implements the Strategy design pattern.
   * <li>&lt;"weights",double[]&gt; optional positive weights.
   * </ul>
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via 
   * <CODE>addAllDocuments(Vector&lt;Document&gt;)</CODE> 
   * or via repeated calls to <CODE>addDocument(Document d)</CODE>, and an 
   * initial clustering must be available via a call to
   * <CODE>setInitialClustering(Vector&lt;Document clusterCenters&gt;)</CODE>
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws ClustererException {
    final double theta = ( (Double) _params.get("theta")).doubleValue();
    final int maxpointset = ((Integer) _params.get("maxpointset")).intValue();
    final double[] weights = (double[]) _params.get("weights");
    // ClustererTermination ct = (ClustererTermination)
    //  _params.get("TerminationCriteria");
    // ct.registerClustering(this); // register this clustering problem with ct

    final DocumentDistIntf distmetric = (DocumentDistIntf)_params.get("metric");
    final SCPMakerIntf scpmaker = (SCPMakerIntf) _params.get("scpmaker");
    final EnhancedEvaluator finalevaluator = 
      (EnhancedEvaluator) _params.get("finalevaluator");
    final int n = _docs.size();
    final int k = ((Integer) _params.get("num_clusters")).intValue();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    long total_writing_time = 0;
    try {
      Vector new_clusters = new Vector();
      boolean cont = true;
      double old_val = Double.MAX_VALUE;
      boolean write_mps = true;
      Boolean write_mpsB = (Boolean) _params.get("writempsfile");
      if (write_mpsB!=null) write_mps = write_mpsB.booleanValue();
      // main algorithm to construct the A and c matrices

      // first run a conventional clusterer
      int numtries = 1;
      Integer numtriesI = (Integer) _params.get("numtries");
      if (numtriesI!=null) numtries = numtriesI.intValue();
      int nborexpansion = -1;
      Integer neI = (Integer) _params.get("nborexpansion");
      if (neI!=null) nborexpansion = neI.intValue();

      String mpsfile = (String) _params.get("mpsfile");
      if (mpsfile==null) mpsfile = "F:\\scp.mps";

      Clusterer improver = (Clusterer) _params.get("improver");
      if (improver==null) improver = new KMeansSqrGuarantClusterer();
      Evaluator eval = (Evaluator) _params.get("evaluator");
      if (eval==null) eval = new KMeansSqrEvaluator();
      if (eval instanceof EnhancedEvaluator) {
        ((EnhancedEvaluator) eval).setMasterDocSet(_docs);
      }
      Document.setMetric(distmetric);
      improver.setParams(_params);
      Boolean use_SDB = (Boolean) _params.get("useSDInSPP");
      boolean use_SD = true;
      double init_incumbent = Double.MAX_VALUE;
      double fval = 0;  // initial clusterers best finalevaluator evaluation
      if (finalevaluator!=null) fval = finalevaluator.getWorstValue();
      double init_imp_val = Double.MAX_VALUE;
      Vector clinds_4_ari = new Vector();  // Vector<int[] clusterIndices>

      scpmaker.addParams(_params);
      final Random rnd = utils.RndUtil.getInstance().getRandom();

      for (int i=0; i<numtries; i++) {
        improver.reset();  // reset clusterer
        improver.addAllDocuments(_docs);
        // setting centers
        Integer init_kmeans_firstI = (Integer) _params.get("init_kmeans_first");
        if (init_kmeans_firstI!=null && init_kmeans_firstI.intValue()>0) {
          KMeansSqrGuarantClusterer auxcl = new KMeansSqrGuarantClusterer();
          // se the docs to cluster
          Vector auxdocs = new Vector();
          int nal = init_kmeans_firstI.intValue();
          // ensure different docs
          if (nal > n)
              throw new ClustererException("nal>n");
          HashSet already_in = new HashSet();  // Set<Integer> helps ensure
                                               // no duplicates
          for (int j=0; j<nal; j++) {
            int e=-1;
            while (true) {
              e = utils.RndUtil.getInstance().getRandom().nextInt(n);
              Integer eI = new Integer(e);
              if (!already_in.contains(eI)) {
                already_in.add(eI);
                break;
              }
            }
            auxdocs.add(_docs.elementAt(e));
          }
          auxcl.addAllDocuments(auxdocs);
          auxcl.setParams(_params);
          Vector randomcenters = new Vector();
          if (nal > k) throw new ClustererException("nal>k");
          already_in.clear();
          for (int l = 0; l < k; l++) {
            int r = -1;
            while(true) {
              r = ((int) Math.floor((rnd.nextDouble() * Integer.MAX_VALUE))) %
                    nal;
              Integer rI = new Integer(r);
              if (!already_in.contains(rI)) {
                already_in.add(rI);
                break;
              }
            }
            randomcenters.addElement(auxdocs.elementAt(r));
          }
          auxcl.setInitialClustering(randomcenters);
          auxcl.clusterDocs();
          improver.setInitialClustering(auxcl.getCurrentCenters());
          improver.clusterDocs();
        }  // if init_kmeans_first
        else {
          Vector randomcenters = new Vector();
          HashSet already_in = new HashSet();  // Set<Integer> used to prevent 
                                               // duplicates
          for (int l = 0; l < k; l++) {
            int r=-1;
            while(true) {
              r = ((int)Math.floor((rnd.nextDouble() * Integer.MAX_VALUE))) %
                    n;
              Integer rI = new Integer(r);
              if (!already_in.contains(rI)) {
                  already_in.add(rI);
                  break;
              }
            }
            randomcenters.addElement(_docs.elementAt(r));
          }
          improver.setInitialClustering(randomcenters);
          improver.clusterDocs();
        }
        new_clusters.addAll(improver.getIntermediateClusters());
        double val = eval.eval(improver);

        if (use_SDB!=null) use_SD = use_SDB.booleanValue();
        if (val<init_incumbent && use_SD) {  // use SD only if solution found is 
                                             // promising
          init_incumbent = val;
          // improve via SD-Means
          Clusterer improver2 = new SDKMeansSqrMTClusterer();
          improver2.addAllDocuments(_docs);
          improver2.setParams(_params);
          improver2.setInitialClustering(improver.getCurrentCenters());
          improver2.setClusteringIndices(improver.getClusteringIndices());
          improver2.clusterDocs();
          // System.out.println("improving soln via SD");
          val = eval.eval(improver2);
          if (finalevaluator!=null) {
            double fvaltemp = finalevaluator.eval(improver2);
            fval = finalevaluator.bestOf(fvaltemp,fval);
          }
          // System.out.println("improved value="+val);
          if (val < init_imp_val) init_imp_val = val;
          new_clusters.addAll(improver2.getIntermediateClusters());
          // int[] inds = (int[]) improver2.getClusteringIndices().clone();
          int[] inds = new int[n];
          for (int i2=0; i2<n; i2++) 
              inds[i2] = improver2.getClusteringIndices()[i2];
          clinds_4_ari.addElement(inds);
        }
        else {
          if (val < init_imp_val) init_imp_val = val;
          // int[] inds = (int[]) improver.getClusteringIndices().clone();
          int[] inds = new int[n];
          for (int i2=0; i2<n; i2++) 
              inds[i2] = improver.getClusteringIndices()[i2];
          clinds_4_ari.addElement(inds);
        }
        System.out.println("Clustering("+n+","+k+") at iteration: "+ i+
                           " final clustering value="+val);
      }
      // bracket exposure
      int bracket_intvl = -1;
      int iters_per_bracket = 1;
      Integer biI = (Integer) _params.get("bracketinterval");
      if (biI!=null) {
        bracket_intvl = biI.intValue();
        Integer ipbI = (Integer) _params.get("itersperbracket");
        if (ipbI!=null) iters_per_bracket = ipbI.intValue();
      }
      if (bracket_intvl>0) {
        for (int k2=k-bracket_intvl; k2<=k+bracket_intvl; k2++) {
          if (k2<=1 || k2>=n) continue;  // boundary checking
          System.err.println("running Clusterer w/ k="+k2);
          if (k2==k) continue;
          for (int i=0; i<iters_per_bracket; i++) {
            improver.reset();  // reset clusterer
            improver.addAllDocuments(_docs);
            // setting centers
            Vector randomcenters=new Vector();
            HashSet already_in = new HashSet();  // Set<Integer>
            for (int l=0; l<k2; l++) {
              int r = -1;
              while (true) {
                r = ((int)Math.floor(rnd.nextDouble()*Integer.MAX_VALUE)) % n;
                Integer rI = new Integer(r);
                if (!already_in.contains(rI)) {
                    already_in.add(rI);
                    break;
                }
              }
              randomcenters.addElement(_docs.elementAt(r));
            }
            improver.setInitialClustering(randomcenters);
            improver.clusterDocs();
            new_clusters.addAll(improver.getIntermediateClusters());
          }
        }
      }
      // done bracket exposure
      if (init_imp_val<Double.MAX_VALUE) {
        System.out.println("Base-Clusterer incumbent over all iterations: "+
                           init_imp_val);
        if (finalevaluator!=null) 
          System.out.println("finalevaluator best evaluation "+
                             "over all iterations: "+fval);
      }
      // compute base clusterers' stability index
      double tot_ari = 0.0;
      AdjRandIndexEvaluator arie = new AdjRandIndexEvaluator();
      for (int i=0; i<clinds_4_ari.size(); i++) {
        int[] arri = (int[]) clinds_4_ari.elementAt(i);
        for (int j=i+1; j<clinds_4_ari.size(); j++) {
          int[] arrj = (int[]) clinds_4_ari.elementAt(j);
          tot_ari += arie.eval(arri,k,arrj,k);
        }
      }
      tot_ari *= ((double) 2.0/(clinds_4_ari.size()*(clinds_4_ari.size()-1)));
      System.out.println("Base Clusterers Total Adjusted Rand Index: "+tot_ari);

      scpmaker.createSCP(_docs,new_clusters,k,theta,maxpointset,distmetric,weights);
      while (cont) {
        System.err.println("SPPClusterer2.clusterDocs(): new iteration");
        DoubleMatrix2D A = scpmaker.getConstraintsMatrix();
        DoubleMatrix1D c = scpmaker.getCostVector();

        // call MPS util to write data to file
        if (write_mps) {
          long st = System.currentTimeMillis();
          utils.DataMgr.writeSCP2MPSFileFast(A, c, k, mpsfile);
          total_writing_time += (System.currentTimeMillis() - st);
        }
        write_mps = true;  // for the other iterations, we must write the file...
        System.err.println("SPPClusterer.clusterDocs(): done creating "+
                           "scp.mps file.");  // itc: HERE rm asap
        
        // don't call SCIP solver
        //String scip = (String) _params.get("sppsolver");
        //scip += " -f " + mpsfile + " -s scip.set";
        // call GUROBI
        String cmd = "gurobi_cl ResultFile=solution.sol "+mpsfile;
        int[] scpsolution = new int[A.columns()];
        int retcode = execGUROBI(cmd, scpsolution);  // call GUROBI and 
                                                     // figure out soln
        if (retcode!=0)
          throw new ClustererException("SPPClusterer2.clusterDocs(): "+
                                       "exec() MIP solver failed...");
        // one more check
        checkOptimalSoln(scpsolution, k);
        // convert to clustering solution
        _clusterIndices = scpmaker.convertSolution(scpsolution, weights);
        // compute _centers
        _centers = Document.getCenters(_docs, _clusterIndices, k, weights);

        boolean improve_via_kmeans = true;
        Boolean ivkmB = (Boolean) _params.get("improveviakmeans");
        if (ivkmB!=null) improve_via_kmeans = ivkmB.booleanValue();
        if (improve_via_kmeans) {
          improver = (Clusterer) _params.get("improver2");
          if (improver==null)
            improver = new KMeansSqrGuarantClusterer();
          improver.reset();
          improver.addAllDocuments(_docs);
          improver.setParams(_params);
          improver.setInitialClustering(_centers);
          improver.setClusteringIndices(_clusterIndices);  // added so as to let
          // non-centers based local search clusterers to work
          improver.clusterDocs();
          double val = eval.eval(improver);
          System.out.println("SPPClusterer2.clusterDocs(): after K-centers "+
                             "improvement value=" + val);
          // improve via SD-Means
          Clusterer improver2 = null;
          if (use_SD) {
            improver2 = new SDKMeansSqrMTClusterer();  
            improver2.addAllDocuments(_docs);
            improver2.setParams(_params);
            improver2.setInitialClustering(improver.getCurrentCenters());
            improver2.setClusteringIndices(improver.getClusteringIndices());
            _centers = improver2.clusterDocs();
            _clusterIndices = improver2.getClusteringIndices();
            // ensure that the improvements make it to the SPPClusterer2 results
            // System.err.println("improving soln via SD");
            val = eval.eval(improver2);
            System.out.println("SPPClusterer2.clusterDocs(): after SD "+
                               "improved value=" + val);
          }
          if (old_val > val) {
            new_clusters.clear();
            new_clusters.addAll(improver.getIntermediateClusters());
            if (improver2!=null) 
              new_clusters.addAll(improver2.getIntermediateClusters());
            // expand the neighborhood of the best soln found via improver2
            // if the appropriate parameter has been given
            if (improver2!=null && nborexpansion > 0) {
              new_clusters.addAll(scpmaker.expand(improver2.getCurrentCenters(),
                                                  improver2.
                                                  getClusteringIndices(),
                                                  nborexpansion));
            }
            scpmaker.addOverExistingColumns(new_clusters, scpsolution, weights);
          }
          else cont = false; // no improvement, stop
          old_val = val;
        }
        else cont = false;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("SPPClusterer2: clustering failed...");
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running "+
                                   "clusterDocs()");
    }
    System.err.println("SPPClusterer2.clusterDocs(): total writing time="+
                       total_writing_time+" msecs.");
    return _centers;
  }


  /**
   * appends the document d in the <CODE>_docs</CODE> data member.
   * @param d Document
   */
  public void addDocument(Document d) {
    if (_docs==null) _docs = new Vector();
    _docs.addElement(d);
  }


  /**
   * adds to the end of <CODE>_docs</CODE> all Documents in vector v.
   * Will throw class cast exception if any object in v is not a Document
   * @param v Vector  // Vector&lt;Document&gt;
   */
  public void addAllDocuments(Vector v) {
    if (v==null) return;
    if (_docs==null) _docs = new Vector();
    for (int i=0; i<v.size(); i++)
      _docs.addElement((Document) v.elementAt(i));
  }


  /**
   * set the initial clustering centers.
   * The vector <CODE>_centers</CODE> is reconstructed, but the Document objects
   * that are the cluster centers are simply passed as references.
   * The <CODE>_centers</CODE> doesn't own copies of them, but references to the
   * objects inside the centers vector that is passed in the param. list
   * @param centers Vector  // Vector&lt;Document&gt;
   * @throws ClustererException
   * @throws ClassCastException if any element of centers is not a Document
   */
  public void setInitialClustering(Vector centers) throws ClustererException {
    if (centers==null) 
        throw new ClustererException("null initial clusters vector");
    _centers = null;  // force gc
    _centers = new Vector();
    for (int i=0; i<centers.size(); i++)
      _centers.addElement((Document) centers.elementAt(i));
  }


  /**
   * return the <CODE>_centers</CODE> vector.
   * @return Vector  // Vector&lt;Document&gt;
   */
  public Vector getCurrentCenters() {
    return _centers;
  }


  /**
   * return the <CODE>_docs</CODE> vector.
   * @return Vector  // Vector&lt;Document&gt;
   */
  public Vector getCurrentDocs() {
    return _docs;
  }


  /**
   * the clustering params are set to p.
   * @param p Hashtable
   */
  public void setParams(Hashtable p) {
    _params = null;
    _params = new Hashtable(p);  // own the params
  }


  /**
   * set the <CODE>_docs,_centers,_clusterIndices</CODE> data members to null.
   */
  public void reset() {
    _docs = null;
    _centers = null;
    _clusterIndices=null;
  }


  /**
   * return the <CODE>_clusterIndices</CODE> data member.
   * @return int[]
   */
  public int[] getClusteringIndices() {
    return _clusterIndices;
  }


  /**
   * sets <CODE>_clusterIndices</CODE> to the values in a. Creates new 
   * <CODE>int[]</CODE>.
   * @param a int[]
   */
  public void setClusteringIndices(int[] a) {
    if (a==null) _clusterIndices = null;
    else {
      _clusterIndices = new int[a.length];
      for (int i=0; i<a.length; i++) _clusterIndices[i] = a[i];
    }
  }


  /**
   * creates and returns an <CODE>int[]</CODE> containing the number of vectors
   * in each cluster.
   * @return int[]
   * @throws ClustererException if <CODE>_clusterIndices</CODE> array is null 
   */
  public int[] getClusterCards() throws ClustererException {
    if (_clusterIndices==null)
      throw new ClustererException("null _clusterIndices");
    final int k = _centers.size();
    final int n = _docs.size();
    int[] cards = new int[_centers.size()];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<n; i++) {
      cards[_clusterIndices[i]]++;
    }
    return cards;
  }


  /**
   * calls the <CODE>eval(Clusterer)</CODE> method of the argument, with this as
   * Clusterer to pass in, and returns the result of the evaluation.
   * @param vtor Evaluator
   * @return double
   * @throws ClustererException 
   */
  public double eval(Evaluator vtor) throws ClustererException {
    return vtor.eval(this);
  }


  /**
   * create and return a vector containing all intermediate clusters created by
   * the clustering process (each cluster represented as a Vector&lt;Integer&gt;
   * of the indices of the documents it contains.
   * @return Vector  // Vector&lt;Vector&lt;Integer&gt;&gt;
   * @throws ClustererException 
   */
  public Vector getIntermediateClusters() throws ClustererException {
    try {
      Vector results = new Vector();  // Vector<Vector<Integer docid>>
      for (int i=0; i<_centers.size(); i++) results.addElement(new Vector());
      for (int i=0; i<_clusterIndices.length; i++) {
        int c = _clusterIndices[i];
        Vector vc = (Vector) results.elementAt(c);
        vc.addElement(new Integer(i));
        results.set(c, vc);  // ensure addition
      }
      return results;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("getIntermediateClusters(): failed");
    }
  }


  /**
   * Executes the SCIP solver as a separate process.
   * @param cmdLine String the scip command line
   * @param scpsolution int[] the output solution array
   * @return int 0 iff success
   */
  private int execSCIP(String cmdLine, int[] scpsolution) throws IOException {
    // zero-out scpsolution
    for (int i=0; i<scpsolution.length; i++) scpsolution[i] = 0;
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
          if (inString.startsWith("X")) {
            StringTokenizer st = new StringTokenizer(inString);
            String varname = st.nextToken();
            String varvalue = st.nextToken();
            double val = Double.parseDouble(varvalue);
            if (Math.abs(val-1)>eps) continue;  // ignore it
            // it's not a column that goes in the solution
            int setindex = Integer.parseInt(varname.substring(1));
            scpsolution[setindex] = 1;
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
  
  /**
   * Executes the GUROBI solver as a separate process.
   * @param cmdLine String the GUROBI cmd line, such as 
   * "gurobi_cl ResultFile=solution.sol scp.mps"
   * @param scpsolution int[] the solution output array
   * @return int 0 iff success
   */
  private int execGUROBI(String cmdLine, int[] scpsolution) throws IOException {
    // zero-out scpsolution
    for (int i=0; i<scpsolution.length; i++) scpsolution[i] = 0;
    int retCode = 1;              // Process return code
    BufferedReader in = null;
    Runtime rt = null;
    // String cmdLine = null;
    // Get a Runtime instance
    rt = Runtime.getRuntime();
    // Get the child process
    Process child = rt.exec(cmdLine);
    // Loop until the child process is finished.
    in = new BufferedReader(new InputStreamReader(child.getInputStream()));
    boolean finished = false;
    do {
      try {
        // Read any data that the child process has written to stdout.
        // This is necessary to prevent the child process from blocking.
        while (in.ready()) {
          String inString = in.readLine();
          if (inString==null) break;
          System.out.println(inString);
          // check to see if line is part of the solution
          if (inString.startsWith("X")) {
            StringTokenizer st = new StringTokenizer(inString);
            String varname = st.nextToken();
            String varvalue = st.nextToken();
            double val = Double.parseDouble(varvalue);
            if (Math.abs(val-1)>eps) continue;  // ignore it
            // it's not a column that goes in the solution
            int setindex = Integer.parseInt(varname.substring(1));
            scpsolution[setindex] = 1;
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
    // now read the contents of the solution.sol file
    try {
        in = new BufferedReader(new FileReader("solution.sol"));
        while(true) {
            String line = in.readLine();
            if (line==null) break;  // reached EOF
            if (line.startsWith("#")) continue;
            StringTokenizer st = new StringTokenizer(line, " ");
            String varname = st.nextToken();
            String varvalue = st.nextToken();
            double val = Double.parseDouble(varvalue);
            if (Math.abs(val-1)>eps) continue;  // ignore it
            // it's not a column that goes in the solution
            int setindex = Integer.parseInt(varname.substring(1));
            scpsolution[setindex] = 1;
        }
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    finally{
        if (in!=null) in.close();
    }
    return retCode;
  }
  
  
  /**
   * verify sum(scpsolution, 0, len-1)=k
   * @param scpsolution int[]
   * @param k int
   * @throws ClustererException
   */
  private static void checkOptimalSoln(int[] scpsolution, int k) 
          throws ClustererException {
    int s = 0;
    for (int i=0; i<scpsolution.length; i++) s += scpsolution[i];
    if (s != k) 
        throw new ClustererException("checkOptimalSoln(): sum="+s+"!=k="+k);
  }

}

