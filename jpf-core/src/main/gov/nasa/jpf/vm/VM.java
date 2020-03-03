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
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFListenerException;
import gov.nasa.jpf.jvm.ClassFile;
import gov.nasa.jpf.vm.FinalizerThreadInfo;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.Predicate;

import java.io.PrintWriter;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;



public abstract class VM {

  
  public static final boolean CHECK_CONSISTENCY = false;
  
  protected static final String[] EMPTY_ARGS = new String[0];
  
  protected static JPFLogger log = JPF.getLogger("vm");

  
  protected JPF jpf;

  
  protected static int error_id;

  
  protected static VM vm;

  static {
    initStaticFields();
  }

  protected SystemState ss;
  
  protected FunctionObjectFactory funcObjFactory = new FunctionObjectFactory();









  protected Path path;  
  protected StringBuilder out;  

  
  protected Transition      lastTrailInfo;

  protected boolean isTraceReplay; 

  
  protected StateSet stateSet;

  
  protected int newStateId;

  
  protected Backtracker backtracker;

  
  protected StateRestorer<?> restorer;

  
  protected StateSerializer serializer;

  
  protected VMListener[] listeners = new VMListener[0];

  
  protected boolean transitionOccurred;

  
  protected TimeModel timeModel;
  
  
  protected Scheduler scheduler;
  
  
  protected Config config; 


  protected boolean runGc;
  protected boolean treeOutput;
  protected boolean pathOutput;
  protected boolean indentOutput;
  protected boolean processFinalizers;
  

  protected boolean isBigEndian;

  protected boolean initialized;


  protected Predicate<ThreadInfo> userliveNonDaemonPredicate;
  protected Predicate<ThreadInfo> timedoutRunnablePredicate;
  protected Predicate<ThreadInfo> alivePredicate;
  protected Predicate<ThreadInfo> userTimedoutRunnablePredicate;





  protected ArrayList<Runnable> postGcActions = new ArrayList<Runnable>();
  
  
  public VM (JPF jpf, Config conf) {
    this.jpf = jpf; 



    vm = this;

    config = conf;

    runGc = config.getBoolean("vm.gc", true);

    treeOutput = config.getBoolean("vm.tree_output", true);

    indentOutput = config.getBoolean("vm.indent_output",false);

    processFinalizers = config.getBoolean("vm.process_finalizers", false);
    
    isBigEndian = getPlatformEndianness(config);
    initialized = false;
    
    initTimeModel(config);

    initSubsystems(config);
    initFields(config);
    

    userliveNonDaemonPredicate = new Predicate<ThreadInfo>() {
      @Override
	public boolean isTrue (ThreadInfo ti) {
        return (!ti.isDaemon() && !ti.isTerminated() && !ti.isSystemThread());
      }
    };

    timedoutRunnablePredicate = new Predicate<ThreadInfo>() {
      @Override
	public boolean isTrue (ThreadInfo ti) {
        return (ti.isTimeoutRunnable());
      }
    };
    
    userTimedoutRunnablePredicate = new Predicate<ThreadInfo>() {
      @Override
	public boolean isTrue (ThreadInfo ti) {
        return (ti.isTimeoutRunnable() && !ti.isSystemThread());
      }
    };
    
    alivePredicate = new Predicate<ThreadInfo>() {
      @Override
	public boolean isTrue (ThreadInfo ti) {
        return (ti.isAlive());
      }
    };
  }

  
  protected VM (){}

  public JPF getJPF() {
    return jpf;
  }

  public void initFields (Config config) {
    path = new Path("fix-this!");
    out = null;

    ss = new SystemState(config, this);

    stateSet = config.getInstance("vm.storage.class", StateSet.class);
    if (stateSet != null) stateSet.attach(this);
    backtracker = config.getEssentialInstance("vm.backtracker.class", Backtracker.class);
    backtracker.attach(this);

    scheduler = config.getEssentialInstance("vm.scheduler.class", Scheduler.class);
    
    newStateId = -1;
  }

  protected void initSubsystems (Config config) {
    ClassLoaderInfo.init(config);
    ClassInfo.init(config);
    ThreadInfo.init(config);
    ElementInfo.init(config);
    MethodInfo.init(config);
    NativePeer.init(config);
    ChoiceGeneratorBase.init(config);
    

  }

  protected void initTimeModel (Config config){
    Class<?>[] argTypes = { VM.class, Config.class };
    Object[] args = { this, config };
    timeModel = config.getEssentialInstance("vm.time.class", TimeModel.class, argTypes, args);
  }
  
  
  public void cleanUp(){

  }
  
