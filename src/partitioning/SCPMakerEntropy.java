package partitioning;

import clustering.DocumentDistIntf;
import clustering.Document;
import clustering.ClustererException;
import clustering.EnhancedEvaluator;
import clustering.EntropyEvaluator;
import java.util.*;
import java.io.*;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

public class SCPMakerEntropy implements SCPMakerIntf {
  private Vector _docs;
  private int _k;
  private double _theta;
  private int _maxSetCard;
  private Hashtable _params;
  private DoubleMatrix2D _A;
  private DoubleMatrix1D _c;
  private DocumentDistIntf _m;
  private int[] _prevSolutions;  // _prevSolutions[i] is the number of times
                                 // the i-th column has been part of a
                                 // clustering solution. Of course, columns
                                 // may enter and leave the problem...
  private EntropyEvaluator _mstdeval;

  public SCPMakerEntropy() {
    _params = new Hashtable();
  }


  /**
   * create a Set Covering/Partitioning Problem using the documents passed in
   * as the 1st arg., and adding any clusters passed in as 2nd arg. The number
   * k is the number of clusters desired. theta is the increment used in adding
   * more documents to a column in the A (constraints) matrix of the problem.
   * maxsetcard is the minimum number of columns to be generated per document.
   * m is the DocumentDistIntf metric to be used.
   * @param docs Vector<Document d>
   * @param clusters Vector<Vector<Integer doc_id> cluster>
   * @param k int
   * @param theta double
   * @param maxsetcard int
   * @param m DocumentDistIntf
   * @param wgts double[] unused
   * @throws PartitioningException
   */
  public void createSCP(Vector docs, Vector clusters, int k,
                        double theta, int maxsetcard, DocumentDistIntf m,
                        double[] wgts)
      throws PartitioningException {
    try {
      _docs = docs;
      _k = k;
      _theta = theta;
      _maxSetCard = maxsetcard;
      _m = m;
      final int docsize = _docs.size();
      final double c_s = (double) docsize / (double) _k;
      double cluster_factor = 1.0;  // itc: HERE factor used to be 2.0
      Double cfD = (Double) _params.get("clusterfactor");
      if (cfD!=null) cluster_factor = cfD.doubleValue();
      final int clusters_per_doc1 = (int) Math.ceil(cluster_factor*c_s / _theta+1.0);
      int clusters_per_doc = clusters_per_doc1 > _maxSetCard ? clusters_per_doc1 : _maxSetCard;
      Boolean use_clusters_onlyB = (Boolean) _params.get("useonlyclusteringresults");
      final boolean use_clusters_only = (use_clusters_onlyB!=null && use_clusters_onlyB.booleanValue()==true);
      Integer boundpointsetI = (Integer) _params.get("boundpointset");
      final int bound_clusters_per_doc = boundpointsetI!=null ? boundpointsetI.intValue() : clusters_per_doc;
      if (clusters_per_doc > bound_clusters_per_doc) clusters_per_doc = bound_clusters_per_doc;
      // create _A and _c
      int rows = _docs.size();
      int cols = (clusters_per_doc + 2) * docsize;
      if (clusters!=null) cols += clusters.size();
      if (use_clusters_only) cols = clusters.size();
      _A = new SparseDoubleMatrix2D(rows, cols);
      _c = new DenseDoubleMatrix1D(cols);
      System.err.println("Matrix _A["+rows+","+cols+"] constructed");  // itc: HERE rm asap
      _mstdeval = (EntropyEvaluator) getParam("ggadevaluator");
      _mstdeval.setParams(_params);  // make sure params are set properly for the guy
      _mstdeval.setMasterDocSet(_docs);
      // populate _A and _c
      SCPMakerHelper helper = new SCPMakerHelper(_docs, _m);
      int column_counter = 0;
      for (int i = 0; use_clusters_only==false && i < docsize && column_counter < _A.columns(); i++) {
        // System.err.println("constructing columns for d"+i+"...");
        final Document di = (Document) _docs.elementAt(i);
        // put in di alone in a column
        _A.setQuick(i, column_counter, 1.0);
        // _c.setQuick(column_counter, 0.0);
        // the above holds true only for K-Means/Median type clustering functions
        Vector div = new Vector();
        div.addElement(new Integer(i));
        double cdi = _mstdeval.evalCluster(div);
        _c.setQuick(column_counter, cdi);

        column_counter++;
        // now start putting clusters of elements in multiples of theta
        int to = 0;
        Vector nnborids=null;
        Vector nnbors = new Vector();
        Document max_cluster_center = null;
        int j;
        theta = _theta;
        double cost = -1;

        int inc1pt = 10;
        Integer inc1ptI = (Integer) _params.get("inc1pt");
        if (inc1ptI!=null) inc1pt = inc1ptI.intValue();
        double inc1val = 2.0;
        Double inc1valD = (Double) _params.get("inc1val");
        if (inc1valD!=null) inc1val = inc1valD.doubleValue();
        int inc2pt = 5;
        Integer inc2ptI = (Integer) _params.get("inc2pt");
        if (inc2ptI!=null) inc2pt = inc2ptI.intValue();
        double inc2val = 3.0;
        Double inc2valD = (Double) _params.get("inc2val");
        if (inc2valD!=null) inc2val = inc2valD.doubleValue();
        int inc3pt = 2;
        Integer inc3ptI = (Integer) _params.get("inc3pt");
        if (inc3ptI!=null) inc3pt = inc3ptI.intValue();
        double inc3val = 4.0;
        Double inc3valD = (Double) _params.get("inc3val");
        if (inc3valD!=null) inc3val = inc3valD.doubleValue();
        Integer inconeszI = (Integer) _params.get("inconesz");
        int inconesz = 10;  // default
        if (inconeszI!=null) inconesz = inconeszI.intValue();

        for (j = 1; j <= clusters_per_doc && column_counter < _A.columns(); j++) {
          // heuristic: for the last clusters, boost up theta so as to have
          // some columns with larger coverage
          if (j==clusters_per_doc-inc1pt) theta *= inc1val;
          else if (j==clusters_per_doc-inc2pt) theta *= inc2val;
          else if (j==clusters_per_doc-inc3pt) theta *= inc3val;
          // end boosting heuristics
          // first inconesz elements are always in 1-by-1
          if (j<=inconesz) to++;  // to += 1;
          else to += ((int) theta);
          nnborids = helper.getKNN(i, to);  // Vector<Integer id>
          nnborids.addElement(new Integer(i));  // add di as well
          nnbors.clear();
          // set _A
          for (int jj=0; jj<nnborids.size(); jj++) {
            Integer ii2 = (Integer) nnborids.elementAt(jj);
            nnbors.addElement(_docs.elementAt(ii2.intValue()));
            _A.setQuick(ii2.intValue(), column_counter, 1);
          }
          // compute cost
          // Vector r = Document.getCenterCosted(nnbors, _m);
          // cost = ((Double) r.elementAt(1)).doubleValue();
          // max_cluster_center = (Document) r.elementAt(0);
          cost = _mstdeval.evalCluster(nnborids);
          _c.setQuick(column_counter, cost);
          column_counter++;
        }
        // heuristic: add a column containing all elements of nnbors plus
        // all those less than the average distance from max_cluster_center
        double avgdist = 0.0;
        for (int jj=0; jj<nnbors.size(); jj++) {
          Document djj =(Document) nnbors.elementAt(jj);
          avgdist += _m.dist(max_cluster_center,djj);
        }
        avgdist /= nnbors.size();
        for (int jj=0; jj<docsize; jj++) {
          Document djj = (Document) _docs.elementAt(jj);
          Integer jji = new Integer(jj);
          if (nnborids.contains(jji)) continue;
          // double chance = Math.random();  // with a very small probability add element
          double chance = utils.RndUtil.getInstance().getRandom().nextDouble();
          if (_m.dist(djj, max_cluster_center) < 1.25*avgdist || chance<0.0001) {
            nnbors.addElement(djj);
            nnborids.add(jji);
          }
        }
        // set _A
        for (int jj=0; jj<nnborids.size(); jj++) {
          Integer ii2 = (Integer) nnborids.elementAt(jj);
          _A.setQuick(ii2.intValue(), column_counter, 1);
        }
        // set _c
        // Vector r = Document.getCenterCosted(nnbors, _m);
        // cost = ((Double) r.elementAt(1)).doubleValue();
        // max_cluster_center = (Document) r.elementAt(0);
        cost = _mstdeval.evalCluster(nnborids);
        _c.setQuick(column_counter, cost);
        column_counter++;
        // end heuristics
      }
      // put in the columns described in the clusters Vector
      if (clusters!=null) {
        Vector cjdocs = new Vector();
        for (int j=0; j<clusters.size(); j++) {
          Vector clusterj = (Vector) clusters.elementAt(j);  // Vector<Integer docid>
          for (int jj=0; jj<clusterj.size(); jj++) {
            Integer docid = (Integer) clusterj.elementAt(jj);
            _A.setQuick(docid.intValue(), column_counter, 1);
          }
          // Vector cjdocs = new Vector();
          /*
          cjdocs.clear();
          for (int l=0; l<clusterj.size(); l++) {
            Integer did = (Integer) clusterj.elementAt(l);
            Document dl = (Document) _docs.elementAt(did.intValue());
            cjdocs.addElement(dl);
          }
          Vector centerjcosted = Document.getCenterCosted(cjdocs, _m);
          double cost = ((Double) centerjcosted.elementAt(1)).doubleValue();
          */
          double cost = _mstdeval.evalCluster(clusterj);
          _c.setQuick(column_counter, cost);
          column_counter++;
        }
      }
      // create _prevSolutions array
      _prevSolutions = new int[cols];
      for (int i=0; i<_prevSolutions.length; i++) _prevSolutions[i] = 0;
    }
    catch (ClustererException e) {
      e.printStackTrace();
      throw new PartitioningException("createSCP(): failed.");
    }
  }


