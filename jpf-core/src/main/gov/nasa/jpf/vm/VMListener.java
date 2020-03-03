/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */


package gov.nasa.jpf.vm;

import gov.nasa.jpf.JPFListener;
import gov.nasa.jpf.jvm.ClassFile;


public interface VMListener extends JPFListener {
  
  
  void vmInitialized (VM vm);
    
  
  void executeInstruction (VM vm, ThreadInfo currentThread, Instruction instructionToExecute);
  
  
  void instructionExecuted (VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction);
  
  
  void threadStarted (VM vm, ThreadInfo startedThread);
    
  
  void threadBlocked (VM vm, ThreadInfo blockedThread, ElementInfo lock);
  
  
  void threadWaiting (VM vm, ThreadInfo waitingThread);

  
  void threadNotified (VM vm, ThreadInfo notifiedThread);
    
  
  void threadInterrupted (VM vm, ThreadInfo interruptedThread);
  
  
  void threadTerminated (VM vm, ThreadInfo terminatedThread);

  
  void threadScheduled (VM vm, ThreadInfo scheduledThread); 

  
  public void loadClass (VM vm, ClassFile cf);
  
  
  void classLoaded (VM vm, ClassInfo loadedClass);
  
  
  void objectCreated (VM vm, ThreadInfo currentThread, ElementInfo newObject);
  
  
  void objectReleased (VM vm, ThreadInfo currentThread, ElementInfo releasedObject);
  
  
  void objectLocked (VM vm, ThreadInfo currentThread, ElementInfo lockedObject);
  
  
  void objectUnlocked (VM vm, ThreadInfo currentThread, ElementInfo unlockedObject);
  
  
  void objectWait (VM vm, ThreadInfo currentThread, ElementInfo waitingObject);
  
  
  void objectNotify (VM vm, ThreadInfo currentThread, ElementInfo notifyingObject);

  
  void objectNotifyAll (VM vm, ThreadInfo currentThread, ElementInfo notifyingObject);
  
  
  
  void objectExposed (VM vm, ThreadInfo currentThread, ElementInfo fieldOwnerObject, ElementInfo exposedObject);
  
  
  void objectShared (VM vm, ThreadInfo currentThread, ElementInfo sharedObject);

  
  void gcBegin (VM vm);
  
  void gcEnd (VM vm);
  
  
  void exceptionThrown (VM vm, ThreadInfo currentThread, ElementInfo thrownException);

  
  void exceptionBailout (VM vm, ThreadInfo currentThread);

  
  void exceptionHandled (VM vm, ThreadInfo currentThread);

  
  void choiceGeneratorRegistered (VM vm, ChoiceGenerator<?> nextCG, ThreadInfo currentThread, Instruction executedInstruction);

  
  void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG);
  
  
  void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG);
  
  
  void choiceGeneratorProcessed (VM vm, ChoiceGenerator<?> processedCG);

  
  void methodEntered (VM vm, ThreadInfo currentThread, MethodInfo enteredMethod);

  
  void methodExited (VM vm, ThreadInfo currentThread, MethodInfo exitedMethod);

}

