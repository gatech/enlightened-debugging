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


package gov.nasa.jpf.vm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;


public class ConstInsnPathTime extends ListenerAdapter implements TimeModel {



  static class TimeVal {
    final long time;
    TimeVal (long t){
      time = t;
    }
  }
  
  VM vm;
  int perInsnTime; 
  
  long transitionStartTime;
  
  public ConstInsnPathTime (VM vm, Config conf){
    perInsnTime = conf.getInt("vm.time.per_insn", 1);
    
    vm.addListener(this);
    this.vm = vm;
  }

  protected long computeTime (){
    ThreadInfo tiCurrent = vm.getCurrentThread();
    long t = tiCurrent.getExecutedInstructions() * perInsnTime;
    
    return transitionStartTime + t;
  }
  

  @Override
  public void choiceGeneratorRegistered(VM vm, ChoiceGenerator<?> nextCG, ThreadInfo ti, Instruction executedInsn){
    ChoiceGenerator<?> cgPrev = nextCG.getPreviousChoiceGenerator();
    ThreadInfo tiCurrent = vm.getCurrentThread();
    long t = tiCurrent.getExecutedInstructions() * perInsnTime;
    
    TimeVal tv = null;
    
    if (nextCG.isCascaded()){

      tv = cgPrev.getAttr(TimeVal.class);
      
    } else {
      if (cgPrev != null){
        TimeVal tvPrev = cgPrev.getAttr(TimeVal.class); 
        tv = new TimeVal(tvPrev.time + t);
             
      } else {
        tv = new TimeVal( t);
      }
    }
    
    nextCG.addAttr( tv);
  }
  
  @Override
  public void choiceGeneratorAdvanced(VM vm, ChoiceGenerator<?> currentCG){
    TimeVal tv = currentCG.getAttr(TimeVal.class);
    if (tv != null){
      transitionStartTime = tv.time;
    } else {
      transitionStartTime = 0;
    }
  }
  

  @Override
  public long currentTimeMillis() {
    return computeTime();
  }

  @Override
  public long nanoTime() {
    return computeTime();
  }
  
}
