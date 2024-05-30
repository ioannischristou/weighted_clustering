package clustering;

import partitioning.*;
import coarsening.*;
import java.util.*;
import java.io.*;

public class CoDoCUp {
  Graph _g;
  Vector _docs;  // Vector<Document>
  int _k;  // num clusters
  Hashtable _props;  // map<String name, Object value>

  int[] _clusterindices;  // array of size _g.getNumNodes() with values [1...k]
  Vector _centers=null;  // Vector<Document> of size _k containing the centers of
                         // each cluster


  public CoDoCUp(Graph g, Vector docs, int k, Hashtable props) {
    _g = g;
    _docs = docs;
    _k = k;
    _props = new Hashtable(props);
  }


  /**
   * the main method of the program. Performs the CODOCUP algorithm
   */
  public double cluster() throws Exception {
    double result = Double.MAX_VALUE;
    int max_acc_graph_size = ((Integer) _props.get("maxaccgraphsize")).intValue();
    Clusterer cl = (Clusterer) _props.get("clusterer");

    // itc: HERE the next needs improvement
    Hashtable cl_params = new Hashtable(_props);
    cl_params.remove("coarsener");
    cl_params.remove("partitioner");
    cl_params.remove("clusterer");
    cl_params.remove("OuterTerminationCriteria");
    cl.setParams(cl_params);
    // ...

    ClustererTermination ct = (ClustererTermination) _props.get("OuterTerminationCriteria");
    Partitioner parter = (Partitioner) _props.get("partitioner");
    Evaluator km_eval = (Evaluator) _props.get("codocupevaluator");  // differentiate from evaluator property!
    if (km_eval==null)
      km_eval = new KMedianEvaluator();  // default value

    int[] coarse_partition=null;
    Document[] coarse_docs=null;

    int[] best_clusterindices = new int[_docs.size()];
    Vector best_centers = new Vector();

    ct.registerClustering(cl);
    while (!ct.isDone()) {  // outer loop
      try {
        System.err.println("Running Outer Loop...");
        System.err.println("-1-");
        // 1. coarsen down graph
        Coarsener co = (Coarsener) _props.get("coarsener");  // get the original Coarsener
        Graph g = _g;
        System.err.println("CoDoCUp.cluster(): -1-: _g.size="+g.getNumNodes()+" _g.getNumComponents()="+_g.getNumComponents());
        Vector coarseners = new Vector();  // Vector<Coarsener>, last one is
        // coarsener used to create last graph
        int num_iters = 0;
        while (g.getNumNodes()>max_acc_graph_size) {
          ++num_iters;
          System.err.println("Entering coarsening level "+num_iters);
          try {
            co.coarsen();
            Graph coarse_g = co.getCoarseGraph();
            System.err.println("coarse_g.size="+coarse_g.getNumNodes()+" #arcs="+coarse_g.getNumArcs());
            coarse_partition = (int[]) co.getProperty("coarsePartition");
            coarse_docs = (Document[]) co.getProperty("coarseNodeDocumentArray");
            Hashtable props = co.getProperties();
            coarseners.addElement(co);

            Coarsener co1 = co.newInstance(coarse_g, coarse_partition, props);
            co1.setProperty("nodeDocumentArray", coarse_docs);
            co = co1;
            g = coarse_g;
          }
          catch (CoarsenerException e) {
            // remove this coarsener from the coarseners as it didn't work
            // coarseners.remove(coarseners.size()-1);  // itc: HERE the coarsener didn't enter the vector so it must not be rm'ed
            break;
          }
        }

        System.err.println("-2-");
        // 2. partition g
        int[] partition = parter.partition(g, _k, _props);

        // sanity check for partitioner
        int[] pszes = new int[_k];
        for (int i=0; i<_k; i++) pszes[i]=0;
        for (int i=0; i<g.getNumNodes(); i++) {
          pszes[partition[i]-1]++;
        }
        for (int i=0; i<_k; i++) {
          if (pszes[i]==0) {
            System.err.println("Graph g="+g);
            throw new PartitioningException("Partitioner failed...");
          }
        }

        System.err.println("-3-");
        // 3. compute partition centers
        Vector[] docs_i = new Vector[_k];  // docs_i is Array of Vector<Document>
        for (int j=0; j<g.getNumNodes(); j++) {
          int p = partition[j]-1;  // partition[j] is from 1..._k
          if (docs_i[p]==null) docs_i[p] = new Vector();
          docs_i[p].addElement(coarse_docs[j]);
        }
        _centers = new Vector();
        for (int i=0; i<_k; i++) {
          Document center_i = Document.getCenter(docs_i[i], null);  // itc: HERE 20220223
          _centers.addElement(center_i);
        }

        System.err.println("-4-");
        // 4. cluster up docs
        for (int i=coarseners.size()-1; i>=0; i--) {
          System.err.println("CDC coarsening level "+i);
          Coarsener ci = (Coarsener) coarseners.elementAt(i);
          // Graph gf = ci.getOriginalGraph();
          cl.reset();
          Document[] node_docs = (Document[]) ci.getProperty("nodeDocumentArray");
          for (int j=0; j<node_docs.length; j++)
            cl.addDocument(node_docs[j]);
          cl.setInitialClustering(_centers);
          try {
            _centers = cl.clusterDocs();
            // 4.3 set the partition of the fine graph (ci._graphPartition)
            _clusterindices = cl.getClusteringIndices();
            int[] cards = cl.getClusterCards();
            // evaluate clustering
            // KMedianEvaluator kmedian_eval = new KMedianEvaluator();
            double obj = cl.eval(km_eval);
            System.out.print("In level #"+i+" clustering obj="+obj);
            System.out.print(" cluster cards: ");
            for (int ii=0; ii<cards.length; ii++) {
              System.out.print("c["+ii+"]="+cards[ii]+" ");
            }
            System.out.println("");
          }
          catch (ClustererException e) {
            System.err.print("cl.clusterDocs() failed at level "+i);
            System.err.print(" (ranging ["+(coarseners.size()-1)+"...0])");
            System.err.println(". Projecting directly low-level clustering...");
            // project previous level clustering directly to higher level
            // set _centers & _clusterindices:
            if (Document.getMetric()==null) {
              DocumentDistIntf metric = (DocumentDistIntf) _props.get("metric");
              if (metric != null) Document.setMetric(metric);
              else Document.setMetric(new DocumentDistL1());  // default value
            }
            _clusterindices = new int[node_docs.length];  // new array required
            for (int j=0; j<_clusterindices.length; j++)
              _clusterindices[j]=-1;  // initialize
            // make sure there is at least one doc for each center
            for (int j=0; j<_centers.size(); j++) {
              double best_val = Double.MAX_VALUE;
              int best_ind=-1;
              Document centerj = (Document) _centers.elementAt(j);
              for (int jj=0; jj<node_docs.length; jj++) {
                if (_clusterindices[jj]>=0) continue;  // used already
                double cd = Document.d(node_docs[jj], centerj);
                if (cd<best_val) {
                  best_ind = jj;
                  best_val = cd;
                }
              }
              _clusterindices[best_ind]=j;
            }
            // now do the rest of the asgnmnt
            for (int j=0; j<node_docs.length; j++) {
              if (_clusterindices[j]>=0) continue;  // found already
              double best_val = Double.MAX_VALUE;
              int best_ind=-1;
              Document nodedocj = node_docs[j];
              for (int jj=0; jj<_centers.size(); jj++) {
                double cd = Document.d(nodedocj, (Document) _centers.elementAt(jj));
                if (cd<best_val) {
                  best_ind = jj;
                  best_val = cd;
                }
              }
              _clusterindices[j]=best_ind;
            }
            // finally, update the _centers
            _centers = Document.getCenters(node_docs, _clusterindices, _k, null);
/*
            // itc: HERE sanity check for cluster sizes
            int[] psizes = new int[_k];
            for (int ii=0; ii<_k; ii++) psizes[ii]=0;
            for (int ii=0; ii<node_docs.length; ii++) {
              psizes[_clusterindices[ii]]++;
            }
            for (int ii=0; ii<_k; ii++) {
              if (psizes[ii]<=0)
                throw new ClustererException("Catching Exception didn't work...");
              else System.err.println("psizes["+ii+"]="+psizes[ii]);
            }
*/
            System.err.println("cl.clusterDocs(): Direct projecting done.");
            // if at the top, set the cl centers and clusteringindices
            if (i==0) {
              cl.setInitialClustering(_centers);
              cl.setClusteringIndices(_clusterindices);
            }
          }

          ci.setPartition(_clusterindices);
        }  // for i=coarseners.size()-1...0
        double ri = cl.eval(km_eval);
        if (ri<result) {
          result = ri;
          // set clustering indices and centers
          for (int i=0; i<_clusterindices.length; i++)
            best_clusterindices[i] = _clusterindices[i];
          best_centers.clear();
          for (int i=0; i<_centers.size(); i++)
            best_centers.addElement(_centers.elementAt(i));
        }
      }
      catch (PartitioningException e) {
        System.exit(-1);  // partitioner should not fail under any circumstances
      }
      catch (Exception e) {
        e.printStackTrace();
        // continue;
      }
    }  // while (!ct.isDone())
    // set the final clusterindices and centers
    _clusterindices = best_clusterindices;
    _centers = best_centers;
    System.err.println("CoDoCUp.cluster() done.");
    return result;
  }


