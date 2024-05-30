package exactclustering;

import java.util.*;
import utils.*;
import clustering.*;

public class ECTester {
  public ECTester() {
  }


  /**
   * args[0]: docs file name
   * args[1]: params file name
   * args[2]: [optional] max num nodes to create in B&B Clustering
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      Vector docs = DataMgr.readDocumentsFromFile(args[0]);
      final int n = docs.size();
      Hashtable params = DataMgr.readPropsFromFile(args[1]);
      final int k = ((Integer) params.get("k")).intValue();
      final DocumentDistIntf metric = (DocumentDistIntf) params.get("metric");
      int maxnodes = -1;
      if (args.length>2)
        maxnodes = Integer.parseInt(args[2]);
      // first try SPP clusterer
      SPPClusterer sppcl = new SPPClusterer();
      sppcl.addAllDocuments(docs);
      sppcl.setParams(params);
      Vector spp_centers = sppcl.clusterDocs();
      // sppcl.clusterDocs();
      final int sppsoln[] = sppcl.getClusteringIndices();
      // Vector spp_centers = Document.getCenters(docs, sppsoln, k);
      Evaluator kmseval = new clustering.KMeansSqrEvaluator();
      double bound = kmseval.eval(sppcl);
      System.out.println("ECTester: best value found by SPPClusterer is "+bound);
      StringBuffer soln_fileb0 = new StringBuffer(args[0]);
      soln_fileb0.append("_origasgns.txt");
      String soln_file0 = soln_fileb0.toString();
      DataMgr.writeLabelsToFile(sppcl,soln_file0);
      Boolean rearrange_docs = (Boolean) params.get("rearrangedocs");
      int rearrange_map[] = new int[n];
      for (int i=0; i<n; i++) rearrange_map[i] = i;  // init
      if (rearrange_docs==null || rearrange_docs.booleanValue()==true) {
        // find the docs that are closest to each center
        for (int i = 0; i < k; i++) {
          Document ci = (Document) spp_centers.elementAt(i);
          int bestj = -1;
          double bestdist = Double.MAX_VALUE;
          for (int j = 0; j < n; j++) {
            Document dj = (Document) docs.elementAt(j);
            double dist = metric.dist(dj, ci);
            if (dist < bestdist) {
              bestdist = dist;
              bestj = j;
            }
          }
          if (sppsoln[rearrange_map[bestj]]!=i)  // sanity check
            throw new ExactClusteringException("asgn["+bestj+"]="+sppsoln[bestj]+" where i="+i+" ...first rearrangement failed?");
          // swap position i with position j in the docs vector
          Document di = new Document( (Document) docs.elementAt(i));
          Document dj = (Document) docs.elementAt(bestj);
          docs.set(i, dj);
          docs.set(bestj, di);
          int tmpi = rearrange_map[i];
          rearrange_map[i] = rearrange_map[bestj];
          rearrange_map[bestj] = tmpi;
        }
        // now rearrange docs in [k...n-1] so that first come all the 0-asnged
        // docs, then the 1-asgned docs, etc.
        utils.Pair pairs[] = new utils.Pair[n-k];
        for (int i=0; i<pairs.length; i++) {
          int j = rearrange_map[i+k];
          int cind = sppcl.getClusteringIndices()[j];
          pairs[i] = new utils.Pair(new Integer(cind), new Integer(i+k));
        }
        Arrays.sort(pairs, new PairComp());
        for (int i=0; i<pairs.length; i++) {
          utils.Pair pi = pairs[i];
          int pos = ((Integer) pi.getSecond()).intValue();
          // swap doc at position k+i with element at position pos
          Document dki = new Document((Document) docs.elementAt(k+i));
          Document dpos = (Document) docs.elementAt(pos);
          docs.set(k+i, dpos);
          docs.set(pos, dki);
          int tmpi = rearrange_map[k+i];
          rearrange_map[k+i] = rearrange_map[pos];
          rearrange_map[pos] = tmpi;
        }
      }

      // now run the BBClustering algorithm
      int nt = 1;
      Integer num_threadsI = (Integer) params.get("numthreads");
      if (num_threadsI!=null)
        nt = num_threadsI.intValue();
      if (nt>1) BBThreadPool.setNumthreads(nt);
      int initsoln[] = new int[n];
      // int orsoln[] = sppcl.getClusteringIndices();
      for (int i=0; i<n; i++) {
        // orsoln[i] is the cluster index of the document originally found in position
        initsoln[i] = sppsoln[rearrange_map[i]];
      }

      Integer parlvlI = (Integer) params.get("parlvl");
      BBTree t = null;
      if (parlvlI!=null)
        t = new BBTree(docs, k, metric, initsoln, bound, parlvlI.intValue());
      else t = new BBTree(docs, k, metric, initsoln, bound);
      // BBTree t = new BBTree(docs, k, metric, Double.MAX_VALUE);
      if (maxnodes>0) t.setMaxNodesAllowed(maxnodes);
      t.run();
      // save reconstructed best soln in args[0]_asgns.txt
      int finalsoln[] = new int[n];
      int orsoln[] = t.getSolution();
      for (int i=0; i<n; i++) {
        // orsoln[i] is the cluster index of the document originally found in position
        finalsoln[rearrange_map[i]] = orsoln[i];
      }
      StringBuffer soln_fileb = new StringBuffer(args[0]);
      soln_fileb.append("_asgns.txt");
      String soln_file = soln_fileb.toString();
      DataMgr.writeLabelsToFile(finalsoln, soln_file);
      System.out.println("Done.");
      System.exit(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

}


class PairComp implements java.io.Serializable, Comparator {
  public int compare(Object pair1, Object pair2) {
    utils.Pair p1 = (utils.Pair) pair1;
    utils.Pair p2 = (utils.Pair) pair2;
    Integer cind1 = (Integer) p1.getFirst();
    Integer cind2 = (Integer) p2.getFirst();
    if (cind1.intValue()<cind2.intValue()) return -1;
    else if (cind1.intValue()==cind2.intValue()) return 0;
    else return 1;
  }
}
