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


package anonymous.domain.enlighten.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.UserPoweredFLException;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import anonymous.domain.enlighten.data.SourceLocationExecutionCount;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.exec.CompileSubjectProgram;
import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;



public abstract class UserPoweredFLBase {
  
  protected SubjectProgram targetProgram;
  
  protected TestOutcomes testOutcomes;
  protected Map<TestName, SourceLocationCoverage> covMatrix;
  protected Map<TestName, ExecutionProfile> testExecutionProfiles;
  
  protected FaultLocalization<SourceLocation> currentFlResult;
  protected MethodInvocationSelection nextForFeedback;
  protected IdentityHashMap<MethodInvocation, UserFeedback> feedbackHistory;
  protected int virtualTestSerialNum = 0;
  protected IdentityHashMap<MethodInvocation, TestName> knownIncorrectExecutions;
  
  public UserPoweredFLBase(SubjectProgram targetProgram, boolean forceGenData) {
    this(targetProgram, forceGenData, null);
  }
  
  public UserPoweredFLBase(SubjectProgram targetProgram, boolean forceGenData,
      SubjectProgram goldenVersion) {
    this.targetProgram = targetProgram;
    testExecutionProfiles = new HashMap<>();
    feedbackHistory = new IdentityHashMap<>();
    initRequiredData(forceGenData, goldenVersion);
  }

  public void localizeFault() throws UserPoweredFLException {
    computeNextInvocationForFeedback();
    try {
      while (!isDone()) {
        queryUserAndUpdateResults();
      }
    } catch (Throwable ex) {
      throw new UserPoweredFLException(ex);
    }
  }
  
  public void queryUserAndUpdateResults() {
    MethodInvocationSelection methodToQuery = selectMethodInvocationForUserFeedback();
    MethodCallRepr prestate = getMethodInvocationPreState(methodToQuery);
    MethodCallRepr poststate = getMethodInvocationPostState(methodToQuery);
    UserFeedback feedback = getUserFeedback(methodToQuery, prestate, poststate);
    incorporateUserFeedback(methodToQuery, feedback);
  }
  
  public FaultLocalization<SourceLocation> getCurrentFlResult() {
    return currentFlResult;
  }
  
  protected MethodInvocationSelection selectMethodInvocationForUserFeedback() {
    return nextForFeedback;
  }

  

  protected MethodCallRepr getMethodInvocationPreState(
      MethodInvocationSelection invocation) {
    
    return null;
  }
  
  protected MethodCallRepr getMethodInvocationPostState(
      MethodInvocationSelection invocation) {
    
    return null;
  }

