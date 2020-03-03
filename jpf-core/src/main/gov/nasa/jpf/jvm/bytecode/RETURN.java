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

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;



public class RETURN extends JVMReturnInstruction {

  @Override
  public Instruction execute (ThreadInfo ti) {

    if (mi.isInit()) {  
      int objref = ti.getThis();
      ElementInfo ei = ti.getElementInfo(objref); 

      if (!ei.isConstructed()) {  

        ClassInfo ei_ci = ei.getClassInfo();  
        ClassInfo mi_ci = mi.getClassInfo();  

        if (ei_ci == mi_ci) { 
          ei = ei.getModifiableInstance();
          ei.setConstructed();
        }
      }

    } else if (mi.isClinit()) {


      mi.getClassInfo().setInitialized();
    }

    return super.execute(ti);
  }

  @Override
  public int getReturnTypeSize() {
    return 0;
  }
  
  @Override
  protected Object getReturnedOperandAttr (StackFrame frame) {
    return null;
  }

  
  @Override
  public Object getReturnAttr (ThreadInfo ti){
    return null; 
  }

  @Override
  protected void getAndSaveReturnValue (StackFrame frame) {

  }

  @Override
  protected void pushReturnValue (StackFrame frame) {

  }

  @Override
  public Object getReturnValue(ThreadInfo ti) {

    return null;
  }
  
  @Override
  public void setReturnAttr (ThreadInfo ti, Object a){
  	return;
  }

  @Override
  public String toString() {
    return "return  " + mi.getFullName();
  }

  @Override
  public int getByteCode () {
    return 0xB1;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
