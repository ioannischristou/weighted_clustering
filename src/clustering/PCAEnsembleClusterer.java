package clustering;

import java.util.*;
import utils.Docs2MatrixConverter;
import cern.colt.matrix.*;
import onedclustering.Solver;
import coarsening.*;
import partitioning.*;

public class PCAEnsembleClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  int _k;
  int _t;  // _t is the number of neighbors that each node is allowed to have
           // in the clustering similarity matrix M.
  Vector _clusterings[];  // []Vector<Integer solind in [1...k]>

  public PCAEnsembleClusterer() {
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
   * <String "t", Integer MaxNumOfNeighborsInGraphToPartition> optional
   * <String "r", Integer MaxNumPCADimsToConsider> optional
   * <String "metric", DocumentDistIntf metric> optional
   * <String "partitioner", Partitioner parter> optional
   * <String "partitiondevperc", Double number> optional
   * <String "DPSolverNumThreads", Integer numthreads> optional
   * <String "similaritymatrixmaxallowedvalue", Double val> optional
   * <String "normalizematrix", Boolean val> optional
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d)
   *
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    _k = ( (Integer) _params.get("k")).intValue();
/*
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct
*/
    _t = 5;  // default value
    Integer t = (Integer) _params.get("t");
    if (t!=null) _t = t.intValue();
    int r = 0;
    Integer ri = (Integer) _params.get("r");
    if (ri!=null) r = ri.intValue();
    boolean use_smallest_svalues = false;
    Boolean invB = (Boolean) _params.get("usesmallestsingularvalues");
    if (invB!=null) use_smallest_svalues = invB.booleanValue();
    int num_threads=1;
    Integer numt_I = (Integer) _params.get("DPSolverNumThreads");
    if (numt_I!=null) num_threads = numt_I.intValue();
    DocumentDistIntf metric = (DocumentDistIntf) _params.get("metric");
    if (metric==null) metric = new DocumentDistL1();  // default value
    Document.setMetric(metric);

    _centers = new Vector();  // Vector<Document>
    final int n = _docs.size();
    final int dims = ((Document) _docs.elementAt(0)).getDim();
    if (_t>n-1) _t = n-1;  // sanity check
    // final DocumentDistIntf distmetric = new DocumentDistL1();

    Docs2MatrixConverter c = new Docs2MatrixConverter(_docs);
    int sz = dims >= n ? n : dims;
    double svalues[] = new double[sz];

    boolean use_covariance = false;
    Boolean covB = (Boolean) _params.get("usecovariance");
    if (covB!=null) use_covariance = covB.booleanValue();
    DoubleMatrix2D P;
    if (use_covariance==false) P = c.getProjectionOnSingularDimensions(svalues);
    else P = c.getCovarianceProjectionOnSingularDimensions(svalues);
    // cluster along each of the r largest dimensions of P
    if (P.columns()<r || r<=0) r = P.columns();
    _clusterings = new Vector[r];  // clusterings[i] holds the result
                                   // of clustering with the (i+1)-st
                                   // singular value
    // for (int i=0; i<r; i++) _clusterings[i] = new Vector();
    if (use_smallest_svalues==false) {
      for (int col = 0; col < r; col++) {
        DoubleMatrix1D p_col = P.viewColumn(col);

        Solver s = (metric instanceof DocumentDistL1) ?
            new Solver(new onedclustering.Params(p_col,
                                                 Double.POSITIVE_INFINITY, _k)) :
            new Solver(new onedclustering.Params(p_col,
                                                 Double.POSITIVE_INFINITY, _k,
                                                 onedclustering.Params._L2));

        // Solver s = new Solver(new onedclustering.Params(p_col, Double.POSITIVE_INFINITY, _k));
        double val = (num_threads == 1) ? s.solveDPMat() :
            s.solveDP2ParallelMat(num_threads);
        Vector solindices = s.getSolutionIndices();
        _clusterings[col] = solindices;
      }
    }
    else {  // use smallest components
      for (int col = 0; col < r; col++) {
        DoubleMatrix1D p_col = P.viewColumn(P.columns()-1-col);

        Solver s = (metric instanceof DocumentDistL1) ?
            new Solver(new onedclustering.Params(p_col,
                                                 Double.POSITIVE_INFINITY, _k)) :
            new Solver(new onedclustering.Params(p_col,
                                                 Double.POSITIVE_INFINITY, _k,
                                                 onedclustering.Params._L2));

        // Solver s = new Solver(new onedclustering.Params(p_col, Double.POSITIVE_INFINITY, _k));
        double val = (num_threads == 1) ? s.solveDPMat() :
            s.solveDP2ParallelMat(num_threads);
        Vector solindices = s.getSolutionIndices();
        _clusterings[col] = solindices;
      }
    }

    // compute the similarity matrix; non-zero elements only for
    // close neighbors in the original space
    double similarity[][] = computeSimilarityMatrix(_clusterings, svalues, r);
    partitionClusteringsGraph(similarity);  // sets _clusterIndices

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
    _clusterIndices=null;
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
   * computes the similarity matrix M[n x n] of the n Documents _docs.
   * M[i][j] is the sum of the singular values of the principal component dimensions
   * for which node i and node j belong to the same cluster.
   * However, the number is set to zero if j is not among the _t closest
   * neighbors of i.
   * 1st argument is an array of Vectors, each holding the clustering along
   * the i-th singular value dimension.
   * 2nd argument is the array of non-zero singular values of the A = _docs matrix.
   * 3rd argument is the number of singular value projections to consider.
   * @param clusterings Vector[]
   * @param svalues double[]
   * @param rmax int
   * @return double[][]
   */
  private double[][] computeSimilarityMatrix(Vector[] clusterings, double[] svalues, int rmax) throws ClustererException {
    final int n = _docs.size();
    final int m = ((Document) _docs.elementAt(0)).getDim();
    double max_allowed_value;
    Double max_allowed_valueD = (Double) _params.get("similaritymatrixmaxallowedvalue");
    boolean normalize_matrix = false;  // default
    Boolean nmB = (Boolean) _params.get("normalizematrix");
    if (nmB!=null) normalize_matrix = nmB.booleanValue();
    // consider the largest or the smallest singular values and vectors?
    boolean use_smallest_svalues = false;
    Boolean invB = (Boolean) _params.get("usesmallestsingularvalues");
    if (invB!=null) use_smallest_svalues = invB.booleanValue();

    if (max_allowed_valueD!=null) max_allowed_value = max_allowed_valueD.doubleValue();
    else max_allowed_value = 1000;  // default

    double matrix[][] = new double[n][n];
    DocDist arr[] = new DocDist[n];
    Set allowed_inds = new HashSet();
    double max_elem = 0.0;
    for (int row=0; row<n; row++) {
      // find the _t closest neighbors of the row-th Document
      Document drow = (Document) _docs.elementAt(row);
      for (int j=0; j<n; j++) {
        Document dj = (Document) _docs.elementAt(j);
        arr[j] = new DocDist(row, j, Document.d(drow, dj));
      }
      Arrays.sort(arr);
      // the first _t elements of arr are the ones allowed to be > 0
      allowed_inds.clear();
      for (int j=0; j<_t; j++) allowed_inds.add(new Integer(arr[j]._j));
      for (int col=0; col<n; col++) {
        // compute matrix[row][col]
        if (row==col) {
          matrix[row][col] = 0;
          continue; // obviously there is similarity between i and i but who cares?
        }
        // short-cut: col is not in allowed_inds
        if (allowed_inds.contains(new Integer(col))==false) {
          matrix[row][col] = 0.0;
        }
        else {
          double sum = 0.0;
          if (use_smallest_svalues==false) {
            for (int r = 0; r < rmax; r++) {
              Vector cr = clusterings[r];
              int rv = ( (Integer) cr.elementAt(row)).intValue();
              int cv = ( (Integer) cr.elementAt(col)).intValue();
              if (rv == cv) sum += svalues[r];
            }
          }
          else {  // use the smallest svalues and eigenvectors to compute
                  // the similarity matrix
            for (int r = 0; r < rmax; r++) {
              Vector cr = clusterings[clusterings.length-1-r];
              int rv = ( (Integer) cr.elementAt(row)).intValue();
              int cv = ( (Integer) cr.elementAt(col)).intValue();
              if (rv == cv) sum += 1.0/(svalues[r]*svalues[r]);
            }
          }
          if (normalize_matrix) {
            Document dcol = (Document) _docs.elementAt(col);
            double doc_rc_dist = Document.d(drow, dcol);
            // watch out for zero distance between documents
            if (Math.abs(doc_rc_dist) < 1.e-16)
              matrix[row][col] = Double.MAX_VALUE;
            else {
              // matrix[row][col] = sum / doc_rc_dist;
              matrix[row][col] = sum;
              // compute the max non-infinite value in the matrix
              if (matrix[row][col] > max_elem) max_elem = matrix[row][col];
            }
          }
          matrix[row][col] = sum;
        }
      }
    }
    if (normalize_matrix) {
      // normalize matrix
      double min_elem = Double.MAX_VALUE;
      for (int row = 0; row < n; row++) {
        for (int col = 0; col < n; col++) {
          if (matrix[row][col] > max_elem) {
            // infinite value
            matrix[row][col] = max_allowed_value;
          }
          else matrix[row][col] = max_allowed_value * matrix[row][col] /
                                  max_elem;
          if (row != col && min_elem > matrix[row][col] && matrix[row][col] > 0)
            min_elem = matrix[row][col];
        }
      }
      System.err.println("max_elem=" + max_elem + " min_nonzero_elem=" +
                         min_elem);  // itc: HERE rm asap
    }
    return matrix;
  }


  /**
   * create the Graph corresponding to the similarity matrix passed in as
   * argument.
   * The graph's arcs are bi-directional, i.e. if there is an arc from i->j with
   * weight w, then there is also an arc from j->i with weight w.
   * @param similarity double[][]
   * @throws GraphException
   * @return Graph
   */
  private Graph createGraphFromClusterEnsemble(double similarity[][]) throws GraphException {
    final int num_nodes = _docs.size();
    int num_edges = 0;
    for (int row=0; row<num_nodes; row++) {
      for (int col = 0; col < num_nodes; col++) {
        if (row==col) continue;
        if (similarity[row][col] > 0 || similarity[col][row] > 0) num_edges++;
      }
    }
    Graph g = new Graph(num_nodes, num_edges);
    for (int row=0; row<num_nodes; row++) {
      for (int col = 0; col < num_nodes; col++) {
        if (row==col) continue;
        double val = (similarity[row][col] + similarity[col][row])/2.0;
        // the graph's arcs are bi-directional, i.e. if there is an arc
        // from i->j with weight w, then there is also an arc from j->i with
        // weight w.
        if (val > 0) {
          g.addLink(row, col, val);
        }
      }
    }
    return g;
  }


  /**
   * create the Graph corresponding to the similarity matrix passed in as first
   * argument, and partition the graph among _k components. These components become
   * the clusters, and _clusterIndices is updated accordingly
   * @param similarity double[][]
   */
  private void partitionClusteringsGraph(double similarity[][]) throws GraphException {
    final int num_nodes = _docs.size();
    Graph g = createGraphFromClusterEnsemble(similarity);

    // params for *MeTiS partitioners
    String partitionerpath = (String) _params.get("partitionerpath");
    String partitionername = (String) _params.get("partitionername");
    String graphfile = (String) _params.get("graphfilename");
    String codocupgraphfile = (String) _params.get("codocupgraphfilename");

    try {
      utils.DataMgr.writeGraphToHGRFile(g, partitionerpath+"/"+graphfile);
      // save for CoDoCUp experiments
      if (codocupgraphfile!=null) utils.DataMgr.writeGraphToFile(g, codocupgraphfile);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    // partition graph
    Partitioner parter = (Partitioner) _params.get("partitioner");
    if (parter==null) parter = new PartitionerGKWR();  // default value
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
    params.put("numnodes",new Integer(num_nodes));  // for kHMeTiS
    int[] partition = parter.partition(g, _k, params);
    // copy results to _clusterIndices
    if (_clusterIndices==null) _clusterIndices = new int[num_nodes];
    for (int i=0; i<num_nodes; i++) _clusterIndices[i] = partition[i]-1;
  }
}


class DocDist implements Comparable {
  int _i, _j;
  double _dist;

  DocDist(int i, int j, double dist) {
    _i = i; _j = j; _dist = dist;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      DocDist dd = (DocDist) o;
      if (_dist==dd._dist) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return (int) Math.floor(_dist);
  }

  public int compareTo(Object o) {
    DocDist c = (DocDist) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_dist < c._dist) return -1;
    else if (_dist == c._dist) return 0;
    else return 1;
  }
}
