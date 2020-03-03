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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.data.Coverage;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.data.TestOutcomes;
import anonymous.domain.enlighten.util.FloatComparison;

public abstract class FaultLocalization<EntityType extends Serializable> {
  
  private static final String DEFAULT_METRIC_NAME = "ochiai";
  
  protected TestOutcomes testOutcomes;
  protected Map<TestName, ? extends Coverage<EntityType>> covMatrix;
  
  private Map<EntityType, Double> suspiciousnessMap;
  private List<EntityType> rankedList;
  
  public static <EntityType extends Serializable> FaultLocalization<EntityType> 
    getFaultLocalization(TestOutcomes testOutcomes, 
      Map<TestName, ? extends Coverage<EntityType>> covMatrix) {
    return getFaultLocalization(DEFAULT_METRIC_NAME, testOutcomes, covMatrix);
  }
  
  public static <EntityType extends Serializable> FaultLocalization<EntityType> 
    getFaultLocalization(String metricName, TestOutcomes testOutcomes, 
        Map<TestName, ? extends Coverage<EntityType>> covMatrix) {
    if ("tarantula".equals(metricName)) {
      return new TarantulaMetric<EntityType>(testOutcomes, covMatrix);
    } else if ("ochiai".equals(metricName)) {
      return new OchiaiMetric<EntityType>(testOutcomes, covMatrix);
    } else {
      throw new IllegalArgumentException(
          "Unknown fault localization metric name \"" + metricName + "\"");
    }
  }
  
  public FaultLocalization(TestOutcomes testOutcomes, 
      Map<TestName, ? extends Coverage<EntityType>> covMatrix) {
    this.testOutcomes = testOutcomes;
    this.covMatrix = covMatrix;
  }

  public Map<EntityType, Double> getSuspiciousness() {
    if (suspiciousnessMap == null) {
      computeSuspiciousness();
    }
    return suspiciousnessMap;
  }
  
  public List<EntityType> getRankedList() {
    if (suspiciousnessMap == null) {
      computeSuspiciousness();
    }
    return rankedList;
  }
  
  public double getSuspiciousness(EntityType entity) {
    if (suspiciousnessMap == null) {
      computeSuspiciousness();
    }
    Double suspiciousness = suspiciousnessMap.get(entity);
    if (suspiciousness == null) {
      return 0;
    } else {
      return suspiciousness;
    }
  }
  
  public int getBestCaseRank(EntityType entity) {
    return computeRank(entity, false);
  }
  
  public int getWorstCaseRank(EntityType entity) {
    return computeRank(entity, true);
  }
  
  public String getRankedListString() {
    StringBuilder rankedListStrBuilder = new StringBuilder();
    rankedListStrBuilder.append("Rank\tProgram Entity\tSuspiciousness\n");
    int counter = 1;
    for (EntityType entity : getRankedList()) {
      double suspiciousness = getSuspiciousness(entity);
      rankedListStrBuilder.append(counter++);
      rankedListStrBuilder.append('\t');
      rankedListStrBuilder.append(entity.toString());
      rankedListStrBuilder.append('\t');
      rankedListStrBuilder.append(String.format("%.4f", suspiciousness));
      rankedListStrBuilder.append('\n');
    }
    return rankedListStrBuilder.toString();
  }
  
  public String getRankedListString(int topN) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Rank\tProgram Entity\tSuspiciousness\n");
    int counter = 1;
    double lastSusp = Double.POSITIVE_INFINITY;
    for (EntityType entity : getRankedList()) {
      double susp = getSuspiciousness(entity);
      buffer.append(counter++);
      buffer.append('\t');
      buffer.append(entity.toString());
      buffer.append('\t');
      buffer.append(String.format("%.4f", susp));
      buffer.append('\n');
      if (counter > topN && (susp != lastSusp || susp == 0)) {
        break;
      }
      lastSusp = susp;
    }
    return buffer.toString();
  }
  
  protected abstract Map<EntityType, Double> doSuspiciousnessComputation();
  
  private void computeSuspiciousness() {
    suspiciousnessMap = doSuspiciousnessComputation();
    rankedList = new ArrayList<>(suspiciousnessMap.keySet());
    Collections.sort(rankedList, new SortBySuspiciousnessReversed());
  }
  
  private int computeRank(EntityType entity, boolean includeSameSuspElems) {
    List<EntityType> rankedList = getRankedList();
    double targetSusp = getSuspiciousness(entity);
    int rank = 0;
    for (EntityType e : rankedList) {
      double susp = getSuspiciousness(e);
      int compare = FloatComparison.compareDouble(susp, targetSusp);
      if (compare > 0) {
        ++rank;
      } else if (compare == 0 && includeSameSuspElems) {
        ++rank;
      } else {
        break;
      }
    }
    if (!includeSameSuspElems) {
      ++rank;
    }
    return rank;
  }
  
  private class SortBySuspiciousnessReversed implements Comparator<EntityType> {

    @Override
    public int compare(EntityType o1, EntityType o2) {
      double susp1 = getSuspiciousness(o1);
      double susp2 = getSuspiciousness(o2);
      if (susp1 < susp2) {
        return 1;
      } else if (susp1 > susp2) {
        return -1;
      } else {
        return 0;
      }
    }
  }
}
