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


package anonymous.domain.enlighten.mutation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.files.CopyDirTree;
import anonymous.domain.enlighten.subjectmodel.MutantSubject;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.wrongfeedback.WrongFeedbackStochastic;

public class WrongFeedbackExpr {
  
  private static Path baseDir = Paths.get("../mutation");
  private static Map<String, SubjectProgram> refImplsMap = new HashMap<>();
  
  public static void main(String[] args) throws Throwable {
    Path exprDataDir = Paths.get("../ErrorSensitivityData");
    Path mutantsListFile = baseDir.resolve("mutants_list.txt");
    List<String> mutantIds = Files.readAllLines(mutantsListFile);
    Collections.shuffle(mutantIds);
    for (String mutantIdStr : mutantIds) {
      System.out.println(mutantIdStr);
      Path mutantRoot = baseDir.resolve("working_dir").resolve(mutantIdStr);
      Path mutantDataRoot = baseDir.resolve("out").resolve(mutantIdStr);
      if (Files.exists(mutantDataRoot.resolve("wrong_feedback_expr_log.txt"))) {
        System.out.println("Already processed. Skipped.");
        continue;
      }
      MutantVersionId mutantId = MutantVersionId.parseFromMutantVersionName(mutantIdStr);
      SubjectProgram refImpl = getRefImpl(mutantId);
      SubjectProgram subjectProgram = new MutantSubject(mutantRoot, mutantDataRoot, refImpl);
      WrongFeedbackStochastic expr = new WrongFeedbackStochastic(subjectProgram, refImpl);
      try {
        expr.exploreAndLog();
        Path resultFile = mutantDataRoot.resolve("wrong_feedback_expr_log.txt");
        if (Files.exists(resultFile)) {
          Path resultDir = exprDataDir.resolve(mutantIdStr);
          Files.createDirectories(resultDir);
          Files.copy(resultFile, resultDir.resolve("wrong_feedback_expr_log.txt"));
          CopyDirTree copyTree = new CopyDirTree(resultDir.resolve("PathLogs"));
          Files.walkFileTree(mutantDataRoot.resolve("PathLogs"), copyTree);
        }
      } catch (Throwable ex) {
        System.out.println("Warning: failed on " + mutantIdStr);
      }
    }
  }

  private static SubjectProgram getRefImpl(MutantVersionId mutantId) throws IOException {
    String subjectName = mutantId.getSubjectName();
    if (!refImplsMap.containsKey(subjectName)) {
      Path refImplDir = baseDir.resolve("subjects").resolve(subjectName);
      SubjectProgram refImpl = SubjectProgram.openSubjectProgram(refImplDir, refImplDir);
      refImplsMap.put(subjectName, refImpl);
    }
    return refImplsMap.get(subjectName);
  }
}
