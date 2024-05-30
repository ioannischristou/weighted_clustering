package clustering;

import java.util.*;

public class SDKMeansClustererAux extends ClustererAux {
  private Vector _docs;  // ref to c._docs
  private Vector _centers;  // ref. to c._centers

  public SDKMeansClustererAux(Clusterer c, int[] indi, int[] numi) {
    super(c, indi, numi);
    _docs = c.getCurrentDocs();
    _centers = c.getCurrentCenters();
  }


  public void runTask() {
    int dims = dims = ((Document) _docs.elementAt(0)).getDim();
    double[][] dists = ((SDKMeansSqrMTClusterer) _master)._dists;
    int k = _centers.size();
    double best_gain = Double.POSITIVE_INFINITY;  // local best_gain
    // when a gain better than the best_gain found so far by this thread
    // is encountered, we call updateBest() which might or might not actually
    // update the global _bestGain for this iteration kept in the _master
    // (e.g. if another thread has already found a better gain and updated the
    // _master._bestGain)

    for (int i=_starti; i<=_endi; i++) {
      Document di = (Document) _docs.elementAt(i);
      int p = _ind[i];
      if (_numi[p]==1) continue;  // cannot move because it will empty cluster
      Document cp = (Document) _centers.elementAt(p);
      // moving di from p, will result in a new cp
      Document cp_new = new Document(new TreeMap(), dims);
      cp_new.addMul(_numi[p]/((double) _numi[p]-1), cp);
      cp_new.addMul(-1.0/((double) _numi[p]-1), di);
      for (int l=0; l<k; l++) {
        if (l==p) continue;  // don't count distance from current center
        Document cl = (Document) _centers.elementAt(l);
        double v_li = (_numi[l]/((double) _numi[l]+1))*dists[i][l] -
                      (_numi[p]/((double) _numi[p]-1))*dists[i][p];
        if (v_li<best_gain) {
          // moving di to cl will result in a cl_new
          Document cl_new = new Document(new TreeMap(), dims);
          cl_new.addMul(_numi[l]/((double) _numi[l]+1), cl);
          cl_new.addMul(1.0/((double) _numi[l]+1), di);
          ((SDKMeansSqrMTClusterer) _master).updateBest(v_li, i, l, cp_new, cl_new);  // (gain, ind, part, cp_new, cl_new)
          best_gain = v_li;
        }
      }
    }

  }
}
