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


public class ClassInfoException extends RuntimeException{

  ClassLoaderInfo classLoader;
  String exceptionClass; 
  String failedClass;

  public ClassInfoException(String details, ClassLoaderInfo cl, String exceptionClass, String faildClass) {
    super(details);
    this.classLoader = cl;
    this.exceptionClass = exceptionClass;
    this.failedClass = faildClass;
  }

  public ClassInfoException (String details, ClassLoaderInfo cl, String exceptionClass, String faildClass, Throwable cause) {
    super(details, cause);
    this.classLoader = cl;
    this.exceptionClass = exceptionClass;
    this.failedClass = faildClass;
  }

  
  public boolean checkSystemClassFailure() {
    return (failedClass.startsWith("java."));
  }

  public ClassLoaderInfo getClassLoaderInfo() {
    return classLoader;
  }

  public String getFailedClass() {
    return failedClass;
  }

  public String getExceptionClass() {
    return exceptionClass;
  }
}