  protected boolean getPlatformEndianness (Config config){
    String endianness = config.getString("vm.endian");
    if (endianness == null) {
      return ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    } else if (endianness.equalsIgnoreCase("big")) {
      return true;
    } else if (endianness.equalsIgnoreCase("little")) {
      return false;
    } else {
      config.exception("illegal vm.endian value: " + endianness);
      return false; 
    }
  }
  
  public boolean isBigEndianPlatform(){
    return isBigEndian;
  }

  public boolean finalizersEnabled() {
    return processFinalizers;
  }
  
  public boolean isInitialized() {
    return initialized;
  }
  
  public boolean isSingleProcess() {
    return true;
  }

  
  static boolean checkSystemClassCompatibility (SystemClassLoaderInfo systemLoader) {
    ClassInfo ci = systemLoader.getClassClassInfo();
    return ci.checkIfValidClassClassInfo();
  }

  static boolean isValidClassName (String clsName) {
    if ( !clsName.matches("[a-zA-Z_$][a-zA-Z_$0-9.]*")) {
      return false;
    }




    if (clsName.endsWith(".java")) {
      return false;
    }
    if (clsName.endsWith(".class")) {
      return false;
    }

    return true;
  }


  protected ThreadInfo createMainThreadInfo (int id, ApplicationContext appCtx){
    ThreadInfo tiMain = new ThreadInfo( this, id, appCtx);
    ThreadInfo.currentThread = tiMain; 
    registerThread(tiMain);
    
    return tiMain;
  }
  
  protected ThreadInfo createThreadInfo (int objRef, int groupRef, int runnableRef, int nameRef){
    ThreadInfo tiCurrent = ThreadInfo.getCurrentThread();
    ThreadInfo tiNew = new ThreadInfo( this, objRef, groupRef, runnableRef, nameRef, tiCurrent);




    registerThread( tiNew);
    
    return tiNew;
  }


  protected ThreadInfo createFinalizerThreadInfo (int id, ApplicationContext appCtx){
    FinalizerThreadInfo finalizerTi = new FinalizerThreadInfo( this, appCtx, id);
    registerThread(finalizerTi);
    
    return finalizerTi;
  }
  
  
  protected List<String> getStartupSystemClassNames() {
    ArrayList<String> startupClasses = new ArrayList<String>(64);


    startupClasses.add("java.lang.Object");
    startupClasses.add("java.lang.Class");
    startupClasses.add("java.lang.ClassLoader");


    startupClasses.add("boolean");
    startupClasses.add("[Z");
    startupClasses.add("byte");
    startupClasses.add("[B");
    startupClasses.add("char");
    startupClasses.add("[C");
    startupClasses.add("short");
    startupClasses.add("[S");
    startupClasses.add("int");
    startupClasses.add("[I");
    startupClasses.add("long");
    startupClasses.add("[J");
    startupClasses.add("float");
    startupClasses.add("[F");
    startupClasses.add("double");
    startupClasses.add("[D");
    startupClasses.add("void");


    startupClasses.add("java.lang.Boolean");
    startupClasses.add("java.lang.Character");
    startupClasses.add("java.lang.Short");
    startupClasses.add("java.lang.Integer");
    startupClasses.add("java.lang.Long");
    startupClasses.add("java.lang.Float");
    startupClasses.add("java.lang.Double");
    startupClasses.add("java.lang.Byte");


    startupClasses.add("gov.nasa.jpf.BoxObjectCaches");


    startupClasses.add("java.lang.String");
    startupClasses.add("java.lang.Thread");
    startupClasses.add("java.lang.ThreadGroup");
    startupClasses.add("java.lang.Thread$State");
    startupClasses.add("java.lang.Thread$Permit");
    startupClasses.add("java.io.PrintStream");
    startupClasses.add("java.io.InputStream");
    startupClasses.add("java.lang.System");
    startupClasses.add("java.lang.ref.Reference");
    startupClasses.add("java.lang.ref.WeakReference");
    startupClasses.add("java.lang.Enum");
    startupClasses.add("gov.nasa.jpf.FinalizerThread");




    String[] extraStartupClasses = config.getStringArray("vm.extra_startup_classes");
    if (extraStartupClasses != null) {      
      for (String extraCls : extraStartupClasses) {
        startupClasses.add(extraCls);
      }
    }



    return startupClasses;
  }

  
  protected List<ClassInfo> getStartupSystemClassInfos (SystemClassLoaderInfo sysCl, ThreadInfo tiMain){
    LinkedList<ClassInfo> list = new LinkedList<ClassInfo>();
    
    try {
      for (String clsName : getStartupSystemClassNames()) {
        ClassInfo ci = sysCl.getResolvedClassInfo(clsName);
        ci.registerStartupClass( tiMain, list); 
      }
    } catch (ClassInfoException e){
      e.printStackTrace();
      throw new JPFConfigException("cannot load system class " + e.getFailedClass());
    } 
    
    return list;
  }
  
  
  protected ClassInfo getMainClassInfo (SystemClassLoaderInfo sysCl, String mainClassName, ThreadInfo tiMain, List<ClassInfo> list){
    try {
      ClassInfo ciMain = sysCl.getResolvedClassInfo(mainClassName);
      ciMain.registerStartupClass(tiMain, list); 
      
      return ciMain;
      
    } catch (ClassInfoException e){
      throw new JPFConfigException("cannot load application class " + e.getFailedClass());
    }
  }
  
  
  protected SystemClassLoaderInfo createSystemClassLoaderInfo (int appId) {
    Class<?>[] argTypes = { VM.class, int.class };
   
    Object[] args = { this, Integer.valueOf(appId)};
    SystemClassLoaderInfo sysCli = config.getEssentialInstance("vm.classloader.class", SystemClassLoaderInfo.class, argTypes, args);
    return sysCli;
  }
  
