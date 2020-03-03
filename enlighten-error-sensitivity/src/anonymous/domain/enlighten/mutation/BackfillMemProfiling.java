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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.subjectmodel.MutantSubject;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class BackfillMemProfiling {
  
  private static final Path MUTANTS_ROOT = Paths.get("../mutation/working_dir");
  private static final Path MUTANTS_DATA_ROOT = Paths.get("../mutation/out");
  private static final Path REF_IMPLS_ROOT = Paths.get("../mutation/subjects");
  
  private static Map<String, SubjectProgram> refImplsMap = new HashMap<>();
    
  public static void main(String[] args) throws Throwable {
    Path logFile = Paths.get("../mutation/bakkfill_mem_access.log");
    List<String> logLines = Files.readAllLines(logFile);
    Set<String> processed = new HashSet<>(logLines);
    PrintWriter logWriter = new PrintWriter(new FileOutputStream(logFile.toString(), true));
    for (Path mutantPath : Files.list(MUTANTS_ROOT).collect(Collectors.toList())) {
      String mutantName = mutantPath.getFileName().toString();
      try {
        if (processed.contains(mutantName)) {
          System.out.println(mutantName + " has been processed. Skipped.");
          continue;
        }
        System.out.println("Processing " + mutantName);
        Path mutantDataPath = MUTANTS_DATA_ROOT.resolve(mutantName);
        MutantVersionId mutantId = MutantVersionId.parseFromMutantVersionName(mutantName);
        SubjectProgram refImpl = getRefImpl(mutantId);
        MutantSubject mutantSubject = new MutantSubject(mutantPath, mutantDataPath, refImpl);
        RunTestsWithCoverage testRunner = new RunTestsWithCoverage(mutantSubject);
        testRunner.writeInvocationTreeForFailedTests(true, true);
        testRunner.writeInvocationTreeForFailingTests();


        File[] execTreeFiles = 
            mutantSubject.getCoverageDir().toFile().listFiles(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            if (name.toLowerCase().endsWith(".tree")) {
              return true;
            } else {
              return false;
            }
          }
        });
        Path destDir = refImpl.getRootDir().resolve("selected_mutation")
            .resolve(String.valueOf(mutantId.getMutantId())).resolve("coverage");
        for (File execTreeFile : execTreeFiles) {
          Path destFile = destDir.resolve(execTreeFile.getName());
          Files.copy(execTreeFile.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING);
        }
        logWriter.println(mutantName);
        logWriter.flush();
      } catch (Throwable ex) {
        System.err.println("Failed on " + mutantName);
      }
    }
    logWriter.close();
  }

  private static SubjectProgram getRefImpl(MutantVersionId mutantId) throws IOException {
    String subjectName = mutantId.getSubjectName();
    if (!refImplsMap.containsKey(subjectName)) {
      Path refImplDir = REF_IMPLS_ROOT.resolve(subjectName);
      SubjectProgram refImpl = SubjectProgram.openSubjectProgram(refImplDir, refImplDir);
      refImplsMap.put(subjectName, refImpl);
    }
    return refImplsMap.get(subjectName);
  }
}
