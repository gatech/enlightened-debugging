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

import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.ObjectProperty;
import gov.nasa.jpf.vm.ClassChangeException;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;



public abstract class VirtualInvocation extends InstanceInvocation {




  
  ClassInfo lastCalleeCi; 

  protected VirtualInvocation () {}

  protected VirtualInvocation (String clsDescriptor, String methodName, String signature){
    super(clsDescriptor, methodName, signature);
  }

  @Override
  public String toPostExecString(){
    StringBuilder sb = new StringBuilder();
    sb.append(getMnemonic());
    sb.append(' ');
    
    if (invokedMethod != null){
      sb.append( lastCalleeCi.getName());
      sb.append('@');
      sb.append(Integer.toHexString(lastObj));
      sb.append('.');
      sb.append(invokedMethod.getUniqueName());

      if (invokedMethod.isMJI()){
        sb.append(" [native]");
      }
      
    } else { 
      if (lastCalleeCi != null){
        sb.append( lastCalleeCi.getName());
      } else {
        sb.append(cname);
      }
      sb.append('@');
      if (lastObj == MJIEnv.NULL){
        sb.append("<null>");
      } else {
        sb.append(Integer.toHexString(lastObj));
      }
      sb.append('.');
      sb.append(mname);
      sb.append(signature);
      sb.append(" (?)");
    }
    
    return sb.toString();
  }
  
  @Override
  public Instruction execute (ThreadInfo ti) {
  	int argSize = getArgSize();
    int objRef = ti.getCalleeThis(argSize);
    Object objRefAttr = ti.getTopFrame().getOperandAttr(argSize - 1);
    MethodInfo callee;

    if (objRef == MJIEnv.NULL) {
      lastObj = MJIEnv.NULL;
      DynamicDependency exDep = DynDepBuilder.newBuilder()
      		.appendDataDependency(objRefAttr, getInstructionDepSource(ti))
      		.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException(
      		"java.lang.NullPointerException", exDep);
    }

    ElementInfo ei = ti.getElementInfo(objRef);
    
    DynamicDependency objTypeDep = null;
    ObjectProperty objPropAttr = (ObjectProperty) ei.getObjectAttr();
    if (objPropAttr != null) {
    	objTypeDep = objPropAttr.getTypeDependency();
    }
    
    try {
      callee = getInvokedMethod(ti, objRef);
    } catch (ClassChangeException ccx){
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(objRefAttr, objTypeDep, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException(
      		"java.lang.IncompatibleClassChangeError", ccx.getMessage(), exDep);
    }
    
    if (callee == null) {
      String clsName = ti.getClassInfo(objRef).getName();
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(objRefAttr, objTypeDep, getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException(
      		"java.lang.NoSuchMethodError", clsName + '.' + mname, exDep);
    } else {
      if (callee.isAbstract()){
      	DynamicDependency exDep = DynDepBuilder.newBuilder()
      			.appendDataDependency(objRefAttr, objTypeDep, getInstructionDepSource(ti))
      			.setControlDependency(getControlDependencyCondition(ti)).build();
        return ti.createAndThrowException(
        		"java.lang.AbstractMethodError", callee.getFullName() + ", object: " + ei, exDep);
      }
    }

    if (callee.isSynchronized()) {
      ei = ti.getScheduler().updateObjectSharedness(ti, ei, null); 
      if (reschedulesLockAcquisition(ti, ei)){
        return this;
      }
    }

    setupCallee( ti, callee); 

    return ti.getPC(); 
  }
  
  
  protected boolean isLockOwner(ThreadInfo ti, ElementInfo ei) {
    return ei.getLockingThread() == ti;
  }

  
  protected boolean isLastUnlock(ElementInfo ei) {
    return ei.getLockCount() == 1;
  }


  @Override
  public MethodInfo getInvokedMethod(ThreadInfo ti){
    int objRef;

    if (ti.getNextPC() == null){ 
      objRef = ti.getCalleeThis(getArgSize());
    } else {                     
      objRef = lastObj;
    }

    return getInvokedMethod(ti, objRef);
  }

  public MethodInfo getInvokedMethod (ThreadInfo ti, int objRef) {

    if (objRef != MJIEnv.NULL) {
      lastObj = objRef;

      ClassInfo cci = ti.getClassInfo(objRef);

      if (lastCalleeCi != cci) { 
        lastCalleeCi = cci;
        invokedMethod = cci.getMethod(mname, true);

        if (invokedMethod == null) {
          invokedMethod = cci.getDefaultMethod(mname);
                    
          if (invokedMethod == null){
            lastObj = MJIEnv.NULL;
            lastCalleeCi = null;
          }
        }
      }

    } else {
      lastObj = MJIEnv.NULL;
      lastCalleeCi = null;
      invokedMethod = null;
    }

    return invokedMethod;
  }

  @Override
  public Object getFieldValue (String id, ThreadInfo ti){
    int objRef = getCalleeThis(ti);
    ElementInfo ei = ti.getElementInfo(objRef);

    Object v = ei.getFieldValueObject(id);

    if (v == null){ 
      v = ei.getClassInfo().getStaticFieldValueObject(id);
    }

    return v;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  @Override
  public Instruction typeSafeClone(MethodInfo clonedMethod) {
    VirtualInvocation clone = null;

    try {
      clone = (VirtualInvocation) super.clone();


      clone.mi = clonedMethod;

      clone.lastCalleeCi = null;
      clone.invokedMethod = null;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return clone;
  }
}
