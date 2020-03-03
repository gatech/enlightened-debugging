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

import gov.nasa.jpf.SystemAttribute;


public abstract class DirectCallStackFrame extends StackFrame implements SystemAttribute {
  
  MethodInfo callee;

  protected DirectCallStackFrame (MethodInfo miDirectCall, MethodInfo callee, int maxLocals, int maxStack){
    super( miDirectCall, maxLocals, maxStack);    
    this.callee = callee;
  }
  
  protected DirectCallStackFrame (MethodInfo miDirectCall, MethodInfo callee){
    super( miDirectCall, miDirectCall.getMaxLocals(), miDirectCall.getMaxStack());
    this.callee = callee;
  }
  
  public MethodInfo getCallee (){
    return callee;
  }
  
  @Override
  public String getStackTraceInfo () {
    StringBuilder sb = new StringBuilder(128);
    sb.append('[');
    sb.append( callee.getUniqueName());
    sb.append(']');
    return sb.toString();
  }
  
  public DirectCallStackFrame getPreviousDirectCallStackFrame(){
    StackFrame f = prev;
    while (f != null && !(f instanceof DirectCallStackFrame)){
      f = f.prev;
    }
    
    return (DirectCallStackFrame) f;
  }
  
  public void setFireWall(){
    mi.setFirewall(true);
  }

  @Override
  public boolean isDirectCallFrame () {
    return true;
  }
  
  @Override
  public boolean isSynthetic() {
    return true;
  }
  
  
  
  public abstract int setArgument (int argOffset, int value, Object attr);
  public abstract int setLongArgument (int argOffset, long value, Object attr);
  public abstract int setReferenceArgument (int argOffset, int ref, Object attr);

  public int setFloatArgument (int argOffset, float value, Object attr){
    return setArgument( argOffset, Float.floatToIntBits(value), attr);
  }
  public int setDoubleArgument (int argOffset, double value, Object attr){
    return setLongArgument( argOffset, Double.doubleToLongBits(value), attr);
  }
  
}
