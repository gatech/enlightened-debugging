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

import gov.nasa.jpf.util.HashData;



public class ThreadData {
  
  ThreadInfo.State state;

  
  int priority;

  
  String name = "?";

  
  boolean isDaemon;

  
  int lockCount;

  
  int suspendCount;


  @Override
  public ThreadData clone () {
    ThreadData t = new ThreadData();

    t.state = state;
    t.lockCount = lockCount;
    t.suspendCount = suspendCount;

    t.priority = priority;
    t.name = name;
    t.isDaemon = isDaemon;

    return t;
  }

  @Override
  public boolean equals (Object o) {
    if ((o == null) || !(o instanceof ThreadData)) {
      return false;
    }

    ThreadData t = (ThreadData) o;

    return ((state == t.state) && 
            (priority == t.priority) &&
            (isDaemon == t.isDaemon) && 
            (lockCount == t.lockCount) &&
            (suspendCount == t.suspendCount) && 
            (name.equals(t.name)));
  }

  public void hash (HashData hd) {
    hd.add(state);
    hd.add(lockCount);
    hd.add(suspendCount);
    hd.add(priority);
    hd.add(isDaemon);
    hd.add(name);
  }

  @Override
  public int hashCode () {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }

  @Override
  public String toString () {
    return ("ThreadData{" + getFieldValues() + '}');
  }

  public String getFieldValues () {
    StringBuilder sb = new StringBuilder("name:");

    sb.append(name);
    sb.append(",status:");
    sb.append(state.name());
    sb.append(",priority:");
    sb.append(priority);
    sb.append(",isDaemon:");
    sb.append(isDaemon);
    sb.append(",lockCount:");
    sb.append(lockCount);
    sb.append(",suspendCount:");
    sb.append(suspendCount);

    return sb.toString();
  }

  public ThreadInfo.State getState() { return state; }
}
