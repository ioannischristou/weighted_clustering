package clustering;

public class DocumentDistCos extends DocumentDistL2 {
  public DocumentDistCos() {
  }


  public double dist(Document x, Document y) throws ClustererException {
    double val = 0.0;
    double x_dot_y = dotproduct(x,y);
    double norm_x = norm(x);
    double norm_y = norm(y);
    // val = Math.abs(x_dot_y/(norm_x*norm_y));
    val = Math.abs(1.0-x_dot_y/(norm_x*norm_y));
    return val;
  }
}

