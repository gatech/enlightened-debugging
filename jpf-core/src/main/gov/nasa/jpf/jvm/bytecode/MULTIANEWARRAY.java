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
import anonymous.domain.enlighten.deptrack.ArrayProperty;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependencySource;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;



public class MULTIANEWARRAY extends DependencyTrackingInstruction implements JVMInstruction {
  protected String type;
  
  protected int dimensions;
  protected int[] arrayLengths;

  public MULTIANEWARRAY (String typeName, int dimensions){
    this.type = Types.getClassNameFromTypeName(typeName);
    this.dimensions = dimensions;
  }

  public int allocateArray (Heap heap, String type, int[] dim, Object[] dimAttrs, 
  		ThreadInfo ti, int d, DynamicDependencySource instrDepTag) {
    int l = dim[d];
    ElementInfo eiArray = heap.newArray(type.substring(d + 1), l, ti);
    
    Object lengthAttr = dimAttrs[d];
    ArrayProperty arrayPropDep = new ArrayProperty();
    arrayPropDep.setAllocationDependency(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDepTag)
    		.setControlDependency(getControlDependencyCondition(ti)).build());
    arrayPropDep.setTypeDependency(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDepTag)
    		.setControlDependency(getControlDependencyCondition(ti)).build());
    arrayPropDep.setArrayLengthDependency(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDepTag, lengthAttr)
    		.setControlDependency(getControlDependencyCondition(ti)).build());
    eiArray.setObjectAttr(arrayPropDep);

    if (dim.length > (d + 1)) {
      for (int i = 0; i < l; i++) {
        eiArray.setReferenceElement(i, allocateArray(heap, type, dim, dimAttrs, ti, d + 1, instrDepTag));
        eiArray.setElementAttr(i, DynDepBuilder.newBuilder()
        		.appendDataDependency(instrDepTag)
        		.setControlDependency(getControlDependencyCondition(ti)).build());
      }
    }

    return eiArray.getObjectRef();
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    String compType = Types.getComponentTerminal(type);


    if(Types.isReferenceSignature(type)) {
      try {
        ti.resolveReferencedClass(compType);
      } catch(LoadOnJPFRequired lre) {
        return ti.getPC();
      }
    }

    arrayLengths = new int[dimensions];
    Object[] arrayLengthAttrs = new Object[dimensions];
    StackFrame frame = ti.getModifiableTopFrame();

    for (int i = dimensions - 1; i >= 0; i--) {
    	arrayLengthAttrs[i] = frame.getOperandAttr();
      arrayLengths[i] = frame.pop();
      if (arrayLengths[i] < 0) {
      	DynamicDependency exDep = DynDepBuilder.newBuilder()
      			.appendDataDependency(arrayLengthAttrs[i], getInstructionDepSource(ti))
      			.setControlDependency(getControlDependencyCondition(ti)).build();
      	return ti.createAndThrowException("java.lang.NegativeArraySizeException", exDep);
      }
    }



    ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(type);
    if (!ci.isRegistered()) {
      ci.registerClass(ti);
      ci.setInitialized();
    }
    
    DynamicDependencySource instrDepTag = getInstructionDepSource(ti);
    int arrayRef = allocateArray(
    		ti.getHeap(), type, arrayLengths, arrayLengthAttrs, ti, 0, instrDepTag);


    frame.pushRef(arrayRef);
    frame.setOperandAttr(DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDepTag)
    		.setControlDependency(getControlDependencyCondition(ti)).build());

    return getNext(ti);
  }

  @Override
  public int getLength() {
    return 4; 
  }
  
  @Override
  public int getByteCode () {
    return 0xC5;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  public String getType(){
    return type;
  }
  
  public int getDimensions() {
    return dimensions;
  }
  
  public int getArrayLength (int dimension){
    if (dimension < dimensions && arrayLengths != null){
      return arrayLengths[dimension];
    } else {
      return -1;
    }
  }
  
  @Override
  public void cleanupTransients(){
    arrayLengths= null;
  }
}
