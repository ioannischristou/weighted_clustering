package clustering;

import java.util.*;
import utils.*;


public class GGADClusterer implements Clusterer {
  private Vector _centers;  // Vector<Document>, size=k
  private Vector _docs;  // Vector<Document>, size=n
  private Hashtable _params;
  private int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>
  double _incValue=Double.MAX_VALUE;

  private GGADThread[] _threads=null;
  private int[] _islandsPop;

  public GGADClusterer() {
    _intermediateClusters = new Vector();
  }


  public Vector getIntermediateClusters() throws ClustererException {
    /*
    // store the final clustering in _intermediateClusters
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
      vc.addElement(new Integer(i));
      _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
    }
    */
    // remove any empty vectors
    for (int i=_intermediateClusters.size()-1; i>=0; i--) {
      Vector v = (Vector) _intermediateClusters.elementAt(i);
      if (v==null || v.size()==0) _intermediateClusters.remove(i);
    }
    return _intermediateClusters;
  }


  public synchronized void addIntermediateClusters(GGAIndividual ind) throws ClustererException {
    Vector groups = ind.getGroups();
    for (int i=0; i<groups.size(); i++) {
      Vector clusteri = new Vector((Set) groups.elementAt(i));
      _intermediateClusters.addElement(clusteri);
    }
  }


  public Hashtable getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   *
   * <"numthreads",Integer nt> optional, how many threads will be used to
   * compute the clustering, default is 1
   *
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllDocuments(Vector<Document>) or
   * via repeated calls to addDocument(Document d)
   *
   * @throws Exception
   * @return Vector
   */
  public Vector clusterDocs() throws ClustererException {
    int nt = 1;
    Integer ntI = (Integer) _params.get("numthreads");
    if (ntI!=null) nt = ntI.intValue();
    if (nt<1) throw new ClustererException("invalid number of threads specified");
    RndUtil.setExtraInstances(nt);
    _threads = new GGADThread[nt];
    _islandsPop = new int[nt];
    for (int i=0; i<nt; i++) _islandsPop[i] = 0;  // init.

    parallel.Barrier.setNumThreads(nt);  // init the Barrier obj.

    for (int i=0; i<nt; i++) {
      _threads[i] = new GGADThread(this, i);
    }
    for (int i=0; i<nt; i++) {
      _threads[i].start();
    }


    for (int i=0; i<nt; i++) {
      GGADThreadAux rti = _threads[i].getGGADThreadAux();
      rti.waitForTask();
    }

    return _centers;
  }


  public void addDocument(Document d) {
    if (_docs==null) _docs = new Vector();
    _docs.addElement(d);
  }


  /**
   * adds to the end of _docs all Documents in v
   * Will throw class cast exception if any object in v is not a Document
   * @param v Vector
   */
  public void addAllDocuments(Vector v) {
    if (v==null) return;
    if (_docs==null) _docs = new Vector();
    for (int i=0; i<v.size(); i++)
      _docs.addElement((Document) v.elementAt(i));
  }


  /**
   * set the initial clustering centers
   * the vector _centers is reconstructed, but the Document objects
   * that are the cluster centers are simply passed as references.
   * the _centers doesn't own copies of them, but references to the
   * objects inside the centers vector that is passed in the param. list
   * @param centers Vector
   * @throws ClustererException
   */
  public void setInitialClustering(Vector centers) throws ClustererException {
    if (centers==null) throw new ClustererException("null initial clusters vector");
    _centers = null;  // force gc
    _centers = new Vector();
    for (int i=0; i<centers.size(); i++)
      _centers.addElement((Document) centers.elementAt(i));
  }


  public Vector getCurrentCenters() {
    return _centers;
  }


  public Vector getCurrentDocs() {
    return _docs;
  }


  /**
   * the clustering params are set to p
   * @param p Hashtable
   */
  public void setParams(Hashtable p) {
    _params = null;
    _params = new Hashtable(p);  // own the params
  }


  public void reset() {
    _docs = null;
    _centers = null;
    _clusterIndices=null;
    _intermediateClusters.clear();
  }


  public int[] getClusteringIndices() {
    return _clusterIndices;
  }


  public void setClusteringIndices(int[] a) {
    if (a==null) _clusterIndices = null;
    else {
      _clusterIndices = new int[a.length];
      for (int i=0; i<a.length; i++)
        _clusterIndices[i] = a[i];
    }
  }


