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
import anonymous.domain.enlighten.deptrack.ArrayProperty;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;



public abstract class ArrayStoreInstruction extends JVMArrayElementInstruction implements StoreInstruction, JVMInstruction {


  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    int aref = peekArrayRef(ti); 
    ElementInfo eiArray = ti.getElementInfo(aref);

    arrayOperandAttr = peekArrayAttr(ti);
    indexOperandAttr = peekIndexAttr(ti);

    if (aref == MJIEnv.NULL) {
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(arrayOperandAttr, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException("java.lang.NullPointerException", exDep);
    }
    
    return setArrayElement(ti, frame, eiArray); 
  }

  protected Instruction setArrayElement (ThreadInfo ti, StackFrame frame, ElementInfo eiArray) {
    int esize = getElementSize();
    Object attr = esize == 1 ? frame.getOperandAttr() : frame.getLongOperandAttr();
    
    popValue(frame);
    Object indexAttr = frame.getOperandAttr();
    index = frame.pop();

    Object arrayRefAttr = frame.getOperandAttr();
    arrayRef = frame.pop();

    eiArray = eiArray.getModifiableInstance();
    
    if (!eiArray.checkArrayBounds(index)) {
    	DynamicDependency arraySizeDep = null;
    	ArrayProperty arrayPropAttr = (ArrayProperty) eiArray.getObjectAttr();
    	if (arrayPropAttr != null) {
    		arraySizeDep = arrayPropAttr.getArrayLengthDependency();
    	}
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(
    					indexOperandAttr, arrayOperandAttr, arraySizeDep, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
    	return ti.createAndThrowException(
    			"java.lang.ArrayIndexOutOfBoundsException", Integer.toString(index), exDep);
    }
    setField(eiArray, index);
    DynamicDependency resultDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(attr, indexAttr, arrayRefAttr, getInstructionDepSource(ti))
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    if (resultDep != null) {
    	eiArray.setElementAttrNoClone(index, resultDep);
    }
    return getNext(ti);
  }
  
  
  @Override
  public int peekArrayRef(ThreadInfo ti) {
    return ti.getTopFrame().peek(2);
  }

  @Override
  public int peekIndex(ThreadInfo ti){
    return ti.getTopFrame().peek(1);
  }


  @Override
  public Object  peekArrayAttr (ThreadInfo ti){
    return ti.getTopFrame().getOperandAttr(2);
  }

  @Override
  public Object peekIndexAttr (ThreadInfo ti){
    return ti.getTopFrame().getOperandAttr(1);
  }


  protected abstract void popValue(StackFrame frame);
 
  protected abstract void setField (ElementInfo e, int index);


  @Override
  public boolean isRead() {
    return false;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

}
