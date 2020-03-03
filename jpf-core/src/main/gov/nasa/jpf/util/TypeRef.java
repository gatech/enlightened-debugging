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

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;


public class TypeRef {
  String clsName;

  public TypeRef (String clsName){
    this.clsName = clsName;
  }
  
  public Class<?> getNativeClass() throws ClassNotFoundException {
    return Class.forName(clsName);
  }

  
  public <T> Class<? extends T> asNativeSubclass(Class<T> superClazz) throws ClassNotFoundException, ClassCastException {
    Class<?> clazz = Class.forName(clsName);
    return clazz.asSubclass(superClazz);
  }

  
  public ClassInfo getClassInfo (){
    return ClassLoaderInfo.getCurrentResolvedClassInfo(clsName);
  }
  
  @Override
  public String toString(){
    return "TypeRef(" + clsName + ")";
  }
}
