package clustering;

import java.util.*;
import java.io.*;

public class HungarianEvaluator extends EnhancedEvaluator {

  private Hashtable _params=null;

  public HungarianEvaluator() {
  }


  /**
   * compute the Best (minimal) weight bipartite matching between the
   * clusterer's clustering and the true clusters that must have been provided
   * as a parameter to the clusterer as well, under the attribute key "trueclusters"
   * An implementation of the Hungarian method is used for this purpose.
   * Note: num_clusters must be greater than or equal to k (k = true #clusters)
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
    int cards[] = cl.getClusterCards();
    // final Vector docs = cl.getCurrentDocs();
    // compute a[i,j] - the number of mislabelings when cluster i is assigned
    // the true label j.
    int a[][] = new int[num_clusters][num_clusters];
    // a[k][num_clusters] has orig. dimensions #rows=k, #cols=num_clusters
    for (int i=0; i<num_clusters; i++) {  // cluster index i
      for (int j=0; j<k; j++) {  // true label j
        a[j][i]=0;
        for (int m=0; m<n; m++) {
          if (clusterindices[m]==i && trueclusterindices[m]!=j)
            a[j][i]--;  // the Hungarian method is used to maximize asgn weight
                        // but we want the minimal weight bipartite matching
                        // so we multiply each edge weight by -1.
                        // a[j][i] represents the negative of the number of
                        // errors we make if we assign cluster i to truecluster j.
        }
      }
      for (int j=k; j<num_clusters; j++) {  // extra nodes at the right-hand side
        a[j][i] = -cards[i];
      }
    }
    // call the jdk1.5 class to solve the problem by the Hungarian method
    String tmpfile="asgn.dat";
    try {
      PrintWriter pw = new PrintWriter(new FileOutputStream(tmpfile));
      for (int i=0; i<num_clusters; i++) {
        for (int j=0; j<num_clusters; j++) {
          pw.print(a[i][j]+" ");
        }
        pw.println("");
      }
      pw.close();
      // String cmdline = "java -cp . Hungarian "+k+" "+k+" "+tmpfile;
      String cmdline = "java -cp . Hungarian "+num_clusters+" "+num_clusters+" "+tmpfile;
      // oops: the Hungarian.class only works correctly when k==num_clusters
      int[] solution = new int[num_clusters];
      exec(cmdline, solution);
      // figure out errors
      for (int i=0; i<num_clusters; i++) {
        res += a[solution[i]][i];
      }
      return -res/(double) n;  // returns the min. mislabelling percent possible
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("Hungarian failed...");
    }
  }


  public double evalCluster(Vector docs) throws ClustererException {
    throw new ClustererException("not implemented");
  }


  public double eval(Vector docs, int[] arr) throws ClustererException {
    throw new ClustererException("not implemented");
  }


  public void setParams(Hashtable params) {
    _params = new Hashtable(params);
  }


  public double bestOf(double v1, double v2) {
    // default: bigger is better
    return Math.min(v1, v2);
  }


  public double getWorstValue() {
    return Double.MAX_VALUE;
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
        int i=0;
        while (in.ready()) {
          inString = in.readLine();
          solution[i++] = Integer.parseInt(inString);
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
