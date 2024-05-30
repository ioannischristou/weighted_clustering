package utils;

public class Pair {
  private Object _first;
  private Object _second;

  public Pair(Object first, Object second) {
    _first = first;
    _second = second;
  }

  public Object getFirst() { return _first; }
  public void setFirst(Object f) { _first = f; }
  public Object getSecond() { return _second; }
  public void setSecond(Object s) { _second = s; }
}

