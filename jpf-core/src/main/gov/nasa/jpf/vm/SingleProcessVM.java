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

import java.util.HashMap;
import java.util.Map;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.Predicate;
import gov.nasa.jpf.vm.choice.BreakGenerator;



public class SingleProcessVM extends VM {

  protected ApplicationContext appCtx; 
  
  protected Predicate<ThreadInfo> runnablePredicate;
  protected Predicate<ThreadInfo> daemonRunnable;
  
  protected SingleProcessVM (){}

  public SingleProcessVM (JPF jpf, Config conf) {
    super(jpf, conf);
    
    appCtx = createApplicationContext();
    
    initializePredicates();
  }
    
  void initializePredicates() {

    runnablePredicate = new Predicate<ThreadInfo>(){
      @Override
	public boolean isTrue (ThreadInfo ti){
        return (ti.isRunnable());
      }
    };
    
    daemonRunnable = new Predicate<ThreadInfo>(){
      @Override
	public boolean isTrue (ThreadInfo ti){
        return (ti.isDaemon() && ti.isRunnable());
      }
    };
  }
  
  protected ApplicationContext createApplicationContext (){
    String clsName;
    String[] args = null;
    
    String[] freeArgs = config.getFreeArgs();
    clsName = config.getProperty("target"); 
    
    if (clsName == null){
      if (freeArgs != null){ 

        clsName = freeArgs[0];
        
        if (freeArgs.length > 1){ 
          args = Misc.arrayWithoutFirst(freeArgs, 1);
        } else {
          args = config.getStringArray("target.args");
        }
      }
    } else {

      args = config.getCompactStringArray("target.args");
      if (args == null){
        if (freeArgs != null){
          args = freeArgs;
        }
      }
    }
    

    if (args == null){
      args = EMPTY_ARGS;
    }
    if (clsName == null){
      throw new JPFConfigException("no target class specified, terminating");
    }
    if (!isValidClassName(clsName)){
      throw new JPFConfigException("main class not a valid class name: " + clsName);      
    }
    

    String mainEntry = config.getProperty("target.entry", "main([Ljava/lang/String;)V");

    String host = config.getString("target.host", "localhost");
    
    SystemClassLoaderInfo sysCli = createSystemClassLoaderInfo(0);
    
    return new ApplicationContext( 0, clsName, mainEntry, args, host, sysCli);
  }
  
  
  @Override
  public boolean initialize(){
    try {

      scheduler.initialize(this, appCtx);
      
      ThreadInfo tiMain = initializeMainThread(appCtx, 0);
      initializeFinalizerThread(appCtx, 1);

      if (tiMain == null) {
        return false; 
      }

      initSystemState(tiMain);
      initialized = true;
      notifyVMInitialized();
      return true;
      
    } catch (JPFConfigException cfe){
      log.severe(cfe.getMessage());
      return false;
    } catch (ClassInfoException cie){
      log.severe(cie.getMessage());
      return false;
    }

  }

  
  @Override
  public int getNumberOfApplications(){
    return 1;
  }
  
  @Override
  public String getSUTName() {
    return appCtx.mainClassName;
  }

  @Override
  public String getSUTDescription(){
    StringBuilder sb = new StringBuilder();
    sb.append(appCtx.mainClassName);
    sb.append('.');
    sb.append(Misc.upToFirst( appCtx.mainEntry, '('));
    
    sb.append('(');
    String[] args = appCtx.args;
    for (int i=0; i<args.length; i++){
      if (i>0){
        sb.append(',');
      }
      sb.append('"');
      sb.append(args[i]);
      sb.append('"');
    }
    sb.append(')');
    return sb.toString();
  }
  
  @Override
  public ApplicationContext getApplicationContext(int obj) {
    return appCtx;
  }

  @Override
  public ApplicationContext[] getApplicationContexts(){
    return new ApplicationContext[] { appCtx };
  }
  
  @Override
  public ApplicationContext getCurrentApplicationContext(){
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    if (ti != null){
      return ti.getApplicationContext();
    } else {
      return appCtx;
    }
  }
  
  
  @Override
  public boolean isEndState () {


    
    boolean hasNonTerminatedDaemon = getThreadList().hasAnyMatching(getUserLiveNonDaemonPredicate());
    boolean hasRunnable = getThreadList().hasAnyMatching(getUserTimedoutRunnablePredicate());
    boolean isEndState = !(hasNonTerminatedDaemon && hasRunnable);
    
    if(processFinalizers) {
      if(isEndState) {
        if(getFinalizerThread().isRunnable()) {
          return false;
        }
      }
    }
    
    return isEndState;
  }

  @Override
  public boolean isDeadlocked () { 
    boolean hasNonDaemons = false;
    boolean hasBlockedThreads = false;

    if (ss.isBlockedInAtomicSection()) {
      return true; 
    }

    ThreadInfo[] threads = getThreadList().getThreads();

    boolean hasUserThreads = false;
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      
      if (ti.isAlive()){
        hasNonDaemons |= !ti.isDaemon();


        if (ti.isTimeoutRunnable()) { 
          return false;
        }
        
        if(!ti.isSystemThread()) {
          hasUserThreads = true;
        }


        hasBlockedThreads = true;
      }
    }

    boolean isDeadlock = hasNonDaemons && hasBlockedThreads;
    
    if(processFinalizers && isDeadlock && !hasUserThreads) {


      return (!getFinalizerThread().isIdle());
    }
    
    return isDeadlock;
  }
  
  @Override
  public void terminateProcess (ThreadInfo ti) {
    SystemState ss = getSystemState();
    ThreadInfo[] liveThreads = getLiveThreads();
    ThreadInfo finalizerTi = null;

    for (int i = 0; i < liveThreads.length; i++) {
      if(!liveThreads[i].isSystemThread()) {

        liveThreads[i].setTerminated();
      } else {


        finalizerTi = liveThreads[i];
      }
    }
    
    ss.setMandatoryNextChoiceGenerator( new BreakGenerator("exit", ti, true), "exit without break CG");
    

    if(finalizerTi != null) {
      assert finalizerTi.isAlive();
      activateGC();
    }
  }
  
  @Override
  public Map<Integer,IntTable<String>> getInitialInternStringsMap() {
    Map<Integer,IntTable<String>> interns = new HashMap<Integer,IntTable<String>>();
    interns.put(0, appCtx.getInternStrings());
    return interns;
  }
  

  
  @Override
  public Predicate<ThreadInfo> getRunnablePredicate() {
    return runnablePredicate;
  }
  
  @Override
  public Predicate<ThreadInfo> getAppTimedoutRunnablePredicate() {
    return getRunnablePredicate();
  }
  
  @Override
  public Predicate<ThreadInfo> getDaemonRunnablePredicate() {
    return daemonRunnable;
  }
  


  @Override
  void updateFinalizerQueues () {
    getFinalizerThread().processNewFinalizables();
  }
}
