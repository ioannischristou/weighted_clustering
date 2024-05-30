package clustering;

import java.util.*;
import utils.Docs2MatrixConverter;
import cern.colt.matrix.*;
import onedclustering.Solver;

public class PCAClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  int _k;

  public PCAClusterer() {
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
   * <String "k", Integer NumberOfClusters> the number of clusters
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    _k = ( (Integer) _params.get("k")).intValue();
    int num_threads=1;
    Integer numt_I = (Integer) _params.get("DPSolverNumThreads");
    if (numt_I!=null) num_threads = numt_I.intValue();

    _centers = new Vector();  // Vector<Document>
    final int n = _docs.size();
    final int dims = ((Document) _docs.elementAt(0)).getDim();
    DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    if (distmetric==null) distmetric = new DocumentDistL1();  // default
    Document.setMetric(distmetric);

    Docs2MatrixConverter c = new Docs2MatrixConverter(_docs);
    double svalues[] = new double[dims];
    DoubleMatrix2D P = c.getProjectionOnSingularDimensions(svalues);
    // use only one dimension, to cluster projections there
    boolean use_smallest_svalues = false;
    Boolean invB = (Boolean) _params.get("usesmallestsingularvalues");
    if (invB!=null) use_smallest_svalues = invB.booleanValue();
    DoubleMatrix1D p1;
    if (use_smallest_svalues==false) p1 = P.viewColumn(0);
    else p1 = P.viewColumn(P.columns()-1);

    Solver s = distmetric instanceof DocumentDistL1 ?
        new Solver(new onedclustering.Params(p1, Double.POSITIVE_INFINITY, _k)) :
        new Solver(new onedclustering.Params(p1, Double.POSITIVE_INFINITY, _k, onedclustering.Params._L2));

    // Solver s = new Solver(new onedclustering.Params(p1, Double.POSITIVE_INFINITY, _k));
    double val=-1;
    if (num_threads==1) val = s.solveDPMat();
    else val = s.solveDP2ParallelMat(num_threads);
    Vector solindices = s.getSolutionIndices();
    // set cluster indices
    _clusterIndices = new int[n];
    for (int i=0; i<n; i++) {
      _clusterIndices[i] = ((Integer) solindices.elementAt(i)).intValue()-1;
    }
    // compute the _centers, slow version
    Vector aux[] = new Vector[_k];
    for (int i=0; i<_k; i++) aux[i] = new Vector();
    for (int i=0; i<n; i++) {
      int k = _clusterIndices[i];
      Document di = (Document) _docs.elementAt(i);
      aux[k].addElement(di);
    }
    for (int i=0; i<_k; i++) {
      Document center_i = Document.getCenter(aux[i], null);  // itc-20220223: HERE
      _centers.addElement(center_i);
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
   * This class does not support this method
   * @param centers Vector
   * @throws ClustererException
   */
  public void setInitialClustering(Vector centers) throws ClustererException {
    // throw new ClustererException("unsupported");
    return;
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

