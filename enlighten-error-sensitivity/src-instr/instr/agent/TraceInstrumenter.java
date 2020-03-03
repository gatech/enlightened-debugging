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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import anonymous.domain.enlighten.data.MethodName;
import instr.callback.InstrumentationCallback;
import instr.transformers.MethodEntryExitTransformer;
import instr.transformers.WrapMethodTransformer;
import instr.transformers.LineNumberTransformer;
import instr.transformers.MemoryAccessTransformer;


public class TraceInstrumenter implements ClassFileTransformer {
  
  private static final Set<String> nonInstrumentableClasses = new HashSet<>();
  
  private static TraceInstrumenter instance;
  
  private Instrumentation inst;
  private InstrumenterConfig config;
  
  private WrapMethodTransformer wrapMethodTransformer = new WrapMethodTransformer();
  
  private boolean flagPrintClassNames = true;
  private boolean flagPrintInstrumentedCode = false;
  
  private String instrumentedPackage;
  private Map<ClassID, byte[]> originalClassFiles;
  
  static {
    nonInstrumentableClasses.add("java/lang/Boolean");
    nonInstrumentableClasses.add("java/lang/Byte");
    nonInstrumentableClasses.add("java/lang/Character");
    nonInstrumentableClasses.add("java/lang/Double");
    nonInstrumentableClasses.add("java/lang/Float");
    nonInstrumentableClasses.add("java/lang/Integer");
    nonInstrumentableClasses.add("java/lang/Long");
    nonInstrumentableClasses.add("java/lang/Number");
    nonInstrumentableClasses.add("java/lang/Short");
    nonInstrumentableClasses.add("java/lang/Void");
    nonInstrumentableClasses.add("java/lang/Class");
    nonInstrumentableClasses.add("java/lang/ClassLoader");
    nonInstrumentableClasses.add("java/lang/Object");
    nonInstrumentableClasses.add("java/lang/Thread");
  }
  
  public static TraceInstrumenter getInstance() {
    if (instance == null) {
      throw new RuntimeException("ClassInstrumenter has not been initialized.");
    }
    return instance;
  }
  
  protected TraceInstrumenter(Instrumentation inst, InstrumenterConfig config) {
    instance = this;
    this.inst = inst;
    setInstrumenterConfig(config);
    originalClassFiles = new HashMap<>();
    inst.addTransformer(this, true);
    reinstrumentAllClasses();
  }

  static int debugId = 0;
  
