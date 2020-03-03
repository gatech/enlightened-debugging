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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import anonymous.domain.enlighten.ExperimentDataLayout;
import anonymous.domain.enlighten.files.CopyDirTree;

public class CollectExperimentData {
  
  public static void main(String[] args) throws Throwable {
    File[] subjects = null;
    if (args.length == 0) {

      File subjectsRoot = new File(ExperimentDataLayout.DATA_ROOT);
      subjects = subjectsRoot.listFiles();
    } else {
      subjects = new File[args.length];
      for (int i = 0; i < args.length; ++i) {
        subjects[i] = Paths.get(ExperimentDataLayout.DATA_ROOT, args[i]).toFile();
      }
    }
    Path resultRoot = Paths.get("ErrorSensitivityData");
    Files.createDirectories(resultRoot);
    for (File subject : subjects) {
      String subjectName = subject.getName();
      if (!subject.isDirectory()) {
        System.err.println(subject + " is not a directory");
        continue;
      }
      System.out.println("Subject: " + subjectName);
      Path dataFilePath = subject.toPath().resolve("faulty/wrong_feedback_expr_log.txt");
      if (!Files.exists(dataFilePath)) {
        System.err.println(dataFilePath + " does not exist.");
      } else {
        Path subjectResultDir = resultRoot.resolve(subjectName);
        Files.createDirectories(subjectResultDir);
        Files.copy(dataFilePath, subjectResultDir.resolve("wrong_feedback_expr_log.txt"));
        Path pathLogData = dataFilePath.getParent().resolve("PathLogs");
        CopyDirTree copyTree = new CopyDirTree(subjectResultDir.resolve("PathLogs"));
        Files.walkFileTree(pathLogData, copyTree);
      }
    }
  }

}
