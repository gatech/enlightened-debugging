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



package gov.nasa.jpf.search.heuristic;

import gov.nasa.jpf.Config;

import java.util.Iterator;
import java.util.TreeSet;


@SuppressWarnings("serial")
public class StaticPriorityQueue extends TreeSet<PrioritizedState> {

  int maxQueueSize;
  
  public StaticPriorityQueue (Config config) {


    maxQueueSize = config.getInt("search.heuristic.queue_limit", 1024);
    if (maxQueueSize < 0){
      maxQueueSize = Integer.MAX_VALUE;
    }
  }
    
  @Override
  public boolean add (PrioritizedState s) {
    if (size() < maxQueueSize) { 
      return super.add(s);
      
    } else {
      PrioritizedState last = last();
      if (s.compareTo(last) < 0) {

        remove(last);
        
        return super.add(s);
        
      } else {

        return false;
      }
    }
  }
  
  public boolean isQueueLimitReached() {
    return size() >= maxQueueSize;
  }
  

  void dump() {
    int i=0;
    System.err.print('[');
    for (Iterator<PrioritizedState> it=iterator(); it.hasNext();) {
      if (i++ > 0) {
        System.err.print(',');
      }
      System.err.print(it.next());
    }
    System.err.println(']');
  }
}
