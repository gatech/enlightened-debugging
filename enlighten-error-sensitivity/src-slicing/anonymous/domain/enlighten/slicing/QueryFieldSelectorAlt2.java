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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.FeedbackSet;
import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.util.Pair;

public class QueryFieldSelectorAlt2 {
  
  private SubjectProgram subject;
  private Map<TestName, ExecutionProfile> failingTests;
  private FeedbackSet feedbackSet;

  public QueryFieldSelectorAlt2(SubjectProgram subject, 
      Map<TestName, ExecutionProfile> failingTests, FeedbackSet feedbackSet) {
    this.subject = subject;
    if (failingTests.size() == 0) {
      throw new IllegalArgumentException(
          "Subject program must have at least one failing test.");
    }
    this.failingTests = failingTests;
    this.feedbackSet = feedbackSet;
    populateTestFailureDependencyIds();
  }

  public FieldSelectionResult selectFieldsForFeedback(
      FaultLocalization<SourceLocation> flResult) {
    TestName preferredTest = getPreferredTestExecution(flResult);
    ExecutionProfile testProfile = failingTests.get(preferredTest);
    FieldSelectionCriterionAlt2 selectionCriterion = new FieldSelectionCriterionAlt2();
    selectionCriterion.executionProfile = testProfile;
    selectionCriterion.flResults = flResult;
    selectionCriterion.correctValues = feedbackSet.getCorrectOutputValues();
    selectionCriterion.incorrectInputValues = feedbackSet.getIncorrectInputValues();
    selectionCriterion.incorrectOutputValues = feedbackSet.getIncorrectOutputValues();
    List<Set<Long>> incorrectValueRelevantDepIds = new ArrayList<>();
    for (Pair<MethodInvocationSelection, RefPath> incorrectValueKey : 
        feedbackSet.incorrectInputRelevantDeps.keySet()) {
      if (preferredTest.equals(incorrectValueKey.getFirst().getTestName())) {
        incorrectValueRelevantDepIds.add(
            feedbackSet.incorrectInputRelevantDeps.get(incorrectValueKey));
      }
    }
    for (Pair<MethodInvocationSelection, RefPath> incorrectValueKey : 
        feedbackSet.incorrectOutputRelevantDeps.keySet()) {
      if (preferredTest.equals(incorrectValueKey.getFirst().getTestName())) {
        incorrectValueRelevantDepIds.add(
            feedbackSet.incorrectOutputRelevantDeps.get(incorrectValueKey));
      }
    }
    selectionCriterion.incorrectValueRelevantDepIdList = incorrectValueRelevantDepIds;
    FieldSelectorAltListener2 selectorListener = 
       new FieldSelectorAltListener2(subject, selectionCriterion);
    SubjectProgramJPFRunner runner = new SubjectProgramJPFRunner(subject);
    runner.setJpfVMListener(selectorListener);
    runner.runTestMethod(preferredTest);
    MethodInvocation selectedInvocation = selectorListener.getSelectedInvocation();
    if (selectedInvocation == null) {
      return null;
    }
    TargetInvocInfo targetInvocInfo = new TargetInvocInfo();
    targetInvocInfo.targetExecutionProfile = testProfile;
    targetInvocInfo.targetInvocation = selectedInvocation;
    targetInvocInfo.flResults = flResult;
    targetInvocInfo.invocCorrectValues = feedbackSet.getCorrectOutputValues().get(selectedInvocation);
    targetInvocInfo.incorrectInputValues = feedbackSet.getIncorrectInputValues();
    targetInvocInfo.incorrectOutputValues = feedbackSet.getIncorrectOutputValues();
    targetInvocInfo.incorrectValueRelevantDepIdList = incorrectValueRelevantDepIds;
    InvocStatesInfoListener stateDetailsListener
        = new InvocStatesInfoListener(subject, targetInvocInfo);
    runner.setJpfVMListener(stateDetailsListener);
    runner.runTestMethod(preferredTest);
    int invocationIndex = testProfile.lookupInvocationIndex(selectedInvocation);
    FieldSelectionResult selectionResult = new FieldSelectionResult(
        new MethodInvocationSelection(
            preferredTest, selectedInvocation.getMethodName(), invocationIndex), 
        stateDetailsListener.getInvocationPreStates(),
        stateDetailsListener.getInvocationPostStates(), 
        stateDetailsListener.getOrderedSuspiciousValuePaths());
    return selectionResult;
  }
  
  private TestName getPreferredTestExecution(FaultLocalization<SourceLocation> flResult) {
    
    TestName shortestChoice = null;
    int numInvocs = Integer.MAX_VALUE;
    for (TestName failingTest : failingTests.keySet()) {
      ExecutionProfile profile = failingTests.get(failingTest);
      int length = getNumInvocs(profile.getInvocationTreeRoot());
      if (shortestChoice == null || numInvocs > length) {
        shortestChoice = failingTest;
        numInvocs = length;
      }
    }
    return shortestChoice;
  }
  
  private void populateTestFailureDependencyIds() {
    for (TestName failingTest : failingTests.keySet()) {
      ComputeFailureRelevantDeps listener = 
          new ComputeFailureRelevantDeps(subject, failingTests.get(failingTest));
      SubjectProgramJPFRunner runner = new SubjectProgramJPFRunner(subject);
      runner.setJpfVMListener(listener);
      runner.runTestMethod(failingTest);
      Set<Long> relevantDeps = listener.getRelevantDepIds();
      if (relevantDeps != null && relevantDeps.size() > 0) {
        Pair<MethodInvocationSelection, RefPath> exceptionRefKey = Pair.of(
            new MethodInvocationSelection(
                failingTest, listener.getTestMethodInvocation().getMethodName(), 0), 
            RefPath.newBuilder().appendExceptionRef().build());
        feedbackSet.incorrectOutputRelevantDeps.put(exceptionRefKey, relevantDeps);
      }
    }
  }
  
  private int getNumInvocs(MethodInvocation invoc) {
    if (invoc == null) {
      return 0;
    }
    int invocs = 1;
    for (MethodInvocation child : invoc.getEnclosedInvocations()) {
      invocs += getNumInvocs(child);
    }
    return invocs;
  }
}