  protected void incorporateUserFeedback(
      MethodInvocationSelection invocation, UserFeedback feedback) {
    ExecutionProfile testExecProfile = getTestExecutionProfile(invocation.getTestName());
    MethodInvocation methodInvocation = testExecProfile.lookupInvocation(
        invocation.getMethodName(), invocation.getInvocationIndex());
    feedbackHistory.put(methodInvocation, feedback);
    if (feedback == UserFeedback.CORRECT) {

      TestName virtualTestName = new TestName("VirtualTests", "test" + (virtualTestSerialNum++));
      SourceLocationExecutionCount virtualTestSourceExecCount = 
          getSourceLocationExecutionCount(methodInvocation, false);
      SourceLocationCoverage virtualTestCov = new SourceLocationCoverage();
      for (SourceLocation sourceLocation 
          : virtualTestSourceExecCount.getExecutionCount().keySet()) {
        if (virtualTestSourceExecCount.getExecutionCount(sourceLocation) > 0) {
          virtualTestCov.addCoverage(sourceLocation);
        }
      }
      testOutcomes.addTestOutcome(virtualTestName, true);
      covMatrix.put(virtualTestName, virtualTestCov);
      



      MethodInvocation shortestEnclosingWrongExecution = methodInvocation;
      while (!knownIncorrectExecutions.containsKey(shortestEnclosingWrongExecution)) {
        shortestEnclosingWrongExecution = shortestEnclosingWrongExecution.getEnclosingInvocation();
      }
      SourceLocationExecutionCount unclearedSourceExecCount = 
          getSourceLocationExecutionCount(shortestEnclosingWrongExecution, true);
      for (SourceLocation sourceLocation 
          : virtualTestSourceExecCount.getExecutionCount().keySet()) {
        if (unclearedSourceExecCount.getExecutionCount(sourceLocation) == 0) {
          for (TestName testName : covMatrix.keySet()) {
            if (!testOutcomes.isPassed(testName)) {
              covMatrix.get(testName).removeCoverage(sourceLocation);
            }
          }
        }
      }
    } else if (feedback == UserFeedback.INCORRECT) {
      knownIncorrectExecutions.put(methodInvocation, invocation.getTestName());


      Set<SourceLocation> coveredSourceLocations = 
          getSourceLocationExecutionCount(methodInvocation, false).getExecutionCount().keySet();
      for (TestName testName : covMatrix.keySet()) {
        if (!testOutcomes.isPassed(testName)) {
          SourceLocationCoverage coverageData = covMatrix.get(testName);
          List<SourceLocation> sourceLocationsToRemove = new ArrayList<>();
          for (SourceLocation sourceLocation : coverageData.getCoverage()) {
            if (!coveredSourceLocations.contains(sourceLocation)) {
              sourceLocationsToRemove.add(sourceLocation);
            }
          }
          for (SourceLocation sourceLocationToRemove : sourceLocationsToRemove) {
            coverageData.removeCoverage(sourceLocationToRemove);
          }
        }
      }
    } else if (feedback == UserFeedback.IMPOSSIBLE_PRESTATE) {
      Set<SourceLocation> preceedingSourceLocations = getSourceLocationExecutionCountFromPartialTree(
          getTestExecutionProfile(invocation.getTestName()).getInvocationTreeRoot(), 
          methodInvocation).getExecutionCount().keySet();
      for (TestName testName : covMatrix.keySet()) {
        if (!testOutcomes.isPassed(testName)) {
          SourceLocationCoverage coverageData = covMatrix.get(testName);
          List<SourceLocation> sourceLocationsToRemove = new ArrayList<>();
          for (SourceLocation sourceLocation : coverageData.getCoverage()) {
            if (!preceedingSourceLocations.contains(sourceLocation)) {
              sourceLocationsToRemove.add(sourceLocation);
            }
          }
          for (SourceLocation sourceLocationToRemove : sourceLocationsToRemove) {
            coverageData.removeCoverage(sourceLocationToRemove);
          }
        }
      }
    }
    computeNextInvocationForFeedback();
  }
  
  protected abstract boolean isDone();
  protected abstract UserFeedback getUserFeedback(
      MethodInvocationSelection invocation, MethodCallRepr prestate, MethodCallRepr poststate);
  
  protected SubjectProgram getTargetProgram() {
    return targetProgram;
  }
  


  private void initRequiredData(boolean forceDataGeneration, SubjectProgram goldenVersion) {
   
   RunTestsWithCoverage runWithCoverage = new RunTestsWithCoverage(targetProgram);
   
   try {
     testOutcomes = runWithCoverage.getTestOutcomes();
   } catch (IOException e) {
     throw new RuntimeException(
         "Failed to read test outcomes data file for subject program at " 
             + targetProgram.getRootDir());
   }
   try {
     covMatrix = runWithCoverage.getSourceLocationCoverageMatrix();
   } catch (IOException e) {
     throw new RuntimeException(
         "Failed to read coverage data for subject program at " 
             + targetProgram.getRootDir());
   }
   Set<TestName> failingTests = new HashSet<>();
   for (TestName test : testOutcomes.getTestSet()) {
     if (!testOutcomes.isPassed(test)) {
       failingTests.add(test);
     }
   }
   knownIncorrectExecutions = new IdentityHashMap<>();
   for (TestName failingTest : failingTests) {
     knownIncorrectExecutions.put(
         getTestExecutionProfile(failingTest).getInvocationTreeRoot(), failingTest);
   }
 }
 
