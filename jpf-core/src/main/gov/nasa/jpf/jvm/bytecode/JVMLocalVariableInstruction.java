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


package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.bytecode.LocalVariableInstruction;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;


public abstract class JVMLocalVariableInstruction extends DependencyTrackingInstruction implements JVMInstruction, LocalVariableInstruction {

  protected int index;
  protected LocalVarInfo lv;


  protected JVMLocalVariableInstruction(int index){
    this.index = index;
  }
  
  @Override
  public int getLocalVariableSlot(){
    return index;
  }

  public int getLocalVariableIndex() {
    return index;
  }
  
  @Override
  public LocalVarInfo getLocalVarInfo(){
    if (lv == null){
     lv = mi.getLocalVar(index, position+getLength());
    }
    return lv;
  }
  
  public String getLocalVariableName () {
    LocalVarInfo lv = getLocalVarInfo();
    return (lv == null) ? Integer.toString(index) : lv.getName();
  }
  
  public String getLocalVariableType () {
    LocalVarInfo lv = getLocalVarInfo();
    return (lv == null) ? "?" : lv.getType();
  }
  
  
  @Override
  public String getVariableId () {
    return mi.getClassInfo().getName() + '.' + mi.getUniqueName() + '.' + getLocalVariableName();
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
  
  public abstract String getBaseMnemonic();
  
  @Override
  public String getMnemonic(){
    String baseMnemonic = getBaseMnemonic();
    
    if (index <= 3){
      return baseMnemonic + '_' + index;
    } else {
      return baseMnemonic;
    }
  }
  
  @Override
  public String toString(){
    String baseMnemonic = getBaseMnemonic();
    
    if (index <= 3){
      return baseMnemonic + '_' + index;
    } else {
      return baseMnemonic + " " + index;
    }
  }
}


