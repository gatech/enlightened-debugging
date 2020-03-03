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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import instr.agent.InstrumenterConfig;


public class LaunchDebuggee extends ExternalProgramInvocation {
  
  private static final Pattern debuggeePortInfoPattern = Pattern.compile(
      "Waiting for debugger connection at: dt_socket (\\d+)");
  

  private static Process livingDebuggee;
  
  private SubjectProgram subject;
  
  public LaunchDebuggee(SubjectProgram subject) {
    this.subject = subject;
  }
  
  public int launch(MethodInvocationSelection invocationSelection) 
      throws IOException, InterruptedException {
    if (getLivingDebuggeeProcess() != null) {
      throw new RuntimeException(
          "Please terminate the existing debuggee process before launching a new one.");
    }
    TestName targetTest = invocationSelection.getTestName();
    MethodName methodForInspection = invocationSelection.getMethodName();
    int invocationIndex = invocationSelection.getInvocationIndex();
    Path configFilePath = subject.getDataDirRoot().resolve("setup_debuggee.config");
    InstrumenterConfig config = new InstrumenterConfig(
        InstrumenterConfig.InstrumenterType.DEBUGGEE_INSTRUMENTER);
    config.setDebuggedInvocation(methodForInspection.toString() + "#" + invocationIndex);
    config.storeToFile(configFilePath.toFile());
    List<String> cmdParts = new ArrayList<>();
    
    cmdParts.add("java");



    cmdParts.add(
        "-agentlib:jdwp=transport=dt_socket,"
        + "server=y,onthrow=instr.callback.SuspendForDebuggerSignal,launch=echo");
    cmdParts.add("-cp");
    List<Path> runtimePaths = new ArrayList<>();
    runtimePaths.addAll(subject.getLibPaths());
    runtimePaths.addAll(subject.getAppSourceDirs());
    runtimePaths.addAll(subject.getTestSourceDirs());
    String classpath = concatPaths(runtimePaths, File.pathSeparator) 
        + ExternalProgramInvocation.getFrameworkClasspathString();
    cmdParts.add(classpath);
    cmdParts.add("-Xbootclasspath/a:"
        + InstrumentationJars.getCallbackJarPath().toAbsolutePath());
    cmdParts.add("-javaagent:"
        + InstrumentationJars.getInstrumenterJarPath().toAbsolutePath()
        + "=" + configFilePath.toAbsolutePath());
    cmdParts.add("-noverify");
    cmdParts.add("instr.runner.RunSingleTest");
    cmdParts.add(targetTest.getTestMethodLongName());
    Process debuggee = new ProcessBuilder().redirectErrorStream(true).command(cmdParts).start();
    BufferedReader debuggeeOutput = new BufferedReader(
        new InputStreamReader(debuggee.getInputStream()));
    PrintWriter debuggeeOutRedirection = new PrintWriter(
        new FileOutputStream(getLogFilePath().toFile()));
    String outputLine = null;
    int listeningPort = -1;
    while ((outputLine = debuggeeOutput.readLine()) != null) {
      debuggeeOutRedirection.println(outputLine);
      debuggeeOutRedirection.flush();
      Matcher outputLineMatcher = debuggeePortInfoPattern.matcher(outputLine);
      if (outputLineMatcher.find()) {
        listeningPort = Integer.parseInt(outputLineMatcher.group(1));
        break;
      }
    }
    debuggeeOutRedirection.close();
    if (listeningPort != -1) {
      livingDebuggee = debuggee;
      return listeningPort;
    } else {
      if (debuggee.isAlive()) {
        debuggee.destroyForcibly().waitFor();
      }
      throw new RuntimeException(
          "Failed to suspend debuggee process at the entry of the specified invocation.");
    }
  }
  
  public static Process getLivingDebuggeeProcess() {
    if (livingDebuggee != null) {
      if (!livingDebuggee.isAlive()) {
        livingDebuggee = null;
      }
    }
    return livingDebuggee;
  }

  @Override
  protected Path getLogFilePath() {
    return subject.getDataDirRoot().resolve("debuggee_diagnostic_output.txt");
  }

  @Override
  protected Path getWorkingDirectory() {
    return subject.getDataDirRoot();
  }
}
