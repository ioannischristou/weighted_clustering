package clustering;

import utils.*;
import java.util.*;

public class XMeansEvaluator {
  public XMeansEvaluator() {
  }

  /**
   * args[0]: docs_file
   * args[1]: centers_file
   * return the SSE value of the clustering produced by a run of the X-Means
   * algorithm
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=2) {
      System.err.println("usage: XMeansEvaluator <docs_file> <centers_file>");
      System.exit(-1);
    }
    final String docs_file = args[0];
    final String centers_file = args[1];
    try {
      DocumentDistIntf metric = new DocumentDistL2Sqr();
      Vector docs = DataMgr.readDocumentsFromFile(docs_file);
      final int n = docs.size();
      // read centers
      Vector centers = DataMgr.readCentersFromFile(centers_file);
      final int k = centers.size();
      // figure out where each point goes
      double val = 0.0;
      for (int i=0; i<n; i++) {
        Document di = (Document) docs.elementAt(i);
        double best_dist = Double.MAX_VALUE;
        for (int j=0; j<k; j++) {
          Document cj = (Document) centers.elementAt(j);
          double dist = metric.dist(di, cj);
          if (dist<best_dist) best_dist = dist;
        }
        val += best_dist;
      }
      System.out.println("X-Means SSE value = "+val);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
