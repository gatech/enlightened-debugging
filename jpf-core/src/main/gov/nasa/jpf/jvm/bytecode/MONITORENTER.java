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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;



public class MONITORENTER extends LockInstruction {

  @Override
  public Instruction execute (ThreadInfo ti) {
    Scheduler scheduler = ti.getScheduler();
    StackFrame frame = ti.getTopFrame();

    int objref = frame.peek();      
    if (objref == MJIEnv.NULL){
      return ti.createAndThrowException("java.lang.NullPointerException", "Attempt to acquire lock for null object");
    }

    lastLockRef = objref;
    ElementInfo ei = ti.getModifiableElementInfo(objref);    
    ei = scheduler.updateObjectSharedness(ti, ei, null); 
    
    if (!ti.isLockOwner(ei)){ 
      if (ei.canLock(ti)) {


        ei.registerLockContender(ti);
        if (scheduler.setsLockAcquisitionCG(ti, ei)) { 
          return this;
        }
        
      } else { 
        ei.block(ti); 
        if (scheduler.setsBlockedThreadCG(ti, ei)){ 
          return this;
        }
        throw new JPFException("blocking MONITORENTER without transition break");            
      }
    }
    

    frame = ti.getModifiableTopFrame(); 
    frame.pop();
    
    ei.lock(ti);  
    return getNext(ti);
  }  

  @Override
  public int getByteCode () {
    return 0xC2;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
