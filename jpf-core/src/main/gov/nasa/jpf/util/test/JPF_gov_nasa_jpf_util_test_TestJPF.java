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



package gov.nasa.jpf.util.test;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DirectCallStackFrame;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.ArrayList;


public class JPF_gov_nasa_jpf_util_test_TestJPF extends NativePeer {

  ClassInfo testClass;
  MethodInfo testClassCtor;

  MethodInfo[] testMethods = null;
  int index = 0;
  int testObjRef = MJIEnv.NULL;

  boolean done;

  private static void pushDirectCallFrame(MJIEnv env, MethodInfo mi, int objRef) {
    ThreadInfo ti = env.getThreadInfo();

    DirectCallStackFrame frame = mi.createDirectCallStackFrame(ti, 0);
    frame.setReferenceArgument( 0, objRef, null);
    ti.pushFrame(frame);
  }

  private boolean initializeTestMethods(MJIEnv env, String[] selectedTests) {
    if (selectedTests != null && selectedTests.length > 0) {
      testMethods = new MethodInfo[selectedTests.length];
      int i = 0;
      for (String test : selectedTests) {
        MethodInfo mi = testClass.getMethod(test + "()V", false);
        if (mi != null && mi.isPublic() && !mi.isStatic()) {
          testMethods[i++] = mi;
        } else {
          env.throwException("java.lang.RuntimeException",
                  "no such test method: public void " + test + "()");
          return false;
        }
      }
    } else { 
      ArrayList<MethodInfo> list = new ArrayList<MethodInfo>();
      for (MethodInfo mi : testClass) {
        if (mi.getName().startsWith("test") && mi.isPublic() && !mi.isStatic() &&
                mi.getSignature().equals("()V")) {
          list.add(mi);
        }
      }
      testMethods = list.toArray(new MethodInfo[list.size()]);
    }

    return true;
  }



  public JPF_gov_nasa_jpf_util_test_TestJPF () {
    done = false;
    index = 0;
    testObjRef = MJIEnv.NULL;
    testMethods = null;
    testClass = null;
    testClassCtor = null;
  }

  @MJI
  public void $init____V (MJIEnv env, int objRef){

  }

  @MJI
  public void runTestsOfThisClass___3Ljava_lang_String_2__V (MJIEnv env, int clsObjRef,
                                                                    int selectedTestsRef) {
    ThreadInfo ti = env.getThreadInfo();

    if (!done) {
      if (testMethods == null) {
        StackFrame frame = env.getCallerStackFrame(); 

        testClass = frame.getClassInfo();
        testClassCtor = testClass.getMethod("<init>()V", true);

        String[] selectedTests = env.getStringArrayObject(selectedTestsRef);
        if (initializeTestMethods(env, selectedTests)) {
          env.repeatInvocation();
        }

      } else { 
        if (testObjRef == MJIEnv.NULL) { 
          testObjRef = env.newObject(testClass);

          if (testClassCtor != null) {
            pushDirectCallFrame(env, testClassCtor, testObjRef);
            env.repeatInvocation();
          }

        } else { 
          if (testMethods != null && (index < testMethods.length)) {
            MethodInfo miTest = testMethods[index++];
            pushDirectCallFrame(env, miTest, testObjRef);

            if (index < testMethods.length) {
              testObjRef = MJIEnv.NULL;
            } else {
              done = true;
            }

            env.repeatInvocation();
          }
        }
      }
    }
  }

  @MJI
  public int createAndRunJPF__Ljava_lang_StackTraceElement_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef, int a1, int a2){

    return MJIEnv.NULL;
  }

  @MJI
  public int getProperty__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef, int keyRef){
    String key = env.getStringObject(keyRef);
    String val = env.getConfig().getString(key);
    
    if (val != null){
      return env.newString(val);
    } else {
      return MJIEnv.NULL;
    }
  }
  
  
  @MJI
  public boolean isJPFRun____Z (MJIEnv env, int clsObjRef){
    return true;
  }

  @MJI
  public boolean isJUnitRun____Z (MJIEnv env, int clsObjRef){
    return false;
  }

  @MJI
  public boolean isRunTestRun____Z (MJIEnv env, int clsObjRef){
    return false;
  }




  @MJI
  public int noPropertyViolation___3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }

  @MJI
  public boolean verifyNoPropertyViolation___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public boolean verifyAssertionErrorDetails__Ljava_lang_String_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int detailsRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public boolean verifyAssertionError___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public int unhandledException__Ljava_lang_String_2Ljava_lang_String_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef,
                                  int xClassNameRef, int detailsRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }

  @MJI
  public boolean verifyUnhandledException__Ljava_lang_String_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int xClassNameRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public boolean verifyUnhandledExceptionDetails__Ljava_lang_String_2Ljava_lang_String_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int xClassNameRef, int detailsRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public int propertyViolation__Ljava_lang_Class_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef,
                                  int propClsRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }

  @MJI
  public boolean verifyPropertyViolation__Lgov_nasa_jpf_util_TypeRef_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int propClsRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public int jpfException__Ljava_lang_Class_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef,
                                  int xClsRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }

  @MJI
  public boolean verifyJPFException__Lgov_nasa_jpf_util_TypeRef_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int xClsRef, int jpfArgsRef){
    return true;
  }

  @MJI
  public int deadlock___3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }

  @MJI
  public boolean verifyDeadlock___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }


}
