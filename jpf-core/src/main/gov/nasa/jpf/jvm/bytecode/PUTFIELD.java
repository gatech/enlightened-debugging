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
import anonymous.domain.enlighten.deptrack.ObjectProperty;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.WriteInstruction;


public class PUTFIELD extends JVMInstanceFieldInstruction implements WriteInstruction {

  public PUTFIELD(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }  

  @Override
  public int getObjectSlot (StackFrame frame){
    return frame.getTopPos() - size;
  }

  
  @Override
  public int getValueSlot (StackFrame frame){
    return frame.getTopPos();
  }

  
  
  @Override
  public ElementInfo getElementInfo(ThreadInfo ti){
    if (isCompleted(ti)){
      return ti.getElementInfo(lastThis);
    } else {
      return peekElementInfo(ti); 
    }
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    Object objRefAttr = frame.getOperandAttr(size);
    int objRef = frame.peek( size);
    lastThis = objRef;
    
    if (objRef == MJIEnv.NULL) {
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(objRefAttr, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException(
      		"java.lang.NullPointerException", 
      		"referencing field '" + fname + "' on null object", exDep);
    }

    ElementInfo eiFieldOwner = ti.getModifiableElementInfo(objRef);
    FieldInfo fieldInfo = getFieldInfo();
    if (fieldInfo == null) {
    	DynamicDependency objTypeDep = null;
    	ObjectProperty objPropAttr = 
    			(ObjectProperty) eiFieldOwner.getObjectAttr();
    	if (objPropAttr != null) {
    		objTypeDep = objPropAttr.getTypeDependency();
    	}
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(objRefAttr, objTypeDep, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException(
      		"java.lang.NoSuchFieldError", "no field " + fname + " in " + eiFieldOwner, exDep);
    }
    
    Object valueAttr = null;
    if (fieldInfo.getStorageSize() == 1) {
    	valueAttr = frame.getOperandAttr();
    	int val = frame.peek();
    	eiFieldOwner.set1SlotField(fieldInfo, val);
    	lastValue = val;
    } else { 
    	valueAttr = frame.getLongOperandAttr();
    	long val = frame.peekLong();
    	eiFieldOwner.set2SlotField(fieldInfo, val);
    	lastValue = val;
    }
    DynamicDependency resultDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(valueAttr, objRefAttr, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    eiFieldOwner.setFieldAttr(fieldInfo, resultDep);
    
    popOperands(frame);      
    return getNext();
  }

  protected void popOperands (StackFrame frame){
    if (size == 1){
      frame.pop(2); 
    } else {
      frame.pop(3); 
    }
  }
    
  @Override
  public ElementInfo peekElementInfo (ThreadInfo ti) {
    FieldInfo fi = getFieldInfo();
    int storageSize = fi.getStorageSize();
    int objRef = ti.getTopFrame().peek( (storageSize == 1) ? 1 : 2);
    ElementInfo ei = ti.getElementInfo( objRef);

    return ei;
  }


  @Override
  public int getLength() {
    return 3; 
  }

  @Override
  public int getByteCode () {
    return 0xB5;
  }

  @Override
  public boolean isRead() {
    return false;
  }

  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}



