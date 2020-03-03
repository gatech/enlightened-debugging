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

import java.lang.reflect.Modifier;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.util.MethodInfoRegistry;
import gov.nasa.jpf.util.RunListener;
import gov.nasa.jpf.util.RunRegistry;


public class JPF_java_lang_reflect_Constructor extends NativePeer {
  
  static MethodInfoRegistry registry;
  
  public static boolean init (Config conf) {




    
    if (registry == null){
      registry = new MethodInfoRegistry();
      
      RunRegistry.getDefaultRegistry().addListener( new RunListener() {
        @Override
		public void reset (RunRegistry reg){
          registry = null;
        }
      });
    }
    return true;
  }

  static int createConstructorObject (MJIEnv env, ClassInfo ciCtor, MethodInfo mi){

    
    int regIdx = registry.registerMethodInfo(mi);
    int eidx = env.newObject(ciCtor);
    ElementInfo ei = env.getModifiableElementInfo(eidx);
    
    ei.setIntField("regIdx", regIdx);
    return eidx;
  }

  static MethodInfo getMethodInfo (MJIEnv env, int objRef){
    return registry.getMethodInfo(env,objRef, "regIdx");
  }
  
  @MJI
  public int getName____Ljava_lang_String_2 (MJIEnv env, int objRef) {
    MethodInfo mi = getMethodInfo(env, objRef);
    
    int nameRef = env.getReferenceField( objRef, "name");
    if (nameRef == MJIEnv.NULL) {
      nameRef = env.newString(mi.getName());
      env.setReferenceField(objRef, "name", nameRef);
    }
   
    return nameRef;
  }
  


  @MJI
  public int newInstance___3Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int mthRef, int argsRef) {
    ThreadInfo ti = env.getThreadInfo();
    DirectCallStackFrame frame = ti.getReturnedDirectCall();
    MethodInfo miCallee = getMethodInfo(env,mthRef);

    if (frame == null) { 
      ClassInfo ci = miCallee.getClassInfo();

       if (ci.isAbstract()){
        env.throwException("java.lang.InstantiationException");
        return MJIEnv.NULL;
      }

      int objRef = env.newObjectOfUncheckedClass( ci);
      frame = miCallee.createDirectCallStackFrame( ti, 1);
      frame.setReflection();
      
      frame.setLocalReferenceVariable(0, objRef);  
      
      int argOffset = frame.setReferenceArgument(0, objRef, null);
      if (!JPF_java_lang_reflect_Method.pushUnboxedArguments( env, miCallee, frame, argOffset, argsRef)) {

        return MJIEnv.NULL;
      }
      ti.pushFrame(frame);
       
      ci.initializeClass(ti);
      
      env.repeatInvocation();
      return MJIEnv.NULL;
      
    } else { 
      int objRef = frame.getLocalVariable(0); 
      return objRef;
    }
  }
    
  @MJI
  public int getParameterTypes_____3Ljava_lang_Class_2 (MJIEnv env, int objRef){


    return JPF_java_lang_reflect_Method.getParameterTypes (env, getMethodInfo(env,objRef));
  }

  @MJI
  public int getAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int objRef){

    return JPF_java_lang_reflect_Method.getAnnotations( env, getMethodInfo(env,objRef));
  }
  
  @MJI
  public int getDeclaredAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int objRef){
    return JPF_java_lang_reflect_Method.getDeclaredAnnotations( env, getMethodInfo(env,objRef));
  }
  
  @MJI
  public int getAnnotation__Ljava_lang_Class_2__Ljava_lang_annotation_Annotation_2 (MJIEnv env, int objRef, int annotationClsRef) {
    return JPF_java_lang_reflect_Method.getAnnotation( env, getMethodInfo(env,objRef), annotationClsRef);
  }
  
  @MJI
  public int getParameterAnnotations_____3_3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int objRef){
    return JPF_java_lang_reflect_Method.getParameterAnnotations( env, getMethodInfo(env,objRef));
  }

  @MJI
  public int getModifiers____I (MJIEnv env, int objRef){
    MethodInfo mi = getMethodInfo(env, objRef);
    env.setReturnAttribute(env.getArgAttributes()[0]);
    return mi.getModifiers();
  }

  @MJI
  public int getDeclaringClass____Ljava_lang_Class_2 (MJIEnv env, int objRef){
    MethodInfo mi = getMethodInfo(env, objRef);    
    ClassInfo ci = mi.getClassInfo();

    return ci.getClassObjectRef();
  }
  
  @MJI
  public int toString____Ljava_lang_String_2 (MJIEnv env, int objRef){
    StringBuilder sb = new StringBuilder();
    
    MethodInfo mi = getMethodInfo(env, objRef);

    sb.append(Modifier.toString(mi.getModifiers()));
    sb.append(' ');

    sb.append(mi.getClassInfo().getName());
    sb.append('(');
    
    String[] at = mi.getArgumentTypeNames();
    for (int i=0; i<at.length; i++){
      if (i>0) sb.append(',');
      sb.append(at[i]);
    }
    
    sb.append(')');
    
    int sref = env.newString(sb.toString());
    return sref;
  }

  @MJI
  public boolean equals__Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int mthRef){
    ElementInfo ei = env.getElementInfo(mthRef);
    ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(JPF_java_lang_Class.CONSTRUCTOR_CLASSNAME);

    if (ei.getClassInfo() == ci){
      MethodInfo mi1 = getMethodInfo(env, objRef);
      MethodInfo mi2 = getMethodInfo(env, mthRef);
      if (mi1.getClassInfo() == mi2.getClassInfo()){
        if (mi1.getName().equals(mi2.getName())){
          if (mi1.getReturnType().equals(mi2.getReturnType())){
            byte[] params1 = mi1.getArgumentTypes();
            byte[] params2 = mi2.getArgumentTypes();
            if (params1.length == params2.length){
              for (int i = 0; i < params1.length; i++){
                if (params1[i] != params2[i])
                  return false;
              }
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  @MJI
  public int hashCode____I (MJIEnv env, int objRef){
    MethodInfo ctor = getMethodInfo(env, objRef);
    return ctor.getClassName().hashCode();
  }
}
