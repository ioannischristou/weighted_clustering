package clustering;

import java.util.*;

public class DetAnnealClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  KMeansSqrEvaluator _evaluator;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>
  private final DocumentDistIntf _distmetric = new DocumentDistL2Sqr();
  private static double _eps = 1.e-3;

  public DetAnnealClusterer() {
    _evaluator = new KMeansSqrEvaluator();
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
   * <"a",double> the rate of annealing
   * <"Tmin",double> the Tmin value which when reached, we stop the annealing.
   * <"lmax",double> the �max(Cx) which instead of being computed must be passed in
   * <"delta",double> the � perturbation when creating a new Document center
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d)
   *
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {

    // 1. init
    final double a = ( (Double) _params.get("a")).doubleValue();
    final double lmax = ( (Double) _params.get("lmax")).doubleValue();
    final double Tmin = ( (Double) _params.get("Tmin")).doubleValue();
    final double delta = ( (Double) _params.get("delta")).doubleValue();
    final int n = _docs.size();
    final int k = ((Integer) _params.get("k")).intValue();
    final int dims = ((Document) _docs.elementAt(0)).getDim();
    // set a new _eps for deciding when centers have settled
    _eps = _eps > delta/dims ? delta/dims : _eps;
    TreeMap onemap = new TreeMap();
    for (int i=0; i<dims; i++)
      onemap.put(new Integer(i), new Double(1.0));
    final Document one = new Document(onemap, dims);

    double T = 2*lmax+1;
    int k_cur = 2;  // count the duplicate too
    double[] p_y = new double[2*k];  //  p_y[i] = p_{y_i}
    double[][] p_yx = new double[2*k][n];  // p_yx[i=0...2k-1][j=0...n-1] = p_{y[i]|j}
    for (int i=0; i<2*k; i++) {  // init arrays
      p_y[i] = 0.0;
      for (int j=0; j<n; j++) p_yx[i][j] = 0.0;
    }

    if (_centers!=null) _centers.clear();
    else _centers = new Vector();  // _centers is not supposed to be initialized
    Document y1 = Document.getCenter(_docs, null);  // itc: HERE 20220223
    Document y1_dup = new Document(y1);  // duplicate the center
    y1_dup.addMul(delta, one);
    p_y[0] = 0.5;
    p_y[1] = 0.5;  // duplicate's value
    // put centers in _centers vector
    _centers.addElement(y1);
    _centers.addElement(y1_dup);

    try {
      // 2. Main Loop: Deterministic Annealing Algorithm
      while (T > 0) {
        Vector new_centers = new Vector(); // updated centers go here
        int numiter = 0;
        while (true) {
          numiter++;
          System.err.println("Updating iteration " + numiter);
          // compute ||x-y||^2 for each x in _docs, y in _centers
          double dists[][] = new double[n][k_cur];
          for (int i = 0; i < n; i++) {
            Document x = (Document) _docs.elementAt(i);
            for (int j = 0; j < k_cur; j++) {
              Document y = (Document) _centers.elementAt(j);
              dists[i][j] = _distmetric.dist(x, y);
            }
          }
          // 3. Update
          new_centers.clear();
          for (int i = 0; i < k_cur; i++) {
            for (int j = 0; j < n; j++) {
              // compute p_yx[i][j] by computing its inverse first
              double p_yi_x_inv = 0;
              for (int r=0; r<k_cur; r++) {
                p_yi_x_inv += p_y[r]*Math.exp((-dists[j][r]+dists[j][i])/T);
              }
              p_yx[i][j] = p_y[i]/p_yi_x_inv;
              if (Double.isNaN(p_yx[i][j])) {
                throw new ClustererException("trouble computing p_yx["+i+"]["+j+"]");
              }
              // end new way of updating p_yx[i][j]
            }
            // update p_{y_i}
            p_y[i] = 0;
            for (int j = 0; j < n; j++) p_y[i] += p_yx[i][j];
            p_y[i] /= (double) n;
            // update y_i
            Document y_i = new Document(new TreeMap(), dims);
            for (int j = 0; j < n; j++) {
              Document xj = (Document) _docs.elementAt(j);
              double mul = p_yx[i][j] / (n * p_y[i]);
              y_i.addMul(mul, xj);
            }
            new_centers.add(y_i);
          }
          // 4. check for convergence
          if (converged(_centers, new_centers)) {
            _centers = new_centers;
            break;
          }
          _centers = new_centers;
        } // while (true) keep updating _centers, p_y[], p_yx[][]
        // 5. exit condition: post-processing K-Means then implements limit for T=0
        if (T <= Tmin)break; //
        // 6. cooling step
        T = a * T;
        System.err.println("Temperature cooling, T=" + T); // itc: HERE rm asap
        // 7. check phase-transition
        int klim = k_cur;
        if (_centers.size() != k_cur) {
          throw new ClustererException("wrong num clusters"); // sanity check
        }
        for (int j = 0; j < klim; j += 2) {
          if (k_cur < 2 * k && clusterHasPhaseTransition(j, delta)) {
            System.err.println("Phase Transition for cluster center j=" + j); // itc: HERE rm asap
            Document yj = (Document) _centers.elementAt(j);
            // set the duplicate of yj to be the same again
            Document yj_dup = new Document(yj);
            _centers.set(j + 1, yj_dup);
            Document y_new = new Document(yj);
            y_new.addMul(delta, one);
            System.err.println("Added new center (tot.real centers=" +
                               k_cur / 2 + ") : " + y_new);
            Document y_new2 = new Document(y_new);
            y_new2.addMul(delta, one);
            _centers.addElement(y_new);
            _centers.addElement(y_new2);
            double t = p_y[j];
            p_y[k_cur] = t / 2.0;
            p_y[k_cur + 1] = t / 2.0;
            p_y[j] = t / 2.0;
            p_y[j + 1] = t / 2.0;
            System.err.println("probability of cluster centers " + j + ", " +
                               k_cur + " set to " + t / 2); // itc: HERE rm asap
            k_cur += 2; // increment K
          }
        }
      } // 8. repeat while T>=0
    }
    catch (ClustererException e) {
      // exception was thrown because a temperature was reached that denom~0
      // go to zero temperature and perform standard K-Means as a post-processing
      // step from ClusteringTester2 calling method
      // no-op
    }
    // 9. Post-processing steps
    System.err.println("post-processing: k_cur="+k_cur+" _centers.size()="+_centers.size());  // itc: HERE rm asap
    for (int i=k_cur-1; i>=1; i-=2) {
      _centers.remove(i);
    }
    k_cur = _centers.size();
    System.err.println("final k_cur="+k_cur);  // itc: HERE rm asap
    _clusterIndices = new int[n];
    for (int j=0; j<n; j++) {
      Document dj = (Document) _docs.elementAt(j);
      double mindist = Double.MAX_VALUE;
      int bij = -1;
      for (int i = 0; i < k_cur; i++) {
        Document ci = (Document) _centers.elementAt(i);
        double dij = _distmetric.dist(dj,ci);
        if (dij < mindist) {
          bij = i;
          mindist = dij;
        }
      }
      _clusterIndices[j] = bij;
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

  private boolean converged(Vector centers, Vector new_centers) throws ClustererException {
    for (int i=0; i<centers.size(); i++) {
      Document c1 = (Document) centers.elementAt(i);
      Document c2 = (Document) new_centers.elementAt(i);
      double dist = _distmetric.dist(c1,c2);
      if (dist > _eps) return false;
    }
    return true;
  }

  private boolean clusterHasPhaseTransition(int j, double delta) throws ClustererException {
    Document dj = (Document) _centers.elementAt(j);
    Document djp1 = (Document) _centers.elementAt(j+1);
    final int dims = dj.getDim();
    if (_distmetric.dist(dj, djp1) <= delta*delta*dims+delta)  // add fudge of delta
      return false;
    else return true;
  }
}

