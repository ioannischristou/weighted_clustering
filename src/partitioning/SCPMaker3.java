package partitioning;

import clustering.DocumentDistIntf;
import clustering.Document;
import clustering.ClustererException;
import java.util.*;
import java.io.*;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

public class SCPMaker3 implements SCPMakerIntf {
  private Vector _docs;
  private int _k;
  private double _theta;
  private int _maxSetCard;
  private Hashtable _params;  // manipulated via set/get/addParams()
  private DoubleMatrix2D _A;
  private DoubleMatrix1D _c;
  private DocumentDistIntf _m;
  private int[] _prevSolutions;  // _prevSolutions[i] is the number of times
                                 // the i-th column has been part of a
                                 // clustering solution. Of course, columns
                                 // may enter and leave the problem...

  public SCPMaker3() {
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
      final int clusters_per_doc1 = (int) Math.ceil(1.25*c_s / _theta+1.0);  // itc: HERE factor used to be 2.0
      int clusters_per_doc = clusters_per_doc1 > _maxSetCard ? clusters_per_doc1 : _maxSetCard;
      // create _A and _c
      int rows = _docs.size();
      int cols = (clusters_per_doc + 2) * docsize;
      if (clusters!=null) cols += clusters.size();
      _A = new SparseDoubleMatrix2D(rows, cols);
      _c = new DenseDoubleMatrix1D(cols);
      System.err.println("Matrix _A["+rows+","+cols+"] constructed");
      // populate _A and _c
      SCPMakerHelper helper = new SCPMakerHelper(_docs, _m);
      int column_counter = 0;
      for (int i = 0; i < docsize && column_counter < _A.columns(); i++) {
        System.err.println("constructing columns for d"+i+"...");
        final Document di = (Document) _docs.elementAt(i);
        // put in di alone in a column
        _A.setQuick(i, column_counter, 1.0);
        _c.setQuick(column_counter, 0.0);
        column_counter++;
        // now start putting clusters of elements in multiples of theta
        int to = 0;
        Vector nnborids=null;
        Vector nnbors = new Vector();
        Document max_cluster_center = null;
        int j;
        theta = _theta;
        double cost = -1;
        for (j = 1; j <= clusters_per_doc && column_counter < _A.columns(); j++) {
          // heuristic: for the last clusters, boost up theta so as to have
          // some columns with larger coverage
          if (j==clusters_per_doc-10) theta *= 2;
          else if (j==clusters_per_doc-5) theta *= 3;
          else if (j==clusters_per_doc-2) theta *= 4;
          // end boosting heuristics
          // first 10 elements are always in 1-by-1
          if (j<=10) to++;  // to += 1;
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
          Vector r = Document.getCenterCosted(nnbors, _m, null);  // itc-20220223: HERE
          cost = ((Double) r.elementAt(1)).doubleValue();
          max_cluster_center = (Document) r.elementAt(0);
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
        Vector r = Document.getCenterCosted(nnbors, _m, null);  // itc-20220223: HERE
        cost = ((Double) r.elementAt(1)).doubleValue();
        max_cluster_center = (Document) r.elementAt(0);
        _c.setQuick(column_counter, cost);
        column_counter++;
        // end heuristics
      }
      // put in the columns described in the clusters Vector
      if (clusters!=null) {
        for (int j=0; j<clusters.size(); j++) {
          Vector clusterj = (Vector) clusters.elementAt(j);  // Vector<Integer docid>
          for (int jj=0; jj<clusterj.size(); jj++) {
          Integer docid = (Integer) clusterj.elementAt(jj);
            _A.setQuick(docid.intValue(), column_counter, 1);
          }
          Vector cjdocs = new Vector();
          for (int l=0; l<clusterj.size(); l++) {
            Integer did = (Integer) clusterj.elementAt(l);
            Document dl = (Document) _docs.elementAt(did.intValue());
            cjdocs.addElement(dl);
          }
          Vector centerjcosted = Document.getCenterCosted(cjdocs, _m, null);  // itc-20220223: HERE
          double cost = ((Double) centerjcosted.elementAt(1)).doubleValue();
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


  public int[] convertSolution(double[] scpsol) throws PartitioningException {
    throw new PartitioningException("method not supported");
  }


  public void addOverExistingColumns(Vector clusters,
                                     double[] scpsolution)
      throws PartitioningException {
    throw new PartitioningException("method not supported");
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
    try {
      DoubleMatrix1D X = new DenseDoubleMatrix1D(scpsol.length);
      for (int i=0; i<X.size(); i++) {
        X.setQuick(i, (double) scpsol[i]);
        if (scpsol[i]==1) _prevSolutions[i]++;
      }
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


  /**
   * adds the specified clusters as solutions in the matrix _A and the cost vector
   * _c. Leaves the columns described in scpsolution intact, as they are part of
   * the solution found before.
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
      for (int i=0; i<clusters.size(); i++) {
        Vector clusteri = (Vector) clusters.elementAt(i);
        DoubleMatrix1D A_col=null;
        boolean cont = true;
        while (cont && cur_col<scpsolution.length) {
          cont = scpsolution[cur_col]==1 || _prevSolutions[cur_col]>0;
          if (!cont) {
            A_col = _A.viewColumn(cur_col);
            int asz = (int) A_col.zSum();
            if (asz<((double)_docs.size())/(((double) _k)*_theta)) cont = true;
          }
          if (cont) cur_col++;
        }
        if (cur_col>=scpsolution.length) return;  // gone wild...
        _prevSolutions[cur_col]=1;  // the particular column as will be entered
                                    // is part of the latest solution we found
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
        Vector centerjcosted = Document.getCenterCosted(cidocs, _m, null);  // itc-20220223: HERE
        double cost = ((Double) centerjcosted.elementAt(1)).doubleValue();
        _c.setQuick(cur_col, cost);
        cur_col++;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("addOverExistingColumns(): failed...");
    }
  }


  public Vector expand(Vector centers, int asngmts[], int expansionsize) throws PartitioningException {
    throw new PartitioningException("method not implemented");
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
