package clustering;

import java.util.*;

public class KMedianClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  private int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>

  public KMedianClusterer() {
    _intermediateClusters = new Vector();
  }


  public Hashtable getParams() {
    return _params;
  }


  public Vector getIntermediateClusters() throws ClustererException {
    return _intermediateClusters;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   *
   * <"h",double> the rate of learning in soft K-Median Clustering
   * <"TerminationCriteria",ClustererTermination> the object that will
   * decide when to stop the iterations.
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setInitialClustering(Vector<Document> clusterCenters)
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws ClustererException {
    final double h = ( (Double) _params.get("h")).doubleValue();
    boolean project_on_empty = false;  // default
    Boolean p_o_e = (Boolean) _params.get("projectonempty");
    if (p_o_e!=null) project_on_empty = p_o_e.booleanValue();
    Double incr_probD = (Double) _params.get("irenterprob");
    double incr_prob = incr_probD!=null ? incr_probD.doubleValue() : 0.1;

    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centers.size();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = new DocumentDistL1();
    double r = Double.MAX_VALUE;
    int ind[] = new int[n];  // to which cluster each doc belongs
    int numi[] = new int[k];  // how many docs each cluster has
    int num_cur_iters = 0;
    boolean last_clustering_entered = false;
    while (!ct.isDone()) {
      num_cur_iters++;
      // System.err.println("KMedianClusterer.clusterDocs(): start new loop...");
      for (int i=0; i<k; i++) numi[i]=0;
      for (int i=0; i<n; i++) {
        r = Double.MAX_VALUE;
        ind[i]=-1;
        Document di = (Document) _docs.elementAt(i);
        for (int l=0; l<k; l++) {
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
      Vector centers_new = new Vector();
      for (int i=0; i<k; i++) {
        // centers_new.addElement(new Document(new TreeMap(), dims));
        centers_new.addElement(null);
      }
      // implement hard K-Median
      for (int i=0; i<k; i++) {
        Vector v = new Vector();
        for (int j=0; j<n; j++)
          if (ind[j]==i) v.addElement(_docs.elementAt(j));
        if (v.size()==0 && project_on_empty==false) {
          // make sure center i remains even w/o any points associated
          // System.err.println("KMedianClusterer.clusterDocs(): "+i+"-th cluster empty...");
          centers_new.set(i, _centers.elementAt(i));
        }
        else centers_new.set(i, Document.getCenter(v, null));  // itc: HERE 20220223
      }
      // in comment is the soft K-Median implementation
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
        last_clustering_entered = true;
      }
      else last_clustering_entered = false;
    }
    // System.err.println("KMedianClusterer.clusterDocs(): ended loop...");
    // store final clustering in _intermediateClusters
    if (last_clustering_entered==false) {
      int prev_ic_sz = _intermediateClusters.size();
      for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
      for (int i=0; i<_clusterIndices.length; i++) {
        int c = _clusterIndices[i];
        Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
        vc.addElement(new Integer(i));
        _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
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
    else {  // deep copy
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

