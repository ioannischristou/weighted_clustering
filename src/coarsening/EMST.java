package coarsening;

import java.util.*;
import clustering.*;
import utils.*;

/**
 * Minimum Spanning Tree.
 * Kruskal's Algorithm Implementation.
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class EMST {
  private Vector _docs;  // the docs which represent nodes in the tree, they
                         // are points in R^d.
  private static int _numNbors2Consider=10;  // the # of nearest nbors for which
                                             // an edge will be created connecting
                                             // them.
  private DocumentDistIntf _metric = new DocumentDistL2();

  private Graph _g;  // the graph created from the _docs, for which we want
                     // a Minimum Spanning Tree

  public EMST(Vector docs) throws ClustererException, GraphException {
    _docs = docs;
    final int n = _docs.size();
    final int nnbors = _numNbors2Consider<n ? _numNbors2Consider : n;
    final int e = n*(nnbors-1);
    _g = new Graph(n,e);
    // populate _g
    Pair dists[] = new Pair[n];
    for (int i=0; i<n; i++) {
      Document di = (Document) _docs.elementAt(i);
      for (int j=0; j<n; j++) {
        // if (j==i) continue;
        Document dj = (Document) _docs.elementAt(j);
        dists[j] = new Pair(new Integer(j), new Double(_metric.dist(di, dj)));
      }
      Arrays.sort(dists, new DistComp());
      for (int j=0; j<nnbors; j++) {
        Pair pj = dists[j];
        if (((Integer) pj.getFirst()).intValue() == i) continue;
        _g.addLink(i,((Integer) pj.getFirst()).intValue(),((Double)pj.getSecond()).doubleValue());
      }
    }

  }


  /**
   * Kruskal's algorithm for computing the MST of the graph _g.
   * Uses the DisjointSet data structure for high performance
   * @return double
   */
  public double cost() {
    double res = 0.0;
    DisjointSet djset = new DisjointSet();
    // 1. put every node in its own set
    for (int i=0; i<_g.getNumNodes(); i++) {
      djset.makeSet(_g.getNode(i));
    }
    // 2. sort the links in asc. order
    Link[] links = new Link[_g.getNumArcs()];
    for (int i=0; i<links.length; i++) {
      links[i] = _g.getLink(i);
    }
    Arrays.sort(links, new LinkComp());
    // 3. for each link in the sorted links array check if both ends of the link
    //    are in the same set. If not, add link to the mst, and unite two sets,
    //    else discard link
    for (int i=0; i<links.length; i++) {
      DisjointSetElem start = djset.find(_g.getNode(links[i].getStart()));
      DisjointSetElem end = djset.find(_g.getNode(links[i].getEnd()));
      if (start != end) {
        djset.union(start, end);
        res += links[i].getWeight();
      }
      // else no-op
    }
    return res;
  }
}


class DistComp implements Comparator, java.io.Serializable {
  public int compare(Object x, Object y) {
    Pair xp = (Pair) x;
    Pair yp = (Pair) y;
    double xv = ((Double) xp.getSecond()).doubleValue();
    double yv = ((Double) yp.getSecond()).doubleValue();
    if (xv<yv) return -1;
    else if (xv>yv) return 1;
    return 0;
  }
}


class LinkComp implements Comparator, java.io.Serializable {
  public int compare(Object x, Object y) {
    Link xp = (Link) x;
    Link yp = (Link) y;
    double xv = xp.getWeight();
    double yv = yp.getWeight();
    if (xv < yv)return -1;
    else if (xv > yv)return 1;
    return 0;
  }
}

