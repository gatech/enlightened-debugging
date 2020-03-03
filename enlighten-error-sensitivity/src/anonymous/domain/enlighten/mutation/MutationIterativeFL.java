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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.SimulatedFeedbackDirectedFL;
import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.files.CopyDirTree;
import anonymous.domain.enlighten.subjectmodel.MutantSubject;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.util.ErrorUtils;

public class MutationIterativeFL {
  


  private static final Path OUT_DIR = Paths.get("../mutation/out");
  private static final Path WORKING_DIR = Paths.get("../mutation/working_dir");
  
  private SubjectProgram subject;
  
  private String subjectName;
  private Set<MutantVersionId> processedMutants;

  public MutationIterativeFL(SubjectProgram mutationSubject) {
    subject = mutationSubject;
    subjectName = subject.getRootDir().getFileName().toString();
    processedMutants = getProcessedMutants();
  }
  
  public boolean runOnRandomVersion() {
    MutantVersionId mutant = getNextRandomMutantVersion();
    if (mutant == null) {
      System.err.println("No remaining mutants available.");
      return false;
    }
    runOnMutantVersion(mutant);
    return true;
  }
  
  public void runOnMutant(int mutantId) {
    MutantVersionId mutant = new MutantVersionId(subjectName, mutantId);
    runOnMutantVersion(mutant);
  }
  
  private void runOnMutantVersion(MutantVersionId mutant) {
    processedMutants.add(mutant);
    System.out.println("Running on mutant version " + mutant.toString());
    try {
      SubjectProgram mutantVersion = setUpMutantVersion(mutant);
      SimulatedFeedbackDirectedFL simulatedFlCore = 
          new SimulatedFeedbackDirectedFL(mutantVersion, subject);
      simulatedFlCore.requireAnswerToMostSuspicousValue();
      int numAnswers = simulatedFlCore.localizeFault();
      if (numAnswers != -1) {
        System.out.println("Fault identified with answers to " + numAnswers + " values");
      } else {
        System.out.println("Fault not identified. Max answers exceeded.");
      }
    } catch (Throwable ex) {
      System.out.println("Failed to run Fl on mutant version " + mutant.toString());
      Path failureLogPath = OUT_DIR.resolve(mutant.toString()).resolve("failure_trace.log");
      ErrorUtils.writeExceptionTrace(ex, failureLogPath);
    }
  }
  

  private MutantVersionId getNextRandomMutantVersion() {
    List<MutantVersionId> candidateMutantsId = new ArrayList<>();
    File[] allMutantDirs = 
        subject.getRootDir().resolve("selected_mutation").toFile().listFiles();
    for (File mutantDir : allMutantDirs) {
      MutantVersionId mutantId = 
          new MutantVersionId(subjectName, Integer.parseInt(mutantDir.getName()));
      if (!processedMutants.contains(mutantId)) {
        candidateMutantsId.add(mutantId);
      }
    }
    if (candidateMutantsId.size() == 0) {
      return null;
    } else {
      return candidateMutantsId.get((int) (Math.random() * candidateMutantsId.size()));
    }
  }
  
