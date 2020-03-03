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
import gov.nasa.jpf.util.SparseObjVector;



public class GlobalSharednessPolicy extends GenericSharednessPolicy {

  protected SparseObjVector<ThreadInfoSet> globalTisCache = new SparseObjVector<ThreadInfoSet>(1024);
  protected SparseObjVector<FieldLockInfo> globalFliCache = new SparseObjVector<FieldLockInfo>(1024);
  

  public GlobalSharednessPolicy (Config config){
    super(config);
  }
  
  protected ThreadInfoSet getRegisteredThreadInfoSet (int key, ThreadInfo allocThread) {
    ThreadInfoSet tis = globalTisCache.get(key);
    if (tis == null) {
      tis = new TidSet(allocThread);
      globalTisCache.set(key, tis);
    }
    
    return tis;    
  }
  
  protected FieldLockInfo getRegisteredFieldLockInfo (int key, ThreadInfo ti){
    FieldLockInfo fli = globalFliCache.get(key);
    
    if (fli == null){
      int[] lockRefs = ti.getLockedObjectReferences();
      if (lockRefs.length == 0) {
        fli = FieldLockInfo.getEmptyFieldLockInfo();
      } else if (lockRefs.length == 1){
        fli = new SingleLockThresholdFli(ti, lockRefs[0], lockThreshold);
      } else {
        fli = new PersistentLockSetThresholdFli(ti, lockRefs, lockThreshold);
      }
      
      globalFliCache.set(key, fli);
    }
    
    return fli;
  }
  
  @Override
  protected boolean checkOtherRunnables (ThreadInfo ti){

    return true;
  }
  
  @Override
  public void initializeObjectSharedness (ThreadInfo allocThread, DynamicElementInfo ei) {
    ThreadInfoSet tis = getRegisteredThreadInfoSet(ei.getObjectRef(), allocThread);
    ei.setReferencingThreads( tis);
  }

  @Override
  public void initializeClassSharedness (ThreadInfo allocThread, StaticElementInfo ei) {
    ThreadInfoSet tis;
    int ref = ei.getClassObjectRef();
    if (ref == MJIEnv.NULL) { 



      tis = new TidSet(allocThread);
    } else {
      tis = getRegisteredThreadInfoSet(ref, allocThread);
    }
    
    ei.setReferencingThreads(tis);
    ei.setExposed(); 
  }

  @Override
  protected FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
    int id;
    
    if (ei instanceof StaticElementInfo){
      id = ((StaticElementInfo)ei).getClassObjectRef();
      if (id == MJIEnv.NULL){
        return null;
      }
    } else {
      id = ei.getObjectRef();
    }
    
    return getRegisteredFieldLockInfo( id, ti);
  }
}
