package clustering;

import java.util.*;
import utils.*;

public class PMedianRandGreedyClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  double[][] _d;  // document distance matrix
  Vector _nbors;  // Vector<int[] nnborids>
  PMedianEvaluator _evaluator;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>
  private int _numdocs2consider;

  public PMedianRandGreedyClusterer() {
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
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d)
   *
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    final int n = _docs.size();
    final int p = _centers.size();
    DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    if (distmetric==null)
      distmetric = new DocumentDistL2();
    Integer nd2cI = (Integer) _params.get("numdocs2consider");
    if (nd2cI!=null) _numdocs2consider = nd2cI.intValue();
    else _numdocs2consider = 100;

    // 0. compute distance matrix D[i,j]
    // itc: HERE use cache
    if (_d==null) {
      _d = new double[n][n];
      for (int i = 0; i < n; i++) {
        Document di = (Document) _docs.elementAt(i);
        _d[i][i] = 0.0;
        for (int j = i + 1; j < n; j++) {
          Document dj = (Document) _docs.elementAt(j);
          double dist = distmetric.dist(di, dj);
          _d[i][j] = dist;
          _d[j][i] = dist;
        }
      }
    }

    Vector center_inds = new Vector();  // Vector<Integer doc_id>
    // 1. find the center of all docs and the document closest to the center
    // assign everyone to c0
    _clusterIndices = new int[n];
    for (int i=0; i<n; i++) _clusterIndices[i] = 0;
    Vector docinds = new Vector();
    for (int i=0; i<n; i++) docinds.addElement(new Integer(i));
    int c0_ind = Document.getDocumentIndCenterInds(docinds, _docs, _d);

    _centers.set(0, _docs.elementAt(c0_ind));
    center_inds.addElement(new Integer(c0_ind));

    // 2. repeatedly find the document di that produces the best improvement if it
    // becomes a facility among a random subset of possible facilities
    for (int i=1; i<p; i++) {
      // choose from a set of randomly picked locations the best one to add as
      // facility
      int[] candidate_ids = getCandidates(n,i,center_inds);
      int best_ind = getBestSpot4Facility(candidate_ids, center_inds);  // updates _clusterIndices as well
      center_inds.addElement(new Integer(best_ind));
      _centers.set(i, _docs.elementAt(best_ind));
    }

    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
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

    Boolean use_alternate_pmedianB = (Boolean) _params.get("use_alternate_pmedian");
    if (use_alternate_pmedianB!=null && use_alternate_pmedianB.booleanValue()==true) {
      runAlternatePMedian(center_inds);
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


  private int[] getCandidates(int n, int i, Vector center_inds) {
    // int num = (int) (Math.log((double)n/ (double) i) / Math.log(2.0));
    int num = _numdocs2consider;  // try out more documents...
    int arr[] = new int[num];
    HashSet cinds = new HashSet(center_inds);
    for (int j=0; j<num; j++) {
      arr[j] = utils.RndUtil.getInstance().getRandom().nextInt(n);
      if (cinds.contains(new Integer(arr[j])))
        j--;  // ensure all the candidates are new candidates
    }
    return arr;
  }


  /**
   * we are given an array of ids of candidate centers (facilities). Given also
   * are the ids of the documents in _docs that serve as current centers.
   * The objective is to locate the index of a point in _docs that will
   * offer the best improvement in the P-Median objective function if we add it
   * as a facility. The documents that are assigned to it also get updated in the
   * _clusterIndices array.
   * @param candidate_ids int[]
   * @param center_inds Vector
   * @return int
   */
  private int getBestSpot4Facility(int[] candidate_ids, Vector center_inds) {
    final int n = _docs.size();
    int p = center_inds.size();
    int best_ind = -1;
    double best_impr = Double.MAX_VALUE;
    for (int j=0; j<candidate_ids.length; j++) {
      int ind_j = candidate_ids[j];
      double impr = 0;
      for (int i=0; i<n; i++) {
        int c_ii = ((Integer) center_inds.elementAt(_clusterIndices[i])).intValue();
        double val = _d[i][ind_j] - _d[i][c_ii];
        impr += (val < 0 ? val : 0);
      }
      if (impr<best_impr) {
        best_ind = ind_j;
        best_impr = impr;
      }
    }
    // update _clusterIndices
    for (int i=0; i<n; i++) {
      int c_ii = ((Integer) center_inds.elementAt(_clusterIndices[i])).intValue();
      if (_d[i][best_ind] - _d[i][c_ii] < 0)
        _clusterIndices[i] = p;
    }

    return best_ind;
  }


  /**
   * run the Alternate-PMedian algorithm.
   * To speed up the computations, only a subset of the _docs is considered as
   * a new center, and in particular a number of the closest nbors of the
   * current center.
   * @param center_inds Vector<Integer>
   */
  private void runAlternatePMedian(Vector center_inds) throws ClustererException {
    final int n = _docs.size();
    final int nd2c = _numdocs2consider/5;
    final int k = _centers.size();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = new DocumentDistL2();
    Double incr_probD = (Double) _params.get("irenterprob");
    double incr_prob = incr_probD!=null ? incr_probD.doubleValue() : 0.1;

    // 1. if _nbors hashtable is null, fill it up
    if (_nbors==null) {
      _nbors = new Vector();
      Pair[] dists = new Pair[n];
      for (int i = 0; i < n; i++) {
        for (int j=0; j < n; j++) {
          dists[j] = new Pair(new Integer(j), new Double(_d[i][j]));
        }
        Arrays.sort(dists, new I2DComptor());
        int[] nborinds = new int[nd2c];
        for (int k2=0; k2<nd2c; k2++) {
          nborinds[k2] = ((Integer) dists[k2].getFirst()).intValue();
        }
        _nbors.addElement(nborinds);
      }
    }

    int[] centerind = new int[k];
    for (int i=0; i<k; i++) centerind[i] = ((Integer) center_inds.elementAt(i)).intValue();

    // 2. run Alternate PMedian now
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct
    double r = Double.MAX_VALUE;
    int ind[] = new int[n];  // to which cluster each doc belongs
    int numi[] = new int[k];  // how many docs each cluster has
    // initialize
    for (int i=0; i<n; i++) {
      ind[i] = _clusterIndices[i];
      numi[ind[i]]++;
    }
    int best_indices[] = new int[n];
    Vector best_centers = new Vector();  // incumbent iteration
    double best_val = Double.MAX_VALUE;
    int num_cur_iters = 0;
    while (!ct.isDone()) {
      ++num_cur_iters;
      for (int i=0; i<n; i++) {
        r = Double.MAX_VALUE;
        Document di = (Document) _docs.elementAt(i);
        if (ind[i]>=0 && numi[ind[i]]==1) {
          continue;  // don't move the Document as it's the only one or block is very small
        }

        // if i is assigned to a center compute its distance first to avoid
        // movement when equal distances are involved
        if (ind[i]>=0) {
          // r = distmetric.dist(di, (Document) _centers.elementAt(ind[i]));
          r = _d[i][centerind[ind[i]]];
        }

        for (int l=0; l<k; l++) {
          // Document cl = (Document) _centers.elementAt(l);
          // double rl = distmetric.dist(di, cl);
          double rl = _d[i][centerind[l]];
          if (rl<r) {
            r = rl;
            if (ind[i]>=0 && numi[ind[i]]>0) --numi[ind[i]];
            numi[l]++;
            ind[i] = l;
          }
        }
      }

      // compute centers
      Vector centers_new = new Vector();
      for (int i=0; i<k; i++) centers_new.addElement(null);
      for (int i=0; i<k; i++) {
        Vector v = new Vector();
        for (int j=0; j<n; j++)
          // if (ind[j]==i) v.addElement(_docs.elementAt(j));
          if (ind[j]==i) v.addElement(new Integer(j));
        if (v.size()>0) {
          // get the collection to consider
          // itc: HERE and put in collection the actual cluster as well
          int[] coll_nbor = (int[]) _nbors.elementAt(centerind[i]);
          int[] coll = new int[coll_nbor.length+v.size()];
          for (int j=0; j<coll_nbor.length; j++)
            coll[j] = coll_nbor[j];
          for (int j=0; j<v.size(); j++)
            coll[j+coll_nbor.length] = ((Integer) v.elementAt(j)).intValue();
          int coll_ind = Document.getDocumentIndCenterCollInds(v,_docs,coll,_d);  // itc: HERE _d used to be distmetric
          Document ci = (Document) _docs.elementAt(coll[coll_ind]);
          centers_new.set(i, new Document(ci));
          centerind[i] = coll[coll_ind];
        }
        else centers_new.set(i, _centers.elementAt(i));
        // make sure center remains even w/o any points assoc with it
      }
      _centers = centers_new;
      _clusterIndices = ind;
      // incumbent computation
      // double new_val = eval(_evaluator);
      double new_val = eval(centerind, ind);  // itc: HERE evaluate the current asgnmnt with the chosen centers
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
  }


  private double eval(int[] centerinds, int[] inds) {
    double res = 0.0;
    final int n = inds.length;
    for (int i=0; i<n; i++) {
      res += _d[centerinds[inds[i]]][i];
    }
    return res;
  }

}


class I2DComptor implements Comparator {
  public int compare(Object o1, Object o2) {
    Pair p1 = (Pair) o1;
    Pair p2 = (Pair) o2;
    double val1 = ((Double) p1.getSecond()).doubleValue();
    double val2 = ((Double) p2.getSecond()).doubleValue();
    if (val1 < val2) return -1;
    else if (val1==val2) return 0;
    else return 1;
  }
}
