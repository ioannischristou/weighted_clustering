package utils;

public class MLException extends Exception {
  public MLException(String msg) {
    System.err.println(msg);
  }
}

