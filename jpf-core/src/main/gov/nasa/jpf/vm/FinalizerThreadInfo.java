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

import gov.nasa.jpf.vm.choice.BreakGenerator;
import java.util.ArrayList;
import java.util.List;


public class FinalizerThreadInfo extends ThreadInfo {
  
  static final String FINALIZER_NAME = "finalizer";
  
  ChoiceGenerator<?> replacedCG;
  
  protected FinalizerThreadInfo (VM vm, ApplicationContext appCtx, int id) {
    super(vm, id, appCtx);
    
    ci = appCtx.getSystemClassLoader().getResolvedClassInfo("gov.nasa.jpf.FinalizerThread");
    threadData.name = FINALIZER_NAME;
    
    tempFinalizeQueue = new ArrayList<ElementInfo>();
  }
  
  protected void createFinalizerThreadObject (SystemClassLoaderInfo sysCl){
    Heap heap = getHeap();

    ElementInfo eiThread = heap.newObject( ci, this);
    objRef = eiThread.getObjectRef();
    
    ElementInfo eiName = heap.newString(FINALIZER_NAME, this);
    int nameRef = eiName.getObjectRef();
    eiThread.setReferenceField("name", nameRef);
    


    int grpRef = ThreadInfo.getCurrentThread().getThreadGroupRef();
    eiThread.setReferenceField("group", grpRef);
    
    eiThread.setIntField("priority", Thread.MAX_PRIORITY-2);

    ClassInfo ciPermit = sysCl.getResolvedClassInfo("java.lang.Thread$Permit");
    ElementInfo eiPermit = heap.newObject( ciPermit, this);
    eiPermit.setBooleanField("blockPark", true);
    eiThread.setReferenceField("permit", eiPermit.getObjectRef());

    addToThreadGroup(getElementInfo(grpRef));
    
    addId( objRef, id);
    
    threadData.name = FINALIZER_NAME;


    startFinalizerThread();
    
    eiThread.setBooleanField("done", false);
    ElementInfo finalizeQueue = getHeap().newArray("Ljava/lang/Object;", 0, this);
    eiThread.setReferenceField("finalizeQueue", finalizeQueue.getObjectRef());
    

    ElementInfo lock = getHeap().newObject(appCtx.getSystemClassLoader().objectClassInfo, this);
    eiThread.setReferenceField("semaphore", lock.getObjectRef());
    


    waitOnSemaphore();

    assert this.isWaiting();
  }
  
  
  protected void startFinalizerThread() {
    MethodInfo mi = ci.getMethod("run()V", false);
    DirectCallStackFrame frame = mi.createDirectCallStackFrame(this, 0);
    frame.setReferenceArgument(0, objRef, frame);
    pushFrame(frame);
  }
  
  public boolean hasQueuedFinalizers() {
    ElementInfo queue = getFinalizeQueue();
    return (queue!=null && queue.asReferenceArray().length>0);
  }
  
  public ElementInfo getFinalizeQueue() {
    ElementInfo ei = vm.getModifiableElementInfo(objRef);
    ElementInfo queue = null;
    
    if(ei!=null) {
      int queueRef = ei.getReferenceField("finalizeQueue");
      queue = vm.getModifiableElementInfo(queueRef);
      return queue;
    }
    
    return queue;
  }
  



  List<ElementInfo> tempFinalizeQueue;
  
  
  public ElementInfo getFinalizerQueuedInstance(ElementInfo ei) {
    ei = ei.getModifiableInstance();
    

    ei.setFinalized();
    
    tempFinalizeQueue.add(ei);
    
    return ei;
  }
  
  
  void processNewFinalizables() {
    if(!tempFinalizeQueue.isEmpty()) {
      
      ElementInfo oldQueue = getFinalizeQueue();
      int[] oldValues = oldQueue.asReferenceArray();    
      int len = oldValues.length;
      
      int n = tempFinalizeQueue.size();
      
      ElementInfo newQueue = getHeap().newArray("Ljava/lang/Object;", len+n, this);
      int[] newValues = newQueue.asReferenceArray();
      
      System.arraycopy(oldValues, 0, newValues, 0, len);
      
      for(ElementInfo ei: tempFinalizeQueue) {
        newValues[len++] = ei.getObjectRef();
      }
      
      vm.getModifiableElementInfo(objRef).setReferenceField("finalizeQueue", newQueue.getObjectRef());
      tempFinalizeQueue.clear();
      
      assert hasQueuedFinalizers();
    }
  }
  
  public boolean scheduleFinalizer() {
    if(hasQueuedFinalizers() && !isRunnable()) {
      SystemState ss = vm.getSystemState();
      replacedCG = ss.getNextChoiceGenerator();
      



      ss.nextCg = null;
      



      ss.setNextChoiceGenerator(new BreakGenerator("finalize", this, false));
      checkNextChoiceGeneratorSet("no transition break prior to finalize");
      

      notifyOnSemaphore();
      assert this.isRunnable();
      
      return true;
    } 
    
    return false;
  }
  
  protected void waitOnSemaphore() {
    int lockRef = vm.getElementInfo(objRef).getReferenceField("semaphore");
    ElementInfo lock = vm.getModifiableElementInfo(lockRef);
    
    lock.wait(this, 0, false);
  }
  
  protected void notifyOnSemaphore() {
    int lockRef = vm.getElementInfo(objRef).getReferenceField("semaphore");
    ElementInfo lock = vm.getModifiableElementInfo(lockRef);
    
    lock.notifies(vm.getSystemState(), this, false);
  }
  


  protected boolean isIdle() {
    if(this.isWaiting()) {
      if(this.lockRef == vm.getElementInfo(objRef).getReferenceField("semaphore")) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean isSystemThread() {
    return true;
  }
}
