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

import gov.nasa.jpf.util.test.TestJPF;
import java.util.HashMap;
import org.junit.Test;


public class NullTrackerTest extends TestJPF {
  
  static class TestObject {
    String d;
  
    TestObject(){

    }
    
    TestObject (String d){
      this.d = d;
    }
    
    int getDLength(){
      return d.length();
    }
    
    void foo(){

    }
  }

  TestObject o;
  
  TestObject getTestObject (){
    return null;
  }
  
  void accessReturnedObject (){
    TestObject o = getTestObject();
    System.out.println("now accessing testObject");
    String d = o.d; 
  }
  
  void accessObject (TestObject o){
    System.out.println("now accessing testObject");
    String d = o.d; 
  }
  
  void createAndAccessObject(){
    TestObject o = getTestObject();
    accessObject(o);
  }
  
  
  @Test
  public void testGetAfterIntraMethodReturn (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      accessReturnedObject();
    }
  }
  
  @Test
  public void testGetAfterInterMethodReturn (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      createAndAccessObject();
    }
  }

  @Test
  public void testGetAfterIntraPut (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      o = null; 
      
      String d = o.d; 
    }    
  }
  
  @Test
  public void testCallAfterIntraPut (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      o = null; 
      
      o.foo(); 
    }    
  }

  @Test
  public void testGetAfterASTORE (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      TestObject myObj = null; 
      
      myObj.foo(); 
    }    
  }

  
  HashMap<String,TestObject> map = new HashMap<String,TestObject>();
  
  TestObject lookupTestObject (String name){
    return map.get(name);
  }
  
  @Test
  public void testHashMapGet (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      TestObject o = lookupTestObject("FooBar");
      o.foo();
    }
  }
  

    
  TestObject createTestObject (){
    return new TestObject();
  }
  
  
  TestObject createTestObject (String d){
    return new TestObject(d);
  }
  
  @Test
  public void testMissingCtorInit (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      TestObject o = createTestObject("blah");
      int len = o.getDLength(); 
      
      o = createTestObject();
      len = o.getDLength(); 
    }    
  }
}
