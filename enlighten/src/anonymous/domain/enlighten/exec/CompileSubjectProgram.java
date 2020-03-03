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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.subjectmodel.SubjectUtils;

public class CompileSubjectProgram extends ExternalProgramInvocation {
  
  private static final String LOG_FILE_NAME = "compilation_out.log";
  
  private SubjectProgram subjectProgram;
  
  public CompileSubjectProgram(SubjectProgram subject) {
    subjectProgram = subject;
  }

  
  public void compile() throws IOException, InterruptedException {
    Path logFilePath = getLogFilePath();
    if (Files.isRegularFile(logFilePath)) {
      try {
        Files.delete(logFilePath);
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Warning: Failed to remove existing log file " + logFilePath);
      }
    }
    String classPathStr = concatPaths(subjectProgram.getLibPaths(), File.pathSeparator);
    List<Path> sourceFiles = readSourceFilePaths();
    

    if (sourceFiles.size() > 0) {
      String[] compileAppSourceCommand = 
          getCompileCommand(classPathStr, sourceFiles);
      Process compileAppSourceProcess = newOutputRedirectedProcessBuilder()
          .command(compileAppSourceCommand)
          .start();
      int retCode = compileAppSourceProcess.waitFor();
      if (retCode != 0) {
        throw new RuntimeException(
            "Failed to comile subject program source files: " + subjectProgram.getRootDir());
      }
    }
  }

  @Override
  protected Path getLogFilePath() {
    return subjectProgram.getDataDirRoot().resolve(LOG_FILE_NAME);
  }
  
  @Override
  protected Path getWorkingDirectory() {
    return Paths.get(".");
  }

  private List<Path> readSourceFilePaths() throws IOException {
    List<Path> sourceFiles = new ArrayList<>();
    sourceFiles.addAll(SubjectUtils.readSrcFilePaths(subjectProgram));
    sourceFiles.addAll(SubjectUtils.readTestFilePaths(subjectProgram));
    return sourceFiles;
  }
  
  private Path writeSourceFileList(List<Path> sourceFiles) throws IOException {
    StringBuilder contentBuilder = new StringBuilder();
    for (Path sourceFile : sourceFiles) {
      contentBuilder.append(sourceFile.toString());
      contentBuilder.append('\n');
    }
    Path sourceListFile = subjectProgram.getDataDirRoot().resolve("source_list.txt");
    if (!Files.isDirectory(subjectProgram.getDataDirRoot())) {
      Files.createDirectories(subjectProgram.getDataDirRoot());
    }
    Files.write(sourceListFile, contentBuilder.toString().getBytes());
    return sourceListFile;
  }
  
  private String[] getCompileCommand(String classPathStr, List<Path> sourceFiles) throws IOException {
    List<String> commandComponents = new ArrayList<>();
    commandComponents.add("javac");
    commandComponents.add("-g");
    commandComponents.add("-encoding");
    commandComponents.add("utf-8");
    if (classPathStr != null) {
    commandComponents.add("-cp");
    commandComponents.add(classPathStr);
    }
    Path sourceListFile = writeSourceFileList(sourceFiles);
    commandComponents.add("@" + sourceListFile.toString());
    String[] cmdArr = new String[commandComponents.size()];
    for (int i = 0; i < cmdArr.length; ++i) {
      cmdArr[i] = commandComponents.get(i);
    }
    return cmdArr;
  }
  
  public static void main(String[] args) {
    for (String subjectPathStr : args) {
      try {
        Path subjectPath = Paths.get(subjectPathStr);
        SubjectProgram subject = SubjectProgram.openSubjectProgram(subjectPath, subjectPath);
        System.out.print("Compiling subject program at " + subjectPathStr + " ...");
        new CompileSubjectProgram(subject).compile();
        System.out.println("Done");
      } catch (Throwable ex) {
        System.err.println("Failed to compile subject program at " + subjectPathStr);
      }
    }
  }
}
