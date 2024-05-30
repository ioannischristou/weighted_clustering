package clustering;

import java.util.*;

public class MergeClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  private int[] _clusterIndices;
  private int[] _centerIndices;

  public MergeClusterer() {
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
    // final int k = _centers.size();
    DocumentDistIntf metric = (DocumentDistIntf) _params.get("metric");
    if (metric==null) metric = new DocumentDistL1();
    // Document.setMetric(metric);  // not needed
    // create sorting of distances
    DocDist arr[] = new DocDist[num_clusters*(num_clusters-1)/2];
    int jj=0;
    for (int i=0; i<num_clusters; i++) {
      Document di = (Document) _centers.elementAt(i);
      for (int j=i+1; j<num_clusters; j++) {
        Document dj = (Document) _centers.elementAt(j);
        double dij = metric.dist(di, dj);
        arr[jj++] = new DocDist(i,j,dij);
      }
    }
    Arrays.sort(arr);
    // arr is an array of DocDist objs sorted in asc. order of distance between
    // the two centers i & j, i<j
    for (int i=0; i<arr.length; i++) {
      System.err.println("DocDist["+arr[i]._i+","+arr[i]._j+", "+arr[i]._dist+"]");
    }

    // 1. initialize _centerIndices
    int numclusters = num_clusters;
    _centerIndices = new int[num_clusters];
    for (int i=0; i<num_clusters; i++) _centerIndices[i] = i;
    // 2. continuously link closest clusters
    Set vanished = new TreeSet();  // Set<Integer m>
    while (numclusters>k) {
      int vanishedcluster = merge2ClosestClusters(arr);
      numclusters--;
      vanished.add(new Integer(vanishedcluster));
    }
/*
    // itc: HERE rm asap
    System.err.println("MergeClusterer intermediate asgns:");
    for (int r=0; r<n; r++) {
      System.err.print(" c["+r+"]="+_clusterIndices[r]);
    }
    System.err.println("");
*/
    System.err.println("_centerIndices: ");
    for (int r=0; r<num_clusters; r++) {
      System.err.print(" c["+r+"]="+_centerIndices[r]);
    }
    System.err.println("");
    // itc: HERE rm up to here

    // 3. renumber cluster assignments
    TreeMap renumbering = new TreeMap();
    int mm=k;
    for (int i=0; i<k; i++) {
      Integer ii = new Integer(i);
      if (vanished.contains(ii)) {
        while (vanished.contains(new Integer(mm))) mm++;
        renumbering.put(new Integer(mm), ii);
        mm++;  // go to next candidate above k
      }
    }
    for (int i=0; i<n; i++) {
      int m = _clusterIndices[i];
      if (m>=k)
        _clusterIndices[i] = ((Integer) renumbering.get(new Integer(m))).intValue();
    }

    // compute _centers, slow version
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


  private int merge2ClosestClusters(DocDist arr[]) {
    for (int i=0; i<arr.length; i++) {
      DocDist ddi = arr[i];
      int ddii = _centerIndices[ddi._i];
      int ddij = _centerIndices[ddi._j];
      if (ddii!=ddij) {
        // put in order
        if (ddii>ddij) {
          int tmp = ddii;
          ddii = ddij;
          ddij = tmp;
        }
        // merge _j with _i
        for (int j=0; j<_docs.size(); j++) {
          if (_clusterIndices[j]==ddij) _clusterIndices[j] = ddii;
        }
        for (int j=0; j<_centerIndices.length; j++) {
          if (_centerIndices[j]==ddij) _centerIndices[j] = ddii;
        }
        return ddij;  // return the cluster index that vanishes
      }
    }
    return -1;  // should not reach this point
  }
}
