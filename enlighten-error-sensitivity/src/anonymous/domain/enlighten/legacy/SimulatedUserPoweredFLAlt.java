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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.mcallrepr.MemberRefDepAnnotator;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.DepIndexRangeAnnotator;
import anonymous.domain.enlighten.slicing.FieldSelectionResult;
import anonymous.domain.enlighten.slicing.QueryFieldSelector;
import anonymous.domain.enlighten.slicing.util.DepBreadthFirstTraversal;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.util.Pair;

public class SimulatedUserPoweredFLAlt extends SimulatedUserPoweredFL {
  
  private QueryFieldSelector fieldSelector;
  
  private FieldSelectionResult query;
  private Pair<RefPath, UserFeedback> answer;
  private int numQueriesIncorporated = 0;
  private List<MethodInvocationSelection> invocHistory = new ArrayList<>();
  private List<RefPath> pathHistory = new ArrayList<>();

  public SimulatedUserPoweredFLAlt(SubjectProgram targetProgram,
      SubjectProgram goldenVersion) {
    super(targetProgram, goldenVersion);
    List<Pair<TestName, ExecutionProfile>> failingTestProfiles = new ArrayList<>();
    for (TestName test : testOutcomes.getTestSet()) {
      if (!testOutcomes.isPassed(test)) {
        failingTestProfiles.add(Pair.of(test, getTestExecutionProfile(test)));
      }
    }
    fieldSelector = new QueryFieldSelector(targetProgram, failingTestProfiles);
  }
  
  @Override
  protected UserFeedback getUserFeedback(MethodInvocationSelection invocation, 
      MethodCallRepr prestate, MethodCallRepr poststate) {
    List<RefPath> candidateFields = query.getOrderedSuspiciousFields().stream().map(
        fieldSuspInfo -> { return fieldSuspInfo.getFirst(); }).collect(Collectors.toList());
    RefPath answeredField = null;
    UserFeedback feedback = null;
    for (RefPath candidateField : candidateFields) {
      ValueGraphNode fieldValue = null;
      try {
        fieldValue = candidateField.getValue(
            selectedInvocationStates.getPostState());
      } catch (NoSuchFieldError | IndexOutOfBoundsException ex) {

        continue;
      }
      ValueGraphNode fieldValueOnRef = null;
      try {
        fieldValueOnRef = candidateField.getValue(
          selectedInvocationStatesOnRef.getPostState());
      } catch (NoSuchFieldError | IndexOutOfBoundsException ex) {

      }
      answeredField = candidateField;
      System.out.println("Feedback given to field " + answeredField);
      if (Objects.equals(fieldValue, fieldValueOnRef)) {
        System.out.println("Its value is correct (" + fieldValue + ").");
        feedback = UserFeedback.CORRECT;
      } else {
        System.out.println("Its value is incorrect. Observed (" + fieldValue + 
            "), but should be (" + fieldValueOnRef + ").");
        feedback = UserFeedback.INCORRECT;
      }
      break;
    }
    if (answeredField == null) {

      throw new RuntimeException("Cannot answer any field in the query.");
    }
    answer = Pair.of(answeredField, feedback);
    return feedback;
  }
  
