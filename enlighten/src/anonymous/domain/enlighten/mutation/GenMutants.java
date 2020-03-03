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

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.exec.CompileSubjectProgram;
import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.files.CopyDirTree;
import anonymous.domain.enlighten.files.LookupFilesByNameVisitor;
import anonymous.domain.enlighten.files.RemoveDirTreeFileVisitor;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.subjectmodel.SubjectUtils;
import anonymous.domain.enlighten.util.StringUtils;

public class GenMutants {
  



  
  private static final Path MUTATOR_ROOT = Paths.get("../mutation/major");
  private static final Path MUTATOR_JAVAC = MUTATOR_ROOT.resolve("lib/javac.jar").toAbsolutePath();
  private static final String MUTATED_CLASSES = "mutated_bin";
  private static final int MUTANTS_PER_FILE = 10;
  private static final int MUTANTS_TEST_FAIL_PER_FILE = 10;
  
  private SubjectProgram subject;
  private Path classFilePath;
  private Path classFileBackupPath;
  private Path mutantsRoot;
  private Path testMutantsProjectPath;
  private Path selectedMutantsDir;
  
  private Map<SourceLocation, Set<TestName>> reversedCovMap = new HashMap<>();
  
  
  public GenMutants(SubjectProgram subject) throws IOException, InterruptedException {
    this.subject = subject;
    classFilePath = subject.getRootDir().resolve("classes");
    classFileBackupPath = subject.getRootDir().resolve("classes_backup");
    mutantsRoot = subject.getRootDir().resolve("mutants");
    testMutantsProjectPath = subject.getRootDir().resolve("test_mutants");
    selectedMutantsDir = subject.getRootDir().resolve("selected_mutation");
    initSubject();
  }
  
  public void prepareMutants() throws IOException, InterruptedException {
    if (Files.isDirectory(selectedMutantsDir)) {
      System.err.println("Selected mutations already exists");
      return;
    }
    Files.createDirectories(selectedMutantsDir);
    int totalMutantsSelected = 0;
    Map<Path, List<Path>> srcMap = SubjectUtils.readSrcFilePathMap(subject);
    for (Path srcFolder : srcMap.keySet()) {
      List<Path> srcFileList = srcMap.get(srcFolder);
      for (Path srcFileRelPath : srcFileList) {
        List<MutantInfo> mutantInfoList = null;
        try {
          mutantInfoList = genMutantsForSrcFile(srcFolder, srcFileRelPath);
        } catch (Throwable ex) {

          System.err.println("Warning: cannot generate mutants for " + srcFileRelPath);
          continue;
        }
        System.out.println(
            srcFileRelPath.toString() + " total number of mutations: " + mutantInfoList.size());
        int selectedMutantsPerFile = 0;
        int testFailuresPerFile = 0;
        while (selectedMutantsPerFile < MUTANTS_PER_FILE 
            && testFailuresPerFile < MUTANTS_TEST_FAIL_PER_FILE && mutantInfoList.size() > 0) {
          MutantInfo mutant = randomlyPickOut(mutantInfoList);
          Set<TestName> affectedTests = getAffectedTests(mutant);
          if (affectedTests.size() == 0) {
            continue;
          }
          System.out.println("Analyzing mutant #" + mutant.getSerialNum());
          System.out.println("Number of affected tests: " + affectedTests.size());
          Path mutantDir = mutantsRoot.resolve(String.valueOf(mutant.getSerialNum()));
          Path mutantSrcFile = mutantDir.resolve(srcFileRelPath);
          if (!compileMutantSourceFile(mutantSrcFile)) {
            System.err.println("Failed to compile this mutant " + mutant);
            continue;
          }
          int failedTestsNum = 0;
          try {
            failedTestsNum = runAffectedTestsWithMutant(mutant, mutantDir, affectedTests);
          } catch (Throwable ex) {
            System.err.println("Failed to execute affected tests for mutant " + mutant);
            ++testFailuresPerFile;
            continue;
          }
          if (failedTestsNum > 0) {
            ++selectedMutantsPerFile;
            mutant.setGlobalId(++totalMutantsSelected);
            saveMutantData(mutant);
            System.out.println("Mutant saved: " + mutant.toString());
          }
        }
      }
    }
  }





