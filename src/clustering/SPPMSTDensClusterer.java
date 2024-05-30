package clustering;

import partitioning.*;
import java.util.*;
import java.io.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;

/**
 * The class implements Clustering by Set Partitioning.
 * In particular, for each Document, a set of its closest neighbors
 * is added -in the form of columns in a DoubleMatrix2D matrix- to the matrix
 * A that will be given as the constraints set in a Set Partitioning Problem.
 * For the biggest set (in cardinality) we may also add its complement -to ensure
 * covering solutions with only 2 columns.
 * The objective value of each set (column) is basically the density of the set
 * computed as the number of docs divided by the cost of the EMST connecting them,
 * and is used to populate a vector c -a DoubleMatrix1D object.
 *
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SPPMSTDensClusterer implements Clusterer {
  private static final double eps = 1.e-6;
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;

  public SPPMSTDensClusterer() {
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
    final double theta = ( (Double) _params.get("theta")).doubleValue();
    final int maxpointset = ((Integer) _params.get("maxpointset")).intValue();
    final DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    final SCPMakerIntf scpmaker = (SCPMakerIntf) _params.get("scpmaker");
    final int n = _docs.size();
    final int k = ((Integer) _params.get("num_clusters")).intValue();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    long total_writing_time = 0;
    try {
      scpmaker.addParams(_params);
      Vector new_clusters = new Vector();
      // boolean cont = true;
      // double old_val = Double.MAX_VALUE;
      boolean write_mps = true;
      Boolean write_mpsB = (Boolean) _params.get("writempsfile");
      if (write_mpsB!=null) write_mps = write_mpsB.booleanValue();
      // main algorithm to construct the A and c matrices

      // first run the GGAD clusterer

      String mpsfile = (String) _params.get("mpsfile");
      if (mpsfile==null) mpsfile = "F:\\spp.mps";

      Clusterer improver = new GGADClusterer();
      Evaluator eval = new MSTDensityEvaluator();
      Document.setMetric(distmetric);
      improver.setParams(_params);
      double init_imp_val = Double.NEGATIVE_INFINITY;
      {
        // improver.reset();  // reset clusterer
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
        init_imp_val = eval.eval(improver);
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
          System.err.println("running GGAD w/ k="+k2);
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
        System.out.println("GGAD incumbent value: "+init_imp_val);

      scpmaker.createSCP(_docs, new_clusters, k, theta, maxpointset, distmetric, null);  // itc-20220223: HERE
      {
        System.err.println("SPPMSTDensClusterer.clusterDocs(): new iteration");
        DoubleMatrix2D A = scpmaker.getConstraintsMatrix();
        DoubleMatrix1D c = scpmaker.getCostVector();
        // call MPS util to write data to file
        if (write_mps) {
          long st = System.currentTimeMillis();
          utils.DataMgr.writeSPP2MPSFileFast(A, c, k, mpsfile);
          total_writing_time += (System.currentTimeMillis() - st);
        }
        write_mps = true;  // for the other iterations, we must write the file...
        System.err.println("SPPMSTDensClusterer.clusterDocs(): done creating spp.mps file.");  // itc: HERE rm asap
        // call SCIP solver
        String scip = (String) _params.get("sppsolver");
        scip += " -f " + mpsfile + " -s scip.set";
        int[] scpsolution = new int[A.columns()];
        int retcode = exec(scip, scpsolution);  // execute the MIP solver and figure out soln
        if (retcode!=0)
          throw new ClustererException("SPPMSTDensClusterer.clusterDocs(): exec() SCIP failed...");
        // convert to clustering solution
        _clusterIndices = scpmaker.convertSolution(scpsolution, null);  // itc-20220223: HERE
        // compute _centers
        _centers = Document.getCenters(_docs, _clusterIndices, k, null);  // itc-20220223: HERE
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("SPPMSTDensClusterer: clustering failed...");
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
    System.err.println("SPPMSTDensClusterer.clusterDocs(): total writing time="+total_writing_time+" msecs.");
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

}

