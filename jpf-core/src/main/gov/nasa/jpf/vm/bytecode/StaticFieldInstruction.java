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



package gov.nasa.jpf.vm.bytecode;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;


public abstract class StaticFieldInstruction extends FieldInstruction {

  protected StaticFieldInstruction(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  
  protected void initialize() {
    ClassInfo ciRef = mi.getClassInfo().resolveReferencedClass(className);
    
    FieldInfo f = ciRef.getStaticField(fname);
    if (f != null){
      ClassInfo ciField = f.getClassInfo();
      if (!ciField.isRegistered()){

        ciField.registerClass(ThreadInfo.getCurrentThread());
        f = ciField.getStaticField(fname);
      }

      fi = f;
    }


  }

  
  @Override
  public ElementInfo getElementInfo(ThreadInfo ti){
    return getFieldInfo().getClassInfo().getStaticElementInfo();
  }
  
  @Override
  public String toPostExecString(){
    StringBuilder sb = new StringBuilder();
    sb.append(getMnemonic());
    sb.append(' ');
    sb.append( fi.getFullName());
    
    return sb.toString();
  }

  public ClassInfo getClassInfo() {
    if (fi == null) {
      initialize();
    }
    return fi.getClassInfo();
  }

  @Override
  public FieldInfo getFieldInfo() {
    if (fi == null) {
      initialize();
    }
    return fi;
  }

  
  @Override
  public ElementInfo peekElementInfo (ThreadInfo ti) {
    return getLastElementInfo();
  }

  @Override
  public StaticElementInfo getLastElementInfo() {
    return getFieldInfo().getClassInfo().getStaticElementInfo();
  }



  public ClassInfo getLastClassInfo(){
    return getFieldInfo().getClassInfo();
  }

  public String getLastClassName() {
    return getLastClassInfo().getName();
  }

}
