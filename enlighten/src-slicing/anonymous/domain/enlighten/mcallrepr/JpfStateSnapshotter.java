/* 
 * Copyright 2019 Georgia Institute of Technology
 * All rights reserved.
 *
 * Author(s): Xiangyu Li <xiangyu.li@cc.gatech.edu>
 *
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package anonymous.domain.enlighten.mcallrepr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

import anonymous.domain.enlighten.deptrack.ArrayProperty;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.mcallrepr.ArrayElementRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayLengthRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayRepr;
import anonymous.domain.enlighten.mcallrepr.FieldReferenceName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.MethodCallSpecialRefName;
import anonymous.domain.enlighten.mcallrepr.NullRepr;
import anonymous.domain.enlighten.mcallrepr.PrimitiveRepr;
import anonymous.domain.enlighten.mcallrepr.ReflectedObjectRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.mcallrepr.VoidRepr;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ExceptionInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;

public class JpfStateSnapshotter {
  
  private IdentityHashMap<ElementInfo, ValueGraphNode> visited = new IdentityHashMap<>();
  
  public MethodCallRepr fromStackFrame(ThreadInfo ti, StackFrame methodStackFrame) {
    MethodInfo methodInfo = methodStackFrame.getMethodInfo();
    MethodCallRepr invocationRepr = new MethodCallRepr(
        "", methodInfo.getClassName(), methodInfo.getName() + methodInfo.getSignature());
    LocalVarInfo[] argInfos = methodInfo.getArgumentLocalVars();
    if (argInfos != null) {
      for (LocalVarInfo argInfo : argInfos) {
        if (argInfo == null) {



          continue;
        }
        Object argAttr = methodStackFrame.getLocalAttr(argInfo.getSlotIndex());
        DynamicDependency argDep = 
            JpfEntityConversion.getDynamicDependencyFromAttr(argAttr);
        Object argValueObj = methodStackFrame.getLocalValueObject(argInfo);
        byte argTypeCode = Types.getBuiltinTypeFromSignature(argInfo.getSignature());
        ValueGraphNode objRepr = null;
        if (argTypeCode == Types.T_ARRAY || argTypeCode == Types.T_REFERENCE) {
          objRepr = fromElementInfo(ti, (ElementInfo) argValueObj);
        } else {
          objRepr = fromBoxedPrimitive(argValueObj);
        }
        if ("this".equals(argInfo.getName())) {
          invocationRepr.setThizz(objRepr);
          if (argDep != null) {
            MemberRefDepAnnotator.annotateDependency(invocationRepr, 
                MethodCallSpecialRefName.thisRef(), argDep);
          }
        } else{
          invocationRepr.setParam(argInfo.getName(), objRepr); 
          if (argDep != null) {
            MemberRefDepAnnotator.annotateDependency(invocationRepr, 
                MethodCallSpecialRefName.fromParamName(argInfo.getName()), argDep);
          }
        }
      }
    } else {
      System.err.println(
          "Warning: no argument variable info for method " + methodInfo.getFullName());
    }
    if (ti.getPC() instanceof JVMReturnInstruction) {
      Object returnValueAttr = null;
      int returnSize = methodInfo.getReturnSize();
      if (returnSize == 1) {
        returnValueAttr = methodStackFrame.getOperandAttr();
      } else if (returnSize == 2) {
        returnValueAttr = methodStackFrame.getLongOperandAttr();
      }
      DynamicDependency returnValueDep = 
          JpfEntityConversion.getDynamicDependencyFromAttr(returnValueAttr);
      byte returnTypeCode = methodInfo.getReturnTypeCode();
      switch (returnTypeCode) {
      case Types.T_ARRAY:
      case Types.T_REFERENCE:
        int objRef = methodStackFrame.peek();
        ElementInfo retObj = ti.getElementInfo(objRef);
        invocationRepr.setReturnVal(fromElementInfo(ti, retObj));
        break;
      case Types.T_BOOLEAN:
        int retValue = methodStackFrame.peek();
        invocationRepr.setReturnVal(fromPrimitive(retValue != 0));
        break;
      case Types.T_BYTE:
        retValue = methodStackFrame.peek();
        invocationRepr.setReturnVal(fromPrimitive((byte) retValue));
        break;
      case Types.T_CHAR:
        retValue = methodStackFrame.peek();
        invocationRepr.setReturnVal(fromPrimitive((char) retValue));
        break;
      case Types.T_SHORT:
        retValue = methodStackFrame.peek();
        invocationRepr.setReturnVal(fromPrimitive((short) retValue));
        break;
      case Types.T_INT:
        retValue = methodStackFrame.peek();
        invocationRepr.setReturnVal(fromPrimitive(retValue));
        break;
      case Types.T_LONG:
        long longRetValue = methodStackFrame.peekLong();
        invocationRepr.setReturnVal(fromPrimitive(longRetValue));
        break;
      case Types.T_FLOAT:
        float floatRetValue = methodStackFrame.peekFloat();
        invocationRepr.setReturnVal(fromPrimitive(floatRetValue));
        break;
      case Types.T_DOUBLE:
        double doubleRetValue = methodStackFrame.peekDouble();
        invocationRepr.setReturnVal(fromPrimitive(doubleRetValue));
        break;
      case Types.T_VOID:
        invocationRepr.setReturnVal(VoidRepr.get());
        break;
      default:
        throw new RuntimeException("Unexpected return type " + methodInfo.getReturnType());
      }
      if (returnValueDep != null) {
        MemberRefDepAnnotator.annotateDependency(invocationRepr, 
            MethodCallSpecialRefName.returnValue(), returnValueDep);
      }
    } else { 
      ExceptionInfo xInfo = ti.getPendingException();
      if (xInfo != null) {

        ElementInfo exceptionObj = xInfo.getException();
        DynamicDependency exceptionDep = xInfo.getDependency();
        invocationRepr.setException(fromElementInfo(ti, exceptionObj));
        if (exceptionDep != null) {
          MemberRefDepAnnotator.annotateDependency(invocationRepr, 
              MethodCallSpecialRefName.exceptionThrown(), exceptionDep);
        }
      } 
    }
    return invocationRepr;
  }

  public ValueGraphNode fromElementInfo(ThreadInfo ti, ElementInfo value) {
    if (value == null) {
      return NullRepr.get();
    }
    if (visited.containsKey(value)) {
      return visited.get(value);
    }
    if (value.isArray()) {
      ArrayRepr repr = new ArrayRepr(
          value.getClassInfo().getName(), System.identityHashCode(value));
      visited.put(value, repr);
      int arrayLength = value.getArrayFields().arrayLength();
      List<ValueGraphNode> elementReprList = new ArrayList<>();
      for (int i = 0; i < arrayLength; ++i) {
        Object elementAttr = value.getElementAttr(i);
        DynamicDependency elementDep = 
            JpfEntityConversion.getDynamicDependencyFromAttr(elementAttr);
        switch (Types.getBuiltinTypeFromSignature(value.getArrayType())) {
        case Types.T_ARRAY:
        case Types.T_REFERENCE:
          int objRef = value.getReferenceElement(i);
          ElementInfo arrElement = ti.getElementInfo(objRef);
          elementReprList.add(fromElementInfo(ti, arrElement));
          break;
        case Types.T_BOOLEAN:
          elementReprList.add(fromPrimitive(value.getBooleanElement(i)));
          break;
        case Types.T_BYTE:
          elementReprList.add(fromPrimitive(value.getByteElement(i)));
          break;
        case Types.T_CHAR:
          elementReprList.add(fromPrimitive(value.getCharElement(i)));
          break;
        case Types.T_SHORT:
          elementReprList.add(fromPrimitive(value.getShortElement(i)));
          break;
        case Types.T_INT:
          elementReprList.add(fromPrimitive(value.getIntElement(i)));
          break;
        case Types.T_LONG:
          elementReprList.add(fromPrimitive(value.getLongElement(i)));
          break;
        case Types.T_FLOAT:
          elementReprList.add(fromPrimitive(value.getFloatElement(i)));
          break;
        case Types.T_DOUBLE:
          elementReprList.add(fromPrimitive(value.getDoubleElement(i)));
          break;
        default:
          throw new RuntimeException("Unexpected array element type: " + value.getArrayType());
        }
        if (elementDep != null) {
          MemberRefDepAnnotator.annotateDependency(repr, new ArrayElementRefName(i), elementDep);
        }
      }
      repr.setElements(new PrimitiveRepr("int", arrayLength), elementReprList);
      DynamicDependency arrayLengthDep = null;
      ArrayProperty arrayProp = (ArrayProperty) value.getObjectAttr();
      if (arrayProp != null) {
        arrayLengthDep = arrayProp.getArrayLengthDependency();
      }
      if (arrayLengthDep != null) {
        MemberRefDepAnnotator.annotateDependency(repr, ArrayLengthRefName.get(), arrayLengthDep);
      }
      return repr;
    } else {
      ReflectedObjectRepr repr = new ReflectedObjectRepr(
          value.getClassInfo().getName(), System.identityHashCode(value));
      visited.put(value, repr);
      List<FieldInfo> allFields = getAllFields(value.getClassInfo());
      for (FieldInfo field : allFields) {
        Object fieldValueAttr = value.getFieldAttr(field);
        DynamicDependency fieldValueDep = 
            JpfEntityConversion.getDynamicDependencyFromAttr(fieldValueAttr);
        switch (Types.getBuiltinTypeFromSignature(field.getSignature())) {
        case Types.T_ARRAY:
        case Types.T_REFERENCE:
          int objRef = value.getReferenceField(field);
          ElementInfo fieldElement = ti.getElementInfo(objRef);
          repr.putField(field.getName(), fromElementInfo(ti, fieldElement));
          break;
        case Types.T_BOOLEAN:
          repr.putField(field.getName(), fromPrimitive(value.getBooleanField(field)));
          break;
        case Types.T_BYTE:
          repr.putField(field.getName(), fromPrimitive(value.getByteField(field)));
          break;
        case Types.T_CHAR:
          repr.putField(field.getName(), fromPrimitive(value.getCharField(field)));
          break;
        case Types.T_SHORT:
          repr.putField(field.getName(), fromPrimitive(value.getShortField(field)));
          break;
        case Types.T_INT:
          repr.putField(field.getName(), fromPrimitive(value.getIntField(field)));
          break;
        case Types.T_LONG:
          repr.putField(field.getName(), fromPrimitive(value.getLongField(field)));
          break;
        case Types.T_FLOAT:
          repr.putField(field.getName(), fromPrimitive(value.getFloatField(field)));
          break;
        case Types.T_DOUBLE:
          repr.putField(field.getName(), fromPrimitive(value.getDoubleField(field)));
          break;
        default:
          throw new RuntimeException("Unexpected field type: " + field.getType());
        }
        if (fieldValueDep != null) {
          MemberRefDepAnnotator.annotateDependency(
              repr, new FieldReferenceName(field.getName()), fieldValueDep);
        }
      }
      return repr;
    }
  }

  public static ValueGraphNode fromBoxedPrimitive(Object boxedPrimitive) {
    if (boxedPrimitive instanceof Boolean) {
      return fromPrimitive(((Boolean) boxedPrimitive).booleanValue());
    } else if (boxedPrimitive instanceof Byte) {
      return fromPrimitive(((Byte) boxedPrimitive).byteValue());
    } else if (boxedPrimitive instanceof Character) {
      return fromPrimitive(((Character) boxedPrimitive).charValue());
    } else if (boxedPrimitive instanceof Short) {
      return fromPrimitive(((Short) boxedPrimitive).shortValue());
    } else if (boxedPrimitive instanceof Integer) {
      return fromPrimitive(((Integer) boxedPrimitive).intValue());
    } else if (boxedPrimitive instanceof Long) {
      return fromPrimitive(((Long) boxedPrimitive).longValue());
    } else if (boxedPrimitive instanceof Float) {
      return fromPrimitive(((Float) boxedPrimitive).floatValue());
    } else if (boxedPrimitive instanceof Double) {
      return fromPrimitive(((Double) boxedPrimitive).doubleValue());
    } else {
      throw new RuntimeException("Unexpected boxed primitive type " 
          + boxedPrimitive.getClass().getName());
    }
  }
  
  public static ValueGraphNode fromPrimitive(boolean value) {
    PrimitiveRepr repr = new PrimitiveRepr("boolean", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(byte value) {
    PrimitiveRepr repr = new PrimitiveRepr("byte", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(char value) {
    PrimitiveRepr repr = new PrimitiveRepr("char", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(short value) {
    PrimitiveRepr repr = new PrimitiveRepr("short", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(int value) {
    PrimitiveRepr repr = new PrimitiveRepr("int", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(long value) {
    PrimitiveRepr repr = new PrimitiveRepr("long", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(float value) {
    PrimitiveRepr repr = new PrimitiveRepr("float", value);
    return repr;
  }
  
  public static ValueGraphNode fromPrimitive(double value) {
    PrimitiveRepr repr = new PrimitiveRepr("double", value);
    return repr;
  }
  
  private static List<FieldInfo> getAllFields(ClassInfo ci) {
    List<FieldInfo> allFields = new ArrayList<>();
    ClassInfo currentCls = ci;
    while (currentCls != null) {
      allFields.addAll(Arrays.asList(currentCls.getDeclaredInstanceFields()));
      currentCls = currentCls.getSuperClass();
    }
    return allFields;
  }
}
