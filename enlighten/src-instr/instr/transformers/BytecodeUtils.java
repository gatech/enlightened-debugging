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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class BytecodeUtils {

  public static String getStandardName(ClassNode classNode) {
    String classNodeName = classNode.name;
    String standardName = classNodeName.replaceAll(
        Pattern.quote("/"), Matcher.quoteReplacement("."));
    return standardName.intern();
  }

  public static AbstractInsnNode getPushIntInsn(int value) {
    switch (value) {
    case -1:
      return new InsnNode(Opcodes.ICONST_M1);
    case 0:
      return new InsnNode(Opcodes.ICONST_0);
    case 1:
      return new InsnNode(Opcodes.ICONST_1);
    case 2:
      return new InsnNode(Opcodes.ICONST_2);
    case 3:
      return new InsnNode(Opcodes.ICONST_3);
    case 4:
      return new InsnNode(Opcodes.ICONST_4);
    case 5:
      return new InsnNode(Opcodes.ICONST_5);
    }
    if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      return new IntInsnNode(Opcodes.BIPUSH, value);
    }
    if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      return new IntInsnNode(Opcodes.SIPUSH, value);
    }
    return new LdcInsnNode(new Integer(value));
  }

  public static AbstractInsnNode getAutoboxInsn(Type valueType) {
    switch (valueType.getSort()) {
    case Type.BOOLEAN:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
    case Type.BYTE:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
    case Type.CHAR:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
    case Type.SHORT:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
    case Type.INT:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
    case Type.LONG:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
    case Type.FLOAT:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
    case Type.DOUBLE:
      return new MethodInsnNode(Opcodes.INVOKESTATIC, 
          "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
      default:
        throw new RuntimeException(
            "Cannot autobox non-primitive data types " + valueType.getDescriptor());
    }
  }

}
