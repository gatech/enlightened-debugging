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
import gov.nasa.jpf.util.ObjVector;

import java.util.Iterator;


public class OVHeap extends GenericSGOIDHeap {
  

  static class OVMemento extends GenericSGOIDHeapMemento {
    ObjVector.Snapshot<ElementInfo> eiSnap;
    
    OVMemento(OVHeap heap) {
      super(heap);
      
      heap.elementInfos.process(ElementInfo.storer);      
      eiSnap = heap.elementInfos.getSnapshot();
    }

    @Override
    public Heap restore(Heap inSitu) {
      super.restore( inSitu);
      
      OVHeap heap = (OVHeap)inSitu;
      heap.elementInfos.restore(eiSnap);      
      heap.elementInfos.process(ElementInfo.restorer);
      
      return heap;
    }
  }
  

  
  ObjVector<ElementInfo> elementInfos;
  
  

  
  public OVHeap (Config config, KernelState ks){
    super(config, ks);
    
    elementInfos = new ObjVector<ElementInfo>();
  }
      


  
  @Override
  public int size() {
    return nLiveObjects;
  }
  
  @Override
  protected void set (int index, ElementInfo ei) {
    elementInfos.set(index, ei);
  }

  
  @Override
  public ElementInfo get (int ref) {
    if (ref <= 0) {
      return null;
    } else {
      return elementInfos.get(ref);
    }
  }

  @Override
  public ElementInfo getModifiable (int ref) {
    if (ref <= 0) {
      return null;
    } else {
      ElementInfo ei = elementInfos.get(ref);

      if (ei != null && ei.isFrozen()) {
        ei = ei.deepClone(); 

        elementInfos.set(ref, ei);
      }

      return ei;
    }
  }
    
  @Override
  protected void remove(int ref) {
    elementInfos.remove(ref);
  }

  @Override
  public Iterator<ElementInfo> iterator() {
    return elementInfos.nonNullIterator();
  }

  @Override
  public Iterable<ElementInfo> liveObjects() {
    return elementInfos.elements();
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
  public Memento<Heap> getMemento(){
    return new OVMemento(this);
  }


}
