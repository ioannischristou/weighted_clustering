package utils;

import java.util.*;
import clustering.Document;
// colt Linear Algebra library
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.SingularValueDecomposition;

public class Docs2MatrixConverter {
  private Vector _docs;  // Vector<Document>

  public Docs2MatrixConverter(Vector docs) {
    _docs = docs;
  }


  /**
   * convert a Vector of Document objects stored in _docs to matrix
   * representation for SVD and PCA analysis using the jlapack library routine
   * org.netlib.lapack.DGGSVD
   * For more information see the documentation of jlapack
   * Each column is a Document, first column is the first Document etc.
   * @return double[][]
   */
  double[][] getDenseMatrix() {
    int cols = _docs.size();
    int rows = ((Document) _docs.elementAt(0)).getDim();
    double[][] matrix = new double[rows][cols];
    for (int j=0; j<cols; j++) {
      // fill in the j-th column
      Document dj = (Document) _docs.elementAt(j);
      for (int i=0; i<rows; i++) {
        Double v = dj.getDimValue(new Integer(i));
        if (v!=null) matrix[i][j] = v.doubleValue();
        else matrix[i][j] = 0;
      }
    }
    return matrix;
  }


  public int getNumRows() {
    return ((Document) _docs.elementAt(0)).getDim();
  }


  public int getNumCols() {
    return _docs.size();
  }


  /**
   * this method returns a matrix whose j-th col is the j-th Document
   * in the vector _docs
   * @return DoubleMatrix2D
   */
  public DoubleMatrix2D getSparseMatrix() {
    int cols = _docs.size();
    int rows = ((Document) _docs.elementAt(0)).getDim();
    DoubleMatrix2D matrix = new SparseDoubleMatrix2D(rows, cols);
    for (int j=0; j<cols; j++) {
      Document dj = (Document) _docs.elementAt(j);
      Iterator it = dj.positions();
      while (it.hasNext()) {
        Integer i = (Integer) it.next();
        Double v = dj.getDimValue(i);
        matrix.setQuick(i.intValue(), j, v.doubleValue());
      }
    }
    return matrix;
  }


  /**
   * this method returns a matrix whose i-th row is the i-th Document
   * in the vector _docs
   * @return DoubleMatrix2D
   */
  public DoubleMatrix2D getSparseMatrixTranspose() {
    int rows = _docs.size();
    int cols = ((Document) _docs.elementAt(0)).getDim();
    DoubleMatrix2D matrix = new SparseDoubleMatrix2D(rows, cols);
    for (int i=0; i<rows; i++) {
      Document dj = (Document) _docs.elementAt(i);
      Iterator it = dj.positions();
      while (it.hasNext()) {
        Integer j = (Integer) it.next();
        Double v = dj.getDimValue(j);
        matrix.setQuick(i, j.intValue(), v.doubleValue());
      }
    }
    return matrix;
  }


  /**
   * Compute the SVD of the matrix of Documents contained in _docs.
   * A = U S V'
   * The documents give rise to the matrix A[M x N] where N is the #_docs, and
   * M is the dimension of each doc.
   * Return the P[N x r] matrix where r is the rank of A, where the P[i,j] element
   * of the matrix is the d_i^T (l_r u_r) where u_r is the r-th column of U
   * Essentially P = A' U where we only take the first r columns from P
   * The returned matrix has columns, each of which is an array depicting the
   * projection of Documents along the singular dimension 1...r It is these
   * arrays we want to cluster then!!!
   * @param singularvalues double[] output variable that will contain the
   * singular values of the decomposition
   * @return DoubleMatrix2D
   */
  public DoubleMatrix2D getProjectionOnSingularDimensions(double[] singularvalues) {
    final boolean work_with_transpose = getNumRows() < getNumCols();
/*
    DoubleMatrix2D A = work_with_transpose==false ?
        getSparseMatrix() : getSparseMatrixTranspose();
*/
    DoubleMatrix2D A = getSparseMatrix();
    final int N = getNumCols();
    final int M = getNumRows();
    DoubleMatrix2D AT = A.viewDice().copy();
    if (work_with_transpose==false) {
      // work with A
      SingularValueDecomposition svd = new SingularValueDecomposition(A);
      double[] singularvalues_aux = svd.getSingularValues();
      int sz = singularvalues.length;
      for (int i=0; i<sz; i++) singularvalues[i] = singularvalues_aux[i];
      DoubleMatrix2D U = svd.getU();
      int r = svd.rank();
/*
      DoubleMatrix2D US = new DenseDoubleMatrix2D(M,N);
      U.zMult(S, US);
*/
      DoubleMatrix2D P = new DenseDoubleMatrix2D(N,N);
      AT.zMult(U, P);
      // we now have P[1...N] but we only care about it first r columns
      return P.viewPart(0, 0, N, r);
    }
    else {
      // work with A'
      SingularValueDecomposition svd = new SingularValueDecomposition(AT);
      double[] singularvalues_aux = svd.getSingularValues();
      int sz = singularvalues.length;
      for (int i=0; i<sz; i++) singularvalues[i] = singularvalues_aux[i];
      DoubleMatrix2D V = svd.getV();
      int r = svd.rank();
/*
      DoubleMatrix2D VS = new DenseDoubleMatrix2D(M,M);
      V.zMult(S, VS);
 */
      DoubleMatrix2D P = new DenseDoubleMatrix2D(N,M);
      AT.zMult(V, P);
      // we now have P[1...M] but we only care about it first r columns
      return P.viewPart(0, 0, N, r);
    }
  }


