package clustering;

import java.util.*;

public abstract class DocumentDist implements DocumentDistIntf{
  public DocumentDist() {
  }


  /**
   * thread-safe version utilizing entrySet()'s iterators
   * @param x Document
   * @param y Document
   * @throws ClustererException
   * @return double
   */
  public double dotproduct(Document x, Document y) throws ClustererException {
    if (x.getDim()!=y.getDim())
      throw new ClustererException("DocumentDist: docs of dif. size");
    Iterator x_iter = x.entrySetIterator();
    Iterator y_iter = y.entrySetIterator();

    double tot=0.0;
    if (x_iter.hasNext()==false || y_iter.hasNext()==false) return tot;
    Map.Entry xe = (Map.Entry) x_iter.next();
    Map.Entry ye = (Map.Entry) y_iter.next();
    int x_pos = ((Integer) xe.getKey()).intValue();
    int y_pos = ((Integer) ye.getKey()).intValue();
    double x_val = ((Double) xe.getValue()).doubleValue();
    double y_val = ((Double) ye.getValue()).doubleValue();
    while (true) {
      if (x_pos==y_pos) {
        tot += x_val * y_val;
        // advance both x and y
        if (x_iter.hasNext()==false || y_iter.hasNext()==false) break;  // done
        xe = (Map.Entry) x_iter.next();
        ye = (Map.Entry) y_iter.next();
        x_pos = ((Integer) xe.getKey()).intValue();
        x_val = ((Double) xe.getValue()).doubleValue();
        y_pos = ((Integer) ye.getKey()).intValue();
        y_val = ((Double) ye.getValue()).doubleValue();
      }
      else if (x_pos<y_pos) {
        // advance x
        if (x_iter.hasNext()==false) break;  // done
        xe = (Map.Entry) x_iter.next();
        x_pos = ((Integer) xe.getKey()).intValue();
        x_val = ((Double) xe.getValue()).doubleValue();
      }
      else {  // x_pos>y_pos
        // advance y
        if (y_iter.hasNext()==false) break;  // done
        ye = (Map.Entry) y_iter.next();
        y_pos = ((Integer) ye.getKey()).intValue();
        y_val = ((Double) ye.getValue()).doubleValue();
      }
    }
    return tot;
  }


  /**
   * Deprecated as it's not thread-safe for some reason...
   * @param x Document
   * @param y Document
   * @throws ClustererException
   * @return double
   */
  public double dotproductOld(Document x, Document y) throws ClustererException {
    if (x.getDim()!=y.getDim())
      throw new ClustererException("DocumentDist: docs of dif. size");
    Iterator x_posit = x.positions();
    Iterator x_valsit = x.values();
    Iterator y_posit = y.positions();
    Iterator y_valsit = y.values();

    double tot=0.0;
    if (x_posit.hasNext()==false || y_posit.hasNext()==false) return tot;
    int x_pos = ((Integer) x_posit.next()).intValue();
    int y_pos = ((Integer) y_posit.next()).intValue();
    double x_val = ((Double) x_valsit.next()).doubleValue();
    double y_val = ((Double) y_valsit.next()).doubleValue();
    while (true) {
      if (x_pos==y_pos) {
        tot += x_val * y_val;
        // advance both x and y
        if (x_posit.hasNext()==false || y_posit.hasNext()==false) break;  // done
        x_pos = ((Integer) x_posit.next()).intValue();
        x_val = ((Double) x_valsit.next()).doubleValue();
        y_pos = ((Integer) y_posit.next()).intValue();
        y_val = ((Double) y_valsit.next()).doubleValue();
      }
      else if (x_pos<y_pos) {
        // advance x
        if (x_posit.hasNext()==false) break;  // done
        x_pos = ((Integer) x_posit.next()).intValue();
        x_val = ((Double) x_valsit.next()).doubleValue();
      }
      else {  // x_pos>y_pos
        // advance y
        if (y_posit.hasNext()==false) break;  // done
        y_pos = ((Integer) y_posit.next()).intValue();
        y_val = ((Double) y_valsit.next()).doubleValue();
      }
    }
    return tot;
  }
}
