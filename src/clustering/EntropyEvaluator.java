package clustering;

import java.util.*;
import java.util.Arrays;


public class EntropyEvaluator extends EnhancedEvaluator {
  private double _rmul = 1.0;  // multiple of min. radius of each hyper-ball around each point to
                               // use to evaluate entropy
  private double _a = 1.0;  // a-structural entropy
  private DocDist2[] _sortedDocs=null;
  private Set[] _nbors=null;  // _nbors[docid]=Set<Integer nbor_docid>
  // DocDist2 class defined in KNNEvaluator.java

  private int[] _clasgns;  // used in evalCluster(Vector<Integer> docids)

  public EntropyEvaluator() {

  }


  public double bestOf(double v1, double v2) {
    return Math.min(v1,v2);
  }


  public double getWorstValue() {
    return Double.MAX_VALUE;
  }


  public void setParams(Hashtable params) {
    if (params!=null) {
      Double pI = (Double) params.get("rmul");
      if (pI!=null) _rmul = pI.doubleValue();
      Double aD = (Double) params.get("a");
      if (aD!=null) _a = aD.doubleValue();
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
   * @param docs Vector<Document>
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
      // clusters[inds[i]].addElement(docs.elementAt(i));
      clusters[inds[i]].addElement(new Integer(i));
    }
    for (int i=0; i<k; i++)
      res += evalCluster(clusters[i]);

    if (_a<1) res -= 1.0;
    else if (_a>1) res += 1.0;

    return res;
  }


  /**
   * Obviously, smaller is better
   * @param docs Vector<Integer id>
   * @throws ClustererException
   * @return double
   */
  public double evalCluster(Vector docids) throws ClustererException {
    final int masterdocsz = getMasterDocSet().size();
    double entropy = 0.0;
    sortDocs();  // called once per Evaluator object

    // int clasgns[] = new int[masterdocsz];  // init to zero
    if (_clasgns==null) _clasgns = new int[masterdocsz];
    for (int i=0; i<masterdocsz; i++) _clasgns[i] = 0;  // reset to zero
    for (int i=0; i<docids.size(); i++) {
      _clasgns[((Integer) docids.elementAt(i)).intValue()]=1;
    }
    for (int i=0; i<masterdocsz; i++) {
      Set nborsi = _nbors[i];
      Iterator nbiter = nborsi.iterator();
      int num_tot = nborsi.size();
      int num_same = 0;  // doc i is included in _nbors[i]
      while (nbiter.hasNext()) {
        int nborid = ((Integer) nbiter.next()).intValue();
        num_same += _clasgns[nborid];
      }
      double pcji = num_same/((double) num_tot);
      if (_a>1) {
        entropy += (-1.0/((double)masterdocsz))*Math.pow(pcji,_a);
      }
      else if (_a==1) {
        if (pcji>0)
          entropy += (-pcji)*Math.log(pcji)/((double)masterdocsz);
        // else if pcji==0 the contribution is zero
      }
      else if (_a<1) {
        entropy += (1.0/((double)masterdocsz))*Math.pow(pcji,_a);
      }
    }
    /*
    if (_a<1) entropy -= 1.0;
    else if (_a>1) entropy += 1.0;
    */
    // finally penalize too large clusters
    // if (docids.size()>0.9*masterdocsz) entropy *= 10;  // itc: HERE rm asap
    return entropy;
  }


