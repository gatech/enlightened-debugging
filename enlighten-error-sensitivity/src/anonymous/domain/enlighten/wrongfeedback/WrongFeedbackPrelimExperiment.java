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
import java.nio.file.Paths;

import anonymous.domain.enlighten.ExperimentDataLayout;
import anonymous.domain.enlighten.OracleFeedback;
import anonymous.domain.enlighten.SimulatedFeedbackDirectedFL;
import anonymous.domain.enlighten.UserFeedback;
import anonymous.domain.enlighten.publish.ExtraStatsPublisher;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class WrongFeedbackPrelimExperiment {
  
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
        SimulatedFeedbackDirectedFL.warning(subjectName, subjectName + " is not a directory", null);
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
        CustomFeedbackController feedbackController = new CustomFeedbackController();
        ControlledFL experimentFlImpl = 
            new ControlledFL(faultyVersion, goldenVersion, feedbackController);
        feedbackController.setFlCore(experimentFlImpl);
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
        SimulatedFeedbackDirectedFL.warning(subjectName, "Failed to run experiment on subject " + subjectName, e);
        if (ExtraStatsPublisher.isAcceptingReports()) {
          ExtraStatsPublisher.writeToFile(
              faultyVersion.getDataDirRoot().resolve("extra_stats.dat"));
          ExtraStatsPublisher.shutdown();
        }
        continue;
      }
    }
  }
  
  private static class CustomFeedbackController implements FeedbackController {
    
    private ControlledFL flCore;
    private boolean awaitingFirstIncorrectFeedback = true;
    
    public void setFlCore(ControlledFL flCore) {
      this.flCore = flCore;
    }

    @Override
    public OracleFeedback getModifiedFeedback(OracleFeedback originalFeedback) {
      if (awaitingFirstIncorrectFeedback 
          && originalFeedback.getFeedback() == UserFeedback.INCORRECT) {
        awaitingFirstIncorrectFeedback = false;
        originalFeedback.setFeedback(UserFeedback.CORRECT);
        originalFeedback.setReferenceValue(null);
        if (flCore != null) {
          flCore.log("Changing feedback to the first encounter of incorrect value to CORRECT.");
          flCore.log("RefPath: " + originalFeedback.getAnsweredPath());
        }
      }
      return originalFeedback;
    }
  }
}
