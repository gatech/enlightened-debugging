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


public class PathSharednessPolicy extends GenericSharednessPolicy {
  
  public PathSharednessPolicy (Config config){
    super(config);
  }
  
  @Override
  public void initializeObjectSharedness (ThreadInfo allocThread, DynamicElementInfo ei) {
    ei.setReferencingThreads( new PersistentTidSet(allocThread));
  }

  @Override
  public void initializeClassSharedness (ThreadInfo allocThread, StaticElementInfo ei) {
    ei.setReferencingThreads( new PersistentTidSet(allocThread));
    ei.setExposed(); 
  }
  
  @Override
  protected FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    int[] lockRefs = ti.getLockedObjectReferences();
    switch (lockRefs.length){
      case 0:
        return FieldLockInfo.getEmptyFieldLockInfo();
      case 1:
        return new PersistentSingleLockThresholdFli(ti, lockRefs[0], lockThreshold);
      default: 
        return new PersistentLockSetThresholdFli(ti, lockRefs, lockThreshold);
    }
  }
  
  @Override
  protected boolean checkOtherRunnables (ThreadInfo ti){


    return ti.hasOtherRunnables();
  }
}
