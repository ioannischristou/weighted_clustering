package utils;

import java.io.*;
import java.util.*;
import clustering.*;

public class TSPLIB2CoDoCUpMaker {
  private static final int EUC_2D=1;
  private static final int EUC_3D=2;
  private static final int MAN_2D=3;
  private static final int MAN_3D=4;

  private int _distmetric;

  public TSPLIB2CoDoCUpMaker() {
  }

  public void maketestfiles(String tspfile, String docsfile, String graphfile,
                            double edgeratio)
      throws FileNotFoundException, IOException, clustering.ClustererException {
    BufferedReader br=null;
    try {
      br = new BufferedReader(new FileReader(tspfile));
      Vector docs = new Vector(); // Vector<Document>
      Vector city_xcoords = new Vector(); // Vector<Double>
      Vector city_ycoords = new Vector(); // Vector<Double>
      if (br.ready()) {
        int i = 0;
        while (i++ >= 0) {
          String line = br.readLine().trim();
          if (line == null || "EOF".equals(line))break; // end reading file
          if (line.matches(".*EUC_2D.*")) _distmetric = EUC_2D;
          if (line.matches(".*EUC_3D.*")) _distmetric = EUC_3D;
          if (line.matches(".*MAN_2D.*")) _distmetric = MAN_2D;
          if (line.matches(".*MAN_3D.*")) _distmetric = MAN_3D;
          if (i <= 6)continue;
          // format from now on is nodenum x-coord y-coord
          StringTokenizer tz = new StringTokenizer(line, " ");
          String cityno = tz.nextToken();
          // System.err.println("read num: "+cityno);  // itc: HERE rm asap
          Double x = new Double(tz.nextToken());
          Double y = new Double(tz.nextToken());
          // System.err.println("read city at ["+x+", "+y+"]");  // itc: HERE rm asap
          city_xcoords.addElement(x);
          city_ycoords.addElement(y);
          Document doci = new Document(new TreeMap(), 2);
          doci.setDimValue(new Integer(0), x.doubleValue());
          doci.setDimValue(new Integer(1), y.doubleValue());
          docs.addElement(doci);
        }
      }
      // OK, now create docs file
      DataMgr.writeDocumentsToFile(docs, 2, docsfile);
      // finally, create graph file if ratio > 0
      if (edgeratio > 0) {
        // 1. figure out all distances between each pair of cities
        final int numcities = docs.size();
        double[][] dists = new double[numcities][numcities];
        TreeSet sortdists = new TreeSet();
        int k = 0;
        for (int i = 0; i < numcities; i++) {
          for (int j = i; j < numcities; j++) {
            // for now, assume EUC_2D metric
            if (j == i) {
              dists[i][i] = 0;
              continue;
            }
            double dx = ( (Double) city_xcoords.elementAt(i)).doubleValue() -
                ( (Double) city_xcoords.elementAt(j)).doubleValue();
            double dy = ( (Double) city_ycoords.elementAt(i)).doubleValue() -
                ( (Double) city_ycoords.elementAt(j)).doubleValue();
            dists[i][j] = Math.sqrt(dx * dx + dy * dy);
            dists[j][i] = dists[i][j];
            sortdists.add(new Double(dists[i][j]));
          }
        }
        // 2. now get sortdists is sorted in ascending order, so get the appropriate ratio
        //    and compute upper distance bound
        double max_dist_to_allow = 0;
        int m = (int) Math.ceil(sortdists.size() * edgeratio);
        Iterator it = sortdists.iterator();
        for (int i = 0; i < m; i++) it.next();
        max_dist_to_allow = ( (Double) it.next()).doubleValue();
        // 3. finally, add only the edges that are <= max_dist_to_allow
        PrintWriter pw = new PrintWriter(new FileOutputStream(graphfile));
        Vector lines = new Vector(); // Vector<String edgeline>
        for (int i = 0; i < numcities; i++) {
          for (int j = i + 1; j < numcities; j++) { // edges are one way only, i->j (j>i)
            if (i == j)continue;
            if (dists[i][j] < max_dist_to_allow) {
              String line = i + " " + j + " " + (1.0 / dists[i][j]);
              // edge weight is inverse of distance so as to keep cities close
              // together in the same partition.
              lines.addElement(line);
            }
          }
        }
        pw.println(numcities + " " + lines.size());
        for (int i = 0; i < lines.size(); i++)
          pw.println( (String) lines.elementAt(i));
        pw.flush();
        pw.close();
      }
    }
    finally {
      if (br!=null) br.close();
    }
  }


  public static void main(String[] args) {
    if (args.length!=4 || args[0].startsWith("-?") || args[0].startsWith("-h")) {
      System.out.println("usage: TSPLIB2CoDoCUpMaker <in String tspfile> "+
                         "<out String docsfile> "+
                         "<out String graphfile> <double top__edges_to_keep_ratio>");
      System.exit(-1);
    }
    String tspfile = args[0];
    String docsfile = args[1];
    String graphfile = args[2];
    double ratio = Double.parseDouble(args[3]);
    try {
      TSPLIB2CoDoCUpMaker maker = new TSPLIB2CoDoCUpMaker();
      maker.maketestfiles(tspfile, docsfile, graphfile, ratio);
      System.out.println("Done.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
