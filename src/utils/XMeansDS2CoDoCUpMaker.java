package utils;

import java.io.*;
import java.util.*;
import clustering.*;

public class XMeansDS2CoDoCUpMaker {

  private int _distmetric;

  public XMeansDS2CoDoCUpMaker() {
  }

  public void maketestfiles(String tspfile, String docsfile, String graphfile,
                            double edgeratio)
      throws FileNotFoundException, IOException, clustering.ClustererException {
    BufferedReader br=null;
    try {
      br = new BufferedReader(new FileReader(tspfile));
      Vector docs = new Vector(); // Vector<Document>
      int dims = 2; // default
      if (br.ready()) {
        int i = 0;
        while (i++ >= 0) {
          String line = br.readLine();
          if (line == null || "EOF".equals(line))break; // end reading file
          if (line.startsWith("#"))continue; // comment
          if (line.startsWith("x")) {
            // how many dimensions
            StringTokenizer st = new StringTokenizer(line);
            dims = st.countTokens();
          }
          else {
            // format from now on is x1-coord x2-coord ...
            StringTokenizer tz = new StringTokenizer(line, " ");
            // tz.nextToken();
            Document doci = new Document(new TreeMap(), dims);
            int j = 0;
            while (tz.hasMoreTokens()) {
              Double xi = new Double(tz.nextToken());
              doci.setDimValue(new Integer(j), xi.doubleValue());
              j++;
            }
            docs.addElement(doci);
          }
        }
      }
      // OK, now create docs file
      DataMgr.writeDocumentsToFile(docs, dims, docsfile);
      // don't create dummy graph file
    }
    finally {
      if (br!=null) br.close();
    }
  }


  public static void main(String[] args) {
    if (args.length!=4 || args[0].startsWith("-?") || args[0].startsWith("-h")) {
      System.out.println("usage: XMeansDS2CoDoCUpMaker <in String tspfile> "+
                         "<out String docsfile> "+
                         "<out String graphfile> <double top__edges_to_keep_ratio>");
      System.exit(-1);
    }
    String tspfile = args[0];
    String docsfile = args[1];
    String graphfile = args[2];
    double ratio = Double.parseDouble(args[3]);
    try {
      XMeansDS2CoDoCUpMaker maker = new XMeansDS2CoDoCUpMaker();
      maker.maketestfiles(tspfile, docsfile, graphfile, ratio);
      System.out.println("Done.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