  private MutantSubject setUpMutantVersion(MutantVersionId mutantId) throws IOException {
    Path mutantDataDir = subject.getRootDir()
        .resolve("selected_mutation").resolve(String.valueOf(mutantId.getMutantId()));
    if (!Files.isDirectory(mutantDataDir)) {
      throw new IllegalArgumentException("Mutant version " + mutantId + " does not exist.");
    }
    MutantInfo mutantInfo = FSTSerialization.readObjectFromFile(
        MutantInfo.class, mutantDataDir.resolve("mutant.info"));
    MethodName faultyMethod = mutantInfo.getContainingMethod();
    SourceLocation faultySrcLoc = mutantInfo.getSourceLocation();
    

    Path mutantVersionPath = WORKING_DIR.resolve(mutantId.toString());
    Files.createDirectories(mutantVersionPath);

    SubjectProgram.Info mutantVersionInfo = SubjectProgram.Info.newSubjectProjectInfo();
    mutantVersionInfo.setAppSourcePaths(Collections.emptyList());
    mutantVersionInfo.setTestSourcePaths(Collections.emptyList());
    mutantVersionInfo.setAppPackage(subject.getAppPackage());
    String mutantClassFilesRelPath = "classes";
    List<String> libPathStrs = new ArrayList<>();
    libPathStrs.add(mutantClassFilesRelPath);
    for (Path subjectLibPath : subject.getLibPaths()) {
      libPathStrs.add(subjectLibPath.toAbsolutePath().toString());
    }
    mutantVersionInfo.setLibPaths(libPathStrs);
    ArrayList<String> faultyLocationsList = new ArrayList<>();
    faultyLocationsList.add(faultySrcLoc.toString());
    mutantVersionInfo.setProperty("faulty_lines", faultyLocationsList);
    mutantVersionInfo.setProperty("faulty_method", faultyMethod.getMethodNameSig());
    mutantVersionInfo.setProperty("faulty_class", faultyMethod.getClassName());
    mutantVersionInfo.storeToFile(mutantVersionPath.resolve("subject_program.info").toFile());

    Path mutantVersionClassDir = mutantVersionPath.resolve(mutantClassFilesRelPath);
    Path refImplClassDir = subject.getRootDir().resolve("classes");
    CopyDirTree copyTree = new CopyDirTree(mutantVersionClassDir);
    Files.walkFileTree(refImplClassDir, copyTree);
    String faultyClassRelPath = faultyMethod.getClassName().replace('.', '/') + ".class";
    Path faultyClassPath = mutantDataDir.resolve(faultyClassRelPath);
    Path destFaultyClassPath = mutantVersionClassDir.resolve(faultyClassRelPath);
    Files.copy(faultyClassPath, destFaultyClassPath, StandardCopyOption.REPLACE_EXISTING);
    

    Path mutantVersionOutPath = OUT_DIR.resolve(mutantId.toString());
    Files.createDirectories(mutantVersionOutPath);

    Path execInfoDir = mutantVersionOutPath.resolve("coverage");
    Path refImplExecData = subject.getCoverageDir();
    copyTree = new CopyDirTree(execInfoDir);
    Files.walkFileTree(refImplExecData, copyTree);
    TestOutcomes testResults = TestOutcomes.readTestOutcomesFromFile(
        refImplExecData.resolve("test-outcomes.dat"));
    Path mutantExecData = mutantDataDir.resolve("coverage");
    copyTree = new CopyDirTree(execInfoDir);
    Files.walkFileTree(mutantExecData, copyTree);
    TestOutcomes mutantTestResults = TestOutcomes.readTestOutcomesFromFile(
        mutantExecData.resolve("test-outcomes.dat"));
    for (TestName test : mutantTestResults.getTestSet()) {
      testResults.addTestOutcome(test, mutantTestResults.isPassed(test));
    }
    testResults.writeTestOutcomes(execInfoDir.resolve("test-outcomes.dat"));
    return new MutantSubject(mutantVersionPath, mutantVersionOutPath, subject);
  }
  
  private Set<MutantVersionId> getProcessedMutants() {
    Set<MutantVersionId> processedMutants = new HashSet<>();
    File[] versionOutputDirs = OUT_DIR.toFile().listFiles();
    for (File versionOutputDir : versionOutputDirs) {
      String versionName = versionOutputDir.getName();
      MutantVersionId mutantId = MutantVersionId.parseFromMutantVersionName(versionName);
      if (mutantId.getSubjectName().equals(subjectName)) {
        processedMutants.add(mutantId);
      }
    }
    return processedMutants;
  }
  
  public static void main(String[] args) throws Throwable {
    Map<String, MutationIterativeFL> runnersMap = new HashMap<>();
    Path mutantsVersionListFile = Paths.get("../mutation/mutants_list.txt");
    List<String> versionLines = Files.readAllLines(mutantsVersionListFile);
    for (String mutantName : versionLines) {
      Path mutantOutDir = Paths.get("../mutation/out/" + mutantName);
      if (Files.isDirectory(mutantOutDir)) {
        System.out.println("Mutant " + mutantName + " has been processed. Skipped");
        continue;
      }
      MutantVersionId mutantId = MutantVersionId.parseFromMutantVersionName(mutantName);
      String programName = mutantId.getSubjectName();
      int mutantNum = mutantId.getMutantId();
      MutationIterativeFL runner = runnersMap.get(programName);
      if (runner == null) {
        Path subjectPath = Paths.get("../mutation/subjects").resolve(programName);
        SubjectProgram mutationSubject = 
            SubjectProgram.openSubjectProgram(subjectPath, subjectPath);
        runner = new MutationIterativeFL(mutationSubject);
        runnersMap.put(programName, runner);
      }
      runner.runOnMutant(mutantNum);
    }
  }
}
