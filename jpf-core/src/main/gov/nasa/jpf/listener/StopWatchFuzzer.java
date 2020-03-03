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

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.LCMP;
import gov.nasa.jpf.jvm.bytecode.LSUB;
import gov.nasa.jpf.jvm.bytecode.NATIVERETURN;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.choice.IntChoiceFromSet;



public class StopWatchFuzzer extends ListenerAdapter {
  
  MethodInfo miCurrentTimeMillis;
  
  static class TimeVal {

  }
  static TimeVal timeValAttr = new TimeVal(); 
  
  static String CG_ID = "LCMP_fuzzer";

  @Override
  public void classLoaded(VM vm, ClassInfo ci){
    if (miCurrentTimeMillis == null){
      if (ci.getName().equals("java.lang.System")) {
        miCurrentTimeMillis = ci.getMethod("currentTimeMillis()J", false); 
      }
    }
  }
  
  @Override
  public void instructionExecuted(VM vm, ThreadInfo ti, Instruction nextInsn, Instruction executedInsn){

    if (executedInsn instanceof NATIVERETURN){
      if (executedInsn.isCompleted(ti)){
        if (((NATIVERETURN)executedInsn).getMethodInfo() == miCurrentTimeMillis){

          StackFrame frame = ti.getModifiableTopFrame();
          frame.addLongOperandAttr( timeValAttr);
        }
      }
    }
  }
  
  @Override
  public void executeInstruction(VM vm, ThreadInfo ti, Instruction insnToExecute){

    if (insnToExecute instanceof LSUB){  
      StackFrame frame = ti.getTopFrame();


      if (frame.hasOperandAttr(1, TimeVal.class) || frame.hasOperandAttr(3, TimeVal.class)){      

        ti.skipInstruction(insnToExecute.execute(ti));
      

        frame = ti.getModifiableTopFrame();
        frame.addLongOperandAttr(timeValAttr);
      }
       
    } else if (insnToExecute instanceof LCMP){ 
      
      if (!ti.isFirstStepInsn()){ 
        StackFrame frame = ti.getTopFrame();
        
        if (frame.hasOperandAttr(1, TimeVal.class) || frame.hasOperandAttr(3, TimeVal.class)){
          IntChoiceFromSet cg = new IntChoiceFromSet( CG_ID, -1, 0, 1);
          if (vm.setNextChoiceGenerator(cg)){
            ti.skipInstruction(insnToExecute); 
          }
        }
        
      } else { 
        IntChoiceFromSet cg = vm.getCurrentChoiceGenerator(CG_ID, IntChoiceFromSet.class);
        if (cg != null){
          int choice = cg.getNextChoice();
          StackFrame frame = ti.getModifiableTopFrame();
          

          frame.popLong();
          frame.popLong();
          
          frame.push(choice);
          
          ti.skipInstruction(insnToExecute.getNext());
        }
      }
    }
  }
}
