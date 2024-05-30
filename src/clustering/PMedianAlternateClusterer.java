package clustering;

import java.util.*;

public class PMedianAlternateClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  // KMeansEvaluator _evaluator;
  PMedianEvaluator _evaluator;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>

  public PMedianAlternateClusterer() {
    _evaluator = new PMedianEvaluator();
    _intermediateClusters = new Vector();
  }


  public Vector getIntermediateClusters() throws ClustererException {
    return _intermediateClusters;
  }


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   *
   * <"h",double> the rate of learning in soft K-Means Clustering
   * <"TerminationCriteria",ClustererTermination> the object that will
   * decide when to stop the iterations.
   * <"movable",Vector<Integer clusterind> > optional, indicates which clusters
   * are allowed to exchange their documents, if it exists.
   * <"percenttokeep",double> optional, a percent of Documents below which if a
   * partition falls no Documents are allowed to move away from it.
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setInitialClustering(Vector<Document clusterCenters>)
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    final double h = ( (Double) _params.get("h")).doubleValue();
    boolean project_on_empty = false;
    Boolean p_o_e = (Boolean) _params.get("projectonempty");
    if (p_o_e!=null) project_on_empty = p_o_e.booleanValue();
    boolean half_max = false;
    Boolean hm = (Boolean) _params.get("halfmax");
    if (hm!=null) half_max = hm.booleanValue();
    double perc_2_keep = -1;
    Double p2kD = (Double) _params.get("percenttokeep");
    if (p2kD!=null) perc_2_keep = p2kD.doubleValue();
    Double incr_probD = (Double) _params.get("irenterprob");
    double incr_prob = incr_probD!=null ? incr_probD.doubleValue() : 0.1;
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centers.size();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = new DocumentDistL2();

    Vector movable = (Vector) _params.get("movable");
    boolean movable_exists=(movable!=null);

    double r = Double.MAX_VALUE;
    int ind[];  // to which cluster each doc belongs
    int numi[];  // how many docs each cluster has

    int best_indices[] = new int[n];
    Vector best_centers = new Vector();  // incumbent iteration
    double best_val = Double.MAX_VALUE;

    if (_clusterIndices==null) {
      ind = new int[n];  // to which cluster each doc belongs
      numi = new int[k];  // how many docs each cluster has
      for (int i = 0; i < k; i++) numi[i] = 0;
      for (int i = 0; i < n; i++) ind[i] = -1;
      // first assign to each center the closest document to it
      for (int i = 0; i < k; i++) {
        Document ci = (Document) _centers.elementAt(i);
        r = Double.MAX_VALUE;
        int best_j = -1;
        for (int j = 0; j < n; j++) {
          if (ind[j] >= 0)continue; // j already taken
          Document dj = (Document) _docs.elementAt(j);
          double distij = distmetric.dist(ci, dj);
          if (distij < r) {
            r = distij;
            best_j = j;
          }
        }
        numi[i] = 1;
        ind[best_j] = i;
      }
    }  // if _clusterIndices==null
    else {
      // _clusterIndices already exists, and is assumed to have at least one
      // element for each cluster.
      ind = _clusterIndices;
      numi = getClusterCards();
      // initialize best solution found
      for (int i=0; i<n; i++) best_indices[i]=_clusterIndices[i];
      best_centers.clear();
      best_centers.addAll(_centers);
      best_val = _evaluator.eval(this);
    }
    int num_cur_iters = 0;
    while (!ct.isDone()) {
      ++num_cur_iters;
      for (int i=0; i<n; i++) {
        r = Double.MAX_VALUE;
        Document di = (Document) _docs.elementAt(i);
        if (ind[i]>=0 && (numi[ind[i]]==1 || ((numi[ind[i]]/((double) n))<=perc_2_keep))) {
          continue;  // don't move the Document as it's the only one or block is very small
        }
        if (movable_exists && _clusterIndices!=null &&
            movable.contains(new Integer(_clusterIndices[i]))) {
          ind[i]=_clusterIndices[i];
          continue;  // Document i is not allowed to move
        }

        // if i is assigned to a center compute its distance first to avoid
        // movement when equal distances are involved
        if (ind[i]>=0) {
          r = distmetric.dist(di, (Document) _centers.elementAt(ind[i]));
        }

        for (int l=0; l<k; l++) {
          if (movable_exists && ind[i]!=l && movable.contains(new Integer(l))==false)
            continue;  // cannot move to partition l
          Document cl = (Document) _centers.elementAt(l);
          double rl = distmetric.dist(di, cl);
          if (rl<r) {
            r = rl;
            if (ind[i]>=0 && numi[ind[i]]>0) --numi[ind[i]];
            numi[l]++;
            ind[i] = l;
          }
        }
      }
      // while a cluster has size 0 move half the points from the largest
      // one
      while (half_max) {
        int min_c = Integer.MAX_VALUE;
        int min_c_ind = -1;
        int max_c = Integer.MIN_VALUE;
        int max_c_ind = -1;
        for (int i = 0; i < k; i++) {
          if (numi[i] < min_c) {
            min_c = numi[i];
            min_c_ind = i;
          }
          if (numi[i] > max_c) {
            max_c = numi[i];
            max_c_ind = i;
          }
        }
        if (min_c>0) break;
        for (int i=0; i<n; i++) {
          if (ind[i]==max_c_ind && numi[max_c_ind]>max_c/2) {
            numi[max_c_ind]--;
            ind[i] = min_c_ind;
            numi[min_c_ind]++;
          }
        }
      }

      // compute centers
      Vector centers_new = new Vector();
      for (int i=0; i<k; i++) centers_new.addElement(null);
      // implement hard K-Means
      for (int i=0; i<k; i++) {
        Vector v = new Vector();
        for (int j=0; j<n; j++)
          // if (ind[j]==i) v.addElement(_docs.elementAt(j));
          if (ind[j]==i) v.addElement(new Integer(j));
        if (v.size()>0 || project_on_empty)
          centers_new.set(i, Document.getDocumentCenterInds(v, _docs, distmetric));
        else centers_new.set(i, _centers.elementAt(i));
        // make sure center remains even w/o any points assoc with it
      }
      // in comment is the soft K-Means implementation
      /*
      for (int i=0; i<n; i++) {
        Document v = (Document) centers_new.elementAt(ind[i]);
        double c = h / numi[ind[i]];
        v.addMul(c, (Document) _docs.elementAt(i));
        v.addMul(-c, (Document) _centers.elementAt(ind[i]));
      }
      */
      _centers = centers_new;
      _clusterIndices = ind;
      // incumbent computation
      double new_val = eval(_evaluator);
      if (new_val<best_val) {
        best_val = new_val;
        best_centers.clear();
        for (int i=0; i<n; i++) best_indices[i] = _clusterIndices[i];
        for (int i=0; i<k; i++) best_centers.addElement(_centers.elementAt(i));
        // store this clustering in _intermediateClusters with a certain probability
        if (utils.RndUtil.getInstance().getRandom().nextDouble()<num_cur_iters*incr_prob) {  // as iterations increase, probability becomes certainty
          int prev_ic_sz = _intermediateClusters.size();
          for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
          for (int i=0; i<_clusterIndices.length; i++) {
            int c = _clusterIndices[i];
            Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz + c);
            vc.addElement(new Integer(i));
            _intermediateClusters.set(prev_ic_sz + c, vc); // ensure addition
          }
        }
      }
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
    // set incumbent
    for (int i=0; i<n; i++) _clusterIndices[i] = best_indices[i];
    _centers = best_centers;
    // test for intermediate clusters
    if (_intermediateClusters.size()==0) {
      // put the final clustering in
      for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
      for (int i=0; i<_clusterIndices.length; i++) {
        int c = _clusterIndices[i];
        Vector vc = (Vector) _intermediateClusters.elementAt(c);
        vc.addElement(new Integer(i));
        _intermediateClusters.set(c, vc); // ensure addition
      }
    }
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
   * the Document in _docs closest to each center is actually recorded as
   * the center, since this is the PMedian clustering problem.
   * @param centers Vector
   * @throws ClustererException
   */
  public void setInitialClustering(Vector centers) throws ClustererException {
    if (centers==null) throw new ClustererException("null initial clusters vector");
    if (_docs==null) throw new ClustererException("null _docs?");
    DocumentDistIntf distmetric = new DocumentDistL2();
    _centers = null;  // force gc
    _centers = new Vector();
    for (int i=0; i<centers.size(); i++) {
      Document orig_ci = (Document) centers.elementAt(i);
      double best_val = Double.MAX_VALUE;
      int best_ind = -1;
      for (int j=0; j<_docs.size(); j++) {
        Document dj = (Document) _docs.elementAt(j);
        double dij = distmetric.dist(orig_ci, dj);
        if (dij<best_val) {
          best_val = dij;
          best_ind = j;
        }
      }
      _centers.addElement(new Document(((Document) _docs.elementAt(best_ind))));
    }
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
}

