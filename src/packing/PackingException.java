package packing;

public class PackingException extends Exception {
  public PackingException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}

