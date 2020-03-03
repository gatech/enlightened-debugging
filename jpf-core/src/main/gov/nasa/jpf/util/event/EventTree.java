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



package gov.nasa.jpf.util.event;

import java.util.ArrayList;
import java.util.List;


public class EventTree implements EventConstructor {
  
  public static final String CONFIG_KEY = "event.tree.class";
  
  protected Event root;

  
  public Event createRoot() {

    return null;
  }

  protected EventTree () {
    root = createRoot();
  }

  protected EventTree (Event root){
    this.root = root;
  }
  
  public Event getRoot(){
    return root;
  }
    


  static final List<Event> NO_EVENTS = new ArrayList<Event>(0);
  
  public List<Event> visibleEndEvents(){
    if (root != null){
      return root.visibleEndEvents();
    } else {
      return NO_EVENTS;
    }
  }
  
  public void printPaths(){
    for (Event es : visibleEndEvents()){
      es.printPath(System.out);
      System.out.println('.');
    }
  }

  public void printTree (){
    if (root != null){
      root.printTree(System.out, 0);
    }
  }

  
  public boolean checkPath (Event lastEvent){
    for (Event ee : root.visibleEndEvents()){
      if (ee.equals(lastEvent)){
        return true;
      }
    }
    
    return false;
  }
  
  public boolean checkPath (Event lastEvent, String[] pathSpecs) {
    String trace = lastEvent.getPathString(null);

    for (int i = 0; i < pathSpecs.length; i++) {
      if (trace.equals(pathSpecs[i])) {
        pathSpecs[i] = null;
        return true;
      }
    }

    return false; 
  }
  
  
  public float getPathCoverage (){
    throw new UnsupportedOperationException("path coverage not supported by generic EventTree");
  }
  
  
  public boolean isCompletelyCovered (){
    throw new UnsupportedOperationException("path coverage not supported by generic EventTree");
  }
  
  
  public void addPath (Event... path){
    if (root != null){
      root.addPath(path.length, path);
    } else {
      root = sequence(path);
    }
  }
  
  public Event interleave (Event... otherTrees){
    if (root != null){
      return root.interleave( otherTrees);
      
    } else {
      if (otherTrees == null || otherTrees.length == 0){
        return root;
        
      } else {
        Event first = null;
        List<Event> rest = new ArrayList<Event>();
        for (int i=0; i< otherTrees.length; i++){
          if (otherTrees[i] != null){
            if (first == null){
              first = otherTrees[i];
            } else {
              rest.add( otherTrees[i]);
            }
          }
        }
        
        if (first != null){
          if (rest.isEmpty()){
            return first;
          } else {
            Event[] ot = new Event[rest.size()];
            rest.toArray(ot);
            return first.interleave(ot);
          }
          
        } else {      
          return null;
        }
      }
    }
  }

  public EventTree interleave (EventTree... otherTrees){
    Event[] otherRoots = new Event[otherTrees.length];
    for (int i=0; i<otherRoots.length; i++){
      otherRoots[i] = otherTrees[i].root;
    }
    return new EventTree( interleave( otherRoots));
  }  
  
  public Event removeSource (Object source){
    if (root != null){
      return root.removeSource(source);
      
    } else { 
      return null;
    }
  }
  
  public int getMaxDepth(){
    if (root != null){
      return root.getMaxDepth();
    } else {
      return 0;
    }
  }
  
}
