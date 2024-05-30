package utils;

import java.util.*;
import java.io.*;

public class KMeansSqrEvaluator {
  public KMeansSqrEvaluator() {
  }

  public static void main(String args[]) {
    if (args.length!=2) {
      System.err.println("usage: java utils.KMeansSqrEvaluator <docs_file> <asgn_file>");
      System.exit(-1);
    }
    String docs_file = args[0];
    String asgns_file = args[1];
    try {
      int numdocs = 0;
      int k = 0;
      int totaldims = 0;
      // read the docs
      BufferedReader br = new BufferedReader(new FileReader(docs_file));
      Vector docs = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        numdocs = Integer.parseInt(st.nextToken());
        totaldims = Integer.parseInt(st.nextToken());
        int dim=-1; double val=0.0;
        while (true) {
          line = br.readLine();
          if (line==null || line.length()==0) break;
          double doc[] = new double[totaldims];
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = Integer.parseInt(st2.nextToken())-1;
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            doc[dim] = val;
          }
          docs.addElement(doc);
        }
      }
      // now read the assignments
      br = new BufferedReader(new FileReader(asgns_file));
      int asgns[] = new int[docs.size()];
      if (br.ready()) {
        int i=0;
        while (true) {
          String line = br.readLine();
          if (line==null || line.length()==0) break;
          asgns[i++] = Integer.parseInt(line);
          if (asgns[i-1]>k) k = asgns[i-1];
        }
        // k = i;
        k++;  // increase k, as it currently contains the max. asgn index from the labels
      }
      // now compute the centers
      Vector centers = new Vector();
      int sizes[] = new int[k];
      for (int i=0; i<k; i++) {
        sizes[i] = 0;  // init sizes
        double ci[] = new double[totaldims];
        for (int m=0; m<totaldims; m++) ci[m] = 0;
        centers.addElement(ci);
      }
      for (int j=0; j<numdocs; j++) {
        double d[] = (double[]) docs.elementAt(j);
        double ci[] = (double[]) centers.elementAt(asgns[j]);
        sizes[asgns[j]]++;
        for (int m=0; m<totaldims; m++) ci[m] += d[m];
      }
      for (int i=0; i<k; i++) {
        double ci[] = (double[]) centers.elementAt(i);
        for (int j=0; j<totaldims; j++)
          ci[j] /= sizes[i];
      }
      // centers computed, now compute distances
      double result = 0.0;
      for (int i=0; i<numdocs; i++) {
        double di[] = (double[]) docs.elementAt(i);
        double ci[] = (double[]) centers.elementAt(asgns[i]);
        double r = 0.0;
        for (int j=0; j<di.length; j++)
          r += (di[j]-ci[j])*(di[j]-ci[j]);
        result += r;
      }
      System.out.println("final SSE = "+result);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
