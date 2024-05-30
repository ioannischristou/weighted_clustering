package coarsening;

import java.util.*;

public abstract class HCoarsener {
  // use of protected is necessary else each
  // data member would need accessor/setter method.
  // since the mechanism of direct inheritance is used
  // to implement different coarsening strategies
  // (as different coarseners) like in the
  // strategy design pattern, the members had better be left
  // with protected access control rights.

  // the partition of the coarse graph is an array int[] of size
  // _coarseG.numnodes which can be returned after having called
  // coarsen(), by calling getProperty("coarsePartition");

  // the document that is the fine graph's nodes is associated with
  // is passed in the coarsener as the "nodeDocumentArray" property
  // which is a Document[0..._g.numnodes-1]. The coarsener will
  // figure out the Document that is the centroid of each new node
  // in the coarse graph and put it in the property "coarseNodeDocumentArray"
  // as a Document[0..._coarseG.numnodes-1] (you get this after having called
  // coarsen() of course.

  protected Hashtable _map;  // map<Integer oldId, Integer newId>
  protected Hashtable _rmap;  // map<Integer newId, Set<Integer oldId> >
  protected HGraph _g;
  protected HGraph _coarseG;
  protected int[] _graphPartition;  // _graphPartition[i] in {1,...,k}
                                    // i in {0,..._g.numnodes-1}
  private Hashtable _properties;


  public HCoarsener(HGraph g, int[] partition, Hashtable properties) {
    _map = new Hashtable();
    _rmap = new Hashtable();
    _g = g;
    _coarseG = null;
    if (properties!=null) {
      _properties = new Hashtable(properties);
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarsening.HCoarsener.coarseNodeDocumentArray");
      _properties.remove("coarsening.HCoarsener.coarsePartition");
    }
    _graphPartition = partition;
  }


  public HGraph getCoarseHGraph() throws CoarsenerException {
    if (_coarseG==null) throw new CoarsenerException("Hgraph not coarsened yet...");
    return _coarseG;
  }


  public HGraph getOriginalHGraph() {
    return _g;
  }


  public int[] getPartition() {
    return _graphPartition;
  }


  public int[] getFinePartition(int[] coarsepartition) throws CoarsenerException {
    if (_coarseG==null) throw new CoarsenerException("HGraph not coarsened yet");
    final int finenodes = _g.getNumNodes();
    int[] finepartition = new int[finenodes];
    for (int i=0; i<finenodes; i++) {
      Integer cid = (Integer) _map.get(new Integer(i));
      int p = coarsepartition[cid.intValue()];
      finepartition[i] = p;
    }
    return finepartition;
  }


  public void setPartition(int[] arr) {
    _graphPartition = arr;
  }


  public Object getProperty(String name) {
    return _properties.get(name);
  }


  public void setProperty(String name, Object obj) {
    _properties.put(name, obj);
  }


  public Hashtable getProperties() {
    return _properties;
  }


  abstract public void coarsen() throws GraphException, CoarsenerException;


  abstract public HCoarsener newInstance(HGraph g, int[] partition, Hashtable properties);


  protected void reset() {
    _map = new Hashtable();
    _rmap = new Hashtable();
    _coarseG = null;
    if (_properties!=null) {
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarsening.HCoarsener.coarseNodeDocumentArray");
      _properties.remove("coarsening.HCoarsener.coarsePartition");
    }
  }

}

