package onedclustering;

import java.util.Vector;

public class SolverWSvc {
  public SolverWSvc() {
  }


  /**
   * the method solves the number clustering problem and returns
   * a Vector<Integer> result where result[i] indicates to which
   * cluster from [1...k] the numbers[i] element belongs to.
   * Last element of result vector is value of partition obtained.
   * If the problem is infeasible, null is returned.
   * @param numbers Vector<Double> not necessarily sorted, size let's say n
   * @param k int
   * @param p double
   * @return Vector<Integer>, size n+1, last element is Double, the value of the
   * partition
   */
  public Vector solve(Vector numbers, int k, double p) {
    if (numbers==null || numbers.size()==0) return null;
    double[] arr = new double[numbers.size()];
    for (int i=0; i<arr.length; i++) arr[i] = ((Double) numbers.elementAt(i)).doubleValue();
    Params params = new Params(arr, p, k);
    Vector result;
    Solver s = new Solver(params);
    try {
      double v = s.solveDP2ParallelMat(2);  // use 2 threads
      result = s.getSolutionIndices();
      result.addElement(new Double(v));  // last element is value of partition
    }
    catch (CException e) {
      e.printStackTrace();
      return null;
    }
    return result;
  }


}
