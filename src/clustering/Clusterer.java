package clustering;

import java.util.*;

public interface Clusterer {
  // public Clusterer newClusterer() throws ClustererException;
  public void addDocument(Document d) throws ClustererException;
  public void addAllDocuments(Vector docs);
  public void setParams(Hashtable params);
  public Hashtable getParams();

  public void setInitialClustering(Vector centers) throws ClustererException;

  public void reset();

  public Vector getCurrentCenters();

  public Vector getCurrentDocs();

  /**
   * cluster the document vectors and return the Vector<Document>
   * with the cluster centers.
   * @return Vector
   */
  public Vector clusterDocs() throws Exception;


  /**
   * return the clustering as a partition. Must be called after clusterDocs().
   * @return int[] an array of size=all docs entered, array values in [1...centers.size()]
   */
  public int[] getClusteringIndices() throws ClustererException;


  /**
   * set the Clusterer asgnmnt indices to the specified array.
   * Should not throw even if null array is passed in.
   * @param asgn int[]
   * @throws Exception
   */
  public void setClusteringIndices(int[] asgn) throws ClustererException;


  /**
   * return an array of size equal to centers.size(), showing for each
   * cluster i, the number of documents assigned to it.
   * Throws if clusterDocs() has not been called.
   * @throws ClustererException
   * @return int[]
   */
  public int[] getClusterCards() throws ClustererException;


  /**
   * evaluate the current clustering according to the Evaluator passed in
   * @param ev Evaluator
   * @throws Exception
   * @return double
   */
  public double eval(Evaluator ev) throws ClustererException;


  /**
   * return in a Vector<HashSet cluster> some or all of the clusters produced
   * during the run of the clustering algorithm. May also choose to throw
   * an exception instead, so any calling code must guard for ClustererException
   * @throws ClustererException
   * @return Vector
   */
  public Vector getIntermediateClusters() throws ClustererException;

}
