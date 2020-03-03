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
import gov.nasa.jpf.SystemAttribute;
import gov.nasa.jpf.util.FieldSpecMatcher;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.MethodSpecMatcher;
import gov.nasa.jpf.util.TypeSpecMatcher;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;


public abstract class GenericSharednessPolicy implements SharednessPolicy, Attributor {
  

  static class NeverBreakIn implements SystemAttribute {
    static NeverBreakIn singleton = new NeverBreakIn();
  } 
  static class NeverBreakOn implements SystemAttribute {
    static NeverBreakOn singleton = new NeverBreakOn();
  } 
  static class AlwaysBreakOn implements SystemAttribute {
    static AlwaysBreakOn singleton = new AlwaysBreakOn();
  } 
  
  protected static JPFLogger logger = JPF.getLogger("shared");
  
  

  
  protected TypeSpecMatcher neverBreakOnTypes;
  
  protected TypeSpecMatcher alwaysBreakOnTypes;
  
  
  protected MethodSpecMatcher neverBreakInMethods;
  
    
  protected FieldSpecMatcher neverBreakOnFields;
    
    
  protected FieldSpecMatcher alwaysBreakOnFields;
  

  
  protected boolean skipFinals;
  protected boolean skipConstructedFinals;
  protected boolean skipStaticFinals;
  
  
  protected boolean skipInits;

  
  protected boolean breakOnExposure;
  
  
  protected boolean useSyncDetection;
  protected int lockThreshold;  
  
  protected VM vm;
  
  
  protected GenericSharednessPolicy (Config config){
    neverBreakInMethods = MethodSpecMatcher.create( config.getStringArray("vm.shared.never_break_methods"));
    
    neverBreakOnTypes = TypeSpecMatcher.create(config.getStringArray("vm.shared.never_break_types"));
    alwaysBreakOnTypes = TypeSpecMatcher.create(config.getStringArray("vm.shared.always_break_types"));
    
    neverBreakOnFields = FieldSpecMatcher.create( config.getStringArray("vm.shared.never_break_fields"));
    alwaysBreakOnFields = FieldSpecMatcher.create( config.getStringArray("vm.shared.always_break_fields"));
    
    skipFinals = config.getBoolean("vm.shared.skip_finals", true);
    skipConstructedFinals = config.getBoolean("vm.shared.skip_constructed_finals", false);
    skipStaticFinals = config.getBoolean("vm.shared.skip_static_finals", true);
    skipInits = config.getBoolean("vm.shared.skip_inits", true);
    
    breakOnExposure = config.getBoolean("vm.shared.break_on_exposure", true);
    
    useSyncDetection = config.getBoolean("vm.shared.sync_detection", true);
    lockThreshold = config.getInt("vm.shared.lockthreshold", 5);  
  }
  

  
  


  protected void setTypeAttributes (TypeSpecMatcher neverMatcher, TypeSpecMatcher alwaysMatcher, ClassInfo ciLoaded){

    for (ClassInfo ci = ciLoaded; ci!= null; ci = ci.getSuperClass()){
      if (alwaysMatcher != null && alwaysMatcher.matches(ci)){
        ciLoaded.addAttr(AlwaysBreakOn.singleton);
        return;
      }
      if (neverMatcher != null && neverMatcher.matches(ci)){
        ciLoaded.addAttr( NeverBreakOn.singleton);
        return;
      }
    }
  }
  
  protected void setFieldAttributes (FieldSpecMatcher neverMatcher, FieldSpecMatcher alwaysMatcher, ClassInfo ci){
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {

      if (fi.getName().startsWith("this$")) {
        fi.addAttr( NeverBreakOn.singleton);
        continue;
      }        


      if (neverMatcher != null && neverMatcher.matches(fi)) {
        fi.addAttr( NeverBreakOn.singleton);
      }
      if (alwaysMatcher != null && alwaysMatcher.matches(fi)) {
        fi.addAttr( AlwaysBreakOn.singleton);
      }
      

      if (fi.hasAnnotation("gov.nasa.jpf.annotation.NeverBreak")){
        fi.addAttr( NeverBreakOn.singleton);        
      }
    }

    for (FieldInfo fi : ci.getDeclaredStaticFields()) {

      if ("$assertionsDisabled".equals(fi.getName())) {
        fi.addAttr( NeverBreakOn.singleton);
        continue;
      }


      if (neverMatcher != null && neverMatcher.matches(fi)) {
        fi.addAttr( NeverBreakOn.singleton);
      }
      if (alwaysMatcher != null && alwaysMatcher.matches(fi)) {
        fi.addAttr( AlwaysBreakOn.singleton);
      }
      

      if (fi.hasAnnotation("gov.nasa.jpf.annotation.NeverBreak")){
        fi.addAttr( NeverBreakOn.singleton);        
      }
    }
  }
  
