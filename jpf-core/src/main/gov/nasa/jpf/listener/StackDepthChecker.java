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


package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


public class StackDepthChecker extends ListenerAdapter {
  
  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.listener.StackDepthChecker");

  protected int maxDepth;
  
  public StackDepthChecker (Config config, JPF jpf){
    maxDepth = config.getInt( "sdc.max_stack_depth", 42);
  }
  
  @Override
  public void methodEntered (VM vm, ThreadInfo thread, MethodInfo mi){
    
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    int depth = ti.getStackDepth(); 
    
    if (depth > maxDepth){
      log.info("configured vm.max_stack_depth exceeded: ", depth);
      




      Instruction nextPc = ti.createAndThrowException("java.lang.StackOverflowError");
      StackFrame topFrame = ti.getModifiableTopFrame();
      topFrame.setPC(nextPc);
    }
  }
}
