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


package anonymous.domain.enlighten.wrongfeedback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import anonymous.domain.enlighten.ExperimentDataLayout;
import anonymous.domain.enlighten.OracleFeedback;
import anonymous.domain.enlighten.SimulatedFeedbackDirectedFL;
import anonymous.domain.enlighten.UserFeedback;
import anonymous.domain.enlighten.files.RemoveDirTreeFileVisitor;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class WrongFeedbackStochastic implements FeedbackController {
  
  private static final double IGNORED_PROBABILITY = 0.01;
  private static final double WRONG_FEEDBACK_PROBABILITY = 0.2;
  
  private SubjectProgram targetProgram;
  private SubjectProgram refImpl;
  
  private FeedbackChoiceNode currentChoiceNode;
  private Deque<FeedbackChoiceNode> pendingBranchingPoints;
  private double cumulativeProb = 0;
  private boolean isLastFeedbackCorrect;
  
  private List<FeedbackChoiceNode> currentPathReplayList;
  private int replayPosition;
  
  private ControlledFL flCore;
  
  private PrintWriter logger;
  
  public WrongFeedbackStochastic(SubjectProgram targetProgram, SubjectProgram refImpl) {
    this.targetProgram = targetProgram;
    this.refImpl = refImpl;
    createLogger();
  }

  public void exploreAndLog() throws IOException {
    ArrayList<Double> pathProbabilities = new ArrayList<>();
    ArrayList<Integer> pathNQueries = new ArrayList<>();
    int pathId = 0;
    Path logDir = createPathLogDir();
    currentChoiceNode = null;
    pendingBranchingPoints = new ArrayDeque<>();
    isLastFeedbackCorrect = true;
    replayPosition = -1;
    flCore = new ControlledFL(targetProgram, refImpl, this);
    Path logFilePath = flCore.getLogFilePath();
    int nQueries = flCore.localizeFault();
    double pathProbability = getPathProbability(currentChoiceNode) 
        * (1 - WRONG_FEEDBACK_PROBABILITY);
    cumulativeProb += pathProbability;
    pathProbabilities.add(pathProbability);
    pathNQueries.add(nQueries);
    logPathStats(pathId, nQueries);
    Files.copy(logFilePath, logDir.resolve("path-" + (pathId++) + ".txt"), 
        StandardCopyOption.REPLACE_EXISTING);
    while (!pendingBranchingPoints.isEmpty()) {
      currentChoiceNode = null;
      isLastFeedbackCorrect = true;
      FeedbackChoiceNode pathToExplore = pendingBranchingPoints.removeFirst();
      currentPathReplayList = new ArrayList<>();
      FeedbackChoiceNode choiceNodeItr = pathToExplore;
      while (choiceNodeItr != null) {
        currentPathReplayList.add(choiceNodeItr);
        choiceNodeItr = choiceNodeItr.getPredecessorNode();
      }
      Collections.reverse(currentPathReplayList);
      replayPosition = 0;
      flCore = new ControlledFL(targetProgram, refImpl, this);
      try {
        nQueries = flCore.localizeFault();
      } catch (Throwable ex) {
        continue;
      }
      pathProbability = getPathProbability(currentChoiceNode) 
          * (1 - WRONG_FEEDBACK_PROBABILITY);
      cumulativeProb += pathProbability;
      pathProbabilities.add(pathProbability);
      pathNQueries.add(nQueries);
      logPathStats(pathId, nQueries);
      Files.copy(logFilePath, logDir.resolve("path-" + (pathId++) + ".txt"), 
          StandardCopyOption.REPLACE_EXISTING);
      if (cumulativeProb > 1 - IGNORED_PROBABILITY) {
    	  break;
      }
    }
  }
  
  public void log(String msg) {
    logger.println(msg);
    logger.flush();
  }

  @Override
  public OracleFeedback getModifiedFeedback(OracleFeedback originalFeedback) {
    if (replayPosition == -1) {

      FeedbackChoiceNode newNode = new FeedbackChoiceNode(originalFeedback);
      if (currentChoiceNode != null) {
        if (isLastFeedbackCorrect) {
          currentChoiceNode.setNextChoiceNode(newNode);
        } else {
          currentChoiceNode.setAlternativeNextChoiceNode(newNode);
        }
      }
      if (newNode.mayHaveAlternativeChoice()) {
        pendingBranchingPoints.addLast(newNode);
      }
      currentChoiceNode = newNode;
      isLastFeedbackCorrect = true;
      return originalFeedback;
    } else {
      FeedbackChoiceNode currentReplayNode = currentPathReplayList.get(replayPosition);
      OracleFeedback expectedFeedback = currentReplayNode.getOracleFeedback();

      if (!expectedFeedback.getInvocationRef().equals(originalFeedback.getInvocationRef())) {

        throw new RuntimeException("Unexpected non-determinism in query selection.");
      }
      if (replayPosition < currentPathReplayList.size() - 1) {
        OracleFeedback replayedFeedback = null;
        FeedbackChoiceNode nextReplayNode = currentPathReplayList.get(replayPosition + 1);
        if (nextReplayNode == currentReplayNode.getNextChoiceNode()) {
          isLastFeedbackCorrect = true;
          replayedFeedback = expectedFeedback;
        } else {
          isLastFeedbackCorrect = false;
          replayedFeedback = new OracleFeedback(expectedFeedback.getInvocationRef(), 
              expectedFeedback.getAnsweredPath(), UserFeedback.CORRECT);
          replayedFeedback.setReferenceValue(expectedFeedback.getActualValue());
          replayedFeedback.setActualValue(expectedFeedback.getActualValue());
        }
        ++replayPosition;
        return replayedFeedback;
      } else {

        OracleFeedback alternativeFeedback = new OracleFeedback(
            expectedFeedback.getInvocationRef(), 
            expectedFeedback.getAnsweredPath(), 
            UserFeedback.CORRECT);
        alternativeFeedback.setReferenceValue(expectedFeedback.getActualValue());
        alternativeFeedback.setActualValue(expectedFeedback.getActualValue());
        currentChoiceNode = currentReplayNode;
        replayPosition = -1;
        isLastFeedbackCorrect = false;
        return alternativeFeedback;
      }
    }
  }
  
  private void logPathStats(int pathId, int nQueries) {
    log("Path-" + pathId + "\t" + nQueries);
  }
  
  private double getPathProbability(FeedbackChoiceNode pathEndPoint) {
    double probability = 1;
    FeedbackChoiceNode current = pathEndPoint;
    while (current != null) {
      if (current.isInducedByWrongFeedback()) {
        probability *= WRONG_FEEDBACK_PROBABILITY;
      } else {
        FeedbackChoiceNode predecessor = current.getPredecessorNode();
        if (predecessor != null && predecessor.mayHaveAlternativeChoice()) {
          probability *= (1 - WRONG_FEEDBACK_PROBABILITY);
        }
      }
      current = current.getPredecessorNode();
    }
    return probability;
  }
  
  private void createLogger() {
    Path logPath = targetProgram.getDataDirRoot().resolve("wrong_feedback_expr_log.txt");
    try {
      logger = new PrintWriter(new FileOutputStream(logPath.toFile()));
    } catch (IOException ex) {
      System.err.println("Warning: Cannot create log file " + logPath);
      System.err.println("Using System.out for experiment log output.");
      logger = new PrintWriter(System.out);
    }
  }
  
  private Path createPathLogDir() throws IOException {
    Path logDir = targetProgram.getDataDirRoot().resolve("PathLogs");
    if (Files.exists(logDir)) {
      RemoveDirTreeFileVisitor removeTreeVisitor = new RemoveDirTreeFileVisitor();
      Files.walkFileTree(logDir, removeTreeVisitor);
    }
    Files.createDirectories(logDir);
    return logDir;
  }
  
  public static void main(String[] args) throws Throwable {
    File[] subjects = null;
    if (args.length == 0) {

      File subjectsRoot = new File(ExperimentDataLayout.SUBJECTS_ROOT);
      subjects = subjectsRoot.listFiles();
    } else {
      subjects = new File[args.length];
      for (int i = 0; i < args.length; ++i) {
        subjects[i] = Paths.get(
        		ExperimentDataLayout.SUBJECTS_ROOT, args[i]).toFile();
      }
    }
    for (File subject : subjects) {
      String subjectName = subject.getName();
      if (!subject.isDirectory()) {
        SimulatedFeedbackDirectedFL.warning(
        		subjectName, subjectName + " is not a directory", null);
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
        WrongFeedbackStochastic experiment = 
        		new WrongFeedbackStochastic(faultyVersion, goldenVersion);
        experiment.exploreAndLog();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
}
