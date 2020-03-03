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
import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;



public class LDC extends DependencyTrackingInstruction implements JVMInstruction {

  public enum Type {STRING, CLASS, INT, FLOAT};

  Type type;

  protected String  string;  
  protected int     value;

  public LDC() {}

  public LDC (String s, boolean isClass){
    if (isClass){
      string = Types.getClassNameFromTypeName(s);
      type = Type.CLASS;
    } else {
      string = s;
      type = Type.STRING;
    }
  }

  public LDC (int v){
    value = v;
    type = Type.INT;
  }

  public LDC (float f){
    value = Float.floatToIntBits(f);
    type = Type.FLOAT;
  }


  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();

    DynamicDependency instrDep = getInstructionDepSource(ti);
    
    switch (type){
      case STRING:

        ElementInfo eiValue = ti.getHeap().newInternString(string, ti); 






        FieldInfo valueField = eiValue.getClassInfo().getDeclaredInstanceField("value");
        DynamicDependency valueFieldDep = DynDepBuilder.newBuilder()
        		.appendDataDependency(eiValue.getFieldAttr(valueField), instrDep)
        		.setControlDependency(getControlDependencyCondition(ti)).build();
        eiValue.setFieldAttr(valueField, valueFieldDep);
        ElementInfo strCharArray = ti.getElementInfo(eiValue.getReferenceField(valueField));
        int charArrayLength = strCharArray.getArrayFields().arrayLength();
        for (int i = 0; i < charArrayLength; ++i) {
        	DynamicDependency charDep = DynDepBuilder.newBuilder()
          		.appendDataDependency(strCharArray.getElementAttr(i), instrDep)
          		.setControlDependency(getControlDependencyCondition(ti)).build();
        	strCharArray.setElementAttr(i, charDep);
        }
        value = eiValue.getObjectRef();
        frame.pushRef(value);
        break;

      case INT:
      case FLOAT:
        frame.push(value);
        break;

      case CLASS:
        ClassInfo ci;

        try {
          ci = ti.resolveReferencedClass(string);
        } catch(LoadOnJPFRequired lre) {
          return frame.getPC();
        }




        if (!ci.isRegistered()) {
          ci.registerClass(ti);
        }

        frame.pushRef( ci.getClassObjectRef());

        break;
    }
    
    DynamicDependency resultDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(instrDep)
    		.setControlDependency(getControlDependencyCondition(ti)).build();
    if (resultDep != null) {
    	frame.setOperandAttr(resultDep);
    }
    
    return getNext(ti);
  }

  @Override
  public int getLength() {
    return 2; 
  }

  @Override
  public int getByteCode () {
    return 0x12;
  }
  
  public int getValue() {
    return value;
  }
  
  public Type getType() {
    return type;
  }
  
  public boolean isString() {
    return (type == Type.STRING);
  }
  
  public float getFloatValue(){
	  if(type!=Type.FLOAT){
      throw new IllegalStateException();
	  }
    
	  return Float.intBitsToFloat(value);
	}

  public String getStringValue() { 
    if (type == Type.STRING) {
      return string;
    } else {
      return null;
    }
  }
  
  public String getClassValue() { 
	    if (type == Type.CLASS) {
	      return string;
	    } else {
	      return null;
	    }
	  }

  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
