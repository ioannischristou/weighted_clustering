package partitioning;

import coarsening.*;
import clustering.*;

public interface HObjFncIntf {
  double value(HGraph g, int[] partition);
  double[] values(HGraph g, int[] partition);
}
