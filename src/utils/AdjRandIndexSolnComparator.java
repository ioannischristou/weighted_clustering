package utils;

import java.io.*;
import java.util.*;
import clustering.AdjRandIndexEvaluator;

public class AdjRandIndexSolnComparator {
  public AdjRandIndexSolnComparator() {
  }

  /**
   * accept as input the file-names of two files containing two different
   * clustering solutions and returns the Adjacent Rand Index of the two
   * solns.
   * usage: java utils.AdjRandIndexSolnComparator solnfile1 solnfile2
   * @param args String[]
   */
  public static void main(String[] args) {
    compare(args);
  }


  public static void compare(String[] args) {
    String file1 = args[0];
    String file2 = args[1];
    try {
      BufferedReader br = new BufferedReader(new FileReader(file1));
      int i1 = 0;
      while (br.readLine()!=null) i1++;
      br.close();
      br = new BufferedReader(new FileReader(file2));
      int i2=0;
      while (br.readLine()!=null) i2++;
      br.close();
      int[] soln1 = utils.DataMgr.readLabelsFromFile(file1, i1);
      int[] soln2 = utils.DataMgr.readLabelsFromFile(file2, i2);
      // find out k1 and k2
      HashSet sets=new HashSet();
      for (int i=0; i<soln1.length; i++) sets.add(new Integer(soln1[i]));
      int k1 = sets.size();
      sets.clear();
      for (int i=0; i<soln2.length; i++) sets.add(new Integer(soln2[i]));
      int k2 = sets.size();
      AdjRandIndexEvaluator eval = new AdjRandIndexEvaluator();
      double res = eval.eval(soln1, k1, soln2, k2);
      System.out.println("AdjRandIndex = "+res);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

