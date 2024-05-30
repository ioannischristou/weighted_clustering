package clustering;

import java.util.*;

public class MutualInformationEvaluator extends EnhancedEvaluator {
  private int _nij[][];

  public MutualInformationEvaluator() {
  }


  public double evalCluster(Vector docs) throws ClustererException {
    throw new ClustererException("not implemented");
  }


  public double eval(Vector docs, int[] inds) throws ClustererException {
    throw new ClustererException("not implemented");
  }


  public void setParams(Hashtable p) {
    return;  // not implemented
  }


  public double eval(Clusterer cl) throws ClustererException {
    Hashtable params = cl.getParams();
    int trueclusterindices[] = (int[]) params.get("trueclusters");
    final int num_clusters = ((Integer) params.get("num_clusters")).intValue();
    int clusterindices[] = cl.getClusteringIndices();
    final int k = ((Integer) params.get("k")).intValue();  // true number of classes, or clusters
    final int n = cl.getCurrentDocs().size();

    getCommons(trueclusterindices,k, clusterindices,num_clusters);
    int[] cardsk = getCards(trueclusterindices,k);
    int[] cardsnc = getCards(clusterindices,num_clusters);

    // _nij[k][num_clusters] array
    final int kab = k*num_clusters;
    final double lnb = Math.log((double) kab);
    double res = 0.0;
    double vij;
    for (int l=0; l<k; l++) {
      if (cardsk[l]==0) continue;
      for (int h=0; h<num_clusters; h++) {
        if (cardsnc[h]==0) continue;
        vij = _nij[l][h];
        if (vij==0) continue;
        res += vij*Math.log(vij*n/(cardsk[l]*cardsnc[h]))/lnb;
      }
    }
    res *= (2.0 / (double) n);
    return res;
  }


  private void getCommons(int inds1[], int ka, int inds2[], int k) {
    double res = 0.0;
    final int n = inds1.length;
    _nij = new int[ka][k];  // nij[i][j] is #objs simultaneously in
                                  // cluster i in inds1, and cluster j in inds2
    for (int i=0; i<ka; i++) {
      for (int j = 0; j < k; j++) _nij[i][j] = 0; // init
    }
    for (int i=0; i<n; i++) {
        _nij[inds1[i]][inds2[i]]++;
    }
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
