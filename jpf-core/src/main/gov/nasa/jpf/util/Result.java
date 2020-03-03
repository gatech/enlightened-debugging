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


package gov.nasa.jpf.util;


public class Result {
  
  public static final Result OK = new Result();
  

  public final String error;
  

  private Result (){
    error = null;
  }
  
  private Result (String errorMsg){
    error = errorMsg;
  }
  
  @Override
  public boolean equals(Object o){

    if (o instanceof Boolean){
      return (error == null) == (Boolean)o;
    } else if (o instanceof Result){
      return (error == null) == (((Result)o).error == null);
    }
    return false;
  }

  public static Result failure (String errorMsg){
    return new Result(errorMsg);
  }
}
