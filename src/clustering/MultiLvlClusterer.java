package clustering;

import java.util.*;

/**
 * This class implements multi-level clustering in a multi-threaded fashion.
 * The advantages include:
 * (1) handling data-sets where the SPP algorithms take too long, or run out
 * of memory
 * (2) providing better quality of results.
 * Algorithm:
 * The data set is first split into a number of large chunks using K-Means to
 * allow for high-k clustering of each chunk using SPP,
 * then many (high-quality) small clusters -represented by their average- are
 * clustered together among the desired k, using K-Means,
 * and then a final K-Means iteration of the resulting partition is performed.
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MultiLvlClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>


  public MultiLvlClusterer() {
    _intermediateClusters = new Vector();
  }


  public Vector getIntermediateClusters() throws ClustererException {
    // store the final clustering in _intermediateClusters
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
      vc.addElement(new Integer(i));
      _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
    }
    // remove any empty vectors
    for (int i=_intermediateClusters.size()-1; i>=0; i--) {
      Vector v = (Vector) _intermediateClusters.elementAt(i);
      if (v==null || v.size()==0) _intermediateClusters.remove(i);
    }
    return _intermediateClusters;
  }


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class.
   *
   * The method implements the following algorithm:
   * 0. Let n=docs.size(); nd=desired_docs_size_4_SPP
   *        nf=fine_grain_docs_size_of_each_cluster
   *    Let S = n/nd;
   * 1. cluster docs via a K-Means type clusterer among S partitions;
   * 2. Let P1,...,PS be the resulting clusters of step 2;
   *    Let Ki = Pi/nf i=1...S
   * 3. Cluster each set Pi via SPP among Ki partitions i=1...S;
   * 4. Let Cji be the j-th center (j=1...Ki) of the clustering Pji of set Pi (i=1...S)
   * 5. cluster the centers Cji among k partitions;
   * 6. Assign the points of cluster Pji to the cluster index of point Cji (cluster index ranging from 0...k-1)
   * 7. Run K-Means and/or SDK-Means type improver clusterer to obtain final clustering;
   *
   * Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   *
   * <"TerminationCriteria",ClustererTermination> the object that will
   * decide when to stop the iterations.
   * <"evaluator",Evaluator> the object that will evaluate a clustering
   * <"metric",DocumentDistIntf> indicates what method will be used, e.g.
   * K-Median, K-Means, K-MeansSqr etc.
   * <"n_desired", Integer> the desired nd (data set size that SPP can handle comfortably).
   * The bigger n_desired the better the accuracy, but the worse the running-times
   * <"n_finegrain",Integer> the parameter nf.
   * <"Step1Clusterer", Clusterer> the object to be used to run step 1.
   * <"Step5Clusterer", Clusterer> the object to be used to run step 5.
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setInitialClustering(Vector<Document clusterCenters>)
   * @throws ClustererException if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    // 0.
    final DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    final int nd = ( (Integer) _params.get("n_desired")).intValue();
    final int nf = ( (Integer) _params.get("n_finegrain")).intValue();
    final int n = _docs.size();
    final int k = _centers.size();
    int S = n/nd;

    // 1.
    Clusterer step1_cl = (Clusterer) _params.get("Step1Clusterer");
    step1_cl.setParams(_params);
    step1_cl.addAllDocuments(_docs);
    Vector step1_init_centers = new Vector();
    for (int i=0; i<S; i++) {
      Document rand_d = (Document) _docs.elementAt((int) (n*utils.RndUtil.getInstance().getRandom().nextDouble()));
      Document init_c_i = new Document(rand_d);
      step1_init_centers.addElement(init_c_i);
    }
    step1_cl.setInitialClustering(step1_init_centers);
    // Vector step1_centers = step1_cl.clusterDocs();
    step1_cl.clusterDocs();
    int asgns1[] = step1_cl.getClusteringIndices();

    System.out.println("MultiLvlClusterer.clusterDocs(): step1 done");

    // 2.
    int lup[] = new int[n];
    int sizesi[] = step1_cl.getClusterCards();
    Vector Pi[] = new Vector[S];  // []<Vector<Document> >
    for (int i=0; i<S; i++) {  // init vector array
      Pi[i] = new Vector();
    }
    for (int i=0; i<n; i++) {  // add di to appropriate cluster
      int t = step1_cl.getClusteringIndices()[i];
      Pi[t].addElement(_docs.elementAt(i));
      lup[i] = Pi[t].size()-1;
    }
    int Ki[] = new int[S];
    for (int i=0; i<S; i++) {
      Ki[i] = sizesi[i]/nf;
    }
    System.out.println("MultiLvlClusterer.clusterDocs(): step2 done");


    // 3-4. use SPP to cluster each Pi data set among Ki components
    Vector step4_centers = new Vector();  // Vector<Document>
    int asgns14[] = new int[_docs.size()];  // init to zero by default
    int m=0;
    for (int i=0; i<S; i++) {
      Clusterer step4_cl = new SPPClusterer();
      Hashtable paramsi = new Hashtable(_params);
      int ki = Ki[i];
      paramsi.put("k",new Integer(ki));
      paramsi.put("num_clusters",new Integer(ki));
      step4_cl.setParams(paramsi);
      step4_cl.addAllDocuments(Pi[i]);
      // init centers
      // itc: HERE is init'n needed for SPP?
      Vector step4_init_centers = new Vector();
      for (int j=0; j<ki; j++) {
        int n34 = Pi[i].size();
        Document rand_d = (Document) Pi[i].elementAt((int) (n34*utils.RndUtil.getInstance().getRandom().nextDouble()));
        Document init_c_i = new Document(rand_d);
        step4_init_centers.addElement(init_c_i);
      }
      step4_cl.setInitialClustering(step4_init_centers);
      // end init centers
      step4_centers.addAll(step4_cl.clusterDocs());
      // figure out to which center each _doc[i] goes
      int asgns4[] = step4_cl.getClusteringIndices();
      for (int ii=0; ii<n; ii++) {
        int piind = asgns1[ii];
        if (piind!=i) continue;  // not part of this iteration
        asgns14[ii] = asgns4[lup[ii]]+m;
      }
      m+=ki;
      System.out.println("MultiLvlClusterer.clusterDocs(): step4."+i+
                         "(out of "+S+") done");
      // done clustering at step4, using SPP
    }

    // 5. cluster the data points in step4_centers among k partitions
    Clusterer step5_cl = (Clusterer) _params.get("Step5Clusterer");
    step5_cl.setParams(_params);
    step5_cl.addAllDocuments(step4_centers);
    int n4 = step4_centers.size();
    Vector step5_init_centers = new Vector();
    for (int i=0; i<k; i++) {
      Document rand_d = (Document) step4_centers.elementAt((int) (n4*utils.RndUtil.getInstance().getRandom().nextDouble()));
      Document init_c_i = new Document(rand_d);
      step5_init_centers.addElement(init_c_i);
    }
    step5_cl.setInitialClustering(step5_init_centers);
    Vector step5_centers = step5_cl.clusterDocs();
    int asgns5[] = step5_cl.getClusteringIndices();
    System.out.println("MultiLvlClusterer.clusterDocs(): step5 done");

    // 6. assign the initial points to the cluster index of their center Cji
    // and figure out new partition centers
    int step6_initasgns[] = new int[n];
    Vector step6_init_centers = null;
    for (int i=0; i<n; i++) {
      int piind = asgns14[i];
      step6_initasgns[i]=asgns5[piind];
    }
    step6_init_centers = Document.getCenters(_docs, step6_initasgns, k, null);  // itc: HERE 20220223
    System.out.println("MultiLvlClusterer.clusterDocs(): step6 done");

    // 7. improve via K-Means and SDK clustering the current partition
    // improve via K-Means
    Clusterer improver = new KMeansSqrGuarantClusterer();
    Evaluator eval = new KMeansSqrEvaluator();
    Document.setMetric(distmetric);
    improver.addAllDocuments(_docs);
    improver.setParams(_params);
    improver.setInitialClustering(step6_init_centers);
    improver.setClusteringIndices(step6_initasgns);
    // improver.setInitialClustering(step5_centers);
    improver.clusterDocs();
    double val = eval.eval(improver);
    System.out.println("MultiLvlClusterer: after improvement best value = " + val);
    // improve via SD-Means
    Clusterer improver2 = new SDKMeansSqrClusterer();
    improver2.addAllDocuments(_docs);
    improver2.setParams(_params);
    improver2.setInitialClustering(improver.getCurrentCenters());
    improver2.setClusteringIndices(improver.getClusteringIndices());
    improver2.clusterDocs();
    System.out.println("improving soln via SD");
    val = eval.eval(improver2);
    System.out.println("MultiLvlClusterer: final improved value="+val);
    // set final data values
    _centers = new Vector(improver2.getCurrentCenters());
    _clusterIndices = improver2.getClusteringIndices();
    // done!.
    System.out.println("MultiLvlClusterer.clusterDocs(): step7[last] done");

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
    _intermediateClusters.clear();
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
    int[] cards = new int[k];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<n; i++) {
      cards[_clusterIndices[i]]++;
    }
    return cards;
  }


  public double eval(Evaluator vtor) throws ClustererException {
    return vtor.eval(this);
  }


  /**
   * break up any clusters that do not appear compact
   * the heuristic is the following:
   * for any cluster that contains points that are more than 2.1*ave_dist away
   * from the center, remove all points that are more than ave_dist away from
   * their center, recompute the center, and assign the removed points to their
   * nearest center
   */
  private void compactClusters(double trycompacting) throws ClustererException {
    final int k = _centers.size();
    final int n = _docs.size();
    // 1. compute average distance from center for each cluster
    double ave_dist[] = new double[k];
    for (int i=0; i<k; i++) ave_dist[i] = 0.0;
    for (int i=0; i<n; i++) {
      int c = _clusterIndices[i];
      Document di = (Document) _docs.elementAt(i);
      Document cl = (Document) _centers.elementAt(c);
      ave_dist[c] += Document.d(di, cl);
    }
    int cards[] = getClusterCards();
    for (int i=0; i<k; i++) {
      ave_dist[i] /= cards[i];
    }
    // 2. find every document that is further than 2.1*ave_dist away
    // and unassign it
    int changed[] = new int[k];
    for (int i=0; i<k; i++) changed[i] = 0;  // init
    for (int i=0; i<n; i++) {
      int c = _clusterIndices[i];
      Document di = (Document) _docs.elementAt(i);
      Document cl = (Document) _centers.elementAt(c);
      double dist = Document.d(di, cl);
      if (dist>trycompacting*ave_dist[c]) {
        changed[c]++;
        _clusterIndices[i] = -1;
      }
    }
    // 3. recompute the centers
    for (int i=0; i<k; i++) {
      if (changed[i]>0) {
        Document ci = (Document) _centers.elementAt(i);
        ci = new Document(new TreeMap(), ci.getDim());  // reset ci
        _centers.set(i, ci);
      }
    }
    for (int i=0; i<n; i++) {
      Document di = (Document) _docs.elementAt(i);
      int c = _clusterIndices[i];
      if (c>=0 && changed[c]>0) {
        Document cl = (Document) _centers.elementAt(c);
        cl.addMul(1.0, di);
        _centers.set(c, cl);
      }
    }
    for (int i=0; i<k; i++) {
      if (changed[i]>0) {
        Document ci = (Document) _centers.elementAt(i);
        ci.div((cards[i]-changed[i]));
        _centers.set(i, ci);
      }
    }
    // 4. reassign the unassigned points
    for (int i=0; i<n; i++) {
      if (_clusterIndices[i]==-1) {  // it is unassigned
        Document di = (Document) _docs.elementAt(i);
        double best = Double.MAX_VALUE;
        int best_ind = -1;
        for (int j=0; j<k; j++) {
          Document cj = (Document) _centers.elementAt(j);
          double dist = Document.d(di, cj);
          if (dist<best) {
            best = dist;
            best_ind = j;
          }
        }
        _clusterIndices[i] = best_ind;
      }
    }
    // 5. finally compute the new centers
    _centers = Document.getCenters(_docs, _clusterIndices, k, null);  // itc: HERE 20220223
  }
}