 protected ExecutionProfile getTestExecutionProfile(TestName testName) {
    if (!testExecutionProfiles.containsKey(testName)) {
      try {
        ExecutionProfile coverageProfile = ExecutionProfile
            .readFromDataFile(targetProgram.getCoverageDir()
                .resolve(testName.getDescription() + ".tree"));
        testExecutionProfiles.put(testName, coverageProfile);
      } catch (IOException ex) {
        throw new RuntimeException("Cannot read execution profile", ex);
      }
    }
    return testExecutionProfiles.get(testName);
 }
 
 protected MethodInvocation getMethodInvocationNode(
     MethodInvocationSelection invocationSelection) {
   return getTestExecutionProfile(invocationSelection.getTestName()).lookupInvocation(
       invocationSelection.getMethodName(), invocationSelection.getInvocationIndex());
 }
 
 protected void computeNextInvocationForFeedback() {
   nextForFeedback = null;
   currentFlResult = FaultLocalization.getFaultLocalization(testOutcomes, covMatrix);
   List<SourceLocation> rankedList = currentFlResult.getRankedList();
   double currentSuspLevel = currentFlResult.getSuspiciousness(rankedList.get(0));
   while (currentSuspLevel > 0) {
     List<SourceLocation> sourceLocationCandidates = new ArrayList<>();
     for (SourceLocation sourceLocation : rankedList) {
       if (compareFloat(currentSuspLevel,
           currentFlResult.getSuspiciousness(sourceLocation)) == 0) {
         sourceLocationCandidates.add(sourceLocation);
       } else if (compareFloat(currentSuspLevel, 
           currentFlResult.getSuspiciousness(sourceLocation)) > 0) {
         break;
       }
     }

     int minNqc = Integer.MAX_VALUE;
     TestName testSelection = null;
     MethodName methodSelection = null;
     int invocationIndexSelection = 0;
     for (SourceLocation sourceLocationCandidate : sourceLocationCandidates) {
       int sourceLocationMinNqc = Integer.MAX_VALUE;
       TestName sourceLocationTestSelection = null;
       MethodName correspondingMethod = null;
       int sourceLocationInvocationIndexSelection = 0;
       for (MethodInvocation incorrectExecution : knownIncorrectExecutions.keySet()) {
         List<MethodInvocation> invocationsForClearance =
             getInvocationsForClearance(sourceLocationCandidate, incorrectExecution);
         if (invocationsForClearance == null) {
           continue;
         }
         if (invocationsForClearance.size() == 0) {





           
           continue;
         }
         if (invocationsForClearance.size() < sourceLocationMinNqc) {
           sourceLocationMinNqc = invocationsForClearance.size();
           sourceLocationTestSelection = knownIncorrectExecutions.get(incorrectExecution);
           correspondingMethod = invocationsForClearance.get(0).getMethodName();
           sourceLocationInvocationIndexSelection = getTestExecutionProfile(sourceLocationTestSelection)
               .lookupInvocationIndex(invocationsForClearance.get(0));
         }
       }
       if (sourceLocationMinNqc < minNqc) {
         methodSelection = correspondingMethod;
         minNqc = sourceLocationMinNqc;
         testSelection = sourceLocationTestSelection;
         invocationIndexSelection = sourceLocationInvocationIndexSelection;
       }
     }
     if (methodSelection != null) {
       nextForFeedback = new MethodInvocationSelection(
           testSelection, methodSelection, invocationIndexSelection);
       return;
     }
     double nextSuspLevel = 0;
     for (SourceLocation sourceLocation : rankedList) {
       if (compareFloat(currentSuspLevel, currentFlResult.getSuspiciousness(sourceLocation)) > 0) {
         nextSuspLevel = currentFlResult.getSuspiciousness(sourceLocation);
         break;
       }
     }
     currentSuspLevel = nextSuspLevel;
   }
 }
 
