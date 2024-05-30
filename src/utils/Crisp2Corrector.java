package utils;

import java.io.*;
import java.util.*;

public class Crisp2Corrector {
  public Crisp2Corrector() {
  }

  /**
   * auxiliary program to correct problematic crisp2 output, in particular
   * it corrects the out_Index.txt file entries that have a line that reads
   * 0,0.0  --->  1,0.0
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=2) {
      System.err.println("usage: java utils.Crisp2Corrector <infile> <outfile>");
      System.exit(-1);
    }
    String infile = args[0];
    String outfile = args[1];
    try {
      BufferedReader br = new BufferedReader(new FileReader(infile));
      PrintWriter pw = new PrintWriter(new FileOutputStream(outfile));
      if (br.ready()) {
        while (true) {
          String line = br.readLine();
          if (line==null || line.length()==0) break;
          if (line.equals("0,0.0")) line = "1,0.0";
          pw.println(line);
        }
      }
      pw.flush();
      pw.close();
      br.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
