package exactclustering;

public class ExactClusteringException extends Exception {
  public ExactClusteringException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}

