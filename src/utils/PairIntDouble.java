package utils;

public class PairIntDouble implements Comparable {
  int _key;
  double _val;

  public PairIntDouble(int i, double val) {
    _key = i;  _val = val;
  }


  public int getInt() {
    return _key;
  }

  public double getDouble() {
    return _val;
  }


  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      PairIntDouble dd = (PairIntDouble) o;
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
    PairIntDouble c = (PairIntDouble) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    if (_val < c._val) return -1;
    else if (_val == c._val) return 0;
    else return 1;
  }
}
