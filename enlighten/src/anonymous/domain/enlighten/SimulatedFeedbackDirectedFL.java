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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.exec.CaptureInvocationStates;
import anonymous.domain.enlighten.exec.CompileSubjectProgram;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.publish.ExtraStats;
import anonymous.domain.enlighten.publish.ExtraStatsPublisher;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.FieldSelectionResult;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.util.Pair;

public class SimulatedFeedbackDirectedFL {
  
  private static final int DEFAULT_MAX_ANSWERS = 100;
  
  private SubjectProgram targetProgram;
  private int maxNumAnswers = DEFAULT_MAX_ANSWERS;
  private boolean requireAnswerToMostSusp = false;
  
  private CaptureInvocationStates statesSnapshotter;
  private CaptureInvocationStates statesOnRefImplSnapshotter;
  
  private PrintWriter logger;

  public SimulatedFeedbackDirectedFL(SubjectProgram targetProgram, SubjectProgram refImpl) {
    this.targetProgram = targetProgram;
    
    try {
      statesSnapshotter = new CaptureInvocationStates(targetProgram);
      statesOnRefImplSnapshotter = new CaptureInvocationStates(targetProgram, refImpl);
    } catch (IOException ex) {
      throw new RuntimeException(
          "Unable to create the workspace folder for state capturing.", ex);
    }
  }
  
  public void setMaxNumAnswers(int maxAnswers) {
    maxNumAnswers = maxAnswers;
  }
  
  public void requireAnswerToMostSuspicousValue() {
    requireAnswerToMostSusp = true;
  }
  
  public int localizeFault() {
    createLogger();
    MethodName faultyMethod = MethodName.get(targetProgram.getFaultyClassName(), 
        targetProgram.getFaultyMethodSignature());
    SourceLocation faultyLine = targetProgram.getFaultySourceLocations().get(0);
    FeedbackDirectedFLCore flCore = new FeedbackDirectedFLCore(targetProgram);
    writeFaultLocalizationResult(flCore.getCurrentFlResult(), "initial_SFL_result.txt");
    for (int nQueries = 0; nQueries < maxNumAnswers; ++nQueries) {
      log("\n=========================================================");
      log("Query number: " + nQueries);
      FaultLocalization<SourceLocation> flResult = flCore.getCurrentFlResult();
      log("Selecting query based on FL results:");
      String rankedListStr = flResult.getRankedListString(20);
      if (rankedListStr.length() > 2000) {
        rankedListStr = rankedListStr.substring(0, 2000);
      }
      log(rankedListStr);
      int faultBestRank = flResult.getBestCaseRank(faultyLine);
      int faultWorstRank = flResult.getWorstCaseRank(faultyLine);
      log(String.format("Rank of the fault: %d / %d", faultBestRank, faultWorstRank));
      long selectionStartTimeMillis = System.currentTimeMillis();
      FieldSelectionResult query = flCore.selectNextInvocationForFeedback();
      long selectionTimeMillis = System.currentTimeMillis() - selectionStartTimeMillis;
      log("Query selection time: " + selectionTimeMillis + " ms.");
      MethodInvocationSelection selectedInvocation = query.getInvocation();
      log("Selected method invocation: " + selectedInvocation);
      log("Most suspicious fields:");
      printSuspiciousFieldsList(query.getOrderedSuspiciousFields());
      MethodCallRepr invocPostStates = null;
      MethodCallRepr invocPostStatesRef = null;
      try {
        invocPostStates = statesSnapshotter.getInvocationStates(
            selectedInvocation.getTestName(), 
            selectedInvocation.getMethodName(), 
            selectedInvocation.getInvocationIndex()).getPostState();
        invocPostStatesRef = statesOnRefImplSnapshotter.getInvocationStates(
            selectedInvocation.getTestName(), 
            selectedInvocation.getMethodName(), 
            selectedInvocation.getInvocationIndex()).getPostState();
      } catch (IOException ex) {
        throw new RuntimeException("Error snapshotting invocation states", ex);
      }
      List<RefPath> candidateFields = new ArrayList<>();
      Map<RefPath, Double> fieldsSuspMap = new HashMap<>();
      for (Pair<RefPath, Double> candidateFieldInfo : query.getOrderedSuspiciousFields()) {
        candidateFields.add(candidateFieldInfo.getFirst());
        fieldsSuspMap.put(candidateFieldInfo.getFirst(), candidateFieldInfo.getSecond());
      }
      double highestSusp = 0;
      if (candidateFields.size() > 0) {
        highestSusp = fieldsSuspMap.get(candidateFields.get(0));
      }
      if (highestSusp < flResult.getSuspiciousness(faultyLine)) {


        throw new RuntimeException("Suspiciousness not propagated correctly.");
      }
      RefPath answeredField = null;
      for (RefPath candidateField : candidateFields) {
        ValueGraphNode fieldValue = null;
        try {
          fieldValue = candidateField.getValue(invocPostStates);
        } catch (NoSuchFieldError | IndexOutOfBoundsException ex) {

          continue;
        }
        ValueGraphNode fieldValueOnRef = null;
        try {
          fieldValueOnRef = candidateField.getValue(invocPostStatesRef);
        } catch (NoSuchFieldError | IndexOutOfBoundsException ex) {

        }
        answeredField = candidateField;
        log("Feedback given to field " + answeredField);
        if (Objects.equals(fieldValue, fieldValueOnRef)) {
          double susp = fieldsSuspMap.get(answeredField);
          if (requireAnswerToMostSusp && susp < highestSusp) {
            throw new RuntimeException("Cannot give answer to the most suspicious fields");
          }
          log("Its value is correct (" + fieldValue + ").");
          flCore.incorporateCorrectOutputValue(query, answeredField);
        } else {
          log("Its value is incorrect. Observed (" + fieldValue + 
              "), but should be (" + fieldValueOnRef + ").");
          if (selectedInvocation.getMethodName().equals(faultyMethod)
              && flCore.getMethodInvocationNode(selectedInvocation).getStatementsExecCountMap()
                  .containsKey(faultyLine)) {
            log("Fault identified with answers to " + (nQueries + 1) + " values. Stopped.");
            return nQueries + 1;
          }
          flCore.incorporateIncorrectOutputValue(query, answeredField, false);
        }
        break;
      }
      if (answeredField == null) {

        throw new RuntimeException("Cannot answer any field in the query.");
      }
    }
    log("Maximum number of queries (" + maxNumAnswers + ") exceeded.");
    return -1;
  }
  
