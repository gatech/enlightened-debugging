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


public class TestEventTree extends EventTree {
  
  protected String[] expected; 
  
  public TestEventTree (){

  }
  
  public TestEventTree (Event root){
    super(root);
  } 
  
  @Override
  public boolean checkPath (Event lastEvent) {
    if (expected != null){
      return checkPath( lastEvent, expected);
    } else {
      System.err.println("warning: trying to check path of " + this + " without 'expected' specification");
      return true; 
    }
  }

  @Override
  public boolean isCompletelyCovered (){
    if (expected != null){
      return isCompletelyCovered(expected);
    } else {
      System.err.println("warning: trying to check coverage of " + this + " without 'expected' specification");
      return true;
    }
  }
  
  public boolean isCompletelyCovered (String[] expected) {
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != null) {

        return false;
      }
    }

    return true; 
  }

  public float getPathCoverage (String[] expected) {
    int n = 0;

    for (int i = 0; i < expected.length; i++) {
      if (expected[i] == null) {
        n++;
      }
    }

    return (float) n / expected.length;
  }
}
