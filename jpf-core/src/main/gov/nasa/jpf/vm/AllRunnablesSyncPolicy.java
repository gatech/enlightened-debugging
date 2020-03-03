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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;


public class AllRunnablesSyncPolicy implements SyncPolicy {

  protected VM vm;
  protected boolean breakSingleChoice;
  protected boolean breakLockRelease;
  protected boolean breakNotify;
  protected boolean breakSleep;
  protected boolean breakYield;
  protected boolean breakPriority;
  
  public AllRunnablesSyncPolicy (Config config){
    breakSingleChoice = config.getBoolean("cg.break_single_choice", false);    
    breakLockRelease = config.getBoolean("cg.break_lock_release", true);
    breakNotify = config.getBoolean("cg.break_notify", true);
    breakSleep = config.getBoolean("cg.break_sleep", true);
    breakYield = config.getBoolean("cg.break_yield", true);
    breakPriority = config.getBoolean("cg.break_priority", true);
  }
  
  


  
  protected ThreadInfo[] getTimeoutRunnables (ApplicationContext appCtx){
    ThreadList tlist = vm.getThreadList();
    
    if (tlist.hasProcessTimeoutRunnables(appCtx)){
      return tlist.getProcessTimeoutRunnables(appCtx);
    } else {
      return tlist.getTimeoutRunnables();
    }
  }

    
  protected ChoiceGenerator<ThreadInfo> getRunnableCG (String id, ThreadInfo tiCurrent){
    ApplicationContext appCtx = tiCurrent.getApplicationContext();
    ThreadInfo[] choices = getTimeoutRunnables(appCtx);
    
    if (choices.length == 0){
      return null;
    }
    
    if ((choices.length == 1) && (choices[0] == tiCurrent) && !tiCurrent.isTimeoutWaiting()){ 
      if (!breakSingleChoice){
        return null;
      }
    }
    
    ChoiceGenerator<ThreadInfo> cg = new ThreadChoiceFromSet( id, choices, true);
    
    if(!vm.getThreadList().hasProcessTimeoutRunnables(appCtx)) {
      GlobalSchedulingPoint.setGlobal(cg);
    }
    
    return cg;
  }
  
  protected boolean setNextChoiceGenerator (ChoiceGenerator<ThreadInfo> cg){
    if (cg != null){
      return vm.getSystemState().setNextChoiceGenerator(cg); 
    } else {
      return false;
    }
  }
  
  
  protected boolean setNonBlockingCG (String id, ThreadInfo tiCurrent){
    if (!tiCurrent.isFirstStepInsn() || tiCurrent.isEmptyTransitionEnabled()) {
      if (vm.getSystemState().isAtomic()) {
        return false;
      } else {
        return setNextChoiceGenerator(getRunnableCG(id, tiCurrent));
      }
      
    } else {
      return false;  
    }
  }
  
  protected static ChoiceGenerator<ThreadInfo> blockedWithoutChoice = 
          new ThreadChoiceFromSet("BLOCKED_NO_CHOICE", new ThreadInfo[0], true);
  
  
  protected boolean setBlockingCG (String id, ThreadInfo tiCurrent){
    if (!tiCurrent.isFirstStepInsn() || tiCurrent.isEmptyTransitionEnabled()) {
      if (vm.getSystemState().isAtomic()) {
        vm.getSystemState().setBlockedInAtomicSection();
      }
      
      ChoiceGenerator<ThreadInfo> cg = getRunnableCG(id, tiCurrent);
      if (cg == null){ 
        if (vm.getThreadList().hasLiveThreads()){
          cg = blockedWithoutChoice;
        }
      }
      
      return setNextChoiceGenerator(cg);
      
    } else {
      return false;  
    }
  }
    
  
  protected boolean setMaybeBlockingCG (String id, ThreadInfo tiCurrent, ThreadInfo tiBlock){
    if (tiCurrent == tiBlock){
      return setBlockingCG( id, tiCurrent);
    } else {
      return setNonBlockingCG( id, tiCurrent);
    }
  }
  
  

  

  @Override
  public void initializeSyncPolicy (VM vm, ApplicationContext appCtx){
    this.vm  = vm;
  }
  
  @Override
  public void initializeThreadSync (ThreadInfo tiCurrent, ThreadInfo tiNew){
  }
    

  @Override
  public boolean setsBlockedThreadCG (ThreadInfo ti, ElementInfo ei){
    return setBlockingCG( BLOCK, ti);
  }
  
  @Override
  public boolean setsLockAcquisitionCG (ThreadInfo ti, ElementInfo ei){
    return setNonBlockingCG( LOCK, ti);
  }
  
