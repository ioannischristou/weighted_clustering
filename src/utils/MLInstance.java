package utils;

import java.util.*;

public class MLInstance {
  private Vector _vattrs;  // Vector<Object>, Object can be Number or String only
  private Set _numericAttrsPositions;  // Set<Integer>
  private String _classLabel;

  public MLInstance() {
    _vattrs = new Vector();
    _numericAttrsPositions = new TreeSet();  // maintain order in iterators
    _classLabel = null;
  }

  public MLInstance(Vector vattrs, Collection numericattrpositions, String classlabel) {
    _vattrs = new Vector(vattrs);
    _numericAttrsPositions = new TreeSet(numericattrpositions);
    _classLabel = classlabel;
  }

  public Object getAttrValueAt(int ind) {
    return _vattrs.elementAt(ind);
  }


  public String getClassLabel() { return _classLabel; }
  public void setClassLabel(String cl) { _classLabel = cl; }

  public void addAttrValue(Object obj, boolean is_numeric) {
    _vattrs.addElement(obj);
    if (is_numeric) _numericAttrsPositions.add(new Integer(_vattrs.size()-1));
  }

  Set getNumericAttrInds() { return _numericAttrsPositions; }

  public boolean isNumericValueAt(int ind) {
    return _numericAttrsPositions.contains(new Integer(ind));
  }

  public Number getNumericAttrValueAt(int ind) throws MLException {
    if (_numericAttrsPositions.contains(new Integer(ind))==false)
      throw new MLException("not a numeric value");
    return (Number) _vattrs.elementAt(ind);
  }

  public String getStringAttrValueAt(int ind) throws MLException {
    if (_numericAttrsPositions.contains(new Integer(ind))==true)
      throw new MLException("it is a numeric value");
    return (String) _vattrs.elementAt(ind);
  }


  /**
   * get number of common non-numeric, non-null attributes that have the
   * same value between this instance and the argument instance.
   * @param inst MLInstance
   * @return int
   */
  public int getNumCommonValsWith(MLInstance inst) {
    int r = 0;
    for (int i=0; i<_vattrs.size(); i++) {
      if (isNumericValueAt(i)) continue;
      String val = (String) _vattrs.elementAt(i);
      if (val==null) continue;
      String instval = (String) inst._vattrs.elementAt(i);
      if (val.equals(instval)) r++;
    }
    return r;
  }


  public String toString() {
    String res = "[";
    for (int i=0; i<_vattrs.size(); i++) {
      if (_vattrs.elementAt(i)==null) continue;
      res += _vattrs.elementAt(i);
      if (isNumericValueAt(i)) res += " (n)";
      if (i<_vattrs.size()-1) res += ", ";
    }
    res += " lbl="+_classLabel;
    res += "]";
    return res;
  }
}

