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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.PSIntMap;
import gov.nasa.jpf.util.Predicate;

import java.util.Iterator;


public class PSIMHeap extends GenericSGOIDHeap {
  
  
  static class PSIMMemento extends GenericSGOIDHeapMemento {
    PSIntMap<ElementInfo> eiSnap;
    
    PSIMMemento (PSIMHeap heap) {
      super(heap);
      
      heap.elementInfos.process(ElementInfo.storer);
      eiSnap = heap.elementInfos; 
    }

    @Override
    public Heap restore(Heap inSitu) {
      super.restore( inSitu);
      
      PSIMHeap heap = (PSIMHeap) inSitu;
      heap.elementInfos = eiSnap;
      heap.elementInfos.process(ElementInfo.restorer);
      
      return heap;
    }
  }
  
  class SweepPredicate implements Predicate<ElementInfo>{
    ThreadInfo ti;
    int tid;
    boolean isThreadTermination;
    
    protected void setContext() {
      ti = vm.getCurrentThread();
      tid = ti.getId();
      isThreadTermination = ti.isTerminated();      
    }
    
    @Override
    public boolean isTrue (ElementInfo ei) {
      
      if (ei.isMarked()){ 
        ei.setUnmarked();
        ei.setAlive( liveBitValue);          
        ei.cleanUp( PSIMHeap.this, isThreadTermination, tid);
        return false;
        
      } else { 

        ei.processReleaseActions();

        vm.notifyObjectReleased( ti, ei);
        return true;
      } 
    }
  }
  
  SweepPredicate sweepPredicate;
  PSIntMap<ElementInfo> elementInfos;
  
  
  public PSIMHeap (Config config, KernelState ks) {
    super(config,ks);
    
    elementInfos = new PSIntMap<ElementInfo>();    
    sweepPredicate = new SweepPredicate();
  }
  
  @Override
  public int size() {
    return elementInfos.size();
  }

  @Override
  protected void set(int index, ElementInfo ei) {
    elementInfos = elementInfos.set(index, ei);
  }

  @Override
  public ElementInfo get(int ref) {
    if (ref <= 0) {
      return null;
    } else {      
      return elementInfos.get(ref);
    }
  }

  @Override
  public ElementInfo getModifiable(int ref) {

    
    if (ref <= 0) {
      return null;
    } else {
      ElementInfo ei = elementInfos.get(ref);

      if (ei != null && ei.isFrozen()) {
        ei = ei.deepClone(); 

        elementInfos = elementInfos.set(ref, ei);
      }

      return ei;
    }
  }

  @Override
  protected void remove(int ref) {
    elementInfos = elementInfos.remove(ref);
  }
  
  @Override
  protected void sweep () {
    sweepPredicate.setContext();
    elementInfos = elementInfos.removeAllSatisfying( sweepPredicate);
  }
  
  @Override
  public Iterator<ElementInfo> iterator() {
    return elementInfos.iterator();
  }

  @Override
  public Iterable<ElementInfo> liveObjects() {
    return elementInfos;
  }

  @Override
  public void resetVolatiles() {

  }

  @Override
  public void restoreVolatiles() {

  }

  @Override
  public Memento<Heap> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  @Override
  public Memento<Heap> getMemento() {
    return new PSIMMemento(this);
  }

}