  protected void createSystemClassLoaderObject (SystemClassLoaderInfo sysCl, ThreadInfo tiMain) {
    Heap heap = getHeap();



    ClassInfo ciCl = sysCl.getClassLoaderClassInfo();
    ElementInfo ei = heap.newObject( ciCl, tiMain);

    heap.registerPinDown(ei.getObjectRef());

    sysCl.setClassLoaderObject(ei);
  }  
  
  protected void pushMainEntryArgs (MethodInfo miMain, String[] args, ThreadInfo tiMain, DirectCallStackFrame frame){
    String sig = miMain.getSignature();
    Heap heap = getHeap();
    
    if (sig.contains("([Ljava/lang/String;)")){
      ElementInfo eiArgs = heap.newArray("Ljava/lang/String;", args.length, tiMain);
      for (int i = 0; i < args.length; i++) {
        ElementInfo eiArg = heap.newString(args[i], tiMain);
        eiArgs.setReferenceElement(i, eiArg.getObjectRef());
      }
      frame.setReferenceArgument( 0, eiArgs.getObjectRef(), null);

    } else if (sig.contains("(Ljava/lang/String;)")){
      if (args.length > 1){
        ElementInfo eiArg = heap.newString(args[0], tiMain);
        frame.setReferenceArgument( 0, eiArg.getObjectRef(), null);
      } else {
        frame.setReferenceArgument( 0, MJIEnv.NULL, null);
      }
      
    } else if (!sig.contains("()")){
      throw new JPFException("unsupported main entry signature: " + miMain.getFullName());
    }
  }
  
  protected void pushMainEntry (MethodInfo miMain, String[] args, ThreadInfo tiMain) {
    DirectCallStackFrame frame = miMain.createDirectCallStackFrame(tiMain, 0);
    pushMainEntryArgs( miMain, args, tiMain, frame);    
    tiMain.pushFrame(frame);
  }

  protected MethodInfo getMainEntryMethodInfo (String mthName, ClassInfo ciMain) {
    MethodInfo miMain = ciMain.getMethod(mthName, true);


    if (miMain == null || !miMain.isStatic()) {
      throw new JPFConfigException("no static entry method: " + ciMain.getName() + '.' + mthName);
    }
    
    return miMain;
  }
  
  protected void pushClinits (List<ClassInfo> startupClasses, ThreadInfo tiMain) {
    for (ClassInfo ci : startupClasses){
      MethodInfo mi = ci.getMethod("<clinit>()V", false);
      if (mi != null) {
        DirectCallStackFrame frame = mi.createDirectCallStackFrame(tiMain, 0);
        tiMain.pushFrame(frame);
      } else {
        ci.setInitialized();
      }      
    }
  }
  
  
  public abstract boolean initialize ();
  
  
  protected ThreadInfo initializeMainThread (ApplicationContext appCtx, int tid){
    SystemClassLoaderInfo sysCl = appCtx.sysCl;
    
    ThreadInfo tiMain = createMainThreadInfo(tid, appCtx);
    List<ClassInfo> startupClasses = getStartupSystemClassInfos(sysCl, tiMain);
    ClassInfo ciMain = getMainClassInfo(sysCl, appCtx.mainClassName, tiMain, startupClasses);

    if (!checkSystemClassCompatibility( sysCl)){
      throw new JPFConfigException("non-JPF system classes, check classpath");
    }
    

    createSystemClassLoaderObject(sysCl, tiMain);
    for (ClassInfo ci : startupClasses) {
      ci.createAndLinkStartupClassObject(tiMain);
    }
    tiMain.createMainThreadObject(sysCl);
    registerThread(tiMain);
    

    MethodInfo miMain = getMainEntryMethodInfo(appCtx.mainEntry, ciMain);
    appCtx.setEntryMethod(miMain);
    pushMainEntry(miMain, appCtx.args, tiMain);
    Collections.reverse(startupClasses);
    pushClinits(startupClasses, tiMain);

    registerThreadListCleanup(sysCl.getThreadClassInfo());

    return tiMain;
  }
  
