package onedclustering;

public class SortAux implements Comparable {
  int _i;
  double _v;

  public SortAux(int ind, double v) {
    _i = ind; _v = v;
  }

  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      SortAux s = (SortAux) o;
      if (_v==s._v) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public int hashCode() {
    return (int) Math.floor(_v);
  }

  public int compareTo(Object o) {
    SortAux c = (SortAux) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_v < c._v) return -1;
    else if (_v == c._v) return 0;
    else return 1;
  }
}
