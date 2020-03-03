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


package anonymous.domain.enlighten.exec;

import instr.agent.InstrumenterConfig;
import instr.agent.InstrumenterConfig.InstrumenterType;
import instr.runner.TestRunner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodCoverage;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.files.RemoveDirTreeFileVisitor;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class RunTestsWithCoverage extends ExternalProgramInvocation {

  private static final String LOG_FILE_NAME = "run_coverage_out.log";
  
  private SubjectProgram subjectProgram;
  private Set<TestName> excludedTests;
  
  private boolean writeCoverage = true;
  private boolean writeInvocationTreeForFailures = true;
  private boolean writeMemAccessWithInvocationTree = false;
  private boolean writeTrace = false;
  
  private TestOutcomes testOutcomes;
  private Map<TestName, MethodCoverage> methodCoverageMatrix;
  private Map<TestName, SourceLocationCoverage> sourceCoverageMatrix;
  
  @SuppressWarnings("unchecked")
  public RunTestsWithCoverage(SubjectProgram subject) {
    subjectProgram = subject;
    Path excludedTestsInfoPath = subjectProgram.getRootDir().resolve("excluded_tests");
    if (Files.exists(excludedTestsInfoPath)) {
      try {
        excludedTests = FSTSerialization.readObjectFromFile(HashSet.class, excludedTestsInfoPath);
      } catch (IOException e) {
        System.err.println(excludedTestsInfoPath + " exists but cannot be read.");
      }
    } else {
      excludedTests = Collections.emptySet();
    }
  }
  
  public void writeCoverageFiles(boolean write) {
    writeCoverage = write;
  }
  
  public void writeInvocationTreeForFailedTests(boolean write) {
    writeInvocationTreeForFailedTests(write, false);
  }
  
  public void writeInvocationTreeForFailedTests(boolean writeTree, boolean writeMemAccess) {
    writeInvocationTreeForFailures = writeTree;
    writeMemAccessWithInvocationTree = writeMemAccess;
  }
  
  public void writeTraceFiles(boolean write) {
    writeTrace = write;
  }

  public void generateCoverageData() throws IOException {
    Path logFilePath = getLogFilePath();
    if (Files.isRegularFile(logFilePath)) {
      try {
        Files.delete(logFilePath);
      } catch (IOException e) {
        System.err.println("Warning: Failed to remove existing log file " + logFilePath + ":");
        e.printStackTrace();
      }
    }
    Path subjectCovDataDir = subjectProgram.getCoverageDir();
    if (Files.exists(subjectCovDataDir)) {
      try {
        Files.walkFileTree(subjectCovDataDir, new RemoveDirTreeFileVisitor());
      } catch (IOException e) {
        System.err.println("Warning: exception thrown while removing existing coverage data:");
        e.printStackTrace();
      }
    }
    Files.createDirectories(subjectCovDataDir);
    Path instrumenterConfigFile = subjectCovDataDir.resolve("cov_runner_instrumenter.config");
    InstrumenterConfig instrumenterConfig = 
        new InstrumenterConfig(InstrumenterType.TRACE_INSTRUMENTER);
    instrumenterConfig.setInstrumentedPackage(subjectProgram.getAppPackage());
    instrumenterConfig.instrumentLineNumber(true);
    instrumenterConfig.instrumentMemoryAccess(false);
    instrumenterConfig.instrumentStateCapture(false);
    instrumenterConfig.storeToFile(instrumenterConfigFile.toFile());

    List<Class<?>> testClasses = new ArrayList<>(subjectProgram.listTestClasses());
    StringBuilder testClassesStrBuilder = new StringBuilder();
    for (int i = 0; i < testClasses.size(); ++i) {
      testClassesStrBuilder.append(testClasses.get(i).getName());
      if (i != testClasses.size() - 1) {
        testClassesStrBuilder.append(',');
      }
    }
    ProcessBuilder runCovProc = newOutputRedirectedProcessBuilder();
    List<String> commandComponents = new ArrayList<>();
    commandComponents.add("java");
    commandComponents.add("-cp");
    List<Path> runtimePaths = new ArrayList<>();
    runtimePaths.addAll(subjectProgram.getLibPaths());
    runtimePaths.addAll(subjectProgram.getAppSourceDirs());
    runtimePaths.addAll(subjectProgram.getTestSourceDirs());


    String classpath = concatPaths(runtimePaths, File.pathSeparator) 
        + getFrameworkClasspathString();
    commandComponents.add(classpath);
    if (writeCoverage || writeTrace) {
      commandComponents.add("-Xbootclasspath/a:" 
          + InstrumentationJars.getCallbackJarPath().toAbsolutePath());
      commandComponents.add("-javaagent:" 
          + InstrumentationJars.getInstrumenterJarPath().toAbsolutePath()
          + "=" + instrumenterConfigFile.toAbsolutePath());
      commandComponents.add("-noverify");
    }
    commandComponents.add("instr.runner.TestRunner");
    commandComponents.add("--testclasses=" + testClassesStrBuilder.toString());
    commandComponents.add("--data_dir=" + subjectCovDataDir.toAbsolutePath());
    commandComponents.add("--write_test_outcomes");
    if (writeCoverage) {
      commandComponents.add("--write_coverage");
    }
    if (writeTrace) {
      commandComponents.add("--write_trace");
    }
    int retVal = 1;
    try {
      retVal = runCovProc.command(commandComponents).start().waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException("Test execution aborted.", e);
    }
    if (retVal != 0) {
      throw new RuntimeException(
          "Error generating coverage data for subject program at " + subjectProgram.getRootDir());
    }
    
    if (writeInvocationTreeForFailures) {
      writeInvocationTreeForFailingTests();
    }
  }
  
  public void writeInvocationTreeForFailingTests() throws IOException {
    Path subjectCovDataDir = subjectProgram.getCoverageDir();
    Path instrumenterConfigFile = subjectCovDataDir.resolve("cov_runner_instrumenter.config");
    InstrumenterConfig instrumenterConfig = 
        new InstrumenterConfig(InstrumenterType.TRACE_INSTRUMENTER);
    instrumenterConfig.setInstrumentedPackage(subjectProgram.getAppPackage());
    instrumenterConfig.instrumentLineNumber(true);
    instrumenterConfig.instrumentMemoryAccess(writeMemAccessWithInvocationTree);
    instrumenterConfig.instrumentStateCapture(true);
    instrumenterConfig.storeToFile(instrumenterConfigFile.toFile());
    readTestOutcomes();
    Set<String> testMethodsToWriteTree = new HashSet<>();
    for (TestName test: testOutcomes.getTestSet()) {
      if (!testOutcomes.isPassed(test)) {
        testMethodsToWriteTree.add(test.getTestClassName() + "." + test.getTestMethodName());
      }
    }
    for (String testLongName : testMethodsToWriteTree) {
      ProcessBuilder genInvocationTreeProc = newOutputRedirectedProcessBuilder();
      List<String> commandComponents = new ArrayList<>();
      commandComponents.add("java");
      commandComponents.add("-cp");
      List<Path> runtimePaths = new ArrayList<>();
      runtimePaths.addAll(subjectProgram.getLibPaths());
      runtimePaths.addAll(subjectProgram.getAppSourceDirs());
      runtimePaths.addAll(subjectProgram.getTestSourceDirs());


      String classpath = concatPaths(runtimePaths, File.pathSeparator) 
          + getFrameworkClasspathString();
      commandComponents.add(classpath);
      commandComponents.add("-Xbootclasspath/a:" 
          + InstrumentationJars.getCallbackJarPath().toAbsolutePath());
      commandComponents.add("-javaagent:" 
          + InstrumentationJars.getInstrumenterJarPath().toAbsolutePath()
          + "=" + instrumenterConfigFile.toAbsolutePath());
      commandComponents.add("-noverify");
      commandComponents.add("instr.runner.TestRunner");
      commandComponents.add("--test_methods=" + testLongName);
      commandComponents.add("--data_dir=" + subjectCovDataDir.toAbsolutePath());
      commandComponents.add("--write_invocation_tree");
      int retVal = 1;
      try {
        retVal = genInvocationTreeProc.command(commandComponents).start().waitFor();
      } catch (InterruptedException e) {
        throw new RuntimeException("Test execution aborted.", e);
      }
      if (retVal != 0) {
        throw new RuntimeException("Error generating invocation tree data");
      }
    }
  }
  
  public TestOutcomes getTestOutcomes() throws IOException {
    if (testOutcomes == null) {
      readTestOutcomes();
    }
    return testOutcomes;
  }
  
  public Map<TestName, MethodCoverage> getMethodCoverageMatrix() throws IOException {
    if (methodCoverageMatrix == null) {
      readTestsCoverage();
    }
    return methodCoverageMatrix;
  }
  
  public Map<TestName, SourceLocationCoverage> getSourceLocationCoverageMatrix() 
      throws IOException {
    if (sourceCoverageMatrix == null) {
      readTestsCoverage();
    }
    return sourceCoverageMatrix;
  }
  
  public ExecutionProfile readExecutionProfile(TestName test) throws IOException {
    Path profilePath = subjectProgram.getCoverageDir().resolve(test.getDescription() + ".tree");
    if (Files.exists(profilePath)) {
      return ExecutionProfile.readFromDataFile(profilePath);
    } else {
      return null;
    }
  }
  
  @Override
  protected Path getLogFilePath() {
    return subjectProgram.getDataDirRoot().resolve(LOG_FILE_NAME);
  }
  
  @Override
  protected Path getWorkingDirectory() {
    return subjectProgram.getRootDir();
  }
  
  private void readTestOutcomes() throws IOException {
    testOutcomes = TestOutcomes.readTestOutcomesFromFile(
        subjectProgram.getCoverageDir().resolve(TestRunner.TEST_OUTCOME_FILE_NAME));
    for (TestName test : excludedTests) {
      testOutcomes.removeTestOutcome(test);
    }
  }
  
  private void readTestsCoverage() throws IOException {
    methodCoverageMatrix = new HashMap<>();
    File[] covDataFiles = subjectProgram.getCoverageDir().toFile().listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".method.cov");
      }
    });
    for (File covDataFile : covDataFiles) {
      String dataFileName = covDataFile.getName();

      String testDescription = dataFileName.substring(0, dataFileName.length() - 11);
      TestName test = TestName.parseFromDescription(testDescription);
      if (!excludedTests.contains(test)) {
        MethodCoverage coverage = 
            FSTSerialization.readObjectFromFile(MethodCoverage.class, covDataFile.toPath());
        coverage.filterCoverageByAppClasses(subjectProgram);
        methodCoverageMatrix.put(test, coverage);
      }
    }
    
    sourceCoverageMatrix = new HashMap<>();
    covDataFiles = subjectProgram.getCoverageDir().toFile().listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".line.cov");
      }
    });
    for (File covDataFile : covDataFiles) {
      String dataFileName = covDataFile.getName();

      String testDescription = dataFileName.substring(0, dataFileName.length() - 9);
      TestName test = TestName.parseFromDescription(testDescription);
      if (!excludedTests.contains(test)) {
        SourceLocationCoverage coverage = 
            FSTSerialization.readObjectFromFile(SourceLocationCoverage.class, covDataFile.toPath());
        coverage.filterCoverageByAppClasses(subjectProgram);
        sourceCoverageMatrix.put(test, coverage);
      }
    }
  }
}
