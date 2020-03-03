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


package anonymous.domain.enlighten.wrongfeedback;

import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.MethodInvocationSelection;
import anonymous.domain.enlighten.OracleFeedback;
import anonymous.domain.enlighten.SimulatedFeedbackDirectedFL;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class ControlledFL extends SimulatedFeedbackDirectedFL {
  
  private FeedbackController feedbackController;

  public ControlledFL(SubjectProgram targetProgram,
      SubjectProgram refImpl, FeedbackController feedbackController) {
    super(targetProgram, refImpl);
    this.feedbackController = feedbackController;
  }
  
  @Override
  protected OracleFeedback getOracleFeedback(
      MethodInvocationSelection selectedInvocation, List<RefPath> candidates, 
      Map<RefPath, Double> valueSuspMap) {
    OracleFeedback oracleFeedback = 
        super.getOracleFeedback(selectedInvocation, candidates, valueSuspMap);
    if (oracleFeedback == null) {
      return null;
    }
    return feedbackController.getModifiedFeedback(oracleFeedback);
  }
}
