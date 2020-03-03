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


package gov.nasa.jpf.vm;

import anonymous.domain.enlighten.deptrack.ArrayProperty;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jvm.bytecode.JVMArrayElementInstruction;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.Types;


public class JPF_java_lang_reflect_Array extends NativePeer {
	
  @MJI
  public int getLength__Ljava_lang_Object_2__I (MJIEnv env, int clsObjRef, 
                                                    int objRef) {
    if (objRef == MJIEnv.NULL) {
      DynamicDependency exDep = DynDepBuilder.newBuilder()
      		.appendDataDependency(env.getArgAttributes()[0])
      		.setControlDependency(getInvocationCondition(env)).build();
      env.throwException(
      		"java.lang.NullPointerException", "array argument is null", exDep);
      return 0;
    }
    if (!env.isArray(objRef)) {
      DynamicDependency exDep = DynDepBuilder.newBuilder()
      		.appendDataDependency(env.getArgAttributes()[0])
      		.setControlDependency(getInvocationCondition(env)).build();
      env.throwException(
      		"java.lang.IllegalArgumentException", "argument is not an array", exDep);
      return 0;
    }
    DynamicDependency arrayLengthDep = null;
    ArrayProperty arrayPropAttr = 
    		(ArrayProperty) env.getElementInfo(objRef).getObjectAttr();
    if (arrayPropAttr != null) {
    	arrayLengthDep = arrayPropAttr.getArrayLengthDependency();
    }
    DynamicDependency resultDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(env.getArgAttributes()[0], arrayLengthDep)
    		.setControlDependency(getInvocationCondition(env)).build();
    env.setReturnAttribute(resultDep);
    return env.getArrayLength(objRef);
  }
  
  @MJI
  public int newArray__Ljava_lang_Class_2I__Ljava_lang_Object_2 (MJIEnv env, int clsRef,
                                                                        int componentTypeRef, int length) {
    ClassInfo ci = env.getReferredClassInfo(componentTypeRef);
    String clsName = ci.getName();
    
    return createNewArray( env, clsName, length);
  }
  
  static int createNewArray (MJIEnv env, String clsName, int length) {
    int aRef = MJIEnv.NULL;
    
    if ("boolean".equals(clsName)) { aRef = env.newBooleanArray(length); }
    else if ("byte".equals(clsName)) { aRef = env.newByteArray(length); }
    else if ("char".equals(clsName)) { aRef = env.newCharArray(length); }
    else if ("short".equals(clsName)) { aRef = env.newShortArray(length); }
    else if ("int".equals(clsName)) { aRef = env.newIntArray(length); }
    else if ("long".equals(clsName)) { aRef = env.newLongArray(length); }
    else if ("float".equals(clsName)) { aRef = env.newFloatArray(length); }
    else if ("double".equals(clsName)) { aRef = env.newDoubleArray(length); }
    else { aRef = env.newObjectArray(clsName, length); }
    return aRef;    
  }
  
  @MJI
  public int multiNewArray__Ljava_lang_Class_2_3I__Ljava_lang_Object_2 (MJIEnv env, int clsRef,
                                                                               int componentTypeRef,
                                                                               int dimArrayRef) {
    ClassInfo ci = env.getReferredClassInfo(componentTypeRef);
    String clsName = ci.getName();
    int n = env.getArrayLength(dimArrayRef);
    int i;

    clsName = Types.getTypeSignature(clsName, true);
    
    String arrayType = "[";
    for (i=2; i<n; i++) arrayType += '[';
    arrayType += clsName;
    
    int[] dim = new int[n];
    for (i=0; i<n; i++) {
      dim[i] = env.getIntArrayElement(dimArrayRef, i);
    }
    
    int aRef = createNewMultiArray(env, arrayType, dim, 0); 
    return aRef;
  }
  
  static int createNewMultiArray (MJIEnv env, String arrayType, int[] dim, int level) {
    int aRef = MJIEnv.NULL;
    int len = dim[level];
    
    if (level < dim.length-1) {
      aRef = env.newObjectArray(arrayType, len);
    
      for (int i=0; i<len; i++) {
        int eRef = createNewMultiArray(env, arrayType.substring(1), dim, level+1);
        env.setReferenceArrayElement(aRef, i, eRef);
      }
    } else {
      aRef = createNewArray( env, arrayType, len);
    }
    
    return aRef;
  }

