package packing;

import java.io.*;
import java.util.*;
import qs.*;
import coarsening.*;
import utils.*;

public class SCIPPacker {
  final static private double _eps = 1.e-8;
  private Graph _g;
  private int[] _x;
  private int _thres;  // how many cycles to add for each link in the constraint
                       // set
  public SCIPPacker(Graph g, int thres) {
    _g = g;
    _x = new int[_g.getNumNodes()];
    _thres = thres;
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
      Integer tI = null;
      Set constraintsets=null;
      if (_thres>0) {
        tI = new Integer(_thres);
        constraintsets = _g.getAllConnectedBy1Nodes(tI);
      } else constraintsets = _g.getAllNborSets();
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
      // lp.opt_primal();
      // if (lp.get_status()==QS.LP_OPTIMAL) {
      //   System.out.println("Optimal Value="+lp.get_objval());
      {
        // if (_x==null) _x = new double[n];
        // lp.get_x_array(_x);
        lp.write_lp("tmp.lp");
        // add the integrality constraints
        BufferedReader br = new BufferedReader(new FileReader("tmp.lp"));
        PrintWriter pw = new PrintWriter(new FileWriter("graph.lp"));
        while (true) {
          String line = br.readLine();
          if (line==null) {  // EOF reached
            pw.flush();
            pw.close();
            br.close();
            break;
          }
          if (line.equals("End")) {
            // insert integrality constraints now
            pw.println("Integer");
            for (int i=0; i<n; i++) {
              if (i % 10 == 0) pw.println("");
              pw.print(" x"+i);
            }
            pw.println("");
          }
          pw.println(line);
        }
        br.close();
        pw.flush();
        pw.close();
        // call scip
        exec("bin\\scip.exe -f graph.lp", _x);
      }
      // else throw new PackingException("LP not solved by QSOpt");
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PackingException("pack() failed...");
    }
  }


  public int[] getSolution() {
    return _x;
  }


  /**
   * Executes the MIP solver as a separate process
   */
  private int exec(String cmdLine, int[] solution) throws IOException {
    // zero-out solution
    for (int i=0; i<solution.length; i++) solution[i] = 0;
    int retCode = 1;              // Process return code
    BufferedReader in = null;
    Runtime rt = null;
    // String cmdLine = null;
    // Get a Runtime instance
    rt = Runtime.getRuntime();
    // Get the child process
    Process child = rt.exec(cmdLine);
    // Get input streams for the child process
    in = new BufferedReader(new InputStreamReader(child.getInputStream()));
    // Loop until the child process is finished.
    boolean finished = false;
    String inString;
    do {
      try {
        // Read any data that the child process has written to stdout.
        // This is necessary to prevent the child process from blocking.
        while (in.ready()) {
          inString = in.readLine();
          System.out.println(inString);
          // check to see if line is part of the solution
          if (inString.startsWith("x")) {
            StringTokenizer st = new StringTokenizer(inString);
            String varname = st.nextToken();
            String varvalue = st.nextToken();
            double val = Double.parseDouble(varvalue);
            if (Math.abs(val-1)>_eps) continue;  // ignore it
            // it's not a column that goes in the solution
            int setindex = Integer.parseInt(varname.substring(1));
            solution[setindex] = 1;
          }
        }
        // Attempt to get the exit code
        retCode = child.exitValue();
        finished = true;

        // If the process is not finished, an attempt to get the exit code
        // will throw the IllegalThreadStateException. Catch this and sleep for
        // 250 msec before trying again.
      } catch (IllegalThreadStateException e) {
        try {
          java.lang.Thread.currentThread().sleep(250);
        } catch (InterruptedException e1) {}
      }
    } while (!finished);
    return retCode;
  }


  public static int doRun(String filename, int thres) {
    try {
      Graph g = DataMgr.readGraphFromFile2(filename);
      SCIPPacker rp = new SCIPPacker(g, thres);
      rp.pack();
      int[] x = rp.getSolution();
      int total=0;
      for (int i=0; i<g.getNumNodes(); i++) {
        if (x[i]==1) {
          total++;
        }
      }
      return total;
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    return -1;
  }


  public static int doRun(String filename) {
    return doRun(filename,0);
  }


  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      String filename = args[0];
      int thres = 0;
      if (args.length>1) thres = Integer.parseInt(args[1]);
      Graph g = DataMgr.readGraphFromFile2(filename);
      SCIPPacker rp = new SCIPPacker(g,thres);
      rp.pack();
      int[] x = rp.getSolution();
      long time = (System.currentTimeMillis()-start)/1000;
      long total=0;
      for (int i=0; i<g.getNumNodes(); i++) {
        if (x[i]==1) {
          // System.out.println("x[" + i + "]=" + x[i] + " ");
          total++;
        }
      }
      System.err.println("Total Wall-Clock Time (secs): "+time);
      PrintWriter pw = new PrintWriter(new FileWriter("sol.out"));
      pw.println(total);
      // now write the indices of the solution
      for (int i=0; i<g.getNumNodes(); i++) {
        if (x[i]==1) pw.println((i+1));
      }
      pw.flush();
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
