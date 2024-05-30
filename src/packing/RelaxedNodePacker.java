package packing;

import java.io.*;
import java.util.*;
import qs.*;
import coarsening.*;
import utils.*;

public class RelaxedNodePacker {
  private Graph _g;
  private double[] _x;
  private Integer _mxcnt=null;
  private Set _constraintsets=null;
  int _cmatcnt[] = null;
  int _cmatbeg[] = null;
  int _cmatind[] = null;
  double _cmatval[] = null;
  String _names[] = null;
  String _rnames[] = null;
  int _zero[] = null;
  char _sense[] = null;
  double _rhs[] = null;
  boolean _isRunning = false;


  public RelaxedNodePacker(Graph g) {
    _g = g;
    _x = new double[_g.getNumNodes()];
  }


  public RelaxedNodePacker(Graph g, Integer maxcount) {
    _g = g;
    _x = new double[_g.getNumNodes()];
    _mxcnt = maxcount;
  }


  public boolean isRunning() {
    return _isRunning;
  }


  /**
   * the method solves the following LP problem defined on a Graph G(V,E):
   *
   * max x1+...+xn
   * s.t.
   * sum_{j \in N(i)} x_j <= 1 forall i \in V
   * x_j >= 0 forall j \in V
   * x_k = 1 forall k \in setnodeids
   * where the set N(i) is a maximal subset of nodes containing i so that all
   * nodes i, j in N(i) have d(i,j) <= 2.
   * @param Set setnodeids
   * @return double the optimal LP value
   */
  public synchronized double getPackingLPBound(Set setnodeids) throws PackingException {
    try {
      System.err.println("getPackingLPBound(): started");  // itc: HERE rm asap
      _isRunning = true;
      if (_constraintsets==null) {
        // _constraintsets = _g.getAllConnectedBy1Nodes(_mxcnt);
        _constraintsets = _g.getAllNborSets();  // itc: HERE rm asap
        System.err.println("total number of constraints in set: "+_constraintsets.size());  // itc: HERE rm asap
        final int n = _g.getNumNodes();
        _names = new String[n];
        for (int i=0; i<n; i++) {
          _names[i] = "x"+(new Integer(i)).toString();
        }
        int rows = _constraintsets.size();
        Set[] constraints = new Set[rows];
        Iterator iter = _constraintsets.iterator();
        int k=0;
        int l = 0;
        while (iter.hasNext()) {
          Set nodes = (Set) iter.next();
          // nodes defines a constraint on the problem
          constraints[k++] = nodes;
          l+=nodes.size();
        }
        // now the constraints array contains the constraints of the problem
        _cmatcnt = new int[n];
        _cmatbeg = new int[n];
        _cmatind = new int[l];
        _cmatval = new double[l];
        k=0;
        int p=0;
        _cmatbeg[0]=0;
        for (int j=0; j<n; j++) {
          if (j>0) _cmatbeg[j] = _cmatbeg[j-1]+p;
          p=0;
          for (int i=0; i<rows; i++) {
            if (constraints[i].contains(new Integer(j))) {  // xj appears in row-i
              _cmatcnt[j]++;
              _cmatind[k] = i;
              _cmatval[k] = 1;
              k++; p++;
            }
          }
        }
        _zero = new int[rows];
        _sense = new char[rows];
        for (int i=0; i<rows; i++) _sense[i]='L';
        _rhs = new double[rows];
        _rnames = new String[rows];
        for (int i=0; i<rows; i++) {
          _rhs[i] = 1;
          _rnames[i] = "R"+i;
        }
      }

      Problem lp = new Problem("lppacker");
      lp.change_objsense(QS.MAX);
      final int n = _g.getNumNodes();
      double[] lower = new double[n];
      double[] upper = new double[n];
      double[] obj = new double[n];
      for (int i=0; i<n; i++) {
        if (setnodeids.contains(new Integer(i))) lower[i]=1;
        else lower[i]=0;
        upper[i]=1;
        obj[i]=1;
      }
      int rows = _constraintsets.size();
      lp.add_rows(rows, _zero, _zero, null, null, _rhs, _sense, _rnames);
      lp.add_cols(n, _cmatcnt, _cmatbeg, _cmatind, _cmatval, obj, lower, upper, _names);
      //System.err.println("running qsopt.opt_primal()");
      lp.opt_primal();
      if (lp.get_status()==QS.LP_OPTIMAL) {
        //System.err.println("LP Optimal Value="+lp.get_objval());
        return lp.get_objval();
      }
      else throw new PackingException("LP not solved by QSOpt");
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PackingException("pack() failed...");
    }
    finally {
      _isRunning = false;
      System.err.println("getPackingLPBound(): ended");  // itc: HERE rm asap
    }
  }


