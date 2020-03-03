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

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;


public class INVOKECLINIT extends INVOKESTATIC {

  public INVOKECLINIT (ClassInfo ci){
    super(ci.getSignature(), "<clinit>", "()V");
  }

  @Override
  public Instruction execute (ThreadInfo ti) {    
    MethodInfo callee = getInvokedMethod(ti);
    ClassInfo ciClsObj = callee.getClassInfo();
    ElementInfo ei = ciClsObj.getClassObject();

    if (ciClsObj.isInitialized()) { 
      if (ei.isRegisteredLockContender(ti)){
        ei = ei.getModifiableInstance();
        ei.unregisterLockContender(ti);
      }
      return getNext();
    }


    if (reschedulesLockAcquisition(ti, ei)){     
      return this;
    }
    

    setupCallee( ti, callee); 
    ciClsObj.setInitializing(ti);

    return ti.getPC(); 
  }

  @Override
  public boolean isExtendedInstruction() {
    return true;
  }

  public static final int OPCODE = 256;

  @Override
  public int getByteCode () {
    return OPCODE;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
