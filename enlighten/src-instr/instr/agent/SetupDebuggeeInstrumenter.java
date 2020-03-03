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


package instr.agent;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import anonymous.domain.enlighten.data.MethodName;
import instr.callback.DebuggeeMethodListener;
import instr.transformers.SetupDebuggeeTransformer;

public class SetupDebuggeeInstrumenter implements ClassFileTransformer {
  
  private static boolean flagPrintInstrumentedCode = false;
  
  private String classInternalName;
  private String methodName;
  private String methodSig;
  
  public SetupDebuggeeInstrumenter(Instrumentation inst, InstrumenterConfig config) {
    setDebuggedInvocation(config.getDebuggedInvocation());
    inst.addTransformer(this, false);
  }

  @Override
  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) throws IllegalClassFormatException {
    if (this.classInternalName.equals(className)) {
      try {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassNode cnode = new ClassNode(Opcodes.ASM5);
        cr.accept(cnode, 0);
        new SetupDebuggeeTransformer(methodName, methodSig).transform(cnode);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
          @Override
          protected String getCommonSuperClass(String type1, String type2) {








            return "java/lang/Object";
          }
        };
        if (flagPrintInstrumentedCode) {
          TraceClassVisitor tracer = new TraceClassVisitor(cw, new PrintWriter(System.out));
          cnode.accept(tracer);
        } else {
          cnode.accept(cw);
        }
        return cw.toByteArray();
      } catch (Throwable ex) {
        ex.printStackTrace();
        throw ex;
      }
    } else {
      return null;
    }
  }

  private void setDebuggedInvocation(String invocationDesc) {
    int separatorIndex = invocationDesc.indexOf('#');
    MethodName fullName = MethodName.get(invocationDesc.substring(0, separatorIndex));
    classInternalName = fullName.getClassName().replace('.', '/');
    methodName = fullName.getMethodName();
    methodSig = fullName.getMethodSignature();
    int invocationIndex = 
        Integer.parseInt(invocationDesc.substring(separatorIndex + 1));
    DebuggeeMethodListener.setTargetInvocationIndex(invocationIndex);
  }
}
