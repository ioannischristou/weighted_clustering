package clustering;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.*;

public class DocumentDensityDist extends DocumentDistL2 {
  final private static double _s2 = 1.0;
  final private static double _epsilon = 1.e+8;

  public DocumentDensityDist() {
  }


  /**
   * Returns the "density at point y exercised from point x".
   * The density-distance at y due to x decreases and goes to zero as
   * x and y become more distant
   * @param x Document
   * @param y Document
   * @return double
   */
  public double dist(Document x, Document y) throws ClustererException {
    double finalres = 0.0;
    // double ds = super.dist(x,y);
    // compute the L2 Document Distance w/o keys() and values() iterators but
    // instead from the entrySet() set and iterators
    double ds = dist2(x,y);
    // res = Math.exp(-res/(2*gets2()));
    if (ds==0.0) ds = _epsilon;
    finalres = Math.pow(ds, -1.5);  // don't drop too fast
    return finalres;
  }


  private double dist2(Document x, Document y) throws ClustererException {
    if (x.getDim()!=y.getDim())
      throw new ClustererException("DocumentDistL2: docs of dif. size");
    Iterator x_iter = x.entrySetIterator();  // returns Entry.Map objects
    Iterator y_iter = y.entrySetIterator();

    double tot=0.0;
    if (x_iter.hasNext()==false) return Math.sqrt(l2dist2(y_iter));
    if (y_iter.hasNext()==false) return Math.sqrt(l2dist2(x_iter));
    Map.Entry x_pair = (Map.Entry) x_iter.next();
    Map.Entry y_pair = (Map.Entry) y_iter.next();
    int x_pos = ( (Integer) x_pair.getKey()).intValue();
    int y_pos = ( (Integer) y_pair.getKey()).intValue();
    double x_val = ( (Double) x_pair.getValue()).doubleValue();
    double y_val = ( (Double) y_pair.getValue()).doubleValue();
    while (true) {
      if (x_pos == y_pos) {
        tot += (x_val - y_val) * (x_val - y_val);
        // advance both x and y
        if (x_iter.hasNext() == false) {
          return Math.sqrt(tot + l2dist2(y_iter)); // done
        }
        if (y_iter.hasNext() == false) {
          return Math.sqrt(tot + l2dist2(x_iter)); // done
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
        tot += (x_val*x_val);
        // advance x
        if (x_iter.hasNext() == false) {
          return Math.sqrt(tot + l2dist2(y_iter)); // done
        }
        x_pair = (Map.Entry) x_iter.next();
        x_pos = ( (Integer) x_pair.getKey()).intValue();
        x_val = ( (Double) x_pair.getValue()).doubleValue();
      }
      else { // x_pos>y_pos
        tot += (y_pos*y_pos);
        // advance y
        if (y_iter.hasNext() == false) {
          return Math.sqrt(tot + l2dist2(x_iter)); // done
        }
        y_pair = (Map.Entry) y_iter.next();
        y_pos = ( (Integer) y_pair.getKey()).intValue();
        y_val = ( (Double) y_pair.getValue()).doubleValue();
      }
    }
    // return tot;
  }


  private static double gets2() { return _s2; }


  private double l2dist2(Iterator vals) {
    double res = 0.0;
    while (vals.hasNext()) {
      Map.Entry me = (Map.Entry) vals.next();
      double v = Math.abs( ( (Double) me.getValue()).doubleValue());
      res += v*v;
    }
    return res;
  }

}
