package clustering;

import java.util.*;

public class PMedianRandGreedyClustererR implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>
  private PMedianRandGreedyClusterer _cl=null;
  private Evaluator _eval = null;

  public PMedianRandGreedyClustererR() {
    _cl = new PMedianRandGreedyClusterer();
  }


  public Vector clusterDocs() throws Exception {
    _eval = (Evaluator) _params.get("evaluator");
    if (_eval instanceof EnhancedEvaluator) {
      ((EnhancedEvaluator) _eval).setParams(_params);
    }
    int numtries = 1;  // default
    Integer ntI = (Integer) _params.get("numtries");
    if (ntI!=null) numtries=ntI.intValue();
    _cl.addAllDocuments(_docs);
    _cl.setParams(_params);
    double bestval=Double.MAX_VALUE;
    _cl.setInitialClustering(_centers);
    // this is only needed so that PMedianRandGreedyClusterer can find out the
    // value of p
    for (int i=0; i<numtries; i++) {
      _cl.clusterDocs();
      double val = _cl.eval(_eval);
      System.err.println("try #"+i+" val="+val);
      if (val<bestval) {
        bestval = val;
        if (_clusterIndices==null) _clusterIndices = new int[_docs.size()];
        for (int j=0; j<_clusterIndices.length; j++)
          _clusterIndices[j] = _cl._clusterIndices[j];
        if (this._centers==null) _centers = new Vector();
        _centers.clear();
        _centers = new Vector(_cl.getCurrentCenters());
        if (_intermediateClusters==null) _intermediateClusters = new Vector();
        _intermediateClusters.addAll(_centers);
      }
    }
    return _centers;
  }


  public Vector getIntermediateClusters() throws ClustererException {
    return _intermediateClusters;
  }


  public Hashtable getParams() {
    return _params;
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
}
