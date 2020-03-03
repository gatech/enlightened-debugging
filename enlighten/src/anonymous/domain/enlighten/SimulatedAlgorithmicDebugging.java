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


package anonymous.domain.enlighten;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationExecutionCount;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.exec.CaptureInvocationStates;
import anonymous.domain.enlighten.legacy.UserPoweredFLBase;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.util.ModifiablePair;
import gov.nasa.jpf.vm.Types;

public class SimulatedAlgorithmicDebugging extends UserPoweredFLBase {
  
  private static final int MAX_QUERIES = 100;

  private MethodInvocation currentShortestFaultyInvocation;
  private TestName parentTestName;
  private ExecutionProfile parentExecProfile;
  private Set<MethodInvocation> clearedInvocations;
  private Map<MethodInvocation, Integer> invocationUnclearedLengthMap;
  private boolean isDone = false;

  private List<SourceLocation> faultySourceLocations;
  private CaptureInvocationStates captureStates;
  private CaptureInvocationStates captureStatesOnRef;
  private MethodInvocation selectedInvocationStates;
  private MethodInvocation selectedInvocationStatesOnRef;
  
  private int numQueries = 0;
  private List<QueryStats> queryStatsList = new ArrayList<>();
  
  public SimulatedAlgorithmicDebugging(SubjectProgram targetProgram,
      SubjectProgram goldenVersion, boolean forceGenData) {
    super(targetProgram, forceGenData, goldenVersion);
    clearedInvocations = Collections.newSetFromMap(
        new IdentityHashMap<MethodInvocation, Boolean>());
    invocationUnclearedLengthMap = new IdentityHashMap<>();
    int shortestExecLength = Integer.MAX_VALUE;
    for (MethodInvocation incorrectTestExecution : knownIncorrectExecutions.keySet()) {
      int invocationLength = getInvocationUnclearedLength(incorrectTestExecution);
      if (invocationLength < shortestExecLength) {
        shortestExecLength = invocationLength;
        currentShortestFaultyInvocation = incorrectTestExecution;
      }
    }
    parentTestName = knownIncorrectExecutions.get(currentShortestFaultyInvocation);
    parentExecProfile = getTestExecutionProfile(parentTestName);
    faultySourceLocations = targetProgram.getFaultySourceLocations();
    if (faultySourceLocations == null || faultySourceLocations.size() == 0) {
      throw new RuntimeException(
          "Faulty source location information is not configured in subject program at " 
              + targetProgram.getRootDir());
    }
    try {
      captureStates = new CaptureInvocationStates(targetProgram);
      captureStates.setProfileMemoryAccess(false);
      captureStatesOnRef = new CaptureInvocationStates(targetProgram, goldenVersion);
      captureStatesOnRef.setProfileMemoryAccess(false);
    } catch (IOException ex) {
      throw new RuntimeException("Unable to create the workspace folder for state capturing.", ex);
    }
  }
  
  public int getNumQueriesPerformed() {
    return numQueries;
  }
  
  public List<QueryStats> getQueryStats() {
    return Collections.unmodifiableList(queryStatsList);
  }

  @Override
  protected boolean isDone() {
    return isDone;
  }

  protected MethodInvocationSelection selectMethodInvocationForUserFeedback() {
    double targetLength = 
        (double)getInvocationUnclearedLength(currentShortestFaultyInvocation) / 2;
    ModifiablePair<MethodInvocation, Double> result = 
        ModifiablePair.of(null, Double.POSITIVE_INFINITY);
    selectInvocationInternal(targetLength, currentShortestFaultyInvocation, result);
    MethodInvocation selectedInvoc = result.getFirst();
    int selectedInvocIndex = parentExecProfile.lookupInvocationIndex(selectedInvoc);
    try {
      selectedInvocationStates = captureStates.getInvocationStates(
          parentTestName, selectedInvoc.getMethodName(), selectedInvocIndex);
      selectedInvocationStatesOnRef = captureStatesOnRef.getInvocationStates(
          parentTestName, selectedInvoc.getMethodName(), selectedInvocIndex);
    } catch (IOException ex) {
      throw new RuntimeException("Unable to capture invocation states", ex);
    }
    return new MethodInvocationSelection(
        parentTestName, selectedInvoc.getMethodName(), selectedInvocIndex);
  }
  
  @Override
  protected MethodCallRepr getMethodInvocationPreState(MethodInvocationSelection invocation) {
    return selectedInvocationStates.getPreState();
  }

  @Override
  protected MethodCallRepr getMethodInvocationPostState(MethodInvocationSelection invocation) {
    return selectedInvocationStates.getPostState();
  }
  
