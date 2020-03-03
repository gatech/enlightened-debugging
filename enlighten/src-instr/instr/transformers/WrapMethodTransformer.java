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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import anonymous.domain.enlighten.data.MethodName;

public class WrapMethodTransformer {
  
  private static final String WRAPPED_METHODD_PREFIX = "SWIFT_INSTR_WRAPPED_METHOD_";
  
  private Set<MethodName> methodsToWrap = new HashSet<>();
  
  public void addMethodsToWrap(MethodName methodName) {
    methodsToWrap.add(methodName);
  }

  @SuppressWarnings("unchecked")
  public boolean transform(ClassNode cn) {
    if (methodsToWrap.size() == 0) {
      return false;
    }
    List<MethodNode> allMethods = cn.methods;
    for (MethodNode mn : allMethods) {
      if (mn.name.startsWith(WRAPPED_METHODD_PREFIX)) {

        return false;
      }
    }
    String standardClassName = cn.name.replace('/', '.');
    List<MethodNode> addedWrappers = new ArrayList<>();
    for (MethodNode mn : allMethods) {
      String mNameSig = mn.name + mn.desc;
      if (methodsToWrap.contains(MethodName.get(standardClassName, mNameSig))) {
        MethodNode wrapperMethod = createWrapperMethodNode(mn, cn.name);
        addedWrappers.add(wrapperMethod);
      }
    }
    allMethods.addAll(addedWrappers);
    return addedWrappers.size() != 0;
  }
  
  @SuppressWarnings("unchecked")
  private static MethodNode createWrapperMethodNode(
      MethodNode mn, String ownerClassInternalName) {
    String[] exceptionClasses = new String[mn.exceptions.size()];
    for (int i = 0; i < mn.exceptions.size(); ++i) {
      exceptionClasses[i] = (String) mn.exceptions.get(i);
    }
    MethodNode wrapperMethod = new MethodNode(
        mn.access, mn.name, mn.desc, mn.signature, exceptionClasses);
    mn.name = WRAPPED_METHODD_PREFIX + mn.name;
    wrapperMethod.visibleAnnotations = mn.visibleAnnotations;
    wrapperMethod.invisibleAnnotations = mn.invisibleAnnotations;
    Type[] argumentTypes = Type.getArgumentTypes(mn.desc);
    boolean isStatic = ((mn.access & Opcodes.ACC_STATIC) != 0);
    int[] argumentIndices = new int[argumentTypes.length];
    int nextArgumentIndex = (isStatic ? 0 : 1);
    for (int argumentCount = 0; argumentCount < argumentTypes.length; ++argumentCount) {
      argumentIndices[argumentCount] = nextArgumentIndex;
      nextArgumentIndex += argumentTypes[argumentCount].getSize();
    }
    wrapperMethod.localVariables = new ArrayList<>();
    if (argumentIndices.length > 0) {
      int maxArgumentIndex = argumentIndices[argumentIndices.length - 1];
      for (LocalVariableNode local : (List<LocalVariableNode>) mn.localVariables) {
        if (local.index >= 0 && local.index <= maxArgumentIndex) {
          wrapperMethod.localVariables.add(local);
        }
      }
    }
    wrapperMethod.instructions = new InsnList();
    if (!isStatic) {

      wrapperMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
    }
    for (int formalParamIndex = 0; formalParamIndex < argumentTypes.length; ++formalParamIndex) {
      VarInsnNode loadParamInsn = null;
      Type paramType = argumentTypes[formalParamIndex];
      int paramIndex = argumentIndices[formalParamIndex];
      switch (argumentTypes[formalParamIndex].getSort()) {
      case Type.BOOLEAN:
      case Type.BYTE:
      case Type.CHAR:
      case Type.SHORT:
      case Type.INT:
        loadParamInsn = new VarInsnNode(Opcodes.ILOAD, paramIndex);
        break;
      case Type.LONG:
        loadParamInsn = new VarInsnNode(Opcodes.LLOAD, paramIndex);
        break;
      case Type.FLOAT:
        loadParamInsn = new VarInsnNode(Opcodes.FLOAD, paramIndex);
        break;
      case Type.DOUBLE:
        loadParamInsn = new VarInsnNode(Opcodes.DLOAD, paramIndex);
        break;
      case Type.ARRAY:
      case Type.OBJECT:
        loadParamInsn = new VarInsnNode(Opcodes.ALOAD, paramIndex);
        break;
      default:

        throw new RuntimeException("Unexpected parameter type " + paramType.getDescriptor());
      }
      wrapperMethod.instructions.add(loadParamInsn);
    }
    if (isStatic) {
      wrapperMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          ownerClassInternalName, mn.name, mn.desc, false));
    } else {
      wrapperMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
          ownerClassInternalName, mn.name, mn.desc, false));
    }
    Type returnType = Type.getReturnType(mn.desc);
    InsnNode returnInsn = null;
    switch (returnType.getSort()) {
    case Type.BOOLEAN:
    case Type.BYTE:
    case Type.CHAR:
    case Type.SHORT:
    case Type.INT:
      returnInsn = new InsnNode(Opcodes.IRETURN);
      break;
    case Type.LONG:
      returnInsn = new InsnNode(Opcodes.LRETURN);
      break;
    case Type.FLOAT:
      returnInsn = new InsnNode(Opcodes.FRETURN);
      break;
    case Type.DOUBLE:
      returnInsn = new InsnNode(Opcodes.DRETURN);
      break;
    case Type.ARRAY:
    case Type.OBJECT:
      returnInsn = new InsnNode(Opcodes.ARETURN);
      break;
    case Type.VOID:
      returnInsn = new InsnNode(Opcodes.RETURN);
      break;
    default:

      throw new RuntimeException("Unexpected return type " + returnType.getDescriptor());
    }
    wrapperMethod.instructions.add(returnInsn);
    return wrapperMethod;
  }
}
