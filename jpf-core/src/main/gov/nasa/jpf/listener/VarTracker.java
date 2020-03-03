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
import gov.nasa.jpf.jvm.bytecode.ALOAD;
import gov.nasa.jpf.jvm.bytecode.ArrayStoreInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.vm.bytecode.ReadInstruction;
import gov.nasa.jpf.vm.bytecode.StoreInstruction;
import gov.nasa.jpf.vm.bytecode.LocalVariableInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.util.StringSetMatcher;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.WriteInstruction;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;



public class VarTracker extends ListenerAdapter {


  StringSetMatcher includeVars = null; 
  StringSetMatcher excludeVars = null; 


  MethodSpec methodSpec;

  int maxVars; 
  
  ArrayList<VarChange> queue = new ArrayList<VarChange>();
  ThreadInfo lastThread;
  HashMap<String, VarStat> stat = new HashMap<String, VarStat>();
  int nStates = 0;
  int maxDepth;


  public VarTracker (Config config, JPF jpf){

    includeVars = StringSetMatcher.getNonEmpty(config.getStringArray("vt.include"));
    excludeVars = StringSetMatcher.getNonEmpty(config.getStringArray("vt.exclude",
            new String[] {"java.*", "javax.*"} ));

    maxVars = config.getInt("vt.max_vars", 25);

    methodSpec = MethodSpec.createMethodSpec(config.getString("vt.methods", "!java.*.*"));

    jpf.addPublisherExtension(ConsolePublisher.class, this);
  }

  @Override
  public void publishPropertyViolation (Publisher publisher) {
    PrintWriter pw = publisher.getOut();
    publisher.publishTopicStart("field access ");

    report(pw);
  }

  void print (PrintWriter pw, int n, int length) {
    String s = Integer.toString(n);
    int l = length - s.length();
    
    for (int i=0; i<l; i++) {
      pw.print(' ');
    }
    
    pw.print(s);
  }
  
  void report (PrintWriter pw) {
    pw.println();
    pw.println("      change    variable");
    pw.println("---------------------------------------");
    
    Collection<VarStat> values = stat.values();
    List<VarStat> valueList = new ArrayList<VarStat>();
    valueList.addAll(values);
    Collections.sort(valueList);

    int n = 0;
    for (VarStat s : valueList) {
      
      if (n++ > maxVars) {
        break;
      }
      
      print(pw, s.nChanges, 12);
      pw.print("    ");
      pw.println(s.id);
    }
  }
  
  @Override
  public void stateAdvanced(Search search) {
    
    if (search.isNewState()) { 
      int stateId = search.getStateId();
      nStates++;
      int depth = search.getDepth();
      if (depth > maxDepth) maxDepth = depth;
      
      if (!queue.isEmpty()) {
        for (Iterator<VarChange> it = queue.iterator(); it.hasNext(); ){
          VarChange change = it.next();
            String id = change.getVariableId();
            VarStat s = stat.get(id);
            if (s == null) {
              s = new VarStat(id, stateId);
              stat.put(id, s);
            } else {

              if (s.lastState != stateId) { 
                s.nChanges++;
                s.lastState = stateId;
              }
            }
        }
      }
    }

    queue.clear();
  }



  @Override
  public void instructionExecuted(VM vm, ThreadInfo ti, Instruction nextInsn, Instruction executedInsn) {
    String varId;

    if (executedInsn instanceof ALOAD) {



      StackFrame frame = ti.getTopFrame();
      int objRef = frame.peek();
      if (objRef != MJIEnv.NULL) {
        ElementInfo ei = ti.getElementInfo(objRef);
        if (ei.isArray()) {
          varId = ((LocalVariableInstruction) executedInsn).getVariableId();




          frame = ti.getModifiableTopFrame();
          frame.addOperandAttr(varId);
        }
      }

    } else if ((executedInsn instanceof ReadInstruction) && ((JVMFieldInstruction)executedInsn).isReferenceField()){
      varId = ((JVMFieldInstruction)executedInsn).getFieldName();

      StackFrame frame = ti.getModifiableTopFrame();
      frame.addOperandAttr(varId);







  } else if (executedInsn instanceof StoreInstruction) {
      if (executedInsn instanceof ArrayStoreInstruction) {



        Object attr = ((ArrayStoreInstruction)executedInsn).getArrayOperandAttr(ti);
        if (attr != null) {
          varId = attr + "[]";
        } else {
          varId = "?[]";
        }
      } else {
        varId = ((LocalVariableInstruction)executedInsn).getVariableId();
      }
      queueIfRelevant(ti, executedInsn, varId);

    } else if (executedInsn instanceof WriteInstruction){
      varId = ((WriteInstruction) executedInsn).getFieldInfo().getFullName();
      queueIfRelevant(ti, executedInsn, varId);
    }
  }

  void queueIfRelevant(ThreadInfo ti, Instruction insn, String varId){
    if (isMethodRelevant(insn.getMethodInfo()) && isVarRelevant(varId)) {
      queue.add(new VarChange(varId));
      lastThread = ti;
    }
  }

  boolean isMethodRelevant (MethodInfo mi){
    return methodSpec.matches(mi);
  }

  boolean isVarRelevant (String varId) {
    if (!StringSetMatcher.isMatch(varId, includeVars, excludeVars)){
      return false;
    }
    



    for (int i=0; i<queue.size(); i++) {
      VarChange change = queue.get(i);
      if (change.getVariableId().equals(varId)) {
        return false;
      }
    }
    
    return true;
  }
}


class VarStat implements Comparable<VarStat> {
  String id;               
  int nChanges;
  
  int lastState;           
  


  
  VarStat (String varId, int stateId) {
    id = varId;
    nChanges = 1;
    
    lastState = stateId;
  }
  
  int getChangeCount () {
    return nChanges;
  }
  
  @Override
  public int compareTo (VarStat other) {
    if (other.nChanges > nChanges) {
      return 1;
    } else if (other.nChanges == nChanges) {
      return 0;
    } else {
      return -1;
    }
  }
}


class VarChange {
  String id;
  
  VarChange (String varId) {
    id = varId;
  }
  
  String getVariableId () {
    return id;
  }
}
