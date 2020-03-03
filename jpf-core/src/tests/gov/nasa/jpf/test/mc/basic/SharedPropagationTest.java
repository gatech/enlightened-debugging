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
import org.junit.Test;


public class SharedPropagationTest extends TestJPF {

  static class Gotcha extends RuntimeException {

  }
  

  
  static class T1 extends Thread {

    static class X {

      boolean pass;
    }
    X myX; 

    public static void main(String[] args) {
      T1 t = new T1();
      t.start();

      X x = new X();
      t.myX = x;        


      x.pass = true;     
    }

    @Override
	public void run() {
      if (myX != null) {
        if (!myX.pass) {  
          throw new Gotcha();
        }
      }
    }
  }
  
  @Test
  public void testLocalRef(){
    if (verifyUnhandledException( Gotcha.class.getName(), "+vm.scheduler.sharedness.class=.vm.GlobalSharednessPolicy")){
      T1.main(new String[0]);
    }
  }
  
  

  
  static class T2 extends Thread {

    static class X {
      boolean pass;
    }

    static class Y {
      X x;
    }
    
    Y y;

    public static void main(String[] args) {
      T2 t = new T2();
      Y y = new Y();
      X x = new X();

      y.x = x;


      t.start();
      t.y = y; 

      x.pass = true;
    }

    @Override
	public void run() {
      if (y != null) {
        if (!y.x.pass) {
          throw new Gotcha();
        }
      }
    }
  }
  
  @Test
  public void testLevel1Ref(){
    if (verifyUnhandledException(Gotcha.class.getName())){
      T2.main(new String[0]);
    }
  }


  
  static class T3 extends Thread {

    static class X {
      boolean pass;
    }

    static class Y {
      X x;
    }
    static Y globalY; 

    
    public static void main(String[] args) {
      T3 t = new T3();
      t.start();

      X x = new X();
      Y y = new Y();
      y.x = x;

      globalY = y;           


      x.pass = true;     
    }

    @Override
	public void run() {
      if (globalY != null) {
        if (!globalY.x.pass) {  
          throw new Gotcha();
        }
      }
    }
  }
  
  @Test
  public void testStaticFieldPropagation(){
    if (verifyUnhandledException(Gotcha.class.getName(), "+vm.scheduler.sharedness.class=.vm.GlobalSharednessPolicy")){
      T3.main(new String[0]);
    }
  }
  
  

  
  static class Hyber {
    private static Timeout thread = new Timeout();

    public static void main(String[] args) {
      thread.start();
      Timeout.Entry timer = thread.setTimeout(); 

      timer.hyber = true;  
    }
  }

  static class Timeout extends Thread {

    static class Entry {
      boolean hyber = false;
      Entry next = null;
      Entry prev = null;
    }
    Entry e = new Entry();

    Timeout() {
      e.next = e.prev = e;
    }

    public Entry setTimeout() {
      Entry entry = new Entry();
      synchronized (e) {
        entry.next = e;
        entry.prev = e.prev;
        entry.prev.next = entry;
        entry.next.prev = entry;
      }

      return entry;
    }

    @Override
	public void run() {
      synchronized (e) {
        for (Entry entry = e.next; entry != e; entry = entry.next) {
          if (!entry.hyber) { 
            throw new Gotcha();
          }
        }
      }
    }
  }
  
  @Test
  public void testHyber() {
    if (verifyUnhandledException(Gotcha.class.getName(), "+vm.scheduler.sharedness.class=.vm.GlobalSharednessPolicy")){
      Hyber.main(new String[0]);
    }    
  }

}
