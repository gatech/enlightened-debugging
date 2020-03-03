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



package gov.nasa.jpf.vm.serialize;

import java.util.Iterator;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadList;


public class CFSerializer extends FilteringSerializer {





  boolean positiveSid;

  int sidCount;

  @Override
  protected void initReferenceQueue() {
    super.initReferenceQueue();

    if (positiveSid){
      positiveSid = false;
      sidCount = -1;
    } else {
      positiveSid = true;
      sidCount = 1;
    }
  }


  protected void queueReference(ElementInfo ei){
    refQueue.add(ei);
  }

  @Override
  public void processReference(int objref) {
    if (objref == MJIEnv.NULL) {
      buf.add(MJIEnv.NULL);

    } else {
      ElementInfo ei = heap.get(objref);
      int sid = ei.getSid();

      if (positiveSid){ 
        if (sid <= 0){  
          sid = sidCount++;
          ei.setSid(sid);
          queueReference(ei);
        }
      } else { 
        if (sid >= 0){ 
          sid = sidCount--;
          ei.setSid(sid);
          queueReference(ei);
        }
        sid = -sid;
      }


      buf.add(sid);
    }
  }
  
  @Override
  protected void serializeStackFrames() {
    ThreadList tl = ks.getThreadList();

    for (Iterator<ThreadInfo> it = tl.canonicalLiveIterator(); it.hasNext(); ) {
      serializeStackFrames(it.next());
    }
  }
  
  @Override
  protected void serializeFrame(StackFrame frame){
    buf.add(frame.getMethodInfo().getGlobalId());

    Instruction pc = frame.getPC();
    buf.add( pc != null ? pc.getInstructionIndex() : -1);

    int len = frame.getTopPos()+1;
    buf.add(len);



    int[] slots = frame.getSlots();
    for (int i = 0; i < len; i++) {
      if (frame.isReferenceSlot(i)) {
        processReference(slots[i]);
      } else {
        buf.add(slots[i]);
      }
    }
  }

  @Override
  protected void processReferenceQueue() {
    refQueue.process(this);
  }
  
  @Override
  protected int getSerializedReferenceValue (ElementInfo ei){
    return Math.abs(ei.getSid());
  }
}
