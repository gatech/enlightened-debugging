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
import java.nio.file.Path;
import java.nio.file.Paths;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.subjectmodel.MutantSubject;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class AvgMemAccess {
  
  private int numInvocations = 0;
  private int numRead = 0;
  private int numWrite = 0;
  
  public void addProfile(ExecutionProfile profile) {
    visitInvocationRecursively(profile.getInvocationTreeRoot());
  }
  
  public int getNumInvocations() {
    return numInvocations;
  }
  
  public double getAvgMemRead() {
    return (double) numRead / numInvocations;
  }
  
  public double getAvgMemWrite() {
    return (double) numWrite / numInvocations;
  }
  
  private void visitInvocationRecursively(MethodInvocation invoc) {
    ++numInvocations;
    numRead += invoc.getNumMemoryReadLocations();
    numWrite += invoc.getNumMemoryWriteLocations();
    for (MethodInvocation child : invoc.getEnclosedInvocations()) {
      visitInvocationRecursively(child);
    }
  }
  
  public static void main(String[] args) throws Throwable {

      AvgMemAccess avgMemAccess = new AvgMemAccess();
      String mutantName = "commons-lang-3_2-mutant-200";

      String subjectName = mutantName.substring(0, mutantName.indexOf("-mutant-"));
      Path mutantPath = Paths.get("../mutation/working_dir/" + mutantName);
      Path mutantDataPath = Paths.get("../mutation/out/" + mutantName);
      Path refImplPath = Paths.get("../mutation/subjects/" + subjectName);
      SubjectProgram refImpl = SubjectProgram.openSubjectProgram(refImplPath, refImplPath);
      MutantSubject mutant = new MutantSubject(mutantPath, mutantDataPath, refImpl);
      RunTestsWithCoverage covRunner = new RunTestsWithCoverage(mutant);
      TestOutcomes testResults = covRunner.getTestOutcomes();
      for (TestName test : testResults.getTestSet()) {
        if (!testResults.isPassed(test)) {
          ExecutionProfile profile = covRunner.readExecutionProfile(test);
          avgMemAccess.addProfile(profile);
        }
      }
      System.out.println("Invoc count: " + avgMemAccess.getNumInvocations());
      System.out.println("Avg mem read: " + avgMemAccess.getAvgMemRead());
      System.out.println("Avg mem write: " + avgMemAccess.getAvgMemWrite());
      if (avgMemAccess.numRead != 0 || avgMemAccess.numWrite != 0) {
        System.exit(1);
      }

  }

}
