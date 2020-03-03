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


package anonymous.domain.enlighten.susp;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.Coverage;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.exec.RunTestsWithCoverage;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class TarantulaMetric<EntityType extends Serializable> extends FaultLocalization<EntityType> {

  
  public TarantulaMetric(TestOutcomes testOutcomes,
      Map<TestName, ? extends Coverage<EntityType>> covMatrix) {
    super(testOutcomes, covMatrix);
  }

  public static void main(String[] args) throws Throwable {
    SubjectProgram testSubject = SubjectProgram.openSubjectProgram(
        Paths.get("subjects/jtopas-v1-fault2/faulty"), Paths.get("out/jtopas-v1-fault2/faulty"));
    RunTestsWithCoverage cov = new RunTestsWithCoverage(testSubject);
    cov.generateCoverageData();
    System.out.println(new TarantulaMetric<SourceLocation>(
        cov.getTestOutcomes(), cov.getSourceLocationCoverageMatrix()).getRankedListString());
  }
  
  protected Map<EntityType, Double> doSuspiciousnessComputation() {
    Map<EntityType, Double> suspiciousnessMap = new HashMap<>();
    Map<EntityType, Integer> passingCovCount = new HashMap<>();
    Map<EntityType, Integer> failingCovCount = new HashMap<>();
    int passingTotal = 0;
    int failingTotal = 0;
    Set<EntityType> allEntities = new HashSet<>();
    for (Map.Entry<TestName, ? extends Coverage<EntityType>> testCoverage : covMatrix.entrySet()) {
      TestName testName = testCoverage.getKey();
      Coverage<EntityType> coverage = testCoverage.getValue();
      Map<EntityType, Integer> covCountMapToUpdate = null;
      if (testOutcomes.isPassed(testName)) {
        ++passingTotal;
        covCountMapToUpdate = passingCovCount;
      } else {
        ++failingTotal;
        covCountMapToUpdate = failingCovCount;
      }
      for (EntityType coveredEntity : coverage.getCoverage()) {
        allEntities.add(coveredEntity);
        if (!covCountMapToUpdate.containsKey(coveredEntity)) {
          covCountMapToUpdate.put(coveredEntity, 1);
        } else {
          covCountMapToUpdate.put(coveredEntity, covCountMapToUpdate.get(coveredEntity) + 1);
        }
      }
    }
    for (EntityType methodName : allEntities) {
      int failingTestsAccounted = 
          failingCovCount.containsKey(methodName) ? failingCovCount.get(methodName) : 0;
      int passingTestsAccounted =
          passingCovCount.containsKey(methodName) ? passingCovCount.get(methodName) : 0;
      double failingPercentAccounted = (double) failingTestsAccounted / failingTotal;
      double passingPercentAccounted = (double) passingTestsAccounted / passingTotal;
      double suspiciousness = 
          failingPercentAccounted / (failingPercentAccounted + passingPercentAccounted);
      suspiciousnessMap.put(methodName, suspiciousness);
    }
    return suspiciousnessMap;
  }
}
