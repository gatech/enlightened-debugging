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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DependencyCreationListener;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class DependentPoststateValues extends ExecTreeProcessingListener 
    implements DependencyCreationListener {
  
  private TestName test;
  private SourceLocation criterionSrcLoc;
  
  private Set<MethodInvocation> invocCandidates = new HashSet<>();
  
  private HashMap<MethodInvocation, Set<RefPath>> dependentValues;

  public DependentPoststateValues(SubjectProgram subject, TestName testName,
      ExecutionProfile profile, SourceLocation criterionSrcLoc) {
    super(subject, profile);
    test = testName;
    this.criterionSrcLoc = criterionSrcLoc;
    setRequireDeterministicExecution(true);
  }

  @Override
  public void dependencyCreated(DynamicDependency dep) {
    if (dep instanceof InstructionDependencySource) {
      DependencyTrackingInstruction insn = 
          ((InstructionDependencySource) dep).getSourceInstruction();
      SourceLocation srcLoc = JpfEntityConversion.getSourceLocationFromInstruction(insn);
      if (srcLoc.equals(criterionSrcLoc)) {
        dep.addAnnotation(DependentMark.getInstance());
        MethodInvocation invocNode = getCurrentInvocation();
        while (invocNode != null) {
          if (getSubjectProgram().isAppClass(invocNode.getMethodName().getClassName())) {
            invocCandidates.add(invocNode);
          }
          invocNode = invocNode.getEnclosingInvocation();
        }
      }
    } else if (dep instanceof CompositeDynamicDependency) {
      List<DynamicDependency> upStreams = ((CompositeDynamicDependency) dep).getAllDependencies();
      for (DynamicDependency upStream : upStreams) {
        if (upStream.getAnnotation(DependentMark.class) != null) {
          dep.addAnnotation(DependentMark.getInstance());
          break;
        }
      }
    }
  }

  @Override
  protected void invocationExited(MethodInvocation exitedInvocation,
      ThreadInfo currentThread) {
    
    
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {}

  @Override
  protected void invocationEntered(MethodInvocation enteredInvocation,
      ThreadInfo currentThread) {}

  private static final class DependentMark implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    private static final DependentMark singleton = new DependentMark();
    
    public static DependentMark getInstance() {
      return singleton;
    }
    
    private DependentMark() {}
  }
}
