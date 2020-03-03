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


package anonymous.domain.enlighten.replay;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import anonymous.domain.enlighten.FeedbackDirectedFLCore;
import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.UserFeedback;
import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.FieldSelectionResult;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.util.Pair;

public class ReplayFeedbackDirectedFL {
  
  private SubjectProgram targetProgram;
  private PrintWriter logger;
  
  public ReplayFeedbackDirectedFL(SubjectProgram targetProgram) {
    this.targetProgram = targetProgram;
  }
  
  public void replayFeedbackSequence(Path sequenceFile, Path logFile) throws IOException {
    createLogger(logFile);
    SourceLocation faultyLine = targetProgram.getFaultySourceLocations().get(0);
    @SuppressWarnings("unchecked")
    List<FeedbackEntry> feedbackSequence = 
        FSTSerialization.readObjectFromFile(List.class, sequenceFile);
    FeedbackDirectedFLCore flCore = new FeedbackDirectedFLCore(targetProgram);
    int queryNum = 0;
    while (true) {
      log("\n=========================================================");
      log("Query number: " + queryNum);
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
      FieldSelectionResult query = flCore.selectNextInvocationForFeedback();
      if (query == null) {
        log("No more debugging queries generated.\nReplay terminated.");
        return;
      }
      MethodInvocationSelection selectedInvocation = query.getInvocation();
      log("Selected method invocation: " + selectedInvocation);
      log("Most suspicious fields:");
      printSuspiciousFieldsList(query.getOrderedSuspiciousFields());
      
      if (queryNum >= feedbackSequence.size()) {
        log("Replay completed.");;
        return;
      }
      FeedbackEntry feedback = feedbackSequence.get(queryNum);
      if (!selectedInvocation.equals(feedback.getInvoc())) {
        log("Selected invocation does not match recorded data");
        log("Expected invocation: " + feedback.getInvoc().toString());
        log("Actual invocation: " + selectedInvocation.toString());
        log("Replay terminated.");
        return;
      }
      RefPath answeredField = feedback.getValuePath();
      log("Feedback given to field " + answeredField);
      if (feedback.getFeedbackType().equals(UserFeedback.CORRECT)) {
        log("Its value is correct.");
        flCore.incorporateCorrectOutputValue(query, answeredField);
      } else if (feedback.getFeedbackType().equals(UserFeedback.INCORRECT)) {
        log("Its value is incorrect in the output.");
        flCore.incorporateIncorrectOutputValue(query, answeredField, feedback.isConfirmDirCovCorrect());
      } else if (feedback.getFeedbackType().equals(UserFeedback.IMPOSSIBLE_PRESTATE)) {
        log("Its value is incorrect in the intput.");
        flCore.incorporateIncorrectInputValue(query, answeredField);
      } else {
        log("Unexpected feedback type " + feedback.getFeedbackType());
        log("Replay terminated.");
        return;
      }
      ++queryNum;
    }
  }

  private void createLogger(Path logFile) {
    try {
      logger = new PrintWriter(new FileOutputStream(logFile.toFile()));
    } catch (IOException ex) {
      System.err.println("Warning: Cannot create log file " + logFile);
      System.err.println("Using System.out for replay log output.");
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
  
  public static void main(String[] args) throws Throwable {
    SubjectProgram targetProgram = SubjectProgram.openSubjectProgram(
        Paths.get("subjects/nanoxml-userstudy/faulty"), Paths.get("out/nanoxml-userstudy/faulty"));
    ReplayFeedbackDirectedFL replay = new ReplayFeedbackDirectedFL(targetProgram);
    Path recordingFile = Paths.get("C:\\experiments\\UserPoweredFL\\debug\\replay\\record.dat");
    Path replayLogFile = recordingFile.getParent().resolve("replay.log");
    replay.replayFeedbackSequence(recordingFile, replayLogFile);
  }
}
