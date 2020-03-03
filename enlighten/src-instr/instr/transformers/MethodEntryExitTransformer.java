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


package instr.transformers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import anonymous.domain.enlighten.data.MethodName;
import instr.staticinfo.MethodInfo;
import instr.staticinfo.MethodInfoDB;


public class MethodEntryExitTransformer {
  
  private boolean instrumentStateCapture = false;
  
  public void setInstrumentStateCapture(boolean instrument) {
    instrumentStateCapture = instrument;
  }

  @SuppressWarnings("unchecked")
  public void transform(ClassNode cn) {



    List<MethodNode> allMethods = cn.methods;
    for (MethodNode mn : allMethods) {
      if ((mn.access & Opcodes.ACC_SYNTHETIC) != 0) {
        continue;
      }
      InsnList insns = mn.instructions;
      if (insns.size() == 0) {
        continue;
      }
      
      Iterator<AbstractInsnNode> j = insns.iterator();
      while (j.hasNext()) {
        AbstractInsnNode abs_ins = j.next();
        int op = abs_ins.getOpcode();
        if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
          
          InsnList il = getNotifyCallFlowInstr(cn, mn, "exit");
          if (instrumentStateCapture) {
            il.insert(getNotifyPostStatesNormalInstr(cn, mn));
          }
          insns.insert(abs_ins.getPrevious(), il);
        }
      }
      


      LabelNode methodCodeStart = new LabelNode();
      insns.insert(methodCodeStart);
      LabelNode methodCodeEnd = new LabelNode();
      insns.add(methodCodeEnd);
      InsnList handlerCode = new InsnList();
      LabelNode handlerStart = new LabelNode();
      handlerCode.add(handlerStart);
      if (instrumentStateCapture) {
        handlerCode.add(getNotifyPostStatesExceptionInstr(cn, mn));
      }
      handlerCode.add(getNotifyCallFlowInstr(cn, mn, "exception_exit"));
      handlerCode.add(new InsnNode(Opcodes.ATHROW));
      insns.add(handlerCode);
      LabelNode handlerEnd = new LabelNode();
      insns.add(handlerEnd);
      TryCatchBlockNode tryCatch = new TryCatchBlockNode(
          methodCodeStart, methodCodeEnd, handlerStart, null);
      mn.tryCatchBlocks.add(tryCatch);


      InsnList il = getNotifyCallFlowInstr(cn, mn, "entry");
      LabelNode methodEntry = new LabelNode();
      il.insert(methodEntry);
      if (instrumentStateCapture) {
        il.add(getNotifyPreStatesInstr(cn, mn));
      }
      insns.insert(il); 
      


      Type[] argumentTypes = Type.getArgumentTypes(mn.desc);
      int argumentSlots = (mn.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
      for (Type argumentType : argumentTypes) {
        argumentSlots += argumentType.getSize();
      }
      for (LocalVariableNode local : (List<LocalVariableNode>) mn.localVariables) {
        if (local.index >= 0 && local.index < argumentSlots) {
          local.start = methodEntry;
          local.end = handlerEnd;
        }
      }
    }
  }
  
  
  private InsnList getNotifyCallFlowInstr(ClassNode cn, MethodNode mn, String event) {
    InsnList il = new InsnList();
    String className = BytecodeUtils.getStandardName(cn);
    il.add(new LdcInsnNode(className));
    String mNameSig = mn.name + mn.desc;
    il.add(new LdcInsnNode(mNameSig));
    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "instr/callback/InstrumentationCallback", 
        event, "(Ljava/lang/String;Ljava/lang/String;)V", false));
    return il;
  }
  
  private InsnList getNotifyPreStatesInstr(ClassNode cn, MethodNode mn) {
    InsnList insnList = new InsnList();
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
        "instr/callback/InstrumentationCallback", "executionSwitchOut", "()V", false));
    insnList.add(getPushMethodTypeAndParamInsnList(cn, mn));
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
        "instr/callback/InstrumentationCallback", "executionSwitchIn", "()V", false));

    insnList.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC, "instr/callback/InstrumentationCallback", 
        "preStates", "(I[Ljava/lang/Object;)V", false));
    return insnList;
  }
  
  private InsnList getNotifyPostStatesNormalInstr(ClassNode cn, MethodNode mn) {
    InsnList insnList = new InsnList();
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
        "instr/callback/InstrumentationCallback", "executionSwitchOut", "()V", false));



    Type returnType = Type.getReturnType(mn.desc);
    switch (returnType.getSort()) {
    case Type.BOOLEAN:
    case Type.BYTE:
    case Type.CHAR:
    case Type.SHORT:
    case Type.INT:
    case Type.FLOAT:
      insnList.add(new InsnNode(Opcodes.DUP));
      insnList.add(BytecodeUtils.getAutoboxInsn(returnType));
      break;
    case Type.LONG:
    case Type.DOUBLE:
      insnList.add(new InsnNode(Opcodes.DUP2));
      insnList.add(BytecodeUtils.getAutoboxInsn(returnType));
      break;
    case Type.ARRAY:
    case Type.OBJECT:
      insnList.add(new InsnNode(Opcodes.DUP));
      break;
    case Type.VOID:



      insnList.add(new InsnNode(Opcodes.ACONST_NULL));
      break;
      default:
        throw new RuntimeException(
            "Unexpected return type " + returnType.getDescriptor());
    }
    insnList.add(getPushMethodTypeAndParamInsnList(cn, mn));
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
        "instr/callback/InstrumentationCallback", "executionSwitchIn", "()V", false));


    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "instr/callback/InstrumentationCallback",
        "postStatesNormal", "(Ljava/lang/Object;I[Ljava/lang/Object;)V", false));
    return insnList;
  }
  
  private InsnList getNotifyPostStatesExceptionInstr(ClassNode cn, MethodNode mn) {
    InsnList insnList = new InsnList();
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
        "instr/callback/InstrumentationCallback", "executionSwitchOut", "()V", false));
    insnList.add(new InsnNode(Opcodes.DUP));
    insnList.add(getPushMethodTypeAndParamInsnList(cn, mn));
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
        "instr/callback/InstrumentationCallback", "executionSwitchIn", "()V", false));


    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "instr/callback/InstrumentationCallback",
        "postStatesException", "(Ljava/lang/Object;I[Ljava/lang/Object;)V", false));
    return insnList;
  }
  
  
  
  private static InsnList getPushMethodTypeAndParamInsnList(ClassNode cn, MethodNode mn) {
    InsnList il = new InsnList();
    String methodSig = mn.desc;
    Type[] argumentTypes = Type.getArgumentTypes(methodSig);
    boolean isStatic = ((mn.access & Opcodes.ACC_STATIC) != 0);
    int[] argumentIndices = new int[argumentTypes.length];
    int nextArgumentIndex = (isStatic ? 0 : 1);
    for (int argumentCount = 0; argumentCount < argumentTypes.length; ++argumentCount) {
      argumentIndices[argumentCount] = nextArgumentIndex;
      nextArgumentIndex += argumentTypes[argumentCount].getSize();
    }

    MethodName methodName = MethodName.get(BytecodeUtils.getStandardName(cn), mn.name + mn.desc);
    int methodInfoId = MethodInfoDB.getMethodInfoId(methodName);
    if (methodInfoId == -1) {
      MethodInfo methodInfo = new MethodInfo(methodName);
      methodInfo.setStatic(isStatic);
      methodInfo.setParamNames(extractParameterNames(cn, mn, argumentIndices));
      methodInfoId = MethodInfoDB.addMethodInfo(methodInfo);
    }
    il.add(BytecodeUtils.getPushIntInsn(methodInfoId));
    
    int argumentNumber = argumentTypes.length;
    if (!isStatic) {
      ++argumentNumber;
    }

    il.add(BytecodeUtils.getPushIntInsn(argumentNumber));
    il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "Ljava/lang/Object;"));
    int argValueArrayIndex = 0;
    if (!isStatic) {

      il.add(new InsnNode(Opcodes.DUP));
      il.add(BytecodeUtils.getPushIntInsn(argValueArrayIndex++));
      il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      il.add(new InsnNode(Opcodes.AASTORE));
    }
    for (int formalParamIndex = 0; formalParamIndex < argumentTypes.length; ++formalParamIndex) {
      il.add(new InsnNode(Opcodes.DUP));
      il.add(BytecodeUtils.getPushIntInsn(argValueArrayIndex++));
      il.add(getPushParamAsObjectInsnList(
          argumentIndices[formalParamIndex], argumentTypes[formalParamIndex]));
      il.add(new InsnNode(Opcodes.AASTORE));
    }
    return il;
  }
  
  private static InsnList getPushParamAsObjectInsnList(int paramIndex, Type paramType) {
    InsnList insnList = new InsnList();
    switch (paramType.getSort()) {
    case Type.BOOLEAN:
    case Type.BYTE:
    case Type.CHAR:
    case Type.SHORT:
    case Type.INT:
      insnList.add(new VarInsnNode(Opcodes.ILOAD, paramIndex));
      insnList.add(BytecodeUtils.getAutoboxInsn(paramType));
      break;
    case Type.LONG:
      insnList.add(new VarInsnNode(Opcodes.LLOAD, paramIndex));
      insnList.add(BytecodeUtils.getAutoboxInsn(paramType));
      break;
    case Type.FLOAT:
      insnList.add(new VarInsnNode(Opcodes.FLOAD, paramIndex));
      insnList.add(BytecodeUtils.getAutoboxInsn(paramType));
      break;
    case Type.DOUBLE:
      insnList.add(new VarInsnNode(Opcodes.DLOAD, paramIndex));
      insnList.add(BytecodeUtils.getAutoboxInsn(paramType));
      break;
    case Type.ARRAY:
    case Type.OBJECT:
      insnList.add(new VarInsnNode(Opcodes.ALOAD, paramIndex));
      break;
    default:

      throw new RuntimeException("Unexpected parameter type " + paramType.getDescriptor());
    }
    return insnList;
  }
  
  @SuppressWarnings("unchecked")
  private static List<String> extractParameterNames(
      ClassNode cn, MethodNode mn, int[] parameterIndices) {
    if (parameterIndices.length == 0) {
      return Collections.emptyList();
    }
    int[] parameterPositions = new int[parameterIndices[parameterIndices.length - 1] + 1];
    Arrays.fill(parameterPositions, -1);
    for (int parameterPosition = 0; 
        parameterPosition < parameterIndices.length; ++parameterPosition) {
      parameterPositions[parameterIndices[parameterPosition]] = parameterPosition;
    }
    String[] parameterNames = new String[parameterIndices.length];
    for (LocalVariableNode local : (List<LocalVariableNode>) mn.localVariables) {
      int position = -1;
      if (local.index < parameterPositions.length) {
        position = parameterPositions[local.index];
      }
      if (position != -1) {
        parameterNames[position] = local.name;
      }
    }
    boolean paramNameInfoIncomplete = false;
    for (int i = 0; i < parameterNames.length; ++i) {
      if (parameterNames[i] == null) {
        parameterNames[i] = "param_" + i;
        paramNameInfoIncomplete = true;
      }
    }
    if (paramNameInfoIncomplete) {
      System.err.println("Warning: method info incomplete:");
      System.err.print(cn.name + "." + mn.name + mn.desc + "[ ");
      for (String paramName : parameterNames) {
        System.err.print(paramName + ", ");
      }
      System.err.println("]");
    }
    return Arrays.asList(parameterNames);
  }
}
