package utils;

public class MPSTest {
  public MPSTest() {
  }

  public static void main(String args[]) {
    cern.colt.matrix.DoubleMatrix2D A = new cern.colt.matrix.impl.SparseDoubleMatrix2D(2, 3);
    A.set(0, 0, 1);
    A.set(0, 2, 1);
    A.set(1, 1, 1);
    A.set(1, 2, 1);
    cern.colt.matrix.DoubleMatrix1D c = new cern.colt.matrix.impl.SparseDoubleMatrix1D(2);
    c.set(0, 1.23e-4);
    c.set(1, 1.05e-3);
    try {
      utils.DataMgr.writeSCP2MPSFileFast(A, c, 2, "mitsos.mps");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