  /**
   * Compute the SVD of the covariance matrix of Documents contained in _docs.
   * A = U S V'
   * The documents give rise to the matrix A[M x N] where N is the #_docs, and
   * M is the dimension of each doc.
   * Return the P[N x r] matrix where r is the rank of A'A, where the P[i,j] element
   * of the matrix is the d_i^T (l_r u_r) where u_r is the r-th column of U
   * Essentially P = A'A U where we only take the first r columns from P
   * The returned matrix has columns, each of which is an array depicting the
   * projection of Documents along the singular dimension 1...r It is these
   * arrays we want to cluster then!!!
   * @param singularvalues double[] output variable that will contain the
   * singular values of the decomposition
   * @return DoubleMatrix2D
   */
  public DoubleMatrix2D getCovarianceProjectionOnSingularDimensions(double[] singularvalues) {
    final int N = getNumCols();
    // final int M = getNumRows();
    DoubleMatrix2D Aaux = getSparseMatrix();
    DoubleMatrix2D AauxT = Aaux.viewDice().copy();
    DoubleMatrix2D A = new DenseDoubleMatrix2D(N,N);
    AauxT.zMult(Aaux,A);
    SingularValueDecomposition svd = new SingularValueDecomposition(A);
    double[] singularvalues_aux = svd.getSingularValues();
    int sz = singularvalues.length;
    for (int i=0; i<sz; i++) singularvalues[i] = singularvalues_aux[i];
    DoubleMatrix2D U = svd.getU();
    int r = svd.rank();
    DoubleMatrix2D P = new DenseDoubleMatrix2D(N,N);
    A.zMult(U, P);
    // we now have P[1...N] but we only care about it first r columns
    return P.viewPart(0, 0, N, r);
  }


  /**
   * test driver program for colt or jlapack SVD
   * @param args String[]
   */
  public static void main(String[] args) {
    String docsfile = null;
    if (args==null || args.length==0) {
      docsfile = "testdata/www.ait.gr/out_Index.txt";
    }
    else docsfile = args[0];
    try {
      Vector docs = DataMgr.readDocumentsFromFile(docsfile);
      final int m = ((Document) docs.elementAt(0)).getDim();
      Docs2MatrixConverter c = new Docs2MatrixConverter(docs);
      long st = System.currentTimeMillis();
      double svs[] = new double[m];
      DoubleMatrix2D P = c.getProjectionOnSingularDimensions(svs);
      System.out.println("total time="+(System.currentTimeMillis()-st)+" msecs.");
      System.out.flush();
      System.out.println(P);
/*
      DoubleMatrix2D A;
      if (docs.size()<=((Document) docs.elementAt(0)).getDim())
        A = c.getSparseMatrix();
      else A = c.getSparseMatrixTranspose();
      // check out colt
      long st = System.currentTimeMillis();
      SingularValueDecomposition svd = new SingularValueDecomposition(A);
      DoubleMatrix2D S = svd.getS();
      DoubleMatrix2D U = svd.getU();
      DoubleMatrix2D V = svd.getV();
      System.out.println("COLT Done. Matrix rank is "+svd.rank());
      System.out.println("total time="+(System.currentTimeMillis()-st)+" msecs.");

      // now check out jlapack
      double[][] a = c.getDenseMatrix();
      DGESVD lapack_svd = new DGESVD();
      int m = c.getNumRows();
      int n = c.getNumCols();
      double s[] = new double[m>=n ? n : m];
      double u[][] = new double[m][m];
      double vt[][] = new double[n][n];
      int lwork = c.getLWork();
      double work[] = new double[lwork];
      st = System.currentTimeMillis();
      lapack_svd.DGESVD("A", "A", m, n, a, s, u, vt, work, lwork, new intW(0));
      System.out.println("LAPACK total time="+(System.currentTimeMillis()-st)+" msecs.");
*/
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
