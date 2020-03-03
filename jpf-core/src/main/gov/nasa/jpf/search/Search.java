/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */


package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ConfigChangeListener;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFListenerException;
import gov.nasa.jpf.Property;
import gov.nasa.jpf.State;
import gov.nasa.jpf.report.Reporter;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.ThreadList;
import gov.nasa.jpf.vm.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class Search {
  
  protected static JPFLogger log = JPF.getLogger("gov.nasa.jpf.search");
  
  
  protected Error currentError = null;
  protected ArrayList<Error> errors = new ArrayList<Error>();

  protected int       depth = 0;
  protected VM       vm;

  protected ArrayList<Property> properties;

  protected boolean matchDepth;
  protected long    minFreeMemory;
  protected int     depthLimit;
  protected boolean getAllErrors;


  protected String lastSearchConstraint;


  protected boolean done = false;
  protected boolean doBacktrack = false;


  protected AtomicBoolean notifyProbeListeners = new AtomicBoolean(false);

  
  protected SearchListener[] listeners = new SearchListener[0];

  
  protected Reporter reporter;

  protected final Config config; 



  class ConfigListener implements ConfigChangeListener {

    @Override
    public void propertyChanged(Config config, String key, String oldValue, String newValue) {

      if (!config.equals(Search.this.config)) {
        return;
      }


      if (key.startsWith("search.")){
        String k = key.substring(7);
        if ("match_depth".equals(k) ||
            "min_free".equals(k) ||
            "multiple_errors".equals(k)){
          initialize(config);
        }
      }
    }
    
    @Override
    public void jpfRunTerminated (Config config){
      config.removeChangeListener(this);
    }
  }
  
  
  protected final IntVector stateDepth = new IntVector();

  protected Search (Config config, VM vm) {
    this.vm = vm;
    this.config = config;

    initialize( config);

    properties = getProperties(config);
    if (properties.isEmpty()) {
      log.severe("no property");
    }
    
    config.addChangeListener( new ConfigListener());
  }

  protected void initialize( Config conf){
    depthLimit = conf.getInt("search.depth_limit", Integer.MAX_VALUE);
    matchDepth = conf.getBoolean("search.match_depth");
    minFreeMemory = conf.getMemorySize("search.min_free", 1024<<10);    
    getAllErrors = conf.getBoolean("search.multiple_errors");
  }
  
  
  public void cleanUp(){

  }
  
  public Config getConfig() {
    return config;
  }
  
  public abstract void search ();

  public void setReporter(Reporter reporter){
    this.reporter = reporter;
  }

  public void addListener (SearchListener newListener) {
    log.info("SearchListener added: ", newListener);
    listeners = Misc.appendElement(listeners, newListener);
  }

  public boolean hasListenerOfType (Class<?> listenerCls) {
    return Misc.hasElementOfType(listeners, listenerCls);
  }
  
  public <T> T getNextListenerOfType(Class<T> type, T prev){
    return Misc.getNextElementOfType(listeners, type, prev);
  }


  public void removeListener (SearchListener removeListener) {
    listeners = Misc.removeElement(listeners, removeListener);
  }


  public void addProperty (Property newProperty) {
    properties.add(newProperty);
  }

  public void removeProperty (Property oldProperty) {
     properties.remove(oldProperty);
  }

  
  protected ArrayList<Property> getProperties (Config config) {
    Class<?>[] argTypes = { Config.class, Search.class };
    Object[] args = { config, this };

    ArrayList<Property> list = config.getInstances("search.properties", Property.class,
                                                   argTypes, args);

    return list;
  }


  protected boolean hasPropertyTermination () {
    if (currentError != null){
      if (done){
        return true;
      } else { 
        doBacktrack = true;
      }
    }

    return false;
  }


  protected boolean checkPropertyViolation () {
    for (Property p : properties) {
      if (!p.check(this, vm)) {
        error(p, vm.getClonedPath(), vm.getThreadList());
        return true;
      }
    }

    return false;
  }

  public List<Error> getErrors () {
    return errors;
  }

  public int getNumberOfErrors(){
    return errors.size();
  }

  public String getLastSearchConstraint() {
    return lastSearchConstraint;
  }

  
  public void probeSearch(){
    notifyProbeListeners.set(true);
  }
  
  
  public void checkAndResetProbeRequest(){
    if (notifyProbeListeners.compareAndSet(true, false)){
      notifySearchProbed();
    }
  }
  
  
  public Error getCurrentError(){
    return currentError;
  }

  public Error getLastError() {
    int i=errors.size()-1;
    if (i >=0) {
      return errors.get(i);
    } else {
      return null;
    }
  }

  public boolean hasErrors(){
    return !errors.isEmpty();
  }

  public VM getVM() {
    return vm;
  }

  public boolean isEndState () {
    return vm.isEndState();
  }

  public boolean isErrorState(){
    return (currentError != null);
  }

  public boolean hasNextState () {
    return !isEndState();
  }

  public boolean transitionOccurred(){
    return vm.transitionOccurred();
  }

  public boolean isNewState () {
    boolean isNew = vm.isNewState();

    if (matchDepth) {
      int id = vm.getStateId();

      if (isNew) {
        setStateDepth(id, depth);
      } else {
        return depth < getStateDepth(id);
      }
    }

    return isNew;
  }

  public boolean isVisitedState () {
    return !isNewState();
  }

  public boolean isIgnoredState(){
    return vm.isIgnoredState();
  }

  public boolean isProcessedState(){
    return vm.getChoiceGenerator().isProcessed();
  }

  public boolean isDone(){
    return done;
  }

  public int getDepth () {
    return depth;
  }

  public String getSearchConstraint () {
    return lastSearchConstraint;
  }

  public Transition getTransition () {
    return vm.getLastTransition();
  }

  public int getStateId () {
    return vm.getStateId();
  }

  public int getPurgedStateId () {
    return -1; 
  }


  
  public boolean requestBacktrack () {
    return doBacktrack = true;
  }


  protected boolean checkAndResetBacktrackRequest() {
    if (doBacktrack){
      doBacktrack = false;
      return true;
    } else {
      return false;
    }
  }

  public boolean supportsBacktrack () {
    return true;
  }

  public boolean supportsRestoreState () {

    return false;
  }

  public int getDepthLimit () {
    return depthLimit;
  }
  
  public void setDepthLimit(int limit){
    depthLimit = limit;
  }

  protected SearchState getSearchState () {
    return new SearchState(this);
  }


  public void error (Property property) {
    error(property, null, null);
  }

  public void error (Property property, Path path, ThreadList threadList) {

    if (getAllErrors) {

      try {
        property = property.clone();
        path = path.clone();
        threadList = (ThreadList) threadList.clone(); 
      } catch (CloneNotSupportedException cnsx){
        throw new JPFException("failed to clone error information: " + cnsx);
      }
      done = false;
      
    } else {
      done = true;
    }

    currentError = new Error(errors.size()+1, property, path, threadList);

    errors.add(currentError);




  }

  public void resetProperties(){
    for (Property p : properties) {
      p.reset();
    }
  }

  protected void notifyStateAdvanced () {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].stateAdvanced(this);
      }
      if (reporter != null){

        reporter.stateAdvanced(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during stateAdvanced() notification", t);
    }
  }

  protected void notifyStateProcessed() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].stateProcessed(this);
      }
      if (reporter != null){
        reporter.stateProcessed(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during stateProcessed() notification", t);
    }
  }

  protected void notifyStateStored() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].stateStored(this);
      }
      if (reporter != null){
        reporter.stateStored(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during stateStored() notification", t);
    }
  }

  protected void notifyStateRestored() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].stateRestored(this);
      }
      if (reporter != null){
        reporter.stateRestored(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during stateRestored() notification", t);
    }
  }

  protected void notifyStateBacktracked() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].stateBacktracked(this);
      }
      if (reporter != null){
        reporter.stateBacktracked(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during stateBacktracked() notification", t);
    }
  }

  protected void notifyStatePurged() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].statePurged(this);
      }
      if (reporter != null){
        reporter.statePurged(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during statePurged() notification", t);
    }
  }

  public void notifySearchProbed() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].searchProbed(this);
      }
      if (reporter != null){
        reporter.searchProbed(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during searchProbed() notification", t);
    }
  }

  
  protected void notifyPropertyViolated() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].propertyViolated(this);
      }
      if (reporter != null){
        reporter.propertyViolated(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during propertyViolated() notification", t);
    }


    if (getAllErrors){
      resetProperties();
    }
  }

  protected void notifySearchStarted() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].searchStarted(this);
      }
      if (reporter != null){
        reporter.searchStarted(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during searchStarted() notification", t);
    }
  }

  public void notifySearchConstraintHit(String details) {
    try {
      lastSearchConstraint = details;
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].searchConstraintHit(this);
      }
      if (reporter != null){
        reporter.searchConstraintHit(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during searchConstraintHit() notification", t);
    }
  }

  protected void notifySearchFinished() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].searchFinished(this);
      }
      if (reporter != null){
        reporter.searchFinished(this);
      }
    } catch (Throwable t) {
      throw new JPFListenerException("exception during searchFinished() notification", t);
    }
  }

  protected boolean forward () {
    currentError = null;

    boolean ret = vm.forward();

    checkPropertyViolation();
    return ret;
  }

  protected boolean backtrack () {
    return vm.backtrack();
  }

  public void setIgnoredState (boolean cond) {
    vm.ignoreState(cond);
  }

  protected void restoreState (State state) {

  }

  
  public void terminate () {
    done = true;
  }

  protected void setStateDepth (int stateId, int depth) {
    stateDepth.set(stateId, depth + 1);
  }

  public int getStateDepth (int stateId) {
    int depthPlusOne = stateDepth.get(stateId);
    if (depthPlusOne <= 0) {
      throw new JPFException("Asked for depth of unvisited state");
    } else {
      return depthPlusOne - 1;
    }
  }

  
  public boolean checkStateSpaceLimit () {
    Runtime rt = Runtime.getRuntime();

    long avail = rt.freeMemory();




    if (avail < minFreeMemory) {

      rt.gc();
      avail = rt.freeMemory();

      if (avail < minFreeMemory) {

        return false;
      }
    }

    return true;
  }
}

