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

import java.io.PrintWriter;

import anonymous.domain.enlighten.deptrack.DynamicDependency;


public class ExceptionInfo {
  ElementInfo  ei;
  DynamicDependency exceptionDependency;
  ThreadInfo ti;
  
  ExceptionInfo (ThreadInfo xThread, ElementInfo xEi) {
    this(xThread, xEi, null);
  }
  
  ExceptionInfo(ThreadInfo xThread, ElementInfo xEi, DynamicDependency xDep) {
  	ti = xThread;
  	ei = xEi;
  	exceptionDependency = xDep;
  }
  
  public ElementInfo getException() {
    return ei;
  }
  
  public DynamicDependency getDependency() {
  	return exceptionDependency;
  }
  
  public int getExceptionReference () {
    return ei.getObjectRef();
  }
  
  public String getExceptionClassname() {
    return ei.getClassInfo().getName();
  }
  
  public String getDetails() {
    StringBuilder sb = new StringBuilder();
    sb.append(getExceptionClassname());
    
    int msgRef = ei.getReferenceField("detailMessage");
    if (msgRef != MJIEnv.NULL){
      ElementInfo eiMsg = ti.getElementInfo(msgRef);
      sb.append(" : ");
      sb.append(eiMsg.asString());
    }
      
    return sb.toString();
  }
  
  public String getCauseClassname() {
    int causeRef = ei.getReferenceField("cause");
    if (causeRef != MJIEnv.NULL){
      ElementInfo eiCause = ti.getElementInfo(causeRef);
      return eiCause.getClassInfo().getName();
    }
    
    return null;
  }
  public String getCauseDetails() {
    int causeRef = ei.getReferenceField("cause");
    if (causeRef != MJIEnv.NULL){
      ElementInfo eiCause = ti.getElementInfo(causeRef);
      int msgRef = eiCause.getReferenceField("detailMessage");
      if (msgRef != MJIEnv.NULL){
        ElementInfo eiMsg = ti.getElementInfo(msgRef);
        return eiMsg.asString();
      }
    }

    return null;
  }

  
  public ThreadInfo getThread() {
    return ti;
  }
  
  public void printOn (PrintWriter pw){
    ti.printStackTrace(pw, ei.getObjectRef());
  }
}
