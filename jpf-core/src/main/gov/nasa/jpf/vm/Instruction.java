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

import gov.nasa.jpf.util.ObjectList;
import gov.nasa.jpf.util.Source;
import gov.nasa.jpf.vm.bytecode.InstructionInterface;




public abstract class Instruction implements Cloneable, InstructionInterface {

  protected int insnIndex;        
  protected int position;     
  protected MethodInfo mi;    


  protected Object attr;
  

  @Override
  public Instruction asInstruction(){
    return this;
  }
  

  public void setContext(String className, String methodName, int lineNumber,
          int offset) {
  }

  
  @Override
  public boolean isFirstInstruction() {
    return (insnIndex == 0);
  }


  
  @Override
  public boolean isBackJump() {
    return false;
  }

  
  public boolean isMonitorEnterPrologue(){
    return false;
  }

  
  @Override
  public boolean isExtendedInstruction() {
    return false;
  }


  @Override
  public MethodInfo getMethodInfo() {
    return mi;
  }


  
  public void setMethodInfo(MethodInfo mi) {
    this.mi = mi;
  }

  
  @Override
  public Instruction getNext() {
    return mi.getInstruction(insnIndex + 1);
  }

  @Override
  public int getInstructionIndex() {
    return insnIndex;
  }

  @Override
  public int getPosition() {
    return position;
  }

  public void setLocation(int insnIdx, int pos) {
    insnIndex = insnIdx;
    position = pos;
  }

  
  @Override
  public int getLength() {
    return 1;
  }

  @Override
  public Instruction getPrev() {
    if (insnIndex > 0) {
      return mi.getInstruction(insnIndex - 1);
    } else {
      return null;
    }
  }

  
  @Override
  public boolean isCompleted(ThreadInfo ti) {
    Instruction nextPc = ti.getNextPC();

    if (nextPc == null) {
      return ti.isTerminated();

    } else {

      return (nextPc != this) && (ti.getStackFrameExecuting(this, 1) == null);
    }


  }

  
  public void cleanupTransients(){

  }
  
  public boolean isSchedulingRelevant(SystemState ss, KernelState ks, ThreadInfo ti) {
    return false;
  }

  
  @Override
  public abstract Instruction execute(ThreadInfo ti);

  @Override
  public String toString() {
    return getMnemonic();
  }

  
  @Override
  public String toPostExecString(){
    return toString();
  }
  
  @Override
  public String getMnemonic() {
    String s = getClass().getSimpleName();
    return s.toLowerCase();
  }

  @Override
  public int getLineNumber() {
    return mi.getLineNumber(this);
  }

  @Override
  public String getSourceLine() {
    ClassInfo ci = mi.getClassInfo();
    if (ci != null) {
      int line = mi.getLineNumber(this);
      String fileName = ci.getSourceFileName();

      Source src = Source.getSource(fileName);
      if (src != null) {
        String srcLine = src.getLine(line);
        if (srcLine != null) {
          return srcLine;
        }
      }
    }
    
    return null;
  }

  
  public String getSourceOrLocation(){
    ClassInfo ci = mi.getClassInfo();
    if (ci != null) {
      int line = mi.getLineNumber(this);
      String file = ci.getSourceFileName();

      Source src = Source.getSource(file);
      if (src != null) {
        String srcLine = src.getLine(line);
        if (srcLine != null) {
          return srcLine;
        }
      }

      return "(" + file + ':' + line + ')'; 

    } else {
      return "[synthetic] " + mi.getName();
    }
  }
  
  
  
  @Override
  public String getFileLocation() {
    ClassInfo ci = mi.getClassInfo();
    if (ci != null) {
      int line = mi.getLineNumber(this);
      String fname = ci.getSourceFileName();
      return (fname + ':' + line);
    } else {
      return "[synthetic] " + mi.getName();
    }
  }

  
  @Override
  public String getFilePos() {
    String file = null;
    int line = -1;
    ClassInfo ci = mi.getClassInfo();

    if (ci != null){
      line = mi.getLineNumber(this);
      file = ci.getSourceFileName();
      if (file != null){
        int i = file.lastIndexOf('/'); 
        if (i >= 0) {
          file = file.substring(i + 1);
        }
      }
    }

    if (file != null) {
      if (line != -1){
        return (file + ':' + line);
      } else {
        return file;
      }
    } else {
      return ("pc " + position);
    }
  }

  
  @Override
  public String getSourceLocation() {
    ClassInfo ci = mi.getClassInfo();

    if (ci != null) {
      String s = ci.getName() + '.' + mi.getName() +
              '(' + getFilePos() + ')';
      return s;

    } else {
      return null;
    }
  }

  public void init(MethodInfo mi, int offset, int position) {
    this.mi = mi;
    this.insnIndex = offset;
    this.position = position;
  }

  
  public boolean requiresClinitExecution(ThreadInfo ti, ClassInfo ci) {
    return ci.initializeClass(ti);
  }

  
  @Override
  public Instruction getNext (ThreadInfo ti) {
    return ti.getPC().getNext();
  }

  


  @Override
  public boolean hasAttr () {
    return (attr != null);
  }

  @Override
  public boolean hasAttr (Class<?> attrType){
    return ObjectList.containsType(attr, attrType);
  }

  
  @Override
  public Object getAttr(){
    return attr;
  }

  
  @Override
  public void setAttr (Object a){
    attr = ObjectList.set(attr, a);    
  }

  @Override
  public void addAttr (Object a){
    attr = ObjectList.add(attr, a);
  }

  @Override
  public void removeAttr (Object a){
    attr = ObjectList.remove(attr, a);
  }

  @Override
  public void replaceAttr (Object oldAttr, Object newAttr){
    attr = ObjectList.replace(attr, oldAttr, newAttr);
  }

  
  @Override
  public <T> T getAttr (Class<T> attrType) {
    return ObjectList.getFirst(attr, attrType);
  }

  @Override
  public <T> T getNextAttr (Class<T> attrType, Object prev) {
    return ObjectList.getNext(attr, attrType, prev);
  }

  @Override
  public ObjectList.Iterator attrIterator(){
    return ObjectList.iterator(attr);
  }
  
  @Override
  public <T> ObjectList.TypedIterator<T> attrIterator(Class<T> attrType){
    return ObjectList.typedIterator(attr, attrType);
  }



  
  public Instruction typeSafeClone(MethodInfo mi) {
    Instruction clone = null;

    try {
      clone = (Instruction) super.clone();


      clone.mi = mi;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return clone;
  }
}