  /**
   * Obviously, smaller is better
   * Parzen density estimation is used to compute p(c_j|x_i)=pji
   * @param docs Set<Integer id>
   * @throws ClustererException
   * @return double
   */
  public double evalCluster(Set docids) throws ClustererException {
    final int masterdocsz = getMasterDocSet().size();
    double entropy = 0.0;
    sortDocs();  // called once per Evaluator object

    // int clasgns[] = new int[masterdocsz];  // init to zero
    if (_clasgns==null) _clasgns = new int[masterdocsz];
    for (int i=0; i<masterdocsz; i++) _clasgns[i] = 0;  // reset to zero
    Iterator iter = docids.iterator();
    while (iter.hasNext()) {
      Integer did = (Integer) iter.next();
      _clasgns[did.intValue()] = 1;
    }
    for (int i=0; i<masterdocsz; i++) {
      Set nborsi = _nbors[i];
      Iterator nbiter = nborsi.iterator();
      int num_tot = nborsi.size();
      int num_same = 0;  // doc i is included in _nbors[i]
      while (nbiter.hasNext()) {
        int nborid = ((Integer) nbiter.next()).intValue();
        num_same += _clasgns[nborid];
      }
      double pcji = num_same/((double) num_tot);
      if (_a>1) {
        entropy += (-1.0/((double)masterdocsz))*Math.pow(pcji,_a);
      }
      else if (_a==1) {
        if (pcji>0)
          entropy += (-pcji)*Math.log(pcji)/((double)masterdocsz);
        // else if pcji==0 the contribution is zero
      }
      else if (_a<1) {
        entropy += (1.0/((double)masterdocsz))*Math.pow(pcji,_a);
      }
    }
    return entropy;
  }


  /**
   * Obviously, smaller is better
   * @param docs Vector<Integer id>
   * @throws ClustererException
   * @return double
   */
  public double evalClusterOld(Vector docids) throws ClustererException {
    final int masterdocsz = getMasterDocSet().size();
    double entropy = 0.0;
    sortDocs();  // called once per Evaluator object

    for (int i=0; i<docids.size(); i++) {
      int did = ((Integer) docids.elementAt(i)).intValue();
      Set nborsi = _nbors[did];
      Iterator nbiter = nborsi.iterator();
      int num_tot = nborsi.size();
      int num_same = 1;
      while (nbiter.hasNext()) {
        int nborid = ((Integer) nbiter.next()).intValue();
        if (docids.contains(new Integer(nborid)))
          num_same++;
      }
      double pcji = num_same/((double) num_tot);
      if (_a>1) {
        entropy += (-1/masterdocsz)*Math.pow(pcji,_a);
      }
      else if (_a==1) {
        entropy += (-pcji)*Math.log(pcji)/masterdocsz;
      }
      else if (_a<1) {
        entropy += (1/masterdocsz)*Math.pow(pcji,_a);
      }
    }
    if (_a<1) entropy -= 1.0;
    else if (_a>1) entropy += 1.0;
    // finally penalize too large clusters
    if (docids.size()>0.75*masterdocsz) entropy *= 10;  // itc: HERE rm asap
    return entropy;
  }


  public Set getNeighbors(int docid) throws ClustererException {
    if (_nbors==null)
      throw new ClustererException(
        "EntropyEvaluator.getNeighbors(): empty _nbors array");
    return _nbors[docid];
  }


  private synchronized void sortDocs() throws ClustererException {
    final Vector docs = getMasterDocSet();
    final int n = docs.size();
    if (_sortedDocs==null) {
      _sortedDocs = new DocDist2[n * n];
      int jj = 0;
      double avg=0;
      for (int i = 0; i < n; i++) {
        double minij=Double.MAX_VALUE;
        for (int j = 0; j < n; j++) {
          double dij = Document.d( (Document) docs.elementAt(i),
                                  (Document) docs.elementAt(j));
          if (i!=j && dij < minij)
            minij = dij;
          _sortedDocs[jj++] = new DocDist2(i, j, dij);
        }
        avg += minij;
      }
      avg /= ((double) n);  // used to be avg /= jj;
      // breakpoint
      Arrays.sort(_sortedDocs);

      // figure out nbors of each document
      double radius = _rmul*avg;
      _nbors = new Set[docs.size()];
      int ii=0;  // maintains position in _sortedDocs array
      for (int i=0; i<docs.size(); i++) {
        _nbors[i] = new HashSet();
        for (; ii < _sortedDocs.length && _sortedDocs[ii]._i<=i; ii++) {
          if (_sortedDocs[ii]._i==i) {
            if (_sortedDocs[ii]._dist<=radius) {  // include self i.e. the i doc in _nbors[i]
              _nbors[i].add(new Integer(_sortedDocs[ii]._j));
            }
            else {
              ii++;
              break; // rest of neighbors are too far to be considered "nbors"
            }
          }
        }
      }
    }
  }
}

