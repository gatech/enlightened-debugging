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
import anonymous.domain.enlighten.deptrack.ObjectProperty;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;



public class CHECKCAST extends DependencyTrackingInstruction implements JVMInstruction {
  String type;

  public CHECKCAST() {} 

  public CHECKCAST(String typeName){
    type = Types.getClassNameFromTypeName(typeName);
  }

  public String getTypeName() {
    return type;
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getTopFrame();
    
    Object objrefAttr = frame.getOperandAttr();
    int objref = frame.peek();

    if (objref == MJIEnv.NULL) {


    } else {
      boolean isValid = false;

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

      ElementInfo e = ti.getElementInfo(objref);
      ClassInfo eci = e.getClassInfo();

      if (type.charAt(0) == '['){  
        if (eci.isArray()) {

          ClassInfo cci = eci.getComponentClassInfo();
          isValid = cci.isInstanceOf(type.substring(1));
        }

      } else { 
        isValid = e.getClassInfo().isInstanceOf(type);
      }

      if (!isValid) {
      	DynamicDependency objTypeDep = null;
      	ObjectProperty objPropAttr = (ObjectProperty) e.getObjectAttr();
      	if (objPropAttr != null) {
      		objTypeDep = objPropAttr.getTypeDependency();
      	}
      	DynamicDependency exDep = DynDepBuilder.newBuilder()
      			.appendDataDependency(objrefAttr, objTypeDep, getInstructionDepSource(ti))
      			.setControlDependency(getControlDependencyCondition(ti)).build();
        return ti.createAndThrowException("java.lang.ClassCastException",
                e.getClassInfo().getName() + " cannot be cast to " + type, exDep);
      }
    }

    return getNext(ti);
  }


  @Override
  public int getLength() {
    return 3; 
  }
  
  @Override
  public int getByteCode () {
    return 0xC0;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
