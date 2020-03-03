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


package instr.callback;

public interface CallbackDelegation {

  public void addCallbackListenerImpl(InstrumentationCallbackListener listener);
  public void removeCallbackListenerImpl(InstrumentationCallbackListener listener);
  public void clearCallbackListenersImpl();
  public void executionStartedImpl(String executionId);
  public void executionEndedImpl(String executionId);
  public void executeSourceLocationImpl(int sourceLocationId);
  public void entryImpl(String className, String methodSig);
  public void preStatesImpl(int methodInfoId, Object[] params);
  public void exitImpl(String className, String methodSig);
  public void postStatesNormalImpl(
      int methodInfoId, Object retValue, Object[] params);
  public void exception_exitImpl(String className, String methodSig);
  public void postStatesExceptionImpl(
      int methodInfoId, Object exception, Object[] params);
  public void readInstanceFieldImpl(Object objRef, String fieldName);
  public void writeInstanceFieldImpl(Object objRef, String fieldName);
  public void newObjectImpl(Object objRef);
  public void readStaticFieldImpl(String className, String fieldName);
  public void writeStaticFieldImpl(String className, String fieldName);
  public void readArrayBucketImpl(Object arrayRef, int index);
  public void writeArrayBucketImpl(Object arrayRef, int index);
  public boolean checkSystemArrayCopy(
      Object src, int srcPos, Object dest, int destPos, int length);
  public void newArrayImpl(Object arrayRef);
  public void executionSwitchOutImpl();
  public void executionSwitchInImpl();

}
