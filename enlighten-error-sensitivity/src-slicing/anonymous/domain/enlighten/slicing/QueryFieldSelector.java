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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.util.FloatComparison;
import anonymous.domain.enlighten.util.Pair;

public class QueryFieldSelector {
  
  private SubjectProgram subject;
  private List<Pair<TestName, ExecutionProfile>> failingTestProfiles;
  

  private Map<TestName, Set<SourceLocation>> excludedSourceLocations = new HashMap<>();
  private Map<MethodInvocation, Set<RefPath>> answeredFields = new HashMap<>();
  private Map<MethodInvocation, Set<RefPath>> invalidPreStates = new HashMap<>();
  
  private Map<TestName, Set<MethodInvocation>> relevantInvocations = new HashMap<>();
  
  public QueryFieldSelector(SubjectProgram subject, 
      List<Pair<TestName, ExecutionProfile>> failingTestProfiles) {
    if (failingTestProfiles.size() == 0) {
      throw new IllegalArgumentException(
          "Subject program must have at least one failing test.");
    }
    this.subject = subject;
    this.failingTestProfiles = failingTestProfiles;
    for (Pair<TestName, ExecutionProfile> testInfo : failingTestProfiles) {
      excludedSourceLocations.put(testInfo.getFirst(), new HashSet<>());
    }
    computeRelevantInvocations();
  }
  
  public FieldSelectionResult selectFieldsForFeedback(
      FaultLocalization<SourceLocation> flResult) {
    TestName selectedTest = null;
    ExecutionProfile selectedTestProfile = null;
    MethodInvocation selectedInvocation = null;
    MethodCallRepr selectedInvocPostStates = null;
    List<Pair<RefPath, Double>> selectedFields = null;
    List<SourceLocation> flRankedList = new ArrayList<>(flResult.getRankedList());
    while (selectedInvocation == null) {
      if (flRankedList.size() == 0) {
        return null;
      }

      List<SourceLocation> mostSuspiciousStmts = new ArrayList<>();
      double highestSuspiciousness = flResult.getSuspiciousness(flRankedList.get(0));
      for (SourceLocation stmt : flRankedList) {
        if (FloatComparison.compareDouble(
            flResult.getSuspiciousness(stmt), highestSuspiciousness) >= 0) {
          mostSuspiciousStmts.add(stmt);
        } else {
          break;
        }
      }


      Map<MethodName, List<SourceLocation>> methodSrcLocMap = null;
      Map<MethodName, Set<MethodInvocation>> suspInvocationsMap = null;
      for (Pair<TestName, ExecutionProfile> testInfo : failingTestProfiles) {
        selectedTest = testInfo.getFirst();
        selectedTestProfile = testInfo.getSecond();
        methodSrcLocMap = new HashMap<>();
        suspInvocationsMap = new HashMap<>();
        Set<SourceLocation> testExcludedLocs = excludedSourceLocations.get(selectedTest);
        for (SourceLocation suspStmt : mostSuspiciousStmts) {
          if (testExcludedLocs.contains(suspStmt)) {



            continue;
          }
          List<MethodInvocation> containingInvocs = new ArrayList<>(
              selectedTestProfile.lookupInvocation(suspStmt));
          for (MethodInvocation excluded : invalidPreStates.keySet()) {

            containingInvocs.remove(excluded);
          }
          if (containingInvocs != null && containingInvocs.size() > 0) {
            MethodName method = containingInvocs.get(0).getMethodName();
            List<SourceLocation> suspStmtGroup = methodSrcLocMap.get(method);
            if (suspStmtGroup == null) {
              suspStmtGroup = new ArrayList<>();
              methodSrcLocMap.put(method, suspStmtGroup);
              suspInvocationsMap.put(method, new HashSet<>());
            }
            suspStmtGroup.add(suspStmt);
            suspInvocationsMap.get(method).addAll(containingInvocs);
          }
        }
        if (methodSrcLocMap.size() > 0) {
          break;
        }
      }
      if (methodSrcLocMap.size() == 0) {


        flRankedList.removeAll(mostSuspiciousStmts);
        continue;
      }

      MethodName preSelectedMethod = methodSrcLocMap.keySet().iterator().next();
      List<SourceLocation> criterionStatements = methodSrcLocMap.get(preSelectedMethod);
      Set<MethodInvocation> preSelectedInvocations = suspInvocationsMap.get(preSelectedMethod);
      FieldSelectionCriterion selectionCriterion = new FieldSelectionCriterion();
      selectionCriterion.executionProfile = selectedTestProfile;
      selectionCriterion.invocations = preSelectedInvocations;
      selectionCriterion.statements = new HashSet<>(criterionStatements);
      selectionCriterion.flResults = flResult;
      selectionCriterion.answeredFields = answeredFields;
      FieldSelectorListener selectionListener = 
          new FieldSelectorListener(subject, selectionCriterion);
      SubjectProgramJPFRunner runner = new SubjectProgramJPFRunner(subject);
      runner.setJpfVMListener(selectionListener);
      runner.runTestMethod(selectedTest);
      selectedInvocation = selectionListener.getSelectedInvocation();
      if (selectedInvocation == null) {






        excludedSourceLocations.get(selectedTest).addAll(criterionStatements);
        continue;
      }
      selectedInvocPostStates = selectionListener.getSelectedInvocationPostStates();
      selectedFields = selectionListener.getOrderedSuspiciousFields();
    }
    MethodInvocationSelection invocSelection = new MethodInvocationSelection(
        selectedTest, selectedInvocation.getMethodName(), 
        selectedTestProfile.lookupInvocationIndex(selectedInvocation));
    FieldSelectionResult selectionResult = new FieldSelectionResult(
        invocSelection, selectedInvocPostStates, selectedFields);
    return selectionResult;
  }
  
  public void addAnsweredField(MethodInvocation invocation, RefPath fieldPath) {
    Set<RefPath> invocAnsweredFields = answeredFields.get(invocation);
    if (invocAnsweredFields == null) {
      invocAnsweredFields = new HashSet<>();
      answeredFields.put(invocation, invocAnsweredFields);
    }
    invocAnsweredFields.add(fieldPath);
  }
  
  public void addInvalidPreState(MethodInvocation invocation, RefPath fieldPath) {
    
    Set<RefPath> invocInvalidPreState = invalidPreStates.get(invocation);
    if (invocInvalidPreState == null) {
      invocInvalidPreState = new HashSet<>();
      invalidPreStates.put(invocation, invocInvalidPreState);
    }
    invocInvalidPreState.add(fieldPath);
  }
  
  public Map<TestName, Set<MethodInvocation>> getRelevantInvocations() {
    return relevantInvocations;
  }
  


  private void computeRelevantInvocations() {
    for (Pair<TestName, ExecutionProfile> testInfo : failingTestProfiles) {
      backSlice(testInfo.getFirst(), testInfo.getSecond());
    }
  }
  
  private void backSlice(TestName testName, ExecutionProfile profile) {
    SubjectProgramJPFRunner runner = new SubjectProgramJPFRunner(subject);
    ComputeFailureRelevantInvocation listener = 
        new ComputeFailureRelevantInvocation(subject, profile);
    runner.setJpfVMListener(listener);
    runner.runTestMethod(testName);
    relevantInvocations.put(testName, listener.getRelevantInvocations());
  }
}
