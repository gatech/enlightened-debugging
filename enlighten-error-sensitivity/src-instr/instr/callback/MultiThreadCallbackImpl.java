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

import instr.callback.memory.MemoryLocation;
import instr.staticinfo.MethodInfoDB;
import instr.staticinfo.SourceLocationDB;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;







public class MultiThreadCallbackImpl implements CallbackDelegation {
  
  private List<InstrumentationCallbackListener> callbackListeners = new ArrayList<>();
  private boolean executionStarted = false;
  
  private boolean isInAppCode = true;
  private ThreadLocal<Integer> switchOutCount = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  
  @Override
  public void addCallbackListenerImpl(InstrumentationCallbackListener listener) {
    callbackListeners.add(listener);
  }

  @Override
  public void removeCallbackListenerImpl(InstrumentationCallbackListener listener) {
    callbackListeners.remove(listener);
  }

  @Override
  public void clearCallbackListenersImpl() {
    callbackListeners.clear();
  }
  
  
  @Override
  public synchronized void executionStartedImpl(String executionId) {
    if (executionStarted) {
      throw new IllegalStateException("Execution is already started. Please stop the "
          + "on-going execution before starting a new one.");
    }



    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.executionStarted(executionId);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    executionStarted = true;
  }
  
  
  @Override
  public synchronized void executionEndedImpl(String executionId) {
    if (!executionStarted) {
      throw new IllegalStateException("No on-going execution to stop.");
    }
    executionStarted = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.executionEnded(executionId);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
  }
  
  
  @Override
  public synchronized void executeSourceLocationImpl(int sourceLocationId) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    SourceLocation sourceLocation = SourceLocationDB.getSourceLocationById(sourceLocationId);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.executingSourceLine(sourceLocation);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void entryImpl(String className, String methodSig) { 
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.methodEntered(MethodName.get(className, methodSig));
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }
  
  @Override
  public synchronized void preStatesImpl(int methodInfoId, Object[] params) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.preStates(MethodInfoDB.getMethodInfoById(methodInfoId), params);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            +". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void exitImpl(String className, String methodSig) { 
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.methodExiting(MethodName.get(className, methodSig));
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }
  
  @Override
  public synchronized void postStatesNormalImpl(
      int methodInfoId, Object retValue, Object[] params) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.postStatesNormal(
            MethodInfoDB.getMethodInfoById(methodInfoId), retValue, params);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            +". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void exception_exitImpl(String className, String methodSig) { 
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.methodExceptionExiting(MethodName.get(className, methodSig));
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }
  
  @Override
  public synchronized void postStatesExceptionImpl(
      int methodInfoId, Object exception, Object[] params) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.postStatesException(
            MethodInfoDB.getMethodInfoById(methodInfoId), exception, params);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            +". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void readInstanceFieldImpl(Object objRef, String fieldName) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    MemoryLocation memLoc = MemoryLocation.getInstanceFieldLocation(objRef, fieldName);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.memoryRead(memLoc);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void writeInstanceFieldImpl(Object objRef, String fieldName) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    writeInstanceFieldInternal(objRef, fieldName);
    isInAppCode = true;
  }

  @Override
  public synchronized void newObjectImpl(Object objRef) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    Class<?> currentClass = objRef.getClass();
    while (currentClass != null) {
      for (Field field : currentClass.getDeclaredFields()) {
        String fieldName = field.getName();
        int fieldModifiers = field.getModifiers();
        if (!Modifier.isStatic(fieldModifiers) && !Modifier.isFinal(fieldModifiers)) {
          writeInstanceFieldInternal(objRef, fieldName);
        }
      }
      currentClass = currentClass.getSuperclass();
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void readStaticFieldImpl(String className, String fieldName) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    MemoryLocation memLoc = MemoryLocation.getStaticFieldLocation(className, fieldName);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.memoryRead(memLoc);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void writeStaticFieldImpl(String className, String fieldName) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    MemoryLocation memLoc = MemoryLocation.getStaticFieldLocation(className, fieldName);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.memoryWrite(memLoc);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void readArrayBucketImpl(Object arrayRef, int index) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    MemoryLocation memLoc = MemoryLocation.getArrayBucketLocation(arrayRef, index);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.memoryRead(memLoc);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on " + listener.toString()
            + ". Caused by:");
        ex.printStackTrace();
      }
    }
    isInAppCode = true;
  }

  @Override
  public synchronized void writeArrayBucketImpl(Object arrayRef, int index) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    writeArrayBucketInternal(arrayRef, index);
    isInAppCode = true;
  }

  @Override
  public boolean checkSystemArrayCopy(Object src, int srcPos, Object dest,
      int destPos, int length) {
    try {
      int srcLength = Array.getLength(src);
      int destLength = Array.getLength(dest);
      if (srcPos < 0 || destPos < 0 || srcPos + length > srcLength || destPos + length > destLength) {
        return false;
      }
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
  

  @Override
  public synchronized void newArrayImpl(Object arrayRef) {
    if (!isProfilingEnabled()) {
      return;
    }
    isInAppCode = false;
    newArrayRecursive(arrayRef);
    isInAppCode = true;
  }
  
  
  @Override
  public synchronized void executionSwitchOutImpl() {




    boolean oldIsInAppCode = isInAppCode;
    isInAppCode = false;
    switchOutCount.set(switchOutCount.get() + 1);
    isInAppCode = oldIsInAppCode;
  }
  
  
  @Override
  public synchronized void executionSwitchInImpl() {




    boolean oldIsInAppCode = isInAppCode;
    isInAppCode = false;
    int currentThreadSwitchOutCount = switchOutCount.get();
    if (currentThreadSwitchOutCount == 0) {
      IllegalStateException ex = new IllegalStateException("Execution has not been switched out.");
      isInAppCode = oldIsInAppCode;
      throw ex;
    }
    switchOutCount.set(currentThreadSwitchOutCount - 1);
    isInAppCode = oldIsInAppCode;
  }
  





  private boolean isProfilingEnabled() {
    if (!(executionStarted && isInAppCode)) {
      return false;
    }



    isInAppCode = false;
    if (switchOutCount.get() == 0) {
      isInAppCode = true;
      return true;
    } else {
      isInAppCode = true;
      return false;
    }
  }

  private void writeInstanceFieldInternal(Object objRef, String fieldName) {
    MemoryLocation memLoc = MemoryLocation.getInstanceFieldLocation(objRef, fieldName);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.memoryWrite(memLoc);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on "
            + listener.toString() + ". Caused by:");
        ex.printStackTrace();
      }
    }
  }

  private void writeArrayBucketInternal(Object arrayRef, int index) {
    MemoryLocation memLoc = MemoryLocation.getArrayBucketLocation(arrayRef, index);
    for (InstrumentationCallbackListener listener : callbackListeners) {
      try {
        listener.memoryWrite(memLoc);
      } catch (Throwable ex) {
        System.err.println("Error invoking callback method on "
            + listener.toString() + ". Caused by:");
        ex.printStackTrace();
      }
    }
  }

  private void newArrayRecursive(Object arrayRef) {
    for (int i = 0; i < Array.getLength(arrayRef); ++i) {
      writeArrayBucketInternal(arrayRef, i);
      Object element = Array.get(arrayRef, i);
      if (element != null && element.getClass().isArray()) {
        newArrayRecursive(element);
      }
    }
  }
}
