package partitioning;

import coarsening.*;
import java.util.*;
import java.io.*;

public class HPartitionerHMeTiS implements HPartitioner {
  public HPartitionerHMeTiS() {
  }


  public void setObjectiveFunction(HObjFncIntf f) {
    // no-op
  }


  public int[] partition(HGraph g, int k, Hashtable props) throws PartitioningException {
    int partition[] = new int[g.getNumNodes()];
    // call HMeTiS
    String partitionerpath = (String) props.get("partitioning.HPartitioner.HPartitionerHMeTiS.partitionerpath");
    String partitionername = (String) props.get("partitioning.HPartitioner.HPartitionerHMeTiS.partitionername");
    if (partitionername==null) partitionername="khmetis.exe";
    String graphfile = (String) props.get("partitioning.HPartitioner.HPartitionerHMeTiS.graphfilename");
    if (graphfile==null) graphfile="tmpgraph.hgr";
    int numtries = 10;  // default value
    Integer numtriesI = (Integer) props.get("partitioning.HPartitioner.HPartitionerHMeTiS.numtries");
    if (numtriesI!=null) numtries = numtriesI.intValue();
    int ubfactor = 5;  // default UBFactor value
    Integer devpercI = (Integer) props.get("partitioning.HPartitioner.partitionubfactor");
    if (devpercI!=null) ubfactor = devpercI.intValue();
    // compute b the UB factor
    //double b = 50 - 100*Math.pow(devperc, Math.log(2.0)/Math.log(k));
    if (ubfactor<1 || ubfactor>=50)
      throw new PartitioningException("ubfactor="+ubfactor+" cannot use it in hmetis");
    // ensure graphfile is built
    String fullgraphname = partitionerpath+"/"+graphfile;
    File f = new File(fullgraphname);
    // if (f.exists()==false) {
    if (true) {
      try {
        System.err.println("HMeTiS: creating graph...");  // itc: HERE rm asap
        utils.DataMgr.writeWeightedHGraphDirectToHGRFile(g, fullgraphname);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    String cmdline = partitionerpath+"/"+partitionername+" "+
                     fullgraphname+" "+k+" "+ubfactor+" "+numtries;
    if (partitionername.equals("hmetis.exe")) cmdline += " 1 1 0 0 0";
    else cmdline += " 1 2 0 0";  // this is for khmetis
    System.err.println("HMeTiS cmd line is: "+cmdline);  // itc: HERE rm asap
    try {
      exec(cmdline);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("HMeTiS partitioner failed");
    }
    String outfilename = partitionerpath+"/"+graphfile+".part."+k;
    utils.DataMgr.readPartitionFromHGROutFile(outfilename, partition);
    // delete files
    f.delete();
    // f.deleteOnExit();
    File f2 = new File(outfilename);
    f2.delete();
    // f2.deleteOnExit();
    return partition;
  }


   /**
    * Executes the cmdLine as a separate thread
    */
   int exec(String cmdLine) throws IOException {
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

}
