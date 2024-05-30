package clustering;

import partitioning.*;
import java.util.*;
import java.io.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;

/**
 * The class implements Coarsening by Set Partitioning.
 * It is an experimental SCP solver working by a simple (and hopefully fast)
 * greedy method: picks the column that minimizes the ratio col_cost / col_coverage
 * where col_coverage is the number of uncovered yet points that the column
 * covers... If we have k-1 columns, we create a final column with all the
 * remaining uncovered points. Needless to say, we could have applied other
 * local optimization procedures earlier on with the remaining uncovered points
 * (when a threshold is reached, like 2 or 5 columns before the end or smth like that).
 * After the problem is solved, many heuristic procedures can be used to tranlate
 * the problem from Set Covering to Set Partitioning. This is because every
 * Set Covering Problem solution for the Clustering Problem can be translated to
 * a solution of the Set Partitioning Problem in many different ways: one heuristic
 * is to find all multiple appearances of documents x in columns, and to remove
 * them from all columns that contain Document x except from the
 * set to which x is closest. For speed of execution, the center of each column
 * may not be shifted, but instead, we can just remove from the objective value
 * of the column the contribution of x. The result is a valid solution to the
 * Set Partitioning Problem.
 *
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SPPGreedyClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;

  public SPPGreedyClusterer() {
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
    // ClustererTermination ct = (ClustererTermination)
    //  _params.get("TerminationCriteria");
    // ct.registerClustering(this); // register this clustering problem with ct

    final DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    final SCPMakerIntf scpmaker = (SCPMakerIntf) _params.get("scpmaker");
    final int n = _docs.size();
    final int k = ((Integer) _params.get("num_clusters")).intValue();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    long total_writing_time = 0;
    try {
      scpmaker.addParams(_params);
      Vector new_clusters = new Vector();
      boolean cont = true;
      double old_val = Double.MAX_VALUE;
      // main algorithm to construct the A and c matrices

      // first run a conventional clusterer
      int numtries = 1;
      Integer numtriesI = (Integer) _params.get("numtries");
      if (numtriesI!=null) numtries = numtriesI.intValue();
      int nborexpansion = -1;
      Integer neI = (Integer) _params.get("nborexpansion");
      if (neI!=null) nborexpansion = neI.intValue();

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
      double init_imp_val = Double.MAX_VALUE;
      Vector clinds_4_ari = new Vector();  // Vector<int[] clusterIndices>
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
          for (int j=0; j<nal; j++) {
            int e = utils.RndUtil.getInstance().getRandom().nextInt(n);
            auxdocs.add(_docs.elementAt(e));
          }
          auxcl.addAllDocuments(auxdocs);
          auxcl.setParams(_params);
          Vector randomcenters = new Vector();
          for (int l = 0; l < k; l++) {
            int r = ( (int) Math.floor( (utils.RndUtil.getInstance().getRandom().
                                         nextDouble() * Integer.MAX_VALUE))) %
                                         nal;
            randomcenters.addElement(auxdocs.elementAt(r));
          }
          auxcl.setInitialClustering(randomcenters);
          auxcl.clusterDocs();
          improver.setInitialClustering(auxcl.getCurrentCenters());
          improver.clusterDocs();
        }  // if init_kmeans_first
        else {
          Vector randomcenters = new Vector();
          for (int l = 0; l < k; l++) {
            int r = ( (int) Math.floor( (utils.RndUtil.getInstance().getRandom().
                                         nextDouble() * Integer.MAX_VALUE))) %
                n;
            randomcenters.addElement(_docs.elementAt(r));
          }
          improver.setInitialClustering(randomcenters);
          improver.clusterDocs();
        }
        new_clusters.addAll(improver.getIntermediateClusters());
        double val = eval.eval(improver);

        if (use_SDB!=null) use_SD = use_SDB.booleanValue();
        if (val<init_incumbent && use_SD) {  // use SD only if the solution found is promising
          init_incumbent = val;
          // improve via SD-Means
          Clusterer improver2 = new SDKMeansSqrClusterer();
          improver2.addAllDocuments(_docs);
          improver2.setParams(_params);
          improver2.setInitialClustering(improver.getCurrentCenters());
          improver2.setClusteringIndices(improver.getClusteringIndices());
          improver2.clusterDocs();
          // System.out.println("improving soln via SD");
          val = eval.eval(improver2);
          // System.out.println("improved value="+val);
          if (val < init_imp_val) init_imp_val = val;
          new_clusters.addAll(improver2.getIntermediateClusters());
          // int[] inds = (int[]) improver2.getClusteringIndices().clone();
          int[] inds = new int[n];
          for (int i2=0; i2<n; i2++) inds[i2] = improver2.getClusteringIndices()[i2];
          clinds_4_ari.addElement(inds);
        }
        else {
          if (val < init_imp_val) init_imp_val = val;
          // int[] inds = (int[]) improver.getClusteringIndices().clone();
          int[] inds = new int[n];
          for (int i2=0; i2<n; i2++) inds[i2] = improver.getClusteringIndices()[i2];
          clinds_4_ari.addElement(inds);
        }
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
          System.err.println("running K-Means w/ k="+k2);
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
        System.out.println("K-Means incumbent over all iterations: "+init_imp_val);

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

      scpmaker.createSCP(_docs, new_clusters, k, theta, maxpointset, distmetric, null);  // itc-20220223: HERE
      while (cont) {
        System.err.println("SPPGreedyClusterer.clusterDocs(): new iteration");
        DoubleMatrix2D A = scpmaker.getConstraintsMatrix();
        DoubleMatrix1D c = scpmaker.getCostVector();

        // call greedy solver
        solveSCP(A, c);  // execute the greedy solver and figure out soln
        // this sets the _clusterIndices array
        // now compute _centers
        _centers = Document.getCenters(_docs, _clusterIndices, k, null);  // itc-20220223: HERE

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
          System.out.println(
              "SPPGreedyClusterer.clusterDocs(): after K-centers improvement value = " +
              val);
          // improve via SD-Means
          Clusterer improver2 = null;
          if (use_SD) {
            improver2 = new SDKMeansSqrClusterer();
            improver2.addAllDocuments(_docs);
            improver2.setParams(_params);
            improver2.setInitialClustering(improver.getCurrentCenters());
            improver2.setClusteringIndices(improver.getClusteringIndices());
            _centers = improver2.clusterDocs();
            _clusterIndices = improver2.getClusteringIndices(); // itc: HERE ensure
            // that the improvements make it to the SPPClusterer2 results
            // System.err.println("improving soln via SD");
            val = eval.eval(improver2);
            System.out.println(
                "SPPClusterer.clusterDocs(): after SD improved value=" + val);
          }
          if (old_val > val) {
            new_clusters.clear();
            new_clusters.addAll(improver.getIntermediateClusters());
            if (improver2!=null) new_clusters.addAll(improver2.getIntermediateClusters());
            // expand the neighborhood of the best soln found via improver2
            // if the appropriate parameter has been given
            if (improver2!=null && nborexpansion > 0) {
              new_clusters.addAll(scpmaker.expand(improver2.getCurrentCenters(),
                                                  improver2.
                                                  getClusteringIndices(),
                                                  nborexpansion));
            }
            int scpsolution[] = new int[k];  // dummy zero solution, doesn't matter where we overwrite
            scpmaker.addOverExistingColumns(new_clusters, scpsolution, null);  // itc-20220223: HERE
          }
          else cont = false; // no improvement, stop
          old_val = val;
        }
        else cont = false;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("SPPClusterer: clustering failed...");
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
    System.err.println("SPPClusterer.clusterDocs(): total writing time="+total_writing_time+" msecs.");
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
   * the method implements the Greedy Set Covering Algorithm
   * @param A DoubleMatrix2D
   * @param c DoubleMatrix1D
   */
  private void solveSCP(DoubleMatrix2D A, DoubleMatrix1D c) {
    final int k = ((Integer) _params.get("num_clusters")).intValue();
    final int n = _docs.size();
    final int numcols = c.size();
    int num_clusters_left_to_stop = 1;
    Integer ncl2sI = (Integer) _params.get("numclusterslefttostop");
    if (ncl2sI!=null && ncl2sI.intValue()>1) num_clusters_left_to_stop = ncl2sI.intValue();
    double scpcost[] = new double[numcols];
    boolean covered[] = new boolean[n];
    if (_clusterIndices==null) {
      _clusterIndices = new int[n];
    }
    for (int i=0; i<n; i++) _clusterIndices[i] = -1;  // init/reset
    int tot_covered = 0;
    double tot_val = 0.0;
    double f = 1.0;
    double step = 0.5;
    for (int i=0;  i < k - num_clusters_left_to_stop; i++) {
      // control the denominator: the effect that the covergae of a column has
      // on the criterion for entering the soln
      double exp_col_coverage = (n - tot_covered) / (double) (k - i);
      double cur_col_coverage = tot_covered / (double) i;
      if (i>10) {  // apply control after some iterations...
        if (cur_col_coverage < exp_col_coverage - 1) f += step;
        else if (cur_col_coverage > exp_col_coverage + 1) f -= step;
      }
      // find best column to include
      int best_col = -1;
      double best_col_cost = Double.MAX_VALUE;
      int best_numcov = -1;
      for (int j=0; j<numcols; j++) {
        int num_covering = 0;
        for (int r=0; r<n; r++)
          if (A.getQuick(r,j)>0 && covered[r]==false) num_covering++;
        if (num_covering==0) continue;  // useless column
        double denom = Math.pow((double) num_covering, f);
        double val = c.getQuick(j) / denom;
        if (val<best_col_cost) {
          best_col = j;
          best_col_cost = val;
          best_numcov = num_covering;
        }
      }
      // set the covered col as well as update the _clusterIndices
      for (int r=0; r<n; r++) {
        if (A.getQuick(r,best_col)>0 && _clusterIndices[r]<0) {
          covered[r] = true;
          _clusterIndices[r] = i;
        }
      }
      tot_covered += best_numcov;
      tot_val += c.getQuick(best_col);
      System.err.println("numcluster="+i+" tot_cov="+tot_covered+" tot_val="+tot_val);  // itc: HERE rm asap
    }
    if (num_clusters_left_to_stop>1) {
      // we are left with num_clusters_left_to_stop and we must put the remaining
      // points in these clusters
      // Clusterer cl = (Clusterer) _params.get("improver");
      Clusterer cl = new KMeansSqrGuarantClusterer();  // itc: HERE improve upon this asap
      // figure out the docs left
      Vector docs_left = new Vector();
      int map_back[] = new int[n];
      for (int j = 0; j < n; j++) {
        if (covered[j] == false) {
          map_back[docs_left.size()] = j;
          docs_left.addElement(_docs.elementAt(j));
        }
      }
      try {
        cl.reset();
        Hashtable left_params = new Hashtable(_params);
        left_params.put("num_clusters", new Integer(num_clusters_left_to_stop));
        cl.setParams(left_params);
        cl.addAllDocuments(docs_left);
        // set initial clustering
        Vector scenters = new Vector();
        for (int i2=0; i2<num_clusters_left_to_stop; i2++) {
          scenters.addElement(new Document((Document) docs_left.elementAt(
            utils.RndUtil.getInstance().getRandom().nextInt(docs_left.size()))));
        }
        cl.setInitialClustering(scenters);
        cl.clusterDocs();
        int cl_inds[] = cl.getClusteringIndices();
        for (int j = 0; j < cl_inds.length; j++) {
          _clusterIndices[map_back[j]] = cl_inds[j] +
              (k - num_clusters_left_to_stop);
        }
        // itc: HERE from here rm asap
        System.err.println("finished clustering "+docs_left.size()+
                           " docs amongs "+num_clusters_left_to_stop+" clusters.");  // itc: HERE rm asap
        int cards[] = getClusterCards();
        System.err.print("cards:");
        for (int i=0; i<cards.length; i++) {
          System.err.print("c["+i+"]="+cards[i]+" ");
        }
        System.err.println("");
        // itc: HERE rm to here
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }  // if num_clusters_left_to_stop > 1
    else {  // just put any uncovered points in the final cluster
      for (int j=0; j<n; j++) {
        if (covered[j]==false) _clusterIndices[j] = k-1;
      }
    }
  }
}

