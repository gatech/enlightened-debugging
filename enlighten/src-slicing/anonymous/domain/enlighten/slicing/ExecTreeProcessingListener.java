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

import java.util.ArrayDeque;
import java.util.Deque;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

public abstract class ExecTreeProcessingListener extends JPFSlicingListener {
  
  private SubjectProgram subject;
  private ExecutionProfile executionProfile;
  private boolean requireDeterministicExecution = false;
  
  private MethodInvocation currentInvocation;
  private Deque<Integer> nextChildIndices = new ArrayDeque<>();
  private boolean listenerDisabled;  
  private ThreadInfo testExecutionThread;
  
  private MethodInvocation testMethodInvocation;
  private RangeMap<Long, MethodInvocation> depIndexInvocMap;
  
  public ExecTreeProcessingListener(SubjectProgram subject, ExecutionProfile profile) {
    this.subject = subject;
    executionProfile = profile;
    testMethodInvocation = searchForTestMethodInvocation(profile);
    currentInvocation = profile.getInvocationTreeRoot();
    nextChildIndices.push(0);
  }
  
  public void setRequireDeterministicExecution(boolean requires) {
    requireDeterministicExecution = requires;
  }
  
  public boolean requireDeterministicExecution() {
    return requireDeterministicExecution;
  }
  
  public SubjectProgram getSubjectProgram() {
    return subject;
  }
  
  public MethodInvocation getTestMethodInvocation() {
    return testMethodInvocation;
  }
  
  protected ExecutionProfile getExecutionProfile() {
    return executionProfile;
  }
  
  protected void disableListener() {
    listenerDisabled = true;
  }
  
  protected abstract void invocationEntered(
      MethodInvocation enteredInvocation, ThreadInfo currentThread);
  
  protected abstract void invocationExited(
      MethodInvocation exitedInvocation, ThreadInfo currentThread);
  
  protected MethodInvocation getCurrentInvocation() {
    return currentInvocation;
  }
  
  protected MethodInvocation lookupDependencyGeneratingInvocation(long depInstanceIndex) {
    if (!listenerDisabled) {
      return searchForContainingNode(executionProfile.getInvocationTreeRoot(), depInstanceIndex);
    }
    if (depIndexInvocMap == null) {
      buildDepIndexInvocationMap();
    }
    return depIndexInvocMap.get(depInstanceIndex);
  }