  protected boolean isInNeverBreakMethod (ThreadInfo ti){
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame=frame.getPrevious()){
      MethodInfo mi = frame.getMethodInfo();
      if (mi.hasAttr( NeverBreakIn.class)){
        return true;
      }
    }

    return false;
  }
  
  protected abstract boolean checkOtherRunnables (ThreadInfo ti);
  

  protected Boolean canHaveSharednessCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){

    if (ti.isFirstStepInsn()){ 
      return Boolean.FALSE;
    }
    
    if (!checkOtherRunnables(ti)){ 
      return Boolean.FALSE;
    }
    
    if (ti.hasAttr( NeverBreakIn.class)){
      return Boolean.FALSE;
    }
    

    if (isInNeverBreakMethod(ti)){
      return false;
    }
    

    ClassInfo ciFieldOwner = eiFieldOwner.getClassInfo();
    if (ciFieldOwner.hasAttr(NeverBreakOn.class)){
      return Boolean.FALSE;
    }
    if (ciFieldOwner.hasAttr(AlwaysBreakOn.class)){
      return Boolean.TRUE;
    }
    

    if (fi != null){
      if (fi.hasAttr(AlwaysBreakOn.class)) {
        return Boolean.TRUE;
      }
      if (fi.hasAttr(NeverBreakOn.class)) {
        return Boolean.FALSE;
      }
    }
    
    return null;    
  }


  
  
  protected abstract FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi);

  
    
  protected ElementInfo updateFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    FieldLockInfo fli = ei.getFieldLockInfo(fi);
    if (fli == null){
      fli = createFieldLockInfo(ti, ei, fi);
      ei = ei.getModifiableInstance();
      ei.setFieldLockInfo(fi, fli);
      
    } else {
      FieldLockInfo newFli = fli.checkProtection(ti, ei, fi);
      if (newFli != fli) {
        ei = ei.getModifiableInstance();
        ei.setFieldLockInfo(fi,newFli);
      }
    }
    
    return ei;
  }
  
  



  
  protected ThreadInfo[] getRunnables (ApplicationContext appCtx){
    return vm.getThreadList().getProcessTimeoutRunnables(appCtx);
  }
  
  protected ChoiceGenerator<ThreadInfo> getRunnableCG (String id, ThreadInfo tiCurrent){
    if (vm.getSystemState().isAtomic()){ 
      return null;
    }
    
    ThreadInfo[] choices = getRunnables(tiCurrent.getApplicationContext());
    if (choices.length <= 1){ 
      return null;
    }
    
    return new ThreadChoiceFromSet( id, choices, true);
  }
  
  protected boolean setNextChoiceGenerator (ChoiceGenerator<ThreadInfo> cg){
    if (cg != null){
      return vm.getSystemState().setNextChoiceGenerator(cg); 
    }
    
    return false;
  }
  
  

  
  protected ElementInfo updateSharedness (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    ThreadInfoSet tis = ei.getReferencingThreads();
    ThreadInfoSet newTis = tis.add(ti);
    
    if (tis != newTis){
      ei = ei.getModifiableInstance();
      ei.setReferencingThreads(newTis);
    }
      

    if (newTis.isShared(ti, ei) && !ei.isShared() && !ei.isSharednessFrozen()) {
      ei = ei.getModifiableInstance();
      ei.setShared(ti, true);
    }

    if (ei.isShared() && fi != null){
      ei = updateFieldLockInfo(ti,ei,fi);
    }
    
    return ei;
  }

  protected boolean setsExposureCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed){
    if (breakOnExposure){
      ClassInfo ciExposed = eiExposed.getClassInfo();
      

      if (ciExposed.hasAttr(NeverBreakOn.class)){
        return false;
      }      
      if (ciExposed.hasAttr(AlwaysBreakOn.class)){
        logger.info("type exposure CG setting field ", fi, " to ", eiExposed);
        return setNextChoiceGenerator(getRunnableCG("EXPOSE", ti));
      }        
        


      
      if (isInNeverBreakMethod(ti)){
        return false;
      }
      
      if (eiFieldOwner.isExposedOrShared() && isFirstExposure(eiFieldOwner, eiExposed)){        


        eiExposed = eiExposed.getExposedInstance(ti, eiFieldOwner);
        logger.info("exposure CG setting field ", fi, " to ", eiExposed);
        return setNextChoiceGenerator(getRunnableCG("EXPOSE", ti));
      }
    }

    return false;
  }

  protected boolean isFirstExposure (ElementInfo eiFieldOwner, ElementInfo eiExposed){
    if (!eiExposed.isImmutable()){
      if (!eiExposed.isExposedOrShared()) {
         return (eiFieldOwner.isExposedOrShared());
      }
    }
        
    return false;
  }

  

    
  
  @Override
  public void initializeSharednessPolicy (VM vm, ApplicationContext appCtx){
    this.vm = vm;
    
    SystemClassLoaderInfo sysCl = appCtx.getSystemClassLoader();
    sysCl.addAttributor(this);
  }
  
  
  @Override
  public void setAttributes (ClassInfo ci){
    setTypeAttributes( neverBreakOnTypes, alwaysBreakOnTypes, ci);
    
    setFieldAttributes( neverBreakOnFields, alwaysBreakOnFields, ci);
    

    if (neverBreakInMethods != null){
      for (MethodInfo mi : ci.getDeclaredMethods().values()){
        if (neverBreakInMethods.matches(mi)){
          mi.setAttr( NeverBreakIn.singleton);
        }
      }
    }
    
  }
    

  
  @Override
  public ElementInfo updateObjectSharedness (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    return updateSharedness(ti, ei, fi);
  }
  @Override
  public ElementInfo updateClassSharedness (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    return updateSharedness(ti, ei, fi);
  }
  @Override
  public ElementInfo updateArraySharedness (ThreadInfo ti, ElementInfo ei, int idx){

    return updateSharedness(ti, ei, null);
  }

  
  
  @Override
  public boolean canHaveSharedObjectCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    Boolean ret = canHaveSharednessCG( ti, insn, eiFieldOwner, fi);
    if (ret != null){
      return ret;
    }
    
    if  (eiFieldOwner.isImmutable()){
      return false;
    }
    
    if (skipFinals && fi.isFinal()){
      return false;
    }
        

    if (skipConstructedFinals && fi.isFinal() && eiFieldOwner.isConstructed()){
      return false;
    }
    
    if (skipInits && insn.getMethodInfo().isInit()){
      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean canHaveSharedClassCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    Boolean ret = canHaveSharednessCG( ti, insn, eiFieldOwner, fi);
    if (ret != null){
      return ret;
    }

    if  (eiFieldOwner.isImmutable()){
      return false;
    }
    
    if (skipStaticFinals && fi.isFinal()){
      return false;
    }


    MethodInfo mi = insn.getMethodInfo();
    if (mi.isClinit() && (fi.getClassInfo() == mi.getClassInfo())) {

      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean canHaveSharedArrayCG (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx){
    Boolean ret = canHaveSharednessCG( ti, insn, eiArray, null);
    if (ret != null){
      return ret;
    }


    
    return true;
  }
  
  
  
  @Override
  public boolean setsSharedObjectCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    if (eiFieldOwner.getClassInfo().hasAttr(AlwaysBreakOn.class) ||
            (eiFieldOwner.isShared() && !eiFieldOwner.isLockProtected(fi))) {
      logger.info("CG accessing shared instance field ", fi);
      return setNextChoiceGenerator( getRunnableCG("SHARED_OBJECT", ti));
    }
    
    return false;
  }

  @Override
  public boolean setsSharedClassCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    if (eiFieldOwner.getClassInfo().hasAttr(AlwaysBreakOn.class) ||
            (eiFieldOwner.isShared() && !eiFieldOwner.isLockProtected(fi))) {
      logger.info("CG accessing shared static field ", fi);
      return setNextChoiceGenerator( getRunnableCG("SHARED_CLASS", ti));
    }
    
    return false;
  }
  
  @Override
  public boolean setsSharedArrayCG (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int index){
    if (eiArray.isShared()){

      logger.info("CG accessing shared array ", eiArray);
      return setNextChoiceGenerator( getRunnableCG("SHARED_ARRAY", ti));
    }
    
    return false;
  }

  

    
  protected boolean isRelevantStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, FieldInfo fi){
    if (!ei.isShared()){
      return false;
    }
    
    if  (ei.isImmutable()){
      return false;
    }
    
    if (skipStaticFinals && fi.isFinal()){
      return false;
    }    
    
    if (!ti.hasOtherRunnables()){ 
      return false;
    }


    MethodInfo mi = insn.getMethodInfo();
    if (mi.isClinit() && (fi.getClassInfo() == mi.getClassInfo())) {

      return false;
    }
    
    return true;
  }

  
  protected boolean isRelevantArrayAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, int index){

    
    if (!ti.hasOtherRunnables()){
      return false;
    }
    
    if (!ei.isShared()){
      return false;
    }
    
    if (ti.isFirstStepInsn()){ 
      return false;
    }

    return true;
  }
  



  
  @Override
  public boolean setsSharedObjectExposureCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed){
    return setsExposureCG(ti,insn,eiFieldOwner,fi,eiExposed);
  }

  @Override
  public boolean setsSharedClassExposureCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed){
    return setsExposureCG(ti,insn,eiFieldOwner,fi,eiExposed);
  }  


  
  
  @Override
  public void cleanupThreadTermination(ThreadInfo ti) {

  }

}