  @Override
  public boolean setsLockReleaseCG (ThreadInfo ti, ElementInfo ei, boolean didUnblock){
    if (breakLockRelease){
      if (didUnblock){
        return setNonBlockingCG( RELEASE, ti);
      }
    }
    
    return false;
  }
  

  @Override
  public boolean setsTerminationCG (ThreadInfo ti){
    return setBlockingCG( TERMINATE, ti);
  }
  

  @Override
  public boolean setsWaitCG (ThreadInfo ti, long timeout){
    return setBlockingCG( WAIT, ti);
  }
  
  @Override
  public boolean setsNotifyCG (ThreadInfo ti, boolean didNotify){
    if (breakNotify){
      if (didNotify){
        return setNonBlockingCG( NOTIFY, ti);
      }
    }
    
    return false;
  }
  
  @Override
  public boolean setsNotifyAllCG (ThreadInfo ti, boolean didNotify){
    if (breakNotify){
      if (didNotify){
        return setNonBlockingCG( NOTIFYALL, ti);
      }
    }
    
    return false;
  }
  
    

  @Override
  public boolean setsStartCG (ThreadInfo tiCurrent, ThreadInfo tiStarted){
    return setNonBlockingCG( START, tiCurrent);
  }
  
  @Override
  public boolean setsYieldCG (ThreadInfo ti){
    if (breakYield){
      return setNonBlockingCG( YIELD, ti);
    } else {
      return false;
    }
  }
  
  @Override
  public boolean setsPriorityCG (ThreadInfo ti){
    if (breakPriority){
      return setNonBlockingCG( PRIORITY, ti);    
    } else {
      return false;
    }
  }
  
  @Override
  public boolean setsSleepCG (ThreadInfo ti, long millis, int nanos){
    if (breakSleep){
      return setNonBlockingCG( SLEEP, ti);
    } else {
      return false;
    }
  }
  
  @Override
  public boolean setsSuspendCG (ThreadInfo tiCurrent, ThreadInfo tiSuspended){
    return setMaybeBlockingCG( SUSPEND, tiCurrent, tiSuspended);      
  }
  
  @Override
  public boolean setsResumeCG (ThreadInfo tiCurrent, ThreadInfo tiResumed){
    return setNonBlockingCG( RESUME, tiCurrent);
  }
  
  @Override
  public boolean setsJoinCG (ThreadInfo tiCurrent, ThreadInfo tiJoin, long timeout){
    return setBlockingCG( JOIN, tiCurrent);      
  }
  
  @Override
  public boolean setsStopCG (ThreadInfo tiCurrent, ThreadInfo tiStopped){
    return setMaybeBlockingCG( STOP, tiCurrent, tiStopped);
  }
  
  @Override
  public boolean setsInterruptCG (ThreadInfo tiCurrent, ThreadInfo tiInterrupted){
    if (tiInterrupted.isWaiting()){
      return setNonBlockingCG( INTERRUPT, tiCurrent);
    } else {
      return false;
    }
  }
  
  

  @Override
  public boolean setsParkCG (ThreadInfo ti, boolean isAbsTime, long timeout){
    return setBlockingCG( PARK, ti);
  }
  
  @Override
  public boolean setsUnparkCG (ThreadInfo tiCurrent, ThreadInfo tiUnparked){

    if (tiUnparked.isBlocked()){
      return setNonBlockingCG( UNPARK, tiCurrent);
    } else {
      return false;
    }
  }

  

  
  
  @Override
  public void setRootCG (){
    ThreadInfo[] runnables = vm.getThreadList().getTimeoutRunnables();
    ChoiceGenerator<ThreadInfo> cg = new ThreadChoiceFromSet( ROOT, runnables, true);
    vm.getSystemState().setMandatoryNextChoiceGenerator( cg, "no ROOT choice generator");
  }
  
  

  @Override
  public boolean setsBeginAtomicCG (ThreadInfo ti){
    return setNonBlockingCG( BEGIN_ATOMIC, ti);
  }
  
  @Override
  public boolean setsEndAtomicCG (ThreadInfo ti){
    return setNonBlockingCG( END_ATOMIC, ti);
  }
  

  @Override
  public boolean setsRescheduleCG (ThreadInfo ti, String reason){
    return setNonBlockingCG( reason, ti);
  }
  

  @Override
  public boolean setsPostFinalizeCG (ThreadInfo tiFinalizer){

    return setBlockingCG( POST_FINALIZE, tiFinalizer);
  }
  

}
