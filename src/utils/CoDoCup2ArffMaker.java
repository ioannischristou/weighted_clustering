package utils;

import java.util.*;

public class CoDoCup2ArffMaker {
  public CoDoCup2ArffMaker() {
  }

  public static void main(String[] args) {
    if (args.length!=3) {
      System.err.println("usage: java -cp ./classes utils.CoDoCup2ArffMaker <docs_file> <labels_file> <arff_file>");
      System.exit(-1);
    }
    try {
      Vector docs = DataMgr.readDocumentsFromFile(args[0]);
      int[] labels = DataMgr.readNumericLabelsFromFile(args[1]);
      DataMgr.writeDocsAndLabelsToArffFile(docs, labels, args[2]);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

