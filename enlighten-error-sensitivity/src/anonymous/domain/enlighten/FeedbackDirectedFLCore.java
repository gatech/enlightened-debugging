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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.exec.CompileSubjectProgram;
import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.mcallrepr.ArrayElementRefName;
import anonymous.domain.enlighten.mcallrepr.MemberRefDepAnnotator;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.DepIndexRangeAnnotator;
import anonymous.domain.enlighten.slicing.FieldSelectionResult;
import anonymous.domain.enlighten.slicing.GetDepGraphNodeIds;
import anonymous.domain.enlighten.slicing.QueryFieldSelectorAlt2;
import anonymous.domain.enlighten.slicing.util.DepBreadthFirstTraversal;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.CoverageUtils;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.susp.WeightedOchiai;
import anonymous.domain.enlighten.util.Pair;

public class FeedbackDirectedFLCore implements FeedbackDirectedFL {
  
  private static final double INITIAL_PASSING_TESTS_FL_WEIGHT = 0.1;
  
  protected SubjectProgram targetProgram;
  
  protected TestOutcomes originalTestOutcomes;
  protected Map<TestName, SourceLocationCoverage> originalCovMatrix;
  protected Map<TestName, ExecutionProfile> testExecutionProfiles;
  protected Set<SourceLocation> tempExcludedLocs;
  
  protected FeedbackSet feedbackSet;
  protected QueryFieldSelectorAlt2 fieldSelector;
  protected FaultLocalization<SourceLocation> currentFlResult;
  
  protected PrintWriter logger;
  
  public FeedbackDirectedFLCore(SubjectProgram targetProgram) {
    this(targetProgram, false);
  }
  
  public FeedbackDirectedFLCore(SubjectProgram targetProgram, boolean forceGenData) {
    this.targetProgram = targetProgram;
    testExecutionProfiles = new HashMap<>();
    initRequiredData(forceGenData);
    Map<TestName, ExecutionProfile> failingTestProfiles = new HashMap<>();
    for (TestName test : originalTestOutcomes.getTestSet()) {
      if (!originalTestOutcomes.isPassed(test)) {
        failingTestProfiles.put(test, getTestExecutionProfile(test));
      }
    }
    feedbackSet = new FeedbackSet();
    fieldSelector = new QueryFieldSelectorAlt2(targetProgram, failingTestProfiles, feedbackSet);
    currentFlResult = computeFaultLocalizationResult();
    try {
      logger = new PrintWriter(new FileOutputStream(targetProgram.getDataDirRoot().resolve("fl_core.log").toString()));
    } catch (FileNotFoundException e) {
      System.err.println("Cannot create FlCore log file. Using System.out instead.");
      logger = new PrintWriter(System.out);
    }
  }
  
  public FaultLocalization<SourceLocation> getCurrentFlResult() {
    return currentFlResult;
  }
  
  @Override
  public FieldSelectionResult selectNextInvocationForFeedback() {
    boolean allowRetry = false;
    if (tempExcludedLocs != null) {
      tempExcludedLocs = null;
      allowRetry = true;
    }
    FieldSelectionResult query = fieldSelector.selectFieldsForFeedback(currentFlResult);
    if (query == null && allowRetry) {

      currentFlResult = computeFaultLocalizationResult();
      query = fieldSelector.selectFieldsForFeedback(currentFlResult);
    }
    String rankedListStr = currentFlResult.getRankedListString(20);
    if (rankedListStr.length() > 2000) {
      rankedListStr = rankedListStr.substring(0, 2000);
    }
    logln(rankedListStr);
    logln("Selected method invocation: " + query.getInvocation());
    logln("Most suspicious values:");
    logln(getSuspValueListStr(query.getOrderedSuspiciousFields()));
    return query;
  }

  @Override
  public void incorporateCorrectOutputValue(FieldSelectionResult query,
      RefPath selectedField) {
    incorporateUserFeedback(query, selectedField, UserFeedback.CORRECT, false);
  }
  
  @Override
  public void incorporateIncorrectOutputValue(FieldSelectionResult query,
      RefPath selectedField, boolean confirmDirCovCorrect) {
    incorporateUserFeedback(query, selectedField, UserFeedback.INCORRECT, confirmDirCovCorrect);
  }
  
  @Override
  public void incorporateIncorrectInputValue(FieldSelectionResult query,
      RefPath selectedField) {
    incorporateUserFeedback(query, selectedField, UserFeedback.IMPOSSIBLE_PRESTATE, false);
  }

  @Override
  public void removeOutputValueFeedback(FieldSelectionResult query,
      RefPath selectedField) {
    MethodInvocation invocationNode = getMethodInvocationNode(query.getInvocation());
    feedbackSet.removeCorrectOutputValue(invocationNode, selectedField);
    feedbackSet.removeIncorrectOutputValue(invocationNode, selectedField);
    Pair<MethodInvocationSelection, RefPath> feedbackValueKey = 
        Pair.of(query.getInvocation(), selectedField);
    feedbackSet.incorrectOutputRelevantDeps.remove(feedbackValueKey);
  }