  @Override
  protected UserFeedback getUserFeedback(MethodInvocationSelection invocation,
      MethodCallRepr prestate, MethodCallRepr poststate) {
    MethodInvocation invocationNode = getTestExecutionProfile(invocation.getTestName())
        .lookupInvocation(invocation.getMethodName(), invocation.getInvocationIndex());
    ++numQueries;
    int memRead = invocationNode.getNumMemoryReadLocations() + Types.getArgumentTypes(
        invocationNode.getMethodName().getMethodSignature()).length + 1;
    int memWrite = invocationNode.getNumMemoryWriteLocations() + 1;
    QueryStats queryStats = new QueryStats(invocation, memRead, memWrite);
    queryStatsList.add(queryStats);
    SourceLocationExecutionCount unclearedExecCount = 
        getSourceLocationExecutionCount(invocationNode, true);
    boolean containsNoFault = true;
    for (SourceLocation faultyLocation : faultySourceLocations) {
      if (unclearedExecCount.getExecutionCount(faultyLocation) != 0) {
        containsNoFault = false;
        break;
      }
    }
    if (containsNoFault) {
      queryStats.feedback = UserFeedback.CORRECT;
      return UserFeedback.CORRECT;
    }
    MethodCallRepr preStatesOnTarget = (MethodCallRepr) prestate;
    MethodCallRepr postStatesOnTarget = (MethodCallRepr) poststate;
    MethodCallRepr preStatesOnRef = (MethodCallRepr) selectedInvocationStatesOnRef.getPreState();
    MethodCallRepr postStatesOnRef = (MethodCallRepr) selectedInvocationStatesOnRef.getPostState();
    if (preStatesOnTarget.getValueHash() != preStatesOnRef.getValueHash()) {
      throw new RuntimeException("Pre-states do not match between the faulty and reference"
          + " implementation. Could not run on this subject.");
    }
    if (postStatesOnTarget.getValueHash() == postStatesOnRef.getValueHash()) {
      queryStats.feedback = UserFeedback.CORRECT;
      return UserFeedback.CORRECT;
    } else {
      queryStats.feedback = UserFeedback.INCORRECT;
      return UserFeedback.INCORRECT;
    }
  }

  private int getInvocationUnclearedLength(MethodInvocation invocation) {
    if (invocationUnclearedLengthMap.containsKey(invocation)) {
      return invocationUnclearedLengthMap.get(invocation);
    }
    if (isCleared(invocation)) {
      return 0;
    }
    int length = 1;
    for (MethodInvocation enclosedInvoc : invocation.getEnclosedInvocations()) {
      length += getInvocationUnclearedLength(enclosedInvoc);
    }
    return length;
  }
  
  protected void incorporateUserFeedback(
      MethodInvocationSelection invocation, UserFeedback feedback) {
    MethodInvocation queriedInvoc = parentExecProfile.lookupInvocation(
          invocation.getMethodName(), invocation.getInvocationIndex());
    if (feedback == UserFeedback.INCORRECT) {
      currentShortestFaultyInvocation = queriedInvoc;
      Map<SourceLocation, Integer> directlyCoveredStmts = 
          queriedInvoc.getStatementsExecCountMap();
      boolean containsFault = false;
      for (SourceLocation fault : faultySourceLocations) {
        if (directlyCoveredStmts.containsKey(fault)) {
          containsFault = true;
          break;
        }
      }
      if (containsFault) {
        isDone = true;
      }
    } else if (feedback == UserFeedback.CORRECT) {
      markAsCleared(queriedInvoc);
    }
    if (numQueries > MAX_QUERIES) {
      isDone = true;
    }
  }
  


  private void selectInvocationInternal(double targetLength,
      MethodInvocation invocation, ModifiablePair<MethodInvocation, Double> result) {
    int invocationLength = getInvocationUnclearedLength(invocation);
    double lengthDifference = Math.abs(targetLength - invocationLength);
    if (lengthDifference < result.getSecond()) {
      result.setFirst(invocation);
      result.setSecond(lengthDifference);
    }
    if (invocationLength <= targetLength) {
      return;
    }
    for (MethodInvocation enclosingInvoc : invocation.getEnclosedInvocations()) {
      if (!isCleared(enclosingInvoc)) {
        selectInvocationInternal(targetLength, enclosingInvoc, result);
      }
    }
  }
  

  private boolean isCleared(MethodInvocation invocation) {
    MethodInvocation current = invocation;
    while (current != null && current != currentShortestFaultyInvocation) {
      if (clearedInvocations.contains(invocation)) {
        return true;
      }
      current = current.getEnclosingInvocation();
    }
    return false;
  }
  
  private void markAsCleared(MethodInvocation invocation) {
    clearedInvocations.add(invocation);
    int unclearedLength = getInvocationUnclearedLength(invocation);
    MethodInvocation current = invocation;
    while (current != null) {
      if (invocationUnclearedLengthMap.containsKey(invocation)) {
        invocationUnclearedLengthMap.put(
            invocation, invocationUnclearedLengthMap.get(invocation) - unclearedLength);
      }
      current = current.getEnclosingInvocation();
    }
  }
  
  public static class QueryStats implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public MethodInvocationSelection invocation;
    public int memRead;
    public int memWrite;
    public UserFeedback feedback;
    
    public QueryStats(MethodInvocationSelection invocation, int memRead, int memWrite) {
      this.invocation = invocation;
      this.memRead = memRead;
      this.memWrite = memWrite;
    }
  }
}
