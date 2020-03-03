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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

public class SetupDebuggeeTransformer {
  
  private String methodName;
  private String methodSig;
  
  public SetupDebuggeeTransformer(String methodName, String methodSig) {
    this.methodName = methodName;
    this.methodSig = methodSig;
  }

  @SuppressWarnings("unchecked")
  public void transform(ClassNode cn) {
    for (MethodNode methodNode : (List<MethodNode>) cn.methods) {
      if (methodName.equals(methodNode.name) && methodSig.equals(methodNode.desc)) {
        LabelNode methodEntry = new LabelNode();
        LabelNode methodEnd = new LabelNode();
        InsnList insnList = methodNode.instructions;
        LabelNode methodBodyStart = new LabelNode();
        insnList.insert(methodBodyStart);
        insnList.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "instr/callback/DebuggeeMethodListener", 
            "debuggeeMethodEntered", "()V", false));
        insnList.insert(methodEntry);
        Iterator<AbstractInsnNode> insnItr = insnList.iterator();
        while (insnItr.hasNext()) {
          AbstractInsnNode insn = insnItr.next();
          int opCode = insn.getOpcode();
          if (opCode >= Opcodes.IRETURN && opCode <= Opcodes.RETURN) {
            insnList.insertBefore(insn, new MethodInsnNode(
                Opcodes.INVOKESTATIC, "instr/callback/DebuggeeMethodListener",
                "trapTargetInvocation", "()V", false));
          }
        }
        LabelNode methodBodyEnd = new LabelNode();
        insnList.add(methodBodyEnd);
        InsnList handlerCode = new InsnList();
        LabelNode handlerStart = new LabelNode();
        handlerCode.add(handlerStart);
        handlerCode.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC, "instr/callback/DebuggeeMethodListener",
            "trapTargetInvocation", "()V", false));
        handlerCode.add(new InsnNode(Opcodes.ATHROW));
        insnList.add(handlerCode);
        insnList.add(methodEnd);
        TryCatchBlockNode tryCatch = new TryCatchBlockNode(
            methodBodyStart, methodBodyEnd, handlerStart, null);
        methodNode.tryCatchBlocks.add(tryCatch);


        Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
        int argumentSlots = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (Type argumentType : argumentTypes) {
          argumentSlots += argumentType.getSize();
        }
        for (LocalVariableNode local : (List<LocalVariableNode>) methodNode.localVariables) {
          if (local.index >= 0 && local.index < argumentSlots) {
            local.start = methodEntry;
            local.end = methodEnd;
          }
        }
      }
    }
  }
}
