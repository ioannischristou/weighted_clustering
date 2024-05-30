package clustering;

import java.util.*;
import onedclustering.*;

public class Rand1DProjClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  Evaluator _evaluator;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>

  public Rand1DProjClusterer() {
    _intermediateClusters = new Vector();
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


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   * <"metric", DocumentDistIntf metric> the metric to use when clustering
   * <"numthreads", Integer numthreads> optional the #threads to use in
   * computing the optimal 1-D clustering.
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setInitialClustering(Vector<Document clusterCenters>)
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {

    final int n = _docs.size();
    final int k = _centers.size();
    final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");

    int numthreads = -1;
    Integer ntI = (Integer) _params.get("numthreads");
    if (ntI!=null && ntI.intValue()>1) numthreads = ntI.intValue();

    // get a random line along which to project data
    double line[] = new double[dims];
    for (int i=0; i<dims; i++) {
      line[i] = utils.RndUtil.getInstance().getRandom().nextDouble();
    }

    // get the projection
    double dataprojections[] = new double[n];
    for (int i=0; i<n; i++) {
      dataprojections[i] = 0;
      for (int j=0; j<dims; j++) {
        dataprojections[i] += line[j]*((Document) _docs.elementAt(i)).getDimValue(new Integer(j)).doubleValue();
      }
    }

    // optimally cluster 1-D array
    Solver s = new Solver(new Params(dataprojections, Double.MAX_VALUE, k, Params._L2));
    if (numthreads>1)
      s.solveDP2ParallelMat(numthreads);
    else s.solveDPMat();

    // get clustering indices
    Vector solinds = s.getSolutionIndices();
    _clusterIndices = new int[n];
    for (int i=0; i<n; i++)
      _clusterIndices[i] = ((Integer) solinds.elementAt(i)).intValue()-1;

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
}

