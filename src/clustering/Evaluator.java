package clustering;

public interface Evaluator {
  public double eval(Clusterer cl) throws ClustererException;
}

