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


package anonymous.domain.enlighten.slicing;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;
import anonymous.domain.enlighten.mcallrepr.JpfStateSnapshotter;
import anonymous.domain.enlighten.mcallrepr.MemberRefDepAnnotator;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.PrintDeps;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.util.DynDepViewer;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

public class SliceOnInvocationListener extends JPFSlicingListener {
  
  private static boolean printDeps = true;
  
  private SubjectProgram subject;
  private MethodName monitoredMethod;
  private int monitoredInvocIndex;
  
  private int monitoredMethodNextIndex;
  private List<Integer> monitoredMethodIndexStack = new ArrayList<>();
  private MethodCallRepr slicingResult;
  
  public SliceOnInvocationListener(SubjectProgram subjectProgram, 
      MethodName monitoredMethod, int monitoredInvocIndex) {
    subject = subjectProgram;
    this.monitoredMethod = monitoredMethod;
    this.monitoredInvocIndex = monitoredInvocIndex;
    monitoredMethodNextIndex = 0;
  }
  
  public MethodCallRepr getSlicingResult() {
    return slicingResult;
  }
  
  @Override
  public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
    MethodName methodName = JpfEntityConversion.getMethodNameFromMethodInfo(enteredMethod);
    if (monitoredMethod.equals(methodName)) {
      int currentInvocationIndex = monitoredMethodNextIndex++;
      monitoredMethodIndexStack.add(currentInvocationIndex);
      if (currentInvocationIndex == monitoredInvocIndex) {
        monitoredInvocationEntered(currentThread);
      }
    }
    super.methodEntered(vm, currentThread, enteredMethod);
    if (isDependencyTrackingStarted()) {
      MethodInvocationAttr frameAttr = 
          currentThread.getModifiableTopFrame().getFrameAttr(MethodInvocationAttr.class);
      if (enteredMethod.getClassName().startsWith(subject.getAppPackage() + ".")) {
        frameAttr.setGenInstrDep(true);
      } 
    }
  }
  
  @Override
  public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
    MethodName methodName = JpfEntityConversion.getMethodNameFromMethodInfo(exitedMethod);
    if (monitoredMethod.equals(methodName)) {
      int currentInvocIndex = monitoredMethodIndexStack.remove(
          monitoredMethodIndexStack.size() - 1);
      if (currentInvocIndex == monitoredInvocIndex) {
        monitoredInvocationExited(currentThread);
        vm.terminateProcess(currentThread);
      }
    }
  }

  protected void monitoredInvocationEntered(ThreadInfo ti) {
    startDependencyTracking();
  }

  protected void monitoredInvocationExited(ThreadInfo ti) {
    JpfStateSnapshotter snapshotter = new JpfStateSnapshotter();
    StackFrame methodStackFrame = ti.getModifiableTopFrame();
    slicingResult = snapshotter.fromStackFrame(ti, methodStackFrame);
    if (printDeps) {
      System.out.println(PrintDeps.toString(slicingResult));
    }
    DynamicDependency retDep = MemberRefDepAnnotator.getDependency(
        slicingResult, RefPath.newBuilder().appendReturnValueRef().build());
    DynDepViewer viewer = new DynDepViewer(retDep);
    try {
      Files.write(viewer.printAsDotFile().getBytes(), Paths.get("C:/Users/Xiangyuli/Desktop/debug/dep.dot").toFile());
    } catch (IOException e) {
      
      e.printStackTrace();
    }
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {}
}
