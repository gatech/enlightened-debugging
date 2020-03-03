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


package anonymous.domain.enlighten.slicing.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependency;


public abstract class DepDepthFirstTraversal {
  
  private boolean followControlDependencies = true;
  
  public void traverse(DynamicDependency dep) {
    if (dep == null) {
      return;
    }
    Deque<ChildVisitingInfo> workingStack = new ArrayDeque<>();
    Set<DynamicDependency> visitedDeps = Collections.newSetFromMap(new IdentityHashMap<>());
    visitedDeps.add(dep);
    if (preVisit(dep)) {
      workingStack.push(new ChildVisitingInfo(dep));
    }
    while (!workingStack.isEmpty()) {
      ChildVisitingInfo current = workingStack.peek();
      if (current.dep instanceof CompositeDynamicDependency) {
        List<DynamicDependency> upStreams = null;
        if (followControlDependencies) {
          upStreams = ((CompositeDynamicDependency) current.dep).getAllDependencies();
        } else {
          upStreams = ((CompositeDynamicDependency) current.dep).getDataDependencies();
        }
        if (upStreams.size() > current.nextChildIndex) {
          DynamicDependency nextChild = upStreams.get(current.nextChildIndex);
          ++current.nextChildIndex;
          if (!visitedDeps.contains(nextChild)) {
            visitedDeps.add(nextChild);
            if (preVisit(nextChild)) {
              workingStack.push(new ChildVisitingInfo(nextChild));
            }
          } 

        } else {
          postVisit(current.dep);
          workingStack.pop();
        }
      } else {
        postVisit(current.dep);
        workingStack.pop();
      }
    }
  }
  
  public void setFollowControlDependencies(boolean follow) {
    followControlDependencies = follow;
  }
  
  public boolean followControlDependencies() {
    return followControlDependencies;
  }
  
  
  protected abstract boolean preVisit(DynamicDependency depNode);
  
  protected abstract void postVisit(DynamicDependency depNode);
  
  private static class ChildVisitingInfo {
    private DynamicDependency dep;
    private int nextChildIndex;
    
    public ChildVisitingInfo(DynamicDependency dep) {
      this.dep = dep;
    }
  }
}
