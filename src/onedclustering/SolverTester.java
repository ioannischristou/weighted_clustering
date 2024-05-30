package onedclustering;

import java.util.*;

public class SolverTester {
  public SolverTester() {
  }

  /**
   * test driver program
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      Params p = new Params((double[]) null, -1, -1);
      int n = Integer.valueOf(args[0]).intValue();
      int M = Integer.valueOf(args[1]).intValue();
      int step = Integer.valueOf(args[2]).intValue();
      double gapm = Double.valueOf(args[3]).doubleValue();
      p.readTestData(n, M, step, gapm);  // set up a random problem
      // System.out.println("next DP...");

      long st = System.currentTimeMillis();
      Solver s2 = new Solver(p);
      double optimal_v = s2.solveDPMat();
      long dur = System.currentTimeMillis()-st;
      System.out.println("\nbest value = "+optimal_v+" in "+s2.getNumIters()+" iters.");
      System.out.println("solution="+s2.getSolutionSortedIndices().toString(p));
      System.out.print("solinds=");
      Vector solinds = s2.getSolutionIndices();
      for (int i=0; i<solinds.size(); i++) {
        Integer ii = (Integer) solinds.elementAt(i);
        System.out.print(ii+", ");
      }
      System.out.println("total time="+dur+" msecs.");
/*
      // solve again using thread-parallel version
      s2 = new Solver(p);
      st = System.currentTimeMillis();
      double v = s2.solveDP2ParallelMat(2);  // solve using two threads
      dur = System.currentTimeMillis()-st;
      System.out.println("Parallel: best value = "+v);
      System.out.println(" solution="+s2.getSolutionSortedIndices().toString(p));
      System.out.println("Parallel: total time="+dur+" msecs.");
*/
      // finally compare with K-Median algorithm
      Vector docs = new Vector();
      clustering.Document.setMetric(new clustering.DocumentDistL1());
      for (int i=0; i<p.getSequenceLength(); i++) {
        clustering.Document di = new clustering.Document(new TreeMap(), 1);
        di.setDimValue(new Integer(0), p.getSequenceValueAt(i));
        docs.addElement(di);
      }
      clustering.ClustererTermination ct = new clustering.ClustererTerminationNoImpr();
      double h = 1.0;
      st = System.currentTimeMillis();
      Hashtable cl_params = new Hashtable();
      cl_params.put("h", new Double(h));
      cl_params.put("TerminationCriteria", ct);
      clustering.Clusterer cl = new clustering.KMedianClusterer();
      final int docs_size = docs.size();
      final int k = p.getM();
      final int dims = 1;
      int total_runs = 1000;
      boolean choose_cluster_centers = true;  // itc: HERE maybe change next
      Vector l = new Vector();
      for (int i=0; i<docs_size; i++)
        l.addElement(new Integer(i));
      int[] best_inds = null;  // best clustering over all runs
      double best_clustering = Double.MAX_VALUE;
      for (int i=0; i<total_runs; i++) {
        Vector centers = new Vector();  // Vector<Document>
        // prepare a random clustering
        if (choose_cluster_centers==false) {
          Collections.shuffle(l);  // get a random permutation
          int cl_index = 0;
          int csize = (int) Math.ceil((double) docs_size/(double) k);
          clustering.Document c = null;
          int c_card = 0;
          for (int j=0; j<docs_size; j++) {
            if (j%csize==0 || j==docs_size-1) {
              if (c!=null) {
                c.div(c_card);
                centers.addElement(c);
              }
              c = new clustering.Document(new TreeMap(), dims);
              c_card=0;
            }
            clustering.Document dj = (clustering.Document) docs.elementAt(((Integer) l.elementAt(j)).intValue());
            c.addMul(1.0, dj);
            c_card++;
          }
        }
        else {  // only set up the center using k random points from docs.
          // assign k random points as the cluster centers and then
          // assign the rest according to min distance
          Collections.shuffle(l);  // get a random permutation
          for (int ii=0; ii<k; ii++) {
            int cii = ((Integer) l.elementAt(ii)).intValue();
            clustering.Document dii = (clustering.Document) docs.elementAt(cii);
            clustering.Document c = new clustering.Document(new TreeMap(), dims);
            c.addMul(1.0, dii);
            centers.addElement(c);
          }  // for ii in [0,...,k-1]
        }
        // run the clustering
        cl.reset();
        cl.setParams(cl_params);
        cl.addAllDocuments(docs);
        cl.setInitialClustering(centers);
        try {
          Vector new_centers = cl.clusterDocs();
          // get clusters cardinality
          int[] cards = cl.getClusterCards();
          // evaluate clustering
          clustering.Evaluator kmedian_eval = new clustering.KMedianEvaluator();
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
        }
        catch (Exception e) {
          System.err.println("In run#"+i+" clustering failed.");
        }
      }
      System.out.println("Best clustering value over all runs of KMedian="+best_clustering);
      System.out.println("%gap="+((best_clustering-optimal_v)/optimal_v));
      dur = System.currentTimeMillis() - st;
      System.out.println("Done in "+dur+" msecs.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

}
