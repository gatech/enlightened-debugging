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
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;

import org.junit.Test;


public class CGRemoverTest extends TestJPF {

  static class R1 implements Runnable {
    int data = 42;

    public synchronized int getData() {
      return data;
    }

    @Override
	public void run() {
      int r = getData();  
    }
  }

  public static class R1Listener extends ListenerAdapter {

    @Override
    public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG){
      Instruction insn = newCG.getInsn();

      if (insn instanceof JVMInvokeInstruction){
        MethodInfo mi = ((JVMInvokeInstruction)insn).getInvokedMethod();
        if (mi.getName().equals("getData")){
          fail("CG should have been removed by CGRemover");
        }
      }
    }
  }


  @Test
  public void testSyncLocation() {
    if (verifyNoPropertyViolation("+listener=.listener.CGRemover,.test.mc.basic.CGRemoverTest$R1Listener",
            "+log.info=gov.nasa.jpf.CGRemover",
            "+cgrm.sync.cg_class=gov.nasa.jpf.vm.ThreadChoiceGenerator",
            "+cgrm.sync.locations=CGRemoverTest.java:45,CGRemoverTest.java:75")){
      R1 o = new R1();
      Thread t = new Thread(o);
      t.start();   

      int r = o.getData(); 
    }
  }


  @Test
  public void testSyncCall() {
    if (verifyNoPropertyViolation("+listener=.listener.CGRemover,.test.mc.basic.CGRemoverTest$R1Listener",
            "+log.info=gov.nasa.jpf.CGRemover",
            "+cgrm.sync.cg_class=gov.nasa.jpf.vm.ThreadChoiceGenerator",
            "+cgrm.sync.method_calls=gov.nasa.jpf.test.mc.basic.CGRemoverTest$R1.getData()")){
      R1 o = new R1();
      Thread t = new Thread(o);
      t.start();   

      int r = o.getData(); 
    }
  }

  @Test
  public void testSyncBody() {
    if (verifyNoPropertyViolation("+listener=.listener.CGRemover,.test.mc.basic.CGRemoverTest$R1Listener",
            "+log.info=gov.nasa.jpf.CGRemover",
            "+cgrm.sync.cg_class=gov.nasa.jpf.vm.ThreadChoiceGenerator",
            "+cgrm.sync.method_bodies=gov.nasa.jpf.test.mc.basic.CGRemoverTest$R1.run(),gov.nasa.jpf.test.mc.basic.CGRemoverTest.testSyncBody()")){
      R1 o = new R1();
      Thread t = new Thread(o);
      t.start();   

      int r = o.getData(); 
    }
  }
}
