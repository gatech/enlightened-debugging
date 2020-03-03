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

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.ThreadInfo;

import java.io.PrintWriter;
import java.io.StringWriter;

import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;



public class JPF_java_lang_Throwable extends NativePeer {    
  
  @MJI
  public int createStackTrace_____3Ljava_lang_StackTraceElement_2 (MJIEnv env, int objref) {
    int aref = env.getReferenceField(objref, "snapshot");
    int[] snap = env.getIntArrayObject(aref);
    
    return env.getThreadInfo().createStackTraceElements(snap);
  }
  
  @MJI
  public int fillInStackTrace____Ljava_lang_Throwable_2 (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfo();
    int[] snap = ti.getSnapshot(objref);
    
    int aref = env.newIntArray(snap);
    env.setReferenceField(objref, "snapshot", aref);
    
    return objref;
  }
    

  @MJI
  public void printStackTrace____V (MJIEnv env, int objRef) {
    env.getThreadInfo().printStackTrace(objRef);
  }
  

  @MJI
  public int getStackTraceAsString____Ljava_lang_String_2 (MJIEnv env, int objRef) {
    ThreadInfo ti = env.getThreadInfo();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    
    ti.printStackTrace(pw, objRef);
    String stackTrace = sw.toString();
    pw.close();
    
    return env.newString(stackTrace);
  }
  
  @MJI
  public int toString____Ljava_lang_String_2 (MJIEnv env, int objRef){
  	env.setDependencyTracked(true);
  	
  	ElementInfo exObj = env.getElementInfo(objRef);
    ClassInfo ci = exObj.getClassInfo();
    DynamicDependency exDep = null;
    Object[] argAttrs = env.getArgAttributes();
    if (argAttrs != null && argAttrs.length >= 1) {
    	exDep = (DynamicDependency) argAttrs[0];
    }
    
    FieldInfo msgField = exObj.getFieldInfo("detailMessage");
    int msgRef = exObj.getReferenceField(msgField);
    ElementInfo msgObj = env.getElementInfo(msgRef);
    DynamicDependency msgDep = (DynamicDependency) exObj.getFieldAttr(msgField);
    
    ElementInfo valueArrayObj = null;
    DynamicDependency valueArrayDep = null;
    if (msgObj != null) {
	    FieldInfo valueArrayField = msgObj.getFieldInfo("value");
	    int valueArrayRef = msgObj.getReferenceField(valueArrayField);
	    valueArrayObj = env.getElementInfo(valueArrayRef);
	    valueArrayDep = (DynamicDependency) msgObj.getFieldAttr(valueArrayField);
	    		
    }
    
    String s = ci.getName();
    int classNameLength = s.length();
    if (msgObj != null){
      s += ": " + msgObj.asString();
    }
    int retStrRef = env.newString(s);
    ElementInfo retStrObj = env.getElementInfo(retStrRef);
    FieldInfo retStrValueArrayField = retStrObj.getFieldInfo("value");
    int retStrValueArrayRef = retStrObj.getReferenceField(retStrValueArrayField);
    ElementInfo retStrValueArrayObj = env.getElementInfo(retStrValueArrayRef);
    
    DynamicDependency invocationCondition = env.getInvocationCondition();
    DynamicDependency retStrDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(exDep, msgDep).setControlDependency(invocationCondition).build();
    env.setReturnAttribute(retStrDep);
    DynamicDependency retValueArrayDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(exDep, msgDep, valueArrayDep)
    		.setControlDependency(invocationCondition).build();
    retStrObj.setFieldAttr(retStrValueArrayField, retValueArrayDep);
    
    for (int i = 0; i < retStrValueArrayObj.arrayLength(); ++i) {
    	if (i < classNameLength + 2) {
    		retStrValueArrayObj.setElementAttr(i, retStrDep);
    	} else {
    		int originalMsgOffset = i - classNameLength - 2;
    		DynamicDependency contentDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(valueArrayObj.getElementAttr(originalMsgOffset), retValueArrayDep)
    				.build();
    		retStrValueArrayObj.setElementAttr(i, contentDep);
    	}
    }
    return retStrRef;
  }
}
