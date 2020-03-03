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


package anonymous.domain.enlighten.subjectmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.files.LookupFilesByNameVisitor;

public class SubjectUtils {
  
  public static List<Path> readSrcFilePaths(SubjectProgram subjectProgram) throws IOException {
    List<Path> sourceFiles = new ArrayList<>();
    for (Path sourceDir : subjectProgram.getAppSourceDirs()) {
      LookupFilesByNameVisitor lookupAppSourceVisitor = new LookupFilesByNameVisitor(".*\\.java");
      if (Files.isDirectory(sourceDir)) {
        Files.walkFileTree(sourceDir, lookupAppSourceVisitor);
      }
      sourceFiles.addAll(lookupAppSourceVisitor.getMatchedFilesPaths());
    }
    return sourceFiles;
  }
  
  public static Map<Path, List<Path>> readSrcFilePathMap(
      SubjectProgram subjectProgram) throws IOException {
    Map<Path, List<Path>> srcFileMap = new HashMap<>();
    for (Path sourceDir : subjectProgram.getAppSourceDirs()) {
      List<Path> relativePaths = new ArrayList<>();
      LookupFilesByNameVisitor lookupAppSourceVisitor = new LookupFilesByNameVisitor(".*\\.java");
      if (Files.isDirectory(sourceDir)) {
        Files.walkFileTree(sourceDir, lookupAppSourceVisitor);
      }
      for (Path fullPath : lookupAppSourceVisitor.getMatchedFilesPaths()) {
        relativePaths.add(sourceDir.relativize(fullPath));
      }
      srcFileMap.put(sourceDir, relativePaths);
    }
    return srcFileMap;
  }

  public static List<Path> readTestFilePaths(SubjectProgram subjectProgram) throws IOException {
    List<Path> sourceFiles = new ArrayList<>();
    for (Path testSourceDir : subjectProgram.getTestSourceDirs()) {
      LookupFilesByNameVisitor lookupTestSourceVisitor = new LookupFilesByNameVisitor(".*\\.java");
      if (Files.isDirectory(testSourceDir)) {
        Files.walkFileTree(testSourceDir, lookupTestSourceVisitor);
      }
      sourceFiles.addAll(lookupTestSourceVisitor.getMatchedFilesPaths());
    }
    return sourceFiles;
  }
  
  public static Map<Path, List<Path>> readTestFilePathMap(
      SubjectProgram subjectProgram) throws IOException {
    Map<Path, List<Path>> testFileMap = new HashMap<>();
    for (Path sourceDir : subjectProgram.getTestSourceDirs()) {
      List<Path> relativePaths = new ArrayList<>();
      LookupFilesByNameVisitor lookupTestSourceVisitor = new LookupFilesByNameVisitor(".*\\.java");
      if (Files.isDirectory(sourceDir)) {
        Files.walkFileTree(sourceDir, lookupTestSourceVisitor);
      }
      for (Path fullPath : lookupTestSourceVisitor.getMatchedFilesPaths()) {
        relativePaths.add(sourceDir.relativize(fullPath));
      }
      testFileMap.put(sourceDir, relativePaths);
    }
    return testFileMap;
  }
}
