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
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.RestorableVMState;



public class RandomSearch extends Search {
  int path_limit = 0;
  
  public RandomSearch (Config config, VM vm) {
    super(config, vm);
    
    path_limit = config.getInt("search.RandomSearch.path_limit", 0);
  }
  
  @Override
  public void search () {
    int    depth = 0;
    int paths = 0;
    depth++;
    
    if (hasPropertyTermination()) {
      return;
    }
    

    RestorableVMState init_state = vm.getRestorableState();
    
    notifySearchStarted();
    while (!done) {
      if ((depth < depthLimit) && forward()) {
        notifyStateAdvanced();

        if (currentError != null){
          notifyPropertyViolated();

          if (hasPropertyTermination()) {
            return;
          }
        }

        if (isEndState()){
          return;
        }

        depth++;

      } else { 


        if (depth >= depthLimit) {
          notifySearchConstraintHit("depth limit reached: " + depthLimit);
        }
        checkPropertyViolation();
        done = (paths >= path_limit);
        paths++;
        System.out.println("paths = " + paths);
        depth = 1;
        vm.restoreState(init_state);
        vm.resetNextCG();
      }
    }
    notifySearchFinished();
  }
}
