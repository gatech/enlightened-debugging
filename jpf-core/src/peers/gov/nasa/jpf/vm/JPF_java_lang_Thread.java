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

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.util.JPFLogger;



public class JPF_java_lang_Thread extends NativePeer {

  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.vm.ThreadInfo");
  
  
  
  @MJI
  public void init0__Ljava_lang_ThreadGroup_2Ljava_lang_Runnable_2Ljava_lang_String_2J__V (MJIEnv env,
                         int objRef, int groupRef, int runnableRef, int nameRef, long stackSize) {
    VM vm = env.getVM();
    


    vm.createThreadInfo( objRef, groupRef, runnableRef, nameRef);
  }

  @MJI
  public boolean isAlive____Z (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    if (ti != null){
      return ti.isAlive();
    } else {
      return false; 
    }
  }

  @MJI
  public void setDaemon0__Z__V (MJIEnv env, int objref, boolean isDaemon) {
    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    ti.setDaemon(isDaemon);
  }

  @MJI
  public void dumpStack____V (MJIEnv env, int clsObjRef){
    ThreadInfo ti = env.getThreadInfo();
    ti.printStackTrace(); 
  }

  @MJI
  public void setName0__Ljava_lang_String_2__V (MJIEnv env, int objref, int nameRef) {

    if (nameRef == MJIEnv.NULL) {
      env.throwException("java.lang.IllegalArgumentException");

      return;
    }









    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    ti.setName(env.getStringObject(nameRef));
  }

  @MJI
  public void setPriority0__I__V (MJIEnv env, int objref, int prio) {

    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    
    if (prio != ti.getPriority()){
      ti.setPriority(prio);
    

      if (ti.getScheduler().setsPriorityCG(ti)){
        env.repeatInvocation();
        return;
      }
    }
  }

  @MJI
  public int countStackFrames____I (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    return ti.countStackFrames();
  }

  @MJI
  public int currentThread____Ljava_lang_Thread_2 (MJIEnv env, int clsObjRef) {
    env.setDependencyTracked(true);
    ThreadInfo ti = env.getThreadInfo();
    return ti.getThreadObjectRef();
  }

  @MJI
  public boolean holdsLock__Ljava_lang_Object_2__Z (MJIEnv env, int clsObjRef, int objref) {
    ThreadInfo  ti = env.getThreadInfo();
    ElementInfo ei = env.getElementInfo(objref);

    return ei.isLockedBy(ti);
  }

  @MJI
  public void interrupt____V (MJIEnv env, int objref) {
    ThreadInfo tiCurrent = env.getThreadInfo();
    ThreadInfo tiInterrupted = env.getThreadInfoForObjRef(objref);

    if (!tiCurrent.isFirstStepInsn()) {
      tiInterrupted.interrupt();
    }
    
    if (tiCurrent.getScheduler().setsInterruptCG(tiCurrent, tiInterrupted)) {
      env.repeatInvocation();
      return;
    }
  }



  @MJI
  public boolean isInterrupted____Z (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    return ti.isInterrupted(false);
  }

  @MJI
  public boolean interrupted____Z (MJIEnv env, int clsObjRef) {
    ThreadInfo ti = env.getThreadInfo();
    return ti.isInterrupted(true);
  }

  @MJI
  public void start____V (MJIEnv env, int objref) {
    ThreadInfo tiCurrent = env.getThreadInfo();
    ThreadInfo tiStarted = env.getThreadInfoForObjRef(objref);
    VM vm = tiCurrent.getVM();


    if (!tiCurrent.isFirstStepInsn()){
      if (tiStarted.isStopped()) {


        tiStarted.setTerminated();
        return;
      }

      if (!tiStarted.isNew()) {



        env.throwException("java.lang.IllegalThreadStateException");
        return;
      }

      int runnableRef = tiStarted.getRunnableRef();
      if (runnableRef == MJIEnv.NULL) {

        runnableRef = objref;
      }

      ElementInfo eiTarget = env.getElementInfo(runnableRef);
      ClassInfo   ci = eiTarget.getClassInfo();
      MethodInfo  miRun = ci.getMethod("run()V", true);




      DirectCallStackFrame runFrame = miRun.createRunStartStackFrame(tiStarted);
      runFrame.setReferenceArgument(0, runnableRef, null);
            
      tiStarted.pushFrame(runFrame);
      tiStarted.setState(ThreadInfo.State.RUNNING);
      
      vm.notifyThreadStarted(tiStarted);
    }
    

    if (tiCurrent.getScheduler().setsStartCG(tiCurrent, tiStarted)){
      env.repeatInvocation();
    }

  }

  @MJI
  public void yield____V (MJIEnv env, int clsObjRef) {
    ThreadInfo ti = env.getThreadInfo();
    if (ti.getScheduler().setsYieldCG(ti)){
      env.repeatInvocation();
    }
  }

