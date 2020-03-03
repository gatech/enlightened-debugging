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


package anonymous.domain.enlighten.deptrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gov.nasa.jpf.vm.DependencyTrackingInstruction;

public class CompositeDynamicDependency extends DynamicDependency {

	private static final DynamicDependency[] ZERO_LEN_DEPENDENCY_ARRAY = new DynamicDependency[0];
  
  private DynamicDependency[] dataDeps;
  private DynamicDependency controlDep;
  
  public CompositeDynamicDependency(
  		Iterable<DynamicDependency> dataDeps, DynamicDependency controlDep) {
    init(dataDeps, controlDep);
  }
  
  public List<DynamicDependency> getDataDependencies() {
    return Collections.unmodifiableList(Arrays.asList(dataDeps));
  }
  
  public DynamicDependency getControlDependency() {
  	return controlDep;
  }
  
  public List<DynamicDependency> getAllDependencies() {
  	List<DynamicDependency> allDeps = new ArrayList<>();
  	allDeps.addAll(getDataDependencies());
  	if (controlDep != null) {
  		allDeps.add(controlDep);
  	}
  	return allDeps;
  }
  
  public String toString() {
  	DependencyTrackingInstruction sourceInsn = null;
  	for (DynamicDependency dataDep : dataDeps) {
  		if (dataDep instanceof InstructionDependencySource) {
  			sourceInsn = ((InstructionDependencySource) dataDep).getSourceInstruction();
  			break;
  		}
  	}
  	if (sourceInsn != null) {
  		return sourceInsn.getMnemonic() + " at " + sourceInsn.getFileLocation();
  	} else {
  		return super.toString();
  	}
  }
  
  private void init(Iterable<DynamicDependency> dataDeps, DynamicDependency controlDep) {
  	List<DynamicDependency> sourceList = new ArrayList<>();
    for (DynamicDependency source : dataDeps) {
      sourceList.add(source);
    }
    this.dataDeps = sourceList.toArray(ZERO_LEN_DEPENDENCY_ARRAY);
    this.controlDep = controlDep;
    notifyDependencyGenerated();
  }
}
