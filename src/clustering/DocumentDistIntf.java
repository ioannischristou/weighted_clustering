package clustering;

public interface DocumentDistIntf {
  public double dist(Document x, Document y) throws ClustererException;
  public double dotproduct(Document x, Document y) throws ClustererException;
  public double norm(Document x) throws ClustererException;
}