  protected void initializeFinalizerThread (ApplicationContext appCtx, int tid) {
    if(processFinalizers) {
      ApplicationContext app = getCurrentApplicationContext();
      FinalizerThreadInfo finalizerTi = app.getFinalizerThread();
    
      finalizerTi = (FinalizerThreadInfo) createFinalizerThreadInfo(tid, app);
      finalizerTi.createFinalizerThreadObject(app.getSystemClassLoader());
    
      appCtx.setFinalizerThread(finalizerTi);
    }
  }
  
  protected void registerThreadListCleanup (ClassInfo ciThread){
    assert ciThread != null : "java.lang.Thread not loaded yet";
    
    ciThread.addReleaseAction( new ReleaseAction(){
      @Override
	public void release (ElementInfo ei) {
        ThreadList tl = getThreadList();
        int objRef = ei.getObjectRef();
        ThreadInfo ti = tl.getThreadInfoForObjRef(objRef);
        if (tl.remove(ti)){        
          vm.getKernelState().changed();    
        }
      }
    });
  }
  

  
  protected void setRootCG(){
    scheduler.setRootCG();
  }
  
  protected void initSystemState (ThreadInfo mainThread){
    ss.setStartThread(mainThread);

    ss.recordSteps(hasToRecordSteps());

    if (!pathOutput) { 
      pathOutput = hasToRecordPathOutput();
    }

    setRootCG(); 
    if (!hasNextChoiceGenerator()){
      throw new JPFException("scheduler failed to set ROOT choice generator: " + scheduler);
    }
    
    transitionOccurred = true;
  }
  
  public void addPostGcAction (Runnable r){
    postGcActions.add(r);
  }
  
  
  public void processPostGcActions(){
    if (!postGcActions.isEmpty()){
      for (Runnable r : postGcActions){
        r.run();
      }
      
      postGcActions.clear();
    }
  }
  
  public void addListener (VMListener newListener) {
    log.info("VMListener added: ", newListener);
    listeners = Misc.appendElement(listeners, newListener);
  }

  public boolean hasListenerOfType (Class<?> listenerCls) {
    return Misc.hasElementOfType(listeners, listenerCls);
  }

  public <T> T getNextListenerOfType(Class<T> type, T prev){
    return Misc.getNextElementOfType(listeners, type, prev);
  }
  
  public void removeListener (VMListener removeListener) {
    listeners = Misc.removeElement(listeners, removeListener);
  }

  public void setTraceReplay (boolean isReplay) {
    isTraceReplay = isReplay;
  }

  public boolean isTraceReplay() {
    return isTraceReplay;
  }

  public boolean hasToRecordSteps() {


    return jpf.getReporter().hasToReportTrace()
             || config.getBoolean("vm.store_steps");
  }

  public void recordSteps( boolean cond) {


    config.setProperty("vm.store_steps", cond ? "true" : "false");

    if (ss != null){
      ss.recordSteps(cond);
    }
  }

