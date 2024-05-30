package partitioning;

import java.util.Vector;
import java.util.Arrays;
import clustering.DocumentDistIntf;
import clustering.Document;


public class SCPMakerHelper {
  private Vector _docs;
  private DocumentDistIntf _m;
  private boolean _isSorted = false;
  private DD[] _array = null;

  public SCPMakerHelper(Vector docs, DocumentDistIntf m) {
    _docs = docs;
    _m = m;
    // create array
    try {
      final int docsize = docs.size();
      final int ddsize = docsize * docsize;
      _array = new DD[ddsize];
      int k = 0;
      for (int i = 0; i < docsize; i++) {
        Document di = (Document) _docs.elementAt(i);
        for (int j = 0; j < docsize; j++) {
          Document dj = (Document) _docs.elementAt(j);
          _array[k++] = new DD(i, j, _m.dist(di, dj));
        }
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
   * return the Vector<Integer> containing the indices of the k nearest neighbors
   * of the Document indexed in _docs by index.
   * @param index int
   * @param k int
   * @throws PartitioningException
   * @return Vector
   */
  public Vector getKNN(int index, int k) throws PartitioningException {
    if (_isSorted==false) throw new PartitioningException("array is not sorted");
    Vector res = new Vector();
    int startpos = index*_docs.size()+1;  // don't return the index itself
    for (int i=0; i<k; i++) {
      if (startpos+i>=_array.length) break;  // reached end
      DD oi = _array[startpos+i];
      if (oi._i!=index) break;  // you asked for too many nbors
      res.addElement(new Integer(oi._j));
    }
    return res;
  }


  /**
   * return the index of the k-th nearest neighbor of the index-th Document.
   * k must be > 0 and < _docs.size()-1.
   * @param index int
   * @param k int
   * @throws PartitioningException
   * @return int
   */
  public int getKthNN(int index, int k) throws PartitioningException {
    if (_isSorted==false) throw new PartitioningException("array is not sorted");
    int startpos = index*_docs.size();
    DD ok = _array[startpos+k];
    if (ok._i!=index) throw new PartitioningException("_i!=index?");
    return ok._j;
  }
}


class DD implements Comparable {
  int _i, _j;
  double _dist;

  DD(int i, int j, double dist) {
    _i = i; _j = j; _dist = dist;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      DD dd = (DD) o;
      if (_i==dd._i && _dist==dd._dist) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return _i;
  }

  public int compareTo(Object o) {
    DD c = (DD) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_i < c._i) return -1;
    else if (_i==c._i) {
      if (_dist < c._dist)return -1;
      else if (_dist == c._dist)return 0;
      else return 1;
    }
    else return 1;
  }
}
