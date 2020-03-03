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
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.TypeSpecMatcher;
import gov.nasa.jpf.vm.choice.BreakGenerator;

import java.io.PrintWriter;
import java.util.LinkedHashMap;



public class SystemState {

  
  static class Memento {
    ChoiceGenerator<?> curCg;  
    ChoiceGenerator<?> nextCg;
    int atomicLevel;
    ChoicePoint trace;
    ThreadInfo execThread;
    int id;              
    LinkedHashMap<Object,ClosedMemento> restorers;
    
    static protected ChoiceGenerator<?> cloneCG( ChoiceGenerator<?> cg){
      if (cg != null){
        try {
          return cg.deepClone();
        } catch (CloneNotSupportedException cnsx){
          throw new JPFException("clone failed: " + cg);          
        }
      } else {
        return null;
      }
    }
    
    Memento (SystemState ss) {
      nextCg = ss.nextCg;      
      curCg = ss.curCg;
      
      atomicLevel = ss.entryAtomicLevel; 
      id = ss.id;
      execThread = ss.execThread;
      

      restorers = ss.restorers;
    }

    
    void backtrack (SystemState ss) {
      ss.nextCg = null; 
      ss.curCg = curCg;
      
      ss.atomicLevel = atomicLevel;
      ss.id = id;
      ss.execThread = execThread;
      
      if (restorers != null){
        for (ClosedMemento r : restorers.values()){
          r.restore();
        }
      }
    }

    void restore (SystemState ss) {
      throw new JPFException("can't restore a SystemState.Memento that was created for backtracking");
      
      
    }
  }
  
  
  static class RestorableMemento extends Memento {
    RestorableMemento (SystemState ss){
      super(ss);
      
      nextCg = cloneCG(nextCg);
      curCg = cloneCG( curCg);
    }
    
    @Override
	void backtrack (SystemState ss){
      super.backtrack(ss);
      ss.curCg = cloneCG(curCg);
    }
    
    
    @Override
	void restore (SystemState ss) {      

      ss.nextCg = cloneCG(nextCg);
      ss.curCg = cloneCG(curCg);

      ss.atomicLevel = atomicLevel;
      ss.id = id;
      ss.execThread = execThread;
      
      if (restorers != null){
        for (ClosedMemento r : restorers.values()){
          r.restore();
        }
      }
    }  
  }

  int id;                   

  ChoiceGenerator<?> nextCg;   
  ChoiceGenerator<?>  curCg;   
  ThreadInfo execThread;    
  


  LinkedHashMap<Object,ClosedMemento> restorers;
  

  
  public KernelState ks;

  public Transition trail;      



  boolean retainAttributes; 


  boolean isIgnored; 
  boolean isForced;  


  boolean isInteresting;
  boolean isBoring;

  boolean isBlockedInAtomicSection;

  
  public UncaughtException uncaughtException;

  
  boolean GCNeeded = false;





  int maxAllocGC;
  int nAlloc;

  
  int atomicLevel;
  int entryAtomicLevel;

  
  boolean recordSteps;

  
  TypeSpecMatcher extendTransitions;
  
  
  public SystemState (Config config, VM vm) {
    ks = new KernelState(config);
    id = StateSet.UNKNOWN_ID;

    Class<?>[] argTypes = { Config.class, VM.class, SystemState.class };


    
    maxAllocGC = config.getInt("vm.max_alloc_gc", Integer.MAX_VALUE);
    if (maxAllocGC <= 0){
      maxAllocGC = Integer.MAX_VALUE;
    }

    extendTransitions = TypeSpecMatcher.create(config.getStringArray("vm.extend_transitions"));

  }

  protected SystemState() {

  }

  public void setStartThread (ThreadInfo ti) {
    execThread = ti;
    trail = new Transition(nextCg, execThread);
  }

  public int getId () {
    return id;
  }

  public void setId (int newId) {
    id = newId;
    trail.setStateId(newId);
    
    if (nextCg != null){
      nextCg.setStateId(newId);
    }
  }

  public void recordSteps (boolean cond) {
    recordSteps = cond;
  }

  
  public void incAtomic () {
    atomicLevel++;
  }

  public void decAtomic () {
    if (atomicLevel > 0) {
      atomicLevel--;
    }
  }
  public void clearAtomic() {
    atomicLevel = 0;
  }

