package utils;

import java.util.*;
import java.io.*;

public class DocsTruncator {
  public DocsTruncator() {
  }

  public static void main(String[] args) {
    if (args.length!=3) {
      System.err.println("usage: java utils.DocsTruncator <infile> <outfile> <numvalues_to_keep>");
      System.exit(-1);
    }
    String infile = args[0];
    String outfile = args[1];
    int num_val2keep = Integer.parseInt(args[2]);
    try {
      BufferedReader br = new BufferedReader(new FileReader(infile));
      PrintWriter pw = new PrintWriter(new FileOutputStream(outfile));

      if (br.ready()) {
        String ignore = br.readLine();
        pw.println(ignore);
        Vector aux = new Vector();
        while (true) {
          String line = br.readLine();
          if (line==null || line.length()==0) break;
          StringTokenizer st = new StringTokenizer(line, " ");
          aux.clear();
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            int dim = Integer.parseInt(st2.nextToken());
            double val = Double.parseDouble(st2.nextToken());
            aux.addElement(new PP(dim, val));
          }
          Object arr[] = aux.toArray();
          Arrays.sort(arr);
          // store in outfile
          for (int i=0; i<num_val2keep && i<arr.length; i++) {
            int d = ((PP) arr[i])._dim;
            double v = ((PP) arr[i])._val;
            pw.print(d+","+v+" ");
          }
          pw.println("");
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

class PP implements Comparable {
  int _dim; double _val;

  public PP(int d, double v) {
    _dim = d; _val = v;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      PP dd = (PP) o;
      if (_val==dd._val) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return (int) Math.floor(_val);
  }


  public int compareTo(Object o) {
    PP p = (PP) o;
    // bigger is smaller, so as to sort in descending order
    if (_val > p._val) return -1;
    else if (_val==p._val) return 0;
    else return 1;
  }
}
