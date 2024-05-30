package utils;

import coarsening.*;
import clustering.*;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import java.util.*;
import java.io.*;
import java.text.*;
import java.lang.reflect.*;

public class DataMgr {

    static int pos[] = {2, 5, 15, 25, 40, 50};
    static int poslen[] = {2, 8, 8, 12, 8, 12};

    /**
     * filename format is of the form: [ numnodes numarcs starta enda [weighta]
     * [...] ] The starta, enda are in [0...num_nodes-1] weighta is double
     * (non-negative)
     *
     * @param filename String
     * @throws IOException
     * @return Graph
     */
    public static Graph readGraphFromFile(String filename) throws IOException, GraphException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            Graph g = null;
            if (br.ready()) {
                // read first line
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numnodes = Integer.parseInt(st.nextToken());
                int numarcs = Integer.parseInt(st.nextToken());
                g = new Graph(numnodes, numarcs);
                int starta, enda;
                double weighta;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, " ");
                    starta = Integer.parseInt(st.nextToken());
                    enda = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens()) {
                        weighta = Double.parseDouble(st.nextToken());
                    } else {
                        weighta = 1.0;
                    }
                    g.addLink(starta, enda, weighta);
                }
            }
            // set cardinality values
            for (int i = 0; i < g.getNumNodes(); i++) {
                Node ni = g.getNode(i);
                ni.setWeight("cardinality", new Double(1.0));
            }
            return g;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * filename format is of the form: [ numarcs numnodes starta enda [weighta]
     * [...] ] The starta, enda are in [1...num_nodes] weighta is double
     * (non-negative)
     *
     * @param filename String
     * @throws IOException
     * @return Graph
     */
    public static Graph readGraphFromFile2(String filename) throws IOException, GraphException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            Graph g = null;
            if (br.ready()) {
                // read first line
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numarcs = Integer.parseInt(st.nextToken());
                int numnodes = Integer.parseInt(st.nextToken());
                g = new Graph(numnodes, numarcs);
                int starta, enda;
                double weighta;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, " ");
                    starta = Integer.parseInt(st.nextToken());
                    enda = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens()) {
                        weighta = Double.parseDouble(st.nextToken());
                    } else {
                        weighta = 1.0;
                    }
                    g.addLink(starta - 1, enda - 1, weighta);
                }
            }
            // set cardinality values
            for (int i = 0; i < g.getNumNodes(); i++) {
                Node ni = g.getNode(i);
                ni.setWeight("cardinality", new Double(1.0));
            }
            return g;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * filename format is of the form: [ numarcs numnodes 1 weighta starta enda
     * [...] ] The starta, enda are in [1...num_nodes] weighta is int
     * (non-negative)
     *
     * @param filename String
     * @throws IOException
     * @return Graph
     */
    public static Graph readGraphFromhMeTiSFile(String filename) throws IOException, GraphException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            Graph g = null;
            if (br.ready()) {
                // read first line
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numarcs = Integer.parseInt(st.nextToken());
                int numnodes = Integer.parseInt(st.nextToken());
                g = new Graph(numnodes, numarcs);
                int starta, enda;
                double weighta;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, " ");
                    weighta = Integer.parseInt(st.nextToken());
                    starta = Integer.parseInt(st.nextToken());
                    enda = Integer.parseInt(st.nextToken());
                    g.addLink(starta - 1, enda - 1, weighta);
                }
            }
            // set cardinality values
            for (int i = 0; i < g.getNumNodes(); i++) {
                Node ni = g.getNode(i);
                ni.setWeight("cardinality", new Double(1.0));
            }
            return g;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * same as above method, only that the labelfile serves to read the labels
     * for each of the nodes in the Graph being created
     *
     * @param filename String
     * @param labelfile String
     * @throws IOException
     * @throws GraphException
     * @return Graph
     */
    public static Graph readGraphFromhMeTiSFile(String filename, String labelfile) throws IOException, GraphException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        BufferedReader br2 = new BufferedReader(new FileReader(labelfile));
        try {
            Graph g = null;
            if (br.ready() && br2.ready()) {
                // read first line
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numarcs = Integer.parseInt(st.nextToken());
                int numnodes = Integer.parseInt(st.nextToken());
                // now read the 2nd file and create the labels array
                Integer[] labels = new Integer[numnodes];
                for (int j = 0; j < numnodes; j++) {
                    int labelj = Integer.parseInt(br2.readLine());
                    labels[j] = new Integer(labelj);
                }
                br2.close();
                g = new Graph(numnodes, numarcs, labels);
                int starta, enda;
                double weighta;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, " ");
                    weighta = Integer.parseInt(st.nextToken());
                    starta = Integer.parseInt(st.nextToken());
                    enda = Integer.parseInt(st.nextToken());
                    g.addLink(starta - 1, enda - 1, weighta);
                }
            }
            // set cardinality values
            for (int i = 0; i < g.getNumNodes(); i++) {
                Node ni = g.getNode(i);
                ni.setWeight("cardinality", new Double(1.0));
            }
            return g;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * filename format is of the form: [ numarcs numnodes [1] [weighta] node1
     * node2 ... nodek [...] ] The nodei are in [1...numnodes] weighta is int
     * (non-negative). If it does not exist (the third value in the first line
     * does not exist), then its value is 1.
     *
     * @param filename String
     * @throws IOException
     * @return HGraph
     */
    public static HGraph readHGraphFromhMeTiSFile(String filename) throws IOException, GraphException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            HGraph g = null;
            boolean no_weight = false;
            if (br.ready()) {
                // read first line
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numarcs = Integer.parseInt(st.nextToken());
                int numnodes = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens() == false) {
                    no_weight = true;
                }
                g = new HGraph(numnodes, numarcs);
                Set nodes = new HashSet();  // Set<Integer id>
                double weighta = 1.0;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, " ");
                    if (no_weight == false) {
                        weighta = Integer.parseInt(st.nextToken());
                    }
                    nodes.clear();
                    while (st.hasMoreTokens()) {
                        int nid = Integer.parseInt(st.nextToken());
                        nodes.add(new Integer(nid - 1));
                    }
                    g.addHLink(nodes, weighta);
                }
            }
            // set cardinality values
            for (int i = 0; i < g.getNumNodes(); i++) {
                HNode ni = g.getHNode(i);
                ni.setWeight("cardinality", new Double(1.0));
            }
            return g;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * reads the documents from a file of the form [ numdocs totaldimensions
     * dim,val [dim,val] [...] ] dim is in [1...totaldimensions] the documents
     * are represented as a sparse vector representation of a vector in a vector
     * space of dimension totaldimensions the return value is a Vector<Document>
     * the 1st document in the list MUST correspond to the node_id ONE in the
     * graph created by the previous method readGraphFromFile, and so on.
     *
     * @param filename String
     * @return Vector
     * @throws IOException, ClustererException
     */
    public static Vector readDocumentsFromFile(String filename)
            throws IOException, ClustererException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            Vector v = new Vector();
            if (br.ready()) {
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numdocs = Integer.parseInt(st.nextToken());
                int totaldims = Integer.parseInt(st.nextToken());
                Integer dim = null;
                double val = 0.0;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    Document d = new Document(new TreeMap(), totaldims);
                    st = new StringTokenizer(line, " ");
                    while (st.hasMoreTokens()) {
                        String pair = st.nextToken();
                        StringTokenizer st2 = new StringTokenizer(pair, ",");
                        dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
                        // dimension value is from 1...totdims
                        val = Double.parseDouble(st2.nextToken());
                        d.setDimValue(dim, val);
                    }
                    v.addElement(d);
                }
            }
            return v;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    public static Vector readDocumentsFromFileAndNormalize(String filename)
            throws IOException, ClustererException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            DocumentDistL2 m = new DocumentDistL2();
            Vector v = new Vector();
            if (br.ready()) {
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, " ");
                int numdocs = Integer.parseInt(st.nextToken());
                int totaldims = Integer.parseInt(st.nextToken());
                Integer dim = null;
                double val = 0.0;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    Document d = new Document(new TreeMap(), totaldims);
                    st = new StringTokenizer(line, " ");
                    while (st.hasMoreTokens()) {
                        String pair = st.nextToken();
                        StringTokenizer st2 = new StringTokenizer(pair, ",");
                        dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
                        // dimension value is from 1...totdims
                        val = Double.parseDouble(st2.nextToken());
                        d.setDimValue(dim, val);
                    }
                    // normalize
                    double norm = m.norm(d);
                    if (norm > 0) {
                        d.div(norm);
                    }
                    v.addElement(d);
                }
            }
            return v;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * reads data from files in Cluster3 format and returns the documents
     * vector. The Cluster3 file format is as follows: uniqid name gweight
     * gorder exp1 exp2 ... expn eweight e1 e2 ... en geneid name 1 data data
     * ... data ... all data/expressions are tab dilimited
     *
     * @param filename String
     * @throws IOException
     */
    public static Vector readDocsFromCluster3File(String filename)
            throws IOException, ClustererException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        int lineno = 2;
        try {
            Vector docs = new Vector();
            if (br.ready()) {
                String line = br.readLine();  // initial line, useless
                line = br.readLine();  // experiment weights
                int dims = countTokensInLine(line, '\t') - 4;
                double[] vals = new double[dims];
                while (true) {
                    line = br.readLine();
                    lineno++;
                    if (line == null || line.length() == 0) {
                        break;  // end of file reached
                    }
                    convertTokensInLine(line, '\t', vals, 4);
                    Document d = new Document(new TreeMap(), dims);
                    for (int i = 0; i < vals.length; i++) {
                        if (Double.isNaN(vals[i]) == false) {
                            d.setDimValue(new Integer(i), vals[i]);
                        }
                    }
                    docs.addElement(d);
                    System.err.println("successfully read " + docs.size() + " docs");  // itc: HERE
                }
            }
            return docs;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClustererException("readDocsFromCluster3File(): in line no: " + lineno);
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * writes the docs in the 1st argument to file specified in 2nd arg.
     * according to the format specified in readDocumentsFromFile()
     *
     * @param docs Vector
     * @param tot_dims int
     * @param filename String
     * @throws IOException
     */
    public static void writeDocumentsToFile(Vector docs, int tot_dims, String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        final int docs_size = docs.size();
        pw.println(docs_size + " " + tot_dims);
        for (int i = 0; i < docs_size; i++) {
            Document di = (Document) docs.elementAt(i);
            Iterator keys = di.positions();
            Iterator vals = di.values();
            // String line="";
            StringBuffer lineb = new StringBuffer();
            while (vals.hasNext()) {
                Integer pos = (Integer) keys.next();
                Double val = (Double) vals.next();
                // line += (pos.intValue()+1)+","+val+" ";
                lineb.append((pos.intValue() + 1));
                lineb.append(",");
                lineb.append(val);
                lineb.append(" ");
            }
            String line = lineb.toString();
            pw.println(line);
        }
        pw.close();
    }

    /**
     *
     * @param Vector docs Vector<Document> the docs file
     * @param int[] labels the class label of each doc in docs
     * @param filename String the filename to write the dataset in arff format
     * @throws Exception
     */
    public static void writeDocsAndLabelsToArffFile(Vector docs, int[] labels, String filename)
            throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        final int docs_size = docs.size();
        final int num_dims = ((Document) docs.elementAt(0)).getDim();
        Set lbls = new TreeSet();
        for (int i = 0; i < labels.length; i++) {
            lbls.add(new Integer(labels[i]));
        }
        // write headers
        pw.println("@RELATION experiment");
        for (int i = 0; i < num_dims; i++) {
            pw.println("@ATTRIBUTE a" + i + " NUMERIC");
        }
        pw.print("@ATTRIBUTE class {");
        Iterator it = lbls.iterator();
        while (it.hasNext()) {
            Integer li = (Integer) it.next();
            pw.print(li.intValue());
            if (it.hasNext()) {
                pw.print(",");
            }
        }
        pw.println("}");
        pw.println("@DATA");
        // write body
        for (int i = 0; i < docs_size; i++) {
            Document di = (Document) docs.elementAt(i);
            for (int j = 0; j < num_dims; j++) {
                Double val = di.getDimValue(new Integer(j));
                if (val != null) {
                    pw.print(val.doubleValue());
                } else {
                    pw.print("0");  // null dimension value means zero
                }
                pw.print(",");
            }
            pw.println(labels[i]);
        }
        // done: close file and return
        pw.flush();
        pw.close();
    }

    /**
     * writes the cluster indices of a cluster solution to the file given in the
     * second argument
     *
     * @param cl Clusterer
     * @param filename String
     * @throws Exception
     */
    public static void writeLabelsToFile(Clusterer cl, String filename) throws Exception {
        System.err.println("writing labels to file " + filename);
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        int indices[] = cl.getClusteringIndices();
        if (indices == null) {
            throw new ClustererException("no clustering soln available");
        }
        for (int i = 0; i < indices.length; i++) {
            pw.println(indices[i]);
        }
        pw.flush();
        pw.close();
    }

    /**
     * writes the cluster indices of a cluster solution to the file given in the
     * second argument
     *
     * @param int[] the asgn to write
     * @param filename String
     * @throws Exception
     */
    public static void writeLabelsToFile(int indices[], String filename) throws Exception {
        System.err.println("writing labels to file " + filename);
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        if (indices == null) {
            throw new ClustererException("no clustering soln available");
        }
        for (int i = 0; i < indices.length; i++) {
            pw.println(indices[i]);
        }
        pw.flush();
        pw.close();
    }

    /**
     * writes the cluster indices of a cluster solution to the file given in the
     * second argument
     *
     * @param int[] the asgn to write with values in {1,...k}
     * @param filename String
     * @throws Exception
     */
    public static void writePartitionToFile(int indices[], String filename) throws Exception {
        System.err.println("writing labels to file " + filename);
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        if (indices == null) {
            throw new ClustererException("no clustering soln available");
        }
        for (int i = 0; i < indices.length; i++) {
            pw.println(indices[i] - 1);
        }
        pw.flush();
        pw.close();
    }

    /**
     * reads the class labels from a file whose format is like this:
     * document_1_classlabel document_2_classlabel ... and returns an int array
     * of length equal to numdocs (#lines in the filename), with array[i]
     * indicating the class to which the (i+1)-st document belongs.
     *
     * @param filename String
     * @param numdocs int
     * @return int[]
     */
    public static int[] readLabelsFromFile(String filename, int numdocs) throws IOException {
        Hashtable lu = new Hashtable();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            int arr[] = new int[numdocs];
            int i = 0, j = 0;
            while (true) {
                String label = br.readLine();
                if (label == null || label.length() == 0) {
                    break;
                }
                Integer lid = (Integer) lu.get(label);
                if (lid == null) {
                    lid = new Integer(i++);
                    lu.put(label, lid);
                }
                arr[j++] = lid.intValue();
            }
            return arr;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    public static int[] readNumericLabelsFromFile(String filename) throws IOException {
        BufferedReader br = null;
        Vector ls = new Vector();
        try {
            br = new BufferedReader(new FileReader(filename));
            if (br.ready()) {
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;  // EOF
                    }
                    int li = Integer.parseInt(line);
                    ls.addElement(new Integer(li));
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        int numdocs = ls.size();
        int labels[] = new int[numdocs];
        for (int i = 0; i < numdocs; i++) {
            labels[i] = ((Integer) ls.elementAt(i)).intValue();
        }
        return labels;
    }

    /**
     * read a set of centers from a file. The first line contains the number of
     * dimensions of the vector space. The rest of the lines are the centers in
     * space separated coordinates.
     *
     * @param filename String
     * @throws IOException
     * @throws ClustererException
     * @return Vector
     */
    public static Vector readCentersFromFile(String filename)
            throws IOException, ClustererException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            Vector v = new Vector();
            if (br.ready()) {
                String line = br.readLine();
                int dims = Integer.parseInt(line);
                Integer dim = null;
                double val = 0.0;
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    Document d = new Document(new TreeMap(), dims);
                    StringTokenizer st = new StringTokenizer(line, ", ");
                    int i = 0;
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken();
                        val = Double.parseDouble(tok);
                        d.setDimValue(new Integer(i++), val);
                    }
                    v.addElement(d);
                }
            }
            return v;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * takes as input an already constructed graph, and prints it in MeTiS
     * format in the file specified in the second argument. The format of this
     * output file is as follows: numnodes numarcs ... starta enda weight
     * starta, enda are in [0,...,numnodes-1]
     *
     * @param g Graph
     * @param filename String
     * @throws IOException
     */
    public static void writeGraphToFile(Graph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numnodes + " " + numarcs);
        for (int i = 0; i < numarcs; i++) {
            Link arci = g.getLink(i);
            pw.println(arci.getStart() + " " + arci.getEnd() + " " + arci.getWeight());
        }
        pw.flush();
        pw.close();
    }

    /**
     * takes as input an already constructed graph, and prints it in MeTiS
     * format in the file specified in the second argument. The format of this
     * output file is as follows: numarcs numnodes ... starta enda weight
     * starta, enda are in [1,...,numnodes]
     *
     * @param g Graph
     * @param filename String
     * @throws IOException
     */
    public static void writeGraphToFile2(Graph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numarcs + " " + numnodes);
        for (int i = 0; i < numarcs; i++) {
            Link arci = g.getLink(i);
            pw.println((arci.getStart() + 1) + " " + (arci.getEnd() + 1) + " " + arci.getWeight());
        }
        pw.flush();
        pw.close();
    }

    /**
     * write the results of clustering along the Principal Components Dimensions
     * into a Hyper-Graph file for HMETIS. HMETIS accepts a single file with the
     * following format (|E|+1 lines): NumEdges NumVertices 1 ... edge_weight
     * node_id1 node_id2 ... node_idk ... In other words, the i-th line (i>1),
     * contains first the weight of the net (as integer quantity...) and the ids
     * of the nodes in the interval [1...NumVertices] participating in the
     * (i-1)-st line. The first argument contains the matrix P[N x r] which
     * contains the results of r clusterings. Each of the r columns of this
     * matrix is a vector containing the cluster indices of the clustering of
     * the projection of the N documents into the r-th principal component
     * dimension. However, P is provided as a Vector[] where each vector in the
     * array is a Vector<Integer i> where the value of i is in [1...k] The
     * second arg is the singular values associated with each decomposition.
     * There are as many columns in P as the dimension of the array svalues. The
     * third arg is the name of the file to store the hyper-graph
     *
     * @param P Vector[]
     * @param svalues double[]
     * @param n int the number of rows of P
     * @param r int the number of columns of P, and also the dimension of
     * svalues
     * @param k int the number of clusters to obtain
     * @param filename String
     * @throws IOException
     */
    public static void writeClusterEnsembleToHGRFile(Vector P[],
            double svalues[],
            int n, int r,
            int k,
            String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        // each column has k nets, so there are k*r nets total
        pw.println((k * r) + " " + n + " 1");
        Vector lines = new Vector();  // Vector<String line-i>
        for (int j = 0; j < r; j++) {
            // figure out the k nets of j-th clustering
            lines.clear();
            for (int ik = 0; ik < k; ik++) {
                String l = Integer.toString(((int) Math.ceil(svalues[j]))) + " ";
                lines.addElement(l);
            }
            Vector Pj = P[j];
            for (int i = 0; i < n; i++) {
                int p = ((Integer) Pj.elementAt(i)).intValue();  // p is in [1...k]
                String lp = (String) lines.elementAt(p - 1);
                lp += Integer.toString(i + 1);
                lp += " ";
                lines.set(p - 1, lp);
            }
            // print out the nets
            for (int i = 0; i < k; i++) {
                pw.println(lines.elementAt(i));
            }
        }
        pw.flush();
        pw.close();
    }

    /**
     * takes as input an already constructed graph, and prints it in HMeTiS
     * format in the file specified in the second argument. The format of this
     * output file is specified in the comments for method
     * writeClusterEnsembleToHGRFile()
     *
     * @param g Graph
     * @param filename String
     * @throws IOException
     */
    public static void writeGraphToHGRFile(Graph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numarcs / 2 + " " + numnodes + " 1");
        for (int i = 0; i < numarcs; i++) {
            Link l = g.getLink(i);
            Node startn = g.getNode(l.getStart());
            Node endn = g.getNode(l.getEnd());
            if (startn.getId() < endn.getId()) {
                long w = (long) (2.0 * Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
                pw.println(w + " " + (startn.getId() + 1) + " " + (endn.getId() + 1));
            }
        }
        pw.flush();
        pw.close();
    }

    public static void writeGraphDirectToHGRFile(Graph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numarcs + " " + numnodes + " 1");
        for (int i = 0; i < numarcs; i++) {
            Link l = g.getLink(i);
            Node startn = g.getNode(l.getStart());
            Node endn = g.getNode(l.getEnd());
            if (startn.getId() < endn.getId()) {
                long w = (long) (Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
                pw.println(w + " " + (startn.getId() + 1) + " " + (endn.getId() + 1));
            }
        }
        pw.flush();
        pw.close();
    }

    public static void writeHGraphDirectToHGRFile(HGraph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numarcs + " " + numnodes + " 1");
        for (int i = 0; i < numarcs; i++) {
            HLink l = g.getHLink(i);
            long w = (long) (Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
            pw.print(w);
            Iterator nids = l.getHNodeIds();
            while (nids.hasNext()) {
                int nid = ((Integer) nids.next()).intValue() + 1;
                pw.print(" " + nid);
            }
            pw.println("");
        }
        pw.flush();
        pw.close();
    }

    public static void writeWeightedHGraphDirectToHGRFile(HGraph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numarcs + " " + numnodes + " 11");
        for (int i = 0; i < numarcs; i++) {
            HLink l = g.getHLink(i);
            long w = (long) (Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
            pw.print(w);
            Iterator nids = l.getHNodeIds();
            while (nids.hasNext()) {
                int nid = ((Integer) nids.next()).intValue() + 1;
                pw.print(" " + nid);
            }
            pw.println("");
        }
        for (int i = 0; i < numnodes; i++) {
            HNode ni = g.getHNode(i);
            pw.println(ni.getWeightValue("cardinality"));
        }
        pw.flush();
        pw.close();
    }

    /**
     * takes as input an already constructed graph, and prints it in MeTiS
     * format in the file specified in the second argument. The format of this
     * output file is as follows: numnodes numarcs 1 ... nbor_id1 edgeweight_1
     * nbor_id2 edgeweight_2 ...
     *
     * @param g Graph
     * @param filename String
     * @throws IOException
     */
    public static void writeGraphToGRFile(Graph g, String filename) throws IOException {
        final int numarcs = g.getNumArcs();
        final int numnodes = g.getNumNodes();
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        pw.println(numnodes + " " + numarcs / 2 + " 1");
        for (int i = 0; i < numnodes; i++) {
            pw.print((i + 1) + " ");
            Set inlinks = g.getNode(i).getInLinks();
            Iterator init = inlinks.iterator();
            while (init.hasNext()) {
                Integer linkid = (Integer) init.next();
                Link l = g.getLink(linkid.intValue());
                int s = l.getStart();
                long w = (long) (2.0 * Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
                pw.print(s + " " + w + " ");
            }
            Set outlinks = g.getNode(i).getOutLinks();
            Iterator outit = outlinks.iterator();
            while (outit.hasNext()) {
                Integer linkid = (Integer) outit.next();
                Link l = g.getLink(linkid.intValue());
                int e = l.getEnd();
                long w = (long) (2.0 * Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
                pw.print(e + " " + w + " ");
            }
            pw.println("");
        }
        pw.flush();
        pw.close();
    }

    /**
     * read the HMeTiS partition result stored in filename and store it into the
     * partition array.
     *
     * @param filename String
     * @param partition int[]
     */
    public static void readPartitionFromHGROutFile(String filename, int partition[]) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            if (br.ready()) {
                int i = 0;
                while (true) {
                    String line = br.readLine();
                    if (line == null || line.length() == 0) {
                        return;
                    }
                    partition[i++] = Integer.valueOf(line.trim()).intValue() + 1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads properties from the given file and stores them as &lt;key,value&gt;
     * pairs in the returned <CODE>HashMap</CODE>. Unless the key string is a
     * special keyword (see below), the value is assumed first to be an int
     * value, and if value does not represent an int it is then assumed to be a
     * long value; if value does not represent a long it is then assumed to be a
     * double value; if value does not represent a double either, value is
     * assumed to be representing a boolean value (the strings "true" or "false"
     * in all lower- or upper-case letters); if this also fails, value is kept
     * in the hash-table as-is, that is, as a <CODE>String</CODE> type.
     * <p>
     * In case key is the keyword "graph", the key value is the next token in
     * the line, and the full filename of the file containing the graph to be
     * constructed using the method <CODE>readGraphFromFile2(String)</CODE> is
     * the next token.
     * <p>
     * In case key is the keyword "class", the key value is the next token in
     * the line, and the object to be constructed together with its string
     * arguments is given in the rest of the line. However, if the line is of
     * the form "class,mitsos,null" then the key "mitsos" is stored in props
     * along with null object value. This allows for null values to be stored in
     * the table for various keys.
     * <p>
     * In case key is the keyword "ref", the key value is the next token in the
     * line, and the next (and last) token in the line must be the name of an
     * existing key in the properties defined so far, the value of which is also
     * stored in props under the new key provided in this line.
     * <p>
     * In case key is the keyword "array", the key value is the next token in
     * the line, the next token is the type of the array ("int","long","double",
     * "boolean","string" or the full class name of the type of the objects in
     * the array) and the rest of the tokens are either the values of the
     * primitive types defined, or else each token must simply specify the name
     * of one of the keys that are already encountered (higher up in the props
     * file) and stored in the hash-table to be returned (and of course they
     * must be of the right type). The order in which the values appear defines
     * the order of elements in the array value object to be stored along with
     * the key name. Notice that when the array is supposed to hold as elements
     * other arrays, then the type of the array must be the canonical type name
     * for arrays, e.g. "[Lpopt4jlib.DblArray1Vector;" etc. Notice the "[L" in
     * the beginning of the full class name the ";" in the end, which is used to
     * indicate array-of in Java. HOWEVER, to declare that the elements of the
     * array are themselves arrays of a primitive type, use the name of the
     * primitive type followed by square brackets, e.g. "int[]" or "double[]".
     * Also notice that when the type of the elements is any non-primitive type
     * (except "String"), the resulting object stored in the return hash-map is
     * of type <CODE>Object[]</CODE>.
     * <p>
     * In case key is the keyword "arrayofcopies", the key value is the next
     * token in the line; the next token is the number of copies, and the next
     * one is the full class-name of the object whose copies shall be stored in
     * the array, followed by the argument values, in the same manner as in the
     * lines starting with the keyword "class". This is a convenient short-cut
     * so that arrays of identically constructed (but different) objects can be
     * stored in an array. The array is stored with type <CODE>Object[]</CODE>.
     * <p>
     * In case key is the keyword "dblarray", then the value is the name of the
     * property, and the next token is the filename of a text file describing
     * the array of doubles to be read via the method
     * <CODE>readDoubleLabelsFromFile(filename)</CODE>. The resulting value
     * object stored in the returned hash-map is of type <CODE>double[]</CODE>.
     * <p>
     * Similarly as above, when the key is the keyword "intarray" (now it is the
     * <CODE>readIntegerLabelsFromFile(filename)</CODE> that does the work.)
     * <p>
     * In case key is the keyword "matrix", then the value is the name of the
     * property, and the next token is the filename of a text file describing
     * the matrix to be read via the method
     * <CODE>readMatrixFromFile(filename)</CODE>. The resulting value object
     * stored in the returned hash-map is of type <CODE>double[][]</CODE>.
     * <p>
     * In case key is the keyword "matrix01" then things work as above, but now
     * the cells of the matrix are all in [0,1] by subtracting from each cell
     * the column minimum and dividing the difference by the difference of the
     * column max minus the column min.
     * <p>
     * In case key is the keyword "matrix-1_1" then things work as above, but
     * now the cells of the matrix are all in [-1,1].
     * <p>
     * In case key is the keyword "matrixN01" then things work as above but now
     * the columns of the matrix have zero mean and unit standard deviation.
     * <p>
     * In case key is the keyword "matrixN01o" then the columns of the matrix
     * are shifted by the mean and variance found in the matrix described in the
     * file whose name is described last in the same line; this means that in
     * such a line, there must be 2 specified filenames, first the required one,
     * then the file containing the data for which we require their mean and
     * variance.
     * <p>
     * In case key is the keyword "matrix-1_1o" then the columns of the matrix
     * are transformed by first subtracting from each cell the column minimum of
     * the matrix described in the file whose name is described last in the same
     * line (as above), and then dividing the difference by the difference of
     * the other matrix column max minus column min. The result does NOT have to
     * be that all cells are in [-1,1] (but should be close.)
     * <p>
     * In case key is the keyword "sparsematrix", the the value is the name of
     * the property, and the next token is the filename of a text file
     * describing the matrix to be read via the method
     * <CODE>readSparseVectorsFromFile</CODE> and the resulting object stored as
     * a value in the returned hash-map is a
     * <CODE>java.util.Vector&lt;DblArray1SparseVector&gt;</CODE> object.
     * <p>
     * In case key is the keyword "rndgen", the <CODE>RndUtil</CODE> class's
     * seed is populated with the value of this line (long int). The seed does
     * not need to be stored in the <CODE>HashMap</CODE> returned. Also, if a
     * 3rd argument is provided, it indicates the number of distinct threads to
     * require access to RndUtil, and thus it sets the required extra instances
     * of Random objects needed.
     * <p>
     * In case key is the keyword "dbglvl", the method
     * <CODE>utils.Messenger.getInstance(classname).setDebugLevel(lvl)</CODE> is
     * invoked where the value of the string classname is the next comma
     * separated value in the line, and the lvl value is the value after that in
     * the same line. If the classname is omitted in the line, the value
     * "default" is assumed.
     * <p>
     * In case key is the keyword "dbgclasses" then value is assumed to be an
     * integer, and the method <CODE>utils.Debug.setDebugBit(val)</CODE> is
     * called with the value represented by the value string.
     * <p>
     * In case key is the keyword "mpc.maxsize", the
     * <CODE>parallel.MsgPassingCoordinator</CODE> class's max data size is set
     * to the value of this line (int), via a call to
     * <CODE>MsgPassingCoordinator.setMaxSize(val)</CODE>.
     * <p>
     * In case key is the keyword "rpp.poolsize", the
     * <CODE>parallel.RegisteredParcelPool</CODE> class's pool size is set to
     * the value of this line (int), via a call to
     * <CODE>RegisteredParcelThreadLocalPools.setPoolSize(val)</CODE>.
     *
     * <p>
     * File format is:
     * <PRE>
     * key,value[,classname|graphfilename][,opts]
     * [...]
     * </PRE>.
     * <p>
     * Empty lines and lines starting with # are ignored.</p>
     *
     * @param filename String
     * @throws IOException
     * @return Hashtable
     */
    public synchronized static Hashtable readPropsFromFile(String filename)
            throws IOException {
        Hashtable props = new Hashtable();
        BufferedReader br = null;
        //Messenger mger = Messenger.getInstance();
        try {
            br = new BufferedReader(new FileReader(filename));
            String line = null;
            StringTokenizer st = null;
            if (br.ready()) {
                while (true) {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    } else if (line.length() == 0 || line.startsWith("#")) {
                        continue;
                    }
                    //mger.msg("DataMgr.readPropsFromFile(): line=" + line, 2);
                    st = new StringTokenizer(line, ",");
                    String key = st.nextToken();
                    String strval = st.nextToken();
                    if ("class".equals(key)) {
                        String classname = st.nextToken();
                        if ("null".equals(classname)) {  // specifies null val 4 strval key
                            props.put(strval, null);
                            continue;
                        }
                        String ctrargs = "";
                        while (st.hasMoreTokens()) {
                            // ctrargs = st.nextToken().trim();
                            ctrargs += st.nextToken();
                            if (st.hasMoreTokens()) {
                                ctrargs += ",";
                            }
                        }
                        try {
                            if (ctrargs.length() > 0) {
                                Class cl = Class.forName(classname);
                                Pair p = getArgTypesAndObjs(ctrargs, props);
                                Class[] argtypes = (Class[]) p.getFirst();
                                Object[] args = (Object[]) p.getSecond();
                // Constructor ctor = cl.getConstructor(argtypes);
                                // itc-20200418: the above only works if there exists a 
                                // constructor having the exact type arguments specified in 
                                // argtypes. If the constructor expects a super-type of the 
                                // type inside the argtypes array, the getConstructor(argtypes)
                                // simply throws.
                                Constructor ctor = null;
                                Constructor[] all_ctors = cl.getConstructors();
                                for (int c = 0; c < all_ctors.length; c++) {
                                    Constructor cc = all_ctors[c];
                                    Class[] cc_param_types = cc.getParameterTypes();
                                    if (cc_param_types.length == argtypes.length) {  // 1st match
                                        boolean found = true;
                                        for (int d = 0; d < argtypes.length; d++) {
                                            Class argtyped = argtypes[d];
                                            Class ctorargd = cc_param_types[d];
                                            if (!ctorargd.isAssignableFrom(argtyped)) {
                                                found = false;
                                                break;
                                            }
                                        }
                                        if (found) {  // cc constructor works, just use it! 
                                            ctor = cc;
                                            break;
                                        }
                                    }
                                }
                                if (ctor == null) {  // throw IllegalArgumentException instead of
                                    // NullPointerException
                                    throw new IllegalArgumentException(
                                            "DataMgr.readPropsFromFile(): no constructor "
                                            + "for data described in line: '" + line + "' found");
                                }
                                Object obj = ctor.newInstance(args);
                                props.put(strval, obj);
                                continue;
                            } else {
                                Class cl = Class.forName(classname);
                                Object obj = cl.newInstance();
                                props.put(strval, obj);
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();  // print stack-trace and ignore
                            continue;
                        }
                    } else if ("ref".equals(key)) {  // next token is key-name, and
                        // last token is name of key in props!
                        key = strval;
                        try {
                            String refname = st.nextToken();
                            Object refval = props.get(refname);
                            props.put(key, refval);
                            continue;
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else if ("array".equals(key)) {  // must construct an array
                        try {
                            key = strval;  // name for the array to be constructed
                            String type = st.nextToken();
                            Class cl = null;
                            if ("int".equals(type)) {
                                cl = int.class;
                            } else if ("long".equals(type)) {
                                cl = long.class;
                            } else if ("double".equals(type)) {
                                cl = double.class;
                            } else if ("boolean".equals(type)) {
                                cl = boolean.class;
                            } else if ("string".equals(type)) {
                                cl = String.class;
                            } else {
                                if ("int[]".equals(type)) {
                                    cl = int[].class;
                                } else if ("long[]".equals(type)) {
                                    cl = long[].class;
                                } else if ("double[]".equals(type)) {
                                    cl = double[].class;
                                } else if ("boolean[]".equals(type)) {
                                    cl = boolean[].class;
                                } else if ("string[]".equals(type)) {
                                    cl = String[].class;
                                } else // non-primitive type 
                                {
                                    cl = Class.forName(type);
                                }
                            }
                            // read the values in the rest of the line.
                            List arrels = new ArrayList();
                            while (st.hasMoreTokens()) {
                                String name = st.nextToken();
                                if (props.containsKey(name)) {
                                    Object val = props.get(name);
                                    if (!cl.isInstance(val)) {
                                        throw new IllegalArgumentException(
                                                "Object " + val
                                                + " inside current props is of type "
                                                + val.getClass() + " but type " + type + " expected.");
                                    }
                                    arrels.add(val);
                                } else {
                                    // ignore name, just interpret value and add to arrels
                                    if ("int".equals(type)) {
                                        arrels.add(new Integer(Integer.parseInt(name)));
                                    } else if ("long".equals(type)) {
                                        arrels.add(new Long(Long.parseLong(name)));
                                    } else if ("double".equals(type)) {
                                        arrels.add(new Double(Double.parseDouble(name)));
                                    } else if ("boolean".equals(type)) {
                                        arrels.add(new Boolean(Boolean.parseBoolean(name)));
                                    } else if ("string".equals(type)) {
                                        arrels.add(name);
                                    } else {
                                        throw new IllegalArgumentException(
                                                "don't know that type");
                                    }
                                }
                            }
                            // now create the right type array and include in props
                            if ("int".equals(type)) {
                                int[] result = new int[arrels.size()];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = ((Integer) arrels.get(i)).intValue();
                                }
                                props.put(key, result);
                                continue;
                            } else if ("long".equals(type)) {
                                long[] result = new long[arrels.size()];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = ((Long) arrels.get(i)).longValue();
                                }
                                props.put(key, result);
                                continue;
                            } else if ("double".equals(type)) {
                                double[] result = new double[arrels.size()];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = ((Double) arrels.get(i)).doubleValue();
                                }
                                props.put(key, result);
                                continue;
                            } else if ("boolean".equals(type)) {
                                boolean[] result = new boolean[arrels.size()];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = ((Boolean) arrels.get(i)).booleanValue();
                                }
                                props.put(key, result);
                                continue;
                            } else if ("string".equals(type)) {
                                String[] result = new String[arrels.size()];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = (String) arrels.get(i);
                                }
                                props.put(key, result);
                                continue;
                            } else {
                                // return the array of complex type as Object[].
                                Object[] results = new Object[arrels.size()];
                                for (int i = 0; i < results.length; i++) {
                                    results[i] = arrels.get(i);
                                }
                                props.put(key, results);
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else if ("arrayofcopies".equals(key)) {
						// example line: 
                        // arrayofcopies,myarray,10,popt4jlib.neural.HardThres,1.0
                        key = strval;
                        try {
                            int num_copies = Integer.parseInt(st.nextToken());
                            Object[] ac = new Object[num_copies];
                            String classname = st.nextToken();
                            Class cl = Class.forName(classname);
                            String ctrargs = "";
                            while (st.hasMoreTokens()) {
                                // ctrargs = st.nextToken().trim();
                                ctrargs += st.nextToken();
                                if (st.hasMoreTokens()) {
                                    ctrargs += ",";
                                }
                            }
                            Constructor ctor = null;
                            Object[] args = null;
                            if (ctrargs.length() > 0) {
                                Pair p = getArgTypesAndObjs(ctrargs, props);
                                Class[] argtypes = (Class[]) p.getFirst();
                                args = (Object[]) p.getSecond();
                                Constructor[] all_ctors = cl.getConstructors();
                                for (int c = 0; c < all_ctors.length; c++) {
                                    Constructor cc = all_ctors[c];
                                    Class[] cc_param_types = cc.getParameterTypes();
                                    if (cc_param_types.length == argtypes.length) {  // 1st match
                                        boolean found = true;
                                        for (int d = 0; d < argtypes.length; d++) {
                                            Class argtyped = argtypes[d];
                                            Class ctorargd = cc_param_types[d];
                                            if (!ctorargd.isAssignableFrom(argtyped)) {
                                                found = false;
                                                break;
                                            }
                                        }
                                        if (found) {  // cc constructor works, just use it! 
                                            ctor = cc;
                                            break;
                                        }
                                    }
                                }
                            }
                            for (int i = 0; i < num_copies; i++) {
                                if (ctor == null) {
                                    Object obj = cl.newInstance();
                                    ac[i] = obj;
                                    continue;
                                }
                                // ctor has arguments
                                ac[i] = ctor.newInstance(args);
                            }
                            props.put(key, ac);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    } else if ("dblarray".equals(key)) {
                        key = strval;
                        String dafile = st.nextToken();
                        double[] da = readDoubleLabelsFromFile(dafile);
                        props.put(key, da);
                        continue;
                    } else if ("intarray".equals(key)) {
                        key = strval;
                        String iafile = st.nextToken();
                        int[] ia = readIntegerLabelsFromFile(iafile);
                        props.put(key, ia);
                        continue;
                    } else if ("rndgen".equals(key)) {
                        long seed = Long.parseLong(strval);
                        RndUtil.getInstance().setSeed(seed);
                        /*
                        if (st.hasMoreTokens()) {  // next token indicates num-threads
                            int nt = Integer.parseInt(st.nextToken());
                            RndUtil.addExtraInstances(nt);
                        }
                        */
                        continue;
                    } else if ("graph".equals(key)) {
                        try {
                            String graphfilename = st.nextToken();
                            Graph g = readGraphFromFile2(graphfilename);
                            props.put(strval, g);
                        } catch (Exception e) {
                            e.printStackTrace();  // ignore
                        }
                        continue;
                    }
          // figure out what is strval: try int, long, double, boolean in that 
                    // order.
                    // as catch-all, keep as String.
                    try {
                        Integer v = new Integer(strval);
                        props.put(key, v);
                    } catch (NumberFormatException e) {
                        try {
                            Long v = new Long(strval);
                            props.put(key, v);
                        } catch (NumberFormatException e1) {
                            try {
                                Double v = new Double(strval);
                                props.put(key, v);
                            } catch (NumberFormatException e2) {
								// strval cannot be interpreted as a number.
                                // check out if it is boolean
                                if ("true".equals(strval) || "TRUE".equals(strval)
                                        || "false".equals(strval) || "FALSE".equals(strval)) {
                                    Boolean b = new Boolean(strval);
                                    props.put(key, b);
                                } // finally, cannot represent as anything else, store as string
                                else {
                                    props.put(key, strval);
                                }
                            }
                        }
                    }
                }
            }
            return props;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    private static Pair getArgTypesAndObjs(String line, Hashtable currentProps) {
        if (line == null || line.length() == 0) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(line, ",");
        int numargs = st.countTokens();  // used to be st.countTokens()/2;
        Class[] argtypes = new Class[numargs];
        Object[] argvals = new Object[numargs];
        Pair result = new Pair(argtypes, argvals);
        int i = 0;
        while (st.hasMoreTokens()) {
            //String objname = st.nextToken();
            String objval = st.nextToken();
      // figure out what is strval: 
            // try int, long, double, boolean, already-read-in-prop, string
            try {
                Integer v = new Integer(objval);
                argtypes[i] = int.class;  // used to be v.getClass();
                argvals[i++] = v;
            } catch (NumberFormatException e) {
                try {
                    Long v = new Long(objval);
                    argtypes[i] = long.class;  // used to be v.getClass();
                    argvals[i++] = v;
                } catch (NumberFormatException e1) {
                    try {
                        Double v = new Double(objval);
                        argtypes[i] = double.class;  // used to be v.getClass();
                        argvals[i++] = v;
                    } catch (NumberFormatException e2) {
						// strval cannot be interpreted as a number.
                        // check out if it is boolean
                        if ("true".equals(objval) || "TRUE".equals(objval)
                                || "false".equals(objval) || "FALSE".equals(objval)) {
                            Boolean b = new Boolean(objval);
                            argtypes[i] = boolean.class;  // used to be b.getClass();
                            argvals[i++] = b;
                        } else {
                            // check if it is already in currentProps
                            Object v = currentProps.get(objval);  // used to be objname
                            if (v != null) {
                                argtypes[i] = v.getClass();
                                argvals[i++] = v;
                            } else {
                                // finally, cannot represent as anything else, must be a string
                                argtypes[i] = "".getClass();  // String.class ok too
                                argvals[i++] = objval;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }


  /**
   * reads numbers from a file (one double in each consecutive line) and
   * returns them as an double[].
   * @param filename String
   * @throws IOException
   * @return double[]
   */
  public static double[] readDoubleLabelsFromFile(String filename) 
		throws IOException {
    BufferedReader br = null;
    Vector ls=new Vector();
    try {
      br = new BufferedReader(new FileReader(filename));
      if (br.ready()) {
        while (true) {
          String line=br.readLine();
          if (line==null) break;  // EOF
          double li = Double.parseDouble(line);
          ls.addElement(new Double(li));
        }
      }
    }
    finally {
      if (br!=null) br.close();
    }
    int numdocs = ls.size();
    double labels[] = new double[numdocs];
    for (int i=0; i<numdocs; i++)
      labels[i]=((Double) ls.elementAt(i)).doubleValue();
    return labels;
  }
  
  
  /**
   * reads numbers from a file (one integer in each consecutive line) and
   * returns them as an int[].
   * @param filename String
   * @throws IOException
   * @return int[]
   */
  public static int[] readIntegerLabelsFromFile(String filename) 
		throws IOException {
    BufferedReader br = null;
    Vector ls=new Vector();
    try {
      br = new BufferedReader(new FileReader(filename));
      if (br.ready()) {
        while (true) {
          String line=br.readLine();
          if (line==null) break;  // EOF
          int li = Integer.parseInt(line);
          ls.addElement(new Integer(li));
        }
      }
    }
    finally {
      if (br!=null) br.close();
    }
    int numdocs = ls.size();
    int labels[] = new int[numdocs];
    for (int i=0; i<numdocs; i++)
      labels[i]=((Integer) ls.elementAt(i)).intValue();
    return labels;
  }

    
    /**
     * In case key is the keyword class, the key value is the next token in the
     * line, and the object to be constructed together with its string arguments
     * is given in the rest of the line In case key is the keyword rndgen, the
     * RndUtil class's seed is populated with the value of this line (long int).
     * The seed does not need to be stored in the Hashtable returned. file
     * format is key,value[,classname][,opts] [...]
     *
     * @param filename String
     * @throws IOException
     * @return Hashtable
     */
    public static Hashtable readPropsFromFileOld(String filename) throws IOException {
        Hashtable props = new Hashtable();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            String line = null;
            StringTokenizer st = null;
            if (br.ready()) {
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, ",");
                    String key = st.nextToken();
                    String strval = st.nextToken();
                    if ("class".equals(key)) {
                        String classname = st.nextToken();
                        String ctrargs = null;
                        if (st.hasMoreTokens()) {
                            ctrargs = "";
                        }
                        while (st.hasMoreTokens()) {
                            ctrargs += st.nextToken();
                            if (st.hasMoreTokens()) {
                                ctrargs += ",";
                            }
                        }
                        try {
                            if (ctrargs != null && ctrargs.length() > 0) {
                                Class cl = Class.forName(classname);
                                Pair p = getArgTypesAndObjsOld(ctrargs, props);
                                Class[] argtypes = (Class[]) p.getFirst();
                                Object[] args = (Object[]) p.getSecond();
                                Constructor ctor = cl.getConstructor(argtypes);
                                Object obj = ctor.newInstance(args);
                                props.put(strval, obj);
                            } else {
                                Class cl = Class.forName(classname);
                                Object obj = cl.newInstance();
                                props.put(strval, obj);
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else if ("rndgen".equals(key)) {
                        long seed = Long.parseLong(strval);
                        RndUtil.getInstance().setSeed(seed);
                        continue;
                    }
                    // figure out what is strval
                    try {
                        Integer v = new Integer(strval);
                        props.put(key, v);
                    } catch (NumberFormatException e) {
                        try {
                            Double v = new Double(strval);
                            props.put(key, v);
                        } catch (NumberFormatException e2) {
              // strval cannot be interpreted as a number.
                            // check out if it is boolean
                            if ("true".equals(strval) || "TRUE".equals(strval)
                                    || "false".equals(strval) || "FALSE".equals(strval)) {
                                Boolean b = new Boolean(strval);
                                props.put(key, b);
                            } // finally, cannot represent as anything else, store as string
                            else {
                                props.put(key, strval);
                            }
                        }
                    }
                }
            }
            return props;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * read and return a Vector<MLInstance> objects from a tests.data file in
     * UCI standard data format (also input for C4.5 and SLIPPER). The
     * optionsfile is a text file containing lines (in any order) of the form:
     *
     * numattributes, <num>
     * numinstances, <num>
     * missing_value_symbol, <String>
     * separator_symbol, <String>
     * numeric_var_pos, <num> // num in [1...numattributes+1] ignore_pos, <num>
     * // num in [1...numattributes+1] class_label_pos,<num> // num in
     * [1...numattributes+1] num_classes, <num> // not used at this point
     *
     * @param datafile String
     * @param optionsfile String
     * @throws IOException
     * @return Vector
     */
    public static Vector readMLInstancesFromFile(String datafile,
            String optionsfile) throws IOException {
        int num_attrs = 0, num_insts = 0, class_label_pos = 0, num_classes = 0;
        Set numeric_var_pos = new HashSet();  // Set<Integer num_var_pos>
        Set ignore_var_pos = new HashSet();  // Set<Integer ignore_var_pos>
        String missing_value_symbol = "";
        String separator_symbol = ",";  // default
        Vector insts = new Vector(); // Vector<MLInstance>
        // first read the optionsfile to be able to read the datafile
        BufferedReader br = new BufferedReader(new FileReader(optionsfile));
        try {
            String line = null;
            StringTokenizer st = null;
            if (br.ready()) {
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    st = new StringTokenizer(line, ",");
                    String key = st.nextToken().trim();
                    String val = st.nextToken().trim();
                    if ("numattributes".equals(key)) {
                        num_attrs = Integer.parseInt(val);
                    } else if ("numinstances".equals(key)) {
                        num_insts = Integer.parseInt(val);
                    } else if ("missing_value_symbol".equals(key)) {
                        missing_value_symbol
                                = val;
                    } else if ("separator_symbol".equals(key)) {
                        if (val.length() > 0) {
                            separator_symbol = val;
                        } else {
                            separator_symbol = " \t";
                        }
                    } else if ("numeric_var_pos".equals(key)) {
                        numeric_var_pos.add(new Integer(val));
                    } else if ("ignore_pos".equals(key)) {
                        ignore_var_pos.add(new Integer(val));
                    } else if ("class_label_pos".equals(key)) {
                        class_label_pos = Integer.parseInt(val);
                    } else if ("num_classes".equals(key)) {
                        num_classes = Integer.parseInt(
                                val);
                    }
                }
            }
            // now read the datafile
            String class_label = null;
            Vector vattrs = new Vector();
            br.close();
            br = new BufferedReader(new FileReader(datafile));
            if (br.ready()) {
                while (true) {
                    line = br.readLine();
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    vattrs.clear();
                    vattrs.addElement(null); // first position (0) doesn't count
                    class_label = null;
                    st = new StringTokenizer(line, separator_symbol);
                    int i = 0;
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken().trim();
                        i++;
                        if (i == class_label_pos) {
                            class_label = tok;
                            vattrs.addElement(null); // must maintain order in vattrs
                        } else if (ignore_var_pos.contains(new Integer(i))) {
                            vattrs.addElement(null); // ignore
                        } else if (missing_value_symbol.equals(tok)) {
                            vattrs.addElement(null);
                        } else if (numeric_var_pos.contains(new Integer(i))) {
                            try {
                                vattrs.addElement(new Double(tok));
                            } catch (NumberFormatException e) {
                                Integer tokI = new Integer(tok);
                                vattrs.addElement(new Double(tokI.intValue()));
                            }
                        } else {
                            vattrs.addElement(tok);
                        }
                    }
                    MLInstance ml = new MLInstance(vattrs, numeric_var_pos, class_label);
                    insts.addElement(ml);
                }
            }
            return insts;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * construct the similarity graph and the Documents vector from a Vector of
     * MLInstance objects and save them to the appropriate files. Also save the
     * labels of each MLInstance for accuracy testing construct the documents
     * file by using only the numeric attributes in the usenumerics argument
     * (Vector<Integer pos>). construct a "similarity graph" from the
     * non-numeric attributes of the Vector of MLInstance objects passed as
     * input. The graph has as many nodes as there are objects in the vector. An
     * arc will exist between 2 nodes iff the two objects have the same
     * (non-null) values for at least nummatchattrs of their non-numeric
     * attributes. Finally, labelsfile contains as many lines as there are
     * instances in the insts Vector. The i-th line in the file only contains
     * the label of the i-th instance in insts.
     *
     * @param insts Vector
     * @param usenumerics Vector
     * @param nummatchingattrs int
     * @param docsfile String
     * @param graphfile String
     * @param labelsfile String
     * @throws IOException
     * @throws MLException
     */
    public static void makeDataSetFromMLInstances(Vector insts,
            Vector usenumerics,
            int nummatchattrs,
            String docsfile,
            String graphfile,
            String labelsfile) throws IOException, MLException {
        // first create the docsfile
        int tot_dims;
        final boolean use_numerics = usenumerics != null && usenumerics.size() > 0;
        Set numerics = ((MLInstance) insts.elementAt(0)).getNumericAttrInds();
        if (use_numerics) {
            tot_dims = usenumerics.size();
        } else {
            tot_dims = numerics.size();
        }
        final int insts_size = insts.size();
        PrintWriter pw = new PrintWriter(new FileOutputStream(docsfile));
        PrintWriter pwlabel = null;
        if (labelsfile != null) {
            pwlabel = new PrintWriter(new FileOutputStream(labelsfile));
        }
        pw.println(insts_size + " " + tot_dims);
        for (int i = 0; i < insts_size; i++) {
            MLInstance mli = (MLInstance) insts.elementAt(i);
            // write down the label of mli in labelsfile
            if (pwlabel != null) {
                pwlabel.println(mli.getClassLabel());
            }
            // write down only the numerics in usenumerics if not null
            if (use_numerics) {
                int k = 1;
                for (int j = 0; j < usenumerics.size(); j++) {
                    int dim = ((Integer) usenumerics.elementAt(j)).intValue();
                    Number val = mli.getNumericAttrValueAt(dim);
                    pw.print(k + "," + val + " ");
                    k++;
                }
            } else {
                // use all numeric attributes
                int k = 1;
                Iterator it = numerics.iterator();
                while (it.hasNext()) {
                    Integer ind = (Integer) it.next();
                    Number val = mli.getNumericAttrValueAt(ind.intValue());
                    pw.print(k + "," + val + " ");
                    k++;
                }
            }
            pw.println("");  // new line
        }
        pw.flush();
        pw.close();
        if (pwlabel != null) {
            pwlabel.flush();
            pwlabel.close();
        }
    // finally create graph
        // make the arcs: for each instance, check all other instances and
        // see if it matches (with non-nulls) at least nummatchattrs.
        // Strength of arc is number of matches above that.
        Vector arc_lines = new Vector();  // Vector<String "starta enda weight">
        for (int i = 0; i < insts_size; i++) {
            MLInstance mli = (MLInstance) insts.elementAt(i);
            for (int j = i + 1; j < insts_size; j++) {
                MLInstance mlj = (MLInstance) insts.elementAt(j);
                // compare mli and mlj
                int strength = mli.getNumCommonValsWith(mlj);
                if (strength >= nummatchattrs) {
                    int w = strength - nummatchattrs + 1;
                    String line = i + " " + j + " " + w;
                    arc_lines.addElement(line);
                }
            }
        }
        pw = new PrintWriter(new FileOutputStream(graphfile));
        pw.println(insts_size + " " + arc_lines.size());
        for (int i = 0; i < arc_lines.size(); i++) {
            String line = (String) arc_lines.elementAt(i);
            pw.println(line);
        }
        pw.close();
    }

    /**
     * create an MPS file describing the Set Covering Problem min. c'x s.t.
     * Ax>=e, e'x=k, x in B^n This problem basically states that we want to find
     * the best covering of the objects in the rows of A, using partitions that
     * are represented in the columns of A. Each partition i has a cost c_i
     * which is the cost of the cluster containing the points of partition i.
     * Obtaining a partition from a good covering can be easily done via
     * heuristics removing from partitions a point that also belongs to another.
     *
     * @param A DoubleMatrix2D
     * @param c DoubleMatrix1D
     * @param k int
     * @param filename String
     */
    public static void writeSCP2MPSFile(DoubleMatrix2D A, DoubleMatrix1D c, int k, String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        // write problem name
        pw.println("NAME           SETCOVPROB");
        pw.println("ROWS");
        pw.println(" N  COST");
        int num_vars = A.columns();
        int num_constrs = A.rows() + 1;
        // declare the A rows
        for (int i = 0; i < num_constrs - 1; i++) {
            String l = newMPSLine();
            String l2 = writeMPSField(l, 0, "G");
            String ci = "CON" + i;
            String l3 = writeMPSField(l2, 1, ci);
            pw.println(l3);
        }
        pw.println(" E  COVCTR");  // the c'x = k
        // now the vars
        pw.println("COLUMNS");
        pw.flush();
        // NumberFormat nf = new DecimalFormat("0.#######E0");
        NumberFormat nf = new DecimalFormat("00000000E0");
        for (int i = 0; i < num_vars; i++) {
            String l = newMPSLine();
            String l2 = writeMPSField(l, 1, "X" + i);
            String l3 = writeMPSField(l2, 2, "COST");
            double ci = c.getQuick(i);
            String cival = nf.format(ci);
            String l4 = writeMPSField(l3, 3, cival);
            String l5 = writeMPSField(l4, 4, "CON0");
            String ai1val = nf.format(A.getQuick(0, i));
            String l6 = writeMPSField(l5, 5, ai1val);
            pw.println(l6);
            pw.flush();
            // print the A coeff for x_i var
            for (int j = 1; j < num_constrs - 1; j++) {
                String l7 = newMPSLine();
                String l8 = writeMPSField(l7, 1, "X" + i);
                String l9 = writeMPSField(l8, 2, "CON" + j);
                String aijval = nf.format(A.getQuick(j, i));
                String l10 = writeMPSField(l9, 3, aijval);
                if (j == num_constrs - 2) {
                    if (Math.abs(A.getQuick(j, i)) > 1.e-8) {  // print only nonzero values
                        pw.println(l10);
                        pw.flush();
                    }
                    break;
                }
                j++;
                String l11 = writeMPSField(l10, 4, "CON" + j);
                aijval = nf.format(A.getQuick(j, i));
                String l12 = writeMPSField(l11, 5, aijval);
                if (Math.abs(A.getQuick(j, i)) > 1.e-8 || Math.abs(A.getQuick(j - 1, i)) > 1.e-8) {
                    // only print a line having at least one non-zero element
                    pw.println(l12);
                    pw.flush();
                }
            }
            // last constraint c'x=k
            l = newMPSLine();
            l2 = writeMPSField(l, 1, "X" + i);
            l3 = writeMPSField(l2, 2, "COVCTR");
            l4 = writeMPSField(l3, 3, "1");
            pw.println(l4);
            pw.flush();
        }
        // RHS
        pw.println("RHS");
        for (int i = 0; i < num_constrs - 1; i++) {
            String l = newMPSLine();
            String l2 = writeMPSField(l, 1, "RHS1");
            String l3 = writeMPSField(l2, 2, "CON" + i);
            String l4 = writeMPSField(l3, 3, "1");
            pw.println(l4);
        }
        pw.println("    RHS1      COVCTR    " + k);
        pw.flush();
        // BOUNDS
        pw.println("BOUNDS");
        for (int i = 0; i < num_vars; i++) {
            String l = newMPSLine();
            String l2 = writeMPSField(l, 0, "UI");
            String l3 = writeMPSField(l2, 1, "X" + i);
            String l4 = writeMPSField(l3, 2, "1");
            pw.println(l4);
        }
        // end
        pw.println("ENDATA");
        pw.flush();
        pw.close();
    }

    public static void writeSCP2MPSFileFast(DoubleMatrix2D A, DoubleMatrix1D c, int k, String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        // write problem name
        pw.println("NAME           SETCOVPROB");
        pw.println("ROWS");
        pw.println(" N  COST");
        int num_vars = A.columns();
        int num_constrs = A.rows() + 1;
        // declare the A rows
        for (int i = 0; i < num_constrs - 1; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 0, "G");
            String ci = "CON" + i;
            writeMPSFieldFast(l, 1, ci);
            pw.println(l);
        }
        pw.println(" E  COVCTR");  // the c'x = k
        // now the vars
        pw.println("COLUMNS");
        pw.flush();
        // NumberFormat nf = new DecimalFormat("0.#######E0");
        NumberFormat nf = new DecimalFormat("00000000E0");
        for (int i = 0; i < num_vars; i++) {
            if (i % 1000 == 0) {
                System.out.println("Now writing column " + i);  // itc: HERE rm asap
            }
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "X" + i);
            writeMPSFieldFast(l, 2, "COST");
            double ci = c.getQuick(i);
            String cival = nf.format(ci);
            writeMPSFieldFast(l, 3, cival);
            writeMPSFieldFast(l, 4, "CON0");
            String ai1val = nf.format(A.getQuick(0, i));
            writeMPSFieldFast(l, 5, ai1val);
            pw.println(l);
            pw.flush();
            // print the A coeff for x_i var
            for (int j = 1; j < num_constrs - 1; j++) {
                l = newMPSLineBuffer();
                writeMPSFieldFast(l, 1, "X" + i);
                writeMPSFieldFast(l, 2, "CON" + j);
                String aijval = nf.format(A.getQuick(j, i));
                writeMPSFieldFast(l, 3, aijval);
                if (j == num_constrs - 2) {
                    if (Math.abs(A.getQuick(j, i)) > 1.e-8) {  // print only nonzero values
                        pw.println(l);
                        pw.flush();
                    }
                    break;
                }
                j++;
                writeMPSFieldFast(l, 4, "CON" + j);
                aijval = nf.format(A.getQuick(j, i));
                writeMPSFieldFast(l, 5, aijval);
                if (Math.abs(A.getQuick(j, i)) > 1.e-8 || Math.abs(A.getQuick(j - 1, i)) > 1.e-8) {
                    // only print a line having at least one non-zero element
                    pw.println(l);
                    pw.flush();
                }
            }
            // last constraint c'x=k
            l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "X" + i);
            writeMPSFieldFast(l, 2, "COVCTR");
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
            pw.flush();
        }
        // RHS
        pw.println("RHS");
        for (int i = 0; i < num_constrs - 1; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "RHS1");
            writeMPSFieldFast(l, 2, "CON" + i);
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
        }
        pw.println("    RHS1      COVCTR    " + k);
        pw.flush();
        // BOUNDS
        pw.println("BOUNDS");
        for (int i = 0; i < num_vars; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 0, "UI");
            writeMPSFieldFast(l, 1, "BOUND");
            writeMPSFieldFast(l, 2, "X" + i);
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
        }
        // end
        pw.println("ENDATA");
        pw.flush();
        pw.close();
    }

    public static void writeSPP2MPSFileFast(DoubleMatrix2D A, DoubleMatrix1D c, int k, String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        // write problem name
        pw.println("NAME           SETPARPROB");
        // write objsense
        pw.println("OBJSENSE");
        pw.println(" MIN");
        pw.println("ROWS");
        pw.println(" N  COST");
        int num_vars = A.columns();
        int num_constrs = A.rows() + 1;
        // declare the A rows
        for (int i = 0; i < num_constrs - 1; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 0, "E");
            String ci = "CON" + i;
            writeMPSFieldFast(l, 1, ci);
            pw.println(l);
        }
        pw.println(" E  PARCTR");  // the c'x = k
        // now the vars
        pw.println("COLUMNS");
        pw.flush();
        // NumberFormat nf = new DecimalFormat("0.#######E0");
        NumberFormat nf = new DecimalFormat("00000000E0");
        for (int i = 0; i < num_vars; i++) {
            if (i % 1000 == 0) {
                System.out.println("Now writing column " + i);  // itc: HERE rm asap
            }
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "X" + i);
            writeMPSFieldFast(l, 2, "COST");
            double ci = c.getQuick(i);
            String cival = nf.format(ci);
            writeMPSFieldFast(l, 3, cival);
            writeMPSFieldFast(l, 4, "CON0");
            String ai1val = nf.format(A.getQuick(0, i));
            writeMPSFieldFast(l, 5, ai1val);
            pw.println(l);
            pw.flush();
            // print the A coeff for x_i var
            for (int j = 1; j < num_constrs - 1; j++) {
                l = newMPSLineBuffer();
                writeMPSFieldFast(l, 1, "X" + i);
                writeMPSFieldFast(l, 2, "CON" + j);
                String aijval = nf.format(A.getQuick(j, i));
                writeMPSFieldFast(l, 3, aijval);
                if (j == num_constrs - 2) {
                    if (Math.abs(A.getQuick(j, i)) > 1.e-8) {  // print only nonzero values
                        pw.println(l);
                        pw.flush();
                    }
                    break;
                }
                j++;
                writeMPSFieldFast(l, 4, "CON" + j);
                aijval = nf.format(A.getQuick(j, i));
                writeMPSFieldFast(l, 5, aijval);
                if (Math.abs(A.getQuick(j, i)) > 1.e-8 || Math.abs(A.getQuick(j - 1, i)) > 1.e-8) {
                    // only print a line having at least one non-zero element
                    pw.println(l);
                    pw.flush();
                }
            }
            // last constraint c'x=k
            l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "X" + i);
            writeMPSFieldFast(l, 2, "PARCTR");
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
            pw.flush();
        }
        // RHS
        pw.println("RHS");
        for (int i = 0; i < num_constrs - 1; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "RHS1");
            writeMPSFieldFast(l, 2, "CON" + i);
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
        }
        pw.println("    RHS1      PARCTR    " + k);
        pw.flush();
        // BOUNDS
        pw.println("BOUNDS");
        for (int i = 0; i < num_vars; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 0, "UI");
            writeMPSFieldFast(l, 1, "BOUND");
            writeMPSFieldFast(l, 2, "X" + i);
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
        }
        // end
        pw.println("ENDATA");
        pw.flush();
        pw.close();
    }

    public static void writeLP2MPSFileFast(DoubleMatrix2D A, DoubleMatrix1D c, int k, String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
        // write problem name
        pw.println("NAME           SETCOVPROB");
        pw.println("ROWS");
        pw.println(" N  COST");
        int num_vars = A.columns();
        int num_constrs = A.rows() + 1;
        // declare the A rows
        for (int i = 0; i < num_constrs - 1; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 0, "G");
            String ci = "CON" + i;
            writeMPSFieldFast(l, 1, ci);
            pw.println(l);
        }
        pw.println(" E  COVCTR");  // the c'x = k
        // now the vars
        pw.println("COLUMNS");
        pw.flush();
        // NumberFormat nf = new DecimalFormat("0.#######E0");
        NumberFormat nf = new DecimalFormat("00000000E0");
        for (int i = 0; i < num_vars; i++) {
            if (i % 1000 == 0) {
                System.out.println("Now writing column " + i);  // itc: HERE rm asap
            }
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "X" + i);
            writeMPSFieldFast(l, 2, "COST");
            double ci = c.getQuick(i);
            String cival = nf.format(ci);
            writeMPSFieldFast(l, 3, cival);
            writeMPSFieldFast(l, 4, "CON0");
            String ai1val = nf.format(A.getQuick(0, i));
            writeMPSFieldFast(l, 5, ai1val);
            pw.println(l);
            pw.flush();
            // print the A coeff for x_i var
            for (int j = 1; j < num_constrs - 1; j++) {
                l = newMPSLineBuffer();
                writeMPSFieldFast(l, 1, "X" + i);
                writeMPSFieldFast(l, 2, "CON" + j);
                String aijval = nf.format(A.getQuick(j, i));
                writeMPSFieldFast(l, 3, aijval);
                if (j == num_constrs - 2) {
                    if (Math.abs(A.getQuick(j, i)) > 1.e-8) {  // print only nonzero values
                        pw.println(l);
                        pw.flush();
                    }
                    break;
                }
                j++;
                writeMPSFieldFast(l, 4, "CON" + j);
                aijval = nf.format(A.getQuick(j, i));
                writeMPSFieldFast(l, 5, aijval);
                if (Math.abs(A.getQuick(j, i)) > 1.e-8 || Math.abs(A.getQuick(j - 1, i)) > 1.e-8) {
                    // only print a line having at least one non-zero element
                    pw.println(l);
                    pw.flush();
                }
            }
            // last constraint c'x=k
            l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "X" + i);
            writeMPSFieldFast(l, 2, "COVCTR");
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
            pw.flush();
        }
        // RHS
        pw.println("RHS");
        for (int i = 0; i < num_constrs - 1; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 1, "RHS1");
            writeMPSFieldFast(l, 2, "CON" + i);
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
        }
        pw.println("    RHS1      COVCTR    " + k);
        pw.flush();
        // BOUNDS
        pw.println("BOUNDS");
        for (int i = 0; i < num_vars; i++) {
            StringBuffer l = newMPSLineBuffer();
            writeMPSFieldFast(l, 0, "UP");
            writeMPSFieldFast(l, 1, "BOUND");
            writeMPSFieldFast(l, 2, "X" + i);
            writeMPSFieldFast(l, 3, "1");
            pw.println(l);
        }
        // end
        pw.println("ENDATA");
        pw.flush();
        pw.close();
    }

    public static void print2PlotFile(String plotfile, Vector docs, int[] cinds) {
        try {
            String[] shapes = {"dot", "diamond", "box", "plus"};
            PrintWriter pw = new PrintWriter(new FileOutputStream(plotfile));
            pw.println("double double");
            pw.println("invisible 0 0");
            pw.println("invisible 150 150");  // itc: HERE rm ASAP
            Integer x = new Integer(0);
            Integer y = new Integer(1);
            for (int j = 0; j < docs.size(); j++) {
                clustering.Document dj = (clustering.Document) docs.elementAt(j);
                Double djx = dj.getDimValue(x);
                Double djy = dj.getDimValue(y);
                int ind = cinds[j];
                String shapei = shapes[ind];
                pw.println(shapei + " " + djx.doubleValue() + " " + djy.doubleValue());
            }
            pw.println("go");
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String writeMPSField(String line, int fieldnum, String val) throws IOException {
    // line must be of length 61
        // assert
        if (line == null || line.length() != 61) {
            throw new IOException("incorrect line");
        }
        String newline = line.substring(0, pos[fieldnum] - 1);
        int val_len = val.length() <= poslen[fieldnum] ? val.length() : poslen[fieldnum];
        String new_val = val.substring(0, val_len);  // cut-off the value of val if it's too big
        newline += new_val;
        for (int i = 0; i < poslen[fieldnum] - val_len; i++) {
            newline += " ";
        }
        if (line.length() > newline.length()) {
            newline += line.substring(newline.length());
        }
        return newline;
    }

    private static void writeMPSFieldFast(StringBuffer line, int fieldnum, String val) throws IOException {
    // line must be of length 61
        // assert
        if (line == null || line.length() != 61) {
            throw new IOException("incorrect line: [" + line + "]");
        }
        int val_len = val.length() <= poslen[fieldnum] ? val.length() : poslen[fieldnum];
        int endpos = pos[fieldnum] - 1 + val_len;
        line.replace(pos[fieldnum] - 1, endpos, val);
        return;
    }

    private static String newMPSLine() {
        return "                                                             ";
    }

    private static StringBuffer newMPSLineBuffer() {
        return new StringBuffer("                                                             ");
    }

    private static void writeSpaces(PrintWriter pw, int numspaces) {
        for (int i = 0; i < numspaces; i++) {
            pw.print(" ");
        }
    }

    private static Pair getArgTypesAndObjsOld(String line, Hashtable currentProps) {
        if (line == null || line.length() == 0) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(line, ",");
        int numargs = st.countTokens() / 2;
        Class[] argtypes = new Class[numargs];
        Object[] argvals = new Object[numargs];
        Pair result = new Pair(argtypes, argvals);
        int i = 0;
        while (st.hasMoreTokens()) {
            String objname = st.nextToken();
            String objval = st.nextToken();
            // figure out what is strval
            try {
                Integer v = new Integer(objval);
                argtypes[i] = v.getClass();
                argvals[i++] = v;
            } catch (NumberFormatException e) {
                try {
                    Double v = new Double(objval);
                    argtypes[i] = v.getClass();
                    argvals[i++] = v;
                } catch (NumberFormatException e2) {
          // strval cannot be interpreted as a number.
                    // check out if it is boolean
                    if ("true".equals(objval) || "TRUE".equals(objval)
                            || "false".equals(objval) || "FALSE".equals(objval)) {
                        Boolean b = new Boolean(objval);
                        argtypes[i] = b.getClass();
                        argvals[i++] = b;
                    } else {
                        // check if it is already in currentProps
                        Object v = currentProps.get(objname);
                        if (v != null) {
                            argtypes[i] = v.getClass();
                            argvals[i++] = v;
                        } else {
                            // finally, cannot represent as anything else, must be a string
                            argtypes[i] = "".getClass();
                            argvals[i++] = objval;
                        }
                    }
                }
            }
        }
        return result;
    }

    private static int countTokensInLine(String line, char delim) {
        int numtoks = 0;  // as many tokens as delims
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == delim) {
                numtoks++;
            }
        }
        if (line.length() > 0) {
            numtoks++;  // count the only token and/or the last token
        }
        return numtoks;
    }

    private static void convertTokensInLine(String line, char delim, double[] arr, int offset) {
        int prev_pos = -1;
        int numtok = 0;
        int tottok = 0;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Double.NaN;  // zero out array
        }
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == delim) {
                // from previous pos up to i-1 we have a token -maybe empty one
                int up_to = i - 1;
                if (prev_pos <= up_to) {
                    // ok, we have a token, maybe it's the first ones
                    if (++tottok <= offset) {
                        prev_pos = i;
                        continue;
                    }
                    // if it's not empty, add it
                    if (prev_pos < up_to) {
                        String sub = line.substring(prev_pos + 1, up_to + 1);
                        // substring lastIndex not included, so we have to add 1
                        arr[numtok++] = Double.parseDouble(sub);
                    } else {
                        arr[numtok++] = Double.NaN;
                    }
                }
                prev_pos = i;
            }
        }
        // last or only token
        if (prev_pos <= line.length()) {
            String sub = line.substring(prev_pos + 1, line.length());
            try {
                arr[numtok++] = Double.parseDouble(sub);
            } catch (NumberFormatException e) {
                // no-op
            }
        }
    }
}
