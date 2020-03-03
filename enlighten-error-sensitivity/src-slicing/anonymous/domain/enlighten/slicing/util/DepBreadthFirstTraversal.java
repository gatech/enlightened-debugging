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
import java.util.Set;

import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependency;

public abstract class DepBreadthFirstTraversal {
  
  private boolean followControlDependencies = true;

  public void traverse(DynamicDependency dep) {
    if (dep == null) {
      return;
    }
    Deque<DynamicDependency> workingList = new ArrayDeque<>();
    Set<DynamicDependency> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    workingList.addLast(dep);
    visited.add(dep);
    while (!workingList.isEmpty()) {
      dep = workingList.removeFirst();
      boolean continueTraversing = visit(dep);
      if (continueTraversing && dep instanceof CompositeDynamicDependency) {
        CompositeDynamicDependency composite = (CompositeDynamicDependency) dep;
        for (DynamicDependency upStream : composite.getDataDependencies()) {
          if (!visited.contains(upStream)) {
            visited.add(upStream);
            workingList.addLast(upStream);
          }
        }
        if (followControlDependencies) {
          DynamicDependency controlDep = composite.getControlDependency();
          if (controlDep != null && !visited.contains(controlDep)) {
            visited.add(controlDep);
            workingList.addLast(controlDep);
          }
        }
      }
    }
  }
  
  public void setFollowControlDependencies(boolean follow) {
    followControlDependencies = follow;
  }
  
  protected abstract boolean visit(DynamicDependency depNode);
}