  @Override
  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) throws IllegalClassFormatException {
    if (className == null) {




      return null;
    }
    try {
      InstrumentationCallback.executionSwitchOut();
      byte[] result = null;
      ClassID classId = new ClassID(loader, className);
      if (originalClassFiles.containsKey(classId)) {
        classfileBuffer = originalClassFiles.get(classId);
        result = classfileBuffer;
      } else {
        byte[] classfileCopy = new byte[classfileBuffer.length];
        System.arraycopy(classfileBuffer, 0, classfileCopy, 0, classfileBuffer.length);
        originalClassFiles.put(classId, classfileCopy);
      }
      ClassNode cnode = null;
      
      if (config.instrumentMemoryAccess()) {
        if (isInstrumentable(className)) {
          ClassReader cr = new ClassReader(classfileBuffer);
          cnode = new ClassNode(Opcodes.ASM4);
          cr.accept(cnode, 0);
          if (flagPrintClassNames) {
            System.out.println("Instrumenting memory access of " + className + " ...");
          }
          new MemoryAccessTransformer().transform(cnode);
        }
      }
      
      if (className.startsWith(instrumentedPackage)) {
        if (cnode == null) {
          ClassReader cr = new ClassReader(classfileBuffer);
          cnode = new ClassNode(Opcodes.ASM4);
          cr.accept(cnode, 0);
        }
        if (wrapMethodTransformer.transform(cnode) && flagPrintClassNames) {
          System.out.println("Wrapped specified methods in class " + className);
        }
        if (config.instrumentLineNumber()) {
          if (flagPrintClassNames) {
            System.out.println("Instrumenting line number of " + className + " ...");
          }
          new LineNumberTransformer().transform(cnode);
        }
        if (flagPrintClassNames) {
          System.out.println("Instrumenting entries and exits of " + className + " ...");
        }
        MethodEntryExitTransformer entryExitTrans = new MethodEntryExitTransformer();
        if (config.instrumentStateCapture()) {
          entryExitTrans.setInstrumentStateCapture(true);
        } else {
          entryExitTrans.setInstrumentStateCapture(false);
        }
        entryExitTrans.transform(cnode);
      }
      
      if (cnode != null) {

        



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

        result = cw.toByteArray();
      }
      InstrumentationCallback.executionSwitchIn();
      return result;
    } catch (Throwable ex) {
      ex.printStackTrace();
      InstrumentationCallback.executionSwitchIn();
      throw ex;
    }
  }
  
  public InstrumenterConfig getInstrumenterConfig() {
    return config;
  }
  
  public void setInstrumenterConfig(InstrumenterConfig config) {
    this.config = config;
    instrumentedPackage = config.getInstrumentedPackage().replace('.', '/') + "/";
  }
  
  
  public void reinstrumentAllClasses() {
    List<Class<?>> classesToRetransform = new ArrayList<>();
    for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
      if(inst.isModifiableClass(loadedClass)) {
        if (isInstrumentable(loadedClass)) {
          classesToRetransform.add(loadedClass);
        }
      }
    }
    try {
      inst.retransformClasses(classesToRetransform.toArray(new Class<?>[0]));
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
  }
  
  

  public void reinstrumentClass(Class<?> classToReinstrument) {
    if (inst.isModifiableClass(classToReinstrument) 
        && isInstrumentable(classToReinstrument)) {
      try {
        inst.retransformClasses(classToReinstrument);
      } catch (Throwable ex) {
        ex.printStackTrace();
      }
    } else {
      System.err.println("Attempting to transform non-instrumentable class " 
          + classToReinstrument.getName());
    }
  }
  
  

  public void redefineClass(Class<?> classToRedefine, byte[] newDefinition) {
    if (inst.isModifiableClass(classToRedefine)
        && isInstrumentable(classToRedefine)) {
      String classInternalName = classToRedefine.getName().replace('.', '/');
      ClassID cid = new ClassID(classToRedefine.getClassLoader(), classInternalName);
      originalClassFiles.put(cid, newDefinition);
      try {
        inst.retransformClasses(classToRedefine);
      } catch (Throwable ex) {
        ex.printStackTrace();
      }
    } else {
      System.err.println("Attempting to redefine non-instrumentable class "
          + classToRedefine.getName());
    }
  }
  
  public void addWrappedMethod(MethodName methodToWrap) {
    String classInternalName = methodToWrap.getClassName().replace('.', '/');
    if (!classInternalName.startsWith(instrumentedPackage)) {
      throw new RuntimeException(
          "Method wrapping not supported on non-instrumented class " + methodToWrap.getClassName());
    }
    for (ClassID cachedClassId : originalClassFiles.keySet()) {
      if (cachedClassId.className.equals(classInternalName)) {
        throw new RuntimeException("Cannot add wrapped methods in classes that have been loaded.");
      }
    }
    wrapMethodTransformer.addMethodsToWrap(methodToWrap);
  }
  
  private static boolean isInstrumentable(String classInternalName) {
    if (classInternalName.startsWith("instr/")
        || classInternalName.startsWith("anonymous/domain/enlighten/")
        || classInternalName.startsWith("sun/")
        || classInternalName.startsWith("java/lang/ref")) {
      return false;
    } else if (nonInstrumentableClasses.contains(classInternalName)) {
      return false;
    }
    return true;
  }
  
  private static boolean isInstrumentable(Class<?> cls) {
    String className = cls.getName();
    if (className.startsWith("instr.") 
            || className.startsWith("anonymous.domain.enlighten.")) {
      return false;
    } else if (className.startsWith("sun.")) {


      return false;
    } else if (className.startsWith("java.lang.ref.")) {
      return false;
    } else if (cls == Boolean.class || cls == Byte.class || cls == Character.class 
        || cls == Double.class || cls == Float.class || cls == Integer.class 
        || cls == Long.class || cls == Number.class || cls == Short.class || cls == Void.class) {

      return false;
    } else if (cls == Class.class || cls == ClassLoader.class || cls == Object.class 
        || cls == Thread.class || cls == java.security.AccessControlContext.class) {


      


      return false;
    }
    return true;
  }
  
  private static final class ClassID {
    
    private ClassLoader loader;
    private String className;
    
    public ClassID(ClassLoader loader, String classInternalName) {
      this.loader = loader;
      this.className = classInternalName;
    }
    
    @Override
    public int hashCode() {
      return System.identityHashCode(loader) ^ className.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof ClassID) {
        ClassID another = (ClassID) o;
        return loader == another.loader && className.equals(another.className);
      }
      return false;
    }
  }
}
