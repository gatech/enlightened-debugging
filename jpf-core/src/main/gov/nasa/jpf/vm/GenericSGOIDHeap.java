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
import gov.nasa.jpf.util.IntTable;


public abstract class GenericSGOIDHeap extends GenericHeap {

  static class GenericSGOIDHeapMemento extends GenericHeapMemento {
    IntTable.Snapshot<AllocationContext> ctxSnap;
    
    GenericSGOIDHeapMemento (GenericSGOIDHeap heap) {
      super(heap);
      
      ctxSnap = heap.allocCounts.getSnapshot();
    }

    @Override
    public Heap restore(Heap inSitu) {
      super.restore( inSitu);
      
      GenericSGOIDHeap heap = (GenericSGOIDHeap) inSitu;
      heap.allocCounts.restore(ctxSnap);
      
      return heap;
    }
  }
  

  protected int nextSgoid;
  protected IntTable<Allocation> sgoids;
  


  protected IntTable<AllocationContext> allocCounts;
  
  protected GenericSGOIDHeap (Config config, KernelState ks){
    super(config, ks);
    

    initAllocationContext(config);
    sgoids = new IntTable<Allocation>();
    nextSgoid = 0;
    
    allocCounts = new IntTable<AllocationContext>();
  }
  
  

  
  protected void initAllocationContext(Config config) {
    HashedAllocationContext.init(config);

  }
  


  @Override
  protected AllocationContext getSUTAllocationContext (ClassInfo ci, ThreadInfo ti) {
    return HashedAllocationContext.getSUTAllocationContext(ci, ti);

  }
  @Override
  protected AllocationContext getSystemAllocationContext (ClassInfo ci, ThreadInfo ti, int anchor) {
    return HashedAllocationContext.getSystemAllocationContext(ci, ti, anchor);

  }
  

  @Override
  protected int getNewElementInfoIndex (AllocationContext ctx) {
    int idx;
    int cnt;
    
    IntTable.Entry<AllocationContext> cntEntry = allocCounts.getInc(ctx);
    cnt = cntEntry.val;
    
    Allocation alloc = new Allocation(ctx, cnt);
    
    IntTable.Entry<Allocation> sgoidEntry = sgoids.get(alloc);
    if (sgoidEntry != null) { 
      idx = sgoidEntry.val;
      
    } else { 
      idx = ++nextSgoid;
      sgoids.put(alloc, idx);
    }
    


    
    return idx;
  }

}
