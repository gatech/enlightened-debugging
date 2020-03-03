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


public abstract class ClassFileMatch {
  public final String typeName;
  public final String url;

  protected ClassFileMatch (String typeName, String url) {
    this.typeName = typeName;
    this.url = url;
  }
  
  public String getClassURL () {
    return url;
  }  
  
  public abstract ClassFileContainer getContainer();



  public abstract ClassInfo createClassInfo (ClassLoaderInfo loader) throws ClassParseException;
  public abstract AnnotationInfo createAnnotationInfo (ClassLoaderInfo loader) throws ClassParseException;
  
}
