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

import gov.nasa.jpf.vm.Instruction;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.BooleanChoiceGenerator;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.KernelState;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;


public abstract class IfInstruction extends DependencyTrackingInstruction implements JVMInstruction {
  protected int targetPosition;  
  protected Instruction target;  
  
  protected boolean conditionValue;  

  protected IfInstruction(int targetPosition){
    this.targetPosition = targetPosition;
  }

  
  public boolean getConditionValue() {
    return conditionValue;
  }
    
  
  @Override
  public boolean isBackJump () { 
    return (conditionValue) && (targetPosition <= position);
  }
    
  
  public abstract boolean popConditionValue(StackFrame frame);
  
  public abstract DynamicDependency getConditionValueDependency(StackFrame frame);
  
  public Instruction getTarget() {
    if (target == null) {
      target = mi.getInstructionAt(targetPosition);
    }
    return target;
  }
  
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();

    DynamicDependency conditionDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(getConditionValueDependency(frame))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    conditionValue = popConditionValue(frame);
    setActiveCondition(ti, conditionDep);
    if (conditionValue) {
      return getTarget();
    } else {
      return getNext(ti);
    }
  }

  
  protected Instruction executeBothBranches (SystemState ss, KernelState ks, ThreadInfo ti){
    if (!ti.isFirstStepInsn()) {
      BooleanChoiceGenerator cg = new BooleanChoiceGenerator(ti.getVM().getConfig(), "ifAll");
      if (ss.setNextChoiceGenerator(cg)){
        return this;

      } else {
        StackFrame frame = ti.getModifiableTopFrame();

        conditionValue = popConditionValue(frame);
        if (conditionValue) {
          return getTarget();
        } else {
          return getNext(ti);
        }
      }
      
    } else {
      BooleanChoiceGenerator cg = ss.getCurrentChoiceGenerator("ifAll", BooleanChoiceGenerator.class);
      assert (cg != null) : "no BooleanChoiceGenerator";
      
      StackFrame frame = ti.getModifiableTopFrame();
      popConditionValue(frame); 
      
      conditionValue = cg.getNextChoice();
      
      if (conditionValue) {
        return getTarget();
      } else {
        return getNext(ti);
      }

    }
  }
  
  @Override
  public String toString () {
    return getMnemonic() + " " + targetPosition;
  }
  
  @Override
  public int getLength() {
    return 3; 
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  @Override
  public Instruction typeSafeClone(MethodInfo mi) {
    IfInstruction clone = null;

    try {
      clone = (IfInstruction) super.clone();


      clone.mi = mi;

      clone.target = null;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return clone;
  }
}
