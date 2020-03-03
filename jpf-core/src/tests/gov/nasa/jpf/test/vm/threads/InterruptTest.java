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


package gov.nasa.jpf.test.vm.threads;

import gov.nasa.jpf.util.test.TestJPF;

import java.util.concurrent.locks.LockSupport;

import org.junit.Test;


public class InterruptTest extends TestJPF {

  @Test public void testInterruptStatus () {
    if (verifyNoPropertyViolation()) {
      Thread t = Thread.currentThread();

      assert !t.isInterrupted() : "initial interrupt status is set";

      System.out.println("setting interrupt status");
      t.interrupt();

      assert t.isInterrupted() : "interrupt status not set";

      System.out.println("query and clear interrupt status");
      assert Thread.interrupted() : "interrupt status prematurely reset";

      assert !Thread.interrupted() : "interrupt status wasn't reset";
    }
  }

  @Test public void testWaitSyncInterrupt() {
    if (verifyNoPropertyViolation()) {
      Runnable r = new Runnable() {

        @Override
		public void run() {


          Thread t = Thread.currentThread();
          t.interrupt();

          synchronized (this) {


            System.out.println("T waiting");
            try {
              wait(); 
              assert false : "wait() did not throw InterruptedException";
            } catch (InterruptedException ix) {
              ix.printStackTrace(); 
              System.out.println("T interrupted, terminating");
              assert !t.isInterrupted() : "throw didn't reset interrupt status";
              return;
            } catch (Throwable x) {
              assert false : "wait did throw wrong exception: " + x;
              return;
            }
          }
          assert false : "should never get here";
        }
      };

      Thread t1 = new Thread(r);

      t1.interrupt(); 
      assert !t1.isInterrupted() : "non-started thread has interrupt status set";

      t1.start();
      System.out.println("main terminated");
    }
  }

  @Test public void testWaitAsyncInterrupt() {
    if (verifyNoPropertyViolation()) {
      Runnable r = new Runnable() {

        @Override
		public void run() {
          synchronized (this) {

            try {
              System.out.println("T waiting");
              wait();
              assert false : "wait() did not throw InterruptedException";
            } catch (InterruptedException ix) {
              ix.printStackTrace(); 
              System.out.println("T interrupted, terminating");
              assert !Thread.currentThread().isInterrupted() : "throw didn't reset interrupt status";
              return;
            } catch (Throwable x) {
              assert false : "wait did throw wrong exception: " + x;
              return;
            }
          }
          assert false : "should never get here";
        }
      };

      Thread t1 = new Thread(r);
      t1.start();




      System.out.println("main interrupting t1");
      t1.interrupt();

      System.out.println("main terminated");
    }
  }

  boolean interrupted;
  boolean waiting;

  @Test public void testBlockedWaitAsyncInterrupt() {
    if (verifyNoPropertyViolation()) {

      interrupted = false;
      waiting = false;

      Runnable r = new Runnable() {

        @Override
		public void run() {
          synchronized (this) {

            try {
              System.out.println("T waiting");
              waiting = true;
              wait();
              assert false : "wait() did not throw InterruptedException";
            } catch (InterruptedException ix) {
              ix.printStackTrace(); 
              assert !Thread.currentThread().isInterrupted() : "throw didn't reset interrupt status";
              System.out.println("T interrupted, terminating");
              interrupted = true;
              return;
            } catch (Throwable x) {
              assert false : "wait did throw wrong exception: " + x;
              return;
            }
          }
          assert false : "should never get here";
        }
      };

      Thread t1 = new Thread(r);
      t1.start();

      while (!waiting) {
        Thread.yield();
      }

      synchronized (r) {
        System.out.println("main interrupting t1");
        t1.interrupt();


        Thread.yield(); 
        assert !interrupted : "t1 prematurely scheduled w/o acquiring the lock";
        System.out.println("main terminated, t1 runnable again");
      }
    }
  }

  @Test public void testPark() {
    if (verifyNoPropertyViolation()) {

      interrupted = false;

      Thread t1 = new Thread(new Runnable() {

        @Override
		public void run() {
          System.out.println("T parking..");
          LockSupport.park();
          interrupted = true;
          System.out.println("T terminated");
        }
      });

      t1.start();

      System.out.println("main interrupting");
      t1.interrupt();

      try {
        System.out.println("main joining t1..");
        t1.join();
        System.out.println("main joined t1");
      } catch (InterruptedException e) {
        assert false : "t1.join() interrupted in main";
      }

      assert interrupted : "LockSupport.park() didn't get interrupted";
      System.out.println("main terminated");
    }
  }

}