  @Override
  public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
    if (!isDependencyTrackingStarted()) {
      if (isStartingInvocation(enteredMethod)) {
        testExecutionThread = currentThread;
        startDependencyTracking();
      }
    }
    super.methodEntered(vm, currentThread, enteredMethod);
    if (isDependencyTrackingStarted()) {
      MethodInvocationAttr frameAttr = 
          currentThread.getModifiableTopFrame().getFrameAttr(MethodInvocationAttr.class);
      if (isSubjectApplicationMethod(enteredMethod)) {
        if (currentThread != testExecutionThread) {
          throw new RuntimeException("The implementation does not support multithreading yet.");
        }
        frameAttr.setGenInstrDep(true);
        if (!enteredMethod.isSynthetic()) {
          int currentChildIndex = nextChildIndices.pop();
          currentInvocation = 
              currentInvocation.getEnclosedInvocations().get(currentChildIndex);
          nextChildIndices.push(currentChildIndex + 1);
          nextChildIndices.push(0);

          MethodName actualMethodName = 
              JpfEntityConversion.getMethodNameFromMethodInfo(enteredMethod);
          if (!currentInvocation.getMethodName().equals(actualMethodName)) {
            System.err.println(
                "JPF execution of the test differs from its recorded execution profile.");
            System.err.println("Subject: " + subject.getRootDir());
            System.err.println("Test method: " + executionProfile.getExecutionId());
            System.err.println("Expected entered method: " + currentInvocation.getMethodName());
            System.err.println("But was: " + actualMethodName);
            throw new RuntimeException("JPF execution differs from recorded execution profile");
          }

          long invocationDepStartIndex = DynamicDependency.getNextInstanceIndex();
          if (requireDeterministicExecution) {
            long previousStartIndex = DepIndexRangeAnnotator.getStartIndex(currentInvocation);
            if (previousStartIndex != -1 && previousStartIndex != invocationDepStartIndex) {
              throw new RuntimeException(
                    "Required deterministic execution but found indeterminism.");
            }
          }
          DepIndexRangeAnnotator.setStartIndex(currentInvocation, invocationDepStartIndex);
          invocationEntered(currentInvocation, currentThread);
        }
      }
    }
  }
  
  @Override
  public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
    if (isDependencyTrackingStarted() && !listenerDisabled) {
      if (isSubjectApplicationMethod(exitedMethod) && !exitedMethod.isSynthetic()
          && !exitedMethod.isDirectCallStub()) {
        if (currentInvocation == getExecutionProfile().getInvocationTreeRoot()) {
          disableListener();
          return;
        }

        MethodName actualMethodName = 
            JpfEntityConversion.getMethodNameFromMethodInfo(exitedMethod);
        if (!currentInvocation.getMethodName().equals(actualMethodName)) {
          System.err.println(
              "JPF execution of the test differs from its recorded execution profile.");
          System.err.println("Subject: " + subject.getRootDir());
          System.err.println("Test method: " + executionProfile.getExecutionId());
          System.err.println("Expected exited method: " + currentInvocation.getMethodName());
          System.err.println("But was: " + actualMethodName);
          throw new RuntimeException("JPF execution differs from recorded execution profile");
        }

        long invocationDepEndIndex = DynamicDependency.getNextInstanceIndex() - 1;
        if (requireDeterministicExecution) {
          long previousEndIndex = DepIndexRangeAnnotator.getEndIndex(currentInvocation);
          if (previousEndIndex != -1 && previousEndIndex != invocationDepEndIndex) {
            throw new RuntimeException(
                "Required deterministic execution but found indeterminism.");
          }
        }
        DepIndexRangeAnnotator.setEndIndex(
            currentInvocation, invocationDepEndIndex);
        invocationExited(currentInvocation, currentThread);
        nextChildIndices.pop();
        currentInvocation = currentInvocation.getEnclosingInvocation();
      }
    }
  }
  
  private boolean isSubjectApplicationMethod(MethodInfo methodInfo) {
    return methodInfo.getClassName().startsWith(subject.getAppPackage() + ".");
  }
  
  private boolean isStartingInvocation(MethodInfo methodInfo) {
    MethodName startingMethodName = 
        executionProfile.getInvocationTreeRoot().getEnclosedInvocations().get(0).getMethodName();
    return startingMethodName.equals(JpfEntityConversion.getMethodNameFromMethodInfo(methodInfo));
  }
  
  private void buildDepIndexInvocationMap() {
    if (!listenerDisabled) {
      throw new RuntimeException("Should not build dep-index to invocations map before the listener stops.");
    }
    depIndexInvocMap = TreeRangeMap.create();
    addRangesRecursively(getExecutionProfile().getInvocationTreeRoot());
  }
  
  private void addRangesRecursively(MethodInvocation invocation) {


    long rangeStart = DepIndexRangeAnnotator.getStartIndex(invocation);
    long rangeEnd = -1;
    for (MethodInvocation child : invocation.getEnclosedInvocations()) {
      rangeEnd = DepIndexRangeAnnotator.getStartIndex(child) - 1;
      if (rangeEnd >= rangeStart) {
        depIndexInvocMap.put(Range.closed(rangeStart, rangeEnd), invocation);

      }
      rangeStart = DepIndexRangeAnnotator.getEndIndex(child) + 1;
    }
    rangeEnd = DepIndexRangeAnnotator.getEndIndex(invocation);
    if (rangeEnd >= rangeStart) {
      depIndexInvocMap.put(Range.closed(rangeStart, rangeEnd), invocation);

    }

    for (MethodInvocation child : invocation.getEnclosedInvocations()) {
      addRangesRecursively(child);
    }
  }
  
  private MethodInvocation searchForContainingNode(
      MethodInvocation invocTreeNode, long depInstanceIndex) {
    for (MethodInvocation childInvoc : invocTreeNode.getEnclosedInvocations()) {
      long startIndex = DepIndexRangeAnnotator.getStartIndex(childInvoc);
      if (startIndex == -1 || startIndex > depInstanceIndex) {
        break;
      }
      long endIndex = DepIndexRangeAnnotator.getEndIndex(childInvoc);
      if (endIndex == -1 || endIndex >= depInstanceIndex) {
        return searchForContainingNode(childInvoc, depInstanceIndex);
      }
    }
    return invocTreeNode;
  }
  
  private MethodInvocation searchForTestMethodInvocation(ExecutionProfile profile) {
    TestName testName = TestName.parseFromDescription(profile.getExecutionId());
    return searchForTestMethodInvocationRecursively(profile.getInvocationTreeRoot(), testName.getTestMethodName());
  }
  
  private MethodInvocation searchForTestMethodInvocationRecursively(
      MethodInvocation root, String methodName) {
    if (!root.getMethodName().getClassName().equals("ExecStart") 
        && root.getMethodName().getMethodName().equals(methodName)) {
      return root;
    }
    for (MethodInvocation child : root.getEnclosedInvocations()) {
      MethodInvocation searchResult = searchForTestMethodInvocationRecursively(child, methodName);
      if (searchResult != null) {
        return searchResult;
      }
    }
    return null;
  }
}