  private void createLogger() {
    Path logFilePath = targetProgram.getDataDirRoot().resolve(
        "feedback_directed_fl_experiment.log");
    try {
      logger = new PrintWriter(new FileOutputStream(logFilePath.toFile()));
    } catch (IOException ex) {
      System.err.println("Warning: Cannot create log file " + logFilePath);
      System.err.println("Using System.out for experiment log output.");
      logger = new PrintWriter(System.out);
    }
  }
  
  private void log(String line) {
    logger.println(line);
    logger.flush();
  }
  
  private void printSuspiciousFieldsList(
      List<Pair<RefPath, Double>> orderedSuspiciousFields) {
    int numEntriesPrinted = 0;
    for (Pair<RefPath, Double> fieldSuspInfo : orderedSuspiciousFields) {
      log(fieldSuspInfo.getFirst() 
          + ":\t" + String.format("%.5f", fieldSuspInfo.getSecond()));
      if (++numEntriesPrinted > 10) {
        break;
      }
    }
  }
  
  private void writeFaultLocalizationResult(
      FaultLocalization<SourceLocation> flResult, String fileName) {
    Path dataFilePath = targetProgram.getDataDirRoot().resolve(fileName);
    try {
      Files.write(dataFilePath, flResult.getRankedListString().getBytes());
    } catch (IOException ex) {
      System.err.println("Warining: cannot write SFL results to " + fileName);
      ex.printStackTrace();
    }
  }
  
  public static void main(String[] args) {
    File[] subjects = null;
    if (args.length == 0) {

      File subjectsRoot = new File(ExperimentDataLayout.SUBJECTS_ROOT);
      subjects = subjectsRoot.listFiles();
    } else {
      subjects = new File[args.length];
      for (int i = 0; i < args.length; ++i) {
        subjects[i] = Paths.get(ExperimentDataLayout.SUBJECTS_ROOT, args[i]).toFile();
      }
    }
    for (File subject : subjects) {
      String subjectName = subject.getName();
      if (!subject.isDirectory()) {
        warning(subjectName, subjectName + " is not a directory", null);
        continue;
      }
      System.out.println("Running experiment on subject " + subjectName);
      SubjectProgram faultyVersion = null;
      SubjectProgram goldenVersion = null;
      try {
        faultyVersion = SubjectProgram.openSubjectProgram(
            subject.toPath().resolve(ExperimentDataLayout.FAULTY_VERSION_DIR), 
            Paths.get(
                ExperimentDataLayout.DATA_ROOT, subjectName,
                ExperimentDataLayout.FAULTY_VERSION_DIR));
        goldenVersion = SubjectProgram.openSubjectProgram(
            subject.toPath().resolve(ExperimentDataLayout.GOLDEN_VERSION_DIR), 
            Paths.get(
                ExperimentDataLayout.DATA_ROOT, subjectName,
                ExperimentDataLayout.GOLDEN_VERSION_DIR));
        ExtraStatsPublisher.startPublisher();
        SimulatedFeedbackDirectedFL experimentFlImpl = 
            new SimulatedFeedbackDirectedFL(faultyVersion, goldenVersion);
        int nQueries = experimentFlImpl.localizeFault();
        if (nQueries != -1) {
          System.out.println("Fault identified with answers to " + nQueries + " values.");
        } else {
          System.out.println("Fault not identified. Max number of queries exceeded.");
        }
        ExtraStatsPublisher.writeToFile(
            faultyVersion.getDataDirRoot().resolve("extra_stats.dat"));
        ExtraStatsPublisher.shutdown();
      } catch (Throwable e) {
        warning(subjectName, "Failed to run experiment on subject " + subjectName, e);
        if (ExtraStatsPublisher.isAcceptingReports()) {
          ExtraStatsPublisher.writeToFile(
              faultyVersion.getDataDirRoot().resolve("extra_stats.dat"));
          ExtraStatsPublisher.shutdown();
        }
        continue;
      }
    }
  }
  
  public static void warning(String subjectName, String message, Throwable exc) {
    if (exc != null) {
      exc.printStackTrace();
    }
    System.err.println(message);
    System.err.println("Skipped the subject " + subjectName);
    System.err.println();
  }
}