  public int[] getClusterCards() throws ClustererException {
    if (_clusterIndices==null)
      throw new ClustererException("null _clusterIndices");
    final int n = _docs.size();
    // final int k = _centers.size();
    // figure out k
    int k=0;
    for (int i=0; i<n; i++)
      if (k<_clusterIndices[i]+1) k = _clusterIndices[i]+1;
    int[] cards = new int[k];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<n; i++) {
      cards[_clusterIndices[i]]++;
    }
    return cards;
  }


  public double eval(Evaluator vtor) throws ClustererException {
    return vtor.eval(this);
  }


  synchronized int getImmigrationIsland(int myid) {
    for (int i=0; i<_islandsPop.length; i++)
      if (_islandsPop[i]==0 || (myid!=i && _islandsPop[myid]>2.5*_islandsPop[i])) return i;
    return -1;
  }


  synchronized void setIslandPop(int id, int size) {
    _islandsPop[id] = size;
  }


  synchronized GGADThread getGGADThread(int id) {
    return _threads[id];
  }


  synchronized double getIncValue() {
    return _incValue;
  }


  /**
   * update the _clusterIndices if we have an incumbent
   * @param ind GGAIndividual
   */
  synchronized void setIncumbent(GGAIndividual ind) throws ClustererException {
    if (_incValue > ind.getValue()) {
      System.err.println("Updating Incumbent w/ val="+ind.getValue());
      if (_clusterIndices==null) _clusterIndices = new int[_docs.size()];
      _incValue = ind.getValue();
      Vector groups = ind.getGroups();
      for (int i=0; i<groups.size(); i++) {
        Set gi = (Set) groups.elementAt(i);
        Iterator giiter = gi.iterator();
        while (giiter.hasNext()) {
          Integer did = (Integer) giiter.next();
          _clusterIndices[did.intValue()] = i;
        }
      }
      // ensure clusters are in intermediate clusters too
      addIntermediateClusters(ind);
      // update centers
      _centers = Document.getCenters(_docs, _clusterIndices, groups.size(), null);  // itc: HERE 20220223
    }
  }
}


class GGADThread extends Thread {
  private GGADThreadAux _aux;
  // private GGADClusterer _master;

  public GGADThread(GGADClusterer master, int id) {
    _aux = new GGADThreadAux(master, id);
    // _master = master;
  }


  public GGADThreadAux getGGADThreadAux() {
    return _aux;
  }


  public void run() {
    _aux.runTask();
  }
}


class GGADThreadAux {
  private int _id;
  private GGADClusterer _master;
  private boolean _finish = false;
  private Vector _individuals;  // Vector<Individual>
  private Vector _immigrantsPool;  // Vector<Individual>
  private Integer _kI = null;
  private int _maxpopnum = 50;  // max pop. size
  private Vector _docs = null;  // Vector<Document> needed to maintain thread integrity...
  private boolean _throwOnFailedRepair = false;

  public GGADThreadAux(GGADClusterer master, int id) {
    _master = master;
    _id = id;
    _immigrantsPool = new Vector();
    // set _docs correctly
    _docs = new Vector();
    Vector mdocs = _master.getCurrentDocs();
    for (int i=0; i<mdocs.size(); i++) {
      _docs.addElement(new Document((Document) mdocs.elementAt(i)));
    }
    Boolean tofrB = (Boolean) _master.getParams().get("failedrepairthrows");
    if (tofrB!=null) _throwOnFailedRepair = tofrB.booleanValue();
  }


