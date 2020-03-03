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

import gov.nasa.jpf.vm.bytecode.StoreInstruction;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;



public class ISTORE extends JVMLocalVariableInstruction implements StoreInstruction {

  public ISTORE(int localVarIndex){
    super(localVarIndex);
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    
    Object attr = frame.getOperandAttr();
    frame.storeOperand(index);
    
    DynamicDependency resultDependences = DynDepBuilder.newBuilder()
    		.appendDataDependency(attr, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    if (resultDependences != null) {
    	frame.setLocalAttr(index, resultDependences);
    }
    
    return getNext(ti);

  }

  @Override
  public int getLength() {
    if (index > 3){
      return 2; 
    } else {
      return 1;
    }
  }
  
  @Override
  public int getByteCode () {
    switch (index) {
    case 0: return 0x3b;
    case 1: return 0x3c;
    case 2: return 0x3d;
    case 3: return 0x3e;
    }
    return 0x36; 
  }
  
  @Override
  public String getBaseMnemonic() {
    return "istore";
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
