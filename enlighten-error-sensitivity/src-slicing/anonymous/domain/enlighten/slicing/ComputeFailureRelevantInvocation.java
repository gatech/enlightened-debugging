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

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.util.DepBreadthFirstTraversal;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ExceptionInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class ComputeFailureRelevantInvocation extends ExecTreeProcessingListener {

  private TestName testName;
  
  private Set<MethodInvocation> relevantInvocations = new HashSet<>();

  public ComputeFailureRelevantInvocation(
      SubjectProgram subject, ExecutionProfile profile) {
    super(subject, profile);
    this.testName = TestName.parseFromDescription(profile.getExecutionId());
  }
  
  public Set<MethodInvocation> getRelevantInvocations() {
    return relevantInvocations;
  }

  @Override
  protected void invocationEntered(
      MethodInvocation enteredInvocation, ThreadInfo currentThread) {}

  @Override
  protected void invocationExited(
      MethodInvocation exitedInvocation, ThreadInfo currentThread) {
    if (exitedInvocation == getTestMethodInvocation()) {
      ExceptionInfo exInfo = currentThread.getPendingException();
      if (exInfo == null) {
        System.err.println(
            "Warning: " + testName + " didn't fail by exception. "
                + "Unable to identify the failure-triggering output. "
                + "No relevant invocations are selected.");
        return;
      }
      disableListener();
      DynamicDependency exDep = exInfo.getDependency();
      new AddRelevantInvocationDepTraversal().traverse(exDep);
      FieldRef exceptionTag = new FieldRef(
          new MethodInvocationSelection(testName, exitedInvocation.getMethodName(), 0),
          RefPath.newBuilder().appendExceptionRef().build());
      for (MethodInvocation relevantInvoc : relevantInvocations) {
        InfluencedFieldsAnnotator.addInfluencedField(relevantInvoc, exceptionTag);
      }
      currentThread.getVM().terminateProcess(currentThread);
    }
  }
  
  private class AddRelevantInvocationDepTraversal extends DepBreadthFirstTraversal {

    @Override
    protected boolean visit(DynamicDependency depNode) {
      long instanceIndex = depNode.getInstanceIndex();

      String depGeneratingInsnDesc = getGeneratingInstructionInfo(depNode);
      if (depGeneratingInsnDesc != null) {

      }
      MethodInvocation genInvoc = lookupDependencyGeneratingInvocation(instanceIndex);
      if (genInvoc != null) {

      } else {

      }

      if (genInvoc == null) {
        throw new RuntimeException("Dep scope tracking error: not generated in any user-level invocations." );
      }

      relevantInvocations.add(genInvoc);
      return true;
    }
    
    private String getGeneratingInstructionInfo(DynamicDependency dep) {
      if (dep instanceof CompositeDynamicDependency) {
        for (DynamicDependency child : ((CompositeDynamicDependency) dep).getDataDependencies()) {
          if (child instanceof InstructionDependencySource) {
            Instruction insn = ((InstructionDependencySource) child).getSourceInstruction();
            return insn.getFileLocation();
          }
        }
      }
      return "No insn info";
    }
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {}
}
