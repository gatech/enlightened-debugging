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
import anonymous.domain.enlighten.deptrack.ObjectProperty;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;



public class INSTANCEOF extends DependencyTrackingInstruction implements JVMInstruction {
  private String type;


  
  public INSTANCEOF (String typeName){
    type = Types.getTypeSignature(typeName, false);
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    if(Types.isReferenceSignature(type)) {
      String t;
      if(Types.isArray(type)) {

        t = Types.getComponentTerminal(type);
      } else {
        t = type;
      }


      try {
        ti.resolveReferencedClass(t);
      } catch(LoadOnJPFRequired lre) {
        return ti.getPC();
      }
    }

    StackFrame frame = ti.getModifiableTopFrame();
    Object objRefAttr = frame.getOperandAttr();
    int objref = frame.pop();

    if (objref == MJIEnv.NULL) {
      frame.push(0);
    } else if (ti.getElementInfo(objref).instanceOf(type)) {
      frame.push(1);
    } else {
      frame.push(0);
    }
    
    DynamicDependency typeDep = null;
    if (objref != MJIEnv.NULL) {
    	ObjectProperty objPropDep = 
    			(ObjectProperty) ti.getElementInfo(objref).getObjectAttr();
    	if (objPropDep != null) {
    		typeDep = objPropDep.getTypeDependency();
    	}
    }
    DynamicDependency resultDependences = DynDepBuilder.newBuilder()
    		.appendDataDependency(objRefAttr, typeDep, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    if (resultDependences != null) {
    	frame.setOperandAttr(resultDependences);
    }

    return getNext(ti);
  }
  
  public String getType() {
	  return type;
  }

  @Override
  public int getLength() {
    return 3; 
  }
  
  @Override
  public int getByteCode () {
    return 0xC1;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
