package clustering;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.Vector;

public class Document {
  private TreeMap _elems;  // TreeMap<Integer dimno, Double val>
                           // using a TreeMap saves space as most documents
                           // only have a few non-zero coordinates
  private int _dim;
  private static DocumentDistIntf _m=null;  // distance function used

  private static double[][] _dists=null;  // distances among documents


  public Document(TreeMap e, int dims) {
    _elems = e;
    _dim = dims;
  }


  /**
   * copy ctor
   * @param d Document
   */
  public Document(Document d) {
    _elems = new TreeMap(d._elems);
    _dim = d._dim;
  }


  public Iterator positions() { return _elems.keySet().iterator(); }
  public Iterator values() { return _elems.values().iterator(); }
  public Iterator entrySetIterator() { return _elems.entrySet().iterator(); }


  public int getDim() { return _dim; }


  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      Document d = (Document) o;
      this.hashCode();
      return _elems.equals(d._elems);
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  public int hashCode() {
    return _elems.hashCode();
  }


  public void addMul(double h, Document d) {
    Iterator d_pos = d.positions();
    Iterator d_vals = d.values();
    while (d_pos.hasNext()) {
      Integer pos = (Integer) d_pos.next();
      double tval = h*((Double) d_vals.next()).doubleValue();
      Double my_val = (Double) _elems.get(pos);
      double new_val = tval;
      if (Math.abs(new_val)<=1.e-12) continue;
      if (my_val!=null) {
        new_val += my_val.doubleValue();
        if (Math.abs(new_val)>=1.e-12) {
          my_val = new Double(new_val);
          _elems.put(pos, my_val);  // must do the update
        }
        else {  // dimension is essentially zero, remove
          _elems.remove(pos);
        }
      }
      else {  // didn't exist, add dim now
        _elems.put(pos, new Double(new_val));
      }
    }
  }


  /**
   * implement vector/h vector division operation.
   * @param h double
   * @throws ClustererException
   */
  public void div(double h) throws ClustererException {
    if (Math.abs(h)<1.e-15)
      throw new ClustererException("Document division by zero?");
    Iterator d_pos = positions();
    Iterator d_vals = values();
    while (d_pos.hasNext()) {
      Integer pos = (Integer) d_pos.next();
      Double my_val = (Double) d_vals.next();
      double nval=0;
      if (my_val!=null) {
        nval = my_val.doubleValue()/h;
      }
      _elems.put(pos,new Double(nval));
    }
  }


  public void setDimValue(Integer dim, double val) throws ClustererException {
    if (dim.intValue()<0 || dim.intValue()>=_dim)
      throw new ClustererException("dim="+dim+" _dims="+_dim+" val="+val);
    // OK, set value
    _elems.put(dim, new Double(val));
  }


  /**
   * get the value along dim dimension. dim ranges in [0, ... ,_dim )
   * @param dim Integer
   * @return Double
   */
  public Double getDimValue(Integer dim) {
    return (Double) _elems.get(dim);
  }


  public boolean isEmpty() { return _elems.size()==0; }


  public static void setMetric(DocumentDistIntf m) {
    _m = m;
  }


  public static DocumentDistIntf getMetric() { return _m; }


  public static double d(Document x, Document y) throws ClustererException {
    return _m.dist(x,y);
  }


  public String toString() {
    String ret="[dim="+_dim+", vals=";
    Iterator posit = positions();
    while (posit.hasNext()) {
      Integer pos = (Integer) posit.next();
      Double val = (Double) _elems.get(pos);
      ret += "("+pos+","+val+") ";
    }
    ret += "]";
    return ret;
  }