  public boolean isAtomic () {
    return (atomicLevel > 0);
  }

  public boolean isBlockedInAtomicSection() {
    return isBlockedInAtomicSection;
  }

  public void setBlockedInAtomicSection() {
    isBlockedInAtomicSection = true;
  }

  public Transition getTrail() {
    return trail;
  }

  public KernelState getKernelState() {
    return ks;
  }

  public Heap getHeap() {
    return ks.getHeap();
  }



  
  public ChoiceGenerator<?> getChoiceGenerator () {
    return curCg;
  }

  public ChoiceGenerator<?> getChoiceGenerator (String id) {
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getPreviousChoiceGenerator()){
      if (id.equals(cg.getId())){
        return cg;
      }
    }

    return null;
  }
  
  
  public ChoiceGenerator<?>[] getChoiceGenerators () {
    if (curCg != null){
      return curCg.getAll();
    } else {
      return null;
    }
  }

  public ChoiceGenerator<?> getLastChoiceGeneratorInThread (ThreadInfo ti){
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getPreviousChoiceGenerator()){
      if (cg.getThreadInfo() == ti){
        return cg;
      }
    }
    
    return null;
  }
  
  
  public <T extends ChoiceGenerator<?>> T[] getChoiceGeneratorsOfType (Class<T> cgType) {
    if (curCg != null){
      return curCg.getAllOfType(cgType);
    } else {
      return null;
    }
  }


  public <T extends ChoiceGenerator<?>> T getLastChoiceGeneratorOfType (Class<T> cgType) {
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getPreviousChoiceGenerator()){
      if (cgType.isAssignableFrom(cg.getClass())) {
        return (T)cg;
      }
    }

    return null;
  }

  public <T> ChoiceGenerator<T> getLastChoiceGeneratorOfChoiceType (String id, Class<T> choiceType){
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getPreviousChoiceGenerator()){
      if ((id == null || id.equals(cg.getId())) && choiceType.isAssignableFrom(cg.getChoiceType())) {
        return (ChoiceGenerator<T>)cg;
      }
    }

    return null;    
  }

  
  public <T extends ChoiceGenerator<?>> T getCurrentChoiceGeneratorOfType (Class<T> cgType) {
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getCascadedParent()){
      if (cgType.isAssignableFrom(cg.getClass())){
        return (T)cg;
      }
    }

    return null;
  }

  public <T extends ChoiceGenerator<?>> T getCurrentChoiceGenerator (String id, Class<T> cgType) {
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getCascadedParent()){
      if (id.equals(cg.getId()) && cgType.isAssignableFrom(cg.getClass())){
        return (T)cg;
      }
    }

    return null;
  }
  
  public <T> ChoiceGenerator<T> getCurrentChoiceGeneratorForChoiceType (String id, Class<T> choiceType){
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getCascadedParent()){
      if ((id == null || id.equals(cg.getId())) && choiceType.isAssignableFrom(cg.getChoiceType())){
        return (ChoiceGenerator<T>)cg;
      }
    }

    return null;    
  }


  public ChoiceGenerator<?> getCurrentChoiceGenerator (String id) {
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getCascadedParent()){
      if (id.equals(cg.getId())){
        return cg;
      }
    }

    return null;
  }

  public ChoiceGenerator<?> getCurrentChoiceGenerator (ChoiceGenerator<?> cgPrev) {
    if (cgPrev == null){
      return curCg;
    } else {
      return cgPrev.getCascadedParent();
    }
  }

  
  public ThreadChoiceGenerator getCurrentSchedulingPoint () {
    for (ChoiceGenerator<?> cg = curCg; cg != null; cg = cg.getCascadedParent()){
      if (cg instanceof ThreadChoiceGenerator){
        ThreadChoiceGenerator tcg = (ThreadChoiceGenerator)cg;
        if (tcg.isSchedulingPoint()){
          return tcg;
        }
      }
    }

    return null;
  }

  public ChoiceGenerator<?>[] getCurrentChoiceGenerators () {
    return curCg.getCascade();
  }

  
  public <T extends ChoiceGenerator<?>> T getInsnChoiceGeneratorOfType (Class<T> cgType, Instruction insn, ChoiceGenerator<?> cgPrev){
    ChoiceGenerator<?> cg = cgPrev != null ? cgPrev.getPreviousChoiceGenerator() : curCg;

    if (cg != null && cg.getInsn() == insn && cgType.isAssignableFrom(cg.getClass())){
      return (T)cg;
    }

    return null;
  }

  public ChoiceGenerator<?> getNextChoiceGenerator () {
    return nextCg;
  }

  
  public boolean setNextChoiceGenerator (ChoiceGenerator<?> cg) {
    if (isIgnored){



      return false;
    }

    if (cg != null){



      if (ChoiceGeneratorBase.useRandomization()) {
        cg = cg.randomize();
      }


      cg.setContext(execThread);


      if (nextCg != null) {
        cg.setPreviousChoiceGenerator(nextCg);
        nextCg.setCascaded(); 

      } else {
        cg.setPreviousChoiceGenerator(curCg);
      }

      nextCg = cg;

      execThread.getVM().notifyChoiceGeneratorRegistered(cg, execThread); 
    }


    return (nextCg != null);
  }

  public void setMandatoryNextChoiceGenerator (ChoiceGenerator<?> cg, String failMsg){
    if (!setNextChoiceGenerator(cg)){
      throw new JPFException(failMsg);
    }
  }

  
  public void removeNextChoiceGenerator (){
    if (nextCg != null){
      nextCg = nextCg.getPreviousChoiceGenerator();
    }
  }

  
  public void removeAllNextChoiceGenerators(){
    while (nextCg != null){
      nextCg = nextCg.getPreviousChoiceGenerator();
    }
  }

  
  public Object getBacktrackData () {
    return new Memento(this);
  }

  public void backtrackTo (Object backtrackData) {
    ((Memento) backtrackData).backtrack( this);
  }

  public Object getRestoreData(){
    return new RestorableMemento(this);
  }
  
  public void restoreTo (Object backtrackData) {
    ((Memento) backtrackData).restore( this);
  }

  public void retainAttributes (boolean b){
    retainAttributes = b;
  }

  public boolean getRetainAttributes() {
    return retainAttributes;
  }

  
  public void setIgnored (boolean b) {
    isIgnored = b;

    if (b){
      isForced = false; 
    }
  }

  public boolean isIgnored () {
    return isIgnored;
  }

  public void setForced (boolean b){
    isForced = b;

    if (b){
      isIgnored = false; 
    }
  }

  public boolean isForced () {
    return isForced;
  }

  public void setInteresting (boolean b) {
    isInteresting = b;

    if (b){
      isBoring = false;
    }
  }

  public boolean isInteresting () {
    return isInteresting;
  }

  public void setBoring (boolean b) {
    isBoring = b;

    if (b){
      isInteresting = false;
    }
  }

  public boolean isBoring () {
    return isBoring;
  }

  public boolean isInitState () {
    return (id == StateSet.UNKNOWN_ID);
  }

  public int getThreadCount () {
    return ks.threads.length();
  }

  public UncaughtException getUncaughtException () {
    return uncaughtException;
  }

  public void activateGC () {
    GCNeeded = true;
  }

  public boolean hasRestorer (Object key){
    if (restorers != null){
      return restorers.containsKey(key);
    }
    
    return false;
  }
  
  public ClosedMemento getRestorer( Object key){
    if (restorers != null){
      return restorers.get(key);
    }
    
    return null;    
  }
  
  
  public void putRestorer (Object key, ClosedMemento restorer){
    if (restorers == null){
      restorers = new LinkedHashMap<Object,ClosedMemento>();
    }
    

    restorers.put(key,restorer);
  }
  
  public boolean gcIfNeeded () {
    boolean needed = false;
    if (GCNeeded) {
      ks.gc();
      GCNeeded = false;
      needed = true;
    }

    nAlloc = 0;
    return needed;
  }

  
  public void checkGC () {
    if (nAlloc++ > maxAllocGC){
      gcIfNeeded();
    }
  }


  void dumpThreadCG (ThreadChoiceGenerator cg) {
    PrintWriter pw = new PrintWriter(System.out, true);
    cg.printOn(pw);
    pw.flush();
  }

  
  public boolean initializeNextTransition(VM vm) {



    if (!retainAttributes){
      isIgnored = false;
      isForced = false;
      isInteresting = false;
      isBoring = false;
    }

    restorers = null;
    





    while (nextCg != null) {
      curCg = nextCg;
      nextCg = null;


      curCg.setCurrent();
      notifyChoiceGeneratorSet(vm, curCg);
    }

    assert (curCg != null) : "transition without choice generator";

    return advanceCurCg(vm);
  }

  
  public void executeNextTransition (VM vm){

    setExecThread( vm);

    assert execThread.isRunnable() : "next transition thread not runnable: " + execThread.getStateDescription();

    trail = new Transition(curCg, execThread);
    entryAtomicLevel = atomicLevel; 

    execThread.executeTransition(this);    
  }

  
  protected boolean extendTransition (){
    ChoiceGenerator<?> ncg = nextCg;
    if (ncg != null){
      if (CheckExtendTransition.isMarked(ncg) ||
              ((extendTransitions != null) && extendTransitions.matches(ncg.getClass()))){
        if (ncg.getTotalNumberOfChoices() == 1 && !ncg.isCascaded()){
          if (ncg instanceof ThreadChoiceGenerator){
            if ((ncg instanceof BreakGenerator) || !((ThreadChoiceGenerator) ncg).contains(execThread)){
              return false;
            }
          }

          initializeNextTransition(execThread.getVM());
          return true;
        }
      }
    }
    
    return false;
  }
  
  protected void setExecThread( VM vm){
    ThreadChoiceGenerator tcg = getCurrentSchedulingPoint();
    if (tcg != null){
      ThreadInfo tiNext = tcg.getNextChoice();
      if (tiNext != execThread) {
        vm.notifyThreadScheduled(tiNext);
        execThread = tiNext;
      }
    }

    if (execThread.isTimeoutWaiting()) {
      execThread.setTimedOut();
    }
  }



  protected int nAdvancedCGs;

  protected void advance( VM vm, ChoiceGenerator<?> cg){
    while (true) {
      if (cg.hasMoreChoices()){
        cg.advance();
        isIgnored = false;
        vm.notifyChoiceGeneratorAdvanced(cg);
        
        if (!isIgnored){




          if (cg.getNextChoice() != null){
            nAdvancedCGs++;
          }
          break;
        }
        
      } else {
        vm.notifyChoiceGeneratorProcessed(cg);
        break;
      }
    }
  }

  protected void advanceAllCascadedParents( VM vm, ChoiceGenerator<?> cg){
    ChoiceGenerator<?> parent = cg.getCascadedParent();
    if (parent != null){
      advanceAllCascadedParents(vm, parent);
    }
    advance(vm, cg);
  }

  protected boolean advanceCascadedParent (VM vm, ChoiceGenerator<?> cg){
    if (cg.hasMoreChoices()){
      advance(vm,cg);
      return true;

    } else {
      vm.notifyChoiceGeneratorProcessed(cg);

      ChoiceGenerator<?> parent = cg.getCascadedParent();
      if (parent != null){
        if (advanceCascadedParent(vm,parent)){
          cg.reset();
          advance(vm,cg);
          return true;
        }
      }
      return false;
    }
  }

  protected boolean advanceCurCg (VM vm){
    nAdvancedCGs = 0;

    ChoiceGenerator<?> cg = curCg;
    ChoiceGenerator<?> parent = cg.getCascadedParent();

    if (cg.hasMoreChoices()){

      if (parent != null && parent.getProcessedNumberOfChoices() == 0){
        advanceAllCascadedParents(vm,parent);
      }
      advance(vm, cg);

    } else { 
      vm.notifyChoiceGeneratorProcessed(cg);

      if (parent != null){
        if (advanceCascadedParent(vm,parent)){
          cg.reset();
          advance(vm,cg);
        }
      }
    }

    return (nAdvancedCGs > 0);
  }



  protected void notifyChoiceGeneratorSet (VM vm, ChoiceGenerator<?> cg){
    ChoiceGenerator<?> parent = cg.getCascadedParent();
    if (parent != null) {
      notifyChoiceGeneratorSet(vm, parent);
    }
    vm.notifyChoiceGeneratorSet(cg); 
  }



  public boolean breakTransition () {
    return ((nextCg != null) || isIgnored);
  }

  void recordExecutionStep (Instruction pc) {


    if (recordSteps) {
      Step step = new Step(pc);
      trail.addStep( step);
    } else {
      trail.incStepCount();
    }
  }




}