  @Override
  protected void incorporateUserFeedback(
      MethodInvocationSelection invocation, UserFeedback feedback) {
    invocHistory.add(invocation);
    pathHistory.add(answer.getFirst());
    ++numQueriesIncorporated;
    MethodInvocation invocationNode = getMethodInvocationNode(invocation);
    fieldSelector.addAnsweredField(invocationNode, answer.getFirst());
    if (feedback == UserFeedback.INCORRECT) {
      Set<SourceLocation> coveredSourceLocations = getSourceLocationExecutionCount(
          invocationNode, false).getExecutionCount().keySet();
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
      
      MethodName faultyMethod = MethodName.get(getTargetProgram().getFaultyClassName(), 
          getTargetProgram().getFaultyMethodSignature());
      SourceLocation faultyLine = getTargetProgram().getFaultySourceLocations().get(0);
      if (invocation.getMethodName().equals(faultyMethod) 
          && invocationNode.getStatementsExecCountMap().containsKey(faultyLine)) {

        System.out.println("Faulty value infected in the current method. Stopped.");
        nextForFeedback = null;
        return;

      } else {


        RangeSet<Long> effectiveDepIndexRange = TreeRangeSet.create();
        long startIndex = DepIndexRangeAnnotator.getStartIndex(invocationNode);
        long endIndex = -1;
        for (MethodInvocation child : invocationNode.getEnclosedInvocations()) {
          endIndex = DepIndexRangeAnnotator.getStartIndex(child) - 1;
          if (endIndex >= startIndex) {
            effectiveDepIndexRange.add(Range.closed(startIndex, endIndex));
          }
          startIndex = DepIndexRangeAnnotator.getEndIndex(child) + 1;
        }
        endIndex = DepIndexRangeAnnotator.getEndIndex(invocationNode);
        if (endIndex >= startIndex) {
          effectiveDepIndexRange.add(Range.closed(startIndex, endIndex));
        }
        CollectInfluencingSourceLocations locCollector = 
            new CollectInfluencingSourceLocations(effectiveDepIndexRange);
        locCollector.traverse(
            MemberRefDepAnnotator.getDependency(query.getPostStates(), answer.getFirst()));
        Set<SourceLocation> locsToRemove = locCollector.getResult();
        System.out.println("Excluding src locs:");
        printSrcLocList(locsToRemove);
        for (TestName testName : covMatrix.keySet()) {
          if (!testOutcomes.isPassed(testName)) {
            SourceLocationCoverage coverageData = covMatrix.get(testName);
            for (SourceLocation sourceLocationToRemove : locsToRemove) {
              coverageData.removeCoverage(sourceLocationToRemove);
            }
          }
        }
      }
    } else if (feedback == UserFeedback.CORRECT) {
      TestName virtualTestName = new TestName("VirtualTests", "test" + (virtualTestSerialNum++));
      SourceLocationCoverage virtualTestCov = new SourceLocationCoverage();
      RangeSet<Long> effectiveDepIndexRange = TreeRangeSet.create();
      effectiveDepIndexRange.add(Range.closed(
          DepIndexRangeAnnotator.getStartIndex(invocationNode), 
          DepIndexRangeAnnotator.getEndIndex(invocationNode)));
      CollectInfluencingSourceLocations locCollector = 
          new CollectInfluencingSourceLocations(effectiveDepIndexRange);
      DynamicDependency feedbackFieldDep = 
          MemberRefDepAnnotator.getDependency(query.getPostStates(), answer.getFirst());
      locCollector.traverse(feedbackFieldDep);
      for (SourceLocation covered : locCollector.getResult()) {
        if (covered != null) {
          virtualTestCov.addCoverage(covered);
        }
      }
      System.out.println("Adding passing virtual test. Src locs:");
      printSrcLocList(virtualTestCov.getCoverage());
      testOutcomes.addTestOutcome(virtualTestName, true);
      covMatrix.put(virtualTestName, virtualTestCov);
    } else {
      throw new RuntimeException("Automatic oracle should not give feedback " + feedback);
    }
    if (numQueriesIncorporated > 100) {
      System.out.println("Num of queries limit (100) exceeded. Stopped.");
      nextForFeedback = null;
      Map<MethodInvocationSelection, Set<RefPath>> fieldsSummary = new HashMap<>();
      for (int i = 0; i < invocHistory.size(); ++i) {
        MethodInvocationSelection invoc = invocHistory.get(i);
        RefPath path = pathHistory.get(i);
        Set<RefPath> pathOfInvoc = fieldsSummary.get(invoc);
        if (pathOfInvoc == null) {
          pathOfInvoc = new HashSet<>();
          fieldsSummary.put(invoc, pathOfInvoc);
        }
        pathOfInvoc.add(path);
      }
      int numFieldsTotal = 0;
      System.out.println("Queried fields summary:");
      for (MethodInvocationSelection invoc : fieldsSummary.keySet()) {
        System.out.println(invoc);
        for (RefPath path : fieldsSummary.get(invoc)) {
          System.out.println("\t" + path);
          ++numFieldsTotal;
        }
      }
      System.out.println("Number of invocations: " + fieldsSummary.keySet().size());
      System.out.println("Number of fields: " + numFieldsTotal);
      return;
    }
    computeNextInvocationForFeedback();
  }
  
  protected void computeNextInvocationForFeedback() {
    System.out.println("\n==========================================================");
    System.out.println("Query number " + numQueriesIncorporated);
    currentFlResult = FaultLocalization.getFaultLocalization(testOutcomes, covMatrix);
    System.out.println("Selecting method/fields based on the FL results below:");
    System.out.println(currentFlResult.getRankedListString(20));
    query = fieldSelector.selectFieldsForFeedback(currentFlResult);
    nextForFeedback = query.getInvocation();
    System.out.println("Selected invocation: " + nextForFeedback);
    printSuspiciousFieldsList(query.getOrderedSuspiciousFields());
  }

  private static class CollectInfluencingSourceLocations extends DepBreadthFirstTraversal {
    
    private RangeSet<Long> depIndexRange;
    private Range<Long> depIndexSpan;
    
    private Set<SourceLocation> influencingLocations;
    
    public CollectInfluencingSourceLocations(RangeSet<Long> effectiveDepIndexRange) {
      depIndexRange = effectiveDepIndexRange;
      depIndexSpan = depIndexRange.span();
    }
    
    public Set<SourceLocation> getResult() {
      return influencingLocations;
    }
    
    @Override
    public void traverse(DynamicDependency dep) {
      influencingLocations = new HashSet<>();
      super.traverse(dep);
    }

    @Override
    protected boolean visit(DynamicDependency depNode) {
      if (depNode.getInstanceIndex() < depIndexSpan.lowerEndpoint()) {
        return false;
      }
      if (depIndexRange.contains(depNode.getInstanceIndex()) 
          && depNode instanceof InstructionDependencySource) {
        influencingLocations.add(JpfEntityConversion.getSourceLocationFromInstruction(
            ((InstructionDependencySource) depNode).getSourceInstruction()));
      }
      return true;
    }
  }
  
  private void printSuspiciousFieldsList(
      List<Pair<RefPath, Double>> orderedSuspiciousFields) {
    System.out.println("Most suspicious fields:");
    int numEntriesPrinted = 0;
    for (Pair<RefPath, Double> fieldSuspInfo : orderedSuspiciousFields) {
      System.out.println(fieldSuspInfo.getFirst() 
          + ":\t" + String.format("%.5f", fieldSuspInfo.getSecond()));
      if (++numEntriesPrinted > 10) {
        break;
      }
    }
  }
  
  private void printSrcLocList(Iterable<SourceLocation> locs) {
    Iterator<SourceLocation> locsItr = locs.iterator();
    while (locsItr.hasNext()) {
      System.out.println(locsItr.next());
    }
  }
}
