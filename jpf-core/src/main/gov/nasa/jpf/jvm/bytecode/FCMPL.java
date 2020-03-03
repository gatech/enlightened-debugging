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
import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;



public class FCMPL extends DependencyTrackingInstruction implements JVMInstruction {

  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();

    Object v1Attr = frame.getOperandAttr();
    float v1 = frame.popFloat();
    Object v2Attr = frame.getOperandAttr();
    float v2 = frame.popFloat();
    
    int condVal = conditionValue(v1, v2);
    
    frame.push(condVal);
    
    DynamicDependency resultDependences = DynDepBuilder.newBuilder()
    		.appendDataDependency(v1Attr, v2Attr, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    if (resultDependences != null) {
    	frame.setOperandAttr(resultDependences);
    }

    return getNext(ti);
  }
  
  protected int conditionValue(float v1, float v2) {
    if (Float.isNaN(v1) || Float.isNaN(v2)) {
      return -1;
    } else if (v1 == v2) {
        return 0;
    } else if (v2 > v1) {
        return 1;
    } else {
        return -1;
    }      
  }

  @Override
  public int getByteCode () {
    return 0x95;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
