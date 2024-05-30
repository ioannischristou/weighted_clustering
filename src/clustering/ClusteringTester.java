package clustering;

import java.util.*;

/**
 * Tests various clusterers (only)
 *
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClusteringTester {
  /**
   * args[0]: class_name
   * args[1]: h-value
   * args[2]: 0 if termination-criteria is NoImpr, -1 if termination-criteria is
   * NoImprL2Sqr, number of iterations in ClusterTerminationNumIters else
   * args[3]: documents_file_name
   * args[4]: k the number of clusters to build
   * args[5]: number of different random runs.
   * args[6]: if it exists, instead of randomly assigning cluster indices to points
   * simply choose one point to be the center of each cluster and assign the
   * rest according to min distance
   * args[7] if it exists, for PCAEnsemble, it sets the value rmax
   * If the clusterer is PCA-based clusterer and args[6] is the String
   * "improve" then one run of KMedianClusterer is used to improve upon the
   * results of PCA-based clustering
   * @param args String[]
   */
  public static void main(String[] args) {
    long start_time = System.currentTimeMillis();
    try {
        ClustererTermination ct=null;
        String clusterer_class_name = args[0];
        Class cc = Class.forName(clusterer_class_name);
        Clusterer cl = (Clusterer) cc.newInstance();
        double h = Double.parseDouble(args[1]);
        int num_iters = Integer.parseInt(args[2]);
        if (num_iters==0) {
          ct = new ClustererTerminationNoImpr();
        }
        else if (num_iters==-1) {
          ct = new ClustererTerminationNoImprL2Sqr();
        }
        else ct = new ClusterTerminationNumIters(num_iters);
        final int k = Integer.parseInt(args[4]);
        Hashtable params = new Hashtable();
        params.put("h", new Double(h));
        params.put("TerminationCriteria", ct);
        params.put("k", new Integer(k));  // for PCA-based clusterers
        if (args.length>7) {
          params.put("r", new Integer(args[7]));  // for PCAEnsemble clusterers
          params.put("numtries", new Integer(num_iters));
        }

        String docs_file = args[3];
        Vector docs = utils.DataMgr.readDocumentsFromFile(docs_file);
        final int docs_size = docs.size();
        final int dims = ((Document) docs.elementAt(0)).getDim();
        int total_runs = Integer.parseInt(args[5]);
        boolean choose_cluster_centers = (args.length>6);
        if (choose_cluster_centers) {
          // for PCAEnsemble clusterers, choose kHMeTis as partitioner
          // minimizing SOED
          partitioning.Partitioner parter = new partitioning.PartitionerkHMeTiS();
          params.put("partitioner", parter);
          params.put("partitionerpath", "C:\\downloads\\vlsi\\hmetis-1.5.3-WIN32");
          params.put("partitionername", "khmetis");
          params.put("graphfilename","sim_graph.hgr");
          params.put("numnodes", new Integer(docs_size));
          // itc: HERE yet another test for PCAEnsembleClusterer
          params.put("normalizematrix", new Boolean(true));
          params.put("t", new Integer(20));
          // itc: HERE up to here
          // for PCAEnsemble
        }
        Vector l = new Vector();
        for (int i=0; i<docs_size; i++)
          l.addElement(new Integer(i));
        int[] best_inds = null;  // best clustering over all runs
        double best_clustering = Double.MAX_VALUE;
        for (int i=0; i<total_runs; i++) {
          Vector centers = new Vector();  // Vector<Document>
          // prepare a random clustering
          choose_cluster_centers = true;  // itc: HERE rm this line asap
          if (choose_cluster_centers==false) {
            Collections.shuffle(l, utils.RndUtil.getInstance().getRandom());  // get a random permutation
            int cl_index = 0;
            int csize = (int) Math.ceil((double) docs_size/(double) k);
            Document c = null;
            int c_card = 0;
            for (int j=0; j<docs_size; j++) {
              if (j%csize==0 || j==docs_size-1) {
                if (c!=null) {
                  c.div(c_card);
                  centers.addElement(c);
                }
                c = new Document(new TreeMap(), dims);
                c_card=0;
              }
              Document dj = (Document) docs.elementAt(((Integer) l.elementAt(j)).intValue());
              c.addMul(1.0, dj);
              c_card++;
            }
          }
          else {  // only set up the center using k random points from docs.
            // assign k random points as the cluster centers and then
            // assign the rest according to min distance
            Collections.shuffle(l, utils.RndUtil.getInstance().getRandom());  // get a random permutation
            for (int ii=0; ii<k; ii++) {
              int cii = ((Integer) l.elementAt(ii)).intValue();
              Document dii = (Document) docs.elementAt(cii);
              Document c = new Document(new TreeMap(), dims);
              c.addMul(1.0, dii);
              centers.addElement(c);
            }  // for ii in [0,...,k-1]
          }
          // run the clustering
          cl.reset();
          cl.setParams(params);
          cl.addAllDocuments(docs);
          cl.setInitialClustering(centers);
          try {
            Vector new_centers = cl.clusterDocs();
            // get clusters cardinality
            int[] cards = cl.getClusterCards();
            // evaluate clustering
            Evaluator kmedian_eval;
            if  (num_iters==-1) kmedian_eval = new KMeansSqrEvaluator();
            // else if (num_iters==0) kmedian_eval = new KMeansEvaluator();
            else kmedian_eval = new KMedianEvaluator();
            double obj = cl.eval(kmedian_eval);
            // update incumbent
            if (obj < best_clustering) {
              best_clustering = obj;
              best_inds = cl.getClusteringIndices();
            }
            System.out.print("In run #"+i+" clustering obj="+obj);
            System.out.print(" cluster cards: ");
            for (int ii=0; ii<cards.length; ii++) {
              System.out.print("c["+ii+"]="+cards[ii]+" ");
            }
            System.out.println("");
            if (args.length>6 && "improve".equals(args[6])) {
              // used only for PCA-based initial clusterer
              Clusterer improver = new KMedianClusterer();
              improver.setParams(params);
              improver.addAllDocuments(docs);
              improver.setInitialClustering(new_centers);
              improver.clusterDocs();
              obj = improver.eval(new KMedianEvaluator());
              System.out.print("After KMedian improvement, clustering obj="+obj);
              if (obj<best_clustering) {
                best_clustering = obj;
                best_inds = cl.getClusteringIndices();
              }
            }
          }
          catch (Exception e) {
            e.printStackTrace();  // itc: HERE rm asap
            System.err.println("In run#"+i+" clustering failed.");
          }
        }
        System.out.println("Best clustering value over all runs="+best_clustering);
        long dur = System.currentTimeMillis() - start_time;
        // print plot
        // utils.DataMgr.print2PlotFile("simpleclustering_plot.txt", docs, best_inds);
        System.out.println("Done in "+dur+" msecs.");
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
}
