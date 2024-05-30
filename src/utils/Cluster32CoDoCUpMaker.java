package utils;

import java.io.IOException;
import java.util.Vector;
import clustering.ClustererException;
import clustering.Document;

public class Cluster32CoDoCUpMaker {
  public Cluster32CoDoCUpMaker() {
  }

  public static void main(String[] args) throws IOException, ClustererException {
    String infile = args[0];
    String outfile = args[1];
    Vector docs = DataMgr.readDocsFromCluster3File(infile);
    int dims = ((Document) docs.elementAt(0)).getDim();
    DataMgr.writeDocumentsToFile(docs, dims, outfile);
    return;
  }
}
