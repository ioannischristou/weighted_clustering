package partitioning;

import coarsening.*;
import clustering.*;

public interface ObjFnc {
  double value(Graph g, Document[] docs, int[] partition);
  double[] values(Graph g, int[] partition);
}