  @MJI
  public int get__Ljava_lang_Object_2I__Ljava_lang_Object_2 (MJIEnv env, int clsRef,
                                                                    int aref, int index){
    String at = env.getArrayType(aref);
    int vref = MJIEnv.NULL;
    DynamicDependency resultDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(env.getElementAttr(aref, index))
    		.setControlDependency(getInvocationCondition(env)).build();
    if (at.equals("int")){
      vref = env.newObject("java.lang.Integer");
      env.setIntField(vref, "value", env.getIntArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("long")){
      vref = env.newObject("java.lang.Long");
      env.setLongField(vref, "value", env.getLongArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("double")){
      vref = env.newObject("java.lang.Double");
      env.setDoubleField(vref, "value", env.getDoubleArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("boolean")){
      vref = env.newObject("java.lang.Boolean");
      env.setBooleanField(vref, "value", env.getBooleanArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("char")){
      vref = env.newObject("java.lang.Character");
      env.setCharField(vref, "value", env.getCharArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("byte")){
      vref = env.newObject("java.lang.Byte");
      env.setByteField(vref, "value", env.getByteArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("short")){
      vref = env.newObject("java.lang.Short");
      env.setShortField(vref, "value", env.getShortArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else if (at.equals("float")){
      vref = env.newObject("java.lang.Float");
      env.setFloatField(vref, "value", env.getFloatArrayElement(aref,index));
      env.setFieldAttr(vref, "value", resultDep);
    } else {
      vref = env.getReferenceArrayElement(aref, index);
    }
    env.setReturnAttribute(resultDep);
    return vref;
  }

  private static boolean check (MJIEnv env, int[] args, Object[] argAttrs) {
  	int aref = args[0];
  	int index = args[1];
  	Object arefAttr = argAttrs[0];
  	Object indexAttr = argAttrs[1];
    if (aref == MJIEnv.NULL) {
      DynamicDependency exDep = DynDepBuilder.newBuilder()
      		.appendDataDependency(arefAttr)
      		.setControlDependency(getInvocationCondition(env)).build();
      env.throwException("java.lang.NullPointerException", "array argument is null", exDep);
      return false;
    }
    if (!env.isArray(aref)) {
      DynamicDependency exDep = DynDepBuilder.newBuilder()
      		.appendDataDependency(arefAttr)
      		.setControlDependency(getInvocationCondition(env)).build();
      env.throwException("java.lang.IllegalArgumentException", "argument is not an array", exDep);
      return false;
    }
    if (index < 0 || index >= env.getArrayLength(aref)) {
      DynamicDependency arrayLengthDep = null;
      ArrayProperty arrayPropAttr = 
      		(ArrayProperty) env.getElementInfo(aref).getObjectAttr();
      if (arrayPropAttr != null) {
      	arrayLengthDep = arrayPropAttr.getArrayLengthDependency();
      }
      DynamicDependency exDep = DynDepBuilder.newBuilder()
      		.appendDataDependency(aref, arrayLengthDep, indexAttr)
      		.setControlDependency(getInvocationCondition(env)).build();
      env.throwException("java.lang.IndexOutOfBoundsException", "index " + index + " is out of bounds", exDep);
      return false;
    }
    return true;
  }

  @MJI
  public boolean getBoolean__Ljava_lang_Object_2I__Z (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getBooleanArrayElement(aref, index);
    }
    return false;
  }

  @MJI
  public static byte getByte__Ljava_lang_Object_2I__B (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getByteArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public char getChar__Ljava_lang_Object_2I__C (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getCharArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public short getShort__Ljava_lang_Object_2I__S (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getShortArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public int getInt__Ljava_lang_Object_2I__I (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getIntArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public long getLong__Ljava_lang_Object_2I__J (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getLongArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public float getFloat__Ljava_lang_Object_2I__F (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getFloatArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public double getDouble__Ljava_lang_Object_2I__D (MJIEnv env, int clsRef, int aref, int index) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency elementDep = 
    			JVMArrayElementInstruction.getArrayElementDependency(env.getElementInfo(aref), index);
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(elementDep, argAttrs[1])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setReturnAttribute(resultDep);
      return env.getDoubleArrayElement(aref, index);
    }
    return 0;
  }

  @MJI
  public void setBoolean__Ljava_lang_Object_2IZ__V (MJIEnv env, int clsRef, int aref, int index, boolean val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setBooleanArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setByte__Ljava_lang_Object_2IB__V (MJIEnv env, int clsRef, int aref, int index, byte val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setByteArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setChar__Ljava_lang_Object_2IC__V (MJIEnv env, int clsRef, int aref, int index, char val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setCharArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setShort__Ljava_lang_Object_2IS__V (MJIEnv env, int clsRef, int aref, int index, short val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setShortArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setInt__Ljava_lang_Object_2II__V (MJIEnv env, int clsRef, int aref, int index, int val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setIntArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setLong__Ljava_lang_Object_2IJ__V (MJIEnv env, int clsRef, int aref, int index, long val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setLongArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setFloat__Ljava_lang_Object_2IF__V (MJIEnv env, int clsRef, int aref, int index, float val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setFloatArrayElement(aref, index, val);
    }
  }

  @MJI
  public void setDouble__Ljava_lang_Object_2ID__V (MJIEnv env, int clsRef, int aref, int index, double val) {
  	int[] args = new int[2];
  	args[0] = aref;
  	args[1] = index;
  	Object[] argAttrs = env.getArgAttributes();
    if (check(env, args, argAttrs)) {
    	DynamicDependency resultDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(argAttrs[1], argAttrs[2])
    			.setControlDependency(getInvocationCondition(env)).build();
    	env.setElementAttr(aref, index, resultDep);
      env.setDoubleArrayElement(aref, index, val);
    }
  }
}
