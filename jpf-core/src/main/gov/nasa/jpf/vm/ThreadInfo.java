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

import gov.nasa.jpf.vm.bytecode.ReturnInstruction;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.SystemAttribute;
import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.Predicate;
import gov.nasa.jpf.util.StringSetMatcher;
import gov.nasa.jpf.vm.choice.BreakGenerator;

import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;



public class ThreadInfo extends InfoObject
     implements Iterable<StackFrame>, Comparable<ThreadInfo>, Restorable<ThreadInfo> {

  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.vm.ThreadInfo");
  static int[] emptyLockRefs = new int[0];


  public enum State {
    NEW,  
    RUNNING,
    BLOCKED,  
    UNBLOCKED,  
    WAITING,  
    TIMEOUT_WAITING,
    NOTIFIED,  
    INTERRUPTED,  
    TIMEDOUT,  
    TERMINATED,
    SLEEPING
  };

  static final int[] emptyRefArray = new int[0];
  static final String MAIN_NAME = "main";
  
  
  static ThreadInfo currentThread;

  protected class StackIterator implements Iterator<StackFrame> {
    StackFrame frame = top;

    @Override
	public boolean hasNext() {
      return frame != null;
    }

    @Override
	public StackFrame next() {
      if (frame != null){
        StackFrame ret = frame;
        frame = frame.getPrevious();
        return ret;

      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
	public void remove() {
      throw new UnsupportedOperationException("can't remove StackFrames");
    }
  }

  protected class InvokedStackIterator extends StackIterator implements Iterator<StackFrame> {
    InvokedStackIterator() {
      frame = getLastInvokedStackFrame();
    }

    @Override
	public StackFrame next() {
      if (frame != null){
        StackFrame ret = frame;
        frame = null;
        for (StackFrame f=ret.getPrevious(); f != null; f = f.getPrevious()){
          if (!f.isDirectCallFrame()){
            frame = f;
            break;
          }
        }
        return ret;

      } else {
        throw new NoSuchElementException();
      }
    }
  }
  
  

      

  protected ExceptionInfo pendingException;


  protected ThreadData threadData;

  


  protected StackFrame top = null;


  protected int stackDepth;

  





  protected int tlIdx;

  



  protected ApplicationContext appCtx;
  

  protected int id;
  
  protected int objRef; 
  protected ClassInfo ci; 
  protected int targetRef; 
  

  static final int   ATTR_STORE_MASK = 0x0000ffff;


  static final int ATTR_DATA_CHANGED       = 0x10000;
  static final int ATTR_STACK_CHANGED      = 0x20000;
  

  static final int ATTR_ENABLE_EMPTY_TRANSITION = 0x4000;
  

  static final int ATTR_SKIP_INSN_EXEC      = 0x100000;
  

  static final int ATTR_SKIP_INSN_LOG       = 0x200000;
  
  
  static final int ATTR_ATTRIBUTE_CHANGED  = 0x80000000;




  static final int ATTR_STOPPED = 0x0001;


  static final int ATTR_ANY_CHANGED = (ATTR_DATA_CHANGED | ATTR_STACK_CHANGED | ATTR_ATTRIBUTE_CHANGED);
  static final int ATTR_SET_STOPPED = (ATTR_STOPPED | ATTR_ATTRIBUTE_CHANGED);

  protected int attributes;

  
  
  protected int executedInstructions;

  
  protected SUTExceptionRequest pendingSUTExceptionRequest;
  
  
  protected DirectCallStackFrame returnedDirectCall;

  
  protected Instruction nextPc;

  
  MJIEnv env;

  
  VM vm;

  
  int[] lockedObjectReferences;

  
  int lockRef = MJIEnv.NULL;

  Memento<ThreadInfo> cachedMemento; 


  static class TiMemento implements Memento<ThreadInfo> {

    ThreadInfo ti;

    ThreadData threadData;
    StackFrame top;
    int stackDepth;
    int attributes;

    TiMemento (ThreadInfo ti){
      this.ti = ti;
      
      threadData = ti.threadData;  
      top = ti.top; 
      stackDepth = ti.stackDepth; 
      attributes = (ti.attributes & ATTR_STORE_MASK);

      ti.freeze();
      ti.markUnchanged();
    }

    @Override
	public ThreadInfo restore(ThreadInfo ignored) {
      ti.resetVolatiles();

      ti.threadData = threadData;
      ti.top = top;
      ti.stackDepth = stackDepth;
      ti.attributes = attributes;

      ti.markUnchanged();

      return ti;
    }
  }








  
  static StringSetMatcher haltOnThrow;

  
  static boolean ignoreUncaughtHandlers;
  
  
  static boolean passUncaughtHandler;

  
  
  static int maxTransitionLength;

  
  static boolean init (Config config) {
    currentThread = null;
    
    globalTids = new HashMap<Integer, Integer>();

    String[] haltOnThrowSpecs = config.getStringArray("vm.halt_on_throw");
    if (haltOnThrowSpecs != null){
      haltOnThrow = new StringSetMatcher(haltOnThrowSpecs);
    }
    
    ignoreUncaughtHandlers = config.getBoolean( "vm.ignore_uncaught_handler", true);
    passUncaughtHandler = config.getBoolean( "vm.pass_uncaught_handler", true);

    maxTransitionLength = config.getInt("vm.max_transition_length", 5000);

    return true;
  }
    


  
  
  static Map<Integer, Integer> globalTids;  
  
  
  protected int computeId (int objRef) {
    Integer id = globalTids.get(objRef);
    
    if(id == null) {
      id = globalTids.size();
      addId(objRef, id);
    }

    return id;
  }

  static void addId(int objRef, int id) {
    globalTids.put(objRef, id);
  }
  
  
  protected ThreadInfo (VM vm, int id, ApplicationContext appCtx) {
    this.id = id;
    this.appCtx = appCtx;
    
    init(vm);


    
    ci = appCtx.getSystemClassLoader().getThreadClassInfo();
    targetRef = MJIEnv.NULL;
    threadData.name = MAIN_NAME;
  }

  
  protected ThreadInfo (VM vm, int objRef, int groupRef, int runnableRef, int nameRef, ThreadInfo parent) {
    id = computeId(objRef);
    this.appCtx = parent.getApplicationContext();
    
    init(vm); 
    
    ElementInfo ei = vm.getModifiableElementInfo(objRef);  
    ei.setExposed(parent, null);        
    
    this.ci = ei.getClassInfo();    
    this.objRef = objRef;
    this.targetRef = runnableRef;
   
    threadData.name = vm.getElementInfo(nameRef).asString();
    
    vm.getScheduler().initializeThreadSync(parent, this);
    

  }
  
  protected void init(VM vm){


    
    this.vm = vm;

    threadData = new ThreadData();
    threadData.state = State.NEW;
    threadData.priority = Thread.NORM_PRIORITY;
    threadData.isDaemon = false;
    threadData.lockCount = 0;
    threadData.suspendCount = 0;

    



    top = null;
    stackDepth = 0;

    lockedObjectReferences = emptyLockRefs;

    markUnchanged();
    attributes |= ATTR_DATA_CHANGED; 
    env = new MJIEnv(this);
  }
  
  @Override
  public Memento<ThreadInfo> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<ThreadInfo> getMemento(){
    return new TiMemento(this);
  }
  
  void freeze() {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()) {
      frame.freeze();
    }
  }



  public Memento<ThreadInfo> getCachedMemento(){
    return cachedMemento;
  }

  public void setCachedMemento(Memento<ThreadInfo> memento){
    cachedMemento = memento;
  }

  public static ThreadInfo getCurrentThread() {
    return currentThread;
  }

  public boolean isExecutingAtomically () {
    return vm.getSystemState().isAtomic();
  }

  public boolean holdsLock (ElementInfo ei) {
    int objRef = ei.getObjectRef();
    
    for (int i=0; i<lockedObjectReferences.length; i++) {
      if (lockedObjectReferences[i] == objRef) {
        return true;
      }
    }
    
    return false;
  }

  public VM getVM () {
    return vm;
  }

  
  public boolean isEmptyTransitionEnabled (){
    return (attributes & ATTR_ENABLE_EMPTY_TRANSITION) != 0;
  }
  
  public void enableEmptyTransition (){
    attributes |= ATTR_ENABLE_EMPTY_TRANSITION;
  }
  
  public void resetEmptyTransition(){
    attributes &= ~ATTR_ENABLE_EMPTY_TRANSITION;
  }
  
  
  public boolean isFirstStepInsn() {
    int nInsn = executedInstructions;
    
    if (nInsn == 0) {

      return true;
      
    } else if (nInsn == 1 && nextPc != null) {




      return true;
    }
    
    return false;
  }
  
  
  public int getExecutedInstructions(){
    return executedInstructions;
  }
  
  
  public boolean isPreExec() {
    return (nextPc == null);
  }




  
  public void setState (State newStatus) {
    State oldStatus = threadData.state;

    if (oldStatus != newStatus) {

      assert (oldStatus != State.TERMINATED) : "can't resurrect thread " + this + " to " + newStatus.name();

      threadDataClone().state = newStatus;

      switch (newStatus) {
      case NEW:
        break; 
      case RUNNING:




        break;
      case TERMINATED:
        vm.notifyThreadTerminated(this);
        break;
      case BLOCKED:
        assert lockRef != MJIEnv.NULL;
        vm.notifyThreadBlocked(this);
        break;
      case UNBLOCKED:
        assert lockRef == MJIEnv.NULL;
        break; 
      case WAITING:
        assert lockRef != MJIEnv.NULL;
        vm.notifyThreadWaiting(this);
        break;
      case INTERRUPTED:
        vm.notifyThreadInterrupted(this);
        break;
      case NOTIFIED:
        assert lockRef != MJIEnv.NULL;
        vm.notifyThreadNotified(this);
        break;
      }

      if (log.isLoggable(Level.FINE)){
        log.fine("setStatus of " + getName() + " from "
                 + oldStatus.name() + " to " + newStatus.name());
      }
    }
  }

  void setBlockedState (int objref) {
    
    State currentState = threadData.state;
    switch (currentState){
      case NEW:
      case RUNNING:
      case UNBLOCKED:
        lockRef = objref;
        setState(State.BLOCKED);
        break;

      default:
        assert false : "thread " + this + "can't be blocked in state: " + currentState.name();
    }
  }

  void setNotifiedState() {
    State currentState = threadData.state;
    switch (currentState){
      case BLOCKED:
      case INTERRUPTED: 
      case NOTIFIED:

        break;
      case WAITING:
      case TIMEOUT_WAITING:
        setState(State.NOTIFIED);
        break;

      default:
        assert false : "thread " + this + "can't be notified in state: " + currentState.name();
    }
  }

  
  public State getState () {
    return threadData.state;
  }


  
  public boolean isRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {
    case RUNNING:
    case UNBLOCKED:
      return true;
    case SLEEPING:
      return true;    
    case TIMEDOUT:
      return true;    
    default:
      return false;
    }
  }

  public boolean willBeRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {
    case RUNNING:
    case UNBLOCKED:
      return true;
    case TIMEOUT_WAITING: 
    case SLEEPING:
      return true;
    default:
      return false;
    }
  }

  public boolean isNew () {
    return (threadData.state == State.NEW);
  }

  public boolean isTimeoutRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {

    case RUNNING:
    case UNBLOCKED:
    case SLEEPING:
      return true;

    case TIMEOUT_WAITING:


      if (lockRef != MJIEnv.NULL){
        ElementInfo ei = vm.getElementInfo(lockRef);
        return ei.canLock(this);
      } else {
        return true;
      }

    default:
      return false;
    }
  }

  public boolean isTimedOut() {
    return (threadData.state == State.TIMEDOUT);
  }

  public boolean isTimeoutWaiting() {
    return (threadData.state == State.TIMEOUT_WAITING);
  }

  public void setTimedOut() {
    setState(State.TIMEDOUT);
  }

  public void setTerminated() {
    setState(State.TERMINATED);
  }

  public void resetTimedOut() {

    setState(State.TIMEOUT_WAITING);
  }

  public void setSleeping() {
    setState(State.SLEEPING);
  }

  public boolean isSleeping(){
    return (threadData.state == State.SLEEPING);
  }

  public void setRunning() {
    setState(State.RUNNING);
  }

  public void setStopped(int throwableRef){
    if (isTerminated()){

      return;
    }

    attributes |= ATTR_SET_STOPPED;

    if (!hasBeenStarted()){


      return;
    }



    if (throwableRef == MJIEnv.NULL){

      ClassInfo cix = ClassInfo.getInitializedSystemClassInfo("java.lang.ThreadDeath", this);
      throwableRef = createException(cix, null, MJIEnv.NULL);
    }








    if (isCurrentThread()){ 
      if (isInNativeMethod()){

        env.throwException(throwableRef);
      } else {
        Instruction nextPc = throwException(throwableRef);
        setNextPC(nextPc);
      }

    } else { 




      ElementInfo eit = getModifiableElementInfo(objRef);
      eit.setReferenceField("stopException", throwableRef);
    }
  }

  public boolean isCurrentThread(){
    return this == currentThread;
  }

  public boolean isInCurrentThreadList(){
    return vm.getThreadList().contains(this);
  }
  
  
  public boolean isAlive () {
    State state = threadData.state;
    return (state != State.TERMINATED && state != State.NEW);
  }

  public boolean isWaiting () {
    State state = threadData.state;
    return (state == State.WAITING) || (state == State.TIMEOUT_WAITING);
  }

  public boolean isWaitingOrTimedOut (){
    State state = threadData.state;
    return (state == State.WAITING) || (state == State.TIMEOUT_WAITING) || (state == State.TIMEDOUT);
  }

  public boolean isNotified () {
    return (threadData.state == State.NOTIFIED);
  }

  public boolean isUnblocked () {
    State state = threadData.state;
    return (state == State.UNBLOCKED) || (state == State.TIMEDOUT);
  }

  public boolean isBlocked () {
    return (threadData.state == State.BLOCKED);
  }

  public boolean isTerminated () {
    return (threadData.state == State.TERMINATED);
  }

  public boolean isAtomic (){
    return vm.getSystemState().isAtomic();
  }
  
  public void setBlockedInAtomicSection (){
    vm.getSystemState().setBlockedInAtomicSection();
  }
  
  MethodInfo getExitMethod() {
    MethodInfo mi = getClassInfo().getMethod("exit()V", true);
    return mi;
  }

  public boolean isBlockedOrNotified() {
    State state = threadData.state;
    return (state == State.BLOCKED) || (state == State.NOTIFIED);
  }


  public boolean isStopped() {
    return (attributes & ATTR_STOPPED) != 0;
  }

  public boolean isInNativeMethod(){
    return top != null && top.isNative();
  }

  public boolean hasBeenStarted(){
    return (threadData.state != State.NEW);
  }

  public String getStateName () {
    return threadData.getState().name();
  }

  @Override
  public Iterator<StackFrame> iterator () {
    return new StackIterator();
  }

  public Iterable<StackFrame> invokedStackFrames () {
    return new Iterable<StackFrame>() {
      @Override
	public Iterator<StackFrame> iterator() {
        return new InvokedStackIterator();
      }
    };
  }

  
  @Deprecated
  public List<StackFrame> getStack() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>(stackDepth);

    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      list.add(frame);
    }

    Collections.reverse(list);

    return list;
  }

  
  public List<StackFrame> getInvokedStackFrames() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>(stackDepth);

    int i = stackDepth-1;
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isDirectCallFrame()){
        list.add( frame);
      }
    }
    Collections.reverse(list);

    return list;
  }

  public Scheduler getScheduler(){
    return vm.getScheduler();
  }
  
  public int getStackDepth() {
    return stackDepth;
  }
  
  public MethodInfo getEntryMethod(){    
    return appCtx.getEntryMethod();
  }

  public StackFrame getCallerStackFrame (int offset){
    int n = offset;
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (n < 0){
        break;
      } else if (n == 0){
        return frame;
      }
      n--;
    }
    return null;
  }

  public StackFrame getLastInvokedStackFrame() {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isDirectCallFrame()){
        return frame;
      }
    }

    return null;
  }

  public StackFrame getLastNonSyntheticStackFrame (){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isSynthetic()){
        return frame;
      }
    }

    return null;
  }
  

  public StackFrame getModifiableLastNonSyntheticStackFrame (){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isSynthetic()){
        if (frame.isFrozen()) {
          StackFrame newFrame = frame.clone();
          
          if (frame == top) {
            frame = newFrame;
            top = newFrame;
            
          } else {

            StackFrame fLast = null;
            for (StackFrame f = getModifiableTopFrame(); f != frame; f = f
                .getPrevious()) {
              if (f.isFrozen()) {
                f = f.clone();
                if (fLast != null) {
                  fLast.setPrevious(f);
                }
              }
              fLast = f;
            }
            if (fLast != null) {
              fLast.setPrevious(newFrame);
            }

            frame = newFrame;
          }
        }
        
        return frame;
      }
    }

    return null;
  }
  

  
  public int getCalleeThis (MethodInfo mi) {
    return top.getCalleeThis(mi);
  }

  
  public int getCalleeThis (int size) {
    return top.getCalleeThis(size);
  }

  public ClassInfo getClassInfo (int objref) {
    return env.getClassInfo(objref);
  }

  public boolean isCalleeThis (ElementInfo r) {
    if (top == null || r == null) {
      return false;
    }

    Instruction pc = getPC();

    if (pc == null ||
        !(pc instanceof JVMInvokeInstruction) ||
        pc instanceof INVOKESTATIC) {
      return false;
    }

    JVMInvokeInstruction call = (JVMInvokeInstruction) pc;

    return getCalleeThis(Types.getArgumentsSize(call.getInvokedMethodSignature()) + 1) == r.getObjectRef();
  }

  public ApplicationContext getApplicationContext(){
    return appCtx;
  }
  
  public SystemClassLoaderInfo getSystemClassLoaderInfo(){
    return appCtx.sysCl;
  }
  
  
  public ClassInfo getClassInfo () {
    return ci;
  }

  public MJIEnv getEnv() {
    return env;
  }

  public boolean isInterrupted (boolean resetStatus) {
    ElementInfo ei = getElementInfo(getThreadObjectRef());
    boolean status =  ei.getBooleanField("interrupted");

    if (resetStatus && status) {
      ei = ei.getModifiableInstance();
      ei.setBooleanField("interrupted", false);
    }

    return status;
  }

  
  public int getId () {
    return id;
  }

  
  public int getGlobalId(){
    return id;
  }
  
  
  
  void setLockRef (int objref) {

    lockRef = objref;
  }

  
  public void resetLockRef () {
    lockRef = MJIEnv.NULL;
  }

  public int getLockRef() {
    return lockRef;
  }

  public ElementInfo getLockObject () {
    if (lockRef == MJIEnv.NULL) {
      return null;
    } else {
      return vm.getElementInfo(lockRef);
    }
  }

  
  public int getLine () {
    if (top == null) {
      return -1;
    } else {
      return top.getLine();
    }
  }
  






  
  
  public boolean suspend() {
    return threadDataClone().suspendCount++ == 0;
  }

  
  public boolean resume() {
    return (threadData.suspendCount > 0) && (--threadDataClone().suspendCount == 0);
  }
  
  public boolean isSuspended() {
    return threadData.suspendCount > 0;
  }



  
  
  public void setLockCount (int l) {
    if (threadData.lockCount != l) {
      threadDataClone().lockCount = l;
    }
  }

  
  public int getLockCount () {
    return threadData.lockCount;
  }


  public List<ElementInfo> getLockedObjects () {
    List<ElementInfo> lockedObjects = new LinkedList<ElementInfo>();
    Heap heap = vm.getHeap();
    
    for (int i=0; i<lockedObjectReferences.length; i++) {
      ElementInfo ei = heap.get(lockedObjectReferences[i]);
      lockedObjects.add(ei);
    }
    
    return lockedObjects;
  }

  public boolean hasLockedObjects() {
    return lockedObjectReferences.length > 0;
  }
  
  public int[] getLockedObjectReferences () {
    return lockedObjectReferences;
  }

  public boolean isLockOwner (ElementInfo ei){
    return ei.getLockingThread() == this;
  }
  
  
  public MethodInfo getTopFrameMethodInfo () {
    if (top != null) {
      return top.getMethodInfo();
    } else {
      return null;
    }
  }

  
  public ClassInfo getExecutingClassInfo(){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      MethodInfo miExecuting = frame.getMethodInfo();
      ClassInfo ciExecuting = miExecuting.getClassInfo();
      if (ciExecuting != null){
        return ciExecuting;
      }
    }
    
    return null;
  }
  
  
  
  public ClassInfo resolveReferencedClass (String clsName){
    ClassInfo ciTop = top.getClassInfo();
    return ciTop.resolveReferencedClass(clsName);
            

  }
  
  public boolean isInCtor () {


    MethodInfo mi = getTopFrameMethodInfo();
    if (mi != null) {
      return mi.isCtor();
    } else {
      return false;
    }
  }

  public boolean isCtorOnStack (int objRef){
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      if (f.getThis() == objRef && f.getMethodInfo().isCtor()){
        return true;
      }
    }

    return false;
  }

  public boolean isClinitOnStack (ClassInfo ci){
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      MethodInfo mi = f.getMethodInfo();
      if (mi.isClinit(ci)){
        return true;
      }
    }

    return false;
  }


  public String getName () {
    return threadData.name;
  }



  
  public int getThreadObjectRef () {
    return objRef;
  }

  public ElementInfo getThreadObject(){
    return getElementInfo(objRef);
  }

  public ElementInfo getModifiableThreadObject() {
    return getModifiableElementInfo(objRef);
  }
  

  
  public void setPC (Instruction pc) {
    getModifiableTopFrame().setPC(pc);
  }

  public void advancePC () {
    getModifiableTopFrame().advancePC();
  }

  
  public Instruction getPC () {
    if (top != null) {
      return top.getPC();
    } else {
      return null;
    }
  }

  public Instruction getNextPC () {
    return nextPc;
  }


  
  public String getStackTrace () {
    StringBuilder sb = new StringBuilder(256);

    for (StackFrame sf = top; sf != null; sf = sf.getPrevious()){
      MethodInfo mi = sf.getMethodInfo();

      if (mi.isCtor()){
        ClassInfo ci = mi.getClassInfo();
        if (ci.isInstanceOf("java.lang.Throwable")) {
          continue;
        }
      }

      sb.append("\tat ");
      sb.append(sf.getStackTraceInfo());
      sb.append('\n');
    }

    return sb.toString();
  }


  
  public void dumpStoringData (IntVector v) {
    v = null;  
  }

  
  public int getRunnableRef () {
    return targetRef;
  }

  
  public int getThis () {
    return top.getThis();
  }

  public ElementInfo getThisElementInfo(){
    return getElementInfo(getThis());
  }

  public boolean isThis (ElementInfo ei) {
    if (ei == null) {
      return false;
    }

    if (top == null) {
      return false;
    }

    if (getTopFrameMethodInfo().isStatic()) {
      return false;
    } else {
      int thisRef = top.getThis();
      return ei.getObjectRef() == thisRef;
    }
  }

  public boolean atMethod (String mname) {
    return top != null && getTopFrameMethodInfo().getFullName().equals(mname);
  }

  public boolean atPosition (int position) {
    if (top == null) {
      return false;
    } else {
      Instruction pc = getPC();
      return pc != null && pc.getPosition() == position;
    }
  }

  public boolean atReturn () {
    if (top == null) {
      return false;
    } else {
      Instruction pc = getPC();
      return pc instanceof ReturnInstruction;
    }
  }


  
  void resetVolatiles () {

    lockedObjectReferences = emptyLockRefs;


    lockRef = MJIEnv.NULL;

    pendingException = null;
  }
  
  
  void updateLockedObject (ElementInfo ei) {
    int n = lockedObjectReferences.length;    
    int[] a = new int[n+1];
    System.arraycopy(lockedObjectReferences, 0, a, 0, n);
    a[n] = ei.getObjectRef();
    lockedObjectReferences = a;
    

  }

  void addLockedObject (ElementInfo ei) {
    int n = lockedObjectReferences.length;    
    int[] a = new int[n+1];
    System.arraycopy(lockedObjectReferences, 0, a, 0, n);
    a[n] = ei.getObjectRef();
    lockedObjectReferences = a;
    
    vm.notifyObjectLocked(this, ei);
  }

  void removeLockedObject (ElementInfo ei) {
    int objRef = ei.getObjectRef();
    int n = lockedObjectReferences.length;
    
    if (n == 1) {
      assert lockedObjectReferences[0] == objRef;
      lockedObjectReferences = emptyLockRefs;
      
    } else {
      int[] a = new int[n - 1];

      for (int i = 0, j = 0; i < n; i++) {
        if (lockedObjectReferences[i] != objRef) {
          a[j++] = lockedObjectReferences[i];
        }
      }
      lockedObjectReferences = a;
    }
    
    vm.notifyObjectUnlocked(this, ei);
  }


  @Override
  public Object clone() {
    try {


      return super.clone();

    } catch (CloneNotSupportedException cnsx) {
      return null;
    }
  }

  
  public int countStackFrames () {
    return stackDepth;
  }

  
  public int[] getSnapshot (int xObjRef) {
    StackFrame frame = top;
    int n = stackDepth;
    
    if (xObjRef != MJIEnv.NULL){ 
      for (;frame != null; frame = frame.getPrevious()){
        if (frame.getThis() != xObjRef){
          break;
        }
        n--;
      }
    }

    int j=0;
    int[] snap = new int[n*2];

    for (; frame != null; frame = frame.getPrevious()){
      snap[j++] = frame.getMethodInfo().getGlobalId();
      snap[j++] = frame.getPC().getInstructionIndex();
    }

    return snap;
  }

  
  public int createStackTraceElements (int[] snapshot) {
    int n = snapshot.length/2;
    int nVisible=0;
    StackTraceElement[] list = new StackTraceElement[n];
    for (int i=0, j=0; i<n; i++){
      int methodId = snapshot[j++];
      int pcOffset = snapshot[j++];
      StackTraceElement ste = new StackTraceElement( methodId, pcOffset);
      if (!ste.ignore){
        list[nVisible++] = ste;
      }
    }

    Heap heap = vm.getHeap();
    ElementInfo eiArray = heap.newArray("Ljava/lang/StackTraceElement;", nVisible, this);
    for (int i=0; i<nVisible; i++){
      int eref = list[i].createJPFStackTraceElement();
      eiArray.setReferenceElement( i, eref);
    }

    return eiArray.getObjectRef();
  }

  void print (PrintWriter pw, String s) {
    if (pw != null){
      pw.print(s);
    } else {
      vm.print(s);
    }
  }

  public void printStackTrace (int objRef) {
    printStackTrace(null, objRef);
  }

  public void printPendingExceptionOn (PrintWriter pw) {
    if (pendingException != null) {
      printStackTrace( pw, pendingException.getExceptionReference());
    }
  }

  
  public void printStackTrace (PrintWriter pw, int objRef) {




    print(pw, env.getClassInfo(objRef).getName());
    int msgRef = env.getReferenceField(objRef,"detailMessage");
    if (msgRef != MJIEnv.NULL) {
      print(pw, ": ");
      print(pw, env.getStringObject(msgRef));
    }
    print(pw, "\n");


    int aRef = env.getReferenceField(objRef, "stackTrace"); 
    if (aRef != MJIEnv.NULL) {
      int len = env.getArrayLength(aRef);
      for (int i=0; i<len; i++) {
        int steRef = env.getReferenceArrayElement(aRef, i);
        if (steRef != MJIEnv.NULL){  
          StackTraceElement ste = new StackTraceElement(steRef);
          ste.printOn( pw);
        }
      }

    } else { 
      aRef = env.getReferenceField(objRef, "snapshot");
      int[] snapshot = env.getIntArrayObject(aRef);
      int len = snapshot.length/2;

      for (int i=0, j=0; i<len; i++){
        int methodId = snapshot[j++];
        int pcOffset = snapshot[j++];
        StackTraceElement ste = new StackTraceElement( methodId, pcOffset);
        ste.printOn( pw);
      }
    }

    int causeRef = env.getReferenceField(objRef, "cause");
    if ((causeRef != objRef) && (causeRef != MJIEnv.NULL)){
      print(pw, "Caused by: ");
      printStackTrace(pw, causeRef);
    }
  }

  class StackTraceElement {
    String clsName, mthName, fileName;
    int line;
    boolean ignore;


    StackTraceElement (int methodId, int pcOffset) {
      if (methodId == MethodInfo.DIRECT_CALL) {
        ignore = true;

      } else {
        MethodInfo mi = MethodInfo.getMethodInfo(methodId);
        if (mi != null) {
          clsName = mi.getClassName();
          mthName = mi.getName();

          fileName = mi.getStackTraceSource();          
          if (pcOffset < 0){


            pcOffset = 0;
          }
          line = mi.getLineNumber(mi.getInstruction(pcOffset));

        } else { 
          clsName = "?";
          mthName = "?";
          fileName = "?";
          line = -1;
        }
      }
    }

    StackTraceElement (int sRef){
      clsName = env.getStringObject(env.getReferenceField(sRef, "clsName"));
      mthName = env.getStringObject(env.getReferenceField(sRef, "mthName"));
      fileName = env.getStringObject(env.getReferenceField(sRef, "fileName"));
      line = env.getIntField(sRef, "line");
    }

    int createJPFStackTraceElement() {
      if (ignore) {
        return MJIEnv.NULL;
        
      } else {
        Heap heap = vm.getHeap();
        ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo("java.lang.StackTraceElement");
        ElementInfo ei = heap.newObject(ci, ThreadInfo.this);

        ei.setReferenceField("clsName", heap.newString(clsName, ThreadInfo.this).getObjectRef());
        ei.setReferenceField("mthName", heap.newString(mthName, ThreadInfo.this).getObjectRef());

        String fname = fileName != null ? fileName : "Unknown Source";
        ei.setReferenceField("fileName", heap.newString(fname, ThreadInfo.this).getObjectRef());
                
        ei.setIntField("line", line);

        return ei.getObjectRef();
      }
    }

    void printOn (PrintWriter pw){
      if (!ignore){

        if (fileName != null){
          int idx = fileName.lastIndexOf(File.separatorChar);
          if (idx >=0) {
            fileName = fileName.substring(idx+1);
          }
        }

        print(pw, "\tat ");
        if (clsName != null){
          print(pw, clsName);
          print(pw, ".");
        } else { 
          print(pw, "[no class] ");
        }
        print(pw, mthName);

        if (fileName != null){
          print(pw, "(");
          print(pw, fileName);
          if (line >= 0){
            print(pw, ":");
            print(pw, Integer.toString(line));
          }
          print(pw, ")");
        } else {

        }

        print(pw, "\n");
      }
    }
  }

  
  static class SUTExceptionRequest {
    String xClsName;
    String details;
    
    SUTExceptionRequest (String xClsName, String details){
      this.xClsName = xClsName;
      this.details = details;
    }
    
    public String getExceptionClassName(){
      return xClsName;
    }
    
    public String getDetails(){
      return details;
    }
  }
  
  public void requestSUTException (String exceptionClsName, String details){
    pendingSUTExceptionRequest = new SUTExceptionRequest( exceptionClsName, details);
    if (nextPc == null){ 
      attributes |= ATTR_SKIP_INSN_EXEC;
    }
  }
  
  protected void processPendingSUTExceptionRequest (){
    if (pendingSUTExceptionRequest != null){

      nextPc = createAndThrowException( pendingSUTExceptionRequest.getExceptionClassName(), pendingSUTExceptionRequest.getDetails());
      pendingSUTExceptionRequest = null;
    }
  }
  
  
  
  int createException (ClassInfo ci, String details, int causeRef){
    int[] snap = getSnapshot(MJIEnv.NULL);
    return vm.getHeap().newSystemThrowable(ci, details, snap, causeRef, this, 0).getObjectRef();
  }

  
  public Instruction createAndThrowException (ClassInfo ci, String details) {
    return createAndThrowException(ci, details, null);
  }
  
  public Instruction createAndThrowException (
  		ClassInfo ci, String details, DynamicDependency dep) {



    ci.initializeClassAtomic(this);

    int objref = createException(ci,details, MJIEnv.NULL);
    if (dep != null) {
    	attachDependencyToSystemThrowable(getElementInfo(objref), dep);
    }
    return throwException(objref, dep);
  }
  
  public void attachDependencyToSystemThrowable(
  		ElementInfo eiException, DynamicDependency dep) {
  	FieldInfo msgField = eiException.getFieldInfo("detailMessage");
  	eiException.setFieldAttr(msgField, dep);
  	FieldInfo causeField = eiException.getFieldInfo("cause");
  	eiException.setFieldAttr(causeField, dep);
  	int refMsg = eiException.getReferenceField(msgField);
  	if (refMsg != MJIEnv.NULL) {
  		ElementInfo eiMsg = getElementInfo(refMsg);
  		DynDepUtils.setDependencyOnString(this, eiMsg, dep);
  	}
  }

  
  public Instruction createAndThrowException (String cname) {
    return createAndThrowException(cname, null,  null);
  }
  
  public Instruction createAndThrowException (String cname, DynamicDependency dep) {
  	return createAndThrowException(cname, null, dep);
  }

  public Instruction createAndThrowException (String cname, String details) {
    return createAndThrowException(cname, details, null);
  }
  
  public Instruction createAndThrowException (
  		String cname, String details, DynamicDependency dep) {
  	try {
      ClassInfo ci = null;
      try {
        ci = ClassLoaderInfo.getCurrentResolvedClassInfo(cname);
      } catch(ClassInfoException cie) {

        if(cie.getExceptionClass().equals("java.lang.ClassNotFoundException") &&
                        !ClassLoaderInfo.getCurrentClassLoader().isSystemClassLoader()) {
          ci = ClassLoaderInfo.getSystemResolvedClassInfo(cname);
        } else {
          throw cie;
        }
      }
      return createAndThrowException(ci, details, dep);
      
    } catch (ClassInfoException cie){
      if(!cname.equals(cie.getExceptionClass())) {
        ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(cie.getExceptionClass());
        return createAndThrowException(ci, cie.getMessage(), dep);
      } else {
        throw cie;
      }
    }
  }

  
  public boolean maxTransitionLengthExceeded(){
    return executedInstructions >= maxTransitionLength;
  }
  
  
  protected void executeTransition (SystemState ss) throws JPFException {
    Instruction pc;
    outer:
    while ((pc = getPC()) != null){
      Instruction nextPc = null;

      currentThread = this;
      executedInstructions = 0;
      pendingException = null;

      if (isStopped()){
        pc = throwStopException();
        setPC(pc);
      }





      while (pc != null) {
        nextPc = executeInstruction();

        if (ss.breakTransition()) {
          if (ss.extendTransition()){
            continue outer;
            
          } else {
            if (executedInstructions == 0){ 
              if (isEmptyTransitionEnabled()){ 
                ss.setForced(true);
              }
            }
            return;
          }

        } else {        
          pc = nextPc;
        }
      }
    }
  }


  protected void resetTransientAttributes(){
    attributes &= ~(ATTR_SKIP_INSN_EXEC | ATTR_SKIP_INSN_LOG | ATTR_ENABLE_EMPTY_TRANSITION);
  }
  
  
  public Instruction executeInstruction () {
    Instruction pc = getPC();
    SystemState ss = vm.getSystemState();

    resetTransientAttributes();
    nextPc = null;
    

    
    if (log.isLoggable(Level.FINER)) {
      log.fine( pc.getMethodInfo().getFullName() + " " + pc.getPosition() + " : " + pc);
    }



    vm.notifyExecuteInstruction(this, pc);

    if ((pendingSUTExceptionRequest == null) && ((attributes & ATTR_SKIP_INSN_EXEC) == 0)){
        try {
          nextPc = pc.execute(this);
        } catch (ClassInfoException cie) {
          nextPc = this.createAndThrowException(cie.getExceptionClass(), cie.getMessage());
        }
      }


    executedInstructions++;
    
    if ((attributes & ATTR_SKIP_INSN_LOG) == 0) {
      ss.recordExecutionStep(pc);
    }


    vm.notifyInstructionExecuted(this, pc, nextPc);
    

    vm.getSearch().checkAndResetProbeRequest();
    

    pc.cleanupTransients();

    if (pendingSUTExceptionRequest != null){
      processPendingSUTExceptionRequest();
    }
    




    if (top != null) {

      setPC(nextPc);
      return nextPc;
    } else {
      return null;
    }
  }

  
  public Instruction executeInstructionHidden () {
    Instruction pc = getPC();
    SystemState ss = vm.getSystemState();
    KernelState ks = vm.getKernelState();

    nextPc = null; 

    if (log.isLoggable(Level.FINE)) {
      log.fine( pc.getMethodInfo().getFullName() + " " + pc.getPosition() + " : " + pc);
    }

    try {
        nextPc = pc.execute(this);
      } catch (ClassInfoException cie) {
        nextPc = this.createAndThrowException(cie.getExceptionClass(), cie.getMessage());
      }


    executedInstructions++;
    

    vm.getSearch().checkAndResetProbeRequest();
    

    if (top != null) { 
      setPC(nextPc);
    }

    return nextPc;
  }

  
  public boolean isPostExec() {
    return (nextPc != null);
  }

  public void reExecuteInstruction() {
    nextPc = getPC();
  }

  public boolean willReExecuteInstruction() {
    return (getPC() == nextPc);
  }
  
  
  public void skipInstruction (Instruction nextInsn) {
    attributes |= ATTR_SKIP_INSN_EXEC;
    

    nextPc = nextInsn;
  }

  
  @Deprecated
  public void skipInstruction(){
    skipInstruction(getPC().getNext());
  }

  public boolean isInstructionSkipped() {
    return (attributes & ATTR_SKIP_INSN_EXEC) != 0;
  }

  public void skipInstructionLogging () {
    attributes |= ATTR_SKIP_INSN_LOG;
  }

  
  public boolean setNextPC (Instruction insn) {
    if (nextPc == null){


      attributes |= ATTR_SKIP_INSN_EXEC;
      nextPc = insn;
      return true;
      
    } else {
      if (top != null && nextPc != top.getPC()){ 
        nextPc = insn;   
        return true;
      }
    }
    
    return false;
  }

  
  public void executeMethodAtomic (StackFrame frame) {

    pushFrame(frame);
    int    depth = countStackFrames();
    Instruction pc = frame.getPC();
    SystemState ss = vm.getSystemState();

    ss.incAtomic(); 

    while (depth <= countStackFrames()) {
      Instruction nextPC = executeInstruction();

      if (ss.getNextChoiceGenerator() != null) {



        throw new JPFException("choice point in atomic method execution: " + frame);
      } else {
        pc = nextPC;
      }
    }

    vm.getSystemState().decAtomic();

    nextPc = null;


  }

  
  public void executeMethodHidden (StackFrame frame) {

    pushFrame(frame);
    
    int depth = countStackFrames(); 
    Instruction pc = frame.getPC();

    vm.getSystemState().incAtomic(); 

    while (depth <= countStackFrames()) {
      Instruction nextPC = executeInstructionHidden();

      if (pendingException != null) {

      } else {
        if (nextPC == pc) {



          throw new JPFException("choice point in hidden method execution: " + frame);
        } else {
          pc = nextPC;
        }
      }
    }

    vm.getSystemState().decAtomic();

    nextPc = null;


  }

  public Heap getHeap () {
    return vm.getHeap();
  }

  
  public ElementInfo getElementInfo (int objRef) {
    Heap heap = vm.getHeap();
    return heap.get(objRef);
  }
  
  public ElementInfo getModifiableElementInfo (int ref) {
    Heap heap = vm.getHeap();
    return heap.getModifiable(ref);
  }
  
  public ElementInfo getBlockedObject (MethodInfo mi, boolean isBeforeCall, boolean isModifiable) {
    int         objref;
    ElementInfo ei = null;

    if (mi.isSynchronized()) {
      if (mi.isStatic()) {
        objref = mi.getClassInfo().getClassObjectRef();
      } else {


        objref = isBeforeCall ? getCalleeThis(mi) : getThis();
      }

      ei = (isModifiable) ? getModifiableElementInfo(objref) : getElementInfo(objref);

      assert (ei != null) : ("inconsistent stack, no object or class ref: " +
                               mi.getFullName() + " (" + objref +")");
    }

    return ei;
  }


  
  public boolean setNextChoiceGenerator (ChoiceGenerator<?> cg){
    return vm.setNextChoiceGenerator(cg);
  }
  
  public boolean hasNextChoiceGenerator(){
    return vm.hasNextChoiceGenerator();
  }

  public void checkNextChoiceGeneratorSet (String msg){
    if (!vm.hasNextChoiceGenerator()){
      throw new JPFException(msg);
    }
  }
  

  
  
  public void enter (){
    MethodInfo mi = top.getMethodInfo();

    if (!mi.isJPFExecutable()){

      throw new JPFException("method is not JPF executable: " + mi);
    }

    if (mi.isSynchronized()){
      int oref = mi.isStatic() ?  mi.getClassInfo().getClassObjectRef() : top.getThis();
      ElementInfo ei = getModifiableElementInfo( oref);
      
      ei.lock(this);
    }

    vm.notifyMethodEntered(this, mi);
  }

  
  public boolean leave(){
    boolean didUnblock = false;
    MethodInfo mi = top.getMethodInfo();
    






    
    if (mi.isSynchronized()) {
      int oref = mi.isStatic() ?  mi.getClassInfo().getClassObjectRef() : top.getThis();
      ElementInfo ei = getElementInfo( oref);
      if (ei.isLocked()){
        ei = ei.getModifiableInstance();
        didUnblock = ei.unlock(this);
      }
    }

    vm.notifyMethodExited(this, mi);
    return didUnblock;
  }

  
  
  public boolean exit(){
    int objref = getThreadObjectRef();
    ElementInfo ei = getModifiableElementInfo(objref); 
    SystemState ss = vm.getSystemState();
    Scheduler scheduler = getScheduler();

    enableEmptyTransition();
    








    if (vm.getThreadList().hasOnlyMatchingOtherThan(this, vm.getDaemonRunnablePredicate())) {
      if (scheduler.setsRescheduleCG(this, "daemonTermination")) {
        return false;
      }
    }
    


    if (!ei.canLock(this)) {


      

      ei.block(this);
      if (!scheduler.setsBlockedThreadCG(this, ei)){
        throw new JPFException("blocking thread termination without transition break");            
      }    
      return false; 
    }
      


    int grpRef = getThreadGroupRef();
    ElementInfo eiGrp = getModifiableElementInfo(grpRef);
    if (eiGrp != null){
      if (!eiGrp.canLock(this)){
        eiGrp.block(this);
        if (!scheduler.setsBlockedThreadCG(this, eiGrp)){
          throw new JPFException("blocking thread termination on group without transition break");            
        }    
        return false; 
      }
    }


    





    eiGrp.lock(this);
    cleanupThreadGroup(grpRef, objref);
    eiGrp.unlock(this);
    
    

    if (!holdsLock(ei)) {



      ei.lock(this);
    }

    ei.notifiesAll(); 
    ei.unlock(this);

    setState(State.TERMINATED);



    ss.clearAtomic();
    cleanupThreadObject(ei);
    vm.activateGC();  



    scheduler.cleanupThreadTermination(this);

    if (vm.getThreadList().hasAnyMatchingOtherThan(this, getRunnableNonDaemonPredicate())) {
      if (!scheduler.setsTerminationCG(this)){
        throw new JPFException("thread termination without transition break");
      }
    }

    popFrame(); 

    return true;
  }

  Predicate<ThreadInfo> getRunnableNonDaemonPredicate() {
    return new Predicate<ThreadInfo>() {
      @Override
	public boolean isTrue (ThreadInfo ti) {
        return (ti.isRunnable() && !ti.isDaemon());
      }
    };
  }
  
  
  void cleanupThreadObject (ElementInfo ei) {




    int grpRef = ei.getReferenceField("group");
    cleanupThreadGroup(grpRef, ei.getObjectRef());

    ei.setReferenceField("group", MJIEnv.NULL);    
    ei.setReferenceField("threadLocals", MJIEnv.NULL);
    
    ei.setReferenceField("uncaughtExceptionHandler", MJIEnv.NULL);
  }

  
  void cleanupThreadGroup (int grpRef, int threadRef) {
    if (grpRef != MJIEnv.NULL) {
      ElementInfo eiGrp = getModifiableElementInfo(grpRef);
      int threadsRef = eiGrp.getReferenceField("threads");
      if (threadsRef != MJIEnv.NULL) {
        ElementInfo eiThreads = getModifiableElementInfo(threadsRef);
        if (eiThreads.isArray()) {
          int nthreads = eiGrp.getIntField("nthreads");

          for (int i=0; i<nthreads; i++) {
            int tref = eiThreads.getReferenceElement(i);

            if (tref == threadRef) { 
              int n1 = nthreads-1;
              for (int j=i; j<n1; j++) {
                eiThreads.setReferenceElement(j, eiThreads.getReferenceElement(j+1));
              }
              eiThreads.setReferenceElement(n1, MJIEnv.NULL);

              eiGrp.setIntField("nthreads", n1);
              if (n1 == 0) {
                eiGrp.lock(this);
                eiGrp.notifiesAll();
                eiGrp.unlock(this);
              }


              return;
            }
          }
        }
      }
    }
  }

  
  protected void createMainThreadObject (SystemClassLoaderInfo sysCl){

    Heap heap = getHeap();

    ClassInfo ciThread = sysCl.threadClassInfo;
    ElementInfo eiThread = heap.newObject( ciThread, this);
    objRef = eiThread.getObjectRef();
     
    ElementInfo eiName = heap.newString(MAIN_NAME, this);
    int nameRef = eiName.getObjectRef();
    eiThread.setReferenceField("name", nameRef);
    
    ElementInfo eiGroup = createMainThreadGroup(sysCl);
    eiThread.setReferenceField("group", eiGroup.getObjectRef());
    
    eiThread.setIntField("priority", Thread.NORM_PRIORITY);

    ClassInfo ciPermit = sysCl.getResolvedClassInfo("java.lang.Thread$Permit");
    ElementInfo eiPermit = heap.newObject( ciPermit, this);
    eiPermit.setBooleanField("blockPark", true);
    eiThread.setReferenceField("permit", eiPermit.getObjectRef());

    addToThreadGroup(eiGroup);
    
    addId( objRef, id);


    setState(ThreadInfo.State.RUNNING);
  }
  

  
  protected ElementInfo createMainThreadGroup (SystemClassLoaderInfo sysCl) {
    Heap heap = getHeap();
    
    ClassInfo ciGroup = sysCl.getResolvedClassInfo("java.lang.ThreadGroup");
    ElementInfo eiThreadGrp = heap.newObject( ciGroup, this);

    ElementInfo eiGrpName = heap.newString("main", this);
    eiThreadGrp.setReferenceField("name", eiGrpName.getObjectRef());

    eiThreadGrp.setIntField("maxPriority", java.lang.Thread.MAX_PRIORITY);



    return eiThreadGrp;
  }

  
  protected void addToThreadGroup (ElementInfo eiGroup){
    FieldInfo finThreads = eiGroup.getFieldInfo("nthreads");
    int nThreads = eiGroup.getIntField(finThreads);
    
    if (eiGroup.getBooleanField("destroyed")){
      env.throwException("java.lang.IllegalThreadStateException");
      
    } else {
      FieldInfo fiThreads = eiGroup.getFieldInfo("threads");
      int threadsRef = eiGroup.getReferenceField(fiThreads);
      
      if (threadsRef == MJIEnv.NULL){
        threadsRef = env.newObjectArray("Ljava/lang/Thread;", 1);
        env.setReferenceArrayElement(threadsRef, 0, objRef);
        eiGroup.setReferenceField(fiThreads, threadsRef);
        
      } else {
        int newThreadsRef = env.newObjectArray("Ljava/lang/Thread;", nThreads+1);
        ElementInfo eiNewThreads = env.getElementInfo(newThreadsRef);        
        ElementInfo eiThreads = env.getElementInfo(threadsRef);
        
        for (int i=0; i<nThreads; i++){
          int tr = eiThreads.getReferenceElement(i);
          eiNewThreads.setReferenceElement(i, tr);
        }
        
        eiNewThreads.setReferenceElement(nThreads, objRef);
        eiGroup.setReferenceField(fiThreads, newThreadsRef);
      }
      
      eiGroup.setIntField(finThreads, nThreads+1);
      
      
    }    
  }
  
  
  public void hash (HashData hd) {
    threadData.hash(hd);

    for (StackFrame f = top; f != null; f = f.getPrevious()){
      f.hash(hd);
    }
  }

  public void interrupt () {
    ElementInfo eiThread = getModifiableElementInfo(getThreadObjectRef());

    State status = getState();

    switch (status) {
    case RUNNING:
    case BLOCKED:
    case UNBLOCKED:
    case NOTIFIED:
    case TIMEDOUT:

      eiThread.setBooleanField("interrupted", true);
      break;

    case WAITING:
    case TIMEOUT_WAITING:
      eiThread.setBooleanField("interrupted", true);
      setState(State.INTERRUPTED);



      ElementInfo eiLock = getElementInfo(lockRef);
      if (eiLock.canLock(this)) {
        resetLockRef();
        setState(State.UNBLOCKED);
        


        




      }
      
      break;

    case NEW:
    case TERMINATED:

      break;

    default:
    }
  }

  
  void markRoots (Heap heap) {
    

    heap.markThreadRoot(objRef, id);


    if (targetRef != MJIEnv.NULL) {
      heap.markThreadRoot(targetRef,id);
    }


    if (pendingException != null){
      heap.markThreadRoot(pendingException.getExceptionReference(), id);
    }
    

    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.markThreadRoots(heap, id);
    }
  }


  
  public void setTopFrame (StackFrame frame) {
    top = frame;


    int n = 0;
    for (StackFrame f = frame; f != null; f = f.getPrevious()){
      n++;
    }
    stackDepth = n;
  }

  
  public void pushFrame (StackFrame frame) {

    frame.setPrevious(top);

    top = frame;
    stackDepth++;



    markTfChanged(top);

    returnedDirectCall = null;
  }

  
  public void popFrame() {
    StackFrame frame = top;


    if (frame.hasAnyRef()) {
      vm.getSystemState().activateGC();
    }


    top = frame.getPrevious();
    stackDepth--;
  }

  public StackFrame popAndGetModifiableTopFrame() {
    popFrame();

    if (top.isFrozen()) {
      top = top.clone();
    }
    
    return top;
  }
  
  public StackFrame popAndGetTopFrame(){
    popFrame();
    return top;
  }
  
  
  public StackFrame popDirectCallFrame() {


    returnedDirectCall = (DirectCallStackFrame) top;

    if (top.hasFrameAttr( UncaughtHandlerAttr.class)){
      return popUncaughtHandlerFrame();
    }
    
    top = top.getPrevious();
    stackDepth--;
    
    return top;
  }

  
  public boolean hasReturnedFromDirectCall () {

    return (returnedDirectCall != null);
  }

  public boolean hasReturnedFromDirectCall(String directCallId){
    return (returnedDirectCall != null &&
            returnedDirectCall.getMethodName().equals(directCallId));
  }

  public DirectCallStackFrame getReturnedDirectCall () {
    return returnedDirectCall;
  }


  public String getStateDescription () {
    StringBuilder sb = new StringBuilder("thread ");
    sb.append(getThreadObjectClassInfo().getName());
    sb.append(":{id:");
    sb.append(id);
    sb.append(',');
    sb.append(threadData.getFieldValues());
    sb.append('}');
    
    return sb.toString();
  }

  public ClassInfo getThreadObjectClassInfo() {
    return getThreadObject().getClassInfo();
  }
  
  
  public void printStackContent () {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.printStackContent();
    }
  }

  
  public void printStackTrace () {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.printStackTrace();
    }
  }

  protected boolean haltOnThrow (String exceptionClassName){
    if ((haltOnThrow != null) && (haltOnThrow.matchesAny(exceptionClassName))){
      return true;
    }

    return false;
  }

  protected Instruction throwStopException (){

    ElementInfo ei = getModifiableThreadObject();

    int xRef = ei.getReferenceField("stopException");
    ei.setReferenceField("stopException", MJIEnv.NULL);
    attributes &= ~ATTR_SET_STOPPED;  

    Instruction insn = getPC();
    if (insn instanceof EXECUTENATIVE){








      env.throwException(xRef);
      return insn;
    }

    return throwException(xRef);
  }
  
  
  public HandlerContext getHandlerContextFor (ClassInfo ciException){
    ExceptionHandler matchingHandler = null; 
    
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()) {

      if (frame.isReflection()) {
        ciException = ClassInfo.getInitializedSystemClassInfo("java.lang.reflect.InvocationTargetException", this);
      }

      matchingHandler = frame.getHandlerFor( ciException);
      if (matchingHandler != null){
        return new HandlerContext( this, ciException, frame, matchingHandler);
      }
    }
    
    if (!ignoreUncaughtHandlers && !isUncaughtHandlerOnStack()) {
      int uchRef;
      if ((uchRef = getInstanceUncaughtHandler()) != MJIEnv.NULL) {
        return new HandlerContext( this, ciException, HandlerContext.UncaughtHandlerType.INSTANCE, uchRef);
      }

      int grpRef = getThreadGroupRef();
      if ((uchRef = getThreadGroupUncaughtHandler(grpRef)) != MJIEnv.NULL) {
        return new HandlerContext( this, ciException, HandlerContext.UncaughtHandlerType.GROUP, uchRef);
      }

      if ((uchRef = getGlobalUncaughtHandler()) != MJIEnv.NULL) {
        return new HandlerContext( this, ciException, HandlerContext.UncaughtHandlerType.GLOBAL, uchRef);
      }
    }
    
    return null;
  }
  
  public Instruction throwException (int exceptionObjRef) {
  	return throwException(exceptionObjRef, null);
  }
  
  
  public Instruction throwException (
  		int exceptionObjRef, DynamicDependency exceptionDependency) {
    Heap heap = vm.getHeap();
    ElementInfo eiException = heap.get(exceptionObjRef);
    ClassInfo ciException = eiException.getClassInfo();
    String exceptionName = ciException.getName();
    StackFrame handlerFrame = null; 
    ExceptionHandler matchingHandler = null; 


    Instruction insn = vm.handleException(this, exceptionObjRef);
    if (insn != null){
      return insn;
    }



    pendingException = new ExceptionInfo(this, eiException, exceptionDependency);

    vm.notifyExceptionThrown(this, eiException);

    if (haltOnThrow(exceptionName)) {


      throw new UncaughtException(this, exceptionObjRef);
    }



    for (StackFrame frame = top; (frame != null) && (handlerFrame == null); frame = frame.getPrevious()) {

      if (frame.isReflection()) {

        ciException = ClassInfo.getInitializedSystemClassInfo("java.lang.reflect.InvocationTargetException", this);
        exceptionObjRef  = createException(ciException, exceptionName, exceptionObjRef);
        exceptionName = ciException.getName();
        eiException = heap.get(exceptionObjRef);
        attachDependencyToSystemThrowable(eiException, exceptionDependency);
        pendingException = new ExceptionInfo(this, eiException, exceptionDependency);
      }

      matchingHandler = frame.getHandlerFor( ciException);
      if (matchingHandler != null){
        handlerFrame = frame;
        break;
      }

      if ((handlerFrame == null) && frame.isFirewall()) {




        unwindTo(frame);

        throw new UncaughtException(this, exceptionObjRef);
      }
    }

    if (handlerFrame == null) {


      if (!ignoreUncaughtHandlers && !isUncaughtHandlerOnStack()) {




        insn = callUncaughtHandler(pendingException);
        if (insn != null) {



          return insn;
        }
      }


      if ("java.lang.ThreadDeath".equals(exceptionName)) { 
        unwindToFirstFrame();
        pendingException = null;
        return top.getPC().getNext(); 

      } else { 

        throw new UncaughtException(this, exceptionObjRef);
      }

    } else { 
      
      unwindTo(handlerFrame);




      handlerFrame = getModifiableTopFrame();
      handlerFrame.setExceptionReference(exceptionObjRef);


      int handlerOffset = matchingHandler.getHandler();
      insn = handlerFrame.getMethodInfo().getInstructionAt(handlerOffset);
      handlerFrame.setPC(insn);
      
      if (exceptionDependency != null) {
      	((DependencyTrackingInstruction) insn).setActiveCondition(this, exceptionDependency);
      }

      vm.notifyExceptionHandled(this);

      pendingException = null; 

      return insn;
    }
  }

  
  public boolean hasUncaughtHandler (){
    if (getInstanceUncaughtHandler() != MJIEnv.NULL){
      return true;
    }
    
    int grpRef = getThreadGroupRef();
    if (getThreadGroupUncaughtHandler(grpRef) != MJIEnv.NULL){
      return true;
    }
    
    if (getGlobalUncaughtHandler() != MJIEnv.NULL){
      return true;
    }
    
    return false;
  }
  
  
  protected Instruction callUncaughtHandler (ExceptionInfo xi){
    Instruction insn = null;
    


    int  hRef = getInstanceUncaughtHandler();
    if (hRef != MJIEnv.NULL){
      insn = callUncaughtHandler(xi, hRef, "[threadUncaughtHandler]");
      
    } else {

      int grpRef = getThreadGroupRef();
      hRef = getThreadGroupUncaughtHandler(grpRef);
      
      if (hRef != MJIEnv.NULL){
        insn = callUncaughtHandler(xi, hRef, "[threadGroupUncaughtHandler]");
      
      } else {

        hRef = getGlobalUncaughtHandler();
        if (hRef != MJIEnv.NULL){
          insn = callUncaughtHandler(xi, hRef, "[globalUncaughtHandler]");
        }    
      }
    }
    
    return insn;
  }
  
  protected boolean isUncaughtHandlerOnStack(){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()) {
      if (frame.hasFrameAttr(UncaughtHandlerAttr.class)){
        return true;
      }
    }
    
    return false;
  }
  
  protected int getInstanceUncaughtHandler (){
    ElementInfo ei = getElementInfo(objRef);
    int handlerRef = ei.getReferenceField("uncaughtExceptionHandler");
    return handlerRef;
  }
  
  protected int getThreadGroupRef() {
    ElementInfo ei = getElementInfo(objRef);
    int groupRef = ei.getReferenceField("group");
    return groupRef;
  }
  
  protected int getThreadGroupUncaughtHandler (int grpRef){


    while (grpRef != MJIEnv.NULL){
      ElementInfo eiGrp = getElementInfo(grpRef);
      ClassInfo ciGrp = eiGrp.getClassInfo();
      MethodInfo miHandler = ciGrp.getMethod("uncaughtException(Ljava/lang/Thread;Ljava/lang/Throwable;)V", true);
      ClassInfo ciHandler = miHandler.getClassInfo();
      if (!ciHandler.getName().equals("java.lang.ThreadGroup")) {
        return eiGrp.getObjectRef();
      }

      grpRef = eiGrp.getReferenceField("parent");
    }
    

    return MJIEnv.NULL;
  }
  
  protected int getGlobalUncaughtHandler(){
    ElementInfo ei = getElementInfo(objRef);
    ClassInfo ci = ei.getClassInfo();
    FieldInfo fi = ci.getStaticField("defaultUncaughtExceptionHandler");
    

    ClassInfo fci = fi.getClassInfo();
    return fci.getStaticElementInfo().getReferenceField(fi);
  }
  
  
  class UncaughtHandlerAttr implements SystemAttribute {
    ExceptionInfo xInfo;
    
    UncaughtHandlerAttr (ExceptionInfo xInfo){
      this.xInfo = xInfo;
    }
    
    ExceptionInfo getExceptionInfo(){
      return xInfo;
    }
  }
  
  protected Instruction callUncaughtHandler (ExceptionInfo xi, int handlerRef, String id){
    ElementInfo eiHandler = getElementInfo(handlerRef);
    ClassInfo ciHandler = eiHandler.getClassInfo();
    MethodInfo miHandler = ciHandler.getMethod("uncaughtException(Ljava/lang/Thread;Ljava/lang/Throwable;)V", true);


    pendingException = null;
    
    DirectCallStackFrame frame = miHandler.createDirectCallStackFrame(this, 0);
    int argOffset = frame.setReferenceArgument( 0, handlerRef, null);
    argOffset = frame.setReferenceArgument( argOffset, objRef, null);
    frame.setReferenceArgument( argOffset, xi.getExceptionReference(), null);
    
    UncaughtHandlerAttr uchContext = new UncaughtHandlerAttr( xi);
    frame.setFrameAttr( uchContext);
    
    pushFrame(frame);
    return frame.getPC();
  }
  
  protected StackFrame popUncaughtHandlerFrame(){    






    
    if (passUncaughtHandler) {

      unwindToFirstFrame(); 
      
      getModifiableTopFrame().advancePC();
      assert top.getPC() instanceof ReturnInstruction : "topframe PC not a ReturnInstruction: " + top.getPC();
      return top;

    } else {

      UncaughtHandlerAttr ctx = returnedDirectCall.getFrameAttr(UncaughtHandlerAttr.class);
      pendingException = ctx.getExceptionInfo();

      throw new UncaughtException(this, pendingException.getExceptionReference());
    }
  }

  
  protected void unwindTo (StackFrame newTopFrame){
    for (StackFrame frame = top; (frame != null) && (frame != newTopFrame); frame = frame.getPrevious()) {
      leave(); 
      vm.notifyExceptionBailout(this); 
      popFrame();
    }
  }

  protected StackFrame unwindToFirstFrame(){
    StackFrame frame;

    for (frame = top; frame.getPrevious() != null; frame = frame.getPrevious()) {
      leave(); 
      vm.notifyExceptionBailout(this); 
      popFrame();
    }

    return frame;
  }

  public ExceptionInfo getPendingException () {
    return pendingException;
  }

  
  public void clearPendingException () {

    pendingException = null;
  }

  
  protected ThreadData threadDataClone () {
    if ((attributes & ATTR_DATA_CHANGED) != 0) {

    } else {

      markTdChanged();
      vm.kernelStateChanged();

      threadData = threadData.clone();
    }

    return threadData;
  }

  public void restoreThreadData(ThreadData td) {
    threadData = td;
  }


  
  public boolean reschedule (String reason){
    return getScheduler().setsRescheduleCG(this, reason);
  }
  
  
  public boolean breakTransition(String reason) {
    SystemState ss = vm.getSystemState();



    BreakGenerator cg = new BreakGenerator(reason, this, false);
    return ss.setNextChoiceGenerator(cg); 
  }
  
  
  public boolean breakTransition(boolean isTerminator) {
    SystemState ss = vm.getSystemState();

    if (!ss.isIgnored()){
      BreakGenerator cg = new BreakGenerator( "breakTransition", this, isTerminator);
      return ss.setNextChoiceGenerator(cg); 
    }
    
    return false;
  }

  public boolean hasOtherRunnables () {
    return vm.getThreadList().hasAnyMatchingOtherThan(this, vm.getRunnablePredicate());
  }
  
  protected void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;
  }

  protected void markTfChanged(StackFrame frame) {
    attributes |= ATTR_STACK_CHANGED;
    vm.kernelStateChanged();
  }

  protected void markTdChanged() {
    attributes |= ATTR_DATA_CHANGED;
    vm.kernelStateChanged();
  }

  public StackFrame getCallerStackFrame() {
    if (top != null){
      return top.getPrevious();
    } else {
      return null;
    }
  }

  public int mixinExecutionStateHash(int h) {
    for (StackFrame frame = top; frame != null; frame = frame.prev) {
      if (!frame.isNative()) {
        h = frame.mixinExecutionStateHash(h);
      }
    }
    
    return h;
  }
  
  public boolean hasDataChanged() {
    return (attributes & ATTR_DATA_CHANGED) != 0;
  }

  public boolean hasStackChanged() {
    return (attributes & ATTR_STACK_CHANGED) != 0;
  }

  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }

  
  public StackFrame getModifiableTopFrame () {
    if (top.isFrozen()) {
      top = top.clone();
      markTfChanged(top);
    }
    return top;
  }

  
  public StackFrame getTopFrame () {
    return top;
  }

  
  public StackFrame getModifiableFrame (StackFrame frame){
    StackFrame newTop = null;
    StackFrame last = null;
    boolean done = false;
    
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      done = (f == frame);
      
      if (f.isFrozen()){
        f = f.clone();
        if (newTop == null){
          newTop = f;
        } else {
          last.setPrevious(f);
        }
        last = f;
        
      }
      
      if (done){ 
        if (newTop != null){
          top = newTop;
          markTfChanged(top);
        }
        return f;
      }
    }
    
    return null; 
  }
  
  
  public StackFrame getStackFrameExecuting (Instruction insn, int offset){
    int n = offset;
    StackFrame frame = top;

    for (; (n > 0) && frame != null; frame = frame.getPrevious()){
      n--;
    }

    for(; frame != null; frame = frame.getPrevious()){
      if (frame.getPC() == insn){
        return frame;
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return "ThreadInfo [name=" + getName() +
            ",id=" + id +
            ",state=" + getStateName() +

            ']';
  }

  void setDaemon (boolean isDaemon) {
    threadDataClone().isDaemon = isDaemon;
  }

  public boolean isDaemon () {
    return threadData.isDaemon;
  }

  public MJIEnv getMJIEnv () {
    return env;
  }
  
  void setName (String newName) {
    threadDataClone().name = newName;



  }

  public void setPriority (int newPrio) {
    if (threadData.priority != newPrio) {
      threadDataClone().priority = newPrio;






    }
  }

  public int getPriority () {
    return threadData.priority;
  }

  
  @Override
  public int compareTo (ThreadInfo that) {
    return this.id - that.id;
  }
  
  
  
  public void checkConsistency(boolean isStore){
    checkAssertion(threadData != null, "no thread data");
    

    if (isRunnable()){
      checkAssertion(lockRef == MJIEnv.NULL, "runnable thread with non-null lockRef: " + lockRef) ;
    }
    

    if (!isTerminated() && !isNew()){
      checkAssertion( stackDepth > 0, "empty stack " + getState());
      checkAssertion( top != null, "no top frame");
      checkAssertion( top.getPC() != null, "no top PC");
    }
    

    if (isTimedOut()){
      Instruction insn = top.getPC();
      checkAssertion( insn instanceof EXECUTENATIVE, "thread timedout outside of native method");
      

      MethodInfo mi = ((EXECUTENATIVE)insn).getExecutedMethod();
      String mname = mi.getUniqueName();
      checkAssertion( mname.equals("wait(J") || mname.equals("park(ZJ"), "timedout thread outside timeout method: " + mi.getFullName());
    }
  
    List<ElementInfo> lockedObjects = getLockedObjects();
    
    if (lockRef != MJIEnv.NULL){

      ElementInfo ei = this.getElementInfo(lockRef);
      checkAssertion( ei != null, "thread locked on non-existing object: " + lockRef);
      

      checkAssertion( ei.isLocking(this), "thread blocked on non-locking object: " + ei);
        

      if (!isWaiting() && lockedObjectReferences.length > 0){
        for (ElementInfo lei : lockedObjects){
            checkAssertion( lei.getObjectRef() != lockRef, "non-waiting thread blocked on owned lock: " + lei);
        }
      }
      

      checkAssertion( isWaiting() || isBlockedOrNotified(), "locked thread not waiting, blocked or notified");
      
    } else { 
      checkAssertion( !isWaiting() && !isBlockedOrNotified(), "non-locked thread is waiting, blocked or notified");
    }
    

    if (lockedObjects != null && !lockedObjects.isEmpty()){
      for (ElementInfo ei : lockedObjects){
        ThreadInfo lti = ei.getLockingThread();
        if (lti != null){
          checkAssertion(lti == this, "not the locking thread for locked object: " + ei + " owned by " + lti);
        } else {

        }
      }
    }

  }
  
  protected void checkAssertion(boolean cond, String failMsg){
    if (!cond){
      System.out.println("!!!!!! failed thread consistency: "  + this + ": " + failMsg);
      vm.dumpThreadStates();
      assert false;
    }
  }
  
  public boolean isSystemThread() {
    return false;
  }
}
