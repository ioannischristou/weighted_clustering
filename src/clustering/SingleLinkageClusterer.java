package clustering;

import java.util.*;

public class SingleLinkageClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;

  public SingleLinkageClusterer() {
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
   * setInitialClustering(Vector<Document clusterCenters>)
   * @throws Exception if at some iteration, one or more clusters becomes empty.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception {
    final int n = _docs.size();
    int k3 = ((Integer) _params.get("k")).intValue();
    try {
      k3 = ((Integer) _params.get("k_try")).intValue();
    }
    catch (Exception e) {
      // no-op
    }
    final int k = k3;
    // final int k = _centers.size();
    DocumentDistIntf metric = (DocumentDistIntf) _params.get("metric");
    if (metric==null) metric = new DocumentDistL1();
    // Document.setMetric(metric);  // not needed
    // create sorting of distances
    DocDist arr[] = new DocDist[n*(n-1)/2];
    int jj=0;
    for (int i=0; i<n; i++) {
      Document di = (Document) _docs.elementAt(i);
      for (int j=i+1; j<n; j++) {
        Document dj = (Document) _docs.elementAt(j);
        double dij = metric.dist(di, dj);
        arr[jj++] = new DocDist(i,j,dij);
      }
    }
    Arrays.sort(arr);
    // arr is an array of DocDist objs sorted in asc. order of distance between
    // the two Documents i & j, i<j

    int num_clusters=-1;
    // 1. each cluster is in a cluster of its own.
    if (_clusterIndices==null) {
      _clusterIndices = new int[n];
      for (int i = 0; i < n; i++) {
        _clusterIndices[i] = i;
      }
      num_clusters = n;
    }
    else {
      // only figure out num_clusters
      HashSet nums = new HashSet();
      for (int i=0; i<n; i++) {
        nums.add(new Integer(_clusterIndices[i]));
      }
      num_clusters = nums.size();
    }
    // 2. continuously link closest clusters
    Set vanished = new TreeSet();  // Set<Integer m>
    while (num_clusters>k) {
      int vanishedcluster = merge2ClosestClusters(arr);
      num_clusters--;
      vanished.add(new Integer(vanishedcluster));
    }
    // 3. renumber cluster assignments
    TreeMap renumbering = new TreeMap();
    int mm=k;
    for (int i=0; i<k; i++) {
      Integer ii = new Integer(i);
      if (vanished.contains(ii)) {
        while (vanished.contains(new Integer(mm))) mm++;
        renumbering.put(new Integer(mm), ii);
        mm++;  // mm by default wasn't vanished, so increment candidate
      }
    }
    for (int i=0; i<n; i++) {
      int m = _clusterIndices[i];
      if (m>=k)
        _clusterIndices[i] = ((Integer) renumbering.get(new Integer(m))).intValue();
    }

    // compute _centers, slow version
    _centers = new Vector();
    Vector aux[] = new Vector[k];
    for (int i=0; i<k; i++) aux[i] = new Vector();
    for (int i=0; i<n; i++) {
      int k1 = _clusterIndices[i];
      Document di = (Document) _docs.elementAt(i);
      aux[k1].addElement(di);
    }
    for (int i=0; i<k; i++) {
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


  private int merge2ClosestClusters(DocDist arr[]) {
    for (int i=0; i<arr.length; i++) {
      DocDist ddi = arr[i];
      int ddii = _clusterIndices[ddi._i];
      int ddij = _clusterIndices[ddi._j];
      if (ddii!=ddij) {
        // merge _j with _i
        for (int j=0; j<_docs.size(); j++) {
          if (_clusterIndices[j]==ddij) _clusterIndices[j] = ddii;
        }
        return ddij;  // return the cluster index that vanishes
      }
    }
    return -1;  // should not reach this point
  }
}
