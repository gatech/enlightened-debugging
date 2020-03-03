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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.SimulatedAlgorithmicDebugging;
import anonymous.domain.enlighten.UserPoweredFLException;
import anonymous.domain.enlighten.SimulatedAlgorithmicDebugging.QueryStats;
import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.subjectmodel.MutantSubject;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class AlgDbg {
  
  private Path baseDir;
  private Map<String, SubjectProgram> refImplsMap = new HashMap<>();
  private PrintWriter logger;
  
  public AlgDbg() throws IOException {
    baseDir = Paths.get("../mutation");
    logger = new PrintWriter(new FileOutputStream(
        baseDir.resolve("alg_dbg/run_alg_dbg.log").toFile(), true));
  }
  
  public void runOnRemainingVersions() throws IOException {
    for (MutantVersionId mutantId : getAllMutantVersions()) {
      Path resultDataFile = getDataFilePath(mutantId);
      if (!Files.exists(resultDataFile)) {
        runOnMutant(mutantId);
      }
    }
  }
  
  public void runOnFailures() throws IOException {
    for (MutantVersionId mutantId : getAllMutantVersions()) {
      Path resultDataFile = getDataFilePath(mutantId);
      @SuppressWarnings("unchecked")
      List<QueryStats> results = FSTSerialization.readObjectFromFile(List.class, resultDataFile);
      if (results == null || results.isEmpty()) {
        runOnMutant(mutantId);
      }
    }
  }

  public void runOnMutant(MutantVersionId mutantId) {
    String mutantName = mutantId.toString();
    System.out.println("Running on " + mutantName);
    Path mutantRootPath = getMutantRoot(mutantId);
    Path mutantDataPath = getMutantDataRoot(mutantId);
    try {
      SubjectProgram refImpl = getRefImpl(mutantId);
      MutantSubject mutant = new MutantSubject(mutantRootPath, mutantDataPath, refImpl);
      SimulatedAlgorithmicDebugging algDbg = 
          new SimulatedAlgorithmicDebugging(mutant, refImpl, false);
      log(mutantName);
      algDbg.localizeFault(); 
      List<QueryStats> queryStatsList = algDbg.getQueryStats();
      FSTSerialization.writeObjectTofile(List.class, getDataFilePath(mutantId), queryStatsList);
      int nQueries = queryStatsList.size();
      int sumMemRead = 0;
      int sumMemWrite = 0;
      for (QueryStats stats : queryStatsList) {
        sumMemRead += stats.memRead;
        sumMemWrite += stats.memWrite;
        logln("\t" + stats.memRead + "\t" + stats.memWrite);
      }
      double avgMemRead = (double) sumMemRead / nQueries;
      double avgMemWrite = (double)sumMemWrite / nQueries;
      logln(mutantName + "\t\t\t" + String.format("%.2f", avgMemRead) 
          + "\t" + String.format("%.2f", avgMemWrite)
          + "\t" + nQueries);
    } catch (IOException | UserPoweredFLException ex) {
      logln("\tFailed due to " + ex.getClass().getName() + ":" + ex.getMessage());
      try {
        FSTSerialization.writeObjectTofile(List.class, getDataFilePath(mutantId), new ArrayList<>());
      } catch (IOException e) {


      }
    }
  }
  
  private List<MutantVersionId> getAllMutantVersions() throws IOException {
    List<MutantVersionId> mutantIds = new ArrayList<>();
    Path mutantsListFile = baseDir.resolve("alg_dbg/mutants_list.txt");
    List<String> lines = Files.readAllLines(mutantsListFile);
    for (String mutantInfo : lines) {
      MutantVersionId mutantId = MutantVersionId.parseFromMutantVersionName(mutantInfo);
      mutantIds.add(mutantId);
    }
    return mutantIds;
  }
  
  private Path getMutantRoot(MutantVersionId mutantId) {
    return baseDir.resolve("working_dir").resolve(mutantId.toString());
  }
  
  private Path getMutantDataRoot(MutantVersionId mutantId) {
    return baseDir.resolve("out").resolve(mutantId.toString());
  }
  
  private Path getDataFilePath(MutantVersionId mutantId) {
    return getMutantDataRoot(mutantId).resolve("alg_dbg_result.dat");
  }
  
  private SubjectProgram getRefImpl(MutantVersionId mutantId) throws IOException {
    String subjectName = mutantId.getSubjectName();
    if (!refImplsMap.containsKey(subjectName)) {
      Path refImplDir = baseDir.resolve("subjects").resolve(subjectName);
      SubjectProgram refImpl = SubjectProgram.openSubjectProgram(refImplDir, refImplDir);
      refImplsMap.put(subjectName, refImpl);
    }
    return refImplsMap.get(subjectName);
  }
  
  private void logln(String line) {
    logger.println(line);
    logger.flush();
  }
  
  private void log(String str) {
    logger.print(str);
    logger.flush();
  }
  
  public static void main(String[] args) throws Throwable {
    AlgDbg runner = new AlgDbg();
    runner.runOnRemainingVersions();
  }
}
