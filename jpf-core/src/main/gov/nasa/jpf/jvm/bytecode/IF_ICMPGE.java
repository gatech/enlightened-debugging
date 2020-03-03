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
import gov.nasa.jpf.vm.StackFrame;



public class IF_ICMPGE extends IfInstruction {

  public IF_ICMPGE(int targetPc) {
    super(targetPc);
  }


  @Override
  public boolean popConditionValue (StackFrame frame) {
    int v1 = frame.pop();
    int v2 = frame.pop();

    return (v1 <= v2);
  }
  
  @Override
  public DynamicDependency getConditionValueDependency(StackFrame frame) {
  	return DynDepBuilder.newBuilder().appendDataDependency(
  			frame.getOperandAttr(), frame.getOperandAttr(1)).build();
  }

  @Override
  public int getByteCode () {
    return 0xA2;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