  @Override
  public void removeInputValueFeedback(FieldSelectionResult query,
      RefPath selectedField) {
    MethodInvocation invocationNode = getMethodInvocationNode(query.getInvocation());
    feedbackSet.removeIncorrectInputValue(invocationNode, selectedField);
    Pair<MethodInvocationSelection, RefPath> feedbackValueKey = 
        Pair.of(query.getInvocation(), selectedField);
    feedbackSet.incorrectInputRelevantDeps.remove(feedbackValueKey);
  }

  protected void incorporateUserFeedback(FieldSelectionResult query,
      RefPath selectedField, UserFeedback feedback, boolean confirmDirCovCorrect) {
    logln("Feedback incorporation:");
    logln("Verify invocation: " + query.getInvocation());
    logln("Feedback given to value: " + selectedField);
    logln("Feedback type: " + feedback);
    MethodInvocation invocationNode = 
        getMethodInvocationNode(query.getInvocation());
    if (feedback == UserFeedback.INCORRECT) {
      Set<SourceLocation> fullCov = CoverageUtils.getFullStatementCoverage(invocationNode);
      Set<SourceLocation> directCov = 
          Collections.unmodifiableSet(invocationNode.getStatementsExecCountMap().keySet());
      
      feedbackSet.addIncorrectOutputValue(invocationNode, selectedField, 
          fullCov, directCov, confirmDirCovCorrect);
      if (!confirmDirCovCorrect) {
        tempExcludedLocs = directCov;
      }
      
      Pair<MethodInvocationSelection, RefPath> feedbackValueKey = 
          Pair.of(query.getInvocation(), selectedField);
      Range<Long> invocationDepIdRange = Range.closed(
          DepIndexRangeAnnotator.getStartIndex(invocationNode), 
          DepIndexRangeAnnotator.getEndIndex(invocationNode));
      MethodCallRepr postStates = query.getPostStates();
      Set<Long> relevantDepIds = getIncorrectValueDepIds(
          postStates, selectedField, invocationDepIdRange);
      if (selectedField.getTail() instanceof ArrayElementRefName) {
        RefPath arrayLengthPath = 
            selectedField.getParent().append().appendArrayLengthRef().build();
        relevantDepIds.addAll(getIncorrectValueDepIds(
            postStates, arrayLengthPath, invocationDepIdRange));
      }
      feedbackSet.incorrectOutputRelevantDeps.put(feedbackValueKey, relevantDepIds);
    } else if (feedback == UserFeedback.CORRECT) {
      RangeSet<Long> effectiveDepIndexRange = TreeRangeSet.create();
      effectiveDepIndexRange.add(Range.closed(
          DepIndexRangeAnnotator.getStartIndex(invocationNode), 
          DepIndexRangeAnnotator.getEndIndex(invocationNode)));
      CollectInfluencingSourceLocations locCollector = 
          new CollectInfluencingSourceLocations(effectiveDepIndexRange);
      locCollector.traverse(MemberRefDepAnnotator.getDependency(
          query.getPostStates(), selectedField));
      Set<SourceLocation> relevantFullCov = locCollector.getResult();
      feedbackSet.addCorrectOutputValue(invocationNode, selectedField, relevantFullCov);
    } else if (feedback == UserFeedback.IMPOSSIBLE_PRESTATE) {
      feedbackSet.addIncorrectInputValue(invocationNode, selectedField);
      Pair<MethodInvocationSelection, RefPath> feedbackValueKey = 
          Pair.of(query.getInvocation(), selectedField);
      MethodCallRepr preStates = query.getPreStates();
      Set<Long> relevantDepIds = getIncorrectValueDepIds(preStates, selectedField, Range.all());
      if (selectedField.getTail() instanceof ArrayElementRefName) {
        RefPath arrayLengthPath = 
            selectedField.getParent().append().appendArrayLengthRef().build();
        relevantDepIds.addAll(getIncorrectValueDepIds(preStates, arrayLengthPath, Range.all()));
      }
      feedbackSet.incorrectInputRelevantDeps.put(feedbackValueKey, relevantDepIds);
    } else {
      throw new RuntimeException("Unexpected user feedback type " + feedback);
    }
    currentFlResult = computeFaultLocalizationResult();
  }
  
  protected SubjectProgram getTargetProgram() {
    return targetProgram;
  }
  
