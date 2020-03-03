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

import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.Iterator;



public abstract class LongReturn extends JVMReturnInstruction {

  protected long ret;
  
  @Override
  public int getReturnTypeSize() {
    return 2;
  }
  
  @Override
  protected Object getReturnedOperandAttr (StackFrame frame) {
    return frame.getLongOperandAttr();
  }
  
  @Override
  protected void getAndSaveReturnValue (StackFrame frame) {
    ret = frame.popLong();
  }

  @Override
  protected void pushReturnValue (StackFrame frame) {
    frame.pushLong(ret);
  }


  
  @Override
  public boolean hasReturnAttr (ThreadInfo ti){
    StackFrame frame = ti.getTopFrame();
    return frame.hasLongOperandAttr();
  }
  @Override
  public boolean hasReturnAttr (ThreadInfo ti, Class<?> type){
    StackFrame frame = ti.getTopFrame();
    return frame.hasLongOperandAttr(type);
  }
  
  
  @Override
  public Object getReturnAttr (ThreadInfo ti){
    StackFrame frame = ti.getTopFrame();
    return frame.getLongOperandAttr();
  }
  
  
  @Override
  public void setReturnAttr (ThreadInfo ti, Object a){
    StackFrame frame = ti.getModifiableTopFrame();
    frame.setLongOperandAttr(a);
  }

  
  @Override
  public <T> T getReturnAttr (ThreadInfo ti, Class<T> type){
    StackFrame frame = ti.getTopFrame();
    return frame.getLongOperandAttr(type);
  }
  @Override
  public <T> T getNextReturnAttr (ThreadInfo ti, Class<T> type, Object prev){
    StackFrame frame = ti.getTopFrame();
    return frame.getNextLongOperandAttr(type, prev);
  }
  @Override
  public Iterator returnAttrIterator (ThreadInfo ti){
    StackFrame frame = ti.getTopFrame();
    return frame.longOperandAttrIterator();
  }
  @Override
  public <T> Iterator<T> returnAttrIterator (ThreadInfo ti, Class<T> type){
    StackFrame frame = ti.getTopFrame();
    return frame.longOperandAttrIterator(type);
  }
  
  @Override
  public void addReturnAttr (ThreadInfo ti, Object attr){
    StackFrame frame = ti.getModifiableTopFrame();
    frame.addLongOperandAttr(attr);
  }


}
