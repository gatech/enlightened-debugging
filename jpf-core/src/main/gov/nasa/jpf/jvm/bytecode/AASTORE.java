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

import anonymous.domain.enlighten.deptrack.ArrayProperty;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.ObjectProperty;
import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;



public class AASTORE extends ArrayStoreInstruction {

  protected int value;
  protected Object indexAttr;
  protected Object arefAttr;

  @Override
  public boolean isReferenceArray() {
    return true;
  }
  
  @Override
  protected void popValue(StackFrame frame){
    value = frame.pop();
  }

  @Override
  protected void setField (ElementInfo ei, int index) {
    ei.setReferenceElement(index, value);
  }
  
  
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    int refValue = frame.peek();
    int idx = frame.peek(1);
    int aref = frame.peek(2);
    
    indexAttr = frame.getOperandAttr(1);
    arefAttr = frame.getOperandAttr(2);
    
    value = aref;
    index = idx;
    
    if (aref == MJIEnv.NULL) {
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(arefAttr, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException("java.lang.NullPointerException", exDep);
    }
    
    ElementInfo eiArray = ti.getModifiableElementInfo(aref);
    Instruction xInsn = checkArrayStoreException(ti, frame, eiArray);
    if (xInsn != null){
      return xInsn;
    }
    Object attr = frame.getOperandAttr();
    if (!eiArray.checkArrayBounds(idx)) {
    	DynamicDependency arraySizeDep = null;
    	ArrayProperty arrayPropAttr = (ArrayProperty) eiArray.getObjectAttr();
    	if (arrayPropAttr != null) {
    		arraySizeDep = arrayPropAttr.getArrayLengthDependency();
    	}
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(arefAttr, indexAttr, arraySizeDep, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
    	return ti.createAndThrowException(
    			"java.lang.ArrayIndexOutOfBoundsException", Integer.toString(index), exDep);
    }
    eiArray.setReferenceElement(idx, refValue);
    DynamicDependency resultDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(indexAttr, arefAttr, attr, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    if (resultDep != null) {
    	eiArray.setElementAttrNoClone(idx, resultDep);
    }
    frame.pop(3);
    return getNext(ti);      
  }

  protected Instruction checkArrayStoreException(ThreadInfo ti, StackFrame frame, ElementInfo ei){
    ClassInfo c = ei.getClassInfo();
    int refVal = frame.peek();
    
    if (refVal != MJIEnv.NULL) { 
      ClassInfo elementCi = ti.getClassInfo(refVal);
      ClassInfo arrayElementCi = c.getComponentClassInfo();
      if (!elementCi.isInstanceOf(arrayElementCi)) {
        String exception = "java.lang.ArrayStoreException";
        String exceptionDescription = elementCi.getName();
        
        Object refValueAttr = frame.getOperandAttr();
        DynamicDependency objTypeDep = null;
        ObjectProperty objPropAttr = 
        		(ObjectProperty) ti.getElementInfo(refVal).getObjectAttr();
        if (objPropAttr != null) {
        	objTypeDep = objPropAttr.getTypeDependency();
        }
        DynamicDependency arrayTypeDep = null;
        ObjectProperty arrayPropAttr = 
        		(ObjectProperty) ei.getObjectAttr();
        if (arrayPropAttr != null) {
        	arrayTypeDep = arrayPropAttr.getTypeDependency();
        }
        DynamicDependency exDep = DynDepBuilder.newBuilder()
        		.appendDataDependency(
        				arefAttr, refValueAttr, arrayTypeDep, objTypeDep, getInstructionDepSource(ti))
        		.setControlDependency(getControlDependencyCondition(ti)).build();
        		
        return ti.createAndThrowException(exception, exceptionDescription, exDep);
      }
    }

    return null;
  }


  @Override
  public int getByteCode () {
    return 0x53;
  }

  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
