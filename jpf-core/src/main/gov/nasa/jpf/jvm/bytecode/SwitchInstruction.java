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



package gov.nasa.jpf.jvm.bytecode;

import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.KernelState;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.choice.IntIntervalGenerator;


public abstract class SwitchInstruction extends DependencyTrackingInstruction implements JVMInstruction {

  public static final int DEFAULT = -1; 
  
  protected int   target;   
  protected int[] targets;  
  protected int[] matches;  

  protected int lastIdx;

  protected SwitchInstruction (int defaultTarget, int numberOfTargets){
    target = defaultTarget;
    targets = new int[numberOfTargets];
    matches = new int[numberOfTargets];
  }

  public int getNumberOfEntries() {
    return targets.length;
  }

  protected Instruction executeConditional (ThreadInfo ti){
    StackFrame frame = ti.getModifiableTopFrame();

    int value = frame.pop();

    lastIdx = DEFAULT;

    for (int i = 0, l = matches.length; i < l; i++) {
      if (value == matches[i]) {
        lastIdx = i;
        return mi.getInstructionAt(targets[i]);
      }
    }

    return mi.getInstructionAt(target);
  }
  
  @Override
  public Instruction execute (ThreadInfo ti) {
  	Object jumpKeyAttr = ti.getTopFrame().getOperandAttr();
  	DynamicDependency activeCondition = DynDepBuilder.newBuilder()
  			.appendDataDependency(jumpKeyAttr, getInstructionDepSource(ti))
  			.setControlDependency(getControlDependencyCondition(ti)).build();
  	setActiveCondition(ti, activeCondition);


    return executeConditional(ti);
  }

  
  public Instruction executeAllBranches (SystemState ss, KernelState ks, ThreadInfo ti) {
    if (!ti.isFirstStepInsn()) {
      IntIntervalGenerator cg = new IntIntervalGenerator("switchAll", 0,matches.length);
      if (ss.setNextChoiceGenerator(cg)){
        return this;

      } else {

        return executeConditional(ti);
      }
      
    } else {
      IntIntervalGenerator cg = ss.getCurrentChoiceGenerator("switchAll", IntIntervalGenerator.class);
      assert (cg != null) : "no IntIntervalGenerator";
      
      StackFrame frame = ti.getModifiableTopFrame();
      int idx = frame.pop(); 
      idx = cg.getNextChoice();
      
      if (idx == matches.length){ 
        lastIdx = DEFAULT;
        return mi.getInstructionAt(target);
      } else {
        lastIdx = idx;
        return mi.getInstructionAt(targets[idx]);
      }
    }
  }


  
  public int getLastTargetIndex () {
    return lastIdx;
  }
  
  public int getNumberOfTargets () {
    return matches.length;
  }
  
  public int getMatchConst (int idx){
    return matches[idx];
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  public int getTarget() {
	return target;
  }

  public int[] getTargets() {
	return targets;
  }

  public int[] getMatches() {
	return matches;
  }
}