  public void runTask() {
    // start: do the GGA for Density-based clustering
    try {
      initPopulation();
      // System.err.println("initPopulation() done.");
      int numgens = 10;
      Integer ngI = (Integer) _master.getParams().get("numgens");
      if (ngI!=null) numgens = ngI.intValue();
      for (int gen = 0; gen < numgens; gen++) {
        System.err.println("Island-Thread id=" + _id + " running gen=" + gen +
                           " popsize=" + _individuals.size());
        recvInds();
        if (_individuals.size()==0) {
          parallel.Barrier.getInstance().barrier();  // synchronize with other threads
          continue;
        }
        nextGeneration();
        sendInds();
        updateMasterSolutions(); // update the intermediateClusters of _master
        _master.setIslandPop(_id, _individuals.size());
        parallel.Barrier.getInstance().barrier();  // synchronize with other threads
      }
    }
    catch (ClustererException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    // end: declare finish
    setFinish();
  }


  public synchronized boolean getFinish() {
    return _finish;
  }


  public synchronized void setFinish() {
    _finish = true;
    notify();
  }


  public synchronized void waitForTask() {
    while (_finish==false) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // System.err.println("Thread-"+_id+" done.");
  }


  /* GA methods */


  private void initPopulation() throws ClustererException {
    // final Vector docs = _master.getCurrentDocs();
    final int n = _docs.size();
    // final DensityEvaluator de = new DensityEvaluator();
    // final MSTDensityEvaluator de = new MSTDensityEvaluator();
    final EnhancedEvaluator de = (EnhancedEvaluator) _master.getParams().get("ggadevaluator");
    // de.setMasterDocSet(_master.getCurrentDocs());
    de.setMasterDocSet(_docs);  // itc: HERE used to be line above
    de.setParams(_master.getParams());
    _kI = (Integer) _master.getParams().get("k");
    int initpopnum = 10;
    Integer initpopnumI = ((Integer) _master.getParams().get("numinitpop"));
    if (initpopnumI!=null) initpopnum = initpopnumI.intValue();
    Clusterer use_init_clusterer = (Clusterer) _master.getParams().get("useinitclusterer");
    // compute random clusterings of the _master docs. and use it for the
    // init. population in the island (stored in _individuals)
    _individuals = new Vector();  // Vector<GGAIndividual>
    double tot_val = 0.0;
    for (int i=0; i<initpopnum; i++) {
      // create a random clustering of up to n/10 clusters if no other estimate for k exists
      int k = (_kI==null) ?
          (int) Math.floor(utils.RndUtil.getInstance(_id).getRandom().nextDouble()*n/10) :
          _kI.intValue();
      Vector groupsi = new Vector();  // Vector<Set<Integer docid> >
      for (int j=0; j<k; j++) {  // init groupsi
        groupsi.addElement(new HashSet());
      }
      int indsi[] = null;
      boolean ok = false;
      if (use_init_clusterer!=null) {
        try {
          parallel.DMCoordinator.getInstance().getWriteAccess();  // coordinate this
          Clusterer use_init_clusterer2 = (Clusterer) use_init_clusterer.
              getClass().newInstance();
          Document.setMetric((DocumentDistIntf) _master.getParams().get("metric"));
          use_init_clusterer2.addAllDocuments(_docs);
          use_init_clusterer2.setParams(_master.getParams());
          Vector init_centers = getKRandPoints(k);
          use_init_clusterer2.setInitialClustering(init_centers);
          use_init_clusterer2.clusterDocs();
          indsi = use_init_clusterer2.getClusteringIndices();
          // ensure k non-empty groups are present as clusterer's result
          int kmax=0;
          for (int j=0; j<indsi.length; j++)
            if (indsi[j]>kmax) kmax = indsi[j];
          kmax++;
          if (kmax!=k) {
            throw new ClustererException("pop-initializing clusterer produced "+
                                         kmax+" clusters.");
            // will be caught in catch clause and properly fall back to
            // random clustering
          }
          for (int j = 0; j < n; j++) {
            int g = indsi[j];
            Set gj = (Set) groupsi.elementAt(g);
            gj.add(new Integer(j));
            groupsi.set(g, gj); // ensure addition
          }
          ok = true;
          parallel.DMCoordinator.getInstance().releaseWriteAccess();
        }
        catch (Exception e) {
          e.printStackTrace();
          System.err.println("fall-back to random clustering");
          parallel.DMCoordinator.getInstance().releaseWriteAccess();
          ok = false;
        }
      }
      if (!ok) {
        indsi = new int[n];
        for (int j = 0; j < n; j++) {
          int g = (int) Math.floor(utils.RndUtil.getInstance(_id).getRandom().
                                   nextDouble() * k); // g in [0...k-1]
          indsi[j] = g; // doc w/ id j goes to cluster g
          Set gj = (Set) groupsi.elementAt(g);
          gj.add(new Integer(j));
          groupsi.set(g, gj); // ensure addition
        }
        // ensure k clusters exist
        for (int g=0; g<groupsi.size(); g++) {
          Set gg = (Set) groupsi.elementAt(g);
          if (gg==null || gg.size()==0) {
            // find a cluster w/ more than 2 elements and move one to gg
            Set gh = null;
            for (int h=0; h<groupsi.size(); h++) {
              gh = (Set) groupsi.elementAt(h);
              if (gh.size()>1) {
                Integer first = (Integer) gh.iterator().next();
                indsi[first.intValue()] = g;
                gh.remove(first);
                gg.add(first);
                groupsi.set(g, gg);
                groupsi.set(h, gh);  // ensure addition
                break;
              }
            }
          }
        }  // done ensuring k clusters in each individual of init population
      }
      double vali=0.0; double fiti = 0.0;
      try {
        vali = de.eval(_docs, indsi);
        tot_val += vali;
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      /*
      // sanity check
      int tot_objs = 0;
      for (int ii=0; ii<groupsi.size(); ii++) {
        tot_objs += ((Set) groupsi.elementAt(ii)).size();
      }
      if (tot_objs != _docs.size()) {
        throw new ClustererException("sanity test failed.");
      }
      */
      GGAIndividual indi = new GGAIndividual(groupsi, vali, fiti, _master);
      System.out.println("Individual-"+i+"="+indi);
      _individuals.addElement(indi);
    }
    // now compute fitnesses of each individual
    computeFitness();
    // finally update incumbent
    GGAIndividual best = null;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<_individuals.size(); i++) {
      GGAIndividual indi = (GGAIndividual) _individuals.elementAt(i);
      // indi.setFitness(indi.getValue()/tot_val);
      double vi = indi.getValue();
      if (vi<best_val) {
        best = indi;
        best_val = vi;
      }
    }
    _master.setIncumbent(best);  // update the master's best clustering found if needed
  }


  private synchronized void recvInds() {
    if (_immigrantsPool.size()>0) {
      _individuals.addAll(_immigrantsPool);
      _immigrantsPool.clear();
    }
  }


  private void sendInds() {
    int sendTo = _master.getImmigrationIsland(_id);
    if (sendTo>=0 && sendTo != _id) {
      Vector immigrants = getImmigrants();
      GGADThreadAux receiverThreadAux = _master.getGGADThread(sendTo).getGGADThreadAux();
      receiverThreadAux.recvIndsAux(immigrants);
      // _master.getGGADThread(sendTo).getGGADThreadAux()._immigrantsPool.addAll(immigrants);
    }
  }


  private synchronized void recvIndsAux(Vector immigrants) {
    _immigrantsPool.addAll(immigrants);
  }


  private void nextGeneration() throws ClustererException {
    // 0. init stuff
    // 1. select pairs of individuals
    // 2. do Xover
    // 3. do mutation
    // 4. compute value and fitness for each
    // 5. update age, remove old and unfit
    double xoverprob = 0.7;
    Double xoverprobD = (Double) _master.getParams().get("xoverprob");
    if (xoverprobD!=null) xoverprob = xoverprobD.doubleValue();
    int popsize = _individuals.size();
    int poplimit = 100;  // default pop limit per island
    Integer plI = (Integer) _master.getParams().get("poplimit");
    if (plI!=null) poplimit = plI.intValue();
    double piesz[] = new double[popsize];
    piesz[0] = ((GGAIndividual) _individuals.elementAt(0)).getFitness();
    double tot_val = piesz[0];
    for (int i=1; i<popsize; i++) {
      piesz[i] = piesz[i-1]+((GGAIndividual) _individuals.elementAt(i)).getFitness();
      tot_val += ((GGAIndividual) _individuals.elementAt(i)).getFitness();
    }
    // for (int i=0; i<popsize; i++) piesz[i] /= tot_val;

    for (int i=0; i<popsize; i++) {
      if (poplimit<=_individuals.size()) break;  // no more procreation this generation...
      // Xover probability
      double r = RndUtil.getInstance(_id).getRandom().nextDouble();
      if (r>xoverprob*xoverprob) continue;  // on average, we should run the exp 0.7*0.7*popsize times
      // select two individuals
      // first
      double r1 = RndUtil.getInstance(_id).getRandom().nextDouble()*tot_val;
      int parid1=0;
      while (r1>piesz[parid1]) parid1++;
      // second
      double r2 = RndUtil.getInstance(_id).getRandom().nextDouble()*tot_val;
      int parid2=0;
      while (r2>piesz[parid2]) parid2++;

      // Xover
      GGAIndividual par1 = (GGAIndividual) _individuals.elementAt(parid1);
      GGAIndividual par2 = (GGAIndividual) _individuals.elementAt(parid2);
      try {
        Pair offspring = doXover(par1, par2);  // operation may throw if
                                               // children are infeasible
        GGAIndividual child1 = (GGAIndividual) offspring.getFirst();
        GGAIndividual child2 = (GGAIndividual) offspring.getSecond();

        // Mutation
        mutate(child1, child2);

        _individuals.addElement(child1);
        _individuals.addElement(child2);
      }
      catch (ClustererException e) {
        e.printStackTrace();
        // no-op
      }
    }
    // values and fitnesses
    double min_val = computeFitness();

    // survival of the fittest
    int cutoffage = 5;
    double varage = 0.9;
    Integer coaI = (Integer) _master.getParams().get("cutoffage");
    if (coaI!=null) cutoffage = coaI.intValue();
    double val_cutoffpoint = cutoffValue();  // figure out the median of the individuals' fitness values
    for (int j=_individuals.size()-1; j>=0; j--) {
      GGAIndividual indj = (GGAIndividual) _individuals.elementAt(j);
      indj.incrAge();  // increase age
      int agej = indj.getAge();
      double fitj = indj.getFitness();
      double valj = indj.getValue();
      if (valj<=min_val) {
        // System.err.println("updating _master w/ inc with val="+valj);
        _master.setIncumbent(indj);  // update the global incumbent clustering
      }
      double rj = RndUtil.getInstance(_id).getRandom().nextGaussian()*varage+cutoffage;
      double fj = RndUtil.getInstance(_id).getRandom().nextDouble();
      boolean fit_cutting = false;
      // fit_cutting = fj > fitj/max_fit;
      fit_cutting = (fitj<val_cutoffpoint && fj>0.01 &&
                     (fitj<1 || j>1));  // don't kill a homogeneous population just for that
      if (rj<agej || fit_cutting) {
        // remove from the population the indj guy
        // System.err.println("removing ind id="+j+" w/ value="+indj.getValue()+
        //                   " w/ fitness="+fitj+" (cutfit="+val_cutoffpoint+" fj="+fj+") w/ age="+agej+
        //                   " (rj="+rj+"), max_val="+max_val);
        _individuals.remove(j);
      }
    }
    computeFitness();  // final update of fitness
  }


  private double computeFitness() {
    double tot_val = 0.0;
    double min_val = Double.MAX_VALUE;
    double max_val = 0.0;
    double avg_val = 0.0;
    for (int i=0; i<_individuals.size(); i++) {
      GGAIndividual indi = (GGAIndividual) _individuals.elementAt(i);
      tot_val += indi.getValue();
      if (indi.getValue()>=max_val) max_val = indi.getValue();
      if (indi.getValue()<=min_val) min_val = indi.getValue();
    }
    // now compute fitnesses of each individual
    for (int i=0; i<_individuals.size(); i++) {
      GGAIndividual indi = (GGAIndividual) _individuals.elementAt(i);
      // indi.setFitness(indi.getValue()/max_val);  // it used to be tot_val
      if (indi.getValue()==min_val) indi.setFitness(1.0);  // avoid division by zero issues
      else indi.setFitness(min_val/indi.getValue());
    }
    avg_val = tot_val/_individuals.size();
    System.err.println("computeFitness(): min_val="+min_val+" avg_val="+avg_val+" max_val="+max_val);
    return min_val;
  }


  private double cutoffValue() {
    Object[] arr = _individuals.toArray();
    Arrays.sort(arr, new IndComp());
    int vind = arr.length<5 ? 1 : arr.length/2;
    if (arr.length<=1) vind = 0;
    double res = ((GGAIndividual) arr[vind]).getFitness();
    if (vind > _maxpopnum) res = ((GGAIndividual) arr[_maxpopnum]).getFitness();
    return res;
  }


  private Pair doXover(GGAIndividual p1, GGAIndividual p2) throws ClustererException {
    Vector g1 = new Vector();
    Vector g2 = new Vector();

    /*
    // sanity check for parents
    int tot_objs = 0;
    for (int i=0; i<p2.getGroups().size(); i++) {
      tot_objs += ((Set) p2.getGroups().elementAt(i)).size();
    }
    if (tot_objs != _docs.size()) {
      throw new ClustererException("sanity test failed. to="+tot_objs+" n="+_docs.size());
    }
    tot_objs = 0;
    for (int i=0; i<p1.getGroups().size(); i++) {
      tot_objs += ((Set) p1.getGroups().elementAt(i)).size();
    }
    if (tot_objs != _docs.size()) {
      throw new ClustererException("sanity test failed.");
    }
    */
    int k1 = p1.getGroups().size();
    int k2 = p2.getGroups().size();

    int xpos11 = RndUtil.getInstance(_id).getRandom().nextInt(k1);
    int xpos12 = RndUtil.getInstance(_id).getRandom().nextInt(k1-xpos11)+xpos11;
    int xpos21 = RndUtil.getInstance(_id).getRandom().nextInt(k2);
    int xpos22 = RndUtil.getInstance(_id).getRandom().nextInt(k2-xpos21)+xpos21;

    // inject groups from [xpos11...xpos12] from p1 into p2
    Vector p1groups = p1.getGroups();
    Vector p2groups = p2.getGroups();
    Set injected = new HashSet();
    for (int i=xpos11; i<xpos12; i++) {
      injected.addAll((Set) p1groups.elementAt(i));
    }
    for (int i=0; i<xpos21; i++) {
      Set s2 = new HashSet((Set) p2groups.elementAt(i));
      s2.removeAll(injected);
      if (s2.size()>0)
        g2.add(s2);
    }
    for (int i=xpos11; i<xpos12; i++) {
      g2.add(new HashSet((Set) p1groups.elementAt(i)));
    }
    for (int i=xpos21; i<p2groups.size(); i++) {
      Set s2 = new HashSet((Set) p2groups.elementAt(i));
      s2.removeAll(injected);
      if (s2.size()>0)
        g2.add(s2);
    }
    // repair child so that it has _kI groups in it
    // repair strategy should be seriously revisited
    if (_kI!=null) {
      while (g2.size()>_kI.intValue()) {
        int min_g_sz = Integer.MAX_VALUE;
        int ming_ind = -1;
        for (int i=0; i<g2.size(); i++) {
          Set si = (Set) g2.elementAt(i);
          if (si.size()<min_g_sz) {
            min_g_sz = si.size();
            ming_ind = i;
          }
        }
        Set mins = (Set) g2.elementAt(ming_ind);
        // remove set from g2
        g2.remove(ming_ind);
        // add mins elements on another set randomly
        int newpos = RndUtil.getInstance(_id).getRandom().nextInt(g2.size());
        Set newset = (Set) g2.elementAt(newpos);
        newset.addAll(mins);
        g2.set(newpos, newset);  // ensure addition
      }
      if (_throwOnFailedRepair && g2.size()<_kI.intValue()) {
        // less than desired clusters, throw an exception
        throw new ClustererException("child2 has fewer clusters than it should, ignore fail...");
      }
    }
    /*
    // sanity check
    tot_objs = 0;
    for (int i=0; i<g2.size(); i++) {
      tot_objs += ((Set) g2.elementAt(i)).size();
    }
    if (tot_objs != _docs.size()) {
      throw new ClustererException("sanity test failed.");
    }
    */
    // same but now inject p2 stuff into p1
    injected.clear();
    for (int i=xpos21; i<xpos22; i++) {
      injected.addAll((Set) p2groups.elementAt(i));
    }
    for (int i=0; i<xpos11; i++) {
      Set s1 = new HashSet((Set) p1groups.elementAt(i));
      s1.removeAll(injected);
      if (s1.size()>0)
        g1.add(s1);
    }
    for (int i=xpos21; i<xpos22; i++) {
      g1.add(new HashSet((Set) p2groups.elementAt(i)));
    }
    for (int i=xpos11; i<p1groups.size(); i++) {
      Set s1 = new HashSet((Set) p1groups.elementAt(i));
      s1.removeAll(injected);
      if (s1.size()>0)
        g1.add(s1);
    }
    // repair child so that it has at most _kI groups in it
    // repair strategy should be seriously revisited
    if (_kI!=null) {
      while (g1.size()>_kI.intValue()) {
        int min_g_sz = Integer.MAX_VALUE;
        int ming_ind = -1;
        for (int i=0; i<g1.size(); i++) {
          Set si = (Set) g1.elementAt(i);
          if (si.size()<min_g_sz) {
            min_g_sz = si.size();
            ming_ind = i;
          }
        }
        Set mins = (Set) g1.elementAt(ming_ind);
        // remove set from g1
        g1.remove(ming_ind);
        // add mins elements on another set randomly
        int newpos = RndUtil.getInstance(_id).getRandom().nextInt(g1.size());
        Set newset = (Set) g1.elementAt(newpos);
        newset.addAll(mins);
        g1.set(newpos, newset);  // ensure addition
      }
      if (this._throwOnFailedRepair&& g1.size()<_kI.intValue()) {
        // less than desired clusters, throw an exception
        throw new ClustererException("child1 has fewer clusters than it should, ignore fail...");
      }
    }
    /*
    // sanity check
    tot_objs = 0;
    for (int i=0; i<g1.size(); i++) {
      tot_objs += ((Set) g1.elementAt(i)).size();
    }
    if (tot_objs != _docs.size()) {
      throw new ClustererException("sanity test failed.");
    }
    */

    // last action: create and return the individuals
    try {
      // Vector docs = _master.getCurrentDocs();
      GGAIndividual c1 = new GGAIndividual(g1, _master);
      GGAIndividual c2 = new GGAIndividual(g2, _master); // same as above
      Pair p = new Pair(c1, c2);
      return p;
    }
    catch (ClustererException e) {
      e.printStackTrace();
      return null;
    }
  }


  private void mutate(GGAIndividual c1, GGAIndividual c2) throws ClustererException {
    // do one mutation with prob mutprob for each child only gor groups
    double mutprob = 0.01;
    Double mutprobD = (Double) _master.getParams().get("mutprob");
    if (mutprobD!=null) mutprob = mutprobD.doubleValue();
    // do c1
    double r = RndUtil.getInstance(_id).getRandom().nextDouble();
    if (r>mutprob) {
      int k = c1.getGroups().size();
      int rpos1 = RndUtil.getInstance(_id).getRandom().nextInt(k);
      int rpos2 = RndUtil.getInstance(_id).getRandom().nextInt(k);
      Vector groups1 = c1.getGroups();
      Set v1 = (Set) groups1.elementAt(rpos1);
      Set v2 = (Set) groups1.elementAt(rpos2);
      groups1.set(rpos2, v1);
      groups1.set(rpos1, v2);
      r = RndUtil.getInstance(_id).getRandom().nextDouble();
      if (r>mutprob) {  // send an object from one group to another
        int gid1 = RndUtil.getInstance(_id).getRandom().nextInt(k);
        Set r1set = (Set) c1.getGroups().elementAt(gid1);
        if (r1set.size()>1) {  // moving this guy won't empty the cluster
          int gid2 = RndUtil.getInstance(_id).getRandom().nextInt(k);
          int oid1 = RndUtil.getInstance(_id).getRandom().nextInt(r1set.size());
          Set r2set = (Set) c1.getGroups().elementAt(gid2);
          Iterator iter = r1set.iterator();
          for (int i2 = 0; i2 < oid1; i2++) iter.next();
          Integer ri = (Integer) iter.next();
          r1set.remove(ri);
          c1.getGroups().set(gid1, r1set);
          r2set.add(ri);
          c1.getGroups().set(gid2, r2set);
          // recompute c1._value
          // Vector docs = _master.getCurrentDocs();
          c1.computeValue();
        }
      }
    }
    // do c2
    r = RndUtil.getInstance(_id).getRandom().nextDouble();
    if (r>mutprob) {
      int k = c2.getGroups().size();
      int rpos1 = RndUtil.getInstance(_id).getRandom().nextInt(k);
      int rpos2 = RndUtil.getInstance(_id).getRandom().nextInt(k);
      Vector groups2 = c2.getGroups();
      Set v1 = (Set) groups2.elementAt(rpos1);
      Set v2 = (Set) groups2.elementAt(rpos2);
      groups2.set(rpos2, v1);
      groups2.set(rpos1, v2);
      r = RndUtil.getInstance(_id).getRandom().nextDouble();
      if (r>mutprob) {  // send an object from one group to another
        int gid1 = RndUtil.getInstance(_id).getRandom().nextInt(k);
        int gid2 = RndUtil.getInstance(_id).getRandom().nextInt(k);
        Set r1set = (Set) c2.getGroups().elementAt(gid1);
        if (r1set.size()>1) {  // moving the guy won't empty the cluster
          int oid1 = RndUtil.getInstance(_id).getRandom().nextInt(r1set.size());
          Set r2set = (Set) c2.getGroups().elementAt(gid2);
          Iterator iter = r1set.iterator();
          for (int i2 = 0; i2 < oid1; i2++) iter.next();
          Integer ri = (Integer) iter.next();
          r1set.remove(ri);
          c2.getGroups().set(gid1, r1set);
          r2set.add(ri);
          c2.getGroups().set(gid2, r2set);
          // update c2._value
          // Vector docs = _master.getCurrentDocs();
          c2.computeValue();
        }
      }
    }
  }


  /**
   * send into master's intermediateClusters the top p% of the population
   */
  private void updateMasterSolutions() {
    double p = 0.1;  // default is include top 10% individuals in the intermediate clusters
    Double pD = (Double) _master.getParams().get("popperc2incl");
    if (pD!=null) p = pD.doubleValue();
    Object inds[] = _individuals.toArray();
    IndComp compfun = new IndComp();
    Arrays.sort(inds, compfun);
    for (int i=inds.length-1; i>=(1-p)*inds.length; i--) {
      try {
        _master.addIntermediateClusters( (GGAIndividual) inds[i]);
      }
      catch (Exception e) {
        e.printStackTrace();
        // no-op
      }
    }
  }


  private Vector getImmigrants() {
    Vector imms = new Vector();
    // move two top individuals
    double best_val = Double.MAX_VALUE;
    int best_ind = -1;
    for (int i=0; i<_individuals.size(); i++) {
      GGAIndividual indi = (GGAIndividual) _individuals.elementAt(i);
      double ival = indi.getValue();
      if (ival<best_val) {
        best_ind = i;
        best_val = ival;
      }
    }
    if (best_ind>=0) {
      imms.add(_individuals.elementAt(best_ind));
      _individuals.remove(best_ind);
    }
    // repeat for second guy, only if there is someone to leave behind in this island
    if (_individuals.size()>1) {
      best_val = Double.MAX_VALUE;
      best_ind = -1;
      for (int i = 0; i < _individuals.size(); i++) {
        GGAIndividual indi = (GGAIndividual) _individuals.elementAt(i);
        double ival = indi.getValue();
        if (ival < best_val) {
          best_ind = i;
          best_val = ival;
        }
      }
      imms.add(_individuals.elementAt(best_ind));
      _individuals.remove(best_ind);
    }
    return imms;
  }


  private Vector getKRandPoints(int k) {
    final int n = _docs.size();
    Vector res = new Vector();
    for (int i=0; i<k; i++) {
      int r = RndUtil.getInstance(_id).getRandom().nextInt(n);
      res.addElement(_docs.elementAt(r));
    }
    return res;
  }
}


class GGAIndividual {
  private Vector _groups;  // Vector<Set<Integer doc_id> > doc_id in [0...n-1]
  private int _age;
  private double _val;  // raw objective value
  private double _fitness;  // normalized value in [0,1]
  private GGADClusterer _master;  // ref. back to master

