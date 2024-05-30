package utils;
import coarsening.*;
import java.util.*;

public class GraphCliqueSupporter {
  public GraphCliqueSupporter() {
  }

  public static void main(String[] args) {
    if (args.length<5) {
      System.err.println("usage: java -cp <classpath> weight graphfile1 mapfile1 cgfile2 cmfile2 [cgfile] [cmfile]");
      return;
    }
    double weight = Double.parseDouble(args[0]);
    String basegraphfile = args[1];
    String basemapfile = args[2];
    try {
      Graph baseg = DataMgr.readGraphFromhMeTiSFile(basegraphfile, basemapfile);
      Set cliques = baseg.getAllMaximalCliques(weight);  // Set<Set<Integer nid> >
      Hashtable cliques_supp_map = new Hashtable();  // map<Set clique, Double support>
      Vector othergraphs = new Vector();
      for (int i=3; i<args.length; i+=2) {
        String gfile = args[i];
        String mfile = args[i+1];
        Graph g = DataMgr.readGraphFromhMeTiSFile(gfile, mfile);
        othergraphs.addElement(g);
      }
      // now compute support for each clique
      Iterator it = cliques.iterator();
      while (it.hasNext()) {
        Set clique = (Set) it.next();
        double sup = 0;
        for (int j=0; j<othergraphs.size(); j++) {
          Graph othergraph = (Graph) othergraphs.elementAt(j);
          sup += baseg.getCliqueLabelSupport(clique, othergraph);
        }
        sup /= othergraphs.size();
        System.out.println("For clique="+clique+" the support in the other graphs = "+sup);
        cliques_supp_map.put(clique, new Double(sup));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }
}
