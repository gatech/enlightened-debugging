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
import java.nio.file.Paths;
import java.util.List;

import anonymous.domain.enlighten.SimulatedAlgorithmicDebugging.QueryStats;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class SimulatedAlgDbgExperiment {
  
  
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
        
        SimulatedAlgorithmicDebugging experimentFlImpl = 
            new SimulatedAlgorithmicDebugging(faultyVersion, goldenVersion, false);
        experimentFlImpl.localizeFault();
        System.out.print(subjectName);
        List<QueryStats> queryStatsList = experimentFlImpl.getQueryStats();
        int nQueries = queryStatsList.size();
        int sumMemRead = 0;
        int sumMemWrite = 0;
        for (QueryStats stats : queryStatsList) {
          sumMemRead += stats.memRead;
          sumMemWrite += stats.memWrite;
          System.out.println("\t" + stats.memRead + "\t" + stats.memWrite);
        }
        double avgMemRead = (double) sumMemRead / nQueries;
        double avgMemWrite = (double)sumMemWrite / nQueries;
        System.out.println(subjectName + "\t\t\t" + String.format("%.2f", avgMemRead) 
            + "\t" + String.format("%.2f", avgMemWrite)
            + "\t" + nQueries);
      } catch (Throwable e) {
        System.out.println("Failed to run Alg debugging on " + subjectName);
        e.printStackTrace();
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
