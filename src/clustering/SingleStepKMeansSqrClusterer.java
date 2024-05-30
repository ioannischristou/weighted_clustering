package clustering;

import java.util.*;

public class SingleStepKMeansSqrClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  int[] _clusterIndices;

  public SingleStepKMeansSqrClusterer() {
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
    ClustererTermination ct = (ClustererTermination)
        _params.get("TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centers.size();
    final int dims = ((Document) _docs.elementAt(0)).getDim();
    final DocumentDistIntf distmetric = new DocumentDistL2Sqr();
    double r = Double.MAX_VALUE;
    int ind[] = _clusterIndices;  // an alias for which cluster each doc belongs
    int numi[] = new int[k];  // how many docs each cluster has

    // compute cardinalities of each cluster
    double dists[][] = new double[n][k];  // distance of di from cluster cl
    for (int i=0; i<k; i++) numi[i]=0;
    for (int i=0; i<n; i++) {
      r = Double.MAX_VALUE;
      numi[_clusterIndices[i]]++;  // compute cluster cardinalities
      Document di = (Document) _docs.elementAt(i);
      for (int l=0; l<k; l++) {
        Document cl = (Document) _centers.elementAt(l);
        double rl = distmetric.dist(di, cl);
        dists[i][l] = rl;
      }
    }

    while (!ct.isDone()) {
      // choose the single point that has maximum gain by moving from current
      // partition to new partition
      System.err.println("SingleStepKMeansSqrClusterer: start new iteration");
      double best_gain = Double.POSITIVE_INFINITY;
      int best_ind = -1;
      int target_ind = -1;
      Document cp_prime = null;
      Document cl_prime = null;
      for (int i=0; i<n; i++) {
        Document di = (Document) _docs.elementAt(i);
        int p = ind[i];
        if (numi[p]==1) continue;  // cannot move because it will empty cluster
        Document cp = (Document) _centers.elementAt(p);
        // moving di from p, will result in a new cp
        Document cp_new = new Document(new TreeMap(), dims);
        cp_new.addMul(numi[p]/((double) numi[p]-1), cp);
        cp_new.addMul(-1.0/((double) numi[p]-1), di);
        for (int l=0; l<k; l++) {
          if (l==p) continue;  // don't count distance from current center
          Document cl = (Document) _centers.elementAt(l);
          double sum = -dists[i][p];
          // moving di to cl will result in a cl_new
          Document cl_new = new Document(new TreeMap(), dims);
          cl_new.addMul(numi[l]/((double) numi[l]+1), cl);
          cl_new.addMul(1.0/((double) numi[l]+1), di);
          sum += distmetric.dist(di,cl_new);
          // what is the gain if it moves from p to l ?
          for (int j=0; j<n; j++) {
            Document dj = (Document) _docs.elementAt(j);
            if (ind[j]==p && j!=i) sum += (distmetric.dist(dj,cp_new)-dists[j][p]);
            if (ind[j]==l && j!=i) sum += (distmetric.dist(dj,cl_new)-dists[j][l]);
          }
          if (sum<best_gain) {
            best_gain = sum;
            best_ind = i;
            target_ind = l;
            cp_prime = cp_new;
            cl_prime = cl_new;
          }
        }
      }
      if (best_gain<0.0) {
        // do the change
        // compute new cp and cl, update numi[] and ind[]
        System.err.println("moving Doc-"+best_ind+" from center-"+ind[best_ind]+" to "+target_ind+" w/ gain="+best_gain);  // itc: HERE rm asap
        _centers.set(ind[best_ind], cp_prime);
        _centers.set(target_ind, cl_prime);
        numi[ind[best_ind]]--;
        numi[target_ind]++;
        // update the dists[][] array
        for (int i=0; i<n; i++) {
          if (ind[i]==ind[best_ind] || ind[i]==target_ind) {
            Document di = (Document) _docs.elementAt(i);
            for (int l = 0; l < k; l++) {
              Document cl = (Document) _centers.elementAt(l);
              dists[i][l] = distmetric.dist(di,cl);
            }
          }
        }
        ind[best_ind] = target_ind;
      }
      // set cluster indices
      _clusterIndices = ind;
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
}