  public DoubleMatrix1D getCostVector() throws PartitioningException {
    if (_c==null) throw new PartitioningException("no cost vector");
    return _c;
  }


  public DoubleMatrix2D getConstraintsMatrix() throws PartitioningException {
    if (_A==null) throw new PartitioningException("no constraints matrix");
    return _A;
  }


  /**
   * the argument passed in is an array of 0s and 1s of length _A.columns()
   * describing which column is contained in the solution of the SCP problem.
   * The output array is an array of length _A.rows() of integers, describing
   * cluster index assignments, in particular to which cluster [0...k-1] each
   * document belongs to.
   * Side effect: the _prevSolutions[] array is updated, so that we don't
   * overwrite columns that were previously part of the solution.
   * @param scpsol int[]
   * @param wgts double[] unused
   * @throws PartitioningException
   * @return int[]
   */
  public int[] convertSolution(int[] scpsol, double[] wgts) throws PartitioningException {
    int min_col_sz = Integer.MAX_VALUE; int max_col_sz = 0;  // statistics
    try {
      DoubleMatrix1D X = new DenseDoubleMatrix1D(scpsol.length);
      for (int i=0; i<X.size(); i++) {
        X.setQuick(i, (double) scpsol[i]);
        if (scpsol[i]==1) {
          _prevSolutions[i]++;
          DoubleMatrix1D ai = _A.viewColumn(i);
          int sz = (int) ai.zSum();
          if (min_col_sz > sz) min_col_sz = sz;
          if (max_col_sz < sz) max_col_sz = sz;
        }
      }
      System.out.println("SCP problem solution statistics: min_col_sz = "+min_col_sz + " max_col_sz = "+max_col_sz);  // itc: HERE
      Hashtable Xindices = new Hashtable();  // map<Integer Xindex, Integer clusterindex>
      Hashtable Xcenters = new Hashtable();  // map<Integer Xindex, Document Xcenter>
      int index=0;
      for (int i=0; i<X.size(); i++) {
        if (X.getQuick(i)==1.0) {
          Xindices.put(new Integer(i), new Integer(index++));
          Vector Xdocs = new Vector();
          for (int j=0; j<_A.rows(); j++) {
            if (_A.getQuick(j,i)>0) Xdocs.addElement(_docs.elementAt(j));
          }
          Document center = Document.getCenter(Xdocs, null);  // itc-20220223: HERE
          Xcenters.put(new Integer(i), center);
        }
      }

      int clusterindices[] = new int[_A.rows()];
      // convert the scpsolution to an sppsolution
      for (int i=0; i<clusterindices.length; i++) clusterindices[i] = -1;  // init
      Hashtable duplicates = new Hashtable();  // map<Integer docid, Set<Integer column_id> >
      // 1. find out all duplicates
      for (int row=0; row<_A.rows(); row++) {
        DoubleMatrix1D A_r = _A.viewRow(row);
        double num_apps = A_r.zDotProduct(X);
        if (num_apps>1.0) {
          // we have duplicates
          Set dups = new HashSet();
          for (int col=0; col<_A.columns(); col++) {
            if (A_r.getQuick(col)==1 && X.getQuick(col)==1) {
              dups.add(new Integer(col));
            }
          }
          duplicates.put(new Integer(row), dups);
        }
      }
      // 2. convert solution by removing duplicates, and then mapping solution
      // to a clusterIndices[] array
      for (int i=0; i<scpsol.length; i++) {
        if (scpsol[i]==1) {
          // Xi is in. Figure out which documents it contains
          DoubleMatrix1D col_i = _A.viewColumn(i);
          for (int j=0; j<col_i.size(); j++) {
            if (col_i.getQuick(j)==1) {
              // j-th doc is in cluster i
              Set dups = (Set) duplicates.get(new Integer(j));
              if (dups==null) {
                // no duplicate appearances, just set clusterindex
                clusterindices[j] =
                    ((Integer) Xindices.get(new Integer(i))).intValue();
              }
              else {
                if (clusterindices[j]>=0) continue;  // already set up
                // duplicate appearances exist. Choose the cluster Xi whose center
                // is closest to document j.
                System.err.println("Document "+j+" appears in "+dups.size()+" clusters. Removing from all but one.");  // itc: HERE rm asap
                Document dj = (Document) _docs.elementAt(j);
                Iterator it = dups.iterator();
                double bestcost = Double.MAX_VALUE;
                Integer bestcol=null;
                while (it.hasNext()) {
                  Integer col_id = (Integer) it.next();
                  Document ccenter = (Document) Xcenters.get(col_id);
                  double ct = _m.dist(ccenter, dj);
                  if (ct<bestcost) {
                    bestcost = ct;
                    bestcol = col_id;
                  }
                }
                clusterindices[j] = ((Integer) Xindices.get(bestcol)).intValue();

                // itc: HERE new heuristic
                // update Xcenters containing dj to indicate removal of bestcol from X column
                Document d = (Document) _docs.elementAt(j);
                it = dups.iterator();
                while (it.hasNext()) {
                  Integer col_id = (Integer) it.next();
                  Document col_center = (Document) Xcenters.get(col_id);
                  DoubleMatrix1D col = _A.viewColumn(col_id.intValue());
                  int col_size = (int) col.zSum();
                  if (col_size<2) continue;  // should not happen
                  double cs1 = (double) (col_size-1) / (double) col_size;
                  col_center.div(cs1);
                  col_center.addMul(-1.0/((double)(col_size-1)), d);
                  Xcenters.put(col_id, col_center);
                }
                // done updating Xcenters

              }
            }
          }
        }
      }
      return clusterindices;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("convertSolution(): failed");
    }
  }


