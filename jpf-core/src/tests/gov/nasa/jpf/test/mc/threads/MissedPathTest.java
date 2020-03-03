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


package gov.nasa.jpf.test.mc.threads;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;


public class MissedPathTest extends TestJPF {

  static class X {
    boolean pass;
  }
  
  static class InstanceFieldPropagation extends Thread {
    X myX; 

    @Override
	public void run() {
      if (myX != null){
        Verify.println("T: accessed global myX");
        if (!myX.pass){  
          throw new AssertionError("gotcha");
        }
      }
    }    
  }

  @Test
  public void testInstanceFieldPropagation () {
    if (verifyAssertionErrorDetails("gotcha", "+vm.shared.break_on_exposure=true")) {
      InstanceFieldPropagation mp = new InstanceFieldPropagation();
      mp.start();
      
      X x = new X();
      Verify.println("M: new " + x);
      mp.myX = x;        
     

      Verify.println("M: x.pass=true");
      x.pass = true;     
    }
  }
  

  
  static class Y {
    X x;
  }
  
  static Y globalY; 
  
  static class StaticFieldPropagation extends Thread {
    @Override
	public void run(){
      if (globalY != null){
        if (!globalY.x.pass){  
          throw new AssertionError("gotcha");
        }
      }
    }
  }
  
  @Test
  public void testStaticFieldPropagation () {
    if (verifyAssertionErrorDetails("gotcha", "+vm.shared.break_on_exposure=true")) {
      StaticFieldPropagation mp = new StaticFieldPropagation();
      mp.start();

      X x = new X();
      Y y = new Y();
      y.x = x;
      
      globalY = y;       


      x.pass = true;     
    }    
  }
  

  
  static class PutContender extends Thread {
    X myX;

    @Override
	public void run () {
      myX = new X();  

      if (myX != null) {  
        Verify.println("T: accessed global myX");
      }
    }
  }
  



  @Test
  public void testCompetingExposures(){
    if (verifyNoPropertyViolation("+vm.storage.class=nil")){
      PutContender mp = new PutContender();
      mp.start();

      X x = new X();
      Verify.println("M: new " + x);
      mp.myX = x;    

      Verify.println("M: x.pass=true");
      x.pass = true; 
    }
  }
}
