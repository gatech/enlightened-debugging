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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.Coverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;

public class WeightedOchiai<EntityType extends Serializable> 
    extends FaultLocalization<EntityType> {
  
  private Map<TestName, Double> testWeights;

  public WeightedOchiai(TestOutcomes testOutcomes, 
      Map<TestName, ? extends Coverage<EntityType>> covMatrix,
      Map<TestName, Double> testWeights) {
    super(testOutcomes, covMatrix);
    this.testWeights = testWeights;
  }

  @Override
  protected Map<EntityType, Double> doSuspiciousnessComputation() {
    Map<EntityType, Double> suspiciousnessMap = new HashMap<>();
    Map<EntityType, Double> passingCovWeights = new HashMap<>();
    Map<EntityType, Double> failingCovWeights = new HashMap<>();
    double failingTotalWeights = 0;
    Set<EntityType> allEntities = new HashSet<>();
    for (Map.Entry<TestName, ? extends Coverage<EntityType>> testCoverage 
        : covMatrix.entrySet()) {
      TestName testName = testCoverage.getKey();
      Coverage<EntityType> coverage = testCoverage.getValue();
      double testWeight = testWeights.get(testName);
      Map<EntityType, Double> covCountMapToUpdate = null;
      if (testOutcomes.isPassed(testName)) {
        covCountMapToUpdate = passingCovWeights;
      } else {
        ++failingTotalWeights;
        covCountMapToUpdate = failingCovWeights;
      }
      for (EntityType coveredEntity : coverage.getCoverage()) {
        allEntities.add(coveredEntity);
        if (!covCountMapToUpdate.containsKey(coveredEntity)) {
          covCountMapToUpdate.put(coveredEntity, testWeight);
        } else {
          covCountMapToUpdate.put(
              coveredEntity, covCountMapToUpdate.get(coveredEntity) + testWeight);
        }
      }
    }
    for (EntityType methodName : allEntities) {
      double failingWeightsAccounted = 
          failingCovWeights.containsKey(methodName) ? failingCovWeights.get(methodName) : 0;
      double passingWeightsAccounted =
          passingCovWeights.containsKey(methodName) ? passingCovWeights.get(methodName) : 0;
      double totalCovWeights = failingWeightsAccounted + passingWeightsAccounted;
      double suspiciousness = (double) failingWeightsAccounted 
          / Math.sqrt((double) totalCovWeights * failingTotalWeights);
      suspiciousnessMap.put(methodName, suspiciousness);
    }
    return suspiciousnessMap;
  }
}
