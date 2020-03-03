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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import anonymous.domain.enlighten.data.SourceLocation;
import instr.staticinfo.SourceLocationDB;

public class LineNumberTransformer {
  
  private static final int INSTRUMENT_LINE_METHOD_SIZE_LIMIT = 5000;
  
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


      if (insns.size() >= INSTRUMENT_LINE_METHOD_SIZE_LIMIT) {
        System.err.println("Warning: method body of " 
            + cn.name + "." + mn.name + mn.desc + " is too large.\n"
            + "Skipping instrumenting line number nodes on this method.");
        continue;
      }
      Iterator<AbstractInsnNode> j = insns.iterator();
      while (j.hasNext()) {
        AbstractInsnNode currentInsn = j.next();
        if (currentInsn instanceof LineNumberNode) {
          int ln = ((LineNumberNode) currentInsn).line;
          AbstractInsnNode insertingPoint = getNextInsertingPoint(currentInsn);
          insns.insertBefore(insertingPoint, getNotifyLineInstr(getSourcePath(cn), ln));
        }
      }
    }
  }

  private InsnList getNotifyLineInstr(String sourcePath, int lineNumber) {
    InsnList il = new InsnList();
    int sourceLocationId = -1;
    SourceLocation sourceLocation = SourceLocation.get(sourcePath, lineNumber);
    if (SourceLocationDB.contains(sourceLocation)) {
      sourceLocationId = SourceLocationDB.getSourceLocationId(sourceLocation);
    } else {
      sourceLocationId = SourceLocationDB.addSourceLocation(sourceLocation);
    }
    il.add(BytecodeUtils.getPushIntInsn(sourceLocationId));
    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "instr/callback/InstrumentationCallback", 
        "executeSourceLocation", "(I)V", false));
    return il;
  }
  
  private AbstractInsnNode getNextInsertingPoint(AbstractInsnNode start) {
    AbstractInsnNode nextInsertingPoint = start.getNext();
    while (nextInsertingPoint != null && ((nextInsertingPoint instanceof LineNumberNode) 
        || (nextInsertingPoint instanceof FrameNode) || (nextInsertingPoint instanceof LabelNode))) {
      nextInsertingPoint = nextInsertingPoint.getNext();
    }
    if (nextInsertingPoint == null) {
      throw new RuntimeException("No next relevant instruction found.");
    }
    return nextInsertingPoint;
  }
  
  private static String getSourcePath(ClassNode classNode) {
    String classNodeName = classNode.name;
    String sourcePath = classNodeName.substring(0, classNodeName.lastIndexOf('/') + 1)
        + classNode.sourceFile;
    return sourcePath.intern();
  }
}
