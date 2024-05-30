package partitioning;

import coarsening.*;
import utils.DataMgr;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.*;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * this class should be used when commercial optimizers such as Gurobi are
 * used, with a cmdline of the form:
 * %>gurobi_cl ResultFile=\\tmp\\sol.txt input.mps
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HPartitionerMLSPP2 implements HPartitioner {
  private final double _eps = 1.e-6;
  private HObjFncIntf _obj=null;

  public HPartitionerMLSPP2() {
  }


  public void setObjectiveFunction(HObjFncIntf f) {
    _obj = f;
  }


  public int[] partition(HGraph g, int k, Hashtable props) throws PartitioningException {
    int max_acc_graph_size = ((Integer) props.get("partitioning.HPartitioner.HPartitionerMLSPP.max_acc_graph_size")).intValue();
    if (_obj==null) {
      HObjFncIntf obj = (HObjFncIntf) props.get("partitioning.hpartrunner.objfnc");
      if (obj==null) obj = new HObjFncSOED();
      _obj = obj;
    }

    // 1. Coarsen HGraph g
    HCoarsener co = new HCoarsenerIEC(g, null, props);
    Vector coarseners = new Vector();  // Vector<Coarsener>, last one is the
    // coarsener used to create last graph
    int num_iters = 0;
    while (g.getNumNodes()>max_acc_graph_size) {
      ++num_iters;
      System.err.println("Entering coarsening level "+num_iters);
      try {
        co.coarsen();
        HGraph coarse_g = co.getCoarseHGraph();
        System.err.println("coarse_g.size="+coarse_g.getNumNodes()+" #arcs="+coarse_g.getNumArcs());
        // Hashtable props = co.getProperties();
        coarseners.addElement(co);

        HCoarsener co1 = co.newInstance(coarse_g, null, props);
        co = co1;
        g = coarse_g;
      }
      catch (Exception e) {
        e.printStackTrace();
        // remove this coarsener from the coarseners as it didn't work
        // coarseners.remove(coarseners.size()-1);  // itc: HERE the coarsener didn't enter the vector so it must not be rm'ed
        break;
      }
    }
    // itc: HERE rm asap
    try {
      DataMgr.writeWeightedHGraphDirectToHGRFile(g, "coarse_g.hrg");
    }
    catch (IOException e) {
      e.printStackTrace();
      throw new PartitioningException("io problem");
    }

    // 2. Partition coarsest graph using SPP approach
    HPartitioner parter = (HPartitioner) props.get("partitioning.HPartitioner.HPartitionerMLSPP.basepartitioner");
    int num_tries = 1;
    Integer num_triesI = (Integer) props.get("partitioning.HPartitioner.HPartitionerMLSPP.num_tries");
    if (num_triesI!=null) num_tries = num_triesI.intValue();
    int[] partition = new int[g.getNumNodes()];  // init to zeros
    Vector parts = new Vector();  // Vector<int[] partition>
    for (int i=0; i<num_tries; i++) {
      int[] partitioni = parter.partition(g, k, props);
      parts.addElement(partitioni);
    }
    SPPMakerIntf sppmaker = (SPPMakerIntf) props.get("partitioning.HPartitioner.HPartitionerMLSPP.sppmaker");
    sppmaker.createSPP(g, parts, k, new HObjFncSOED());
    String filename = (String) props.get("partitioning.HPartitioner.HPartitionerMLSPP.sppfilename");
    if (filename==null) filename="spp.mps";
    String cmdname = (String) props.get("partitioning.HPartitioner.HPartitionerMLSPP.solverfullpathname");
    try {
      DataMgr.writeSPP2MPSFileFast(sppmaker.getConstraintsMatrix(), sppmaker.getCostVector(), k, filename);
      String cmdLine = cmdname + " ResultFile=C:\\Temp\\"+filename+".sol " + filename;  // gurobi-specific
      int[] chosenblocks = new int[parts.size()*k];
      exec(cmdLine, chosenblocks);
      // get the partition from the chosenblocks
      int p = 0;
      DoubleMatrix2D A = sppmaker.getConstraintsMatrix();
      for (int j=0; j<chosenblocks.length; j++) {
        if (chosenblocks[j]==1) {
          p++;  // the partition number for the chosen block
          for (int i=0; i<A.rows(); i++) {
            if (A.getQuick(i,j)>=1.0) {
              partition[i]=p;
            }
          }
        }
      }
    }
    catch (java.io.IOException e) {
      e.printStackTrace();
      throw new PartitioningException("io problem");
    }
    // sanity check for partitioner
    int[] pszes = new int[k];
    for (int i=0; i<k; i++) pszes[i]=0;
    for (int i=0; i<g.getNumNodes(); i++) {
      pszes[partition[i]-1]++;
    }
    for (int i=0; i<k; i++) {
      if (pszes[i]==0) {
        System.err.println("HGraph g="+g);
        throw new PartitioningException("Partitioner failed...");
      }
    }
    if (_obj!=null) {
      double val = _obj.value(g, partition);
      System.out.println("partition value="+val);
    }

    // 3. Roll-up, refining the partition at each level
    HPartitioner refiner = (HPartitioner) props.get("partitioning.HPartitioner.HPartitionerMLSPP.refiner");
    refiner.setObjectiveFunction(_obj);
    for (int i=coarseners.size()-1; i>=0; i--) {
      System.err.println("CDC coarsening level "+i);
      HCoarsener ci = (HCoarsener) coarseners.elementAt(i);
      HGraph gf = ci.getOriginalHGraph();
      try {
        int[] fpartition = ci.getFinePartition(partition);
        props.remove("initialpartition");
        props.put("initialpartition",fpartition);
        partition = refiner.partition(gf, k, props);
        if (_obj!=null) {
          double val = _obj.value(gf, partition);
          System.out.println("partition value="+val);
        }
      }
      catch (CoarsenerException e) {
        e.printStackTrace();
        throw new PartitioningException("error getting fine-grain partition from coarse one");
      }
    }

    return partition;
  }


  /**
   * Executes the MIP solver as a separate process
   */
  private int exec(String cmdLine, int[] scpsolution) throws IOException {
    // zero-out scpsolution
    for (int i=0; i<scpsolution.length; i++) scpsolution[i] = 0;
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
          System.err.println(inString);  // print out the optimizer output
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
    // now read the result file
    StringTokenizer st=new StringTokenizer(cmdLine," ");
    String optname=st.nextToken();
    while (st.hasMoreTokens()) {
      String tok=st.nextToken();
      if (tok.startsWith("ResultFile")) {
        //int ind=tok.lastIndexOf('=');
        String filename = tok.substring(11);
        // read the solution file
        BufferedReader br = new BufferedReader(new FileReader(filename));
        if (br.ready()) {
          while (true) {
            String line = br.readLine();
            if (line==null) break;  // end-of-file
            if (line.startsWith("X")) {
              StringTokenizer st2 = new StringTokenizer(line, " \t");
              String varname = st2.nextToken();
              double value = Double.parseDouble(st2.nextToken());
              int ivalue = (int) Math.round(value);
              int setindex = Integer.parseInt(varname.substring(1));
              scpsolution[setindex] = ivalue;
            }
          }
        }
        br.close();
        break;  // out of the read-cmd-line-options loop
      }
    }
    return retCode;
  }

}