  public boolean hasToRecordPathOutput() {
    if (config.getBoolean("vm.path_output")){ 
      return true;
    } else {
      return jpf.getReporter().hasToReportOutput(); 
    }
  }
  

  
  
  
  protected void notifyVMInitialized () {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].vmInitialized(this);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during vmInitialized() notification", t);
    }    
  }
  
  protected void notifyChoiceGeneratorRegistered (ChoiceGenerator<?>cg, ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorRegistered(this, cg, ti, ti.getPC());
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorRegistered() notification", t);
    }
  }

  protected void notifyChoiceGeneratorSet (ChoiceGenerator<?>cg) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorSet(this, cg);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorSet() notification", t);
    }
  }

  protected void notifyChoiceGeneratorAdvanced (ChoiceGenerator<?>cg) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorAdvanced(this, cg);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorAdvanced() notification", t);
    }
  }

  protected void notifyChoiceGeneratorProcessed (ChoiceGenerator<?>cg) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorProcessed(this, cg);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorProcessed() notification", t);
    }
  }

  protected void notifyExecuteInstruction (ThreadInfo ti, Instruction insn) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].executeInstruction(this, ti, insn);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during executeInstruction() notification", t);
    }
  }

  protected void notifyInstructionExecuted (ThreadInfo ti, Instruction insn, Instruction nextInsn) {
    try {

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].instructionExecuted(this, ti, nextInsn, insn);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during instructionExecuted() notification", t);
    }
  }

  protected void notifyThreadStarted (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadStarted(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadStarted() notification", t);
    }
  }



  protected void notifyThreadBlocked (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadBlocked(this, ti, ti.getLockObject());
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadBlocked() notification", t);
    }
  }

  protected void notifyThreadWaiting (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadWaiting(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadWaiting() notification", t);
    }
  }

  protected void notifyThreadNotified (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadNotified(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadNotified() notification", t);
    }
  }

  protected void notifyThreadInterrupted (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadInterrupted(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadInterrupted() notification", t);
    }
  }

  protected void notifyThreadTerminated (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadTerminated(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadTerminated() notification", t);
    }
  }

  protected void notifyThreadScheduled (ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadScheduled(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadScheduled() notification", t);
    }
  }
  
  protected void notifyLoadClass (ClassFile cf){
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].loadClass(this, cf);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during classLoaded() notification", t);
    }    
  }

  protected void notifyClassLoaded(ClassInfo ci) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].classLoaded(this, ci);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during classLoaded() notification", t);
    }
  }

  protected void notifyObjectCreated(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectCreated(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectCreated() notification", t);
    }
  }

  protected void notifyObjectReleased(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectReleased(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectReleased() notification", t);
    }
  }

  protected void notifyObjectLocked(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectLocked(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectLocked() notification", t);
    }
  }

  protected void notifyObjectUnlocked(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectUnlocked(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectUnlocked() notification", t);
    }
  }

  protected void notifyObjectWait(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectWait(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectWait() notification", t);
    }
  }

   protected void notifyObjectExposed(ThreadInfo ti, ElementInfo eiShared, ElementInfo eiExposed) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectExposed(this, ti, eiShared, eiExposed);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectExposed() notification", t);
    }
  }

   protected void notifyObjectShared(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectShared(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectShared() notification", t);
    }
  }
  
  protected void notifyObjectNotifies(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectNotify(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectNotifies() notification", t);
    }
  }

  protected void notifyObjectNotifiesAll(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectNotifyAll(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectNotifiesAll() notification", t);
    }
  }

  protected void notifyGCBegin() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].gcBegin(this);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during gcBegin() notification", t);
    }
  }

  protected void notifyGCEnd() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].gcEnd(this);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during gcEnd() notification", t);
    }
  }

  protected void notifyExceptionThrown(ThreadInfo ti, ElementInfo ei) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].exceptionThrown(this, ti, ei);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during exceptionThrown() notification", t);
    }
  }

  protected void notifyExceptionBailout(ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].exceptionBailout(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during exceptionBailout() notification", t);
    }
  }

  protected void notifyExceptionHandled(ThreadInfo ti) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].exceptionHandled(this, ti);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during exceptionHandled() notification", t);
    }
  }

  protected void notifyMethodEntered(ThreadInfo ti, MethodInfo mi) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].methodEntered(this, ti, mi);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during methodEntered() notification", t);
    }
  }

  protected void notifyMethodExited(ThreadInfo ti, MethodInfo mi) {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].methodExited(this, ti, mi);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during methodExited() notification", t);
    }
  }


  public String getThreadName () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    return ti.getName();
  }


  public Instruction getInstruction () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    return ti.getPC();
  }

  
  public ExceptionInfo getPendingException () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    if (ti != null){
      return ti.getPendingException();
    } else {
      return null;
    }
  }

  public Step getLastStep () {
    Transition trail = ss.getTrail();
    if (trail != null) {
      return trail.getLastStep();
    }

    return null;
  }

  public Transition getLastTransition () {
    if (path.size() == 0) {
      return null;
    }
    return path.get(path.size() - 1);
  }

  public ClassInfo getClassInfo (int objref) {
    if (objref != MJIEnv.NULL) {
      return getElementInfo(objref).getClassInfo();
    } else {
      return null;
    }
  }

  
  public Path getPath () {
    return path;
  }

  
  public Transition getCurrentTransition() {
    return ss.getTrail();
  }

  
  public Path getClonedPath () {
    return path.clone();
  }

  public int getPathLength () {
    return path.size();
  }

  public ThreadList getThreadList () {
    return getKernelState().getThreadList();
  }
  
  public ClassLoaderList getClassLoaderList() {
    return getKernelState().getClassLoaderList();
  }

  
  
  public RestorableVMState getRestorableState () {
    return new RestorableVMState(this);
  }

  
  public SystemState getSystemState () {
    return ss;
  }

  public KernelState getKernelState () {
    return ss.getKernelState();
  }

  public void kernelStateChanged(){
    ss.getKernelState().changed();
  }
  
  public Config getConfig() {
    return config;
  }

  public Backtracker getBacktracker() {
    return backtracker;
  }

  @SuppressWarnings("unchecked")
  public <T> StateRestorer<T> getRestorer() {
    if (restorer == null) {
      if (serializer instanceof StateRestorer) {
        restorer = (StateRestorer<?>) serializer;
      } else if (stateSet instanceof StateRestorer) {
        restorer = (StateRestorer<?>) stateSet;
      } else {

        restorer = config.getInstance("vm.restorer.class", StateRestorer.class);
      }
      restorer.attach(this);
    }

    return (StateRestorer<T>) restorer;
  }

  public StateSerializer getSerializer() {
    if (serializer == null) {
      serializer = config.getEssentialInstance("vm.serializer.class",
                                      StateSerializer.class);
      serializer.attach(this);
    }
    return serializer;
  }

  public void setSerializer (StateSerializer newSerializer){
    serializer = newSerializer;
    serializer.attach(this);
  }
  
  
  public StateSet getStateSet() {
    return stateSet;
  }

  public Scheduler getScheduler(){
    return scheduler;
  }
  
  public FunctionObjectFactory getFunctionObjectFacotry() {
    return funcObjFactory;
  }
  
  
  public ChoiceGenerator<?> getChoiceGenerator () {
    return ss.getChoiceGenerator();
  }

  public ChoiceGenerator<?> getNextChoiceGenerator() {
    return ss.getNextChoiceGenerator();
  }
  
  public boolean hasNextChoiceGenerator(){
    return (ss.getNextChoiceGenerator() != null);
  }

  public boolean setNextChoiceGenerator (ChoiceGenerator<?> cg){
    return ss.setNextChoiceGenerator(cg);
  }
  
  public void setMandatoryNextChoiceGenerator (ChoiceGenerator<?> cg, String failMsg){
    ss.setMandatoryNextChoiceGenerator(cg, failMsg);
  }
  
  
  public <T extends ChoiceGenerator<?>> T getCurrentChoiceGenerator (String id, Class<T> cgType) {
    return ss.getCurrentChoiceGenerator(id,cgType);
  }

  
  public ChoiceGenerator<?>[] getChoiceGenerators() {
    return ss.getChoiceGenerators();
  }

  public <T extends ChoiceGenerator<?>> T[] getChoiceGeneratorsOfType (Class<T> cgType) {
    return ss.getChoiceGeneratorsOfType(cgType);
  }

  public <T extends ChoiceGenerator<?>> T getLastChoiceGeneratorOfType (Class<T> cgType){
    return ss.getLastChoiceGeneratorOfType(cgType);
  }

  public ChoiceGenerator<?> getLastChoiceGeneratorInThread (ThreadInfo ti){
    return ss.getLastChoiceGeneratorInThread(ti);
  }
  
  public void print (String s) {
    if (treeOutput) {
      System.out.print(s);
    }

    if (pathOutput) {
      appendOutput(s);
    }
  }

  public void println (String s) {
    if (treeOutput) {
      if (indentOutput){
        StringBuilder indent = new StringBuilder();
        int i;
        for (i = 0;i<=path.size();i++) {
          indent.append('|').append(i);
        }
        indent.append("|").append(s);
        System.out.println(indent);
      }
      else {
        System.out.println(s);
      }
    }

    if (pathOutput) {
      appendOutput(s);
      appendOutput('\n');
    }
  }

  public void print (boolean b) {
    if (treeOutput) {
      System.out.print(b);
    }

    if (pathOutput) {
      appendOutput(Boolean.toString(b));
    }
  }

  public void print (char c) {
    if (treeOutput) {
      System.out.print(c);
    }

    if (pathOutput) {
      appendOutput(c);
    }
  }

  public void print (int i) {
    if (treeOutput) {
      System.out.print(i);
    }

    if (pathOutput) {
      appendOutput(Integer.toString(i));
    }
  }

  public void print (long l) {
    if (treeOutput) {
      System.out.print(l);
    }

    if (pathOutput) {
      appendOutput(Long.toString(l));
    }
  }

  public void print (double d) {
    if (treeOutput) {
      System.out.print(d);
    }

    if (pathOutput) {
      appendOutput(Double.toString(d));
    }
  }

  public void print (float f) {
    if (treeOutput) {
      System.out.print(f);
    }

    if (pathOutput) {
      appendOutput(Float.toString(f));
    }
  }

  public void println () {
    if (treeOutput) {
      System.out.println();
    }

    if (pathOutput) {
      appendOutput('\n');
    }
  }


  void appendOutput (String s) {
    if (out == null) {
      out = new StringBuilder();
    }
    out.append(s);
  }

  void appendOutput (char c) {
    if (out == null) {
      out = new StringBuilder();
    }
    out.append(c);
  }

  
  public String getPendingOutput() {
    if (out != null && out.length() > 0){
      return out.toString();
    } else {
      return null;
    }
  }
  
  
  public Instruction handleException (ThreadInfo ti, int xObjRef){
    ti = null;        
    xObjRef = 0;
    return null;
  }

  public void storeTrace (String fileName, String comment, boolean verbose) {
    ChoicePoint.storeTrace(fileName, getSUTName(), comment,
                           ss.getChoiceGenerators(), verbose);
  }

  public void storePathOutput () {
    pathOutput = true;
  }

  private void printCG (ChoiceGenerator<?> cg, int n){
    ChoiceGenerator cgPrev = cg.getPreviousChoiceGenerator();
    if (cgPrev != null){
      printCG( cgPrev, --n);
    }
    
    System.out.printf("[%d] ", n);
    System.out.println(cg);
  } 
  

  public void printChoiceGeneratorStack(){
    ChoiceGenerator<?> cg = getChoiceGenerator();
    if (cg != null){
      int n = cg.getNumberOfParents();
      printCG(cg, n);
    }
  }
  
  public ThreadInfo[] getLiveThreads () {
    return getThreadList().getThreads();
  }
  
  
  public void printLiveThreadStatus (PrintWriter pw) {
    int nThreads = ss.getThreadCount();
    ThreadInfo[] threads = getThreadList().getThreads();
    int n=0;

    for (int i = 0; i < nThreads; i++) {
      ThreadInfo ti = threads[i];

      if (ti.getStackDepth() > 0){
        n++;


        pw.println(ti.getStateDescription());

        List<ElementInfo> locks = ti.getLockedObjects();
        if (!locks.isEmpty()) {
          pw.print("  owned locks:");
          boolean first = true;
          for (ElementInfo e : locks) {
            if (first) {
              first = false;
            } else {
              pw.print(",");
            }
            pw.print(e);
          }
          pw.println();
        }

        ElementInfo ei = ti.getLockObject();
        if (ei != null) {
          if (ti.getState() == ThreadInfo.State.WAITING) {
            pw.print( "  waiting on: ");
          } else {
            pw.print( "  blocked on: ");
          }
          pw.println(ei);
        }

        pw.println("  call stack:");
        for (StackFrame frame : ti){
          if (!frame.isDirectCallFrame()) {
            pw.print("\tat ");
            pw.println(frame.getStackTraceInfo());
          }
        }

        pw.println();
      }
    }

    if (n==0) {
      pw.println("no live threads");
    }
  }


  public void dumpThreadStates () {
    java.io.PrintWriter pw = new java.io.PrintWriter(System.out, true);
    printLiveThreadStatus(pw);
    pw.flush();
  }

  
  public boolean backtrack () {
    transitionOccurred = false;

    boolean success = backtracker.backtrack();
    if (success) {
      if (CHECK_CONSISTENCY) checkConsistency(false);
      

      path.removeLast();
      lastTrailInfo = path.getLast();

      return true;
      
    } else {
      return false;
    }
  }

  
  public void updatePath () {
    Transition t = ss.getTrail();
    Transition tLast = path.getLast();




    if (tLast != t) {




      if ((out != null) && (out.length() > 0)) {
        t.setOutput( out.toString());
        out.setLength(0);
      }

      path.add(t);
    }
  }

  
  public boolean forward () {
















    transitionOccurred = ss.initializeNextTransition(this);
    
    if (transitionOccurred){
      if (CHECK_CONSISTENCY) {
        checkConsistency(true); 
      }

      backtracker.pushKernelState();


      lastTrailInfo = path.getLast();

      try {
        ss.executeNextTransition(vm);

      } catch (UncaughtException e) {


      } 

      backtracker.pushSystemState();
      updatePath();

      if (!isIgnoredState()) {




        if (runGc && !hasPendingException()) {
          if(ss.gcIfNeeded()) {
            processFinalizers();
          }
        }

        if (stateSet != null) {
          newStateId = stateSet.size();
          int id = stateSet.addCurrent();
          ss.setId(id);

        } else { 
          ss.setId(++newStateId); 
        }
      }
      
      return true;

    } else {

      return false;  
    }
  }

  
  public void printCurrentStackTrace () {
    ThreadInfo th = ThreadInfo.getCurrentThread();

    if (th != null) {
      th.printStackTrace();
    }
  }


  public void restoreState (RestorableVMState state) {
    if (state.path == null) {
      throw new JPFException("tried to restore partial VMState: " + state);
    }
    backtracker.restoreState(state.getBkState());
    path = state.path.clone();
  }

  public void activateGC () {
    ss.activateGC();
  }




  public void retainStateAttributes (boolean isRetained){
    ss.retainAttributes(isRetained);
  }

  public void forceState () {
    ss.setForced(true);
  }

  
  public void ignoreState (boolean cond) {
    ss.setIgnored(cond);
  }

  public void ignoreState(){
    ignoreState(true);
  }

  
  public void breakTransition (String reason) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    ti.breakTransition(reason);
  }

  public boolean transitionOccurred(){
    return transitionOccurred;
  }

  
  public boolean isNewState() {

    if (!transitionOccurred){
      return false;
    }

    if (stateSet != null) {
      if (ss.isForced()){
        return true;
      } else if (ss.isIgnored()){
        return false;
      } else {
        return (newStateId == ss.getId());
      }

    } else { 
      return true;
    }
  }

  
  public abstract boolean isEndState ();

  public boolean isVisitedState(){
    return !isNewState();
  }

  public boolean isIgnoredState(){
    return ss.isIgnored();
  }

  public boolean isInterestingState () {
    return ss.isInteresting();
  }

  public boolean isBoringState () {
    return ss.isBoring();
  }

  public boolean hasPendingException () {
    return (getPendingException() != null);
  }

  public abstract boolean isDeadlocked ();
  
  public Exception getException () {
    return ss.getUncaughtException();
  }



  
  public int getStateId() {
    return ss.getId();
  }

  public int getStateCount() {
    return newStateId;
  }


  
  public static VM getVM () {
    return vm;
  }

  
  public Search getSearch() {
    return jpf.getSearch();
  }
  
  
  static void initStaticFields () {
    error_id = 0;
  }

  
  public abstract ApplicationContext getCurrentApplicationContext();
  public abstract ApplicationContext getApplicationContext(int objRef);
  public abstract ApplicationContext[] getApplicationContexts();
  public abstract String getSUTName();
  public abstract String getSUTDescription();

  public abstract int getNumberOfApplications();
  
  public Heap getHeap() {
    return ss.getHeap();
  }

  public ElementInfo getElementInfo(int objref){
    return ss.getHeap().get(objref);
  }

  public ElementInfo getModifiableElementInfo(int objref){
    return ss.getHeap().getModifiable(objref);
  }

  
  public ThreadInfo getCurrentThread () {
    return ThreadInfo.currentThread;
  }
  
  public void registerClassLoader(ClassLoaderInfo cl) {
    this.getKernelState().addClassLoader(cl);
  }

  public int registerThread (ThreadInfo ti){
    getKernelState().changed();
    return getThreadList().add(ti);    
  }

  
  protected ClassLoaderInfo getClassLoader(int gid) {
    return ss.ks.getClassLoader(gid);
  }

  
  public long currentTimeMillis () {
    return timeModel.currentTimeMillis();
  }

  
  public long nanoTime() {
    return timeModel.nanoTime();
  }

  public void resetNextCG() {
    if (ss.nextCg != null) {
      ss.nextCg.reset();
    }
  }
  
  
  public void checkConsistency(boolean isStateStore) {
    getThreadList().checkConsistency( isStateStore);
    getHeap().checkConsistency( isStateStore);
  }
  
  public abstract void terminateProcess (ThreadInfo ti);
  


  public abstract Map<Integer,IntTable<String>> getInitialInternStringsMap();
  

  
  public abstract Predicate<ThreadInfo> getRunnablePredicate();
  
  public abstract Predicate<ThreadInfo> getDaemonRunnablePredicate();
  
  public abstract Predicate<ThreadInfo> getAppTimedoutRunnablePredicate();
  
  public Predicate<ThreadInfo> getUserTimedoutRunnablePredicate () {
    return userTimedoutRunnablePredicate;
  }
  
  public Predicate<ThreadInfo> getUserLiveNonDaemonPredicate() {
    return userliveNonDaemonPredicate;
  }
  
  public Predicate<ThreadInfo> getTimedoutRunnablePredicate () {
    return timedoutRunnablePredicate;
  }
  
  public Predicate<ThreadInfo> getAlivePredicate () {
    return alivePredicate;
  }
  
  

    
  public FinalizerThreadInfo getFinalizerThread() {
    return getCurrentApplicationContext().getFinalizerThread();
  }
  
  abstract void updateFinalizerQueues();
  
  public void processFinalizers() {
    if(processFinalizers) {
      updateFinalizerQueues();
      ChoiceGenerator<?> cg = getNextChoiceGenerator();
      if(cg==null || (cg.isSchedulingPoint() && !cg.isCascaded())) {
        getFinalizerThread().scheduleFinalizer();
      }
    }
  }
}
