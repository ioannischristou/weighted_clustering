package clustering;

import java.util.*;

public class KMeansSqrClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>


  public KMeansSqrClusterer() {
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
   * <"h",double> the rate of learning in soft K-Means Clustering
   * <"TerminationCriteria",ClustererTermination> the object that will
   * decide when to stop the iterations.
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
    final double h = ( (Double) _params.get("h")).doubleValue();
    final double[] wgts = (double[]) _params.get("weights");
    boolean project_on_empty = false;
    Boolean p_o_e = (Boolean) _params.get("projectonempty");
    if (p_o_e!=null) project_on_empty = p_o_e.booleanValue();
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centers.size();
    // final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = new DocumentDistL2Sqr();
    double r = Double.MAX_VALUE;
    int ind[] = new int[n];  // to which cluster each doc belongs
    int numi[] = new int[k];  // how many docs each cluster has
    while (!ct.isDone()) {
      for (int i=0; i<k; i++) numi[i]=0;
      for (int i=0; i<n; i++) {
        r = Double.MAX_VALUE;
        ind[i]=-1;
        Document di = (Document) _docs.elementAt(i);
        for (int l=0; l<k; l++) {
          Document cl = (Document) _centers.elementAt(l);
          double rl = distmetric.dist(di, cl);
          if (rl<r) {
            r = rl;
            if (ind[i]>=0 && numi[ind[i]]>0) --numi[ind[i]];
            numi[l]++;
            ind[i] = l;
          }
        }
      }
      Vector centers_new = new Vector();
      for (int i=0; i<k; i++) centers_new.addElement(null);
      // implement hard K-Means
      for (int i=0; i<k; i++) {
        Vector v = new Vector();
        ArrayList wgtsi = new ArrayList();
        for (int j=0; j<n; j++) {
          if (ind[j]==i) {
            v.addElement(_docs.elementAt(j));
            if (wgts!=null) wgtsi.add(new Double(wgts[j]));  
          }
        }
        if (v.size()>0 || project_on_empty) {
          double[] wsi = wgtsi.size() > 0 ? new double[wgtsi.size()] : null;
          for (int j=0; j<wgtsi.size(); j++) 
              wsi[j] = ((Double)wgtsi.get(j)).doubleValue();
          centers_new.set(i, Document.getCenter(v, wsi));
        }
        else centers_new.set(i, _centers.elementAt(i));
        // make sure center remains even w/o any points assoc with it
      }
      // in comment is the soft K-Means implementation
      /*
      for (int i=0; i<n; i++) {
        Document v = (Document) centers_new.elementAt(ind[i]);
        double c = h / numi[ind[i]];
        v.addMul(c, (Document) _docs.elementAt(i));
        v.addMul(-c, (Document) _centers.elementAt(ind[i]));
      }
      */
      _centers = centers_new;
      // set cluster indices
      _clusterIndices = ind;
      // store this clustering in _intermediateClusters
      int prev_ic_sz = _intermediateClusters.size();
      for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
      for (int i=0; i<_clusterIndices.length; i++) {
        int c = _clusterIndices[i];
        Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
        vc.addElement(new Integer(i));
        _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
      }

    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
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
}

