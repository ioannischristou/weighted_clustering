package partitioning;

import clustering.DocumentDistIntf;
import java.util.Vector;
import java.util.Hashtable;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public interface SCPMakerIntf {

  public void createSCP(Vector docs, Vector clusters, int k, double theta, 
                        int maxpointset, DocumentDistIntf m, 
                        double[] weights) throws PartitioningException;
  public DoubleMatrix2D getConstraintsMatrix() throws PartitioningException;
  public DoubleMatrix1D getCostVector() throws PartitioningException;
  public int[] convertSolution(int[] scpsolution, double[] weights) throws PartitioningException;
  public int[] convertSolution(double[] scpsolution) throws PartitioningException;
  public void addOverExistingColumns(Vector clusters, int[] scpsolution, double[] weights) throws PartitioningException;
  public void addOverExistingColumns(Vector clusters, double[] scpsolution) throws PartitioningException;
  public Vector expand(Vector centers, int asngmts[], int expansionsize) throws PartitioningException;
  public void setParam(String param, Object val) throws PartitioningException;
  public void addParams(Hashtable params) throws PartitioningException;
  public Object getParam(String param) throws PartitioningException;
}
