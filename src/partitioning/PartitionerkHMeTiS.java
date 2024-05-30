package partitioning;

import coarsening.*;
import java.util.*;
import java.io.*;

public class PartitionerkHMeTiS implements Partitioner {
  public PartitionerkHMeTiS() {
  }


  public void setObjectiveFunction(ObjFnc f) {
    // no-op
  }


  /**
   * g is expected to be null. However props must contain at least the following
   * attributes:
   *
   * numnodes Integer
   * partitionerpath String
   * partitionername String
   * graphfilename String
   *
   * @param g Graph
   * @param k int
   * @param props Hashtable
   * @return int[]
   */
  public int[] partition(Graph g, int k, Hashtable props) {
    int numnodes = ((Integer) props.get("numnodes")).intValue();
    int partition[] = new int[numnodes];
    // call HMeTiS
    String partitionerpath = (String) props.get("partitionerpath");
    String partitionername = (String) props.get("partitionername");
    String graphfile = (String) props.get("graphfilename");
    int numtries = 20;  // default value
    Integer numtriesI = (Integer) props.get("numtries");
    if (numtriesI!=null) numtries = numtriesI.intValue();
    String cmdline = partitionerpath+"/"+partitionername+" "+
                     partitionerpath+"/"+graphfile+" "+k+" 155 "+numtries+
                     " 1 2 1 0";
    System.err.println("kHMeTiS cmd line is: "+cmdline);  // itc: HERE rm asap
    try {
      exec(cmdline);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    String outfilename = partitionerpath+"/"+graphfile+".part."+k;
    utils.DataMgr.readPartitionFromHGROutFile(outfilename, partition);
    // delete files
    File f = new File(partitionerpath+"/"+graphfile);
    f.deleteOnExit();
    File f2 = new File(outfilename);
    f2.deleteOnExit();
    return partition;
  }


   /**
    * Executes as a separate thread
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
