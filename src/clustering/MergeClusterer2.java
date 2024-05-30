package clustering;

import java.util.*;

public class MergeClusterer2 implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  private int[] _clusterIndices;
  private int[] _centerIndices;

  public MergeClusterer2() {
  }


  public Hashtable getParams() {
    return _params;
  }


  public Vector getIntermediateClusters() throws ClustererException {
    throw new ClustererException("method not supported");
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   *
   * "metric", DocumentDistIntf
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setClusteringIndices(int asgns[])
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    final int n = _docs.size();
    // final int k = ((Integer) _params.get("k")).intValue();
    int k3 = ((Integer) _params.get("k")).intValue();
    try {
      k3 = ((Integer) _params.get("k_try")).intValue();
    }
    catch (Exception e) {
      // no-op
    }
    final int k = k3;
    final int num_clusters = ((Integer) _params.get("num_clusters")).intValue();
    _centers = Document.getCenters(_docs, _clusterIndices, num_clusters, null);  // itc: HERE 20220223
    DocumentDistIntf metric = (DocumentDistIntf) _params.get("metric");
    if (metric==null) metric = new DocumentDistL1();
    Clusterer cl = new GMeansMTClusterer();
    cl.setParams(_params);
    cl.addAllDocuments(_centers);
    Vector randcenters = new Vector();
    for (int i=0; i<k; i++) {
      Document ci = new Document(
        (Document) _docs.elementAt(
          utils.RndUtil.getInstance().getRandom().nextInt(num_clusters)));
      randcenters.addElement(ci);
    }
    cl.setInitialClustering(randcenters);
    cl.clusterDocs();  // cluster the centers!
    // now figure out the new cluster indices
    int cl_asgns[] = cl.getClusteringIndices();
    for (int i=0; i<n; i++)
      _clusterIndices[i] = cl_asgns[_clusterIndices[i]];
    // finally compute _centers
    _centers = Document.getCenters(_docs, _clusterIndices, k, null);  // itc: HERE 20220223
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
    _clusterIndices=null;  // itc: HERE
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
