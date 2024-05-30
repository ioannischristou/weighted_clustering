package utils;

import java.util.*;
import java.io.*;

public class Alpha2NumLabelSetConverter {
  public Alpha2NumLabelSetConverter() {
  }

  /**
   * convert a label set containing alphanumeric labels to a numeric label set
   * containing labels from the set N*={1,2,3,...}
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=2) {
      System.err.println("usage: java -cp . utils.Alpha2NumLabelSetConverter <input_alpha_labels_file> <output_num_labels_file>");
      System.exit(-1);
    }
    String infile = args[0];
    String outfile = args[1];
    try {
      BufferedReader br = new BufferedReader(new FileReader(infile));
      if (br.ready()) {
        int i=1;
        Hashtable t = new Hashtable();  // map<String label, Integer cnt>
        Vector labels = new Vector();
        while (true) {
          String label = br.readLine();
          if (label==null) break;
          labels.add(label);
          Integer nl = (Integer) t.get(label);
          if (nl==null) {
            nl = new Integer(i);
            i++;
            t.put(label,nl);
          }
        }
        br.close();
        // now create the new file
        PrintWriter pw = new PrintWriter(new FileWriter(outfile));
        for (int j=0; j<labels.size(); j++) {
          String lj = (String) labels.elementAt(j);
          Integer ij = (Integer) t.get(lj);
          pw.println(ij.intValue());
        }
        pw.flush();
        pw.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
