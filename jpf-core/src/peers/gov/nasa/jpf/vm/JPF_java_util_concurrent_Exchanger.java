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



package gov.nasa.jpf.vm;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.util.InstructionState;


public class JPF_java_util_concurrent_Exchanger extends NativePeer {

  static class ExchangeState extends InstructionState {
    int exchangeRef; 
    boolean isWaiter;

    static ExchangeState createWaiterState (MJIEnv env, int exchangeRef){
      ExchangeState s = new ExchangeState();
      
      s.exchangeRef = exchangeRef;
      s.isWaiter = true;
      return s;
    }
    
    static ExchangeState createResponderState (MJIEnv env, int exchangeRef){
      ExchangeState s = new ExchangeState();
      
      s.exchangeRef = exchangeRef;
      s.isWaiter = false;
      return s;      
    }
  }
  
  ElementInfo createExchangeObject (MJIEnv env, int waiterDataRef) throws ClinitRequired {
    ElementInfo ei = env.newElementInfo("java.util.concurrent.Exchanger$Exchange");
    ei.setReferenceField("waiterData", waiterDataRef);
    ei.setReferenceField("waiterThread", env.getThreadInfo().getThreadObjectRef());
    return ei;
  }
  
  private int repeatInvocation (MJIEnv env, StackFrame frame, ElementInfo exchange, ExchangeState state){
    frame.addFrameAttr(state);
    env.registerPinDown(exchange);
    env.repeatInvocation();
    return MJIEnv.NULL;
  }
  

  
  @MJI
  public int exchange__Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int objRef, int dataRef){
    return exchange0__Ljava_lang_Object_2J__Ljava_lang_Object_2(env, objRef, dataRef, -1L);
  }
    
  @MJI
  public int exchange0__Ljava_lang_Object_2J__Ljava_lang_Object_2 (MJIEnv env, int objRef, int dataRef, long timeoutMillis) {
    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getModifiableTopFrame();
    ExchangeState state = frame.getFrameAttr(ExchangeState.class);
    long to = (timeoutMillis <0) ? 0 : timeoutMillis;

    if (state == null){ 
      int eRef = env.getReferenceField(objRef, "exchange");
      
      if (eRef == MJIEnv.NULL){ 
        ElementInfo eiExchanger;

        try {
          eiExchanger = createExchangeObject(env, dataRef);
        } catch (ClinitRequired x){

          env.repeatInvocation();
          return MJIEnv.NULL;
        }

        eRef = eiExchanger.getObjectRef();
        env.setReferenceField(objRef, "exchange", eRef);
        


        if (timeoutMillis == 0) {
          env.throwException("java.util.concurrent.TimeoutException");
          return MJIEnv.NULL;
        }

        eiExchanger.wait(ti, to, false);  
        
        if (ti.getScheduler().setsWaitCG(ti, to)) {
          return repeatInvocation(env, frame, eiExchanger, ExchangeState.createWaiterState(env, eRef));
        } else {
          throw new JPFException("blocked exchange() waiter without transition break");
        }
        
      } else { 
        ElementInfo ei = ti.getModifiableElementInfo(eRef);        
        ei.setReferenceField("responderData", dataRef);
        state = ExchangeState.createResponderState(env, eRef);
        
        if (ei.getBooleanField("waiterTimedOut")){

          ei.wait(ti, to, false);

          if (ti.getScheduler().setsWaitCG(ti, to)) {
            return repeatInvocation(env, frame, ei, state);
          } else {
            throw new JPFException("blocked exchange() responder without transition break");
          }          
        }


                
        boolean didNotify = ei.notifies(env.getSystemState(), ti, false); 
        env.setReferenceField(objRef, "exchange", MJIEnv.NULL); 
                
        if (ti.getScheduler().setsNotifyCG(ti, didNotify)){
          return repeatInvocation(env, frame, ei, state);
        }
        
        return ei.getReferenceField("waiterData");
      }
      
    } else { 
      ElementInfo eiExchanger = env.getElementInfo(state.exchangeRef);

      int retRef = MJIEnv.NULL;
      
      if (ti.isInterrupted(true)) {
        env.throwException("java.lang.InterruptedException");

      } else if (ti.isTimedOut()){
        if (state.isWaiter) {
          eiExchanger = eiExchanger.getModifiableInstance();
          eiExchanger.setBooleanField("waiterTimedOut", true);


          eiExchanger.notifies(env.getSystemState(), ti, false);
        }

        env.throwException("java.util.concurrent.TimeoutException");
        
      } else {
        retRef = eiExchanger.getReferenceField( state.isWaiter ? "responderData" : "waiterData");
      }
      

      frame.removeFrameAttr(state);
      eiExchanger = eiExchanger.getModifiableInstance();
      env.releasePinDown(eiExchanger);
      return retRef;
    }
  }
}