  private List<MutantInfo> genMutantsForSrcFile(Path srcFolder, Path srcFileRelPath) 
      throws IOException, InterruptedException {
    if (Files.exists(mutantsRoot)) {
      Files.walkFileTree(mutantsRoot, new RemoveDirTreeFileVisitor());
    }
    List<String> cmdParts = new ArrayList<>();
    cmdParts.add("java");
    cmdParts.add("-Dmajor.export.mutants=true");
    cmdParts.add("-Xbootclasspath/p:" + MUTATOR_JAVAC);
    cmdParts.add("-jar");
    cmdParts.add(MUTATOR_JAVAC.toString());
    cmdParts.add("-Xlint:none");
    cmdParts.add("-XMutator:ALL");
    cmdParts.add("-d");
    cmdParts.add(MUTATED_CLASSES);
    cmdParts.add("-cp");
    List<Path> subjectCps = new ArrayList<>();
    subjectCps.addAll(subject.getAppSourceDirs());
    subjectCps.addAll(subject.getTestSourceDirs());
    subjectCps.addAll(subject.getLibPaths());
    List<String> subjectCpStrs = new ArrayList<>();
    for (Path cpEntry : subjectCps) {
      subjectCpStrs.add(cpEntry.toAbsolutePath().toString());
    }
    String cpStr = StringUtils.concat(subjectCpStrs, File.pathSeparator);
    cmdParts.add(cpStr);
    cmdParts.add(srcFolder.resolve(srcFileRelPath).toAbsolutePath().toString());
    Path workingDir = subject.getRootDir();
    ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true)
        .redirectOutput(subject.getRootDir().resolve("run_mutation_compiler.log").toFile())
        .directory(workingDir.toFile()).command(cmdParts);
    Process mutationCompilerProcess = pb.start();
    int retVal = mutationCompilerProcess.waitFor();
    if (retVal != 0) {
      throw new RuntimeException(
          "Mutation compiler failed on subject at " + subject.getRootDir());
    }
    Path mutantsLogFile = workingDir.resolve("mutants.log");
    return createMutantInfo(mutantsLogFile, srcFileRelPath);
  }
  
  private List<MutantInfo> createMutantInfo(Path mutantsLogFile, Path srcFileRelPath) 
      throws IOException {
    List<String> logLines = Files.readAllLines(mutantsLogFile);
    List<MutantInfo> mutantInfoList = new ArrayList<>();
    for (String logLine : logLines) {
      if (logLine.length() > 0) {
        MutantInfo mutantInfo = new MutantInfo(logLine, srcFileRelPath.toString());
        if (!mutantInfo.getType().equals("STD")) {
          mutantInfoList.add(mutantInfo);
        }
      }
    }
    return mutantInfoList;
  }
  
  
  private void initSubject() throws IOException, InterruptedException {
    if (!Files.isDirectory(classFilePath)) {

      new CompileSubjectProgram(subject).compile();
      if (!Files.isDirectory(classFilePath)) {
        Files.createDirectories(classFilePath);
      }

      List<Path> sourcePaths = new ArrayList<>();
      sourcePaths.addAll(subject.getAppSourceDirs());
      sourcePaths.addAll(subject.getTestSourceDirs());
      Set<Path> nonResourceFiles = new HashSet<>();
      for (Path sourcePath : sourcePaths) {
        LookupFilesByNameVisitor lookupClassFiles = new LookupFilesByNameVisitor(".*\\.class");
        Files.walkFileTree(sourcePath, lookupClassFiles);
        List<Path> classFiles = lookupClassFiles.getMatchedFilesPaths();
        nonResourceFiles.addAll(classFiles);
        for (Path classFile : classFiles) {
          Path targetPath = classFilePath.resolve(sourcePath.relativize(classFile));
          Files.createDirectories(targetPath.getParent());
          Files.copy(classFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        LookupFilesByNameVisitor lookupJavaFiles = new LookupFilesByNameVisitor(".*\\.java");
        Files.walkFileTree(sourcePath, lookupJavaFiles);
        nonResourceFiles.addAll(lookupJavaFiles.getMatchedFilesPaths());
      }

      for (Path sourcePath : sourcePaths) {
        LookupFilesByNameVisitor lookupAllFiles = new LookupFilesByNameVisitor(".*");
        Files.walkFileTree(sourcePath, lookupAllFiles);
        for (Path filePath : lookupAllFiles.getMatchedFilesPaths()) {
          if (!nonResourceFiles.contains(filePath)) {
            Path targetPath = classFilePath.resolve(sourcePath.relativize(filePath));
            Files.createDirectories(targetPath.getParent());
            Files.copy(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }

    RunTestsWithCoverage testsRunner = new RunTestsWithCoverage(subject);
    Path covDir = subject.getCoverageDir();
    if (!Files.exists(covDir)) {
      testsRunner.generateCoverageData();
    }

    TestOutcomes testResult = testsRunner.getTestOutcomes();
    for (TestName test : testResult.getTestSet()) {
      if (!testResult.isPassed(test)) {
        throw new RuntimeException("Test " + test + " failed.");
      }
    }

    Map<TestName, SourceLocationCoverage> srcCovMap = 
        testsRunner.getSourceLocationCoverageMatrix();
    for (TestName test : srcCovMap.keySet()) {
      SourceLocationCoverage testCov = srcCovMap.get(test);
      for (SourceLocation srcLoc : testCov.getCoverage()) {
        Set<TestName> coveringTests = reversedCovMap.get(srcLoc);
        if (coveringTests == null) {
          coveringTests = new HashSet<>();
          reversedCovMap.put(srcLoc, coveringTests);
        }
        coveringTests.add(test);
      }
    }

    if (!Files.isDirectory(classFileBackupPath)) {
      Files.createDirectories(classFileBackupPath);
    }

    Path mutatedBinPath = subject.getRootDir().resolve(MUTATED_CLASSES);
    if (!Files.isDirectory(mutatedBinPath)) {
      Files.createDirectories(mutatedBinPath);
    }

    if (!Files.isDirectory(testMutantsProjectPath)) {
      Files.createDirectories(testMutantsProjectPath);
      Path projectInfoPath = testMutantsProjectPath.resolve("subject_program.info");
      SubjectProgram.Info projectInfo = SubjectProgram.Info.newSubjectProjectInfo();
      projectInfo.setAppSourcePaths(Collections.emptyList());
      projectInfo.setTestSourcePaths(Collections.emptyList());
      projectInfo.setAppPackage(subject.getAppPackage());
      List<String> libPathStrs = new ArrayList<>();
      libPathStrs.add("../classes");
      for (String subjectLibPath : subject.getProgramInfo().getLibPaths()) {
        libPathStrs.add("../" + subjectLibPath);
      }
      projectInfo.setLibPaths(libPathStrs);
      projectInfo.storeToFile(projectInfoPath.toFile());
    }
  }
  
  private Set<TestName> getAffectedTests(MutantInfo mutant) {
    SourceLocation srcLoc = mutant.getSourceLocation();
    if (reversedCovMap.containsKey(srcLoc)) {
      return Collections.unmodifiableSet(reversedCovMap.get(srcLoc));
    } else {
      return Collections.emptySet();
    }
  }
  


  private MutantInfo randomlyPickOut(List<MutantInfo> mutantsList) {
    int randomIndex = (int) (Math.random() * mutantsList.size());
    MutantInfo pickedInstance = mutantsList.remove(randomIndex);
    return pickedInstance;
  }
  
  private boolean compileMutantSourceFile(Path srcFullPath) {
    List<String> cmdParts = new ArrayList<>();
    cmdParts.add("javac");
    cmdParts.add("-encoding");
    cmdParts.add("utf-8");
    List<String> classPathEntries = new ArrayList<>();
    classPathEntries.add(classFilePath.toString());
    for (Path libPath : subject.getLibPaths()) {
      classPathEntries.add(libPath.toString());
    }
    String cpStr = StringUtils.concat(classPathEntries, File.pathSeparator);
    cmdParts.add("-cp");
    cmdParts.add(cpStr);
    cmdParts.add(srcFullPath.toString());
    try {
      int retVal = new ProcessBuilder().command(cmdParts).start().waitFor();
      if (retVal != 0) {
        return false;
      }
    } catch (InterruptedException | IOException e) {
      return false;
    }
    return true;
  }
  
  private int runAffectedTestsWithMutant(
      MutantInfo mutant, Path mutantDir, Set<TestName> affectedTests) throws IOException {
    int failedTestNum = 0;
    SubjectProgram testMutantProject = SubjectProgram.openSubjectProgram(
        testMutantsProjectPath, testMutantsProjectPath);
    Path testMutantCovDir = testMutantProject.getCoverageDir();
    if (Files.isDirectory(testMutantCovDir)) {
      Files.walkFileTree(testMutantCovDir, new RemoveDirTreeFileVisitor());
    }
    LookupFilesByNameVisitor lookupClassfiles = new LookupFilesByNameVisitor(".*\\.class");
    Files.walkFileTree(mutantDir, lookupClassfiles);
    List<Path> changedClassFiles = lookupClassfiles.getMatchedFilesPaths();
    try {
      for (Path changedClassFile : changedClassFiles) {
        Path classFileRelPath =mutantDir.relativize(changedClassFile);
        Path destClassFile = classFilePath.resolve(classFileRelPath);
        Path backupClassFile = classFileBackupPath.resolve(classFileRelPath);
        Files.createDirectories(backupClassFile.getParent());
        Files.copy(destClassFile, backupClassFile, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(changedClassFile, destClassFile, StandardCopyOption.REPLACE_EXISTING);
      }
      MutationTestRunner testRunner = new MutationTestRunner(testMutantProject, affectedTests);
      testRunner.generateCoverageData();
      TestOutcomes testResult = testRunner.getTestOutcomes();
      TestName anyFailedTest = null;
      for (TestName test : testResult.getTestSet()) {
        if (!testResult.isPassed(test)) {
          ++failedTestNum;
          if (anyFailedTest == null) {
            anyFailedTest = test;
          }
        }
      }
      if (failedTestNum > 0) {
        testRunner.writeCoverageFiles(true);
        testRunner.writeInvocationTreeForFailedTests(true);
        testRunner.generateCoverageData();

        ExecutionProfile failureInvocTree = testRunner.readExecutionProfile(anyFailedTest);
        MethodName faultyMethod = searchSourceLine(
            failureInvocTree.getInvocationTreeRoot(), mutant.getSourceLocation());
        if (faultyMethod == null) {
          throw new RuntimeException("Failure test didn't cover faulty line.");
        }
        mutant.setContainingMethod(faultyMethod);
      }
    } catch (Throwable ex) {
      throw ex;
    } finally {
      for (Path changedClassFile : changedClassFiles) {
        Path classFileRelPath =mutantDir.relativize(changedClassFile);
        Path destClassFile = classFilePath.resolve(classFileRelPath);
        Path backupClassFile = classFileBackupPath.resolve(classFileRelPath);
        if (Files.exists(backupClassFile)) {
          Files.copy(backupClassFile, destClassFile, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
    return failedTestNum;
  }
  
  private void saveMutantData(MutantInfo mutant) throws IOException {
    Path mutantDir = mutantsRoot.resolve(String.valueOf(mutant.getSerialNum()));
    Path mutantDataDir = selectedMutantsDir.resolve(String.valueOf(mutant.getGlobalId()));
    Files.createDirectories(mutantDataDir);

    Path mutantInfoFile = mutantDataDir.resolve("mutant.info");
    FSTSerialization.writeObjectTofile(MutantInfo.class, mutantInfoFile, mutant);

    Path mutationSourceFullPath = mutantDir.resolve(mutant.getSrcFilePath());
    Path destSourcePath = mutantDataDir.resolve(mutant.getSrcFilePath());
    Files.createDirectories(destSourcePath.getParent());
    Files.copy(mutationSourceFullPath, destSourcePath);

    Path faultyClassRelPath = Paths.get(
        mutant.getContainingMethod().getClassName().replace('.', '/') + ".class");
    Path classFileFullPath = mutantDir.resolve(faultyClassRelPath);
    Path destClassFilePath = mutantDataDir.resolve(faultyClassRelPath);
    Files.createDirectories(destClassFilePath.getParent());
    Files.copy(classFileFullPath, destClassFilePath);

    Path covDataDir = testMutantsProjectPath.resolve("coverage");
    Path destDataDir = mutantDataDir.resolve("coverage");
    CopyDirTree copyTree = new CopyDirTree(destDataDir);
    Files.walkFileTree(covDataDir, copyTree);
  }
  
  private static MethodName searchSourceLine(MethodInvocation treeRoot, SourceLocation sourceLine) {
    if (treeRoot.getStatementsExecCountMap().containsKey(sourceLine)) {
      return treeRoot.getMethodName();
    }
    for (MethodInvocation child : treeRoot.getEnclosedInvocations()) {
      MethodName name = searchSourceLine(child, sourceLine);
      if (name != null) {
        return name;
      }
    }
    return null;
  }
  
  public static void main(String[] args) {
    try {
      Path subjectPath = Paths.get(
          "C:\\experiments\\UserPoweredFL\\mutation\\base_versions\\commons-lang-3_2");
      SubjectProgram test = SubjectProgram.openSubjectProgram(subjectPath, subjectPath);
      GenMutants genMutants = new GenMutants(test);
      genMutants.prepareMutants();
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
    
    
  }
}
