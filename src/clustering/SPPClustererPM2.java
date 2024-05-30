package clustering;

import partitioning.*;
import java.util.*;
import java.io.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;

/**
 * This is the same class as SPPClusterer2, only that the clusterDocs()
 * calls the Document.getDocumentCenters(...) instead of Document.getCenters(...)
 * because this class is for P-Median Clustering ONLY.
 * Uses the GUROBI optimizer
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SPPClustererPM2 implements Clusterer {
  private static final double eps = 1.e-6;
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;

  public SPPClustererPM2() {
  }


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   *
   * <"num_clusters", int> number of clusters to create
   * <"theta",double> threshold rate in distances for stop adding documents
   * far from the point which we currently consider as center
   * <"maxpointset", int> the maximum number of columns to generate for a given
   * data point -by adding more and more distant neighbors
   * <"TerminationCriteria",ClustererTermination> the object that will
   * decide when to stop the iterations.
   * <"metric", DocumentDistIntf> the metric distance.
   * <"scpmaker",SCPMaker> the object encapsulating the algorithm for creating
   * and converting the SCP set into eventually an SPP soln. Essentially
   * implements the Strategy design pattern.
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setInitialClustering(Vector<Document clusterCenters>)
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws ClustererException {
    final DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    final SCPMakerIntf scpmaker = (SCPMakerIntf) _params.get("scpmaker");
    final int n = _docs.size();
    final int k = ((Integer) _params.get("num_clusters")).intValue();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    double theta = 0.0;
    int maxpointset=0;
    try {
      theta = ( (Double) _params.get("theta")).doubleValue();
    }
    catch (Exception e) {
      // no-op
    }
    try {
      maxpointset = ( (Integer) _params.get("maxpointset")).intValue();
    }
    catch (Exception e) {
      // no-op
    }

    long total_writing_time = 0;
    try {
      scpmaker.addParams(_params);
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
      if (improver==null) improver = new PMedianRandGreedyClusterer();
      Evaluator eval = (Evaluator) _params.get("evaluator");
      if (eval==null) eval = new PMedianEvaluator();
      if (eval instanceof EnhancedEvaluator) {
        ((EnhancedEvaluator) eval).setMasterDocSet(_docs);
        ((EnhancedEvaluator) eval).setParams(_params);
      }
      Document.setMetric(distmetric);
      improver.setParams(_params);
      double init_incumbent = Double.MAX_VALUE;
      double init_imp_val = Double.MAX_VALUE;
      for (int i=0; i<numtries; i++) {
        improver.reset();  // reset clusterer
        improver.addAllDocuments(_docs);
        // setting centers
        Vector randomcenters=new Vector();
        for (int l=0; l<k; l++) {
          int r = ((int) Math.floor((utils.RndUtil.getInstance().getRandom().nextDouble()*Integer.MAX_VALUE))) % n;
          randomcenters.addElement(_docs.elementAt(r));
        }
        improver.setInitialClustering(randomcenters);
        improver.clusterDocs();
        new_clusters.addAll(improver.getIntermediateClusters());
        double val = eval.eval(improver);
        if (val < init_imp_val) init_imp_val = val;
        System.out.println("Clustering("+n+","+k+") at iteration: "+ i+" final clustering value="+val);
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
          System.err.println("running base clusterer w/ k="+k2);
          if (k2==k) continue;
          for (int i=0; i<iters_per_bracket; i++) {
            improver.reset();  // reset clusterer
            improver.addAllDocuments(_docs);
            // setting centers
            Vector randomcenters=new Vector();
            for (int l=0; l<k2; l++) {
              int r = ((int) Math.floor((utils.RndUtil.getInstance().getRandom().nextDouble()*Integer.MAX_VALUE))) % n;
              randomcenters.addElement(_docs.elementAt(r));
            }
            improver.setInitialClustering(randomcenters);
            improver.clusterDocs();
            new_clusters.addAll(improver.getIntermediateClusters());
          }
        }
      }
      // done bracket exposure
      if (init_imp_val<Double.MAX_VALUE)
        System.out.println("incumbent over all iterations: "+init_imp_val);

      scpmaker.createSCP(_docs, new_clusters, k, theta, maxpointset, distmetric, null);  // itc-20220223: HERE
      while (cont) {
        System.err.println("SPPClustererPM.clusterDocs(): new iteration");
        DoubleMatrix2D A = scpmaker.getConstraintsMatrix();
        DoubleMatrix1D c = scpmaker.getCostVector();

        // call MPS util to write data to file
        // itc: HERE should coordinate in case many threads concurrently write to
        // this file and then read it in an attempt to solve many instances in
        // parallel?...
        if (write_mps) {
          long st = System.currentTimeMillis();
          utils.DataMgr.writeSCP2MPSFileFast(A, c, k, mpsfile);
          total_writing_time += (System.currentTimeMillis() - st);
        }
        write_mps = true;  // for the other iterations, we must write the file...
        System.err.println("SPPClustererPM.clusterDocs(): done creating mps file.");  // itc: HERE rm asap
        // call GUROBI solver
        String cmdname = (String) _params.get("sppsolver");
        String cmdLine = cmdname + " ResultFile=C:\\Temp\\"+mpsfile+".sol " + mpsfile;  // gurobi-specific
        int[] scpsolution = new int[A.columns()];
        int retcode = exec(cmdLine, scpsolution);  // execute the MIP solver and figure out soln
        if (retcode!=0)
          throw new ClustererException("SPPClusterer.clusterDocs(): exec() SCIP failed...");
        // convert to clustering solution
        _clusterIndices = scpmaker.convertSolution(scpsolution, null);  // itc-20220223: HERE
        // compute _centers
        _centers = Document.getDocumentCentersFast(_docs, _clusterIndices, k, distmetric);
        // itc: HERE why not use the iterative improvement scheme?
        cont=false;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("SPPClustererPM: clustering failed...");
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
    System.err.println("SPPClustererPM.clusterDocs(): total writing time="+total_writing_time+" msecs.");
    return _centers;
  }


  public void addDocument(Document d) {
    if (_docs==null) _docs = new Vector();
    _docs.addElement(d);
  }


  /**
   * adds to the end of _docs all Documents in v
   * Will throw class cast exception if any object in v is not a Document
   * @param v Vector
   */
  public void addAllDocuments(Vector v) {
    if (v==null) return;
    if (_docs==null) _docs = new Vector();
    for (int i=0; i<v.size(); i++)
      _docs.addElement((Document) v.elementAt(i));
  }


  /**
   * set the initial clustering centers
   * the vector _centers is reconstructed, but the Document objects
   * that are the cluster centers are simply passed as references.
   * the _centers doesn't own copies of them, but references to the
   * objects inside the centers vector that is passed in the param. list
   * @param centers Vector
   * @throws ClustererException
   */
  public void setInitialClustering(Vector centers) throws ClustererException {
    if (centers==null) throw new ClustererException("null initial clusters vector");
    _centers = null;  // force gc
    _centers = new Vector();
    for (int i=0; i<centers.size(); i++)
      _centers.addElement((Document) centers.elementAt(i));
  }


  public Vector getCurrentCenters() {
    return _centers;
  }


  public Vector getCurrentDocs() {
    return _docs;
  }


  /**
   * the clustering params are set to p
   * @param p Hashtable
   */
  public void setParams(Hashtable p) {
    _params = null;
    _params = new Hashtable(p);  // own the params
  }


  public void reset() {
    _docs = null;
    _centers = null;
    _clusterIndices=null;
  }


  public int[] getClusteringIndices() {
    return _clusterIndices;
  }


  public void setClusteringIndices(int[] a) {
    if (a==null) _clusterIndices = null;
    else {
      _clusterIndices = new int[a.length];
      for (int i=0; i<a.length; i++)
        _clusterIndices[i] = a[i];
    }
  }


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


  public double eval(Evaluator vtor) throws ClustererException {
    return vtor.eval(this);
  }


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
   * Executes the MIP solver as a separate process
   */
  private int exec(String cmdLine, int[] scpsolution) throws IOException {
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
          System.err.println(inString);  // print out the optimizer output
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
    // now read the result file
    StringTokenizer st=new StringTokenizer(cmdLine," ");
    String optname=st.nextToken();
    while (st.hasMoreTokens()) {
      String tok=st.nextToken();
      if (tok.startsWith("ResultFile")) {
        //int ind=tok.lastIndexOf('=');
        String filename = tok.substring(11);
        // read the solution file
        BufferedReader br = new BufferedReader(new FileReader(filename));
        if (br.ready()) {
          while (true) {
            String line = br.readLine();
            if (line==null) break;  // end-of-file
            if (line.startsWith("X")) {
              StringTokenizer st2 = new StringTokenizer(line, " \t");
              String varname = st2.nextToken();
              double value = Double.parseDouble(st2.nextToken());
              int ivalue = (int) Math.round(value);
              int setindex = Integer.parseInt(varname.substring(1));
              scpsolution[setindex] = ivalue;
            }
          }
        }
        br.close();
        break;  // out of the read-cmd-line-options loop
      }
    }
    return retCode;
  }

}

