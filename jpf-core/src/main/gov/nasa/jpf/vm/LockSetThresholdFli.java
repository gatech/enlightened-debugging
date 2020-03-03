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


public class LockSetThresholdFli extends ThresholdFieldLockInfo {

  protected int[] lockRefSet;


  public LockSetThresholdFli(ThreadInfo ti, int[] currentLockRefs, int checkThreshold) {
    super(checkThreshold);

    tiLastCheck = ti;
    lockRefSet = currentLockRefs;
  }

  @Override
  protected int[] getCandidateLockSet() {
    return lockRefSet;
  }

  
  protected SingleLockThresholdFli singleLockThresholdFli (ThreadInfo ti, int lockRef, int remainingChecks) {
    return new SingleLockThresholdFli(ti, lockRef, remainingChecks);
  }
  
  protected LockSetThresholdFli lockSetThresholdFli (ThreadInfo ti, int[] lockRefs, int remainingChecks){
    this.lockRefSet = lockRefs;
    this.tiLastCheck = ti;
    this.remainingChecks = remainingChecks;
    
    return this;
  }
  
  @Override
  public FieldLockInfo checkProtection(ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
    int[] currentLockRefs = ti.getLockedObjectReferences();
    int nLocks = currentLockRefs.length;
    int nRemaining = Math.max(0, remainingChecks--);

    if (nLocks == 0) { 
      checkFailedLockAssumption(ti, ei, fi);
      return empty;

    } else { 
      int l = 0;
      int[] newLset = new int[lockRefSet.length];

      for (int i = 0; i < nLocks; i++) { 
        int leidx = currentLockRefs[i];

        for (int j = 0; j < lockRefSet.length; j++) {
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
        return singleLockThresholdFli(ti, newLset[0], nRemaining);

      } else if (l < newLset.length) { 
        int[] newLockRefSet = new int[l];
        System.arraycopy(newLset, 0, newLockRefSet, 0, l);
        return lockSetThresholdFli(ti, newLockRefSet, nRemaining);

      } else {
        return lockSetThresholdFli(ti, lockRefSet, nRemaining);
      }
    }
  }

  
  @Override
  public FieldLockInfo cleanUp(Heap heap) {
    int[] newSet = null;
    int l = 0;

    if (lockRefSet != null) {
      for (int i = 0; i < lockRefSet.length; i++) {
        ElementInfo ei = heap.get(lockRefSet[i]);

        if (!heap.isAlive(ei)) { 
          if (newSet == null) { 
            newSet = new int[lockRefSet.length - 1];
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
      return new SingleLockThresholdFli(tiLastCheck, newSet[0], remainingChecks);

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
    sb.append("LockSetThresholdFli {");
    sb.append("remainingChecks=");
    sb.append(remainingChecks);
    sb.append(",lset=[");
    if (lockRefSet != null) {
      for (int i = 0; i < lockRefSet.length; i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(lockRefSet[i]);
      }
    }
    sb.append("]}");

    return sb.toString();
  }
}
