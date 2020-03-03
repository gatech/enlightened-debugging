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


package gov.nasa.jpf.search.heuristic;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.VM;

import java.util.ArrayList;
import java.util.List;



public abstract class HeuristicSearch extends Search {
  
  static final String DEFAULT_HEURISTIC_PACKAGE = "gov.nasa.jpf.search.heuristic.";
  
  protected HeuristicState parentState;
  protected List<HeuristicState> childStates;
  
  protected boolean isPathSensitive = false;  
  
  
  protected boolean useAstar;
  
  
  protected boolean isBeamSearch;

  
  public HeuristicSearch (Config config, VM vm) {
    super(config, vm);
    
    useAstar = config.getBoolean("search.heuristic.astar");
    isBeamSearch = config.getBoolean("search.heuristic.beam_search");
  }

  

  protected abstract HeuristicState queueCurrentState ();
  


  protected abstract HeuristicState getNextQueuedState ();

  public abstract int getQueueSize();
  public abstract boolean isQueueLimitReached();
  
  public HeuristicState getParentState() {
    return parentState;
  }
  
  public List<HeuristicState> getChildStates() {
    return childStates;
  }
  
  public void setPathSensitive (boolean isPathSensitive) {
    this.isPathSensitive = isPathSensitive;
  }  
  
  void backtrackToParent () {
    backtrack();

    depth--;
    notifyStateBacktracked();    
  }
  
  
  protected boolean generateChildren () {

    childStates = new ArrayList<HeuristicState>();
    
    while (!done) {
      
      if (!forward()) {
        notifyStateProcessed();
        return true;
      }

      depth++;
      notifyStateAdvanced();

      if (currentError != null){
        notifyPropertyViolated();
        if (hasPropertyTermination()) {
          return false;
        }
        



        
      } else {
      
        if (!isEndState() && !isIgnoredState()) {
          boolean isNewState = isNewState();

          if (isNewState && depth >= depthLimit) {



            notifySearchConstraintHit("depth limit reached: " + depthLimit);

          } else if (isNewState || isPathSensitive) {

            if (isQueueLimitReached()) {
              notifySearchConstraintHit("queue limit reached: " + getQueueSize());
            }
          
            HeuristicState newHState = queueCurrentState();            
            if (newHState != null) { 
              childStates.add(newHState);
              notifyStateStored();
            }
          }
        
        } else {

        }
      }
      
      backtrackToParent();
    }
    
    return false;
  }

  
  private void restoreState (HeuristicState hState) {    
    vm.restoreState(hState.getVMState());



    depth = vm.getPathLength();
    notifyStateRestored();
  }
   
  @Override
  public void search () {
        
    queueCurrentState();
    notifyStateStored();
    


    parentState = getNextQueuedState();
    
    done = false;
    notifySearchStarted();
    
    if (!hasPropertyTermination()) {
      generateChildren();

      while (!done && (parentState = getNextQueuedState()) != null) {
        restoreState(parentState);
        
        generateChildren();
      }
    }
    
    notifySearchFinished();
  }

  @Override
  public boolean supportsBacktrack () {


    return false;
  }
}


