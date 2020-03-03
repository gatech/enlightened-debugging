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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationExecutionCount;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.exec.CaptureInvocationStates;
import anonymous.domain.enlighten.files.CopyDirTree;
import anonymous.domain.enlighten.htmlview.MethodStatesPrinter;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;

public class SimulatedUserPoweredFL extends UserPoweredFLBase {
  
  private static final String LOG_FILE_NAME = "simulated_user_powered_fl_experiment.log";
  private static final String HTML_FILE_NAME = "experiment_log.html";
  
  private List<SourceLocation> faultySourceLocations;
  private Map<TestName, ExecutionProfile> testExecutionProfilesWithStates;
  
  private CaptureInvocationStates captureStates;
  private CaptureInvocationStates captureStatesOnRef;
  protected MethodInvocation selectedInvocationStates;
  protected MethodInvocation selectedInvocationStatesOnRef;
  
  private PrintWriter logWriter;
  private PrintWriter htmlWriter;
  private Map<SourceLocation, MethodName> sourceLocationMethodMap;
  private List<FlResultStats> flResultStatsProgression;
  private List<QueryStats> queryStatsList;

  public SimulatedUserPoweredFL(SubjectProgram targetProgram, SubjectProgram goldenVersion) {
    this(targetProgram, goldenVersion, false);
  }
  