  public GGAIndividual(Vector groups, GGADClusterer master) throws ClustererException {
    _age = 0;
    _groups = groups;
    _master = master;
    computeValue();
    _fitness = 0.0;
  }


  public GGAIndividual(Vector groups, double val, double fit, GGADClusterer master) {
    _age = 0;
    _groups = groups;
    _val = val;
    _fitness = fit;
    _master = master;
  }


  public String toString() {
    String r = "groups=[";
    for (int i=0; i<_groups.size(); i++) {
      Set gi = (Set) _groups.elementAt(i);
      if (gi==null) {
        r += "null ";  // shouldn't happen
        continue;
      }
      r += "(";
      Iterator it = gi.iterator();
      while (it.hasNext()) {
        Integer id = (Integer) it.next();
        r += id.intValue();
        if (it.hasNext()) r+=",";
      }
      r += ") ";
    }
    r += "] val="+_val+" fitness="+_fitness+" age="+_age;
    return r;
  }
  public Vector getGroups() { return _groups; }
  public int getAge() { return _age; }
  public void incrAge() { _age++; }
  public double getValue() { return _val; }  // enhance the density value differences
  public double getFitness() { return _fitness; }
  public void setFitness(double f) { _fitness = f; }
  public void computeValue() throws ClustererException {
    // final MSTDensityEvaluator de = new MSTDensityEvaluator();
    final EnhancedEvaluator de = (EnhancedEvaluator) _master.getParams().get("ggadevaluator");
    de.setMasterDocSet(_master.getCurrentDocs());
    de.setParams(_master.getParams());
    final int n = _master.getCurrentDocs().size();
    int inds[] = new int[n];
    for (int i=0; i<_groups.size(); i++) {
      Set gi = (Set) _groups.elementAt(i);
      Iterator giiter = gi.iterator();
      while (giiter.hasNext()) {
        Integer id = (Integer) giiter.next();
        inds[id.intValue()] = i;
      }
    }
    _val = de.eval(_master.getCurrentDocs(), inds);
  }
}


class IndComp implements Comparator, java.io.Serializable {
  public int compare(Object i1, Object i2) {
    GGAIndividual ind1 = (GGAIndividual) i1;
    GGAIndividual ind2 = (GGAIndividual) i2;
    if (ind1.getValue()>ind2.getValue()) return -1;
    else if (ind1.getValue()<ind2.getValue()) return 1;
    else return 0;
  }
}
