package utils;


import java.util.*;
import java.io.*;
import java.awt.*;

/**
 * The class ClusterTestMaker produces synthetic test-sets for clustering
 * algorithms, in file formats the DataMgr class in this package can read.
 *
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClusterTestMaker {
  private static Random _r = new Random();


  /**
   * the method creates an input file for the main program of the class.
   * the input is the dimensionality of the vector space, and the number of
   * points to make in total. The points are created in the unit cube in R^d
   * @param d int the dimensionality of the input space
   * @param n int the total number of points
   * @param k int the total number of clusters
   * @param s double the standard deviation for each dimension
   * @param name String the file name where to store the lines
   */
  private void makeparamfile(int d, int n, int k, double s, String filename) throws Exception {
    PrintWriter pw = new PrintWriter(new FileWriter(filename));
    final double pts_per_cluster = (double) n/k;
    double[] poss = new double[d];
    for (int i=0; i<d; i++) poss[i]=1.0;
    int r=0;
    int pts_left = n;
    for (int i=0; i<k; i++) {
      int ppc = (int) gaussian(pts_per_cluster, 15.0);
      if (i==k-1) ppc = pts_left;
      pts_left -= ppc;
      pw.print(ppc+",");
      for (int j=0; j<d; j++) {
        pw.print(poss[j]+","+s);
        if (j<d-1) pw.print(",");
      }
      pw.println("");
      poss[r] += 2.95*s;  // increase position
      if (r<d-1) r++;
      else if (r>=d-1) r=0;  // cycle
    }
    pw.close();
  }


  /**
   * input arguments are:
   * args[0] -- int d, the dimensionality of the vector space
   * args[1] -- string f, the filename containing the p.d.f. data for each
   * cluster. This file has the following format:
   * line-i: num_points_to_make,mean_of_pdf_i1,sd_of_pdf_i1,mean_of_pdf_i2,sd_of_pdf_i2,...mean_of_pdf_id,sd_of_pdf_id
   * i=1...k
   * args[2] -- string of, the filename containing the resulting cluster set.
   * args[3] -- double linkdens, optional, the density of links in each cluster,
   * in that there will be approx. linkdens*cluster_size links connecting random nodes
   * within each cluster.
   * Alternatively args[3] can be the name of the file containing the labels of the test data.
   * args[4] -- int randomlinks, optional, the number of random links between
   * nodes in the data points to create (approximate number).
   * args[5] -- string graphfile, optional, the file containing the graph iff
   * args[3-4] are provided.
   * args[6] -- string plotfile, optional the file containing the plot of the
   * data points for display by xplot (or jplot). Up to 4 different class
   * representations can be used (dot, plus, box, diamond). Only the first two
   * dimensions of the data points will be displayed (in R^2)
   * args[7-10] -- int x1 y1 x2 y2 the bounding box of the plot in xplot or
   * jplot
   *
   * The program produces between one and three file(s) containing the data set
   * created.
   * The program also writes on its output, the L1 clustering objective value for
   * this data-set (according to the true label of the data-set).
   * @param args String[]
   */
  public static void main(String[] args) throws Exception {
    final int dims = Integer.parseInt(args[0]);

    if (args.length==2) {
      // create test file and exit
      ClusterTestMaker tm = new ClusterTestMaker();
      tm.makeparamfile(dims, 10000, 100, 0.01, args[1]);
      return;
    }

    String ifname = args[1];
    String ofname = args[2];

    double link_dens = -1;  // indicate no graph to be made
    int rand_links = 0;
    String graph_file = null;
    Vector graph_lines = null;  // Vector<String> representing the lines to be printed
                                // in the graph_file, in sequence.
    boolean make_graph=false;

    String plotfile = null;
    PrintWriter plotwriter=null;
    PrintWriter labelwriter = null;

    if (args.length==4) {
      try {
        int n = Integer.parseInt(args[3]);
      }
      catch (Exception e) {
        labelwriter = new PrintWriter(new FileOutputStream(args[3]));
      }
    }
    if (args.length>=6) {
      link_dens = Double.parseDouble(args[3]);
      rand_links = Integer.parseInt(args[4]);
      graph_file = args[5];
      graph_lines = new Vector();
      make_graph=true;
    }

    if (args.length>7) {
      plotfile = args[6];
      int x1 = Integer.parseInt(args[7]);
      int y1 = Integer.parseInt(args[8]);
      int x2 = Integer.parseInt(args[9]);
      int y2 = Integer.parseInt(args[10]);
      plotwriter = new PrintWriter(new FileOutputStream(plotfile));
      plotwriter.println("double double");
      plotwriter.println("invisible "+x1+" "+y1);
      plotwriter.println("invisible "+x2+" "+y2);  // specify the plot's bbox
    }

    // read ifname
    BufferedReader br = new BufferedReader(new FileReader(ifname));
    String line=null;
    StringTokenizer st=null;
    double tot_obj=0.0;
    int i=0;
    if (br.ready()) {
      Vector all_clusters = new Vector();
      while (true) {
        line = br.readLine();
        if (line == null || line.length() == 0) break;
        i++;
        st = new StringTokenizer(line, ",");
        int card_i = Integer.parseInt(st.nextToken());
        // make the ith cluster of Documents
        Vector cluster_i = new Vector();  // vector<Document>
        for (int j=0; j<card_i; j++) {
          cluster_i.addElement(new clustering.Document(new TreeMap(), dims));
          if (labelwriter!=null) labelwriter.println(i);
        }
        // populate the cluster-i:
        int dim=0;
        while (st.hasMoreTokens()) {
          double m_dim=Double.parseDouble(st.nextToken());
          double sd_dim = Double.parseDouble(st.nextToken());
          // produce the value of the dim-th coordinate
          Integer ddim = new Integer(dim);
          for (int j=0; j<card_i; j++) {
            double val = gaussian(m_dim, sd_dim);
            clustering.Document doc_j = (clustering.Document) cluster_i.elementAt(j);
            doc_j.setDimValue(ddim, val);
          }
          dim++;
          // compute L2Sqr distance obj. of cluster_i
          tot_obj += clustering.KMeansSqrEvaluator.evalCluster(cluster_i);
        }
        if (make_graph) {
          int num_links = (int) (link_dens*cluster_i.size()*Math.random()*2);
          final int sind = all_clusters.size();
          final int eind = sind+cluster_i.size()-1;
          int starta, enda;
          for (int nl=0; nl<num_links; nl++) {
            // starta, enda are in [all_clusters.size(), all_clusters.size()+cluster_i.size()-1]
            starta = (int) Math.rint(sind + Math.random()*(eind-sind));
            enda = (int) Math.rint(sind + Math.random()*(eind-sind));
            if (starta!=enda) {
              String gline = starta + " " + enda + " 1.0";
              graph_lines.addElement(gline);
            }
          }
        }
        all_clusters.addAll(cluster_i);
        if (plotwriter!=null) {
          // write the cluster for plotting
          writeDataForPlotting(plotwriter, i, cluster_i);
        }
      }  // next cluster i
      // close plotwriter
      if (plotwriter!=null) {
        plotwriter.println("go");
        plotwriter.close();
      }
      if (labelwriter!=null) labelwriter.close();
      // write the Documents to ofile
      DataMgr.writeDocumentsToFile(all_clusters, dims, ofname);
      if (make_graph) {
        // create random connections, where starta, enda are in
        // [0, all_clusters.size()-1]
        final int sind=0;
        final int eind=all_clusters.size()-1;
        int starta, enda;
        for (int nl=0; nl<rand_links; nl++) {
          // starta, enda are in [all_clusters.size(), all_clusters.size()+cluster_i.size()-1]
          starta = (int) Math.rint(sind + Math.random()*(eind-sind));
          enda = (int) Math.rint(sind + Math.random()*(eind-sind));
          if (starta!=enda) {
            String gline = starta + " " + enda + " 1.0";
            graph_lines.addElement(gline);
          }
        }
        // write graph to file
        PrintWriter gpw = new PrintWriter(new FileOutputStream(graph_file));
        gpw.println(all_clusters.size()+" "+graph_lines.size());
        for (int nl=0; nl<graph_lines.size(); nl++) {
          gpw.println((String) graph_lines.elementAt(nl));
        }
        gpw.close();
      }
      System.out.println(tot_obj);
    }
  }


  private static double gaussian(double m, double sd) {
    return m+sd*_r.nextGaussian();
  }


  private static void writeDataForPlotting(PrintWriter pw, int i, Vector cluster_i) {
    String[] shapes = {"dot", "diamond", "box", "plus"};
    int ind = i%4;
    String shapei = shapes[ind];
    Integer x = new Integer(0);
    Integer y = new Integer(1);
    for (int j=0; j<cluster_i.size(); j++) {
      clustering.Document dj = (clustering.Document) cluster_i.elementAt(j);
      Double djx = dj.getDimValue(x);
      Double djy = dj.getDimValue(y);
      pw.println(shapei+" "+djx.doubleValue()+" "+djy.doubleValue());
    }
  }
}
