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
import gov.nasa.jpf.vm.Verify;

import org.junit.Test;


public class UnlockNonSharedTest extends TestJPF
{
   @Test
   public void test() throws InterruptedException
   {
      Runnable task, nop;
      Thread thread;
      
      if (verifyNoPropertyViolation())
      {
         Verify.setProperties("vm.por.skip_local_sync=true");

         nop = new Runnable()
         {
            @Override
			public void run()
            {

            }
         };
         
         task = new Runnable()
         {
            private final Object m_lock = new Object();
            private       int    m_count;
            
            @Override
			public void run()
            {
               synchronized (m_lock)
               {
                  m_count++;
               }
            }
         };
         
         task.run();                    
         
         thread = new Thread(task);     
         
         thread.setDaemon(false);
         thread.start();                
         
         thread = new Thread(nop);      
   
         thread.setDaemon(false);
         thread.start();                
      }
   }
}