  /**
   * return a Document that represents the centroid of the Documents in docs. If
   * the wgts parameter is not-null, then the centroid is weighted.
   * @param docs Vector  // Vector&lt;Document&gt;
   * @param wgts double[] may be null
   * @return Document
   * @throws ClustererException
   */
  public static Document getCenter(Vector docs, double[] wgts) 
    throws ClustererException {
    if (docs==null || docs.size()==0) 
      throw new ClustererException("empty docs collection");
    final int n = docs.size();
    if (wgts!=null && n!=wgts.length)
      throw new ClustererException("weights and docs sizes dont' match");
    int dims = ((Document) docs.elementAt(0)).getDim();
    Document center = new Document(new TreeMap(), dims);
    double tot_wgt = 0.0;
    for (int i=0; i<n; i++) {
      Document di = (Document) docs.elementAt(i);
      double wi = wgts==null ? 1.0 : wgts[i];
      tot_wgt += wi;
      Iterator di_poss = di.positions();
      while (di_poss.hasNext()) {
        Integer di_dim = (Integer) di_poss.next();
        Double di_dim_val = di.getDimValue(di_dim);
        Double center_val = center.getDimValue(di_dim);
        if (center_val == null) {
          center.setDimValue(di_dim, wi*di_dim_val.doubleValue());
        }
        else {
          double newval = wi*di_dim_val.doubleValue()+center_val.doubleValue();
          center.setDimValue(di_dim, newval);
        }
      }
    }
    /*
    // finally divide all dims by docs.size()
    int docs_size = docs.size();
    Iterator keys_iter = center.positions();
    while (keys_iter.hasNext()) {
      Integer key = (Integer) keys_iter.next();
      Double val = (Double) center.getDimValue(key);
      double val1 = val.doubleValue()/docs_size;
      center.setDimValue(key, val1);
    }
    */
    center.div(tot_wgt);
    return center;
  }


  /**
   * compute the cluster centers of this clustering described in the args
   * the clusterindices[] values range from [0...num_clusters-1].
   * @param docs Document[]
   * @param clusterindices int[]
   * @param k int
   * @param weights double[] maybe null
   * @throws ClustererException
   * @return Vector  // Vector&lt;Document&gt;
   */
  public static Vector getCenters(Document[] docs, int[] clusterindices, int k,
                                  double[] weights)
      throws ClustererException {
    if (weights!=null && weights.length != docs.length)
        throw new ClustererException("weights.length!=docs.length");
    final int dims = docs[0].getDim();
    final int docs_size = docs.length;
    Vector centers = new Vector();  // Vector<Document>
    for (int i=0; i<k; i++)
      centers.addElement(new Document(new TreeMap(), dims));
    double[] cards = new double[k];
    for (int i=0; i<k; i++) cards[i]=0.0;

    for (int i=0; i<docs_size; i++) {
      int ci = clusterindices[i];
      Document centeri = (Document) centers.elementAt(ci);
      Document di = docs[i];
      if (weights==null) {
        centeri.addMul(1.0, di);
        cards[ci]++;
      } else {
          centeri.addMul(weights[i], di);
          cards[ci] += weights[i];
      }
    }
    // divide by cards
    for (int i=0; i<k; i++) {
      Document centeri = (Document) centers.elementAt(i);
      centeri.div(cards[i]);
    }
    return centers;
  }