  private void initRequiredData(boolean forceDataGeneration) {
    if (forceDataGeneration) {
      try {
        System.out.print("Compiling target program... ");
        new CompileSubjectProgram(targetProgram).compile();
        System.out.println("Done.");
      } catch (IOException | InterruptedException e) {
        System.out.println();
        throw new RuntimeException(
            "Failed to compile subject program at " + targetProgram.getRootDir(),
            e);
      }
    }
    RunTestsWithCoverage runWithCoverage = new RunTestsWithCoverage(
        targetProgram);
    boolean covDataPresent = Files.isDirectory(targetProgram.getCoverageDir());
    if (forceDataGeneration || !covDataPresent) {
      try {
        System.out.print(
            "Generating coverage data of the test cases of the target program... ");
        runWithCoverage.generateCoverageData();
        System.out.println("Done.");
      } catch (IOException e) {
        System.out.println();
        throw new RuntimeException(
            "Failed to run tests or generate coverage data for subject program at "
                + targetProgram.getRootDir());
      }
    } else {
      System.out.println("Re-using target program existing coverage data.");
    }
    try {
      originalTestOutcomes = runWithCoverage.getTestOutcomes();
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to read test outcomes data file for subject program at "
              + targetProgram.getRootDir());
    }
    try {
      originalCovMatrix = runWithCoverage.getSourceLocationCoverageMatrix();
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to read coverage data for subject program at "
              + targetProgram.getRootDir());
    }
  }
  
  private FaultLocalization<SourceLocation> computeFaultLocalizationResult() {
    TestOutcomes testOutcomes = new TestOutcomes();
    Map<TestName, SourceLocationCoverage> covMatrix = new HashMap<>();
    Map<TestName, Double> testWeights = new HashMap<>();
    Set<SourceLocation> flScope = feedbackSet.getFLSourceLocationScope();
    Set<SourceLocation> excludedSrcLocs = feedbackSet.getExcludedSourceLocations();
    if (tempExcludedLocs != null) {
      excludedSrcLocs.addAll(tempExcludedLocs);
    }
    for (TestName originalTest : originalCovMatrix.keySet()) {
      boolean isPassing = originalTestOutcomes.isPassed(originalTest);
      testOutcomes.addTestOutcome(originalTest, isPassing);
      if (isPassing) {
        covMatrix.put(originalTest, originalCovMatrix.get(originalTest));
        testWeights.put(originalTest, INITIAL_PASSING_TESTS_FL_WEIGHT);
      } else {
        Set<SourceLocation> filteredCoverage = 
            new HashSet<>(originalCovMatrix.get(originalTest).getCoverage());
        if (flScope != null) {
          filteredCoverage.retainAll(flScope);
        }
        filteredCoverage.removeAll(excludedSrcLocs);
        SourceLocationCoverage filteredSrcCov = new SourceLocationCoverage();
        filteredSrcCov.addCoverage(filteredCoverage);
        covMatrix.put(originalTest, filteredSrcCov);
        testWeights.put(originalTest, 1.0);
      }
    }
    Map<TestName, Set<SourceLocation>> passingVirtualTests =  feedbackSet.getPassingVirtualTests();
    for (TestName virtualTest : passingVirtualTests.keySet()) {
      testOutcomes.addTestOutcome(virtualTest, true);
      SourceLocationCoverage srcCov = new SourceLocationCoverage();
      srcCov.addCoverage(passingVirtualTests.get(virtualTest));
      covMatrix.put(virtualTest, srcCov);
      testWeights.put(virtualTest, 1.0);
    }
    return new WeightedOchiai<SourceLocation>(testOutcomes, covMatrix, testWeights);
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
  
  public MethodInvocation getMethodInvocationNode(
      MethodInvocationSelection invocationSelection) {
    return getTestExecutionProfile(invocationSelection.getTestName()).lookupInvocation(
        invocationSelection.getMethodName(), invocationSelection.getInvocationIndex());
  }
  
  protected void logln(String str) {
    logger.println(str);
    logger.flush();
  }
  
  protected String getSuspValueListStr(List<Pair<RefPath, Double>> orderedSuspValues) {
    StringBuilder sb = new StringBuilder();
    int numEntriesPrinted = 0;
    for (Pair<RefPath, Double> fieldSuspInfo : orderedSuspValues) {
      sb.append(fieldSuspInfo.getFirst() 
          + ":\t" + String.format("%.5f", fieldSuspInfo.getSecond()) + "\n");
      if (++numEntriesPrinted > 10) {
        break;
      }
    }
    return sb.toString();
  }
  
  private Set<Long> getIncorrectValueDepIds(
      MethodCallRepr mStates, RefPath valuePath, Range<Long> depIndexRange) {
    Set<Long> relevantDepIds = new HashSet<>();
    RefPath currentPath = valuePath;
    while (currentPath != null && currentPath.getLength() != 0) {
      relevantDepIds.addAll(GetDepGraphNodeIds.getDepGraphNodeIds(
          MemberRefDepAnnotator.getDependency(mStates, currentPath), depIndexRange));
      currentPath = currentPath.getParent();
    }
    return relevantDepIds;
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
}