  /**
   * the method sets up the following LP problem defined on a Graph G(V,E):
   *
   * max x1+...+xn
   * s.t.
   * sum_{j \in N(i)} x_j <= 1 forall i \in V
   * x_j >= 0 forall j \in V
   *
   * where the set N(i) is a maximal subset of nodes containing i so that all
   * nodes i, j in N(i) have d(i,j) <= 2.
   */
  public void pack() throws PackingException {
    try {
      Problem lp = new Problem("lppacker");
      lp.change_objsense(QS.MAX);
      final int n = _g.getNumNodes();
      String[] names = new String[n];
      for (int i=0; i<n; i++) {
        names[i] = "x"+(new Integer(i)).toString();
      }
      double[] lower = new double[n];
      double[] upper = new double[n];
      double[] obj = new double[n];
      for (int i=0; i<n; i++) {
        lower[i]=0;
        upper[i]=1;
        obj[i]=1;
      }
      Set constraintsets = _g.getAllConnectedBy1Nodes(_mxcnt);
      System.err.println("total number of constraints in set: "+constraintsets.size());  // itc: HERE rm asap
      // Set constraintsets = _g.getAllNborSets();  // itc: HERE rm asap
      int rows = constraintsets.size();
      Set[] constraints = new Set[rows];
      Iterator iter = constraintsets.iterator();
      int k=0;
      int l = 0;
      while (iter.hasNext()) {
        Set nodes = (Set) iter.next();
        // nodes defines a constraint on the problem
        constraints[k++] = nodes;
        l+=nodes.size();
      }
      // now the constraints array contains the constraints of the problem
      int cmatcnt[] = new int[n];
      int cmatbeg[] = new int[n];
      int cmatind[] = new int[l];
      double cmatval[] = new double[l];

      k=0;
      int p=0;
      cmatbeg[0]=0;
      for (int j=0; j<n; j++) {
        if (j>0) cmatbeg[j] = cmatbeg[j-1]+p;
        p=0;
        for (int i=0; i<rows; i++) {
          if (constraints[i].contains(new Integer(j))) {  // xj appears in row-i
            cmatcnt[j]++;
            cmatind[k] = i;
            cmatval[k] = 1;
            k++; p++;
          }
        }
      }
      int[] zero = new int[rows];
      char[] sense = new char[rows];
      for (int i=0; i<rows; i++) sense[i]='L';
      double[] rhs = new double[rows];
      String[] rnames = new String[rows];
      for (int i=0; i<rows; i++) {
        rhs[i] = 1;
        rnames[i] = "R"+i;
      }
      lp.add_rows(rows, zero, zero, null, null, rhs, sense, rnames);
      lp.add_cols(n, cmatcnt, cmatbeg, cmatind, cmatval, obj, lower, upper, names);
      System.err.println("running qsopt.opt_primal()");
      lp.opt_primal();
      if (lp.get_status()==QS.LP_OPTIMAL) {
        System.out.println("Optimal Value="+lp.get_objval());
        if (_x==null) _x = new double[n];
        lp.get_x_array(_x);
      }
      else throw new PackingException("LP not solved by QSOpt");
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PackingException("pack() failed...");
    }
  }


  public double[] getSolution() {
    return _x;
  }


  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      String filename = args[0];
      Integer mc = null;
      if (args.length>1) mc = new Integer(args[1]);
      Graph g = DataMgr.readGraphFromFile2(filename);
      RelaxedNodePacker rp = new RelaxedNodePacker(g,mc);
      rp.pack();
      double[] x = rp.getSolution();
      long time = (System.currentTimeMillis()-start);
      double s = 0;
      for (int i=0; i<g.getNumNodes(); i++) {
        // System.out.println("x["+i+"]="+x[i]+" ");
        // if (i % 5 == 0) System.out.println("");
        s += x[i];
      }
      System.out.println("max obj val="+s);
      System.err.println("Total Wall-Clock Time (msecs): "+time);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
