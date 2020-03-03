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

import gov.nasa.jpf.vm.bytecode.NewInstruction;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependencySource;
import anonymous.domain.enlighten.deptrack.ObjectProperty;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;



public class NEW extends NewInstruction implements JVMInstruction  {
  protected String cname;
  protected int newObjRef = MJIEnv.NULL;

  public NEW (String clsDescriptor){
    cname = Types.getClassNameFromTypeName(clsDescriptor);
  }
  
  public String getClassName(){    
    return(cname);
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    Heap heap = ti.getHeap();
    ClassInfo ci;


    try {
      ci = ti.resolveReferencedClass(cname);
    } catch(LoadOnJPFRequired lre) {
      return ti.getPC();
    }

    if (ci.initializeClass(ti)){

      return ti.getPC();
    }

    if (heap.isOutOfMemory()) { 


    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException("java.lang.OutOfMemoryError",
                                        "trying to allocate new " + cname, exDep);
    }

    ElementInfo ei = heap.newObject(ci, ti);
    int objRef = ei.getObjectRef();
    newObjRef = objRef;


    StackFrame frame = ti.getModifiableTopFrame();
    frame.pushRef( objRef);
    
    DynamicDependencySource instrDep = getInstructionDepSource(ti);
    ObjectProperty objPropDep = new ObjectProperty();
    objPropDep.setAllocationDependency(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDep)
    		.setControlDependency(getControlDependencyCondition(ti)).build());
    objPropDep.setTypeDependency(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDep)
    		.setControlDependency(getControlDependencyCondition(ti)).build());
    ei.setObjectAttr(objPropDep);
    
    frame.setOperandAttr(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDep)
    		.setControlDependency(getControlDependencyCondition(ti)).build());

    return getNext(ti);
  }

  @Override
  public int getLength() {
    return 3; 
  }

  @Override
  public int getByteCode () {
    return 0xBB;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  public int getNewObjectRef() {
    return newObjRef;
  }

  @Override
  public String toString() {
    if (newObjRef != MJIEnv.NULL){
      return "new " + cname + '@' + Integer.toHexString(newObjRef);

    } else {
      return "new " + cname;
    }
  }
}
