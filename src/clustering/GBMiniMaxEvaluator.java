package clustering;

import java.util.*;
import java.util.Arrays;
import coarsening.*;

public class GBMiniMaxEvaluator extends EnhancedEvaluator {
  private double _p = 0.1;  // percent of edges to consider
  private DocDist[] _sortedDocs=null;
  // DocDist class defined in PCAEnsembleClusterer.java
  private Graph _g = null;

  public GBMiniMaxEvaluator() {
  }


  public void setParams(Hashtable params) {
    if (params!=null) {
      Double pI = (Double) params.get("p");
      if (pI!=null) _p = pI.doubleValue();
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
   * Figure out cluster "measure" as discussed in "Pattern Recognition" by
   * Theodoridis and Koutroumbas, 3rd ed. pp. 763-764.
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
        try {
          Vector origdocs = getMasterDocSet();
          final int ntot = origdocs.size();
          // now figure out total Graph
          int a = (int) Math.floor(_p * ntot * (ntot-1)/2);
          _g = new Graph(ntot, a);
          for (int i = 0; i < a; i++) {
            DocDist dd3 = _sortedDocs[i];
            _g.addLink(dd3._i, dd3._j, dd3._dist);
          }
        }
        catch (GraphException e) {
          e.printStackTrace();
          throw new ClustererException("GBEvaluator.evalCluster() failed");
        }
      }
    }
    // figure out the index of each doc in docs w.r.t. the master doc data set
    // figure out the reverse, i.e. for each doc in the master doc data set its
    // position in docs or -1 if it doesn't exist
    final int n = docs.size();
    Vector origdocs = getMasterDocSet();
    int inds[] = new int[n];
    int revinds[] = new int[origdocs.size()];
    for (int i=0; i<revinds.length; i++) revinds[i] = -1;
    for (int i=0; i<n; i++) {
      Document di = (Document) docs.elementAt(i);
      for (int j=0; j<origdocs.size(); j++) {
        Document dj = (Document) origdocs.elementAt(j);
        if (di.equals(dj)) {
          inds[i] = j;
          revinds[j] = i;
        }
      }
    }
    // compute qC: #edges connecting vertices in docs w/ rest of world
    double qC = 0.0;
    // compute rC: #edges connecting vertices in docs only
    double rC = 1.0;
    for (int i=0; i<n; i++) {
      int indi = inds[i];
      Node ni = _g.getNode(indi);
      Set ninbors = ni.getNbors();
      Iterator it = ninbors.iterator();
      while (it.hasNext()) {
        Node nbor = (Node) it.next();
        if (revinds[nbor.getId()]==-1)
          qC += 2.0;
        else rC += 1.0;
      }
    }
    res = qC/rC;
    return res;
  }


  private void sortDocs() throws ClustererException {
    final Vector docs = getMasterDocSet();
    final int n = docs.size();
    _sortedDocs = new DocDist[n*(n-1)/2];
    int jj=0;
    for (int i=0; i<n; i++) {
      for (int j=i+1; j<n; j++) {
        double dij = Document.d((Document) docs.elementAt(i), (Document) docs.elementAt(j));
        _sortedDocs[jj++] = new DocDist(i, j, dij);
      }
    }
    // breakpoint
    Arrays.sort(_sortedDocs);
  }
}

/*
class DocDist3 implements Comparable {
  int _i, _j;
  double _dist;

  DocDist3(int i, int j, double dist) {
    _i = i; _j = j; _dist = dist;
  }

  public int compareTo(Object o) {
    DocDist3 c = (DocDist3) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_dist < c._dist)return -1;
    else if (_dist == c._dist)return 0;
    else return 1;
  }
}
*/
