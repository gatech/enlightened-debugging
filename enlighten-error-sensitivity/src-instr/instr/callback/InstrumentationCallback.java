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











public abstract class InstrumentationCallback {
  
  private static CallbackDelegation impl;
  
  public static void init(CallbackDelegation implementation) {
    impl = implementation;
  }

  public static void addCallbackListener(InstrumentationCallbackListener listener) {
    impl.addCallbackListenerImpl(listener);
  }
  
  public static void removeCallbackListener(InstrumentationCallbackListener listener) {
    impl.removeCallbackListenerImpl(listener);
  }
  
  public static void clearCallbackListeners() {
    impl.clearCallbackListenersImpl();
  }
  
  
  public static void executionStarted(String executionId) {
    if (impl != null) {
      impl.executionStartedImpl(executionId);
    }
  }
  
  
  public static void executionEnded(String executionId) {
    if (impl != null) {
      impl.executionEndedImpl(executionId);
    }
  }
  
  
  public static void executeSourceLocation(int sourceLocationId) {
    if (impl != null) {
      impl.executeSourceLocationImpl(sourceLocationId);
    }
  }
  
  public static void entry(String className, String methodSig) {
    if (impl != null) {
      impl.entryImpl(className, methodSig);
    }
  }
  
  public static void preStates(int methodInfoId, Object[] params) {
    if (impl != null) {
      impl.preStatesImpl(methodInfoId, params);
    }
  }
  
  public static void exit(String className, String methodSig) { 
    if (impl != null) {
      impl.exitImpl(className, methodSig);
    }
  }
  
  public static void postStatesNormal(
      Object retValue, int methodInfoId, Object[] params) {
    if (impl != null) {
      impl.postStatesNormalImpl(methodInfoId, retValue, params);
    }
  }
  
  public static void exception_exit(String className, String methodSig) { 
    if (impl != null) {
      impl.exception_exitImpl(className, methodSig);
    }
  }
  
  public static void postStatesException(
      Object exception, int methodInfoId, Object[] params) {
    if (impl != null) {
      impl.postStatesExceptionImpl(methodInfoId, exception, params);
    }
  }
  
  public static void readInstanceField(Object objRef, String fieldName) {
    if (impl != null) {
      impl.readInstanceFieldImpl(objRef, fieldName);
    }
  }
  
  public static void writeInstanceField(Object objRef, String fieldName) {
    if (impl != null) {
      impl.writeInstanceFieldImpl(objRef, fieldName);
    }
  }
  
  public static void newObject(Object objRef) {
    if (impl != null) {
      impl.newObjectImpl(objRef);
    }
  }
  
  public static void readStaticField(String className, String fieldName) {
    if (impl != null) {
      impl.readStaticFieldImpl(className, fieldName);
    }
  }
  
  public static void writeStaticField(String className, String fieldName) {
    if (impl != null) {
      impl.writeStaticFieldImpl(className, fieldName);
    }
  }
  
  public static void readArrayBucket(Object arrayRef, int index) {
    if (impl != null) {
      impl.readArrayBucketImpl(arrayRef, index);
    }
  }
  
  public static void writeArrayBucket(Object arrayRef, int index) {
    if (impl != null) {
      impl.writeArrayBucketImpl(arrayRef, index);
    }
  }
  

  public static void newArray(Object arrayRef) {
    if (impl != null) {
      impl.newArrayImpl(arrayRef);
    }
  }
  
  public static ArrayCopyParams systemArrayCopy(
      Object src, int srcPos, Object dest, int destPos, int length) {
    if (impl != null) {
      if (impl.checkSystemArrayCopy(src, srcPos, dest, destPos, length)) {
        for(int i = 0; i < length; ++i) {
          readArrayBucket(src, srcPos + i);
          writeArrayBucket(dest, destPos + i);
        }
      }
    }
    return new ArrayCopyParams(src, srcPos, dest, destPos, length);
  }
  
  
  public static void executionSwitchOut() {
    if (impl != null) {
      impl.executionSwitchOutImpl();
    }
  }
  
  
  public static void executionSwitchIn() {
    if (impl != null) {
      impl.executionSwitchInImpl();
    }
  }
  
  
}
