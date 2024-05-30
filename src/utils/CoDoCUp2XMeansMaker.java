package utils;

import java.io.*;
import java.util.*;
import clustering.*;

/**
 *
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System. The class CoDoCUp2XMeansMaker
 * class is responsible for converting a CoDoCUp documents file into
 * X-Means format. The format of the X-Means program is a space-delimited ASCII
 * format as follows:
 * dim1 dim2 dim3 dim4 ...
 * v11 v12  v13  v14
 * v21 v22  v23  v24
 * ...
 * </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class CoDoCUp2XMeansMaker {

  public CoDoCUp2XMeansMaker() {
  }


  /**
   * args[0]: input file of CoDoCUp documents.
   * args[1]: output file in X-Means format.
   * Each row in the output file is one document
   * Each col in the output file is one dimension of each document
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=2) {
      System.err.println("usage: java CoDoCUp2XMeansMaker CoDoCUpInputFile XMeansOutputFile");
      System.exit(-1);
    }
    String infile = args[0];
    String outfile = args[1];
    try {
      // read
      Vector docs = DataMgr.readDocumentsFromFile(infile);  // Vector<Document>
      Document d0 = (Document) docs.elementAt(0);
      int numdims = d0.getDim();
      // write
      PrintWriter pw = new PrintWriter(new FileOutputStream(outfile));
      // write 1st line
      for (int i=0; i<numdims; i++) {
        pw.print("i" + i);
        if (i<numdims-1) pw.print(" ");
      }
      pw.println("");  // new line
      for (int i=0; i<docs.size(); i++) {
        Document di = (Document) docs.elementAt(i);
        for (int j=0; j<numdims; j++) {
          Double v = di.getDimValue(new Integer(j));
          if (v!=null) pw.print(v);
          else pw.print("0");
          if (j<numdims-1) pw.print(" ");
        }
        pw.println("");  // new line
      }
      pw.flush();
      pw.close();
      System.out.println("Done creating file "+outfile);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
