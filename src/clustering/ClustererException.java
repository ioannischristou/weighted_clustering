package clustering;

public class ClustererException extends Exception {
  public ClustererException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}