  public SimulatedUserPoweredFL(SubjectProgram targetProgram, 
      SubjectProgram goldenVersion, boolean forceGenData) {
    super(targetProgram, forceGenData, goldenVersion);
    faultySourceLocations = targetProgram.getFaultySourceLocations();
    if (faultySourceLocations == null || faultySourceLocations.size() == 0) {
      throw new RuntimeException(
          "Faulty source location information is not configured in subject program at " 
              + targetProgram.getRootDir());
    }
    try {
      captureStates = new CaptureInvocationStates(targetProgram);
      captureStatesOnRef = new CaptureInvocationStates(targetProgram, goldenVersion);
      



    } catch (IOException ex) {
      throw new RuntimeException("Unable to create the workspace folder for state capturing.", ex);
    }
    flResultStatsProgression = new ArrayList<>();
    queryStatsList = new ArrayList<>();
    initRequiredData(forceGenData);
    try {
      logWriter = new PrintWriter(new FileWriter(
          targetProgram.getDataDirRoot().resolve(LOG_FILE_NAME).toString()));
      initHtmlLog();
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to create experiment log file for subject program at " 
              + targetProgram.getRootDir(), e);
    }
    sourceLocationMethodMap = new HashMap<>();
    for (MethodInvocation invocationTree : knownIncorrectExecutions.keySet()) {
      initSourceLocationMethodMapRecursive(invocationTree);
    }
  }
  
  public List<FlResultStats> getFlResultStatsProgression() {
    return flResultStatsProgression;
  }
  
  public List<QueryStats> getQueryStatsList() {
    return queryStatsList;
  }

  @Override
  protected boolean isDone() {
    if (nextForFeedback == null) {
      recordFaultLocalizationResultReportingStats();
      logWriter.close();
      finalizeHtmlLog();
      htmlWriter.close();
      Path finalFlResultDataFile = 
          getTargetProgram().getDataDirRoot().resolve("final_ranked_list.txt");
      try {
        printRankedListToFile(
            FaultLocalization.getFaultLocalization(testOutcomes, covMatrix), finalFlResultDataFile);
      } catch (IOException e) {
        throw new RuntimeException("Failed to write initial ranked list file for subject program at "
            + getTargetProgram().getRootDir(), e);
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected MethodInvocationSelection selectMethodInvocationForUserFeedback() {
    recordFaultLocalizationResultReportingStats();
    MethodInvocationSelection selectedInvoc = super.selectMethodInvocationForUserFeedback();
    try {
      selectedInvocationStates = captureStates.getInvocationStates(
          selectedInvoc.getTestName(), selectedInvoc.getMethodName(),
          selectedInvoc.getInvocationIndex());
      selectedInvocationStatesOnRef = captureStatesOnRef.getInvocationStates(
          selectedInvoc.getTestName(), selectedInvoc.getMethodName(),
          selectedInvoc.getInvocationIndex());
    } catch (IOException ex) {
      throw new RuntimeException("Unable to capture invocation states", ex);
    }
    return selectedInvoc;
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
    SourceLocationExecutionCount unclearedExecCount = getSourceLocationExecutionCount(
        getTestExecutionProfile(invocation.getTestName()).lookupInvocation(
            invocation.getMethodName(), invocation.getInvocationIndex()), true);
    boolean containsNoFault = true;
    for (SourceLocation faultyLocation : faultySourceLocations) {
      if (unclearedExecCount.getExecutionCount(faultyLocation) != 0) {
        containsNoFault = false;
        break;
      }
    }
    if (containsNoFault) {
      recordMethodInvocationFeedbackReportingStats(invocation, UserFeedback.CORRECT, 
          "Method invocation does not execute the faulty code.");
      return UserFeedback.CORRECT;
    }
    MethodCallRepr preStatesOnTarget = (MethodCallRepr) prestate;
    MethodCallRepr postStatesOnTarget = (MethodCallRepr) poststate;
    MethodCallRepr preStatesOnRef = (MethodCallRepr) selectedInvocationStatesOnRef.getPreState();
    MethodCallRepr postStatesOnRef = (MethodCallRepr) selectedInvocationStatesOnRef.getPostState();
    if (preStatesOnTarget.getValueHash() != preStatesOnRef.getValueHash()) {
      recordMethodInvocationFeedbackReportingStats(invocation, UserFeedback.DONT_KNOW, 
          "Pre-states do not match between the faulty program and its reference implementation.");
      return UserFeedback.DONT_KNOW;
    }
    if (postStatesOnTarget.getValueHash() == postStatesOnRef.getValueHash()) {
      recordMethodInvocationFeedbackReportingStats(invocation, UserFeedback.CORRECT, 
          "Post-states match between the faulty program and its reference implementation.");
      return UserFeedback.CORRECT;
    } else {
      recordMethodInvocationFeedbackReportingStats(invocation, UserFeedback.INCORRECT, 
          "Post-states do not match between the faulty program and its reference implementation.");
      return UserFeedback.INCORRECT;
    }
  }

  private void initRequiredData(boolean forceDataGeneration) {
    SubjectProgram targetProgram = getTargetProgram();
    Set<TestName> failingTests = new HashSet<>();
    for (TestName test : testOutcomes.getTestSet()) {
      if (!testOutcomes.isPassed(test)) {
        failingTests.add(test);
      }
    }
    FaultLocalization<SourceLocation> faultLocalization = 
        FaultLocalization.getFaultLocalization(testOutcomes, covMatrix);
    Path initialFlResultDataFile = 
        targetProgram.getDataDirRoot().resolve("initial_ranked_list.txt");
    try {
      printRankedListToFile(faultLocalization, initialFlResultDataFile);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write initial ranked list file for subject program at "
          + targetProgram.getRootDir(), e);
    }
  }
  
  @Override
  protected ExecutionProfile getTestExecutionProfile(TestName testName) {
    if (testExecutionProfilesWithStates == null) {
      testExecutionProfilesWithStates = new HashMap<>();
    }
    return getProfileWithCaching(getTargetProgram(), 
        testExecutionProfilesWithStates, testName, true);
  }
  
  private ExecutionProfile getProfileWithCaching(SubjectProgram subject, Map<TestName, 
      ExecutionProfile> cache, TestName testName, boolean hasStmtCoverageTree) {
    if (!cache.containsKey(testName)) {
      try {
        ExecutionProfile coverageProfile = ExecutionProfile.readFromDataFile(
            subject.getCoverageDir().resolve(testName.getDescription() + ".tree"));
        cache.put(testName, coverageProfile);
      } catch (IOException ex) {
        throw new RuntimeException("Cannot read execution profile", ex);
      }
    }
    return cache.get(testName);
  }
  
  public static class FlResultStats {
    public int bestStatementRank;
    public int worstStatementRank;
    public int bestMethodRank;
    public int worstMethodRank;
    public double faultSuspiciousness;
    public int bestInvocationRank;
    public int worstInvocationRank;
  }
  
  private void recordFaultLocalizationResultReportingStats() {
    FaultLocalization<SourceLocation> faultLocalization = 
        FaultLocalization.getFaultLocalization(testOutcomes, covMatrix);
    List<SourceLocation> rankedList = faultLocalization.getRankedList();
    SourceLocation firstRankedFault = null;
    double firstRankedFaultSuspiciousness = 0;
    MethodName faultyMethod = null;
    FlResultStats flResultStats = new FlResultStats();
    for (SourceLocation suspiciousSourceLocation : rankedList) {
      if (faultySourceLocations.contains(suspiciousSourceLocation)) {
        firstRankedFault = suspiciousSourceLocation;
        faultyMethod = sourceLocationMethodMap.get(firstRankedFault);
        firstRankedFaultSuspiciousness = faultLocalization.getSuspiciousness(firstRankedFault);
        flResultStats.faultSuspiciousness = firstRankedFaultSuspiciousness;
        break;
      }
    }
    Set<MethodName> methodsRankedHigher = new HashSet<>();
    Set<MethodName> methodsRankedSame = new HashSet<>();
    Set<MethodInvocation> invocationsRankedHigher = 
        Collections.newSetFromMap(new IdentityHashMap<MethodInvocation, Boolean>());
    Set<MethodInvocation> invocationsRankedSame =
        Collections.newSetFromMap(new IdentityHashMap<MethodInvocation, Boolean>());
    for (SourceLocation suspiciousSourceLocation : rankedList) {
      double currentSuspiciousness = faultLocalization.getSuspiciousness(suspiciousSourceLocation);
      if (compareFloat(currentSuspiciousness, firstRankedFaultSuspiciousness) > 0) {
        ++flResultStats.bestStatementRank;
        ++flResultStats.worstStatementRank;
        MethodName containingMethod = sourceLocationMethodMap.get(suspiciousSourceLocation);
        if (containingMethod != faultyMethod) {
          methodsRankedHigher.add(containingMethod);
        }
        for (TestName test : testOutcomes.getTestSet()) {
          if (!testOutcomes.isPassed(test)) {
            ExecutionProfile profile = getTestExecutionProfile(test);
            List<MethodInvocation> invocations = profile.lookupInvocation(suspiciousSourceLocation);
            if (invocations == null) {
              continue;
            }
            for (MethodInvocation invocation : invocations) {
              if (!invocation.getStatementsExecCountMap().keySet().contains(firstRankedFault)) {
                invocationsRankedHigher.add(invocation);
              }
            }
          }
        }
      } else if (compareFloat(currentSuspiciousness, firstRankedFaultSuspiciousness) == 0) {
        ++flResultStats.worstStatementRank;
        methodsRankedSame.add(sourceLocationMethodMap.get(suspiciousSourceLocation));
        for (TestName test : testOutcomes.getTestSet()) {
          if (!testOutcomes.isPassed(test)) {
            ExecutionProfile profile = getTestExecutionProfile(test);
            List<MethodInvocation> invocations = profile.lookupInvocation(suspiciousSourceLocation);
            if (invocations == null) {
              continue;
            }
            for (MethodInvocation invocation : invocations) {
              if (!invocation.getStatementsExecCountMap().keySet().contains(firstRankedFault)) {
                invocationsRankedSame.add(invocation);
              }
            }
          }
        }
      } else {
        break;
      }
    }
    ++flResultStats.bestStatementRank;
    flResultStats.bestMethodRank = methodsRankedHigher.size() + 1;
    flResultStats.worstMethodRank = methodsRankedHigher.size() + methodsRankedSame.size();
    flResultStats.bestInvocationRank = invocationsRankedHigher.size() + 1;
    flResultStats.worstInvocationRank = 
        invocationsRankedHigher.size() + invocationsRankedSame.size() + 1;
    flResultStatsProgression.add(flResultStats);
    logWriter.println(String.format("Fault suspiciousness: %.2f", 
        flResultStats.faultSuspiciousness));
    logWriter.println(String.format("Stmt rank(b/w): %d/%d", 
        flResultStats.bestStatementRank, flResultStats.worstStatementRank));
    logWriter.println(String.format("Method rank(b/w): %d/%d", 
        flResultStats.bestMethodRank, flResultStats.worstMethodRank));
    logWriter.println(String.format("Invocation rank(b/w): %d/%d", 
        flResultStats.bestInvocationRank, flResultStats.worstInvocationRank)); 
    logWriter.println();
    logWriter.flush();
  }
  
  public static class QueryStats {
    public MethodInvocationSelection invocationSelection;
    public UserFeedback feedback;
    public String reason;
    public int memReadSize;
    public int memReadObjects;
    public int memWriteSize;
    public int memWriteObjects;
  }
  
  private void recordMethodInvocationFeedbackReportingStats(
      MethodInvocationSelection invocation, UserFeedback feedback, String reason) {
    
    htmlWriter.println("<p>");
    QueryStats queryStats = new QueryStats();
    queryStatsList.add(queryStats);
    queryStats.invocationSelection = invocation;
    queryStats.feedback = feedback;
    queryStats.reason = reason;
    String queriedMethodId = "Queried invocation: " + invocation.getMethodName() + "#" 
        + invocation.getInvocationIndex() + " in " + invocation.getTestName();
    logWriter.println(queriedMethodId);
    htmlWriter.println(StringEscapeUtils.escapeHtml4(queriedMethodId) + "<br/>");
    MethodInvocation methodInvocation =  getTestExecutionProfile(invocation.getTestName())
        .lookupInvocation(invocation.getMethodName(), invocation.getInvocationIndex());
    queryStats.memReadSize = methodInvocation.getNumMemoryReadLocations();
    queryStats.memReadObjects = methodInvocation.getNumMemoryReadObjects();
    queryStats.memWriteSize = methodInvocation.getNumMemoryWriteLocations();
    queryStats.memWriteObjects = methodInvocation.getNumMemoryWriteObjects();
    String readWriteSizeStr = "Mem Read Size: " + queryStats.memReadSize
        + "\tMem Read Objects: " + queryStats.memReadObjects
        + "\tMem Write Size: " + queryStats.memWriteSize
        + "\tMem Write Objects: " + queryStats.memWriteObjects;
    logWriter.println(readWriteSizeStr);
    logWriter.println("Feedback: " + feedback);
    String feedbackColorStr = null;
    if (feedback == UserFeedback.CORRECT) {
      feedbackColorStr = "#66ff66"; 
    } else if (feedback == UserFeedback.INCORRECT) {
      feedbackColorStr = "#ff6666"; 
    } else if (feedback == UserFeedback.DONT_KNOW) {
      feedbackColorStr = "yello";
    }
    htmlWriter.println("<span style=\"background:" + feedbackColorStr 
        + "\">Feedback: " +feedback + "</span><br/>");
    logWriter.println("Reason: " + reason);
    MethodInvocation states = selectedInvocationStates;
    htmlWriter.println("States details:<br/>");
    htmlWriter.println(
        MethodStatesPrinter.getHtmlViewContent((MethodCallRepr) states.getPreState()));
    htmlWriter.println(
        MethodStatesPrinter.getHtmlViewContent((MethodCallRepr) states.getPostState()));
    logWriter.println("===============================================================");
    htmlWriter.println("</p>");
    logWriter.flush();
    htmlWriter.flush();
  }
  
  private void initHtmlLog() throws IOException {
    htmlWriter = new PrintWriter(new FileWriter(
        getTargetProgram().getDataDirRoot().resolve(HTML_FILE_NAME).toString()));
    
    

    Path scriptsResourcePath = Paths.get("src/resources/CollapsibleLists");
    Files.walkFileTree(scriptsResourcePath, 
        new CopyDirTree(getTargetProgram().getDataDirRoot().resolve("CollapsibleLists")));
    Path logTemplatePath = Paths.get("src/resources/HtmlLogTemplate.txt");
    htmlWriter.write(new String(Files.readAllBytes(logTemplatePath)));
  }
  
  private void finalizeHtmlLog() {
    htmlWriter.println("</body>\n</html>");
    htmlWriter.flush();
  }
  
  private void printRankedListToFile(FaultLocalization<?> fl, Path dataFile) throws IOException {
    Files.write(dataFile, fl.getRankedListString().getBytes());
  }
  
  private void initSourceLocationMethodMapRecursive(
      MethodInvocation invocationTree) {
    Map<SourceLocation, Integer> stmtExecCounts = invocationTree.getStatementsExecCountMap();
    for (SourceLocation stmt : stmtExecCounts.keySet()) {
      sourceLocationMethodMap.put(stmt, invocationTree.getMethodName());
    }
    for (MethodInvocation childInvocation : invocationTree.getEnclosedInvocations()) {
      initSourceLocationMethodMapRecursive(childInvocation);
    }
  }
}
