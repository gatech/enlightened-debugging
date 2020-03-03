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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.files.RemoveDirTreeFileVisitor;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import instr.agent.InstrumenterConfig;
import instr.agent.InstrumenterConfig.InstrumenterType;

public class CaptureInvocationStates extends ExternalProgramInvocation {

  private SubjectProgram subject;
  private String workspaceDirName;
  private String logFileName;
  private boolean profileMemAccess = true;
  private SubjectProgram refImpl;

  private Path stateCaptureTempDir;
  private Path configFilePath;

  public CaptureInvocationStates(SubjectProgram subject) throws IOException {
    this.subject = subject;
    workspaceDirName = "state_capturing";
    logFileName = "capture_invocation_states.log";
    createLogFile();
    createTempDataDir();
  }

  
  public CaptureInvocationStates(
      SubjectProgram subject, SubjectProgram refImpl) throws IOException {
    this.subject = subject;
    this.refImpl = refImpl;
    workspaceDirName = "state_capturing_on_ref";
    logFileName = "capture_invocation_states_on_ref.log";
    createLogFile();
    createTempDataDir();
  }
  
  public void setProfileMemoryAccess(boolean profileMemoryAccess) {
    profileMemAccess = profileMemoryAccess;
  }

  @Override
  protected Path getLogFilePath() {
    return subject.getDataDirRoot().resolve(logFileName);
  }

  @Override
  protected Path getWorkingDirectory() {
    return subject.getRootDir();
  }

  public MethodInvocation getInvocationStates(TestName targetTest,
      MethodName targetMethod, int targetInvocIndex) throws IOException {
    Path tempDataFile = Files.createTempFile(stateCaptureTempDir, "states_",
        null);
    ProcessBuilder stateCaptureProc = newOutputRedirectedProcessBuilder();
    List<String> commandComponents = new ArrayList<>();
    commandComponents.add("java");
    commandComponents.add("-Xss20m");
    commandComponents.add("-cp");
    List<Path> runtimePaths = new ArrayList<>();
    runtimePaths.addAll(subject.getLibPaths());
    runtimePaths.addAll(subject.getAppSourceDirs());
    runtimePaths.addAll(subject.getTestSourceDirs());
    String cpStr = concatPaths(runtimePaths, File.pathSeparator) + getFrameworkClasspathString();
    commandComponents.add(cpStr);

    commandComponents.add("-Xbootclasspath/a:" 
        + InstrumentationJars.getCallbackJarPath().toAbsolutePath());
    commandComponents.add("-javaagent:" 
        + InstrumentationJars.getInstrumenterJarPath().toAbsolutePath()
        + "=" + configFilePath.toAbsolutePath());
    commandComponents.add("-noverify");
    commandComponents.add("instr.runner.TestRunner");
    String targetTestStr = targetTest.getTestClassName() + "."
        + targetTest.getTestMethodName();
    commandComponents.add("--test_methods=" + targetTestStr);
    commandComponents
        .add("--data_dir=" + subject.getCoverageDir().toAbsolutePath());
    commandComponents.add("--capture_invocation_states");
    commandComponents
        .add("--capture_states_data_file=" + tempDataFile.toAbsolutePath());
    commandComponents.add("--capture_states_test_desc=" + targetTest);
    commandComponents.add("--capture_states_method_name=" + targetMethod);
    commandComponents.add("--capture_states_invocation_index=" + targetInvocIndex);
    if (profileMemAccess) {
      commandComponents.add("--capture_states_profile_mem");
    }
    if (refImpl != null) {
      String faultyClassName = subject.getFaultyClassName();
      commandComponents.add("--capture_states_redefine_classname=" + faultyClassName);
      commandComponents.add("--capture_states_redefine_classfile=" 
          + refImpl.getAppClassFilePath(faultyClassName).toAbsolutePath());
    }
    int retVal = 1;
    try {
      retVal = stateCaptureProc.command(commandComponents).start().waitFor();
    } catch (InterruptedException ex) {
      throw new RuntimeException("Test execution aborted.", ex);
    }
    if (!Files.exists(tempDataFile) || retVal != 0) {
      throw new RuntimeException("Error capturing invocation states.");
    }
    return FSTSerialization.readObjectFromFile(MethodInvocation.class,
        tempDataFile);
  }

  private void createLogFile() {
    Path logFilePath = getLogFilePath();
    if (Files.isRegularFile(logFilePath)) {
      try {
        Files.delete(logFilePath);
      } catch (IOException e) {
        System.err.println(
            "Warning: Failed to remove existing log file " + logFilePath + ":");
        e.printStackTrace();
      }
    }
  }

  private void createTempDataDir() throws IOException {
    stateCaptureTempDir = subject.getDataDirRoot().resolve(workspaceDirName);
    if (Files.exists(stateCaptureTempDir)) {
      try {
        Files.walkFileTree(stateCaptureTempDir, new RemoveDirTreeFileVisitor());
      } catch (IOException e) {
        if (Files.isDirectory(stateCaptureTempDir)) {
          System.err.println(
              "Warning: Failed to remove existing state-capturing temp data. Caused by:");
          e.printStackTrace();
        } else {
          throw e;
        }
      }
    }
    Files.createDirectories(stateCaptureTempDir);
    configFilePath = stateCaptureTempDir.resolve("state_capture.config");
    InstrumenterConfig instrumenterConfig = 
        new InstrumenterConfig(InstrumenterType.TRACE_INSTRUMENTER);
    instrumenterConfig.setInstrumentedPackage(subject.getAppPackage());
    instrumenterConfig.instrumentLineNumber(false);
    instrumenterConfig.instrumentMemoryAccess(false);
    instrumenterConfig.instrumentStateCapture(false);
    instrumenterConfig.storeToFile(configFilePath.toFile());
  }
}
