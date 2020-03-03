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
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.ReturnInstruction;

import java.util.Iterator;

import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependencySource;



public abstract class JVMReturnInstruction extends ReturnInstruction implements JVMInstruction {


  protected StackFrame returnFrame;

  abstract public int getReturnTypeSize();
  abstract protected Object getReturnedOperandAttr(StackFrame frame);
  


  abstract protected void getAndSaveReturnValue (StackFrame frame);
  abstract protected void pushReturnValue (StackFrame frame);

  public abstract Object getReturnValue(ThreadInfo ti);

  public StackFrame getReturnFrame() {
    return returnFrame;
  }

  public void setReturnFrame(StackFrame frame){
    returnFrame = frame;
  }

  
  @Override
  public void cleanupTransients(){
    returnFrame = null;
  }
  

  




  
  public boolean hasReturnAttr (ThreadInfo ti){
    StackFrame frame = ti.getTopFrame();
    return frame.hasOperandAttr();
  }
  public boolean hasReturnAttr (ThreadInfo ti, Class<?> type){
    StackFrame frame = ti.getTopFrame();
    return frame.hasOperandAttr(type);
  }
  
  
  public Object getReturnAttr (ThreadInfo ti){
    StackFrame frame = ti.getTopFrame();
    return frame.getOperandAttr();
  }

  
  public void setReturnAttr (ThreadInfo ti, Object a){
    StackFrame frame = ti.getModifiableTopFrame();
    frame.setOperandAttr(a);
  }
  
  public void addReturnAttr (ThreadInfo ti, Object attr){
    StackFrame frame = ti.getModifiableTopFrame();
    frame.addOperandAttr(attr);
  }

  
  public <T> T getReturnAttr (ThreadInfo ti, Class<T> type){
    StackFrame frame = ti.getTopFrame();
    return frame.getOperandAttr(type);
  }
  public <T> T getNextReturnAttr (ThreadInfo ti, Class<T> type, Object prev){
    StackFrame frame = ti.getTopFrame();
    return frame.getNextOperandAttr(type, prev);
  }
  public Iterator<?> returnAttrIterator (ThreadInfo ti){
    StackFrame frame = ti.getTopFrame();
    return frame.operandAttrIterator();
  }
  public <T> Iterator<T> returnAttrIterator (ThreadInfo ti, Class<T> type){
    StackFrame frame = ti.getTopFrame();
    return frame.operandAttrIterator(type);
  }
  

  
  @Override
  public Instruction execute (ThreadInfo ti) {
  	DynamicDependencySource instrDep = getInstructionDepSource(ti);
    StackFrame frame = ti.getModifiableTopFrame();
  	Object attr = getReturnedOperandAttr(frame);
    DynamicDependency resultDependences = DynDepBuilder.newBuilder()
    		.appendDataDependency(attr, instrDep)
    		.setControlDependency(getControlDependencyCondition(ti)).build();


  	setReturnAttr(ti, resultDependences);
  	
    boolean didUnblock = false;
    
    if (!ti.isFirstStepInsn()) {
      didUnblock = ti.leave();  
    }
    
    if (mi.isSynchronized()) {
      int objref = mi.isStatic() ? mi.getClassInfo().getClassObjectRef() : ti.getThis();
      ElementInfo ei = ti.getElementInfo(objref);

      if (ei.getLockCount() == 0) {
        if (ti.getScheduler().setsLockReleaseCG(ti, ei, didUnblock)){
          return this;
        }
      }
    }

    returnFrame = frame;

    getAndSaveReturnValue(frame);
    


    frame = ti.popAndGetModifiableTopFrame();



    frame.removeArguments(mi);
    pushReturnValue(frame);

    if (resultDependences != null) {
      setReturnAttr(ti, resultDependences);
    }

    return frame.getPC().getNext();
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
  
  @Override
  public String toPostExecString() {
    return getMnemonic() + " [" + mi.getFullName() + ']';
  }
}
