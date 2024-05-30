package utils;

import java.util.*;
import java.io.*;

public class MLSet2CoDoCUpMaker {
  public MLSet2CoDoCUpMaker() {
  }

  public static void main(String[] args) {
    if (args.length<5 || args[0].startsWith("-?") || args[0].startsWith("-h")) {
      System.out.println("usage: MLSet2CoDoCUpMaker <in String datafile> "+
                         "<in String optfile> <out String docsfile> "+
                         "<out String graphfile> <int num_matching_attrs_req> "+
                         "[<out String labelsfile>]");
      System.exit(-1);
    }
    String datafile = args[0];
    String optionsfile = args[1];
    String docsfile = args[2];
    String graphfile = args[3];
    int nummatchattrs = Integer.parseInt(args[4]);
    String labelsfile = null;
    if (args.length>5) labelsfile = args[5];
    try {
      Vector mlinsts = DataMgr.readMLInstancesFromFile(datafile, optionsfile);
      DataMgr.makeDataSetFromMLInstances(mlinsts, null, nummatchattrs, docsfile, graphfile,
                                         labelsfile);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Done.");
  }
}

