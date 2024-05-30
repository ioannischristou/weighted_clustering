package clustering;

import java.util.*;

public class DocumentDistL1 extends DocumentDist {
  public DocumentDistL1() {
  }


  /**
   * thread-safe version of dist(x,y) method, utilizing the entrySet()'s
   * iterators.
   * @param x Document
   * @param y Document
   * @throws ClustererException
   * @return double
   */
  public double dist(Document x, Document y) throws ClustererException {
    if (x.getDim()!=y.getDim())
      throw new ClustererException("DocumentDistL1: docs of dif. size");
    Iterator x_iter = x.entrySetIterator();  // returns Entry.Map objects
    Iterator y_iter = y.entrySetIterator();

    double tot=0.0;
    if (x_iter.hasNext()==false) return l1dist(y_iter);
    if (y_iter.hasNext()==false) return l1dist(x_iter);
    Map.Entry x_pair = (Map.Entry) x_iter.next();
    Map.Entry y_pair = (Map.Entry) y_iter.next();
    int x_pos = ( (Integer) x_pair.getKey()).intValue();
    int y_pos = ( (Integer) y_pair.getKey()).intValue();
    double x_val = ( (Double) x_pair.getValue()).doubleValue();
    double y_val = ( (Double) y_pair.getValue()).doubleValue();
    while (true) {
      if (x_pos == y_pos) {
        tot += Math.abs(x_val - y_val);
        // advance both x and y
        if (x_iter.hasNext() == false) {
          return tot + l1dist(y_iter); // done
        }
        if (y_iter.hasNext() == false) {
          return tot + l1dist(x_iter); // done
        }
        // advance both x_pair and y_pair
        x_pair = (Map.Entry) x_iter.next();
        y_pair = (Map.Entry) y_iter.next();
        x_pos = ( (Integer) x_pair.getKey()).intValue();
        y_pos = ( (Integer) y_pair.getKey()).intValue();
        x_val = ( (Double) x_pair.getValue()).doubleValue();
        y_val = ( (Double) y_pair.getValue()).doubleValue();
      }
      else if (x_pos < y_pos) {
        tot += Math.abs(x_val);
        // advance x
        if (x_iter.hasNext() == false) {
          return tot + l1dist(y_iter); // done
        }
        x_pair = (Map.Entry) x_iter.next();
        x_pos = ( (Integer) x_pair.getKey()).intValue();
        x_val = ( (Double) x_pair.getValue()).doubleValue();
      }
      else { // x_pos>y_pos
        tot += Math.abs(y_val);
        // advance y
        if (y_iter.hasNext() == false) {
          return tot + l1dist(x_iter); // done
        }
        y_pair = (Map.Entry) y_iter.next();
        y_pos = ( (Integer) y_pair.getKey()).intValue();
        y_val = ( (Double) y_pair.getValue()).doubleValue();
      }
    }
    // return tot;
  }


  /**
   * Deprecated method as it is not thread-safe for some reason...
   * @param x Document
   * @param y Document
   * @throws ClustererException
   * @return double
   */
  public double distOld(Document x, Document y) throws ClustererException {
    if (x.getDim()!=y.getDim())
      throw new ClustererException("DocumentDistL1: docs of dif. size");
    Iterator x_posit = x.positions();
    Iterator x_valsit = x.values();
    Iterator y_posit = y.positions();
    Iterator y_valsit = y.values();

    double tot=0.0;
    if (x_posit.hasNext()==false) return l1distOld(y_valsit);
    if (y_posit.hasNext()==false) return l1distOld(x_valsit);
    int x_pos = ((Integer) x_posit.next()).intValue();
    int y_pos = ((Integer) y_posit.next()).intValue();
    double x_val = ((Double) x_valsit.next()).doubleValue();
    double y_val = ((Double) y_valsit.next()).doubleValue();
    while (true) {
      if (x_pos==y_pos) {
        tot += Math.abs(x_val - y_val);
        // advance both x and y
        if (x_posit.hasNext()==false)
          return tot+l1distOld(y_valsit);  // done
        if (y_posit.hasNext()==false)
          return tot+l1distOld(x_valsit);  // done
        x_pos = ((Integer) x_posit.next()).intValue();
        x_val = ((Double) x_valsit.next()).doubleValue();
        y_pos = ((Integer) y_posit.next()).intValue();
        y_val = ((Double) y_valsit.next()).doubleValue();
      }
      else if (x_pos<y_pos) {
        // advance x
        if (x_posit.hasNext()==false) return tot+l1distOld(y_valsit);  // done
        x_pos = ((Integer) x_posit.next()).intValue();
        x_val = ((Double) x_valsit.next()).doubleValue();
      }
      else {  // x_pos>y_pos
        // advance y
        if (y_posit.hasNext()==false) return tot+l1distOld(x_valsit);;  // done
        y_pos = ((Integer) y_posit.next()).intValue();
        y_val = ((Double) y_valsit.next()).doubleValue();
      }
    }
    // return tot;
  }


  public double norm(Document d) {
    double val=0.0;
    Iterator d_valsit = d.values();
    while (d_valsit.hasNext()) {
      double v = ((Double) d_valsit.next()).doubleValue();
      val += Math.abs(v);
    }
    return val;
  }


  private double l1dist(Iterator vals) {
    double res = 0.0;
    while (vals.hasNext()) {
      Map.Entry me = (Map.Entry) vals.next();
      res += Math.abs( ( (Double) me.getValue()).doubleValue());
    }
    return res;
  }


  private double l1distOld(Iterator vals) {
    double res = 0.0;
    while (vals.hasNext())
      res += Math.abs(((Double)vals.next()).doubleValue());
    return res;
  }
}