  @MJI
  public void sleep__JI__V (MJIEnv env, int clsObjRef, long millis, int nanos) {
    ThreadInfo ti = env.getThreadInfo();


    if (ti.getScheduler().setsSleepCG(ti, millis, nanos)){
      ti.setSleeping();
      env.repeatInvocation();
      return;
    }
    
    if (ti.isSleeping()){
      ti.setRunning();
    }
  }

  @MJI
  public void suspend____V (MJIEnv env, int threadObjRef) {
    ThreadInfo tiCurrent = env.getThreadInfo();
    ThreadInfo tiSuspended = env.getThreadInfoForObjRef(threadObjRef);

    if (tiSuspended.isTerminated()) {
      return;  
    }
    
    if (!tiCurrent.isFirstStepInsn()){ 
      tiSuspended.suspend();
    }
    

    if (tiCurrent.getScheduler().setsSuspendCG(tiCurrent, tiSuspended)){
      env.repeatInvocation();      
    }
  }

  @MJI
  public void resume____V (MJIEnv env, int threadObjRef) {
    ThreadInfo tiCurrent = env.getThreadInfo();
    ThreadInfo tiResumed = env.getThreadInfoForObjRef(threadObjRef);

    if (tiCurrent == tiResumed){
      return;  
    }

    if (tiResumed.isTerminated()) {
      return;  
    }

    if (!tiCurrent.isFirstStepInsn()) { 
      tiResumed.resume();
    }
    

    if (tiCurrent.getScheduler().setsResumeCG(tiCurrent, tiResumed)){
      env.repeatInvocation();
      return;
    }
  }

  
  protected void join0 (MJIEnv env, int joineeRef, long timeout){
    ThreadInfo tiCurrent = env.getThreadInfo();
    ThreadInfo tiJoinee = env.getThreadInfoForObjRef(joineeRef);
    ElementInfo eiJoinee = env.getModifiableElementInfo(joineeRef); 

    if (timeout < 0) {
      env.throwException("java.lang.IllegalArgumentException", "timeout value is negative");
      return;
    }
      
    if (tiCurrent.isInterrupted(true)){ 

      eiJoinee.setMonitorWithoutLocked(tiCurrent);
      

      env.throwInterrupt();
      return;
    }
  
    if (!tiCurrent.isFirstStepInsn()){ 
      if (tiJoinee.isAlive()) {

        eiJoinee.wait( tiCurrent, timeout, false);
      } else {
        return; 
      }
    }    

    if (tiCurrent.getScheduler().setsJoinCG(tiCurrent, tiJoinee, timeout)) {
      env.repeatInvocation();
      return;
    }
    

    switch (tiCurrent.getState()) {
      case WAITING:
      case TIMEOUT_WAITING:
        throw new JPFException("blocking join without transition break");        
      
      case UNBLOCKED:


        eiJoinee.lockNotified(tiCurrent);
        break;

      case TIMEDOUT:
        eiJoinee.resumeNonlockedWaiter(tiCurrent);
        break;

      case RUNNING:
        if (tiJoinee.isAlive()) { 
          eiJoinee.wait(tiCurrent, timeout, false); 
          env.repeatInvocation();
        }
        break;
    }
  }

  @MJI
  public void join____V (MJIEnv env, int objref){
    join0(env,objref,0);
  }

  @MJI
  public void join__J__V (MJIEnv env, int objref, long millis) {
    join0(env,objref,millis);

  }

  @MJI
  public void join__JI__V (MJIEnv env, int objref, long millis, int nanos) {
    join0(env,objref,millis); 
  }

  @MJI
  public int getState0____I (MJIEnv env, int objref) {

    ThreadInfo ti = env.getThreadInfoForObjRef(objref);

    switch (ti.getState()) {
      case NEW:
        return 1;
      case RUNNING:
        return 2;
      case BLOCKED:
        return 0;
      case UNBLOCKED:
        return 2;
      case WAITING:
        return 5;
      case TIMEOUT_WAITING:
        return 4;
      case SLEEPING:
        return 4;
      case NOTIFIED:
        return 0;
      case INTERRUPTED:
        return 0;
      case TIMEDOUT:
        return 2;
      case TERMINATED:
        return 3;
      default:
        throw new JPFException("illegal thread state: " + ti.getState());
    }
  }

  @MJI
  public long getId____J (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfoForObjRef(objref);
    return ti.getId();
  }
  
  @MJI
  public void stop____V (MJIEnv env, int threadRef) {
    stop__Ljava_lang_Throwable_2__V(env, threadRef, MJIEnv.NULL);
  }

  @MJI
  public void stop__Ljava_lang_Throwable_2__V (MJIEnv env, int threadRef, int throwableRef) {
    ThreadInfo tiCurrent = env.getThreadInfo();
    ThreadInfo tiStopped = env.getThreadInfoForObjRef(threadRef);

    if (tiStopped.isTerminated() || tiStopped.isStopped()) {
      return; 
    }

    if (tiCurrent.getScheduler().setsStopCG(tiCurrent, tiStopped)){
      env.repeatInvocation();
      return;
    }


    tiStopped.setStopped(throwableRef);
  }
}