 protected boolean isMethodInvocationCleared(MethodInvocation invocation) {
   MethodInvocation current = invocation;
   while (current != null) {
     UserFeedback feedback = feedbackHistory.get(current);
     if (feedback == UserFeedback.CORRECT) {
       return true;
     }
     if (feedback == UserFeedback.INCORRECT) {
       return false;
     }
     current = current.getEnclosingInvocation();
   }
   return false;
 }
 
 protected List<MethodInvocation> getInvocationsForClearance(
     SourceLocation sourceLocation, MethodInvocation root) {
   List<MethodInvocation> invocationsForClearance = new ArrayList<>();
   ExecutionProfile execution = new ExecutionProfile("temp_profile", root);
   List<MethodInvocation> allInvocations = execution.lookupInvocation(sourceLocation);
   if (allInvocations == null) {
     return invocationsForClearance;
   }
   for (MethodInvocation invocation : allInvocations) {
     UserFeedback directFeedback = feedbackHistory.get(invocation);
     if (directFeedback != null && directFeedback != UserFeedback.CORRECT) {

       return null;
     }
     if (!isMethodInvocationCleared(invocation)) {
       invocationsForClearance.add(invocation);
     }
   }
   return invocationsForClearance;
 }
 
 protected SourceLocationExecutionCount getSourceLocationExecutionCount(
     MethodInvocation invocation, boolean unclearedInvocationsOnly) {
   SourceLocationExecutionCount execCount = new SourceLocationExecutionCount();
   addExecCountRecursively(invocation, execCount, unclearedInvocationsOnly);
   return execCount;
 }
 
 protected void addExecCountRecursively(MethodInvocation invocation, 
     SourceLocationExecutionCount execCount, boolean unclearedInvocationsOnly) {
   if (unclearedInvocationsOnly && isMethodInvocationCleared(invocation)) {
     return;
   }
   Map<SourceLocation, Integer> stmtExecCountMap = invocation.getStatementsExecCountMap();
   for (SourceLocation location : stmtExecCountMap.keySet()) {
     execCount.incrementExecutionCount(location, stmtExecCountMap.get(location));
   }
   for (MethodInvocation enclosedInvocation : invocation.getEnclosedInvocations()) {
     addExecCountRecursively(enclosedInvocation, execCount, unclearedInvocationsOnly);
   }
 }
 
 private SourceLocationExecutionCount getSourceLocationExecutionCountFromPartialTree(
     MethodInvocation root, MethodInvocation end) {
   SourceLocationExecutionCount execCount = new SourceLocationExecutionCount();
   if (root != end) {
     addExecCountRecursivelyFromPartialTree(root, end, execCount);
   }
   return execCount;
 }
 
 private void addExecCountRecursivelyFromPartialTree(MethodInvocation root, MethodInvocation end,
     SourceLocationExecutionCount execCount) {
   Map<SourceLocation, Integer> stmtExecCountMap = root.getStatementsExecCountMap();
   for (SourceLocation location : stmtExecCountMap.keySet()) {
     execCount.incrementExecutionCount(location, stmtExecCountMap.get(location));
   }
   for (MethodInvocation enclosedInvocation : root.getEnclosedInvocations()) {
     if (enclosedInvocation == end) {
       break;
     }
     addExecCountRecursivelyFromPartialTree(enclosedInvocation, end, execCount);
   }
 }
 
 protected static int compareFloat(double n1, double n2) {
   final double PRECISION = 0.00000001;
   if (n1 - n2 > PRECISION) {
     return 1;
   } else if (n2 - n1 > PRECISION) {
     return -1;
   } else {
     return 0;
   }
 }
  
  public static enum UserFeedback {
    
    
    CORRECT,
    
    
    INCORRECT,
    
    
    IMPOSSIBLE_PRESTATE,
    
    
    DONT_KNOW
  }
}
