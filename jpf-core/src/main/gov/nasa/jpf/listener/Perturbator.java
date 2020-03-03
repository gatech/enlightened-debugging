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



package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.JVMInstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.perturb.OperandPerturbator;
import gov.nasa.jpf.util.FieldSpec;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.util.SourceRef;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class Perturbator extends ListenerAdapter {

  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.Perturbator");

  public static class Perturbation {
    SourceRef sref;    
    Class<? extends ChoiceGenerator<?>> cgType; 
    OperandPerturbator perturbator;

    Perturbation (OperandPerturbator perturbator, String loc){
      this.perturbator = perturbator;

      if (loc != null){
        sref = new SourceRef(loc);
      }
    }
  }

  public static class FieldPerturbation extends Perturbation {
    FieldSpec fieldSpec;

    FieldPerturbation (FieldSpec fieldSpec, OperandPerturbator perturbator, String loc){
      super(perturbator, loc);

      this.fieldSpec = fieldSpec;
    }
  }

  public static class ReturnPerturbation extends Perturbation {
    MethodSpec mthSpec;

    ReturnPerturbation (MethodSpec mthSpec, OperandPerturbator perturbator, String loc){
      super(perturbator, loc);

      this.mthSpec = mthSpec;
    }
  }
  
  public static class ParamsPerturbation extends Perturbation {
  	public MethodSpec mthSpec;
  	
  	ParamsPerturbation (MethodSpec mthSpec, OperandPerturbator perturbator, String loc) {
  		super(perturbator, loc);
  		
  		this.mthSpec = mthSpec;
  	}
  }
  
  protected static Class<?>[] argTypes = { Config.class, String.class };

  protected List<FieldPerturbation> fieldWatchList = new ArrayList<FieldPerturbation>();
  protected HashMap<FieldInfo,FieldPerturbation> perturbedFields = new HashMap<FieldInfo,FieldPerturbation>();

  protected List<ReturnPerturbation> returnWatchList = new ArrayList<ReturnPerturbation>();
  protected HashMap<MethodInfo,ReturnPerturbation> perturbedReturns = new HashMap<MethodInfo,ReturnPerturbation>();
  
  protected List<ParamsPerturbation> paramsWatchList = new ArrayList<ParamsPerturbation>();
  protected HashMap<MethodInfo, ParamsPerturbation> perturbedParams = new HashMap<MethodInfo, ParamsPerturbation>();

  protected StackFrame savedFrame;
  
  public Perturbator (Config conf){






    String[] fieldIds = conf.getCompactTrimmedStringArray("perturb.fields");
    for (String id : fieldIds){
      addToFieldWatchList(conf, id);
    }

    String[] returnIds = conf.getCompactTrimmedStringArray("perturb.returns");
    for (String id : returnIds){
      addToReturnWatchList(conf, id);
    }
    
    String[] paramsIds = conf.getCompactTrimmedStringArray("perturb.params");
    for (String id: paramsIds) {
    	addToParamsWatchList(conf, id);
    }
  }
  
  public boolean isMethodWatched(Instruction insn, MethodInfo mi) {
    ParamsPerturbation e = perturbedParams.get(mi);
    if (e != null && isRelevantCallLocation(insn, e)){
      return true;
    }
    return false;
  }
  
  protected void addToFieldWatchList (Config conf, String id){
    String keyPrefix = "perturb." + id;

    String fs = conf.getString(keyPrefix + ".field");
    if (fs != null) {
      FieldSpec fieldSpec = FieldSpec.createFieldSpec(fs);
      if (fieldSpec != null){
        Object[] args = {conf, keyPrefix};
        OperandPerturbator perturbator = conf.getInstance(keyPrefix + ".class", OperandPerturbator.class, argTypes, args);
        if (perturbator != null) {
          String loc = conf.getString(keyPrefix + ".location");
          FieldPerturbation p = new FieldPerturbation(fieldSpec, perturbator, loc);
          fieldWatchList.add(p);
        } else {
          log.warning("invalid perturbator spec for ", keyPrefix);
        }
      } else {
        log.warning("malformed field specification for ", keyPrefix);
      }

    } else {
      log.warning("missing field specification for ", keyPrefix);
    }
  }

  protected void addToReturnWatchList (Config conf, String id){
    String keyPrefix = "perturb." + id;

    String ms = conf.getString(keyPrefix + ".method");
    if (ms != null) {
      MethodSpec mthSpec = MethodSpec.createMethodSpec(ms);
      if (mthSpec != null) {
        Object[] args = {conf, keyPrefix};
        OperandPerturbator perturbator = conf.getInstance(keyPrefix + ".class", OperandPerturbator.class, argTypes, args);
        if (perturbator != null) {
          String loc = conf.getString(keyPrefix + ".location");
          ReturnPerturbation p = new ReturnPerturbation(mthSpec, perturbator, loc);
          returnWatchList.add(p);
        } else {
          log.warning("invalid perturbator spec for ", keyPrefix);
        }

      } else {
        log.warning("malformed method specification for ", keyPrefix);
      }

    } else {
      log.warning("missing method specification for ", keyPrefix);
    }
  }

  protected void addToParamsWatchList (Config conf, String id){
    String keyPrefix = "perturb." + id;

    String ms = conf.getString(keyPrefix + ".method");
    if (ms != null) {
      MethodSpec mthSpec = MethodSpec.createMethodSpec(ms);
      if (mthSpec != null) {
        Object[] args = {conf, keyPrefix};
        OperandPerturbator perturbator = conf.getInstance(keyPrefix + ".class", OperandPerturbator.class, argTypes, args);        
        if (perturbator != null) {
          String loc = conf.getString(keyPrefix + ".location");
          ParamsPerturbation p = new ParamsPerturbation(mthSpec, perturbator, loc);
          paramsWatchList.add(p);
        } else {
          log.warning("invalid perturbator spec for ", keyPrefix);
        }

      } else {
        log.warning("malformed method specification for ", keyPrefix);
      }
    } else {
      log.warning("missing method specification for ", keyPrefix);
    }
  }

  @Override
  public void classLoaded (VM vm, ClassInfo loadedClass){






    String clsName = loadedClass.getName();

    for (FieldPerturbation p : fieldWatchList){
      FieldSpec fs = p.fieldSpec;
      if (fs.isMatchingType(loadedClass)){
        addFieldPerturbations( p, loadedClass, loadedClass.getDeclaredInstanceFields());
        addFieldPerturbations( p, loadedClass, loadedClass.getDeclaredStaticFields());
      }
    }

    for (ReturnPerturbation p : returnWatchList){
      MethodSpec ms = p.mthSpec;
      if (ms.isMatchingType(loadedClass)){
        for (MethodInfo mi : loadedClass.getDeclaredMethodInfos()){
          if (ms.matches(mi)){
            Class<? extends ChoiceGenerator<?>> returnCGType = mi.getReturnChoiceGeneratorType();
            Class<? extends ChoiceGenerator<?>> perturbatorCGType = p.perturbator.getChoiceGeneratorType();
            if (returnCGType.isAssignableFrom(perturbatorCGType)){
              p.cgType = returnCGType;
              perturbedReturns.put(mi, p);
            } else {
              log.warning("method " + mi + " not compatible with perturbator choice type " + perturbatorCGType.getName());
            }
          }
        }
      }
    }

    for (ParamsPerturbation p : paramsWatchList){
      MethodSpec ms = p.mthSpec;
      if (ms.isMatchingType(loadedClass)){
        for (MethodInfo mi : loadedClass.getDeclaredMethodInfos()){
          if (ms.matches(mi)){

            Class<? extends ChoiceGenerator<?>> perturbatorCGType = p.perturbator.getChoiceGeneratorType();
            p.cgType = perturbatorCGType;
          	perturbedParams.put(mi, p);
          }
        }
      }
    }
  }

  protected void addFieldPerturbations (FieldPerturbation p, ClassInfo ci, FieldInfo[] fieldInfos){
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
      if (p.fieldSpec.matches(fi)) {
        Class<? extends ChoiceGenerator<?>> fieldCGType = fi.getChoiceGeneratorType();
        Class<? extends ChoiceGenerator<?>> perturbatorCGType = p.perturbator.getChoiceGeneratorType();
        if (fieldCGType.isAssignableFrom(perturbatorCGType)) {
          p.cgType = fieldCGType;
          perturbedFields.put(fi, p);
        } else {
          log.warning("field " + fi + " not compatible with perturbator choice type " + perturbatorCGType.getName());
        }
      }
    }
  }

  protected boolean isRelevantCallLocation (ThreadInfo ti, Perturbation p){
    if (p.sref == null){

      return true;
    } else {
      StackFrame caller = ti.getCallerStackFrame();
      if (caller != null) {
        Instruction invokeInsn = caller.getPC();
        return p.sref.equals(invokeInsn.getFilePos());
      } else {
        return false;
      }
    }
  }
  
  protected boolean isRelevantCallLocation (Instruction invokeInsn, Perturbation p) {



  	if (p.sref == null)
  		return true;
  	else
  		return p.sref.equals(invokeInsn.getFilePos());
  }

  @Override
  public void executeInstruction (VM vm, ThreadInfo ti, Instruction insnToExecute){
    
    if (insnToExecute instanceof GETFIELD){
      FieldInfo fi = ((JVMInstanceFieldInstruction)insnToExecute).getFieldInfo();
      FieldPerturbation e = perturbedFields.get(fi);

      if (e != null) {  
        if (isMatchingInstructionLocation(e,insnToExecute)) {
          if (!ti.isFirstStepInsn()){


            savedFrame = ti.getTopFrame().clone();
          }
        }
      }

    } else if (insnToExecute instanceof JVMReturnInstruction){
      MethodInfo mi = insnToExecute.getMethodInfo();
      ReturnPerturbation e = perturbedReturns.get(mi);

      if (e != null && isRelevantCallLocation(ti, e)){
        SystemState ss = vm.getSystemState();

        if (!ti.isFirstStepInsn()){




          ChoiceGenerator<?> cg = e.perturbator.createChoiceGenerator("perturbReturn", ti.getTopFrame(), new Integer(0));
          if (ss.setNextChoiceGenerator(cg)){
            ti.skipInstruction(insnToExecute);
          }
        } else {

          ChoiceGenerator<?> cg = ss.getCurrentChoiceGenerator("perturbReturn", e.cgType);
          if (cg != null) {
            e.perturbator.perturb(cg, ti.getTopFrame());
          }
        }
      }
    } else if (insnToExecute instanceof JVMInvokeInstruction) {



    	MethodInfo mi = ((JVMInvokeInstruction) insnToExecute).getInvokedMethod();
    	ParamsPerturbation e = perturbedParams.get(mi);
    	
      if (e != null && isRelevantCallLocation(insnToExecute, e)){
        SystemState ss = vm.getSystemState();

        if (!ti.isFirstStepInsn()) {



          ChoiceGenerator<?> cg = e.perturbator.createChoiceGenerator(mi.getFullName(), ti.getTopFrame(), mi);


          if (cg != null) {
            log.info("--- Creating choice generator: " + mi.getFullName() + " for thread: " + ti);
            if (ss.setNextChoiceGenerator(cg)) {
              ti.skipInstruction(insnToExecute);
            }
          }
        } else {

          ChoiceGenerator<?> cg = ss.getChoiceGenerator(mi.getFullName());
          if (cg != null) {
            log.info("--- Using choice generator: " + mi.getFullName() + " in thread: " + ti);
            e.perturbator.perturb(cg, ti.getTopFrame());
          }
        }
      }
    }
  }

  @Override
  public void instructionExecuted(VM vm, ThreadInfo ti, Instruction nextInsn, Instruction executedInsn) {
    
    if (executedInsn instanceof GETFIELD){
      FieldInfo fi = ((JVMInstanceFieldInstruction)executedInsn).getFieldInfo();
      FieldPerturbation p = perturbedFields.get(fi);
      if (p != null){
        if (isMatchingInstructionLocation(p, executedInsn)) {  
          StackFrame frame = ti.getTopFrame();
          SystemState ss = vm.getSystemState();

          if (ti.isFirstStepInsn()) { 
            ChoiceGenerator<?> cg = ss.getCurrentChoiceGenerator( "perturbGetField", p.cgType);
            if (cg != null) {
              p.perturbator.perturb(cg, frame);
            } else {
              log.warning("wrong choice generator type ", cg);
            }

          } else { 
            ChoiceGenerator<?> cg = p.perturbator.createChoiceGenerator( "perturbGetField", frame, new Integer(0));
            if (ss.setNextChoiceGenerator(cg)){
              assert savedFrame != null;



              ti.setTopFrame(savedFrame);
              ti.setNextPC(executedInsn); 

              savedFrame = null;
            }
          }
        }
      }
    } 
  }
  
  protected boolean isMatchingInstructionLocation (Perturbation p, Instruction insn){
    return p.sref == null || p.sref.equals(insn.getFilePos());
  }
}
