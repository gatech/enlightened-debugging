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


package java8;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.Verify;
import org.junit.Test;


public class DefaultMethodTest extends TestJPF {
  

  
  interface A1 {
    default int getValue(){
      return 42;
    }
  } 
  
  interface B1 extends A1 {

  }
  
  static class C1 implements A1 {

  }
  
  static class D1 extends C1 {

  } 
  
  @Test
  public void testSingleMethod (){
    if (verifyNoPropertyViolation()){
      D1 o = new D1();
      int result = o.getValue();
      System.out.println(result);
      assertTrue (result == 42);
    }
  }
  

  
  interface B2 {
    default int getValue(){
      return 3;
    }
  }
  
  static class D2 implements A1, B2 {
    @Override
    public int getValue(){
      return A1.super.getValue() + B2.super.getValue();
    }
  }
  
  @Test
  public void testExplicitDelegation (){
    if (verifyNoPropertyViolation()){
      D2 o = new D2();
      int result = o.getValue();
      System.out.println(result);
      assertTrue (result == 45);
    }    
  }



  interface E1 {
    default int foo (String s1, String s2){
      System.out.println("this is E1.foo(String,String)");
      return 42;
    }

    default int foo (Object o1, Object o2){
      System.out.println("this is E1.foo(Object,Object)");
      return 0;
    }
  }

  static class F implements E1 {
    String getId() {
      return "whatever";
    }

    void bar (){
      int r = foo("blah", getId());
      assertTrue(r == 42);
    }
  }

  @Test
  public void testOverloadedDefaults(){
    if (verifyNoPropertyViolation()){
      F o = new F();
      o.bar();
    }
  }



  interface G1 {
    default int foo (){  
      System.out.println("this is bytecode G1.foo()");
      return -1;
    }
  }

  static class H implements G1 {
    void bar (){
      int r = foo();

    }
  }

  @Test
  public void testInterfacePeer(){
    if (verifyNoPropertyViolation()){
      H o = new H();
      o.bar();
    }
  }


}

