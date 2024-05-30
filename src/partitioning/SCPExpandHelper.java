package partitioning;

import java.util.Vector;
import java.util.Arrays;
import clustering.DocumentDistIntf;
import clustering.Document;


public class SCPExpandHelper {
  private Vector _docs;
  private DocumentDistIntf _m;
  private Document _center;
  private boolean _isSorted = false;
  private DD2[] _array = null;

  public SCPExpandHelper(Vector docs, Document center, DocumentDistIntf m) {
    _docs = docs;
    _m = m;
    _center = center;
    // create array
    try {
      final int ddsize = docs.size();
      _array = new DD2[ddsize];
      for (int i = 0; i < ddsize; i++) {
        Document di = (Document) _docs.elementAt(i);
          _array[i] = new DD2(i, _m.dist(di, _center));
      }
      // sort
      Arrays.sort(_array);
      _isSorted = true;
    }
    catch (Exception e) {
      e.printStackTrace();
      // failed...
    }
  }


  /**
   * return the Vector<Integer> containing the indices of the k nearest neighbors not contained in the current set
   * @param k int
   * @param asgns int[]
   * @param c int
   * @throws PartitioningException
   * @return Vector
   */
  public Vector getKNN(int k, int asgns[], int c) throws PartitioningException {
    if (_isSorted==false) throw new PartitioningException("array is not sorted");
    Vector res = new Vector();
    int j = 0;
    for (int i=0; i<_array.length; i++) {
      if (i>=_array.length || j>=k) break;  // reached end
      DD2 oi = _array[i];
      if (asgns[oi._i]!=c) {
        j++;
        res.addElement(new Integer(oi._i));
      }
    }
    return res;
  }


  /**
   * return the index of the k-th nearest neighbor.
   * k must be > 0 and < _docs.size()-1.
   * @param index int
   * @param k int
   * @throws PartitioningException
   * @return int
   */
  public int getKthNN(int k) throws PartitioningException {
    if (_isSorted==false) throw new PartitioningException("array is not sorted");
    DD2 ok = _array[k];
    return ok._i;
  }
}


class DD2 implements Comparable {
  int _i;
  double _dist;

  DD2(int i, double dist) {
    _i = i; _dist = dist;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      DD2 dd = (DD2) o;
      if (_dist==dd._dist) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return (int) Math.floor(_dist);
  }


  public int compareTo(Object o) {
    DD2 c = (DD2) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
      if (_dist < c._dist)return -1;
      else if (_dist == c._dist)return 0;
      else return 1;
  }
}
