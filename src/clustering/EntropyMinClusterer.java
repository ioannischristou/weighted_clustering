package clustering;

import java.util.*;

public class EntropyMinClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  Hashtable _params;
  int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>

  public EntropyMinClusterer() {
    _intermediateClusters = new Vector();
    _clusterIndices = null;
  }


  public Vector getIntermediateClusters() throws ClustererException {
    return _intermediateClusters;
  }


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class.
   *
   * Before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d), and the params must be
   * set via a call to setParams(p).
   * @throws Exception
   * @return Vector centers of clusters
   */
  public Vector clusterDocs() throws Exception {

    final int n = _docs.size();
    final int k = ((Integer) _params.get("num_clusters")).intValue();

    boolean do_first_descent = false;
    Boolean dfdB=(Boolean) _params.get("ent_first_desc");
    if (dfdB!=null) do_first_descent = dfdB.booleanValue();
    double max_acc_desc = -1.e-15;
    Double madD=(Double) _params.get("ent_max_acc_desc");
    if (madD!=null) max_acc_desc = madD.doubleValue();

    if (_clusterIndices==null) {
      // set random cluster indices first
      int numi[] = new int[k];
      _clusterIndices = new int[n];
      for (int i=0; i<n; i++) {
        _clusterIndices[i] = utils.RndUtil.getInstance().getRandom().nextInt(k);
        numi[_clusterIndices[i]]++;
      }
      // ensure no cluster is empty (at least when n >= k)
      for (int i=0; i<k;i++) {
        if (numi[i]==0) {
          int j;
          for (j=0; j<n; j++) {
            if (numi[_clusterIndices[j]]>1) break;
          }
          numi[_clusterIndices[j]]--;
          _clusterIndices[j]=i;
          numi[i]=1;
        }
      }
      // run K-Median to speed up convergence
      _centers = Document.getCenters(_docs, _clusterIndices, k, null);  // itc: HERE 20220223
      ClustererTermination ct = new ClustererTerminationNoImpr();
      ct.registerClustering(this);
      DocumentDist distmetric = new DocumentDistL1();
      int[] ind = new int[n];
      for (int i=0; i<n; i++) ind[i]=_clusterIndices[i];
      double r=0;
      while (!ct.isDone()) {
        // 1. figure out cluster assignments given the centers
        // for (int i=0; i<k; i++) numi[i]=0;
        for (int i=0; i<n; i++) {
          r = Double.MAX_VALUE;
          if (numi[ind[i]]==1) continue;  // don't re-assign unique point in cluster
          // ind[i]=-1;
          Document di = (Document) _docs.elementAt(i);
          for (int l=0; l<k; l++) {
            Document cl = (Document) _centers.elementAt(l);
            double rl = distmetric.dist(di, cl);
            if (rl<r) {
              r = rl;
              // if (ind[i]>=0 && numi[ind[i]]>0) --numi[ind[i]];  // condition is always satisfied
              --numi[ind[i]];
              numi[l]++;
              ind[i] = l;
            }
          }
        }
        Vector centers_new = new Vector();
        for (int i=0; i<k; i++) {
          // centers_new.addElement(new Document(new TreeMap(), dims));
          centers_new.addElement(null);
        }
        // implement hard K-Median
        for (int i=0; i<k; i++) {
          Vector v = new Vector();
          for (int j=0; j<n; j++)
            if (ind[j]==i) v.addElement(_docs.elementAt(j));
          if (v.size()==0) {
            // make sure center i remains even w/o any points associated
            // System.err.println("KMedianClusterer.clusterDocs(): "+i+"-th cluster empty...");
            centers_new.set(i, _centers.elementAt(i));
          }
          else centers_new.set(i, Document.getCenter(v, null));  // itc: HERE 20220223
        }
        _centers = centers_new;
        _clusterIndices = ind;
      }
    }  // if _clusterIndices==null

    System.err.println("EntropyMinClusterer Main Loop Starts...");  // itc: HERE rm asap
    Document.setMetric((DocumentDistIntf) _params.get("metric"));
    EntropyEvaluator eeval = new EntropyEvaluator();
    eeval.setMasterDocSet(_docs);
    eeval.setParams(_params);

    // double cur_val = eeval.eval(this);

    // entropy minimization steepest descent algorithm or first descent algorithm
    // slow method
    double cluster_entropies[] = new double[k];
    Set indices[] = new HashSet[k];
    // indices[j] is a HashSet<Integer docid> containing
    // all document ids belonging to cluster j
    for (int i=0; i<k; i++) {
      indices[i] = new HashSet();
    }
    for (int j=0; j<n; j++)
      indices[_clusterIndices[j]].add(new Integer(j));
    for (int i=0; i<k; i++) {
      cluster_entropies[i] = eeval.evalCluster(indices[i]);
    }
    while (true) {
      double best_move_val=Double.MAX_VALUE;
      int best_move_ind = -1;
      int best_move_to = -1;
      double best_cl_ent_i = -1;
      double best_cl_ent_j = -1;
      for (int i=0; i<n; i++) {  // figure out move that reduces entropy the most
        int cur_cl_i = _clusterIndices[i];
        if (indices[cur_cl_i].size()<=1) continue;  // no empty clusters allowed
        for (int j=0; j<k; j++) {
          if (j==cur_cl_i) continue;
          // suppose the move happens
          Integer ii = new Integer(i);
          indices[j].add(ii);
          double ent_j_new = eeval.evalCluster(indices[j]);
          indices[cur_cl_i].remove(ii);
          double ent_cur_cl_i_new = eeval.evalCluster(indices[cur_cl_i]);
          double ent_diff = ent_j_new + ent_cur_cl_i_new -
              cluster_entropies[j] - cluster_entropies[cur_cl_i];
          if (ent_diff < best_move_val) {
            best_move_val = ent_diff;
            best_move_ind = i;
            best_move_to = j;
            best_cl_ent_i = ent_cur_cl_i_new;
            best_cl_ent_j = ent_j_new;
          }
          // restore order
          indices[j].remove(ii);
          indices[cur_cl_i].add(ii);
          if (do_first_descent && best_move_val < max_acc_desc) {
            break;  // move is ok, accept immediately
          }
        }
        if (do_first_descent && best_move_val < max_acc_desc) {
          break;  // move is ok, accept immediately
        }
      }
      if (best_move_val>=max_acc_desc) break;  // reached local minimum
      // do the change
      System.err.println("moving doc-"+best_move_ind+
                         " from "+_clusterIndices[best_move_ind]+
                         " to "+best_move_to+" w/ impr="+best_move_val);  // itc: HERE rm asap
      Integer bi = new Integer(best_move_ind);
      indices[_clusterIndices[best_move_ind]].remove(bi);
      indices[best_move_to].add(bi);
      cluster_entropies[best_move_to] = best_cl_ent_j;
      cluster_entropies[_clusterIndices[best_move_ind]] = best_cl_ent_i;
      _clusterIndices[best_move_ind] = best_move_to;
    }

    // compute centers
    Vector centers_new = new Vector();
    for (int i=0; i<k; i++) centers_new.addElement(null);
    for (int i=0; i<k; i++) {
      Vector v = new Vector();
      for (int j=0; j<n; j++)
        if (_clusterIndices[j]==i) v.addElement(_docs.elementAt(j));
      if (v.size()>0) centers_new.set(i, Document.getCenter(v, null));  // itc: HERE 20220223
      else centers_new.set(i, new Document(new TreeMap(), ((Document)_docs.elementAt(0)).getDim()));  // empty center
    }
    _centers = centers_new;
    // store this clustering in _intermediateClusters
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz + c);
      vc.addElement(new Integer(i));
      _intermediateClusters.set(prev_ic_sz + c, vc); // ensure addition
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

