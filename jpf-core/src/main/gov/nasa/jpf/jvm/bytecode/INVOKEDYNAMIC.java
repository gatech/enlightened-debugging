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


package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.BootstrapMethodInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FunctionObjectFactory;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;


public class INVOKEDYNAMIC extends DependencyTrackingInstruction {
  


  int bootstrapMethodIndex;
  



  String[] freeVariableTypeNames;
  byte[] freeVariableTypes;
  
  String functionalInterfaceName;
  
  String samMethodName;
  
  int funcObjRef = MJIEnv.NULL;
  
  ElementInfo lastFuncObj = null;
  
  public INVOKEDYNAMIC () {}

  protected INVOKEDYNAMIC (int bmIndex, String methodName, String descriptor){
    bootstrapMethodIndex = bmIndex;
    samMethodName = methodName;
    freeVariableTypeNames = Types.getArgumentTypeNames(descriptor);
    freeVariableTypes = Types.getArgumentTypes(descriptor);
    functionalInterfaceName = Types.getReturnTypeSignature(descriptor);
  }

  @Override
  public int getByteCode () {
    return 0xBA;
  }
  
  @Override
  public String toString() {
    String args = "";
    for(String type: freeVariableTypeNames) {
      if(args.length()>0) {
        type += ','+ type;
      }
      args += type;
    }
    return "invokedynamic " + bootstrapMethodIndex + " " + 
    samMethodName + '(' + args +"):" + functionalInterfaceName;
  }

  
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    
    ElementInfo ei = ti.getHeap().get(funcObjRef);
    
    if(ei==null || ei!=lastFuncObj || freeVariableTypes.length>0) {
      ClassInfo fiClassInfo;


      try {
        fiClassInfo = ti.resolveReferencedClass(functionalInterfaceName);
      } catch(LoadOnJPFRequired lre) {
        return ti.getPC();
      }

      if (fiClassInfo.initializeClass(ti)) {
        return ti.getPC();
      }
      
      ClassInfo enclosingClass = this.getMethodInfo().getClassInfo();
      
      BootstrapMethodInfo bmi = enclosingClass.getBootstrapMethodInfo(bootstrapMethodIndex);
      
      VM vm = VM.getVM();
      FunctionObjectFactory funcObjFactory = vm.getFunctionObjectFacotry();
      
      Object[] freeVariableValues = frame.getArgumentsValues(ti, freeVariableTypes);
      
      funcObjRef = funcObjFactory.getFunctionObject(bootstrapMethodIndex, ti, fiClassInfo, samMethodName, bmi, freeVariableTypeNames, freeVariableValues);
      lastFuncObj = ti.getHeap().get(funcObjRef);
    }
    
    frame.pop(freeVariableTypes.length);
    frame.pushRef(funcObjRef);
    
    return getNext(ti);
  }
}
