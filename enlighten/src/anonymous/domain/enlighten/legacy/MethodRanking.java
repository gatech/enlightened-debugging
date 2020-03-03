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


package anonymous.domain.enlighten.legacy;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;

public class MethodRanking {
  
  private List<SourceLocation> suspStmts;
  private Set<MethodName> suspMethods;
  private List<MethodInvocation> suspInvocations;

  public static void main(String[] args) throws Throwable {
    String[] subjectNames = { 
        "jtopas-v1-fault2",
        "jtopas-v1-fault5",
        "jtopas-v1-fault6",
        "commons-math-v1-C_AK_1",
        "commons-math-v1-EDI_AK_1",
        "commons-math-v1-F_AK_1",
        "commons-math-v1-M_AK_1",
        "commons-math-v1-VS_AK_1",
        "commons-math-v2-CDI_AK_1",
        "commons-math-v2-F_AK_2",
        "commons-math-v2-MU_AK_1",
        "commons-math-v2-MU_AK_4",
        "commons-math-v2-URSU_AK_1",
        "commons-math-v3-ERKI_AK_1"
        };
    for (String subjectName : subjectNames) {
      SubjectProgram subject = SubjectProgram.openSubjectProgram(
          Paths.get("subjects", subjectName, "faulty"), Paths.get("out", subjectName, "faulty"));
      MethodRanking methodRanking = new MethodRanking(subject);
      System.out.println(subjectName + ": " + methodRanking.getSuspLines().size() 
          + ", " + methodRanking.getSuspMethods().size() 
          + ", " + methodRanking.getSuspInvocations().size());
    }
  }
  
  public MethodRanking(SubjectProgram subject) throws IOException {
    RunTestsWithCoverage runWithCov = new RunTestsWithCoverage(subject);
    TestOutcomes testOutcomes = runWithCov.getTestOutcomes();
    Map<TestName, SourceLocationCoverage> covMatrix = runWithCov.getSourceLocationCoverageMatrix();
    FaultLocalization<SourceLocation> fl = 
        FaultLocalization.getFaultLocalization(testOutcomes, covMatrix);
    SourceLocation faultyLocation = subject.getFaultySourceLocations().get(0);
    double faultSusp = fl.getSuspiciousness(faultyLocation);
    suspStmts = new ArrayList<>();
    for (SourceLocation sourceLocation : fl.getRankedList()) {
      if (compareFloat(fl.getSuspiciousness(sourceLocation), faultSusp) >= 0) {
        suspStmts.add(sourceLocation);
      }
    }

    suspMethods = new HashSet<>();
    suspInvocations = new ArrayList<>();
    for (TestName test : testOutcomes.getTestSet()) {
      if (!testOutcomes.isPassed(test)) {
        ExecutionProfile failedTest = ExecutionProfile.readFromDataFile(
            subject.getCoverageDir().resolve(test.getDescription() + ".tree"));
        for (SourceLocation suspStmt : suspStmts) {
          List<MethodInvocation> containingInvocations = failedTest.lookupInvocation(suspStmt);
          if (containingInvocations != null) {
            for (MethodInvocation containingInvocation : containingInvocations) {
              suspMethods.add(containingInvocation.getMethodName());
              suspInvocations.add(containingInvocation);
            }
          }
        }
      }
    }
    
  }
  
  public List<SourceLocation> getSuspLines() {
    return suspStmts;
  }
  
  public Set<MethodName> getSuspMethods() {
    return suspMethods;
  }

  public List<MethodInvocation> getSuspInvocations() {
    return suspInvocations;
  }
  
  private static int compareFloat(double n1, double n2) {
    final double PRECISION = 0.00000001;
    if (n1 - n2 > PRECISION) {
      return 1;
    } else if (n2 - n1 > PRECISION) {
      return -1;
    } else {
      return 0;
    }
  }
}
