package clustering;

import java.util.Hashtable;
import java.util.Vector;

public class DensityEvaluator extends EnhancedEvaluator {
  private static final double _a = 0.25;  // exponent to dampen the effect of #elems in density calculation

  public DensityEvaluator() {
  }


  public void setParams(Hashtable t) {
    // no-op
  }


  /**
   * return the total density value of the clusters found by clusterer cl.
   * Bigger is better.
   * @param cl Clusterer
   * @return double
   */
  public double eval(Clusterer cl) throws ClustererException {
    final int[] inds = cl.getClusteringIndices();
    return eval(cl.getCurrentDocs(), inds);
  }


  /**
   *
   * @param docs Vector Vector<Document>
   * @param inds int[] array[0...docs.size()-1] values in [0,...k-1]
   * @throws ClustererException
   * @return double
   */
  public double eval(Vector docs, int[] inds) throws ClustererException {
    double res = 0.0;

    final int n = inds.length;
    // figure out k
    int k=0;
    for (int i=0; i<n; i++)
      if (k<inds[i]+1) k = inds[i]+1;
    // figure out cards[j] j=0...k-1
    int cards[] = new int[k];
    for (int i=0; i<n; i++) {
      cards[inds[i]]++;
    }

    double cluster_density[] = new double[k];  // init. to zero by default
    Hashtable clusters = new Hashtable();  // map<Integer clusterid, Vector<Integer docid> >
    for (int i=0; i<n; i++) {
      int indi = inds[i];
      Integer ii = new Integer(indi);
      Vector docsi = (Vector) clusters.get(ii);
      if (docsi == null) {
        docsi = new Vector();
      }
      else {
        // update the cluster_density of cluster indi, by adding to it
        // the contribution of doc-i in it.
        double contr = 0.0;
        DocumentDistIntf ddd = new DocumentDensityDist();
        Document doci = (Document) docs.elementAt(i);
        for (int j=0; j<docsi.size(); j++) {
          Integer docj_id = (Integer) docsi.elementAt(j);
          Document docj = (Document) docs.elementAt(docj_id.intValue());
          if (doci==docj) continue;  // don't compare with yourself
          contr += ddd.dist(doci, docj);
        }
        cluster_density[indi] += contr;
      }
      docsi.addElement(new Integer(i));
      clusters.put(ii, docsi);  // ensure addition into Hashtable, required!
    }
    for (int i=0; i<k; i++) {
      if (cards[i]==0) continue;  // safe-guard against empty clusters...
      cluster_density[i] /= Math.pow(cards[i], _a);  // divide by #elems in cluster i raised to the power of a
      res += cluster_density[i];
    }
    return res;
  }


  /**
   *
   * @param docs Vector Vector<Document>
   * @throws ClustererException
   * @return double
   */
  public double evalCluster(Vector docs) throws ClustererException {
    double res = 0.0;
    DocumentDensityDist ddd = new DocumentDensityDist();
    for (int i=0; i<docs.size(); i++) {
      Document doci = (Document) docs.elementAt(i);
      for (int j=i+1; j<docs.size(); j++) {
        Document docj = (Document) docs.elementAt(j);
        res += ddd.dist(doci, docj);
      }
    }
    res /= Math.pow(docs.size(), _a);
    return res;
  }

}

