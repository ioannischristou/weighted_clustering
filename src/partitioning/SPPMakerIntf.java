package partitioning;

import coarsening.HGraph;
import java.util.Vector;
import java.util.Hashtable;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public interface SPPMakerIntf {
  // create/update method
  /**
   * creates an SPP problem to choose among the partition bloks the k ones that
   * minimize the Obj Function described in obj
   * @param g HGraph
   * @param partitions Vector Vector<int[] partition>
   * @param k int
   * @param obj HObjFncIntf
   * @throws PartitioningException
   */
  public void createSPP(HGraph g, Vector partitions, int k, HObjFncIntf obj) throws PartitioningException;
  // getter methods
  public DoubleMatrix2D getConstraintsMatrix() throws PartitioningException;
  public DoubleMatrix1D getCostVector() throws PartitioningException;
  // auxiliary methods
  public void setParam(String param, Object val) throws PartitioningException;
  public void addParams(Hashtable params) throws PartitioningException;
  public Object getParam(String param) throws PartitioningException;
}

