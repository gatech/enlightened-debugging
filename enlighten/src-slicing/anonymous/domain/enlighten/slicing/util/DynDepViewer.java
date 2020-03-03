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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Range;

import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.util.GraphViewer;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;

public class DynDepViewer extends GraphViewer<DynamicDependency> {
  
  private Range<Long> instanceIndexRange;
  
  public DynDepViewer(DynamicDependency startNode, Range<Long> instanceIndexRange) {
    super(startNode);
    this.instanceIndexRange = instanceIndexRange;
  }

  public DynDepViewer(DynamicDependency startNode) {
    this(startNode, Range.all());
  }

  @Override
  protected List<DynamicDependency> getSuccessors(DynamicDependency node) {
    checkRange(node);
    if (node instanceof CompositeDynamicDependency) {
      List<DynamicDependency> successors = new ArrayList<>();
      for (DynamicDependency successor : 
        ((CompositeDynamicDependency) node).getAllDependencies()) {
        if (instanceIndexRange.contains(successor.getInstanceIndex())) {
          successors.add(successor);
        }
      }
      return successors;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  protected List<String> getSuccessorEdgeDescriptions(
      DynamicDependency node) {
    if (node instanceof CompositeDynamicDependency) {
      List<String> labels = new ArrayList<>();
      CompositeDynamicDependency cdd = (CompositeDynamicDependency) node;
      for (DynamicDependency upStream : cdd.getDataDependencies()) {
        if (!instanceIndexRange.contains(upStream.getInstanceIndex())) {
          continue;
        }
        if (upStream instanceof InstructionDependencySource) {
          labels.add("insn");
        } else {
          labels.add("");
        }
      }
      DynamicDependency controlDep = cdd.getControlDependency();
      if (controlDep != null && instanceIndexRange.contains(controlDep.getInstanceIndex())) {
        labels.add("ctrl");
      }
      return labels;
    }else {
      return Collections.emptyList();
    }
  }

  @Override
  protected String getDescription(DynamicDependency node) {
    String desc = String.valueOf(node.getInstanceIndex());
    if (node instanceof InstructionDependencySource) {
      DependencyTrackingInstruction insn = ((InstructionDependencySource) node).getSourceInstruction();
      desc += "\n" + insn.getMnemonic() + ":" + insn.getFileLocation();
    }
    return desc;
  }
  
  private void checkRange(DynamicDependency dep) {
    if (!instanceIndexRange.contains(dep.getInstanceIndex())) {
      throw new RuntimeException("Out-of-range dependency node");
    }
  }
}
