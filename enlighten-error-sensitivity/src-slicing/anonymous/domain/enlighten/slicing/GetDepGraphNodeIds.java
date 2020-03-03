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


package anonymous.domain.enlighten.slicing;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Range;

import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.slicing.util.DepBreadthFirstTraversal;

public class GetDepGraphNodeIds extends DepBreadthFirstTraversal {
  
  private Range<Long> traversalRange;
  private Set<Long> relevantDepIds;
  
  public static Set<Long> getDepGraphNodeIds(DynamicDependency root, Range<Long> instanceIdRange) {
    GetDepGraphNodeIds getIds = new GetDepGraphNodeIds(instanceIdRange);
    getIds.traverse(root);
    return getIds.getRelevantDepIds();
  }
  
  public static Set<Long> getDepGraphNodeIds(DynamicDependency root) {
    return getDepGraphNodeIds(root, Range.all());
  }
  
  public GetDepGraphNodeIds(Range<Long> traversalRange) {
    this.traversalRange = traversalRange;
  }
  
  public Set<Long> getRelevantDepIds() {
    return relevantDepIds;
  }
  
  @Override
  public void traverse(DynamicDependency dep) {
    relevantDepIds = new HashSet<>();
    super.traverse(dep);
  }

  @Override
  protected boolean visit(DynamicDependency depNode) {
    if (!traversalRange.contains(depNode.getInstanceIndex())) {
      return false;
    }
    relevantDepIds.add(depNode.getInstanceIndex());
    return true;
  }
}
