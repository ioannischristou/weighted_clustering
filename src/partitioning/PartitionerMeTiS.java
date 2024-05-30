package partitioning;

import coarsening.*;
import java.util.*;
import java.io.*;

public class PartitionerMeTiS implements Partitioner {
  public PartitionerMeTiS() {
  }


  public void setObjectiveFunction(ObjFnc f) {
    // no-op
  }


  public int[] partition(Graph g, int k, Hashtable props) {
    int partition[] = new int[g.getNumNodes()];
    // call HMeTiS
    String partitionerpath = (String) props.get("partitionerpath");
    String partitionername = (String) props.get("partitionername");
    String graphfile = (String) props.get("graphfilename");
    String cmdline = partitionerpath+"/"+partitionername+" "+
                     partitionerpath+"/"+graphfile+" "+k;
    System.err.println("HMeTiS cmd line is: "+cmdline);  // itc: HERE rm asap
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
    boolean f_del = f.delete();
    if (!f_del) {
      System.err.println("PartitionerMetis.partition(): failed to delete file...");
    }
    File f2 = new File(outfilename);
    boolean f2_del = f2.delete();
    if (!f2_del) {
      System.err.println("PartitionerMetis.partition(): failed to delete file...");
    }
    return partition;
  }


   /**
    * Executes the backup applications as a separate thread
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
