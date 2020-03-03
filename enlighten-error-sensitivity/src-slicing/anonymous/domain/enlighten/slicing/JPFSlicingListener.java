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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencyListener;
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import javassist.NotFoundException;

public abstract class JPFSlicingListener 
    extends ListenerAdapter implements InstructionDependencyListener {
  
  private static final int EXECUTION_TIMEOUT_MILLIS = 60000; 

  private boolean isDependencyTrackingStarted;
  private DependencyAnalysis depAnalysis;
  private long timeoutMillis = -1;
  private boolean enableTimeout = true;
  
  public void setEnableTimeout(boolean enable) {
    enableTimeout = enable;
  }
  
  @Override
  public void classLoaded(VM vm, ClassInfo loadedClass) {
    if (timeoutMillis == -1) {
      timeoutMillis = System.currentTimeMillis() + EXECUTION_TIMEOUT_MILLIS;
    }
    if (loadedClass.isBuiltin()) {
      return;
    }
    if (loadedClass.getSuperClass() != null 
        && loadedClass.getSuperClass().getName().equals("gov.nasa.jpf.AnnotationProxyBase")) {

      return;
    }
    MethodInfo[] methods = loadedClass.getDeclaredMethodInfos();
    for (MethodInfo method : methods) {
      Instruction[] rawInstructions = method.getInstructions();
      if (rawInstructions == null || rawInstructions.length == 0) {
        continue;
      }
      DependencyTrackingInstruction[] instructions = 
          new DependencyTrackingInstruction[rawInstructions.length];
      System.arraycopy(rawInstructions, 0, instructions, 0, rawInstructions.length);
      MethodName methodName = MethodName.get(
          loadedClass.getName(), method.getName(), method.getSignature());
      ControlDependencyInfo cdInfo = null;
      try {
        cdInfo = getDependencyAnalysis(vm).getControlDependencyInfo(methodName);
      } catch (NotFoundException e) {
        e.printStackTrace();
        return;
      }
      Map<Integer, DependencyTrackingInstruction> posInstMap = new HashMap<>();
      for (DependencyTrackingInstruction inst : instructions) {
        posInstMap.put(inst.getPosition(), inst);
      }
      for (DependencyTrackingInstruction inst : instructions) {
        List<Integer> dependencies = cdInfo.getControlDependencies(inst.getPosition());
        for (Integer dependency : dependencies) {
          inst.addControlDependency(posInstMap.get(dependency));
        }
      }
      for (CatchBodyInfo handlerInfo : cdInfo.getExceptionHandlerInfo()) {
        DependencyTrackingInstruction handlerEntry = 
            posInstMap.get(handlerInfo.getEntryInstructionPosition());
        for (int handlerInstrPos : handlerInfo.getInstructionPositionRange()) {
          DependencyTrackingInstruction handlerInstr = posInstMap.get(handlerInstrPos);
          if (handlerInstr == null) {
            continue;
          }
          if (handlerInstr.getControlDependencyInstructions().isEmpty()) {




            handlerInstr.addControlDependency(handlerEntry);
          }



        }
      }
    }
  }
  
  @Override
  public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
    if (enableTimeout && System.currentTimeMillis() > timeoutMillis) {
      throw new RuntimeException("JPF execution time limit reached. Terminated.");
    }
    if (isDependencyTrackingStarted) {
      MethodInvocationAttr frameAttr = new MethodInvocationAttr();
      currentThread.getModifiableTopFrame().addFrameAttr(frameAttr);
    }
  }
  
  protected DependencyAnalysis getDependencyAnalysis(VM vm) {
    if (depAnalysis == null) {
      depAnalysis = new DependencyAnalysis();
      List<String> extraClasspaths = new ArrayList<>(
          Arrays.asList(vm.getConfig().getStringArray("classpath")));
      extraClasspaths.add(Paths.get(System.getProperty("java.home"), "lib", "rt.jar")
          .toAbsolutePath().toString());
      depAnalysis.appendExtraClasspath(extraClasspaths);
    }
    return depAnalysis;
  }
  
  protected void startDependencyTracking() {
    DynamicDependency.resetInstanceIndexCounter();
    isDependencyTrackingStarted = true;
  }
  
  protected boolean isDependencyTrackingStarted() {
    return isDependencyTrackingStarted;
  }
}
