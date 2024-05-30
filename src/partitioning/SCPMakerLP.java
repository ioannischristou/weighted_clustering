package partitioning;

import clustering.DocumentDistIntf;
import clustering.Document;
import clustering.ClustererException;
import utils.PairIntDouble;
import java.util.*;
import java.io.*;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;


/**
 * The class allows an LP relaxation of the SCP produced by SCPMaker1
 * to be converted to a valid clustering solution. The only difference from
 * the SCPMaker1 class is then in figuring out the columns to participate in
 * the solution, and also to figure out which points remain unassigned and then
 * to assign them to their nearest clusters.
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SCPMakerLP extends SCPMaker1 {
  public SCPMakerLP() {
    super();
  }

  /**
   * the argument passed in is an array of 0s and 1s of length _A.columns()
   * describing which column is contained in the solution of the SCP problem.
   * The output array is an array of length _A.rows() of integers, describing
   * cluster index assignments, in particular to which cluster [0...k-1] each
   * document belongs to.
   * Side effect: the _prevSolutions[] array is updated, so that we don't
   * overwrite columns that were previously part of the solution.
   * @param scpsol double[]
   * @throws PartitioningException
   * @return int[]
   */
  public int[] convertSolutionOld(double[] scpsol) throws PartitioningException {
    int min_col_sz = Integer.MAX_VALUE; int max_col_sz = 0;  // statistics
    try {
      int clusterindices[] = new int[_A.rows()];  // the return array
      for (int i=0; i<clusterindices.length; i++) clusterindices[i] = -1;  // init

      DoubleMatrix1D X = new DenseDoubleMatrix1D(scpsol.length);
      for (int i=0; i<X.size(); i++) {
        X.setQuick(i, scpsol[i]);
        if (scpsol[i]>0) {
          _prevSolutions[i]++;
          DoubleMatrix1D ai = _A.viewColumn(i);
          int sz = (int) ai.zSum();
          if (min_col_sz > sz) min_col_sz = sz;
          if (max_col_sz < sz) max_col_sz = sz;
        }
      }

      // figure out the _k larger values in the scpsol
      // itc: HERE notice that the method implemented below to get the
      // _k highest valued columns of the solution is not completely correct
      // in the event there are multiple columns with value equal to that
      // of the largest_val variable below, the for-loop stops once the first
      // _k columns with value larger than largest_val have been found.
      // This does not mean the _k columns with the highest values were picked,
      // and it may degrade quality when many variables are set to 0.5 from the
      // LP. The right way would be to create an array of Pair<col-ind,val> objects
      // and sort them according to their val, and then pick the _k top objects
      // and put them in the X vector.
      double[] aux = new double[scpsol.length];
      for (int i=0; i<aux.length; i++) aux[i] = scpsol[i];
      Arrays.sort(aux);
      double largest_val = aux[aux.length-_k];

      Hashtable Xindices = new Hashtable();  // map<Integer Xindex, Integer clusterindex>
      Hashtable Xcenters = new Hashtable();  // map<Integer Xindex, Document Xcenter>
      int index=0;

      int covered[] = new int[_docs.size()];
      for (int i=0; i<covered.length; i++) covered[i]=-1;
      // covered[i] indicates to which cluster in the orig. soln the point lies

      for (int i=0; i<X.size(); i++) {
        if (X.getQuick(i)>=largest_val && index<_k) {  // itc: HERE ensure up to k clusters only
          Xindices.put(new Integer(i), new Integer(index++));
          Vector Xdocs = new Vector();
          for (int j=0; j<_A.rows(); j++) {
            if (_A.getQuick(j,i)>0) {
              Xdocs.addElement(_docs.elementAt(j));
              covered[j] = i;
            }
          }
          Document center = Document.getCenter(Xdocs, null);  // itc-20220223: HERE
          Xcenters.put(new Integer(i), center);
          X.setQuick(i, 1.0);  // set index for X
        }
        else X.setQuick(i, 0.0);  // remove index from X
      }
      // now add to the existing clusters points that are not covered
      Hashtable cluster_sizes = new Hashtable();  // map<Integer Xind, Integer clustersize>
      for (int i=0; i<_docs.size(); i++) {
        if (covered[i]==-1) {
          Document di = (Document) _docs.elementAt(i);
          // figure out closest center and assign there
          double best_dist = Double.MAX_VALUE;
          int best_ind = -1;
          Iterator iter = Xcenters.keySet().iterator();
          while (iter.hasNext()) {
            Integer Xid = (Integer) iter.next();
            Document x_center = (Document) Xcenters.get(Xid);
            double dist = _m.dist(di, x_center);
            if (dist<best_dist) {
              best_dist = dist;
              best_ind = Xid.intValue();
            }
          }
          Document best_center = (Document) Xcenters.get(new Integer(best_ind));
          Document x_center2 = new Document(di);
          Integer nprimeI = (Integer) cluster_sizes.get(new Integer(best_ind));
          double nprime;
          if (nprimeI!=null) nprime = nprimeI.doubleValue();
          else nprime = _A.viewColumn(best_ind).zSum();
          x_center2.addMul(nprime, best_center);
          x_center2.div(nprime+1);
          cluster_sizes.put(new Integer(best_ind), new Integer((int)(nprime+1)));
          Xcenters.put(new Integer(best_ind), x_center2);
          covered[i] = best_ind;
          Integer cindexI = (Integer) Xindices.get(new Integer(best_ind));
          clusterindices[i] = cindexI.intValue();
        }
      }
      // convert the scpsolution to an sppsolution
      Hashtable duplicates = new Hashtable();  // map<Integer docid, Set<Integer column_id> >
      // 1. find out all duplicates
      for (int row=0; row<_A.rows(); row++) {
        DoubleMatrix1D A_r = _A.viewRow(row);
        double num_apps = A_r.zDotProduct(X);
        if (num_apps>1.0) {
          // we have duplicates
          Set dups = new HashSet();
          for (int col=0; col<_A.columns(); col++) {
            if (A_r.getQuick(col)==1.0 && X.getQuick(col)>=1.0) {
              dups.add(new Integer(col));
            }
          }
          duplicates.put(new Integer(row), dups);
        }
      }
      // 2. convert solution by removing duplicates, and then mapping solution
      // to a clusterIndices[] array
      for (int i=0; i<_A.columns(); i++) {  // itc: HERE iterate over the 1s of X
        if (X.getQuick(i)>0.0) {
          // Xi is in. Figure out which documents it contains
          DoubleMatrix1D col_i = _A.viewColumn(i);
          for (int j=0; j<col_i.size(); j++) {
            if (col_i.getQuick(j)>0) {
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

      // itc: HERE sanity check
      for (int i=0; i<clusterindices.length; i++) {
        if (clusterindices[i]<0 || clusterindices[i]>=_k)
          throw new PartitioningException("sanity check failed. clusterindices["+i+"]="+clusterindices[i]);
      }

      return clusterindices;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("convertSolution(): failed");
    }
  }


  /**
   * the argument passed in is an array of 0s and 1s of length _A.columns()
   * describing which column is contained in the solution of the SCP problem.
   * The output array is an array of length _A.rows() of integers, describing
   * cluster index assignments, in particular to which cluster [0...k-1] each
   * document belongs to.
   * Side effect: the _prevSolutions[] array is updated, so that we don't
   * overwrite columns that were previously part of the solution.
   * @param scpsol double[]
   * @throws PartitioningException
   * @return int[]
   */
  public int[] convertSolution(double[] scpsol) throws PartitioningException {
    int min_col_sz = Integer.MAX_VALUE; int max_col_sz = 0;  // statistics
    try {
      int clusterindices[] = new int[_A.rows()];  // the return array
      for (int i=0; i<clusterindices.length; i++) clusterindices[i] = -1;  // init

      DoubleMatrix1D X = new DenseDoubleMatrix1D(scpsol.length);
      for (int i=0; i<X.size(); i++) {
        X.setQuick(i, scpsol[i]);
        if (scpsol[i]>0) {
          _prevSolutions[i]++;
          DoubleMatrix1D ai = _A.viewColumn(i);
          int sz = (int) ai.zSum();
          if (min_col_sz > sz) min_col_sz = sz;
          if (max_col_sz < sz) max_col_sz = sz;
        }
      }

      PairIntDouble[] aux = new PairIntDouble[scpsol.length];
      for (int i=0; i<aux.length; i++) aux[i] = new PairIntDouble(i, scpsol[i]);
      Arrays.sort(aux);
      Set largest_vals_indices = new HashSet();
      for (int i=0; i<_k; i++)
        largest_vals_indices.add(new Integer(aux[aux.length-i-1].getInt()));

      Hashtable Xindices = new Hashtable();  // map<Integer Xindex, Integer clusterindex>
      Hashtable Xcenters = new Hashtable();  // map<Integer Xindex, Document Xcenter>
      int index=0;

      int covered[] = new int[_docs.size()];
      for (int i=0; i<covered.length; i++) covered[i]=-1;
      // covered[i] indicates to which cluster in the orig. soln the point lies

      for (int i=0; i<X.size(); i++) {
        if (largest_vals_indices.contains(new Integer(i))) {
          Xindices.put(new Integer(i), new Integer(index++));
          Vector Xdocs = new Vector();
          for (int j=0; j<_A.rows(); j++) {
            if (_A.getQuick(j,i)>0) {
              Xdocs.addElement(_docs.elementAt(j));
              covered[j] = i;
            }
          }
          Document center = Document.getCenter(Xdocs, null);  // itc-20220223: HERE
          Xcenters.put(new Integer(i), center);
          X.setQuick(i, 1.0);  // set index for X
        }
        else X.setQuick(i, 0.0);  // remove index from X
      }
      // now add to the existing clusters points that are not covered
      Hashtable cluster_sizes = new Hashtable();  // map<Integer Xind, Integer clustersize>
      for (int i=0; i<_docs.size(); i++) {
        if (covered[i]==-1) {
          Document di = (Document) _docs.elementAt(i);
          // figure out closest center and assign there
          double best_dist = Double.MAX_VALUE;
          int best_ind = -1;
          Iterator iter = Xcenters.keySet().iterator();
          while (iter.hasNext()) {
            Integer Xid = (Integer) iter.next();
            Document x_center = (Document) Xcenters.get(Xid);
            double dist = _m.dist(di, x_center);
            if (dist<best_dist) {
              best_dist = dist;
              best_ind = Xid.intValue();
            }
          }
          Document best_center = (Document) Xcenters.get(new Integer(best_ind));
          Document x_center2 = new Document(di);
          Integer nprimeI = (Integer) cluster_sizes.get(new Integer(best_ind));
          double nprime;
          if (nprimeI!=null) nprime = nprimeI.doubleValue();
          else nprime = _A.viewColumn(best_ind).zSum();
          x_center2.addMul(nprime, best_center);
          x_center2.div(nprime+1);
          cluster_sizes.put(new Integer(best_ind), new Integer((int)(nprime+1)));
          Xcenters.put(new Integer(best_ind), x_center2);
          covered[i] = best_ind;
          Integer cindexI = (Integer) Xindices.get(new Integer(best_ind));
          clusterindices[i] = cindexI.intValue();
        }
      }
      // compute the cluster_sizes for all clusters
      for (int i=0; i<_A.columns(); i++) {
        if (cluster_sizes.get(new Integer(i))==null) {
          int szi = (int) _A.viewColumn(i).zSum();
          cluster_sizes.put(new Integer(i), new Integer(szi));
        }
      }
      // convert the scpsolution to an sppsolution
      Hashtable duplicates = new Hashtable();  // map<Integer docid, Set<Integer column_id> >
      // 1. find out all duplicates
      for (int row=0; row<_A.rows(); row++) {
        DoubleMatrix1D A_r = _A.viewRow(row);
        double num_apps = A_r.zDotProduct(X);
        if (num_apps>1.0) {
          // we have duplicates
          Set dups = new HashSet();
          for (int col=0; col<_A.columns(); col++) {
            if (A_r.getQuick(col)==1.0 && X.getQuick(col)>=1.0) {
              dups.add(new Integer(col));
            }
          }
          duplicates.put(new Integer(row), dups);
        }
      }
      // 2. convert solution by removing duplicates, and then mapping solution
      // to a clusterIndices[] array
      for (int i=0; i<_A.columns(); i++) {  // itc: HERE iterate over the 1s of X
        if (X.getQuick(i)>0.0) {
          // Xi is in. Figure out which documents it contains
          DoubleMatrix1D col_i = _A.viewColumn(i);
          for (int j=0; j<col_i.size(); j++) {
            if (col_i.getQuick(j)>0) {
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
                // is closest to document j, unless there is a cluster with only
                // this point, in which case, we assign it to this cluster.
                // This may still not be enough to alleviate cluster dissapearances
                // though.
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
                  // check if col_id has only one point
                  Integer col_id_sz = (Integer) cluster_sizes.get(col_id);
                  if (col_id_sz.intValue()==1) {
                    bestcol = col_id;
                    bestcost = -1;
                    break;
                  }
                }
                clusterindices[j] = ((Integer) Xindices.get(bestcol)).intValue();
                // itc: HERE new heuristic
                // update Xcenters containing dj to indicate removal of bestcol from X column
                // update cluster_sizes as well
                Document d = (Document) _docs.elementAt(j);
                it = dups.iterator();
                while (it.hasNext()) {
                  Integer col_id = (Integer) it.next();
                  int col_size=((Integer) cluster_sizes.get(col_id)).intValue();
                  if (col_id.intValue()!=bestcol.intValue()) {
                    cluster_sizes.put(col_id, new Integer(col_size-1));
                  }
                  Document col_center = (Document) Xcenters.get(col_id);
                  if (col_size<2) continue;  // should not happen
                  double cs1 = (double) (col_size-1) / (double) col_size;
                  col_center.div(cs1);
                  col_center.addMul(-1.0/((double)(col_size-1)), d);
                  Xcenters.put(col_id, col_center);
                }
                // done updating Xcenters and cluster_sizes
              }
            }
          }
        }
      }

      // itc: HERE sanity check
      for (int i=0; i<clusterindices.length; i++) {
        if (clusterindices[i]<0 || clusterindices[i]>=_k)
          throw new PartitioningException("sanity check failed. clusterindices["+i+"]="+clusterindices[i]);
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
   * It also modifies some columns not in the solution
   * @param clusters Vector<Vector<Integer docid> cluster>
   * @param scpsolution int[]
   * @throws PartitioningException
   */
  public void addOverExistingColumns(Vector clusters,
                                     double[] scpsolution)
      throws PartitioningException {
    try {
      int cur_col = 0;
      if (clusters==null) return;

      // figure out the _k larger values in the scpsol
      double[] aux = new double[scpsolution.length];
      for (int i=0; i<aux.length; i++) aux[i] = scpsolution[i];
      Arrays.sort(aux);
      double largest_val = aux[aux.length-_k];

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
        while (cur_col<scpsolution.length && scpsolution[cur_col]<largest_val) {
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
                if (cur_col == cur_col3 || scpsolution[cur_col3] >= largest_val)
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
                Vector centerjcosted = Document.getCenterCosted(cidocs, _m, null);  // // itc-20220223: HERE
                double cost = ( (Double) centerjcosted.elementAt(1)).
                    doubleValue();
                _c.setQuick(cur_col, cost);
                for (int j = 0; j < _A.rows(); j++) {
                  if (_A.getQuick(j, cur_col3) > 0.0) {
                    Document dj = (Document) _docs.elementAt(j);
                    docs2move.addElement(dj);
                  }
                }
                centerjcosted = Document.getCenterCosted(docs2move, _m, null);  // itc-20220223: HERE
                cost = ( (Double) centerjcosted.elementAt(1)).doubleValue();
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
}
