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

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFNativePeerException;
import gov.nasa.jpf.util.JPFLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;


public class NativeMethodInfo extends MethodInfo {

  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.vm.NativePeer");

  static final int  MAX_NARGS = 6;
  static Object[][]  argCache;

  static {
    argCache = new Object[MAX_NARGS][];

    for (int i = 0; i < MAX_NARGS; i++) {
      argCache[i] = new Object[i];
    }
  }

  protected Method mth; 
  protected NativePeer peer;

  public NativeMethodInfo (MethodInfo mi, Method mth, NativePeer peer){
    super(mi);  

    this.peer = peer;
    this.mth = mth;

    ci.setNativeCallCode(this);
  }

  public void replace( MethodInfo mi){
    mthTable.set(mi.globalId, this);
    mi.ci.putDeclaredMethod(this);
  }
  
  @Override
  public boolean isUnresolvedNativeMethod() {

    return false;
  }

  @Override
  public boolean isMJI () {
    return true;
  }

  @Override
  public boolean hasEmptyBody (){

    return false;
  }

  @Override
  public boolean isJPFExecutable(){
    return true; 
  }

  public NativePeer getNativePeer() {
    return peer;
  }

  public Method getMethod() {
    return mth;
  }

  @Override
  public String getStackTraceSource() {
    if (peer != null){
      return peer.getPeerClassName();
    } else {
      return "no peer";
    }
  }

  @Override
  public int getLineNumber (Instruction pc) {
    return -1; 
  }

  public Instruction executeNative (ThreadInfo ti) {
    Object   ret = null;
    Object[] args = null;
    MJIEnv   env = ti.getMJIEnv();
        
    NativeStackFrame nativeFrame = (NativeStackFrame)ti.getTopFrame();
    env.setCallEnvironment(this);
    MethodInvocationAttr frameAttr = nativeFrame.getFrameAttr(MethodInvocationAttr.class);
    if (frameAttr != null) {
    	env.setInvocationCondition(frameAttr.getInvocationCondition());
    }

    if (isUnsatisfiedLinkError(env)) {
      return ti.createAndThrowException("java.lang.UnsatisfiedLinkError",
                                        "cannot find native " + ci.getName() + '.' + getName());
    }

    try {
      args = nativeFrame.getArguments();
      

      ret = mth.invoke(peer, args);

      if (env.hasException()) {





      	DependencyTrackingInstruction callingInstr = 
      			(DependencyTrackingInstruction) nativeFrame.getCallerFrame().getPC();
      	DynamicDependency exTriggerDep = env.getExceptionDependency();
      	int exRef = env.popException();
      	DynamicDependency exDep = DynDepBuilder.newBuilder()
      			.appendDataDependency(exTriggerDep, 
      					callingInstr.getInstructionDepSource(ti))
      			.setControlDependency(callingInstr.getControlDependencyCondition(ti)).build();
        return ti.throwException(exRef, exDep);
      }

      StackFrame top = ti.getTopFrame();

      if (top.originatesFrom(nativeFrame)){ 
        NativeStackFrame ntop = (NativeStackFrame)top;

        if (env.isInvocationRepeated()){

          return ntop.getPC();

        } else {




          ntop.setReturnValue(ret);
          ntop.setReturnAttr(env.getReturnAttribute());

          return ntop.getPC().getNext(); 
        }

      } else {



        return top.getPC();
      }

    } catch (IllegalArgumentException iax) {
      logger.warning(iax.toString());
      return ti.createAndThrowException("java.lang.IllegalArgumentException",
                                        "calling " + ci.getName() + '.' + getName());
    } catch (IllegalAccessException ilax) {
      logger.warning(ilax.toString());
      return ti.createAndThrowException("java.lang.IllegalAccessException",
                                        "calling " + ci.getName() + '.' + getName());
    } catch (InvocationTargetException itx) {


      if(itx.getTargetException() instanceof ClassInfoException) {
        ClassInfoException cie = (ClassInfoException) itx.getTargetException();
        return ti.createAndThrowException(cie.getExceptionClass(), cie.getMessage());
      }

      if (itx.getTargetException() instanceof UncaughtException) {  
        throw (UncaughtException) itx.getTargetException();
      } 
       


      throw new JPFNativePeerException("exception in native method "
          + ci.getName() + '.' + getName(), itx.getTargetException());
    }
  }

  protected boolean isUnsatisfiedLinkError(MJIEnv env){
    return(mth == null);
  }

  
  protected Object[] getArguments (ThreadInfo ti) {

    int      nArgs = getNumberOfArguments();
    byte[]   argTypes = getArgumentTypes();


    Object[] a = new Object[nArgs+2];

    int      stackOffset;
    int      i, j, k;
    int      ival;
    long     lval;
    StackFrame caller = ti.getTopFrame();


    for (i = 0, stackOffset = 0, j = nArgs + 1, k = nArgs - 1;
         i < nArgs;
         i++, j--, k--) {
      switch (argTypes[k]) {
      case Types.T_BOOLEAN:
        ival = caller.peek(stackOffset);
        a[j] = Boolean.valueOf(Types.intToBoolean(ival));

        break;

      case Types.T_BYTE:
        ival = caller.peek(stackOffset);
        a[j] = Byte.valueOf((byte) ival);

        break;

      case Types.T_CHAR:
        ival = caller.peek(stackOffset);
        a[j] = Character.valueOf((char) ival);

        break;

      case Types.T_SHORT:
        ival = caller.peek(stackOffset);
        a[j] = new Short((short) ival);

        break;

      case Types.T_INT:
        ival = caller.peek(stackOffset);
        a[j] = new Integer(ival);

        break;

      case Types.T_LONG:
        lval = caller.peekLong(stackOffset);
        stackOffset++; 
        a[j] = new Long(lval);

        break;

      case Types.T_FLOAT:
        ival = caller.peek(stackOffset);
        a[j] = new Float(Types.intToFloat(ival));

        break;

      case Types.T_DOUBLE:
        lval = caller.peekLong(stackOffset);
        stackOffset++; 
        a[j] = new Double(Types.longToDouble(lval));

        break;

      default:


        ival = caller.peek(stackOffset);
        a[j] = new Integer(ival);
      }

      stackOffset++;
    }


    if (isStatic()) {
      a[1] = new Integer(ci.getClassObjectRef());
    } else {
      a[1] = new Integer(ti.getCalleeThis(this));
    }

    a[0] = ti.getMJIEnv();

    return a;
  }
}
