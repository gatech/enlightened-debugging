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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.util.Pair;

public class FeedbackSet {

  private Map<MethodInvocation, Set<RefPath>> correctOutputValues = new HashMap<>();
  private Map<Pair<MethodInvocation, RefPath>, Set<SourceLocation>> passingVTestsCov = 
      new HashMap<>();
  
  private Map<MethodInvocation, Set<RefPath>> incorrectOutputValues = new HashMap<>();
  private Map<MethodInvocation, Set<SourceLocation>> incorrectInvocationCov = new HashMap<>();
  private Map<Pair<MethodInvocation, RefPath>, Set<SourceLocation>> correctDirectCov = 
      new HashMap<>();
  private Map<Pair<MethodInvocation, RefPath>, Set<SourceLocation>> likelyCorrectDirectCov = 
      new HashMap<>();
  public Map<Pair<MethodInvocationSelection, RefPath>, Set<Long>> incorrectOutputRelevantDeps =
      new HashMap<>();
  
  private Map<MethodInvocation, Set<RefPath>> incorrectInputValues = new HashMap<>();
  public Map<Pair<MethodInvocationSelection, RefPath>, Set<Long>> incorrectInputRelevantDeps =
      new HashMap<>();
  
  public void addCorrectOutputValue(MethodInvocation invocation, RefPath refPath, 
      Set<SourceLocation> vTestCov) {
    removeIncorrectOutputValue(invocation, refPath);
    Set<RefPath> paths = correctOutputValues.get(invocation);
    if (paths == null) {
      paths = new HashSet<>();
      correctOutputValues.put(invocation, paths);
    }
    paths.add(refPath);
    Pair<MethodInvocation, RefPath> feedbackLocKey = Pair.of(invocation, refPath);
    passingVTestsCov.put(feedbackLocKey, vTestCov);
  }
  
  public void addIncorrectOutputValue(MethodInvocation invocation, RefPath refPath, 
      Set<SourceLocation> fullCov, Set<SourceLocation> relevantDrctCov, 
      boolean confirmDirCovCorrect) {
    removeCorrectOutputValue(invocation, refPath);
    Set<RefPath> paths = incorrectOutputValues.get(invocation);
    if (paths == null) {
      paths = new HashSet<>();
      incorrectOutputValues.put(invocation, paths);
    }
    paths.add(refPath);
    incorrectInvocationCov.put(invocation, fullCov);
    Pair<MethodInvocation, RefPath> feedbackLocKey = Pair.of(invocation, refPath);
    if (confirmDirCovCorrect) {
      correctDirectCov.put(feedbackLocKey, relevantDrctCov);
    } else {
      likelyCorrectDirectCov.put(feedbackLocKey, relevantDrctCov);
    }
  }
  
  public void addIncorrectInputValue(MethodInvocation invocation, RefPath refPath) {
    Set<RefPath> paths = incorrectInputValues.get(invocation);
    if (paths == null) {
      paths = new HashSet<>();
      incorrectInputValues.put(invocation, paths);
    }
    paths.add(refPath);
  }
  
  public Map<MethodInvocation, Set<RefPath>> getCorrectOutputValues() {
    return correctOutputValues;
  }
  
  public Map<MethodInvocation, Set<RefPath>> getIncorrectOutputValues() {
    return incorrectOutputValues;
  }
  
  public Map<MethodInvocation, Set<RefPath>> getIncorrectInputValues() {
    return incorrectInputValues;
  }
  
  public Map<TestName, Set<SourceLocation>> getPassingVirtualTests() {
    Map<TestName, Set<SourceLocation>> passingVirtualTests = new HashMap<>();
    int virtualTestId = 0;
    for (Set<SourceLocation> cov : passingVTestsCov.values()) {
      TestName vtName = new TestName("VirtualTest", "vt" + (virtualTestId++));
      passingVirtualTests.put(vtName, cov);
    }
    for (Set<SourceLocation> cov : likelyCorrectDirectCov.values()) {
      TestName vtName = new TestName("VirtualTest", "vt" + (virtualTestId++));
      passingVirtualTests.put(vtName, cov);
    }
    return passingVirtualTests;
  }
  
  
  public Set<SourceLocation> getFLSourceLocationScope() {
    if (incorrectInvocationCov.size() > 0) {
      Iterator<Set<SourceLocation>> scopeItr = incorrectInvocationCov.values().iterator();
      Set<SourceLocation> flScope = new HashSet<>(scopeItr.next());
      while (scopeItr.hasNext()) {
        flScope.retainAll(scopeItr.next());
      }
      return flScope;
    } else {
      return null;
    }
  }
  
  public Set<SourceLocation> getExcludedSourceLocations() {
    Set<SourceLocation> excludedSourceLocations = new HashSet<>();
    for (Set<SourceLocation> excluded : correctDirectCov.values()) {
      excludedSourceLocations.addAll(excluded);
    }
    return excludedSourceLocations;
  }
  
  public void removeCorrectOutputValue(MethodInvocation invocation, RefPath refPath) {
    Set<RefPath> paths = correctOutputValues.get(invocation);
    if (paths == null || !paths.contains(refPath)) {
      return;
    }
    paths.remove(refPath);
    Pair<MethodInvocation, RefPath> feedbackLocKey = Pair.of(invocation, refPath);
    passingVTestsCov.remove(feedbackLocKey);
  }
  
  public void removeIncorrectOutputValue(MethodInvocation invocation, RefPath refPath) {
    Set<RefPath> paths = incorrectOutputValues.get(invocation);
    if (paths == null || !paths.contains(refPath)) {
      return;
    }
    paths.remove(refPath);
    if (paths.size() == 0) {
      incorrectInvocationCov.remove(invocation);
    }
    Pair<MethodInvocation, RefPath> feedbackLocKey = Pair.of(invocation, refPath);
    correctDirectCov.remove(feedbackLocKey);
    likelyCorrectDirectCov.remove(feedbackLocKey);
  }
  
  public void removeIncorrectInputValue(MethodInvocation invocation, RefPath refPath) {
    Set<RefPath> paths = incorrectInputValues.get(invocation);
    if (paths != null) {
      paths.remove(refPath);
    }
  }
}
