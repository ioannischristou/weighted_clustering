package coarsening;

public class GraphException extends Exception {
  public GraphException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
