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
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.ReadInstruction;



public class GETSTATIC extends JVMStaticFieldInstruction  implements ReadInstruction {

  public GETSTATIC(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    FieldInfo fieldInfo;


    try {
      fieldInfo = getFieldInfo();
    } catch (LoadOnJPFRequired lre) {
      return ti.getPC();
    }
    
    if (fieldInfo == null) {
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException("java.lang.NoSuchFieldError",
              (className + '.' + fname), exDep);
    }


    ClassInfo ciField = fieldInfo.getClassInfo();
    if (!mi.isClinit(ciField) && ciField.initializeClass(ti)) {

      return ti.getPC();
    }
    ElementInfo eiFieldOwner = ciField.getStaticElementInfo();
        
    Object fieldAttr = eiFieldOwner.getFieldAttr(fieldInfo);
    StackFrame frame = ti.getModifiableTopFrame();
    
    DynamicDependency resultDependences = DynDepBuilder.newBuilder()
    		.appendDataDependency(fieldAttr, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();

    if (size == 1) {
      int ival = eiFieldOwner.get1SlotField(fieldInfo);
      lastValue = ival;

      if (fieldInfo.isReference()) {
        frame.pushRef(ival);
      } else {
        frame.push(ival);
      }
      
      if (resultDependences != null) {
        frame.setOperandAttr(resultDependences);
      }

    } else {
      long lval = eiFieldOwner.get2SlotField(fieldInfo);
      lastValue = lval;
      
      frame.pushLong(lval);
      
      if (resultDependences != null) {
        frame.setLongOperandAttr(resultDependences);
      }
    }
        
    return getNext(ti);
  }
  
  @Override
  public boolean isMonitorEnterPrologue(){
    return GetHelper.isMonitorEnterPrologue(mi, insnIndex);
  }
  
  @Override
  public int getLength() {
    return 3; 
  }
  
  @Override
  public int getByteCode () {
    return 0xB2;
  }

  @Override
  public boolean isRead() {
    return true;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
