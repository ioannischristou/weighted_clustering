package clustering;

import java.util.*;

public class GMeansMTClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>


  public GMeansMTClusterer() {
    _intermediateClusters = new Vector();
  }


  public Vector getIntermediateClusters() throws ClustererException {
    // store the final clustering in _intermediateClusters
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
      vc.addElement(new Integer(i));
      _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
    }
    // remove any empty vectors
    for (int i=_intermediateClusters.size()-1; i>=0; i--) {
      Vector v = (Vector) _intermediateClusters.elementAt(i);
      if (v==null || v.size()==0) _intermediateClusters.remove(i);
    }
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
   * <"TerminationCriteria",ClustererTermination> the object that will
   * decide when to stop the iterations.
   * <"evaluator",Evaluator> the object that will evaluate a clustering
   * <"metric",DocumentDistIntf> indicates what method will be used, e.g.
   * K-Median, K-Means, K-MeansSqr etc.
   * <"movable",Vector<Integer clusterind> > optional, indicates which clusters
   * are allowed to exchange their documents, if it exists.
   * <"numthreads",Integer nt> optional, how many threads will be used to
   * compute the clustering, default is 1
   * <"projectonempty",Boolean> optional, if true then an exception will be
   * thrown if a cluster becomes empty, else the cluster center will remain
   * the same, without any points attached to it for the next iteration
   * (if any).
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and an initial clustering
   * must be available via a call to
   * setInitialClustering(Vector<Document clusterCenters>)
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws ClustererException {
    boolean project_on_empty = false;
    Boolean p_o_e = (Boolean) _params.get("projectonempty");
    if (p_o_e!=null) project_on_empty = p_o_e.booleanValue();
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    Evaluator evaluator = (Evaluator) _params.get("evaluator");
    if (evaluator==null) evaluator = (Evaluator) _params.get("codocupevaluator");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centers.size();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = (DocumentDistIntf) _params.get("metric");
    int num_threads = 1;
    Integer ntI = (Integer) _params.get("numthreads");
    if (ntI!=null) num_threads = ntI.intValue();
    Vector movable = (Vector) _params.get("movable");
    boolean movable_exists=(movable!=null);
    Double tcI = (Double) _params.get("trycompacting");
    final double try_compacting = (tcI!=null) ? tcI.doubleValue() : -1;  // default false

    double r = Double.MAX_VALUE;
    int ind[];  // to which cluster each doc belongs
    int numi[];  // how many docs each cluster has
    if (_clusterIndices==null) {
      ind = new int[n];  // to which cluster each doc belongs
      numi = new int[k];  // how many docs each cluster has
      for (int i = 0; i < k; i++) numi[i] = 0;
      for (int i = 0; i < n; i++) ind[i] = -1;
      // first assign to each center the closest document to it
      for (int i = 0; i < k; i++) {
        Document ci = (Document) _centers.elementAt(i);
        r = Double.MAX_VALUE;
        int best_j = -1;
        for (int j = 0; j < n; j++) {
          if (ind[j] >= 0)continue; // j already taken
          Document dj = (Document) _docs.elementAt(j);
          double distij = distmetric.dist(ci, dj);
          if (distij < r) {
            r = distij;
            best_j = j;
          }
        }
        numi[i] = 1;
        ind[best_j] = i;
      }
    }
    else {
      // _clusterIndices already exists, and is assumed to have at least one
      // element for each cluster.
      ind = _clusterIndices;
      numi = getClusterCards();
    }

    GMClustererThread threads[] = new GMClustererThread[num_threads];
    for (int i=0; i<num_threads; i++) {
      GMClustererAux ai = new GMClustererAux(this, ind, numi);
      threads[i] = new GMClustererThread(ai);
      threads[i].start();
    }

    final int interval = n/num_threads;

    // incumbent iteration
    int best_indices[] = new int[n];
    Vector best_centers = new Vector();
    double best_val = Double.MAX_VALUE;
    boolean stop = ct.isDone();
    while (!stop) {
      // all threads except last have floor((n-1)/numthreads) work to do
      int starti = 0; int endi = interval;
      for (int t=0; t<num_threads-1; t++) {
        GMClustererAux rt = threads[t].getGMClustererAux();
        rt.runFromTo(starti, endi);
        starti = endi+1;
        endi += interval;
      }
      // last thread
      GMClustererAux last_aux = threads[num_threads-1].getGMClustererAux();
      last_aux.runFromTo(starti, n-1);
      // wait for threads to finish their current task
      for (int t=0; t<num_threads; t++) {
        GMClustererAux rt = threads[t].getGMClustererAux();
        rt.waitForTask();
      }
      // ind[] is already computed correctly as each thread had a reference to it
      // numi[] is not even needed

      Vector centers_new = new Vector();
      for (int i=0; i<k; i++) centers_new.addElement(null);
      // implement hard K-Means
      for (int i=0; i<k; i++) {
        Vector v = new Vector();
        for (int j=0; j<n; j++)
          if (ind[j]==i) v.addElement(_docs.elementAt(j));
        if (v.size()>0 || project_on_empty)
          centers_new.set(i, Document.getCenter(v, null));  // itc: HERE 20220223
        else centers_new.set(i, new Document((Document) _centers.elementAt(i)));
        // make sure center remains even w/o any points assoc with it
      }
      _centers = centers_new;
      _clusterIndices = ind;
      // incumbent computation
      double new_val = eval(evaluator);
      // System.err.println("v="+new_val);  // itc: HERE rm asap
      if (new_val<best_val) {
        best_val = new_val;
        best_centers.clear();
        for (int i=0; i<n; i++) best_indices[i] = _clusterIndices[i];
        for (int i=0; i<k; i++) best_centers.addElement(_centers.elementAt(i));
      }
      stop = ct.isDone();  // check if we're done
      // break up clusters that are not "compact enough"
      if (stop && try_compacting>=0) {
        System.err.println("trying compacting clusters");
        compactClusters(try_compacting);
        stop = ct.isDone();  // see if indeed we're done
      }
    }
    // stop threads
    for (int t=0; t<num_threads; t++) {
      threads[t].getGMClustererAux().setFinish();
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
    // set incumbent
    for (int i=0; i<n; i++) _clusterIndices[i] = best_indices[i];
    _centers = best_centers;
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
    int[] cards = new int[k];
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
   * break up any clusters that do not appear compact
   * the heuristic is the following:
   * for any cluster that contains points that are more than 2.1*ave_dist away
   * from the center, remove all points that are more than ave_dist away from
   * their center, recompute the center, and assign the removed points to their
   * nearest center
   */
  private void compactClusters(double trycompacting) throws ClustererException {
    final int k = _centers.size();
    final int n = _docs.size();
    // 1. compute average distance from center for each cluster
    double ave_dist[] = new double[k];
    for (int i=0; i<k; i++) ave_dist[i] = 0.0;
    for (int i=0; i<n; i++) {
      int c = _clusterIndices[i];
      Document di = (Document) _docs.elementAt(i);
      Document cl = (Document) _centers.elementAt(c);
      ave_dist[c] += Document.d(di, cl);
    }
    int cards[] = getClusterCards();
    for (int i=0; i<k; i++) {
      ave_dist[i] /= cards[i];
    }
    // 2. find every document that is further than 2.1*ave_dist away
    // and unassign it
    int changed[] = new int[k];
    for (int i=0; i<k; i++) changed[i] = 0;  // init
    for (int i=0; i<n; i++) {
      int c = _clusterIndices[i];
      Document di = (Document) _docs.elementAt(i);
      Document cl = (Document) _centers.elementAt(c);
      double dist = Document.d(di, cl);
      if (dist>trycompacting*ave_dist[c]) {
        changed[c]++;
        _clusterIndices[i] = -1;
      }
    }
    // 3. recompute the centers
    for (int i=0; i<k; i++) {
      if (changed[i]>0) {
        Document ci = (Document) _centers.elementAt(i);
        ci = new Document(new TreeMap(), ci.getDim());  // reset ci
        _centers.set(i, ci);
      }
    }
    for (int i=0; i<n; i++) {
      Document di = (Document) _docs.elementAt(i);
      int c = _clusterIndices[i];
      if (c>=0 && changed[c]>0) {
        Document cl = (Document) _centers.elementAt(c);
        cl.addMul(1.0, di);
        _centers.set(c, cl);
      }
    }
    for (int i=0; i<k; i++) {
      if (changed[i]>0) {
        Document ci = (Document) _centers.elementAt(i);
        ci.div((cards[i]-changed[i]));
        _centers.set(i, ci);
      }
    }
    // 4. reassign the unassigned points
    for (int i=0; i<n; i++) {
      if (_clusterIndices[i]==-1) {  // it is unassigned
        Document di = (Document) _docs.elementAt(i);
        double best = Double.MAX_VALUE;
        int best_ind = -1;
        for (int j=0; j<k; j++) {
          Document cj = (Document) _centers.elementAt(j);
          double dist = Document.d(di, cj);
          if (dist<best) {
            best = dist;
            best_ind = j;
          }
        }
        _clusterIndices[i] = best_ind;
      }
    }
    // 5. finally compute the new centers
    _centers = Document.getCenters(_docs, _clusterIndices, k, null);  // itc: HERE 20220223
  }
}


class GMClustererAux {
  private GMeansMTClusterer _master;
  private int _ind[];
  private int _numi[];
  private int _starti=-1;
  private int _endi=-1;  // the indices to work on [_starti, _endi]
  private boolean _finish = false;


  public GMClustererAux(GMeansMTClusterer master, int[] ind, int[] numi) {
    _master = master;
    _ind = ind;
    _numi = new int[numi.length];
    for (int i=0; i<numi.length; i++) _numi[i] = numi[i];  // own private copy
  }


  public void go() {
    while (getFinish()==false) {
      go1();
    }
  }


  public synchronized boolean getFinish() {
    return _finish;
  }


  public synchronized void setFinish() {
    _finish = true;
    notify();
  }


  public synchronized void waitForTask() {
    while (_starti!=-1 || _endi!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
  }


  public synchronized void runFromTo(int starti, int endi) {
    while (_starti!=-1 || _endi!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // OK, now set values
    _starti = starti; _endi = endi;
    notify();  // itc: HERE do I need notifyAll(); ?
  }

  private synchronized void go1() {
    while (_starti==-1 || _endi==-1) {
      if (_finish) return;  // finish
      try {
        wait();  // wait for order
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // run the code
    Vector centers = _master.getCurrentCenters();
    int k = centers.size();
    DocumentDistIntf distmetric = (DocumentDistIntf) _master.getParams().get("metric");
    double r;
    Vector docs = _master.getCurrentDocs();
    Vector movable = (Vector) _master.getParams().get("movable");
    boolean movable_exists=(movable!=null);
    for (int i=_starti; i<=_endi; i++) {
      r = Double.MAX_VALUE;
      Document di = (Document) docs.elementAt(i);
      if (_ind[i]>=0 && _numi[_ind[i]]==1) {
        continue;  // don't move the Document as it's the only one
      }
      if (movable_exists && _master._clusterIndices!=null &&
          movable.contains(new Integer(_master._clusterIndices[i]))) {
        _ind[i]=_master._clusterIndices[i];
        continue;  // Document i is not allowed to move
      }
      for (int l=0; l<k; l++) {
        if (movable_exists && _ind[i]!=l && movable.contains(new Integer(l))==false)
          continue;  // cannot move to partition l
        Document cl = (Document) centers.elementAt(l);
        try {
          double rl = distmetric.dist(di, cl);
          if (rl < r) {
            r = rl;
            if (_ind[i] >= 0 && _numi[_ind[i]] > 0) --_numi[_ind[i]];
            _numi[l]++;
            _ind[i] = l;
          }
        }
        catch (ClustererException e) {
          e.printStackTrace();
        }
      }
    }
    // finished, reset indices
    _starti=-1; _endi=-1;
    notify();
  }
}


class GMClustererThread extends Thread {
  private GMClustererAux _r=null;

  public GMClustererThread(GMClustererAux r) {
    _r = r;
  }

  public void run() {
    _r.go();
  }

  public GMClustererAux getGMClustererAux() {
    return _r;
  }
}

