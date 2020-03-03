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


package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.choice.BreakGenerator;
import org.junit.Test;


public class TransitionLengthTest extends TestJPF {
    
  public static class Listener extends ListenerAdapter {
    @Override
    public void choiceGeneratorRegistered (VM vm, ChoiceGenerator<?> nextCG, ThreadInfo currentThread, Instruction executedInstruction) {
      if (nextCG instanceof BreakGenerator){
        System.out.println();
        System.out.println("registered: " + nextCG);
      }
    }
  }
  
  @Test
  public void testTermination(){
    if (verifyNoPropertyViolation("+vm.max_transition_length=500", 
                                  "+listener=" + TransitionLengthTest.class.getName() + "$Listener")){
      System.out.println("starting loop");
      while (true){

        System.out.print(".");
      }

    }
  }
}
