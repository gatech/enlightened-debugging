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

import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javassist.bytecode.Opcode;

public class MemoryAccessTransformer {
  
  private static final String CALLBACK_CLASS_NAME = "instr/callback/InstrumentationCallback";
  private static final int INSTRUMENT_MACCESS_METHOD_SIZE_LIMIT = 5000;
  
  @SuppressWarnings("unchecked")
  public void transform(ClassNode cn) {
    for (MethodNode mn : (List<MethodNode>) cn.methods) {
      InsnList insns = mn.instructions;
      if (insns.size() == 0) { 
        continue;
      }
      if (insns.size() >= INSTRUMENT_MACCESS_METHOD_SIZE_LIMIT) {
        System.err.println("Warning: method body of " 
            + cn.name + "." + mn.name + mn.desc + " is too large.\n"
            + "Skipping instrumenting memory access instructions on this method.");
        continue;
      }
      Iterator<AbstractInsnNode> insnItr = insns.iterator();
      while (insnItr.hasNext()) {
        AbstractInsnNode insn = insnItr.next();
        int op = insn.getOpcode();
        if (isArrayElementAccessInsn(insn)) {
          InsnList list = getArrayElementAccessProbingInstr(insn);
          insns.insertBefore(insn, list);
        } else if (insn instanceof FieldInsnNode) {
          FieldInsnNode fieldInsn = (FieldInsnNode)insn;
          InsnList list = getFieldAccessProbingInstr(fieldInsn);
          insns.insertBefore(insn, list);
        } else if (op == Opcodes.NEW) {
          insns.insert(insn, getNewObjectProbingInstr());
        } else if (isNewArrayInsn(insn)) {
          insns.insert(insn, getNewArrayProbingInstr());
        } else if (isSystemArrayCopy(insn)) {
          insns.insertBefore(insn, getSysArrayCopyProbingInstr());
        }
      }
    } 
  }
  
  private boolean isArrayElementAccessInsn(AbstractInsnNode insn) {
    int opCode = insn.getOpcode();
    if ((opCode >= Opcodes.IASTORE && opCode <= Opcodes.SASTORE) ||
            (opCode >= Opcodes.IALOAD && opCode <= Opcodes.SALOAD)) {
      return true;
    }
    if (opCode == Opcodes.INVOKESTATIC) {
      MethodInsnNode staticInvocationInsn = (MethodInsnNode) insn;
      if ("java/lang/reflect/Array".equals(staticInvocationInsn.owner)) {
        String methodName = staticInvocationInsn.name;
        if (!methodName.equals("getLength") &&
            (methodName.startsWith("get") || methodName.startsWith("set"))) {
          return true;
        }
      }
    }
    return false;
  }
  
  private InsnList getArrayElementAccessProbingInstr(AbstractInsnNode insn) {
    boolean isWrite = false;
    boolean isDword = false;
    int opCode = insn.getOpcode();
    if (opCode == Opcodes.INVOKESTATIC) {
      MethodInsnNode callInsn = (MethodInsnNode) insn;
      String methodName = callInsn.name;
      if (methodName.startsWith("set")) {
        isWrite = true;
        Type[] argTypes = Type.getArgumentTypes(callInsn.desc);
        Type dataElementType = argTypes[2];
        if (dataElementType.equals(Type.LONG_TYPE) 
            || dataElementType.equals(Type.DOUBLE_TYPE)) {
          isDword = true;
        }
      } 
    } else {
      if (opCode >= Opcodes.IASTORE && opCode <= Opcodes.SASTORE) {
        isWrite = true;
      }
      if (opCode == Opcodes.DALOAD || opCode == Opcodes.DASTORE
          || opCode == Opcodes.LALOAD || opCode == Opcodes.LASTORE) {
        isDword = true;
      }
    }
    return getArrayElementAccessProbingInstr(isWrite, isDword);
  }

