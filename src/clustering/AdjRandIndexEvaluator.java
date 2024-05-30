package clustering;

import java.util.*;

public class AdjRandIndexEvaluator extends EnhancedEvaluator {
  public AdjRandIndexEvaluator() {
  }


  /**
   * compute the Adjusted Rand Index between the clusterer's clustering and
   * the true clusters that must have been provided as a parameter to the
   * clusterer as well, under the attribute key "trueclusters"
   * The Adjusted Rand Index is described in the paper
   * Kuncheva L. and Vetrov, D.P. "Evaluation of Stability of K-Means Cluster
   * Ensembles with Respect to Random Initialization", IEEE Transactions on
   * Pattern Analysis and Machine Intelligence, 28(11), Nov. 2006, pp. 1798-1808
   * @param cl Clusterer
   * @throws ClustererException
   * @return double
   */
  public double eval(Clusterer cl) throws ClustererException {
    double res=0.0;
    Hashtable params = cl.getParams();
    int trueclusterindices[] = (int[]) params.get("trueclusters");
    int clusterindices[] = cl.getClusteringIndices();
    final int k = ((Integer) params.get("k")).intValue();
    final int ka = cl.getCurrentCenters().size();
    final int n = cl.getCurrentDocs().size();
    double t2 = getT(trueclusterindices, k);
    double t1 = getT(clusterindices, ka);
    double t12 = getT12(clusterindices, ka, trueclusterindices, k);
    double t3 = 2*t1*t2/(n*(n-1));
    res = (t12-t3)/((t1+t2)/2.0-t3);
    return res;
  }


  public double evalCluster(Vector clusterDocs) throws ClustererException {
    throw new ClustererException("method not supported");
  }
  public double eval(Vector docs, int[] indices) throws ClustererException {
    throw new ClustererException("method not supported");
  }
  public void setParams(Hashtable params) {
    // no-op
  }


  public double eval(int[] soln1, int k1, int[] soln2, int k2) throws ClustererException {
    final int n = soln1.length;
    double t2 = getT(soln2, k2);
    double t1 = getT(soln1, k1);
    double t12 = getT12(soln1, k1, soln2, k2);
    double t3 = 2*t1*t2/(n*(n-1));
    double res = (t12-t3)/((t1+t2)/2.0-t3);
    return res;
  }


  private double getT(int inds[], int k) throws ClustererException {
    int cards[] = getCards(inds, k);
    double res = 0.0;
    for (int i=0; i<k; i++) {
      int ni = cards[i];
      res += ni*(ni-1)/2.0;
    }
    return res;
  }


  /**
   * find the objects nij that simultaneously belong to cluster i in inds1
   * and cluster j in inds2, forall i = 1...ka, j = 1...k
   * @param inds1 int[]
   * @param ka int
   * @param inds2 int[]
   * @param k int
   * @return double
   */
  private double getT12(int inds1[], int ka, int inds2[], int k) {
    double res = 0.0;
    final int n = inds1.length;
    int nij[][] = new int[ka][k];  // nij[i][j] is #objs simultaneously in
                                  // cluster i in inds1, and cluster j in inds2
    for (int i=0; i<ka; i++) {
      for (int j = 0; j < k; j++) nij[i][j] = 0; // init
    }
    for (int i=0; i<n; i++) {
        nij[inds1[i]][inds2[i]]++;
    }
    for (int i=0; i<ka; i++) {
      for (int j=0; j<k; j++) {
        res += nij[i][j]*(nij[i][j]-1)/2.0;
      }
    }
    return res;
  }


  private int[] getCards(int inds[], int k) throws ClustererException {
    if (inds==null)
      throw new ClustererException("null Indices");
    final int n = inds.length;
    int[] cards = new int[k];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<n; i++) {
      cards[inds[i]]++;
    }
    return cards;
  }

}
