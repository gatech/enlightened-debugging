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


public class DaemonTest extends TestJPF {

  static class T1 extends Thread {

    boolean blowUp = false;


    T1() {
      setDaemon(true);
    }

    @Override
	public void run() {
      if (blowUp){
        throw new RuntimeException("blow up");
      }
    }
  }

  
  
  @Test
  public void testRace(){
    if (verifyUnhandledExceptionDetails("java.lang.RuntimeException", "blow up")){
      T1 t = new T1();
      t.start();
      




      t.blowUp = true;
      
      int dummy = 42; 
    }
  }
}
