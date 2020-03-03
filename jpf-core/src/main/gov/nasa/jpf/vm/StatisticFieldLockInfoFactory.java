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
import gov.nasa.jpf.JPF;

import java.util.logging.Logger;



public class StatisticFieldLockInfoFactory implements FieldLockInfoFactory {

  static Logger log = JPF.getLogger("gov.nasa.jpf.vm.FieldLockInfo");
  
  
  static int CHECK_THRESHOLD = 5;
  
  
  static boolean PINDOWN = false;

  
  static boolean AGRESSIVE = false;
  
  
  public StatisticFieldLockInfoFactory (Config conf) {
    CHECK_THRESHOLD = conf.getInt("vm.por.sync_detection.threshold", CHECK_THRESHOLD);
    PINDOWN = conf.getBoolean("vm.por.sync_detection.pindown", PINDOWN);
    AGRESSIVE = conf.getBoolean("vm.por.sync_detection.agressive",AGRESSIVE);    
  }
  
  @Override
  public FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
    int[] currentLockRefs = ti.getLockedObjectReferences();
    int nLocks = currentLockRefs.length;

    if (nLocks == 0) {
      return FieldLockInfo.empty; 
      
    } else {
      
      if (AGRESSIVE) {
        int lockCandidateRef = strongProtectionCandidate(ei,fi,currentLockRefs);
        if (lockCandidateRef != MJIEnv.NULL) {

          return new SingleLockFli( ti, lockCandidateRef, CHECK_THRESHOLD);
        }
      }
      
      if (nLocks == 1) { 
        return new SingleLockFli( ti, currentLockRefs[0], 0);
      
      } else {
        return new MultiLockFli( ti, fi, currentLockRefs);
      }
    }
  }

  
  int strongProtectionCandidate (ElementInfo ei, FieldInfo fi, int[] currentLockRefs) {
    int n = currentLockRefs.length;
    Heap heap = VM.getVM().getHeap();

    if (fi.isStatic()) { 
      ClassInfo ci = fi.getClassInfo();
      int cref = ci.getClassObjectRef();

      for (int i=0; i<n; i++) {
        if (currentLockRefs[i] == cref) {
          ElementInfo e = heap.get(cref);
          log.info("sync-detection: " + ei + " assumed to be synced on class object: @" + e);
          return cref;
        }
      }

    } else { 
      int objRef = ei.getObjectRef();
      
      for (int i=0; i<n; i++) {
        int eidx = currentLockRefs[i];


        if (eidx == objRef) {
          log.info("sync-detection: " + ei + " assumed to be synced on itself");
          return objRef;
        }

        ElementInfo e = heap.get(eidx);
        

        if (ei.hasRefField(eidx)) {
          log.info("sync-detection: "+ ei + " assumed to be synced on sibling: " + e);
          return eidx;
        }
        

        if (e.hasRefField(objRef)) {
          log.info("sync-detection: " + ei + " assumed to be synced on object wrapper: " + e);
          return eidx;
        }
      }
    }

    return -1;
  }

  
  

  static abstract class StatisticFieldLockInfo extends FieldLockInfo {
    int checkLevel;

    @Override
	public boolean isProtected () {
      return (checkLevel >= CHECK_THRESHOLD);
    }

    @Override
	public boolean needsPindown (ElementInfo ei) {
      return PINDOWN && (checkLevel >= CHECK_THRESHOLD);
    }

    protected void checkFailedLockAssumption(ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
      if (checkLevel >= CHECK_THRESHOLD) {
        lockAssumptionFailed(ti,ei,fi);
      }
    }
  }
  

  static class SingleLockFli extends StatisticFieldLockInfo {
    int lockRef;
    
    SingleLockFli (ThreadInfo ti, int lockRef, int nChecks) {
      tiLastCheck = ti;

      this.lockRef = lockRef;
      checkLevel = nChecks;
    }

    @Override
	protected int[] getCandidateLockSet() {
      int[] set = { lockRef };
      return set;
    }
    

    @Override
	public FieldLockInfo checkProtection (ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
      int[] currentLockRefs = ti.getLockedObjectReferences();
      int nLocks = currentLockRefs.length;
      
      checkLevel++;
      
      for (int i=0; i<nLocks; i++) {
        if (currentLockRefs[i] == lockRef) {
          return this;
        }
      }
      
      checkFailedLockAssumption(ti, ei, fi);
      return empty;
    }

    
    @Override
	public FieldLockInfo cleanUp (Heap heap) {
      ElementInfo ei = heap.get(lockRef);
      if (!heap.isAlive(ei)) {
        return FieldLockInfo.empty;
      } else {
        return this;
      }
    }

    @Override
	public String toString() {
      return ("SingleLockFli {checkLevel="+checkLevel+",lock="+lockRef + '}');
    }  
  }
  
  

  static class MultiLockFli extends StatisticFieldLockInfo {

    int[] lockRefSet;
      

    public MultiLockFli (ThreadInfo ti, FieldInfo fi, int[] currentLockRefs) {
      lockRefSet = currentLockRefs;
    }
    
    @Override
	protected int[] getCandidateLockSet() {
      return lockRefSet;
    }
      

    @Override
	public FieldLockInfo checkProtection (ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
      int[] currentLockRefs = ti.getLockedObjectReferences();      
      int nLocks = currentLockRefs.length;
          
      checkLevel++;

      if (nLocks == 0) { 
        checkFailedLockAssumption(ti, ei, fi);
        return empty;

      } else { 
        int l =0;
        int[] newLset = new int[lockRefSet.length];

        for (int i=0; i<nLocks; i++) { 
          int leidx = currentLockRefs[i];

          for (int j=0; j<lockRefSet.length; j++) {
            if (lockRefSet[j] == leidx) {
              newLset[l++] = leidx;
              break; 
            }
          }
        }

        if (l == 0) { 
          checkFailedLockAssumption(ti, ei, fi);
          return empty;
          
        } else if (l == 1) { 
          return new SingleLockFli( ti, newLset[0], checkLevel);
        
        } else if (l < newLset.length) { 
          lockRefSet = new int[l];
          System.arraycopy(newLset, 0, lockRefSet, 0, l);
          
        } else {

        }
      }

      tiLastCheck = ti;
      return this;
    }

    
    @Override
	public FieldLockInfo cleanUp (Heap heap) {
      int[] newSet = null;
      int l = 0;

      if (lockRefSet != null) {
        for (int i=0; i<lockRefSet.length; i++) {
          ElementInfo ei = heap.get(lockRefSet[i]);

          if (!heap.isAlive(ei)) { 
            if (newSet == null) { 
              newSet = new int[lockRefSet.length-1];
              if (i > 0) {
                System.arraycopy(lockRefSet, 0, newSet, 0, i);
                l = i;
              }
            }

          } else {
            if (newSet != null) { 
              newSet[l++] = lockRefSet[i];
            }
          }
        }
      }

      if (l == 1) {
          assert (newSet != null);
          return new SingleLockFli(tiLastCheck, newSet[0], checkLevel);
          
      } else {
        if (newSet != null) {
          if (l == newSet.length) { 
            lockRefSet = newSet;
          } else { 
            if (l == 0) {
              return empty;
            } else {
              lockRefSet = new int[l];
              System.arraycopy(newSet, 0, lockRefSet, 0, l);
            }
          }
        }
        return this;
      }
    }

    @Override
	public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MultiLockFli {");
      sb.append("checkLevel=");
      sb.append(checkLevel);
      sb.append(",lset=[");
      if (lockRefSet != null) {
        for (int i=0; i<lockRefSet.length; i++) {
          if (i>0) {
            sb.append(',');
          }
          sb.append(lockRefSet[i]);
        }
      }
      sb.append("]}");

      return sb.toString();
    }
  }  
}
