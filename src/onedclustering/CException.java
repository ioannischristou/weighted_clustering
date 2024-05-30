package onedclustering;

public class CException extends Exception {
  public CException(String msg) {
    System.err.println(msg);
  }
}
