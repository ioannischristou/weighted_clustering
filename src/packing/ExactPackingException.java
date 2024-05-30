package packing;

public class ExactPackingException extends Exception {
  public ExactPackingException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}