  public Document getClusterCenter(int i) throws ClustererException {
    if (_centers==null) throw new ClustererException("CoDoCUp not run yet");
    return (Document) _centers.elementAt(i);
  }


  public int getDocumentClusterIndex(int docindex) throws ClustererException {
    if (_clusterindices==null) throw new ClustererException("CoDoCUp not run yet");
    return _clusterindices[docindex];
  }


  int[] getDocumentClusterIndices() {
    return _clusterindices;
  }


  /**
   * main(): read the Graph and Documents Vector from data files, and run
   * CODOCUP clustering.
   */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: CoDoCUp graph_file document_file options_file [numiters] [makeplotgraph | labels_file]");
      System.exit(-1);
    }

    long start_time = System.currentTimeMillis();

    String graph_file = args[0];
    String docs_file = args[1];
    String opts_file = args[2];
    int num_iters = 3;  // default 3 outer iterations
    if (args.length>=4) {
      num_iters = Integer.parseInt(args[3]);
    }
    boolean makeplotgraph = (args.length==5 && "makeplotgraph".equals(args[4]));
    try {
      // 1. read data
      Graph g = utils.DataMgr.readGraphFromFile(graph_file);
      System.err.println("g.numNodes="+g.getNumNodes()+" g.numArcs="+g.getNumArcs());
      Vector docs = utils.DataMgr.readDocumentsFromFile(docs_file);
      System.err.println("docs.size="+docs.size());
      Hashtable props = utils.DataMgr.readPropsFromFile(opts_file);
      // check metric to set
      DocumentDistIntf metric = (DocumentDistIntf) props.get("metric");
      if (metric!=null)
        Document.setMetric(metric);
      else Document.setMetric(new DocumentDistL1());  // default

      Evaluator eval=(Evaluator) props.get("evaluator");
      if (eval!=null && makeplotgraph==false && args.length==5) {
        int trueclusters[] = utils.DataMgr.readLabelsFromFile(args[4], docs.size());
        props.put("trueclusters", trueclusters);
      }

      // nodeDocumentArray
      Document[] n_d_a = new Document[g.getNumNodes()];
      for (int i=0; i<g.getNumNodes(); i++)
        n_d_a[i] = (Document) docs.elementAt(i);
      props.put("nodeDocumentArray", n_d_a);

      // 1.5 add the Coarsener & TerminationCriteria into the props
      Coarsener cer = new CoarsenerIEC(g, null, props);
      props.put("coarsener",cer);  // itc: HERE is this props thing OK?
      ClustererTermination ct = (ClustererTermination) props.get("TerminationCriteria");
      if (ct==null) {
        if (metric instanceof clustering.DocumentDistL2)
          ct = new ClustererTerminationNoImprL2();
        else if (metric instanceof clustering.DocumentDistL2Sqr)
          ct = new ClustererTerminationNoImprL2Sqr();
        else ct = new ClustererTerminationNoImpr();  // default
        props.put("TerminationCriteria",ct);
      }
      ClustererTermination outerct = new ClusterTerminationNumIters(num_iters);  // num_iters outer iterations
      props.put("OuterTerminationCriteria",outerct);

      int k = ((Integer) props.get("num_clusters")).intValue();

      // 2. cluster
      CoDoCUp cdcup = new CoDoCUp(g, docs, k, props);
      double clobj = cdcup.cluster();

      // 3. print results
      System.out.println("Clustering Results:");
      for (int i=0; i<docs.size(); i++) {
        System.out.println("c["+i+"]="+cdcup.getDocumentClusterIndex(i));
      }
      System.out.println("Clustering Best Objective Value="+clobj);
      // 3.5 print accuracy results
      if (eval!=null) {
        Clusterer tmp_cl = (Clusterer) props.get("evalclusterer");
        if (tmp_cl==null) tmp_cl = new KMeansSqrClusterer();
        // just use any Clusterer with a precomputed clustering to pass it to Evaluator
        tmp_cl.addAllDocuments(docs);
        // make sure projectonempty is not in props
        props.remove("projectonempty");
        tmp_cl.setClusteringIndices(cdcup._clusterindices);
        tmp_cl.setInitialClustering(cdcup._centers);
        double accuracy_val = eval.eval(tmp_cl);
        System.out.println("CoDoCUp accuracy value before improvement = "+accuracy_val);
        // now improve balance of clustering and then cluster again
        boolean balance=false;
        props.remove("movable");  // the "movable" Vector is the vector of cluster indices that should be allowed to move.
        while (balance==false) {
          balance = balanceClusters(tmp_cl,props);
        }
        tmp_cl.setParams(props);
        tmp_cl.clusterDocs();  // one more time!!!
        accuracy_val = eval.eval(tmp_cl);
        System.out.println("CoDoCUp accuracy value after KMeans improvement = "+accuracy_val);
      }
      System.out.println("Total time="+(System.currentTimeMillis()-start_time)+" msecs");

      // itc: HERE print into the debug_plot.txt file a plot of the clustering
      if (makeplotgraph)
        utils.DataMgr.print2PlotFile("debug_plot.txt", docs, cdcup.getDocumentClusterIndices());
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  /**
   * take the maximum and minimum cardinality clusters and create two new
   * clusters containing two equal size blocks. The operation only happens
   * if the relative difference between max and min size blocks is bigger than
   * 10.
   * The method also sets the "movable" clusters in the props hashtable to
   * indicate which clusters should be allowed to change during clusterDocs()
   * @param cl Clusterer
   * @param props Hashtable
   * @return true if there is no cluster of size 1 or less
   */
  private static boolean balanceClusters(Clusterer cl, Hashtable props)
      throws ClustererException {
    boolean ret=false;
    int ind[] = cl.getClusteringIndices();
    int numi[] = cl.getClusterCards();
    Vector centers = cl.getCurrentCenters();
    int minc = Integer.MAX_VALUE;
    int maxc = -1;
    int mind=-1; int Mind=-1;  // index of min. and Max. size cluster
    for (int i=0; i<numi.length; i++) {
      if (numi[i]<minc) {
        mind=i; minc = numi[i];
      }
      if (numi[i]>maxc) {
        Mind=i; maxc = numi[i];
      }
    }
    if (minc>=2) ret = true;
    if (ret==false && ((double) (maxc-minc)/(double) minc) >=10) {
      // do the balancing operation
      Vector movable = (Vector) props.get("movable");
      if (movable==null) movable = new Vector();  // Vector<Integer clusterindex>
      movable.addElement(new Integer(mind));
      movable.addElement(new Integer(Mind));
      props.put("movable", movable);
      Vector min_vecs = new Vector();
      Vector max_vecs = new Vector();
      for (int i=0; i<ind.length; i++)
        if (ind[i]==mind) min_vecs.addElement(cl.getCurrentDocs().elementAt(i));

      for (int i=0; i<ind.length; i++) {
        if (numi[Mind]<=numi[mind]) break;  // too much movement
        if (ind[i]==Mind && utils.RndUtil.getInstance().getRandom().nextDouble()<0.5) {
          min_vecs.addElement(cl.getCurrentDocs().elementAt(i));
          ind[i] = mind;  // move point from big cluster to small cluster
          numi[Mind]--;
          numi[mind]++;
        }
        else if (ind[i]==Mind) {
          max_vecs.addElement(cl.getCurrentDocs().elementAt(i));
        }
      }
      // compute the new centers
      Document min_center = Document.getCenter(min_vecs, null);  // itc: HERE 20220223
      Document max_center = Document.getCenter(max_vecs, null);  // itc: HERE 20220223
      // update clusterer
      centers.set(mind, min_center);
      centers.set(Mind, max_center);
      cl.setClusteringIndices(ind);
      cl.setInitialClustering(centers);
    }
    return ret;
  }


/*
  private static void print2PlotFile(String plotfile, Vector docs, int[] cinds) {
    try {
      String[] shapes = {"dot", "diamond", "box", "plus"};
      PrintWriter pw = new PrintWriter(new FileOutputStream(plotfile));
      pw.println("double double");
      pw.println("invisible 0 0"); pw.println("invisible 150 150");  // itc: HERE rm ASAP
      Integer x = new Integer(0);
      Integer y = new Integer(1);
      for (int j=0; j<docs.size(); j++) {
        clustering.Document dj = (clustering.Document) docs.elementAt(j);
        Double djx = dj.getDimValue(x);
        Double djy = dj.getDimValue(y);
        int ind = cinds[j];
        String shapei = shapes[ind];
        pw.println(shapei+" "+djx.doubleValue()+" "+djy.doubleValue());
      }
      pw.println("go");
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
*/
}
