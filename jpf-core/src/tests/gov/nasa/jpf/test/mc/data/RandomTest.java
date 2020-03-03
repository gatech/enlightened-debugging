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


package gov.nasa.jpf.test.mc.data;


import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;

import java.util.Random;

import org.junit.Test;


public class RandomTest extends TestJPF {
  private void run (int n){
    int i = Verify.getInt(0,n); 
    Verify.incrementCounter(0); 
    System.out.println(i);
  }

  @Test public void testRandom () {
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }
    if (verifyNoPropertyViolation()){
      run(3);
    }
    if (!isJPFRun()){
      if (Verify.getCounter(0) != 4){
        fail("wrong number of paths");
      }
    }
  }

  @Test public void testRandomBFS () {
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }
    if (verifyNoPropertyViolation("+search.class=gov.nasa.jpf.search.heuristic.BFSHeuristic")){
      run(3);
    }
    if (!isJPFRun()){
      if (Verify.getCounter(0) != 4){
        fail("wrong number of paths");
      }
    }
  }


  
  @Test public void testJavaUtilRandom () {

    if (verifyUnhandledException("java.lang.ArithmeticException", "+cg.enumerate_random=true")) {
      Random random = new Random(42);      

      int a = random.nextInt(4);           
      System.out.print("a=");
      System.out.println(a);



      int b = random.nextInt(3);           
      System.out.print("a=");
      System.out.print(a);
      System.out.print(",b=");
      System.out.println(b);


      int c = a / (b + a - 2);                  
      System.out.print("a=");
      System.out.print(a);
      System.out.print(",b=");
      System.out.print(b);
      System.out.print(",c=");
      System.out.println(c);
    }
  }
}
