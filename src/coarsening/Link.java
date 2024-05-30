package coarsening;

public class Link {
  private int _id;
  private int _starta;
  private int _enda;
  private double _weight;


  Link(int id, Node in, Node out) {
    _id = id;
    _starta = in.getId();
    _enda = out.getId();
    _weight = 1.0;
    in.addOutLink(out, new Integer(id));
  }


  Link(Graph g, int id, int starta, int enda, double weight) {
    _id = id;
    _starta = starta;
    _enda = enda;
    _weight = weight;
    Node s = g.getNode(starta);
    Node e = g.getNode(enda);
    s.addOutLink(e, new Integer(id));
  }


  public int getId() { return _id; }


  public double getWeight() { return _weight; }


  public int getStart() { return _starta; }


  public int getEnd() { return _enda; }

}
