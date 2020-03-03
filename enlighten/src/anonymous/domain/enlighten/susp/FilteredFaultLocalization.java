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
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;

public class FilteredFaultLocalization<E extends Serializable> extends FaultLocalization<E> {
  
  private FaultLocalization<E> fl;
  private Set<E> filter;
  
  public static FilteredFaultLocalization<SourceLocation> filteredSrcLocFlByInvocationCov(
      FaultLocalization<SourceLocation> fl, MethodInvocation invocation) {
    return new FilteredFaultLocalization<SourceLocation>(
        fl, CoverageUtils.getFullStatementCoverage(invocation));
  }

  public FilteredFaultLocalization(FaultLocalization<E> fl, Set<E> filter) {
    super(fl.testOutcomes, fl.covMatrix);
    this.fl = fl;
    this.filter = filter;
  }

  @Override
  protected Map<E, Double> doSuspiciousnessComputation() {
    Map<E, Double> suspiciousnessMap = new HashMap<>();
    for (E entity : fl.getRankedList()) {
      if (filter.contains(entity)) {
        suspiciousnessMap.put(entity, fl.getSuspiciousness(entity));
      } else {
        suspiciousnessMap.put(entity, 0d);
      }
    }
    return suspiciousnessMap;
  }
}
