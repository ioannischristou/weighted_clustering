package utils;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * the main class method accepts as input three arguments
 * 1. fraufile: String, a file representing a data-cube from a number of ESU
 *    files, in csv format, that is to be converted to proper CoDoCUp format.
 *    The original csv format is as follows:
 *    #comment line
 *    user/agent-id,game-id,time-rem-slot-end(secs),time-rem-slot-start(secs), \
 *    max_coupon_value,freq[0-25],freq[26-50],...freq[975-1000],freq[>1000]
 * 2. nummerge: String, representing an integer quantity, with the semantics
 *    of how many continuous freq[] intervals to merge together.
 * 3. docsfile: String, the docs_file.txt where the resulting docs vector will
 *    be stored. Note that the agent-id and game-id parameters are not included
 *    as dimensions.
 * The final docs_file will contain docs in the space R^{d+1} where d is the
 * number of final merged buckets, and the extra dimension is the max_amount
 * dimension, which is actually inconsistent with the previous d dimensions
 * which represent the histogram of amounts played. The total number of points
 * is an aggregation over agent, game, and time, i.e. the tuple
 * (agent-id, game-id, median-time-remaining) forms the primary key for the
 * vectors in the final data-set.
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FD2CoDoCUpMaker {
  public FD2CoDoCUpMaker() {
  }

  public static void main(String[] args) {
    String fraufile = args[0];
    int nummerge = (new Integer(args[1])).intValue();
    String docs_file = args[2];
    BufferedReader br = null;
    PrintWriter pw = null;
    Hashtable vals_ht = new Hashtable();  // map<TripleKey k, Vector<double> >
    try {
      br = new BufferedReader(new FileReader(fraufile));
      pw = new PrintWriter(new FileOutputStream(docs_file));
      boolean first_line=true;
      int num_dims=0;
      if (br.ready()) {
        while (true) {
          String line = br.readLine();
          if (line==null) break;  // end-of-file
          if (line.startsWith("#")) continue;  // comment
          Vector vals = new Vector();
          System.err.println(line);  // itc: HERE debug
          StringTokenizer st = new StringTokenizer(line,",");
          String agentid = st.nextToken();
          String gameid = st.nextToken();
          double tend = (new Double(st.nextToken())).doubleValue();
          double tstart = (new Double(st.nextToken())).doubleValue();
          double med = (tend+tstart)/2.0;
          // vals.addElement(new Double(med));
          double max_val = (new Double(st.nextToken())).doubleValue();
          vals.addElement(new Double(max_val));
          int count=0;
          double val=0;
          while (st.hasMoreTokens()) {
            val += Double.parseDouble(st.nextToken());
            if (++count>=nummerge) {
              vals.addElement(new Double(val));  // add aggregate to vals vector
              val=0;
              count=0;  // reset
            }
          }
          if (count>0) vals.addElement(new Double(val));  // left-overs

          // add line
          TripleKey tk = new TripleKey(agentid, gameid, med);
          Vector v = (Vector) vals_ht.get(tk);
          if (v==null) vals_ht.put(tk,vals);
          else {
            // get the max of max vals
            double v0 = ((Double) v.elementAt(0)).doubleValue();
            double vals0 = ((Double) vals.elementAt(0)).doubleValue();
            if (v0 < vals0) v0 = vals0;
            v.set(0, new Double(v0));
            // add values one-by-one and store
            for (int i=1; i<v.size(); i++) {
              double vi = ((Double) v.elementAt(i)).doubleValue();
              double nvi = ((Double) vals.elementAt(i)).doubleValue();
              vi += nvi;
              v.set(i,new Double(vi));
            }
            vals_ht.put(tk, v);
          }
          if (first_line) num_dims = vals.size();
          first_line = false;
        }
        // now write docs file
        pw.println(vals_ht.size()+" "+num_dims);
        Iterator iter = vals_ht.values().iterator();
        int i=0;
        while (iter.hasNext()) {
          Vector vals = (Vector) iter.next();
          for (int j=0; j<vals.size(); j++) {
            Double v = (Double) vals.elementAt(j);
            pw.print((j+1)+","+v.doubleValue()+" ");
          }
          if (i<vals_ht.size()-1) pw.println("");
          i++;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        br.close();
        pw.close();
        br = null;
        pw = null;
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}

class TripleKey {
  private String _agentid="";
  private String _gameid="";
  private double _medtime;
  TripleKey(String a, String g, double t) {
    _agentid = a;
    _gameid=g;
    _medtime=t;
  }
  public boolean equals(Object o) {
    TripleKey ot=null;
    try {
      ot = (TripleKey) o;
      if (_agentid.equals(ot._agentid) && _gameid.equals(ot._gameid) && ot._medtime==_medtime)
        return true;
      else return false;
    }
    catch (Exception e) {
      return false;
    }
  }
  public int hashCode() {
    try {
      return Integer.parseInt(_agentid) + (int) Math.floor(_medtime);
    }
    catch (NumberFormatException e) {
      return (int) Math.floor(_medtime);
    }
  }

}
