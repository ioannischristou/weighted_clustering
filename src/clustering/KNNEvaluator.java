package clustering;

import java.util.*;
import java.util.Arrays;
import coarsening.*;

public class KNNEvaluator extends EnhancedEvaluator {
  private int _knn = 3;  // how many nearest neighbors to consider
  private DocDist2[] _sortedDocs=null;

  public KNNEvaluator() {
  }


  public double bestOf(double v1, double v2) {
    return Math.min(v1, v2);
  }


  public double getWorstValue() {
    return Double.MAX_VALUE;
  }


  public void setParams(Hashtable params) {
    if (params!=null) {
      Integer knnI = (Integer) params.get("knn");
      if (knnI!=null) _knn = knnI.intValue();
    }
  }


  /**
   *
   * @param cl Clusterer
   * @throws ClustererException
   * @return double
   */
  public double eval(Clusterer cl) throws ClustererException {
    final int[] inds = cl.getClusteringIndices();
    return eval(cl.getCurrentDocs(), inds);
  }


  /**
   *
   * @param docs Vector
   * @param inds int[]
   * @throws ClustererException
   * @return double
   */
  public double eval(Vector docs, int[] inds) throws ClustererException {
    double res = 0.0;
    // figure out k
    int k=0;
    for (int i=0; i<inds.length; i++)
      if (inds[i]>k) k = inds[i];
    k++;
    Vector clusters[] = new Vector[k];
    for (int i=0; i<k; i++) clusters[i] = new Vector();
    for (int i=0; i<docs.size(); i++) {
      clusters[inds[i]].addElement(docs.elementAt(i));
    }
    for (int i=0; i<k; i++)
      res += evalCluster(clusters[i]);
    return res;
  }


  /**
   * Figure out the k-nearest neighbors of each point in docs. For each neighbor
   * that is not part of docs, add to the resulting value its distance from the
   * point. The effect is magnified by the number of connected components of the
   * docs cluster.
   * Obviously, smaller is better
   * @param docs Vector
   * @throws ClustererException
   * @return double
   */
  public double evalCluster(Vector docs) throws ClustererException {
    double res = 0.0;

    synchronized (this) {
      if (_sortedDocs == null) {
        sortDocs(); // sort the master docs set.
      }
    }

    double graph_cost=1;

    try {
      res = 1;
      graph_cost = getConnectednessCost(docs);
    }
    catch (GraphException e) {
      e.printStackTrace();
      throw new ClustererException("evalCluster(): graph creation failed...");
    }

    for (int i=0; i<docs.size(); i++) {
      Document di = (Document) docs.elementAt(i);
      res += getKNNborsCost2(di, docs);
    }

    double exponent = Math.sqrt(graph_cost);
    res = Math.pow(res, exponent);  // for connected graph, this is 1, for
                                    // a graph with two components, raise to 1.414
                                    // and so on... strongly encourage connected clusters
                                    // via k-NearestNeighbors

    return res;
  }


  /**
   * figure out the _knn nearest neighbors of d, and for each one that is not
   * in docs, add its distance from d to the total return value
   * @param d Document
   * @param docs Vector
   * @return double
   */
  private double getKNNborsCost(Document d, Vector docs) {
    double res = 0.0;
    // get d id
    final Vector _docs = getMasterDocSet();
    final int n = _docs.size();
    int id=-1;
    for (int i=0; i<n; i++) {
      if (((Document) _docs.elementAt(i)).equals(d)) {
        id = i;
        break;
      }
    }
    // figure out where DocDist objects for id start
    int i;
    for (i=0; i<n; i++) {
      DocDist2 dd2 = _sortedDocs[i];
      if (dd2._i==id) break;
    }
    for (int j=0; j<_knn; j++) {
      Document dj = (Document) _docs.elementAt(_sortedDocs[i+j]._j);
      if (docs.contains(dj)) continue;
      else res += _sortedDocs[i+j]._dist;
    }
    return res;
  }


