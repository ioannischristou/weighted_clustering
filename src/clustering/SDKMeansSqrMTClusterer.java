package clustering;

import java.util.*;

public class SDKMeansSqrMTClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  int[] _numi;  // cards array
  double[][] _dists;
  Vector _intermediateClusters;
  // next three data members are used to figure out in each iteration who's best
  double _bestGain = Double.MAX_VALUE;
  int _bestInd=-1;
  int _bestPart=-1;
  Document _cpPrime = null;
  Document _clPrime = null;

  public SDKMeansSqrMTClusterer() {
    _intermediateClusters = new Vector();
  }


  public Hashtable getParams() {
    return _params;
  }


  public Vector getIntermediateClusters() throws ClustererException {
    return _intermediateClusters;
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


  public Vector clusterDocs() throws ClustererException {
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centers.size();
    final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = new DocumentDistL2Sqr();
    double r = Double.MAX_VALUE;
    int num_threads = 1;
    Integer ntI = (Integer) _params.get("numthreads");
    if (ntI!=null) num_threads = ntI.intValue();
    boolean project_on_empty = false;
    Boolean p_o_e = (Boolean) _params.get("projectonempty");
    if (p_o_e!=null) project_on_empty = p_o_e.booleanValue();
    Evaluator evaluator = (Evaluator) _params.get("evaluator");
    if (evaluator==null) evaluator = (Evaluator) _params.get("codocupevaluator");

    int ind[] = _clusterIndices;  // an alias for which cluster each doc belongs
    _numi = new int[k];  // how many docs each cluster has

    // compute cardinalities of each cluster
    _dists = new double[n][k];  // distance of di from cluster cl
    for (int i=0; i<k; i++) _numi[i]=0;
    for (int i=0; i<n; i++) {
      r = Double.MAX_VALUE;
      _numi[_clusterIndices[i]]++;  // compute cluster cardinalities
      Document di = (Document) _docs.elementAt(i);
      for (int l=0; l<k; l++) {
        Document cl = (Document) _centers.elementAt(l);
        double rl = distmetric.dist(di, cl);
        _dists[i][l] = rl;
      }
    }

    ClustererThread threads[] = new ClustererThread[num_threads];
    for (int i=0; i<num_threads; i++) {
      ClustererAux ai = new SDKMeansClustererAux(this, ind, _numi);
      threads[i] = new ClustererThread(ai);
      threads[i].start();
    }

    final int interval = n/num_threads;

    // incumbent iteration
    int best_indices[] = new int[n];
    Vector best_centers = new Vector();
    double best_val = Double.MAX_VALUE;
    boolean stop = false;
    while (!stop) {
      // all threads except last have floor((n-1)/numthreads) work to do
      int starti = 0; int endi = interval;
      for (int t=0; t<num_threads-1; t++) {
        ClustererAux rt = threads[t].getClustererAux();
        rt.runFromTo(starti, endi);
        starti = endi+1;
        endi += interval;
      }
      // last thread
      ClustererAux last_aux = threads[num_threads-1].getClustererAux();
      last_aux.runFromTo(starti, n-1);
      // wait for threads to finish their current task
      for (int t=0; t<num_threads; t++) {
        ClustererAux rt = threads[t].getClustererAux();
        rt.waitForTask();
      }
      // ind[] is already computed correctly as each thread had a reference to it
      // numi[] is not even needed
      if (_bestGain<0.0) {
        // do the change
        // compute new cp and cl, update numi[] and ind[]
        System.err.println("moving Doc-"+_bestInd+" from center-"+ind[_bestInd]+" to "+_bestPart+" w/ gain="+_bestGain);  // itc: HERE rm asap
        _centers.set(ind[_bestInd], _cpPrime);
        _centers.set(_bestPart, _clPrime);
        _numi[ind[_bestInd]]--;
        _numi[_bestPart]++;
        // update the dists[][] array
        for (int i=0; i<n; i++) {
          if (ind[i]==ind[_bestInd] || ind[i]==_bestPart) {
            Document di = (Document) _docs.elementAt(i);
            for (int l = 0; l < k; l++) {
              Document cl = (Document) _centers.elementAt(l);
              _dists[i][l] = distmetric.dist(di,cl);
            }
          }
        }
        ind[_bestInd] = _bestPart;
      }
      _bestGain = Double.MAX_VALUE;  // reset _bestGain for next iteration

      // set cluster indices
      _clusterIndices = ind;

      stop = ct.isDone();  // check if we're done
    }

    // stop threads
    for (int t=0; t<num_threads; t++) {
      threads[t].getClustererAux().setFinish();
    }

    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }

    // store this clustering in _intermediateClusters
    _intermediateClusters.clear();
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
      vc.addElement(new Integer(i));
      _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
    }

    return _centers;

  }


  synchronized void updateBest(double gain, int ind, int part, Document cp_new, Document cl_new) {
    if (_bestGain > gain) {
      _bestGain = gain;
      _bestInd = ind;
      _bestPart = part;
      _cpPrime = cp_new;
      _clPrime = cl_new;
    }
  }
}
