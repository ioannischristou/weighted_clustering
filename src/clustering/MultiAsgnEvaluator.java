package clustering;

import java.util.*;
import java.io.*;

public class MultiAsgnEvaluator extends EnhancedEvaluator {
  public MultiAsgnEvaluator() {
  }

  public double evalCluster(Vector docs) throws ClustererException {
    throw new ClustererException("not implemented");
  }
  public double eval(Vector docs, int[] inds) throws ClustererException {
    throw new ClustererException("not implemented");
  }
  public void setParams(Hashtable p) {
    return;  // not implemented
  }


  /**
   * compute the Best (minimal) weight bipartite matching between the
   * clusterer's clustering and the true clusters that must have been provided
   * as a parameter to the clusterer as well, under the attribute key "trueclusters"
   * Uses the Network Simplex Method to do the multi-assignment properly.
   * The NSM3 implementation of I.T. Christou is used.
   * @param cl Clusterer
   * @throws ClustererException
   * @return double
   */
  public double eval(Clusterer cl) throws ClustererException {
    double res=0.0;
    Hashtable params = cl.getParams();
    int trueclusterindices[] = (int[]) params.get("trueclusters");
    int clusterindices[] = cl.getClusteringIndices();
    final int num_clusters = cl.getCurrentCenters().size();
    final int k = ((Integer) params.get("k")).intValue();  // true number of classes, or clusters
    final int n = cl.getCurrentDocs().size();
    final String nsmcmd = (String) params.get("nsmcmd");
    // final Vector docs = cl.getCurrentDocs();
    // compute a[i,j] - the number of mislabelings when cluster i is assigned
    // the true label j.
    int a[][] = new int[k][num_clusters];  // a has dimensions #rows=k, #cols=num_clusters
    for (int i=0; i<num_clusters; i++) {  // cluster index i
      for (int j=0; j<k; j++) {  // true label j
        a[j][i]=0;
        for (int m=0; m<n; m++) {
          if (clusterindices[m]==i && trueclusterindices[m]!=j)
            a[j][i]++;  // a[j][i] represents the number of errors we make
                        // if we assign cluster i to truecluster j.
        }
      }
    }
    // prepare the input to NSM3
    String tmpfile="asgn.dat";
    try {
      PrintWriter pw = new PrintWriter(new FileOutputStream(tmpfile));
      pw.println((num_clusters+k+1));  // #nodes
      pw.println((num_clusters+1)*k);  // #arcs
      for (int i=0; i<num_clusters; i++) // node-demands
        pw.println("1");
      for (int i=0; i<k; i++)
        pw.println("-1");
      pw.println((k-num_clusters));
      for (int i=0; i<num_clusters; i++) {  // arcs-costs+capacities
        for (int j=0; j<k; j++) {
          pw.println((i+1)+" "+(j+1+num_clusters)+" "+a[j][i]+" -1");
        }
      }
      for (int i=0; i<k; i++)
        pw.println((num_clusters+1+i)+" "+(num_clusters+k+1)+" 0 -1");
      pw.println(1);  // dlevel: used to be zero, but I need the X values too.
      pw.close();
      // String cmdline = "java -cp . Hungarian "+k+" "+k+" "+tmpfile;
      String cmdline = nsmcmd + " " + tmpfile;
      Vector valv = new Vector();
      exec(cmdline, num_clusters, k, clusterindices, trueclusterindices, valv);
      double val = ((Double) valv.elementAt(0)).doubleValue();
      return val/(double) n;  // returns the min. mislabelling percent possible
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("nsm3 failed...");
    }
  }


  /**
   * Executes the NSM3 solver as a separate process
   */
  private int exec(String cmdLine, int num_clusters, int k, int ci[], int tci[],
                   Vector valv) throws IOException {
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
        int i=0;
        while (in.ready()) {
          inString = in.readLine();
          System.err.println(inString);
          if (inString.startsWith("Z*")) {
            StringTokenizer tz = new StringTokenizer(inString, " ");
            tz.nextToken();
            Double val = new Double(Double.parseDouble(tz.nextToken()));
            valv.addElement(val);
          }
          else if (inString.startsWith("X :")) {
            int[] mclusters = new int[num_clusters];
            int nck = num_clusters*k;
            // find out what arc has value one
            int arcind = 0;
            StringTokenizer st = new StringTokenizer(inString, " ");
            st.nextToken();  // "X"
            st.nextToken();  // ":"
            while (st.hasMoreTokens()) {
              String val = st.nextToken();
              String val2 = val.substring(0, val.length()-1);  // avoid "B"
              double v = Double.parseDouble(val2);
              if (arcind>=nck) break;
              if (Math.abs(v-1)<1.e-8) {
                int nci = arcind / k;
                int ki = arcind % k;
                mclusters[nci] = ki;
              }
              arcind++;
            }
            // now compute the confusion matrix c[i,j]: #points from cluster i
            // that belong to class j
            int[][] confmat = new int[k][k];
            for (int r=0; r<k; r++) {
              for (int c=0; c<k; c++) {
                confmat[r][c] = 0;
              }
            }
            for (int j=0; j<tci.length; j++) {
              int fcj = mclusters[ci[j]];
              confmat[tci[j]][fcj]++;
            }
            // Now print confusion matrix
            System.err.println("Confusion Matrix:");
            for (int r=0; r<k; r++) {
              for (int c=0; c<k; c++) {
                System.err.print(confmat[r][c]+" ");
              }
              System.err.println("");
            }
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

}
