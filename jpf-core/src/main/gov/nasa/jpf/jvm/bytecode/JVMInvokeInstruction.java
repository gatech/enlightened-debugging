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
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.bytecode.InvokeInstruction;



public abstract class JVMInvokeInstruction extends InvokeInstruction implements JVMInstruction {
  
  protected String cname;
  protected String mname;
  protected String signature;

  protected int argSize = -1;

  
  protected int lastObj = Integer.MIN_VALUE;

  
  protected MethodInfo invokedMethod;

  protected Object[] arguments; 

  protected JVMInvokeInstruction (String clsName, String methodName, String signature){
    this.cname = Types.getClassNameFromTypeName(clsName);
    this.signature = signature;
    this.mname = MethodInfo.getUniqueName(methodName, signature);
  }

  protected JVMInvokeInstruction () {}

  @Override
  public int getLength() {
    return 3; 
  }
  

  public int getLastObjRef() {
    return lastObj;
  }

  
  public void setInvokedMethod (String clsName, String mthName, String sig) {
    cname = clsName;
    mname = mthName + sig;
    signature = sig;
  }

  
  @Override
  public String getInvokedMethodClassName() {
    return cname;
  }

  @Override
  public String getInvokedMethodSignature() {
    return signature;
  }

  @Override
  public String getInvokedMethodName () {
    return mname;
  }

  public abstract MethodInfo getInvokedMethod (ThreadInfo ti);

  @Override
  public MethodInfo getInvokedMethod () {
    if (invokedMethod == null){
      invokedMethod = getInvokedMethod(ThreadInfo.getCurrentThread());
    }

    return invokedMethod;
  }

  @Override
  public boolean isCompleted(ThreadInfo ti) {
    Instruction nextPc = ti.getNextPC();

    if (nextPc == null || nextPc == this){
      return false;
    }

    if (invokedMethod != null){
      MethodInfo topMethod = ti.getTopFrame().getMethodInfo();
      if (invokedMethod.isMJI() && (topMethod == mi)) {

        return true;
      }

      if (topMethod == invokedMethod){
        return true;
      }
    }



    return false;
  }

  StackFrame getCallerFrame (ThreadInfo ti, MethodInfo callee) {
    return ti.getStackFrameExecuting(this, 0);
  }



  protected void setupCallee (ThreadInfo ti, MethodInfo callee){
    ClassInfo ciCaller = callee.getClassInfo();
    StackFrame frame = ciCaller.createStackFrame( ti, callee);
    


    DynamicDependency invocationCondition = DynDepBuilder.newBuilder()
  			.appendDataDependency(getInstructionDepSource(ti))
  			.setControlDependency(getControlDependencyCondition(ti)).build();
    ti.pushFrame(frame);
    ti.enter();
    




    MethodInvocationAttr frameAttr = frame.getFrameAttr(MethodInvocationAttr.class);
    if (invocationCondition != null && frameAttr != null) {
    	frameAttr.setInvocationCondition(invocationCondition);
    }
  }
  
  
  public Object[] getArgumentValues (ThreadInfo ti) {    
    MethodInfo callee = getInvokedMethod(ti);
    StackFrame frame = getCallerFrame(ti, callee);
    
    assert frame != null : "can't find caller stackframe for: " + this;
    return frame.getCallArguments(ti);
  }

  public Object[] getArgumentAttrs (ThreadInfo ti) {
    MethodInfo callee = getInvokedMethod(ti);
    StackFrame frame = getCallerFrame(ti, callee);

    assert frame != null : "can't find caller stackframe for: " + this;
    return frame.getArgumentAttrs(callee);
  }
  
  
  
  public boolean hasArgumentAttr (ThreadInfo ti, Class<?> type){
    MethodInfo callee = getInvokedMethod(ti);
    StackFrame frame = getCallerFrame(ti, callee);

    assert frame != null : "no caller stackframe for: " + this;
    return frame.hasArgumentAttr(callee,type);
  }

  
  public boolean hasArgumentObjectAttr (ThreadInfo ti, Class<?> type){
    MethodInfo callee = getInvokedMethod(ti);
    StackFrame frame = getCallerFrame(ti, callee);

    assert frame != null : "no caller stackframe for: " + this;
    return frame.hasArgumentObjectAttr(ti,callee,type);
  }

  
  abstract public int getArgSize();
  
  public int getReturnType() {
    return Types.getReturnBuiltinType(signature);
  }

  public boolean isReferenceReturnType() {
    int r = Types.getReturnBuiltinType(signature);
    return ((r == Types.T_REFERENCE) || (r == Types.T_ARRAY));
  }

  public String getReturnTypeName() {
    return Types.getReturnTypeName(signature);
  }

  public Object getFieldOrArgumentValue (String id, ThreadInfo ti){
    Object v = null;

    if ((v = getArgumentValue(id,ti)) == null){
      v = getFieldValue(id, ti);
    }

    return v;
  }

  public abstract Object getFieldValue (String id, ThreadInfo ti);


  
  public Object getArgumentValue (String id, ThreadInfo ti){
    MethodInfo mi = getInvokedMethod();
    LocalVarInfo localVars[] = mi.getLocalVars();
    Object[] args = getArgumentValues(ti);

    if (localVars != null){
      int j = mi.isStatic() ? 0 : 1;

      for (int i=0; i<args.length; i++, j++){
        Object a = args[i];
        if (id.equals(localVars[j].getName())){
          return a;
        }
      }
    }

    return null;
  }
    
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  @Override
  public Instruction typeSafeClone(MethodInfo mi) {
    JVMInvokeInstruction clone = null;

    try {
      clone = (JVMInvokeInstruction) super.clone();


      clone.mi = mi;

      clone.invokedMethod = null;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return clone;
  }
}
