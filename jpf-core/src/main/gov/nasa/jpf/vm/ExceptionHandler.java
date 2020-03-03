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


public class ExceptionHandler {
  
  private String name;

  
  private int begin;

  
  private int end;

  
  private int handler;

  
  public ExceptionHandler (String n, int b, int e, int h) {
    name = n;
    begin = b;
    end = e;
    handler = h;
  }

  
  public int getBegin () {
    return begin;
  }

  
  public int getEnd () {
    return end;
  }

  
  public int getHandler () {
    return handler;
  }

  
  public String getName () {
    return name;
  }


  @Override
  public String toString() {
    return "Handler [name="+name+",from="+begin+",to="+end+",target="+handler+"]";
  }
}
