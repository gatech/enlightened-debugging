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


import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;



public class INVOKESTATIC extends JVMInvokeInstruction {
  ClassInfo ci;
  
  protected INVOKESTATIC (String clsDescriptor, String methodName, String signature){
    super(clsDescriptor, methodName, signature);
  }

  protected ClassInfo getClassInfo () {
    if (ci == null) {
      ci = ClassLoaderInfo.getCurrentResolvedClassInfo(cname);
    }
    return ci;
  }
  
  @Override
  public int getByteCode () {
    return 0xB8;
  }

  @Override
  public String toPostExecString(){
    StringBuilder sb = new StringBuilder();
    sb.append(getMnemonic());
    sb.append(' ');
    sb.append( invokedMethod.getFullName());

    if (invokedMethod.isMJI()){
      sb.append(" [native]");
    }
    
    return sb.toString();

  }
  
  public StaticElementInfo getStaticElementInfo (){
    return getClassInfo().getStaticElementInfo();
  }

  public int getClassObjectRef(){
    return getClassInfo().getStaticElementInfo().getClassObjectRef();
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    MethodInfo callee;
    
    try {
      callee = getInvokedMethod(ti);
    } catch (LoadOnJPFRequired lre) {
      return ti.getPC();
    }
        
    if (callee == null) {
    	DynamicDependency exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(getInstructionDepSource(ti))
    			.setControlDependency(getControlDependencyCondition(ti)).build();
      return ti.createAndThrowException("java.lang.NoSuchMethodException", cname + '.' + mname, exDep);
    }


    ClassInfo ciCallee = callee.getClassInfo();
    
    if (ciCallee.initializeClass(ti)) {


      return ti.getPC();
    }

    if (callee.isSynchronized()) {
      ElementInfo ei = ciCallee.getClassObject();
      ei = ti.getScheduler().updateObjectSharedness(ti, ei, null); 
      
      if (reschedulesLockAcquisition(ti, ei)){
        return this;
      }
    }
        
    setupCallee( ti, callee); 

    return ti.getPC(); 
  }

  @Override
  public MethodInfo getInvokedMethod(){
    if (invokedMethod != null){
      return invokedMethod;
    } else {


      return getInvokedMethod( ThreadInfo.getCurrentThread());
    }
  }
  
  @Override
  public MethodInfo getInvokedMethod (ThreadInfo ti){
    if (invokedMethod == null) {
      ClassInfo clsInfo = getClassInfo();
      if (clsInfo != null){
        MethodInfo callee = clsInfo.getMethod(mname, true);
        if (callee != null){
          ClassInfo ciCallee = callee.getClassInfo(); 

          if (!ciCallee.isRegistered()){

            ciCallee.registerClass(ti);

            callee = clsInfo.getMethod(mname, true);
          }
          invokedMethod = callee;
        }
      }    
    }
    return invokedMethod;
  }
  

  public ClassInfo getInvokedClassInfo(){
    return getInvokedMethod().getClassInfo();
  }

  public String getInvokedClassName(){
    return getInvokedClassInfo().getName();
  }

  @Override
  public int getArgSize () {
    if (argSize < 0) {
      argSize = Types.getArgumentsSize(signature);
    }

    return argSize;
  }

  
  @Override
  public String toString() {

    return "invokestatic " + cname + '.' + mname;
  }

  @Override
  public Object getFieldValue (String id, ThreadInfo ti) {
    return getClassInfo().getStaticFieldValueObject(id);
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  @Override
  public Instruction typeSafeClone(MethodInfo mi) {
    INVOKESTATIC clone = null;

    try {
      clone = (INVOKESTATIC) super.clone();


      clone.mi = mi;

      clone.invokedMethod = null;
      clone.lastObj = Integer.MIN_VALUE;
      clone.ci = null;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return clone;
  }
}

