package clustering;

public interface ClustererTermination {
  public boolean isDone();
  public void registerClustering(Clusterer problem);
}