  public int[] convertSolution(double[] scpsol) throws PartitioningException {
    throw new PartitioningException("method not supported");
  }


  public void addOverExistingColumns(Vector clusters,
                                     double[] scpsolution)
      throws PartitioningException {
    throw new PartitioningException("method not supported");
  }


  /**
   * adds the specified clusters as solutions in the matrix _A and the cost vector
   * _c. Leaves the columns described in scpsolution intact, as they are part of
   * the solution found before.
   * It also modifies some columns not in the solution
   * @param clusters Vector<Vector<Integer docid> cluster>
   * @param scpsolution int[]
   * @param wgts double[] unused
   * @throws PartitioningException
   */
  public void addOverExistingColumns(Vector clusters,
                                     int[] scpsolution,
                                     double[] wgts)
      throws PartitioningException {
    try {
      int cur_col = 0;
      if (clusters==null) return;
      // how many docs will move in non-solution columns?
      Integer numdocs2moveI = (Integer) _params.get("numdocs2move");
      int numdocs2move = numdocs2moveI!=null ? numdocs2moveI.intValue() : 0;
      Vector shuffled_cols = new Vector();  // Vector<Integer row>
      for (int i=0; i<_A.columns(); i++) shuffled_cols.addElement(new Integer(i));
      Collections.shuffle(shuffled_cols, utils.RndUtil.getInstance().getRandom());  // randomly shuffled the contents
      for (int i=0; i<clusters.size(); i++) {
        Vector clusteri = (Vector) clusters.elementAt(i);
        DoubleMatrix1D A_col=null;

        int cur_col2 = 0;
        // put clusters over the previous solution
        while (cur_col<scpsolution.length && scpsolution[cur_col]==0) {
          // modify the cur_col with a small probability
          A_col = _A.viewColumn(cur_col);
          double cur_col_ones = A_col.zSum();
          boolean col_has_docs = cur_col_ones > numdocs2move*(3.0 / 2.0);
          if (i==0 &&
              utils.RndUtil.getInstance().getRandom().nextDouble()<0.15 &&
              numdocs2move>0 &&
              col_has_docs) {
            Vector cidocs = new Vector();
            Vector iidocs = new Vector();
            Vector idocs2move = new Vector();
            Vector docs2move = new Vector();
            Vector docids2move = new Vector();
            System.out.print("Moving docs from column " + cur_col +": ");
            for (int l = 0; l < _A.rows(); l++) {
              Document dl = (Document) _docs.elementAt(l);
              if (cur_col_ones<numdocs2move) cur_col_ones = _A.rows();  // move with very small probability
              if (_A.getQuick(l, cur_col) > 0.0 &&
                  utils.RndUtil.getInstance().getRandom().nextDouble() < (numdocs2move / cur_col_ones)) {
                // document l moves
                System.out.print(", "+l);
                idocs2move.addElement(new Integer(l));
                docs2move.addElement(dl);
              }
              else { // document l stays
                iidocs.addElement(new Integer(l));
                cidocs.addElement(dl);
              }
            }
            // figure out where documents to move go
            int cur_col3=-1;
            if (docs2move.size() > 0) {
              boolean cont = true;
              for (; cur_col2 < _A.columns(); cur_col2++) {
                cur_col3 = ((Integer) shuffled_cols.elementAt(cur_col2)).intValue();
                if (cur_col == cur_col3 || scpsolution[cur_col3] == 1)
                  continue;
                cont = false;
                // cur_col2 must not contain any of the docs in idocs2move
                for (int j = 0; j < idocs2move.size(); j++) {
                  Integer jj = (Integer) idocs2move.elementAt(j);
                  if (_A.getQuick(jj.intValue(), cur_col3) > 0.0) {
                    cont = true;
                    break;
                  }
                }
                if (!cont)break;
              }
              System.out.println(".");
              if (cur_col2 < _A.columns() && cont == false) {
                // modify cur_col & cur_col3
                System.out.println(" Moving the docs to column "+cur_col3);
                // Vector centerjcosted = Document.getCenterCosted(cidocs, _m);
                // double cost = ( (Double) centerjcosted.elementAt(1)).
                //    doubleValue();
                double cost = _mstdeval.evalCluster(iidocs);
                _c.setQuick(cur_col, cost);
                for (int j = 0; j < _A.rows(); j++) {
                  if (_A.getQuick(j, cur_col3) > 0.0) {
                    Document dj = (Document) _docs.elementAt(j);
                    docs2move.addElement(dj);
                    docids2move.addElement(new Integer(j));
                  }
                }
                // centerjcosted = Document.getCenterCosted(docs2move, _m);
                // cost = ( (Double) centerjcosted.elementAt(1)).doubleValue();
                docids2move.addAll(idocs2move);
                cost = _mstdeval.evalCluster(docids2move);
                _c.setQuick(cur_col3, cost);
                for (int j = 0; j < idocs2move.size(); j++) {
                  Integer jj = (Integer) idocs2move.elementAt(j);
                  _A.setQuick(jj.intValue(), cur_col, 0.0);
                  _A.setQuick(jj.intValue(), cur_col3, 1.0);
                }
              }
            }
          }
          cur_col++;
        }

        if (cur_col>=scpsolution.length) return;  // gone wild...
        _prevSolutions[cur_col]=1;  // the particular column as will be entered
                                    // is part of the latest solution we found
        A_col = _A.viewColumn(cur_col);
        A_col.assign(0);
        for (int j=0; j<clusteri.size(); j++) {
          Integer rowid = (Integer) clusteri.elementAt(j);
          _A.setQuick(rowid.intValue(), cur_col, 1);
        }
        Vector cidocs = new Vector();
        for (int l=0; l<clusteri.size(); l++) {
          Integer did = (Integer) clusteri.elementAt(l);
          Document dl = (Document) _docs.elementAt(did.intValue());
          cidocs.addElement(dl);
        }
        // Vector centerjcosted = Document.getCenterCosted(cidocs, _m);
        // double cost = ((Double) centerjcosted.elementAt(1)).doubleValue();
        double cost = _mstdeval.evalCluster(clusteri);
        _c.setQuick(cur_col, cost);
        cur_col++;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("addOverExistingColumns(): failed...");
    }
  }


  /**
   * expands the solution given by the asgns[] array by the factor given as the
   * second argument as follows: For Each cluster ci we do the following:
   * first we shrink ci's size up to factor points. This creates factor new clusters.
   * Then we expand ci, by adding up to factor closest docs that do not belong to
   * ci already. This creates another factor clusters. Thus, we return a
   * Vector<Vector<Integer docid> > which has size 2*factor*k where k is the
   * number of clusters required.
   * @param centers Vector<Document>
   * @param asgns int[]
   * @param factor int
   * @return Vector
   */
  public Vector expand(Vector centers, int[] asgns, int factor) throws PartitioningException {
    System.err.println("scpmaker1.expand(...,"+factor+") started.");
    Vector results = new Vector();
    Vector clusters = new Vector();
    // 0. compute clusters Vector<Vector<Integer docid> > from asgns.
    try {
      for (int i=0; i<centers.size(); i++) clusters.addElement(new Vector());
      for (int i=0; i<asgns.length; i++) {
        int c = asgns[i];
        Vector vc = (Vector) clusters.elementAt(c);
        vc.addElement(new Integer(i));
        clusters.set(c, vc);  // ensure addition
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("expand(): failed");
    }
    // 1. expand by factor docs
    for (int j=0; j<clusters.size(); j++) {
      Document cj = (Document) centers.elementAt(j);
      // find the factor closest points to cj other than the docs already in cj
      SCPExpandHelper ehelper = new SCPExpandHelper(_docs, cj, _m);
      Vector expanded = ehelper.getKNN(factor, asgns, j);
      // expanded is a Vector<Integer docid> showing the closest docs to
      // cj that don't belong to it.
      for (int i = 1; i <= factor; i++) {
        Vector aux = new Vector((Vector) clusters.elementAt(j));  // put in all the points already assigned to it
        for (int ii=0; ii<i; ii++) {
          aux.addElement(expanded.elementAt(ii));
        }
        results.addElement(aux);
      }
    }
    // 2. shrink by factor docs
    for (int j=0; j<clusters.size(); j++) {
      Document cj = (Document) centers.elementAt(j);
      // find among the docs belonging to cluster-j the factor most distant ones
      Vector cjindices = new Vector((Vector) clusters.elementAt(j));
      for (int i=0; i<factor; i++) {
        // find most distant object in cjindices from cj
        if (cjindices.size()<=2) break;  // removed many points from orig. cluster-j
        double rmax = 0.0;
        int max_ind = -1;
        for (int k=cjindices.size()-1; k>=0; k--) {
          int indk = ((Integer) cjindices.elementAt(k)).intValue();
          Document doc_k = (Document) _docs.elementAt(indk);
          try {
            double d = _m.dist(cj, doc_k);
            if (d > rmax) {
              rmax = d;
              max_ind = k;
            }
          }
          catch (clustering.ClustererException e) {
            e.printStackTrace();
            throw new PartitioningException("expand(): failed");
          }
        }
        if (max_ind>=0) {
          cjindices.remove(max_ind);
          if (cjindices.size()>1) results.addElement(new Vector(cjindices));  // add only sized clusters
        }
      }
    }
    System.err.println("scpmaker1.expand(...): added "+results.size()+" columns.");
    return results;
  }


  public void setParam(String param, Object val) {
    _params.put(param, val);
  }


  public Object getParam(String param) {
    return _params.get(param);
  }


  public void addParams(Hashtable params) {
    _params.putAll(params);
  }

}
