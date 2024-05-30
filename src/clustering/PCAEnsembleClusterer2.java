package clustering;

import java.util.*;
import utils.Docs2MatrixConverter;
import cern.colt.matrix.*;
import onedclustering.Solver;
import coarsening.*;
import partitioning.*;

public class PCAEnsembleClusterer2 implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  int _k;
  Vector _clusterings[];  // clusterings[i] holds the result
                          // of clustering with the (i+1)-st
                          // singular value

  public PCAEnsembleClusterer2() {
  }


  public Hashtable getParams() {
    return _params;
  }


  public Vector getIntermediateClusters() throws ClustererException {
    // use the _clusterings Vector[] to produce the
    // required Vector<Vector<Integer doc_index> >
    if (_clusterings==null)
      throw new ClustererException("PCAEnsembleClusterer: clusterDocs() must be called first...");
    final int n = _docs.size();
    Vector res = new Vector();
    Vector ir[] = new Vector[_k];
    for (int i=0; i<_clusterings.length; i++) {
      for (int j=0; j<_k; j++) ir[j] = new Vector();  // init
      Vector solinds_i = _clusterings[i];
      for (int j=0; j<n; j++) {
        int cj = ((Integer) solinds_i.elementAt(j)).intValue() - 1;
        ir[cj].addElement(new Integer(j));
      }
      for (int j=0; j<_k; j++) res.addElement(ir[j]);
    }
    // add the final solution if it exists
    if (_clusterIndices!=null) {
      for (int j=0; j<_k; j++) ir[j] = new Vector();  // init
      for (int j=0; j<n; j++) {
        int cj = _clusterIndices[j];
        ir[cj].addElement(new Integer(j));
      }
      for (int j=0; j<_k; j++) res.addElement(ir[j]);
    }
    return res;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   * <String "k", Integer NumberOfClusters> the number of clusters
   * <String "partitionerpath", String path> the path to find the partitioner
   * <String "partitionername", String filename> the filename of the executable
   * <String "graphfilename", String graphfile> the filename of the graph to partition
   * <String "r", Integer MaxNumPCADimsToConsider> optional
   * <String "metric", DocumentDistIntf metric> optional
   * <String "partitioner", Partitioner parter> optional
   * <String "partitiondevperc", Double number> optional
   * <String "DPSolverNumThreads", Integer numthreads> optional
   * <String "similaritymatrixmaxallowedvalue", Double val> optional
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d)
   *
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    _k = ( (Integer) _params.get("k")).intValue();
    int r = 0;
    Integer ri = (Integer) _params.get("r");
    if (ri!=null) r = ri.intValue();
    int num_threads=1;
    Integer numt_I = (Integer) _params.get("DPSolverNumThreads");
    if (numt_I!=null) num_threads = numt_I.intValue();
    DocumentDistIntf metric = (DocumentDistIntf) _params.get("metric");
    if (metric==null) metric = new DocumentDistL1();  // default value
    Document.setMetric(metric);

    _centers = new Vector();  // Vector<Document>
    final int n = _docs.size();
    final int dims = ((Document) _docs.elementAt(0)).getDim();

    Docs2MatrixConverter c = new Docs2MatrixConverter(_docs);
    int sz = dims >= n ? n : dims;
    double svalues[] = new double[sz];
    DoubleMatrix2D P = c.getProjectionOnSingularDimensions(svalues);
    // cluster along each of the r largest dimensions of P
    if (P.columns()<r || r<=0) r = P.columns();
    _clusterings = new Vector[r];
    // for (int i=0; i<r; i++) _clusterings[i] = new Vector();
    for (int col=0; col<r; col++) {
      DoubleMatrix1D p_col = P.viewColumn(col);
/*
      Solver s = (metric instanceof DocumentDistL1) ?
          new Solver(new onedclustering.Params(p_col,
          Double.POSITIVE_INFINITY, _k)) :
          new Solver(new onedclustering.Params(p_col,
          Double.POSITIVE_INFINITY, _k, onedclustering.Params._L2));
*/
      Solver s = new Solver(new onedclustering.Params(p_col, Double.POSITIVE_INFINITY, _k));
      double val = (num_threads==1) ? s.solveDPMat() :
                                      s.solveDP2ParallelMat(num_threads);
      Vector solindices = s.getSolutionIndices();
      _clusterings[col] = solindices;
    }

    partitionClusteringsHGraph(_clusterings, svalues);  // sets _clusterIndices

    // finally, compute the _centers, slow version
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


  /**
   *
   * @param clusterings Vector[]
   * @param n int
   * @param r int
   */
  private void partitionClusteringsHGraph(Vector clusterings[], double svalues[]) {
    final int num_nodes = _docs.size();
    final int r = clusterings.length;
    // params for kHMeTiS partitioner
    String partitionerpath = (String) _params.get("partitionerpath");
    String partitionername = (String) _params.get("partitionername");
    String graphfile = (String) _params.get("graphfilename");
    try {
      utils.DataMgr.writeClusterEnsembleToHGRFile(clusterings, svalues, num_nodes, r, _k, partitionerpath+"/"+graphfile);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    // partition graph
    Partitioner parter = (Partitioner) _params.get("partitioner");
    if (parter==null) parter = new PartitionerkHMeTiS();  // default value
    Hashtable params = new Hashtable();
    Double pdp = (Double) _params.get("partitiondevperc");
    if (pdp == null) pdp = new Double(0.95);  // default value
    params.put("partitiondevperc", pdp);
    Integer num_tries = (Integer) _params.get("numtries");
    if (num_tries!=null) params.put("numtries",num_tries);
    // for *MeTiS
    params.put("partitionerpath", partitionerpath);
    params.put("partitionername", partitionername);
    params.put("graphfilename", graphfile);
    params.put("numnodes", new Integer(num_nodes));
    int[] partition = parter.partition(null, _k, params);
    // copy results to _clusterIndices
    if (_clusterIndices==null) _clusterIndices = new int[num_nodes];
    for (int i=0; i<num_nodes; i++) _clusterIndices[i] = partition[i]-1;
  }
}