  /**
   * figure out the _knn nearest nbors of d in docs. For each nbor that's not
   * in the original _knn nearest nbors of d in the original doc set, add its
   * distance to the result value.
   * @param d Document
   * @param docs Vector
   * @return double
   */
  private double getKNNborsCost2(Document d, Vector docs) throws ClustererException {
    Vector _docs = getMasterDocSet();
    Vector nbors = new Vector();
    Vector tdocs = new Vector(docs);  // needed for correct sorting
    final int n = docs.size();
    int cn = docs.size()>=_knn ? _knn : docs.size();
    for (int counter=0; counter<cn; counter++) {
      double best_dist = Double.MAX_VALUE;
      int best_ind = -1;
      for (int i=0; i<tdocs.size(); i++) {
        Document di = (Document) tdocs.elementAt(i);
        double dist = Document.d(d, di);
        if (dist<best_dist) {
          best_dist = dist;
          best_ind = i;
        }
      }
      nbors.addElement( (Document) tdocs.elementAt(best_ind));
      tdocs.remove(best_ind);
    }
    double res = 0.0;
    int id=-1;
    for (int i=0; i<n; i++) {
      if (((Document) _docs.elementAt(i)).equals(d)) {
        id = i;
        break;
      }
    }
    // figure out where DocDist objects for id start
    int i;
    for (i=0; i<_docs.size(); i++) {
      DocDist2 dd2 = _sortedDocs[i];
      if (dd2._i==id) break;
    }
    for (int r=0; r<nbors.size(); r++) {
      Document nborr = (Document) nbors.elementAt(r);
      boolean ok = false;
      for (int j = 0; j < _knn && !ok; j++) {
        Document dj = (Document) _docs.elementAt(_sortedDocs[i + j]._j);
        if (dj.equals(nborr)) ok = true;
      }
      if (!ok) res += Document.d(d, nborr);
    }
    return res;
  }


  private double getConnectednessCost(Vector docs) throws GraphException {
    final int n = docs.size();
    final int a = 2*n*_knn;  // max. number of arcs the graph may have...
    // figure out the index of each doc in docs w.r.t. the master doc data set
    // figure out the reverse, i.e. for each doc in the master doc data set its
    // position in docs or -1 if it doesn't exist
    Vector origdocs = getMasterDocSet();
    int inds[] = new int[n];
    int revinds[] = new int[origdocs.size()];
    for (int i=0; i<revinds.length; i++) revinds[i] = -1;
    for (int i=0; i<n; i++) {
      Document di = (Document) docs.elementAt(i);
      for (int j=0; j<origdocs.size(); j++) {
        Document dj = (Document) origdocs.elementAt(j);
        if (di==dj) {
          inds[i] = j;
          revinds[j] = i;
        }
      }
    }
    Graph g = new Graph(n, a);
    for (int i=0; i<n; i++) {
      // get K-NNbors of i
      int indi = inds[i];
      int h = -1;
      for (int j=0; j<_sortedDocs.length; j++) {
        if (_sortedDocs[j]._i==indi) {
          h = j;
          break;
        }
      }
      for (int j=0; j<_knn; j++) {
        int j2 = _sortedDocs[h+j]._j;
        // is j2 nbor in docs?
        int jdocs = revinds[j2];
        if (jdocs==-1) continue;  // j2 is not in docs
        // j2 is indeed in the cluster, so put it in the graph
        double w2 = _sortedDocs[h+j]._dist;
        g.addLink(i,jdocs, w2);
        g.addLink(jdocs,i,w2);  // make it symmetric
      }
    }
    return g.getNumComponents();
  }


  private void sortDocs() throws ClustererException {
    final Vector docs = getMasterDocSet();
    final int n = getMasterDocSet().size();
    _sortedDocs = new DocDist2[n*(n-1)];
    int jj=0;
    for (int i=0; i<n; i++) {
      for (int j=0; j<n; j++) {
        if (i==j) continue;  // don't include self
        double dij = Document.d((Document) docs.elementAt(i), (Document) docs.elementAt(j));
        _sortedDocs[jj++] = new DocDist2(i, j, dij);
      }
    }
    // breakpoint
    Arrays.sort(_sortedDocs);
  }
}


class DocDist2 implements Comparable {
  int _i, _j;
  double _dist;

  DocDist2(int i, int j, double dist) {
    _i = i; _j = j; _dist = dist;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      DocDist2 d2 = (DocDist2) o;
      if (_i==d2._i && _dist==d2._dist) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return _i;
  }

  public int compareTo(Object o) {
    DocDist2 c = (DocDist2) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_i < c._i) return -1;
    else if (_i > c._i) return 1;
    else {  // _i == c._i
      if (_dist < c._dist)return -1;
      else if (_dist == c._dist)return 0;
      else return 1;
    }
  }
}

