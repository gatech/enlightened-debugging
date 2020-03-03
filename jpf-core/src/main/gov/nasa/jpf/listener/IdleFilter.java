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


package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.ArrayStoreInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.DynamicObjectArray;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.logging.Logger;


public class IdleFilter extends PropertyListenerAdapter {

  static Logger log = JPF.getLogger("gov.nasa.jpf.listener.IdleFilter");

  static class ThreadStat {
    String tname;

    int backJumps;

    boolean isCleared = false;

    int loopStartPc;

    int loopEndPc;

    int loopStackDepth;

    ThreadStat(String tname) {
      this.tname = tname;
    }
  }

  static enum Action { JUMP, PRUNE, BREAK, YIELD, WARN }

  DynamicObjectArray<ThreadStat> threadStats = new DynamicObjectArray<ThreadStat>(4,16);

  ThreadStat ts;


  boolean brokeTransition;

  int maxBackJumps;
  Action action;





  public IdleFilter(Config config) {
    maxBackJumps = config.getInt("idle.max_backjumps", 500);

    String act = config.getString("idle.action", "break");
    if ("warn".equalsIgnoreCase(act)){
      action = Action.WARN;
    } else if ("break".equalsIgnoreCase(act)){
      action = Action.BREAK;
    } else if ("yield".equalsIgnoreCase(act)){
      action = Action.YIELD;
    } else if ("prune".equalsIgnoreCase(act)){
      action = Action.PRUNE;
    } else if ("jump".equalsIgnoreCase(act)){
      action = Action.JUMP;
    } else {
      throw new JPFConfigException("unknown IdleFilter action: " +act);
    }

  }
  
  @Override
  public void stateAdvanced(Search search) {
    ts.backJumps = 0;
    ts.isCleared = false;
    ts.loopStackDepth = 0;
    ts.loopStartPc = ts.loopEndPc = 0;

    brokeTransition = false;
  }

  @Override
  public void stateBacktracked(Search search) {
    ts.backJumps = 0;
    ts.isCleared = false;
    ts.loopStackDepth = 0;
    ts.loopStartPc = ts.loopEndPc = 0;
  }


  @Override
  public void instructionExecuted(VM vm, ThreadInfo ti, Instruction nextInsn, Instruction executedInsn) {

    int tid = ti.getId();
    ts = threadStats.get(tid);
    if (ts == null) {
      ts = new ThreadStat(ti.getName());
      threadStats.set(tid, ts);
    }

    if (executedInsn.isBackJump()) {
      ts.backJumps++;

      int loopStackDepth = ti.getStackDepth();
      int loopPc = nextInsn.getPosition();

      if ((loopStackDepth != ts.loopStackDepth) || (loopPc != ts.loopStartPc)) {

        ts.isCleared = false;
        ts.loopStackDepth = loopStackDepth;
        ts.loopStartPc = loopPc;
        ts.loopEndPc = executedInsn.getPosition();
        ts.backJumps = 0;
        
      } else {
        if (!ts.isCleared) {
          if (ts.backJumps > maxBackJumps) {

            ti.reschedule("idleFilter"); 
            MethodInfo mi = executedInsn.getMethodInfo();
            ClassInfo ci = mi.getClassInfo();
            int line = mi.getLineNumber(executedInsn);
            String file = ci.getSourceFileName();

            switch (action) {
              case JUMP:


                Instruction next = executedInsn.getNext();
                ti.setNextPC(next);

                log.warning("jumped past loop in: " + ti.getName() +
                        "\n\tat " + ci.getName() + "." + mi.getName() + "(" + file + ":" + line + ")");
                break;

              case PRUNE:

                vm.ignoreState();
                log.warning("pruned thread: " + ti.getName() +
                        "\n\tat " + ci.getName() + "." + mi.getName() + "(" + file + ":" + line + ")");
                break;

              case BREAK:

                brokeTransition = true;
                ti.breakTransition("breakIdleLoop");

                log.warning("breaks transition on suspicious loop in thread: " + ti.getName() +
                        "\n\tat " + ci.getName() + "." + mi.getName() + "(" + file + ":" + line + ")");

                break;

              case YIELD:

                brokeTransition = true;
                ti.reschedule("rescheduleIdleLoop");

                log.warning("yield on suspicious loop in thread: " + ti.getName() +
                        "\n\tat " + ci.getName() + "." + mi.getName() + "(" + file + ":" + line + ")");

                break;
                
              case WARN:
                log.warning("detected suspicious loop in thread: " + ti.getName() +
                        "\n\tat " + ci.getName() + "." + mi.getName() + "(" + file + ":" + line + ")");
                break;

            }
          }
        }
      }

    } else if (!ts.isCleared) {



      
      if ((executedInsn instanceof JVMInvokeInstruction)
          || (executedInsn instanceof ArrayStoreInstruction)) {
        int stackDepth = ti.getStackDepth();
        int pc = executedInsn.getPosition();

        if (stackDepth == ts.loopStackDepth) {
          if ((pc >= ts.loopStartPc) && (pc < ts.loopEndPc)) {
            ts.isCleared = true;
          }
        }
      }
    }
  }
  

  @Override
  public void threadTerminated (VM vm, ThreadInfo ti){
    int tid = ti.getId();
    threadStats.set(tid, null);
  }


}
