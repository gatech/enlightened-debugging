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


package anonymous.domain.enlighten.legacy;

import java.io.IOException;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.exec.CaptureInvocationStates;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class UserPoweredFLCore extends UserPoweredFLBase {

  private DeveloperInteraction devInteraction;
  private CaptureInvocationStates captureStates;
  
  private MethodInvocation currentInvocStates;
  
  
  public UserPoweredFLCore(SubjectProgram targetProgram, 
      DeveloperInteraction devInteraction, boolean forceGenData) throws IOException {
    super(targetProgram, forceGenData);
    this.devInteraction = devInteraction;
    captureStates = new CaptureInvocationStates(targetProgram);
  }
  
  public UserPoweredFLCore(SubjectProgram targetProgram, 
      DeveloperInteraction devInteraction) throws IOException {
    this(targetProgram, devInteraction, false);
  }

  @Override
  protected boolean isDone() {
    
    return selectInvocInternal() == null || devInteraction.isDone();
  }

  @Override
  protected MethodInvocationSelection selectMethodInvocationForUserFeedback() {
    
    MethodInvocationSelection selectedInvoc = selectInvocInternal();
    try {
      currentInvocStates = captureStates.getInvocationStates(
          selectedInvoc.getTestName(), 
          selectedInvoc.getMethodName(), 
          selectedInvoc.getInvocationIndex());
    } catch (IOException e) {
      throw new RuntimeException("Fatal internal error", e);
    }
    return selectedInvoc;
  }
  
  protected MethodInvocationSelection selectInvocInternal() {
    return super.selectMethodInvocationForUserFeedback();
  }

  @Override
  protected MethodCallRepr getMethodInvocationPreState(
      MethodInvocationSelection invocation) {
    return currentInvocStates.getPreState();
  }

  @Override
  protected MethodCallRepr getMethodInvocationPostState(
      MethodInvocationSelection invocation) {
    return currentInvocStates.getPostState();
  }

  @Override
  protected UserFeedback getUserFeedback(MethodInvocationSelection invocation,
      MethodCallRepr preStates, MethodCallRepr postStates) {
    return devInteraction.getUserFeedback(
        invocation, (MethodCallRepr) preStates, (MethodCallRepr) postStates);
  }

  @Override
  protected void incorporateUserFeedback(MethodInvocationSelection invocation,
      UserFeedback feedback) {
    
  }

}
