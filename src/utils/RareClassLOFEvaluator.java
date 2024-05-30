package utils;

import java.util.*;
import java.io.*;


public class RareClassLOFEvaluator {
  public RareClassLOFEvaluator() {
  }

  /**
   * computes the True Positive (TP), True Negative (TN), False Positive (FP)
   * and False Negative (FN) numbers for an assignment of rare-class instances
   * to actual rare classes, and from there, computes the detection-rate and
   * false-alarm-rate of a method that assigned "rare-class" status to some
   * instances.
   * Inputs:
   * args[0] - lofscore_file (String) the file containing the assigned LOF scores.
   *         - This file is text file with one number in each row, representing the
   *         - LOF score of the data point corresponding to the row.
   * args[1] - labels_file (String) the file containing the true class labels.
   *         - This file is as the previous one, only that it contains the true
   *         - class labels for the problem.
   * args[2] - max_acc_LOF_score (int) the threshold size above which a point is
   *         - considered an outlier.
   * args[3...numargs] - Strings that are those class labels found in labels_file
   *                   - that are true rare classes.
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java -cp $CLASS_DIR utils.RareClassLOFEvaluator LOF_score_file labels_file max_acc_LOF_score rare_label1 [rare_label2 ...]");
      System.exit(-1);
    }
    // init: read args
    String lofscore_file = args[0];
    String labels_file = args[1];
    double max_acc_LOF_score = Double.parseDouble(args[2]);
    Set rare_labels = new HashSet();
    for (int i=3; i<args.length; i++) {
      rare_labels.add(args[i]);
    }

    Vector asgns = new Vector();  // Vector<Double score>
    Vector labels = new Vector();  // Vector<String>
    // init: read files
    try {
      BufferedReader bra = new BufferedReader(new FileReader(lofscore_file));
      if (bra.ready()) {
        while (true) {
          String line = bra.readLine();
          if (line==null) break;  // EOF
          Double asgni = Double.valueOf(line);
          asgns.addElement(asgni);
        }
      }
      else throw new IOException("lofscore_file reading problem?");
      BufferedReader brl = new BufferedReader(new FileReader(labels_file));
      if (brl.ready()) {
        int i=0;
        while (true) {
          String line = brl.readLine();
          if (line==null) break;  // EOF
          labels.addElement(line.trim());
        }
      }
      else throw new IOException("labels file reading problem?");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

    // compute TP, TN, FP, FN, DR, FA
    final int n=asgns.size();
    int tp=0;
    int tn=0;
    int fp=0;
    int fn=0;
    for (int i=0; i<n; i++) {
      Double asgni = (Double) asgns.elementAt(i);
      String labeli = (String) labels.elementAt(i);
      boolean is_rare = rare_labels.contains(labeli);
      if (is_rare && asgni.doubleValue()>max_acc_LOF_score) ++tp;
      else if (is_rare) ++fn;
      else if (!is_rare && asgni.doubleValue()<=max_acc_LOF_score) ++tn;
      else if (!is_rare) ++fp;
    }
    double dr = tp/((double) tp+fn);
    double fa = fp/((double) fp+tn);
    System.out.println("TP="+tp+" TN="+tn+" FP="+fp+" FN="+fn+" DR="+dr+" FA="+fa);
    return;
  }
}