  /**
   * compute the cluster centers of this clustering described in the args
   * the clusterindices[] values range from [0...num_clusters-1].
   * @param docs Vector  // Vector&lt;Document&gt;
   * @param clusterindices int[]
   * @param k int
   * @param weights double[] may be null
   * @throws ClustererException
   * @return Vector  // Vector&lt;Document&gt;
   */
  public static Vector getCenters(Vector docs, int[] clusterindices, int k, 
                                  double[] weights)
      throws ClustererException {
    if (weights!=null && weights.length!=docs.size()) 
      throw new ClustererException("docs and weights lengths don't match");
    final int dims = ((Document) docs.elementAt(0)).getDim();
    final int docs_size = docs.size();
    Vector centers = new Vector();  // Vector<Document>
    for (int i=0; i<k; i++)
      centers.addElement(new Document(new TreeMap(), dims));
    double[] cards = new double[k];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<docs_size; i++) {
      int ci = clusterindices[i];
      if (ci<0 || ci >= centers.size()) {
          throw new ClustererException("clusterindices["+i+"]="+ci+
                                       " centers.size()="+centers.size());
      }
      Document centeri = (Document) centers.elementAt(ci);
      Document di = (Document) docs.elementAt(i);
      if (weights==null) {
        centeri.addMul(1.0, di);
        cards[ci]++;
      } else {
          centeri.addMul(weights[i], di);
          cards[ci] += weights[i];
      }
    }
    // divide by cards
    for (int i=0; i<k; i++) {
      Document centeri = (Document) centers.elementAt(i);
      centeri.div(cards[i]);
    }
    return centers;
  }


  /**
   * return a Vector of two elements, containing as first element the Document
   * representing the center of the vector of Documents passed in and as second
   * element a Double representing the cost of this cluster measured by the
   * metric passed in.
   * @param docs Vector  // Vector&lt;Document&gt;
   * @param metric DocumentDistIntf 
   * @param weights double[] may be null
   * @throws ClustererException
   * @return Vector
   */
  public static Vector getCenterCosted(Vector docs, DocumentDistIntf metric,
                                       double[] weights)
      throws ClustererException {
    Vector results = new Vector();
    try {
      final int docsize = docs.size();
      if (weights!=null && docsize != weights.length)
        throw new ClustererException("docs and weights sizes don't match");
      if (metric==null) metric = _m;
      Document center = getCenter(docs, weights);
      results.addElement(center);
      // compute cost
      double cost = 0;
      for (int i=0; i<docsize; i++) {
        Document di = (Document) docs.elementAt(i);
        if (weights==null) cost += metric.dist(center, di);
        else {
          cost += metric.dist(center, di)*weights[i];
        }
      }
      Double d = new Double(cost);
      results.addElement(d);
      return results;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("getCenterCosted(): failed");
    }
  }


  /**
   * return the Document in locations that minimizes the sum of distances of 
   * each Document in docs from that doc. If weights is non-null, then the 
   * returned Document minimizes the sum of the weighted distances.
   * @param docs Vector  // Vector&lt;Document&gt;
   * @param locations Vector  // Vector&lt;Document&gt;
   * @param metric DocumentDistIntf
   * @param weights double[] may be null
   * @return Document
   * @throws ClustererException
   */
  public static Document getDocumentCenter(Vector docs, 
                                           Vector locations, 
                                           DocumentDistIntf metric,
                                           double[] weights) 
    throws ClustererException {
    final int n = locations.size();
    final int m = docs.size();
    if (weights!=null && m!=weights.length) 
        throw new ClustererException("weights and docs sizes don't match");
    if (weights!=null && !(metric instanceof DocumentDistL2Sqr))
        throw new ClustererException("weights can only be used "+
                                     "with L2Sqr metric");
    double dists[] = new double[n];
    int best_ind = -1;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<n; i++) {
      Document di = (Document) locations.elementAt(i);
      for (int j=0; j<m; j++) {
        Document dj = (Document) docs.elementAt(j);
        if (weights==null) dists[i] += metric.dist(di, dj);
        else dists[i] += metric.dist(di, dj)*weights[j];
      }
      if (dists[i]<best_val) {
        best_val = dists[i];
        best_ind = i;
      }
    }
    return new Document((Document) locations.elementAt(best_ind));
  }


  /**
   * return the Document in locations that minimizes the sum of distances of each
   * Document in docs from that doc.
   * docinds is a Vector<Integer>, and locations Vector<Document>
   * @param docinds Vector
   * @param locations Vector
   * @param metric DocumentDistIntf
   * @return Document
   * @throws ClustererException
   */
  public static Document getDocumentCenterInds(Vector docinds, Vector locations, DocumentDistIntf metric) throws ClustererException {
    final int n = locations.size();
    final int m = docinds.size();
    // build cache
    if (_dists==null) {
      _dists = new double[n][n];
      for (int i=0; i<n; i++) {
        Document di = (Document) locations.elementAt(i);
        for (int j=i+1; j<n; j++) {
          Document dj = (Document) locations.elementAt(j);
          _dists[i][j] = metric.dist(di, dj);
          _dists[j][i] = _dists[i][j];
        }
      }
    }

    double dists[] = new double[n];
    int best_ind = -1;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<n; i++) {
      for (int j=0; j<m; j++) {
        int j2 = ((Integer) docinds.elementAt(j)).intValue();
        dists[i] += _dists[i][j2];
      }
      if (dists[i]<best_val) {
        best_val = dists[i];
        best_ind = i;
      }
    }
    return new Document((Document) locations.elementAt(best_ind));
  }


  /**
   * return the index of the Document in locations that minimizes the sum of distances of each
   * Document in docs from that doc.
   * docinds is a Vector<Integer>, and locations Vector<Document>
   * @param docinds Vector<Integer>
   * @param locations Vector<Document>
   * @param metric DocumentDistIntf
   * @return int
   * @throws ClustererException
   */
  public static int getDocumentIndCenterInds(Vector docinds, Vector locations, DocumentDistIntf metric) throws ClustererException {
    final int n = locations.size();
    final int m = docinds.size();
    // build cache
    if (_dists==null) {
      _dists = new double[n][n];
      for (int i=0; i<n; i++) {
        Document di = (Document) locations.elementAt(i);
        for (int j=i+1; j<n; j++) {
          Document dj = (Document) locations.elementAt(j);
          _dists[i][j] = metric.dist(di, dj);
          _dists[j][i] = _dists[i][j];
        }
      }
    }

    double dists[] = new double[n];
    int best_ind = -1;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<n; i++) {
      for (int j=0; j<m; j++) {
        int j2 = ((Integer) docinds.elementAt(j)).intValue();
        dists[i] += _dists[i][j2];
      }
      if (dists[i]<best_val) {
        best_val = dists[i];
        best_ind = i;
      }
    }
    return best_ind;
  }


  /**
   * return the index of the Document in locations that minimizes the sum of distances of each
   * Document in docs from that doc.
   * docinds is a Vector<Integer>, and locations Vector<Document>
   * @param docinds Vector<Integer>
   * @param locations Vector<Document>
   * @param d double[][]
   * @return int
   * @throws ClustererException
   */
  public static int getDocumentIndCenterInds(Vector docinds, Vector locations, double[][] d) throws ClustererException {
    final int n = locations.size();
    final int m = docinds.size();

    int best_ind = -1;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<n; i++) {
      double di = 0.0;
      for (int j=0; j<m; j++) {
        int j2 = ((Integer) docinds.elementAt(j)).intValue();
        di += d[i][j2];
      }
      if (di<best_val) {
        best_val = di;
        best_ind = i;
      }
    }
    return best_ind;
  }


  /**
   * return the index of the Document in collection that minimizes the sum of distances of each
   * Document in docs from that doc.
   * docinds is a Vector<Integer>, and locations is Vector<Document>
   * @param docinds Vector<Integer>
   * @param locations Vector<Document>
   * @param collection int[]
   * @param metric DocumentDistIntf
   * @return Document
   * @throws ClustererException
   */
  public static int getDocumentIndCenterCollInds(Vector docinds, Vector locations, int[] collection,
                                                 DocumentDistIntf metric) throws ClustererException {
    final int n = locations.size();
    final int m = docinds.size();
    final int c = collection.length;
    // build cache
    if (_dists==null) {
      _dists = new double[n][n];
      for (int i=0; i<n; i++) {
        Document di = (Document) locations.elementAt(i);
        for (int j=i+1; j<n; j++) {
          Document dj = (Document) locations.elementAt(j);
          _dists[i][j] = metric.dist(di, dj);
          _dists[j][i] = _dists[i][j];
        }
      }
    }

    double dists[] = new double[c];
    int best_ind = -1;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<c; i++) {
      int i2 = collection[i];
      for (int j=0; j<m; j++) {
        int j2 = ((Integer) docinds.elementAt(j)).intValue();
        dists[i] += _dists[i2][j2];
      }
      if (dists[i]<best_val) {
        best_val = dists[i];
        best_ind = i;
      }
    }
    return best_ind;
  }


  /**
   * return the index of the Document in collection that minimizes the sum of distances of each
   * Document in docs from that doc.
   * docinds is a Vector<Integer>, and locations is Vector<Document>
   * @param docinds Vector<Integer>
   * @param locations Vector<Document>
   * @param collection int[]
   * @param d double[][]
   * @return Document
   * @throws ClustererException
   */
  public static int getDocumentIndCenterCollInds(Vector docinds, Vector locations, int[] collection,
                                                 double[][] d) throws ClustererException {
    final int n = locations.size();
    final int m = docinds.size();
    final int c = collection.length;

    int best_ind = -1;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<c; i++) {
      int i2 = collection[i];
      double di=0.0;
      for (int j=0; j<m; j++) {
        int j2 = ((Integer) docinds.elementAt(j)).intValue();
        di += d[i2][j2];
      }
      if (di<best_val) {
        best_val = di;
        best_ind = i;
      }
    }
    return best_ind;
  }


  /**
   * compute the cluster centers of this clustering described in the args
   * the clusterindices[] values range from [0...num_clusters-1], with the
   * constraint that the cluster centers MUST BE points in docs...
   * @param docs Vector<Document>
   * @param clusterindices int[]
   * @param k int
   * @param metric DocumentDistIntf
   * @param weights double[] may be null
   * @throws ClustererException
   * @return Vector<Document>
   */
  public static Vector getDocumentCenters(Vector docs, 
                                          int[] clusterindices, 
                                          int k, 
                                          DocumentDistIntf metric,
                                          double[] weights)
    throws ClustererException {
    final int docs_size = docs.size();
    if (weights!=null && weights.length!=docs_size)
      throw new ClustererException("weights and docs dims don't match");
    if (weights!=null & !(metric instanceof DocumentDistL2Sqr))
      throw new ClustererException("weights requires L2Sqr metric");
    final int dims = ((Document) docs.elementAt(0)).getDim();
    Vector centers = new Vector();  // Vector<Document>
    Vector v = new Vector();
    for (int i=0; i<k; i++) {
      v.clear();
      for (int j=0; j<docs_size; j++) {
        if (clusterindices[j]==i) v.addElement(docs.elementAt(j));
      }
      centers.addElement(Document.getDocumentCenter(v, docs, metric, weights));
    }
    return centers;
  }


  /**
   * compute the cluster centers of this clustering described in the args
   * the clusterindices[] values range from [0...num_clusters-1], with the
   * constraint that the cluster centers MUST BE points in docs...
   * @param docs Vector<Document>
   * @param clusterindices int[]
   * @param k int
   * @param metric DocumentDistIntf
   * @throws ClustererException
   * @return Vector<Document>
   */
  public static Vector getDocumentCentersFast(Vector docs, int[] clusterindices, int k, DocumentDistIntf metric)
      throws ClustererException {
    final int dims = ((Document) docs.elementAt(0)).getDim();
    final int docs_size = docs.size();
    Vector centers = new Vector();  // Vector<Document>
    Vector v = new Vector();
    for (int i=0; i<k; i++) {
      v.clear();
      for (int j=0; j<docs_size; j++) {
        if (clusterindices[j]==i) v.addElement(new Integer(j));
      }
      centers.addElement(Document.getDocumentCenterInds(v, docs, metric));
    }
    return centers;
  }


  /**
   * return a Vector of two elements, containing as first element the Document
   * representing the center of the vector of Documents passed in and as second
   * element a Double representing the cost of this cluster measured by the
   * metric passed in as third element. All this subject to the constraint that
   * the center MUST be one of the locations documents.
   * @param docs Vector
   * @param locations Vector
   * @param metric DocumentDistIntf
   * @throws ClustererException
   * @return Vector
   */
  public static Vector getDocumentCenterCosted(Vector docs, Vector locations,
                                       DocumentDistIntf metric)
      throws ClustererException {
    Vector results = new Vector();
    try {
      final int docsize = docs.size();
      if (metric==null) metric = _m;
      Document center = getDocumentCenter(docs, locations, metric, null);
      results.addElement(center);
      // compute cost
      double cost = 0;
      for (int i=0; i<docsize; i++) {
        Document di = (Document) docs.elementAt(i);
        cost += metric.dist(center, di);
      }
      Double d = new Double(cost);
      results.addElement(d);
      return results;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("getCenterCosted(): failed");
    }
  }


  /**
   * return a Vector of two elements, containing as first element the Document
   * representing the center of the vector of Documents passed in and as second
   * element a Double representing the cost of this cluster measured by the
   * metric passed in as third element. All this subject to the constraint that
   * the center MUST be one of the locations documents.
   * @param docinds Vector<Integer>
   * @param locations Vector<Document>
   * @param metric DocumentDistIntf
   * @throws ClustererException
   * @return Vector
   */
  public static Vector getDocumentCenterCostedInds(Vector docinds, Vector locations,
                                       DocumentDistIntf metric)
      throws ClustererException {
    Vector results = new Vector();
    try {
      final int docsize = docinds.size();
      if (metric==null) metric = _m;
      Document center = getDocumentCenterInds(docinds, locations, metric);
      results.addElement(center);
      // compute cost
      double cost = 0;
      for (int i=0; i<docsize; i++) {
        Document di = (Document) locations.elementAt(((Integer) docinds.elementAt(i)).intValue());
        cost += metric.dist(center, di);
      }
      Double d = new Double(cost);
      results.addElement(d);
      return results;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("getCenterCosted(): failed");
    }
  }


  /**
   * Return the diameter of the docs set, i.e. the max. distance between any two
   * points in the set, as defined by metric.
   * @param docs Vector
   * @param metric DocumentDistIntf
   * @throws ClustererException
   * @return double
   */
  public static double getDiameter(Vector docs, DocumentDistIntf metric) throws ClustererException {
    try {
      double max_dist = 0.0;
      final int n = docs.size();
      for (int i = 0; i < n; i++) {
        Document di = (Document) docs.elementAt(i);
        for (int j=i+1; j<n; j++) {
          Document dj = (Document) docs.elementAt(j);
          double d = metric.dist(di, dj);
          if (d>max_dist) max_dist = d;
        }
      }
      return max_dist;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("Document.getDiameter() failed.");
    }
  }


  /**
   * return a Vector of two different Objects. The first is the new center of
   * the collection comprised of the union of docs U dnew. The second is the
   * cost of the expanded collection expressed as a Double. The DocumentDistIntf
   * passed in is used to measure the distances from the center.
   * The second arg. is the center of the original docs collection.
   * @param docs Vector
   * @param oldmp Document
   * @param dnew Document
   * @param metric DocumentDistIntf
   * @throws ClustererException
   * @return Vector
   */
  public static Vector updateDocCollection(Vector docs,
                                           Document oldmp,
                                           Document dnew,
                                           DocumentDistIntf metric)
      throws ClustererException {
    try {
      if (metric==null) metric = _m;
      Vector result = new Vector();
      final int np = docs.size();
      final double np1 = np+1;
      Document mpnew = new Document(dnew);
      mpnew.addMul(np, oldmp);
      mpnew.div(np1);
      result.addElement(mpnew);
      // now compute new cost
      double cost = 0;
      for (int i=0; i<np; i++) {
        Document di = (Document) docs.elementAt(i);
        cost += metric.dist(mpnew, di);
      }
      cost += metric.dist(mpnew, dnew);
      result.addElement(new Double(cost));
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("updateDocCollection() failed...");
    }
  }


  // test
  public static void main(String[] args) {
    Document d1 = new Document(new TreeMap(), 5);
    Document d2 = new Document(new TreeMap(), 5);
    Document d3 = new Document(new TreeMap(), 5);
    try {
    d1.setDimValue(new Integer(0), 5);
    // d1.setDimValue(new Integer(1), 0);
    d1.setDimValue(new Integer(1), 1);
    d1.setDimValue(new Integer(2), 1);
    d1.setDimValue(new Integer(4), 0);

    d2.setDimValue(new Integer(0), 5);
    d2.setDimValue(new Integer(3), 2);
    d2.setDimValue(new Integer(4), 1);

/*
    d3.setDimValue(new Integer(0), 3);
    d3.setDimValue(new Integer(1), 5);
    Vector v = new Vector();
    v.addElement(d1); v.addElement(d2); v.addElement(d3);
    // Document c = Document.getCenter(v);
    System.out.print("d1="+d1);
    d1.addMul(3, d2);
    System.out.println(" d2="+d2+" d1="+d1);
*/
    DocumentDistL2Sqr dl2 = new DocumentDistL2Sqr();
    double dist12 = dl2.dist(d1, d2);
    System.err.println("d12="+dist12);
    }
    catch (Exception e) { e.printStackTrace();}
  }

}