  private InsnList getArrayElementAccessProbingInstr(boolean isWrite, boolean isDword) {
    InsnList il = new InsnList();    

    String callbackMethodName;
    String signature = "(Ljava/lang/Object;I)V";
    if (isWrite) {
      callbackMethodName = "writeArrayBucket";
      if (isDword) {


        il.add(new InsnNode(Opcodes.DUP2_X2));
        il.add(new InsnNode(Opcodes.POP2));
        il.add(new InsnNode(Opcodes.DUP2_X2));
      } else {


        il.add(new InsnNode(Opcodes.DUP_X2));
        il.add(new InsnNode(Opcodes.POP));
        il.add(new InsnNode(Opcodes.DUP2_X1));
      }
    } else {
      callbackMethodName = "readArrayBucket";

      il.add(new InsnNode(Opcodes.DUP2));
    }
    il.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC, CALLBACK_CLASS_NAME, callbackMethodName, signature, false));
    return il;
  }
  
  private InsnList getFieldAccessProbingInstr(FieldInsnNode instr) {
    boolean isWrite = false;
    boolean isStatic = false;
    boolean isDword = false;
    int opCode = instr.getOpcode();
    if (opCode == Opcodes.PUTFIELD || opCode == Opcodes.PUTSTATIC) {
      isWrite = true;
    }
    if (opCode == Opcodes.GETSTATIC || opCode == Opcodes.PUTSTATIC) {
      isStatic = true;
    }
    Type fieldType = Type.getType(instr.desc);
    if (fieldType.equals(Type.LONG_TYPE) || fieldType.equals(Type.DOUBLE_TYPE)) {
      isDword = true;
    }
    InsnList il = new InsnList();
    String callbackMethodName;
    if (isWrite) {
      callbackMethodName = "write";
    } else {
      callbackMethodName = "read";
    }
    if (isStatic) {
      callbackMethodName += "StaticField";
    } else {      
      callbackMethodName += "InstanceField";
    }
    String signature;
    if (isStatic) {

      signature = "(Ljava/lang/String;Ljava/lang/String;)V";
      il.add(new LdcInsnNode(instr.owner));
    } else {

      signature = "(Ljava/lang/Object;Ljava/lang/String;)V";
      if (isWrite) {
        if (isDword) {


          il.add(new InsnNode(Opcodes.DUP2_X1));
          il.add(new InsnNode(Opcodes.POP2));
          il.add(new InsnNode(Opcodes.DUP_X2));
        } else {


          il.add(new InsnNode(Opcodes.DUP2));
          il.add(new InsnNode(Opcodes.POP));
        }
      } else {
        il.add(new InsnNode(Opcodes.DUP));
      }
    }
    il.add(new LdcInsnNode(instr.name));
    il.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC, CALLBACK_CLASS_NAME, callbackMethodName, signature, false));
    return il;
  }
  
  private InsnList getNewObjectProbingInstr() {
    String callbackMethodName = "newObject";
    String signature = "(Ljava/lang/Object;)V";
    InsnList il = new InsnList();
    il.add(new InsnNode(Opcodes.DUP));
    il.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC, CALLBACK_CLASS_NAME, callbackMethodName, signature, false));
    return il;
  }
  
  private boolean isNewArrayInsn(AbstractInsnNode insn) {
    int opCode = insn.getOpcode();
    if (opCode == Opcodes.NEWARRAY || opCode == Opcodes.ANEWARRAY
        || opCode == Opcodes.MULTIANEWARRAY) {
      return true;
    }
    if (opCode == Opcodes.INVOKESTATIC) {
      MethodInsnNode invokeInsn = (MethodInsnNode) insn;
      if ("java/lang/reflect/Array".equals(invokeInsn.owner)) {
        String methodName = invokeInsn.name;
        if (methodName.equals("newArray") || methodName.equals("multiNewArray")) {
          return true;
        }
      }
    }
    return false;
  }
  
  private InsnList getNewArrayProbingInstr() {
    String callbackMethodName = "newArray";
    String signature = "(Ljava/lang/Object;)V";
    InsnList il = new InsnList();
    il.add(new InsnNode(Opcodes.DUP));
    il.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC, CALLBACK_CLASS_NAME, callbackMethodName, signature, false));
    return il;
  }
  
  private boolean isSystemArrayCopy(AbstractInsnNode insn) {
    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
      MethodInsnNode invokeInsn = (MethodInsnNode) insn;
      if ("java/lang/System".equals(invokeInsn.owner) 
          && invokeInsn.name.equals("arraycopy")) {
        return true;
      }
    }
    return false;
  }
  
  private InsnList getSysArrayCopyProbingInstr() {
    InsnList insnList = new InsnList();
    insnList.add(new MethodInsnNode(Opcode.INVOKESTATIC, CALLBACK_CLASS_NAME, "systemArrayCopy", 
        "(Ljava/lang/Object;ILjava/lang/Object;II)Linstr/callback/ArrayCopyParams;", false));


    insnList.add(new InsnNode(Opcode.DUP));
    insnList.add(new FieldInsnNode(Opcode.GETFIELD, 
        "instr/callback/ArrayCopyParams", "src", "Ljava/lang/Object;"));
    insnList.add(new InsnNode(Opcode.SWAP));

    insnList.add(new InsnNode(Opcode.DUP));
    insnList.add(new FieldInsnNode(Opcode.GETFIELD, 
        "instr/callback/ArrayCopyParams", "srcPos", "I"));
    insnList.add(new InsnNode(Opcode.SWAP));

    insnList.add(new InsnNode(Opcode.DUP));
    insnList.add(new FieldInsnNode(Opcode.GETFIELD, 
        "instr/callback/ArrayCopyParams", "dest", "Ljava/lang/Object;"));
    insnList.add(new InsnNode(Opcode.SWAP));

    insnList.add(new InsnNode(Opcode.DUP));
    insnList.add(new FieldInsnNode(Opcode.GETFIELD, 
        "instr/callback/ArrayCopyParams", "destPos", "I"));
    insnList.add(new InsnNode(Opcode.SWAP));

    insnList.add(new FieldInsnNode(Opcode.GETFIELD, 
        "instr/callback/ArrayCopyParams", "length", "I"));
    return insnList;
  }
}
