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



package gov.nasa.jpf.test.java.concurrent;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class CountDownLatchTest extends TestJPF {

  private static final int N = 2;                    
  private static final int COUNTER_SUCCESS   = 0;
  private static final int COUNTER_EXCHANGED = 1;



  @Test
  public void testCountDown() throws InterruptedException {
    if (verifyNoPropertyViolation("+vm.time.model=ConstantZero", "+vm.por.break_on_exposure=true")) {

      final CountDownLatch    latch     = new CountDownLatch(N);
      final Exchanger<Object> exchanger = new Exchanger<Object>();
      final ExecutorService   service   = Executors.newFixedThreadPool(N);

      Runnable task = new Runnable() {
        @Override
		public void run() {
          try {
            Object source = new Object();
            Object result = exchanger.exchange(source);

            assert source != result : "source != result";
            assert result != null : "result != null";
            latch.countDown();
            Verify.incrementCounter(COUNTER_EXCHANGED);
          } catch (InterruptedException e) {
            throw new Error(e);
          }
        }
      };

      for (int i = 0; i < N; i++) {
        service.execute(task);
      }

      latch.await();
      service.shutdown();

      Verify.incrementCounter(COUNTER_SUCCESS);

    } else { 
      assert Verify.getCounter(COUNTER_SUCCESS) > 0 : "never succeeded";
      assert Verify.getCounter(COUNTER_EXCHANGED) > 0 : "never exchanged";
    }
  }
}
