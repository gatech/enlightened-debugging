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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.annotation.MJI;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import anonymous.domain.enlighten.deptrack.ArrayProperty;
import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.ObjectProperty;


public class JPF_java_lang_System extends NativePeer {
  
  @MJI
  public void arraycopy__Ljava_lang_Object_2ILjava_lang_Object_2II__V (MJIEnv env, int clsObjRef,
                                                                              int srcArrayRef, int srcIdx, 
                                                                              int dstArrayRef, int dstIdx,
                                                                              int length) {
  	env.setDependencyTracked(true);
  	Object[] argAttrs = env.getArgAttributes();

  	DynamicDependency[] deps = new DynamicDependency[6];
  	for (int i = 0; i < 5; ++i) {
  		deps[i] = (DynamicDependency) argAttrs[i];
  	}
  	deps[5] = env.getInvocationCondition();
    if ((srcArrayRef == MJIEnv.NULL) || (dstArrayRef == MJIEnv.NULL)) {
    	DynamicDependency exDep = null;
    	if (srcArrayRef == MJIEnv.NULL) {
    		exDep = DynDepBuilder.newBuilder()
    			.appendDataDependency(deps[0]).setControlDependency(deps[5]).build();
    	} else {
    		exDep = DynDepBuilder.newBuilder()
      			.appendDataDependency(deps[2]).setControlDependency(deps[5]).build();
    	}
      env.throwException("java.lang.NullPointerException", exDep);
      return;
    }

    ElementInfo eiSrc = env.getElementInfo(srcArrayRef);
    ElementInfo eiDst = env.getModifiableElementInfo(dstArrayRef);
    
    try {
      eiDst.copyElements( env.getThreadInfo(), eiSrc ,srcIdx, dstIdx, length, deps);
    } catch (IndexOutOfBoundsException iobx){
    	DynamicDependency exDep = null;
    	if (srcIdx < 0) {
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(deps[1]).setControlDependency(deps[5]).build();
    	} else if (dstIdx < 0) {
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(deps[3]).setControlDependency(deps[5]).build();
    	} else if (length < 0) {
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(deps[4]).setControlDependency(deps[5]).build();
    	} else if (eiSrc.arrayLength() < srcIdx + length) {
    		DynamicDependency srcRefDep = deps[0];
    		ArrayProperty srcArrayProp = (ArrayProperty) eiSrc.getObjectAttr();
    		DynamicDependency srcArrayLengthDep = 
    				srcArrayProp != null? srcArrayProp.getArrayLengthDependency() : null;
    		DynamicDependency srcIdxDep = deps[1];
    		DynamicDependency lengthDep = deps[4];
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(srcRefDep, srcArrayLengthDep, srcIdxDep, lengthDep)
    				.setControlDependency(deps[5]).build();
    	} else { 
    		DynamicDependency dstRefDep = deps[2];
    		ArrayProperty dstArrayProp = (ArrayProperty) eiDst.getObjectAttr();
    		DynamicDependency dstArrayLengthDep = 
    				dstArrayProp != null? dstArrayProp.getArrayLengthDependency() : null;
    		DynamicDependency dstIdxDep = deps[3];
    		DynamicDependency lengthDep = deps[4];
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(dstRefDep, dstArrayLengthDep, dstIdxDep, lengthDep)
    				.setControlDependency(deps[5]).build();
    	}
      env.throwException("java.lang.IndexOutOfBoundsException", iobx.getMessage(), exDep);
    } catch (ArrayStoreException asx){
    	String errMsg = asx.getMessage();
    	DynamicDependency exDep = null;
    	if (errMsg.startsWith("destination object not an array")) {
    		ObjectProperty srcObjProp = (ObjectProperty) eiSrc.getObjectAttr();
    		DynamicDependency typeDep = srcObjProp != null ? srcObjProp.getTypeDependency() : null;
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(typeDep).setControlDependency(deps[5]).build();
    	} else if (errMsg.startsWith("source object not an array")) {
    		ObjectProperty dstObjProp = (ObjectProperty) eiDst.getObjectAttr();
    		DynamicDependency typeDep = dstObjProp != null ? dstObjProp.getTypeDependency() : null;
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(typeDep).setControlDependency(deps[5]).build();
    	} else {
    		ObjectProperty srcObjProp = (ObjectProperty) eiSrc.getObjectAttr();
    		ObjectProperty dstObjProp = (ObjectProperty) eiDst.getObjectAttr();
    		DynamicDependency srcTypeDep = srcObjProp != null ? srcObjProp.getTypeDependency() : null;
    		DynamicDependency dstTypeDep = dstObjProp != null ? dstObjProp.getTypeDependency() : null;
    		exDep = DynDepBuilder.newBuilder()
    				.appendDataDependency(srcTypeDep, dstTypeDep).setControlDependency(deps[5]).build();
    	}
      env.throwException("java.lang.ArrayStoreException", errMsg, exDep);      
    }
  }

  @MJI
  public int getenv__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
                                                                         int keyRef){
    String k = env.getStringObject(keyRef);
    String v = System.getenv(k);
    
    if (v == null){
      return MJIEnv.NULL;
    } else {
      return env.newString(v);
    }
  }

  
  int createPrintStream (MJIEnv env, int clsObjRef){
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();
    StackFrame frame = ti.getTopFrame();
    ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo("gov.nasa.jpf.ConsoleOutputStream");

    if (ci.initializeClass(ti)) {
      env.repeatInvocation();
      return MJIEnv.NULL;
    }

    return env.newObject(ci);
  }
  
  @MJI
  public int createSystemOut____Ljava_io_PrintStream_2 (MJIEnv env, int clsObjRef){
  	env.setDependencyTracked(true);
    return createPrintStream(env,clsObjRef);
  }
  
  @MJI
  public int createSystemErr____Ljava_io_PrintStream_2 (MJIEnv env, int clsObjRef){
  	env.setDependencyTracked(true);
    return createPrintStream(env,clsObjRef);
  }
  
  int getProperties (MJIEnv env, Properties p){
    int n = p.size() * 2;
    int aref = env.newObjectArray("Ljava/lang/String;", n);
    int i=0;
    
    for (Map.Entry<Object,Object> e : p.entrySet() ){
      env.setReferenceArrayElement(aref,i++, 
                                   env.newString(e.getKey().toString()));
      env.setReferenceArrayElement(aref,i++,
                                   env.newString(e.getValue().toString()));
    }
    
    return aref;
  }

  int getSysPropsFromHost (MJIEnv env){
    return getProperties(env, System.getProperties());
  }
  
  int getSysPropsFromFile (MJIEnv env){
    Config conf = env.getConfig();
    
    String cf = conf.getString("vm.sysprop.file", "system.properties");
    if (cf != null){
      try {
        Properties p = new Properties();
        FileInputStream fis = new FileInputStream(cf);
        p.load(fis);
        
        return getProperties(env, p);
        
      } catch (IOException iox){
        return MJIEnv.NULL;
      }
    }

    return MJIEnv.NULL;
  }
  
  static String JAVA_CLASS_PATH = "java.class.path";
  
  @MJI
  public String getSUTJavaClassPath(VM vm) {
    ClassInfo system = ClassLoaderInfo.getSystemResolvedClassInfo("java.lang.System");
    
    ThreadInfo ti = vm.getCurrentThread();
    Heap heap = vm.getHeap();
    ElementInfo eiClassPath = heap.newString(JAVA_CLASS_PATH, ti);
    
    MethodInfo miGetProperty = system.getMethod("getProperty(Ljava/lang/String;)Ljava/lang/String;", true);
    DirectCallStackFrame frame = miGetProperty.createDirectCallStackFrame(ti, 0);
    frame.setReferenceArgument( 0, eiClassPath.getObjectRef(), null);
    frame.setFireWall(); 
    
    
    try {
      ti.executeMethodHidden(frame);
      
    } catch (UncaughtException e) {
       ti.clearPendingException();
       StackFrame caller = ti.popAndGetModifiableTopFrame();
       caller.advancePC();
       return null;
    }
    
    int ref = frame.peek();
    ElementInfo metaResult = heap.get(ref);
    String result = metaResult.asString();
    
    return result;
  }
  
  int getSelectedSysPropsFromHost (MJIEnv env){
    Config conf = env.getConfig();
    String keys[] = conf.getStringArray("vm.sysprop.keys");

    if (keys == null){
      String[] defKeys = {
        "path.separator",
        "line.separator", 
        "file.separator",
        "user.name",
        "user.dir",
        "user.timezone",
        "user.country",
        "java.home",
        "java.version",
        "java.io.tmpdir",
        JAVA_CLASS_PATH


      };
      keys = defKeys;
    }
    
    int aref = env.newObjectArray("Ljava/lang/String;", keys.length * 2);
    int i=0;
    
    for (String s : keys) {
      String v;
      
      int idx = s.indexOf('/');
      if (idx >0){ 
        v = s.substring(idx+1);
        s = s.substring(0,idx);
        
      } else {

        if (s == JAVA_CLASS_PATH) {


          ClassPath cp = ClassLoaderInfo.getCurrentClassLoader().getClassPath();

          v = cp.toString();
          
        } else { 
          v = System.getProperty(s);
        }
      }
            
      if (v != null){
        env.setReferenceArrayElement(aref,i++, env.newString(s));
        env.setReferenceArrayElement(aref,i++, env.newString(v));
      }
    }
        
    return aref;
  }

  
  static enum SystemPropertyPolicy {
    SELECTED,  
    FILE, 
    HOST
  };

  @MJI
  public int getKeyValuePairs_____3Ljava_lang_String_2 (MJIEnv env, int clsObjRef){
    Config conf = env.getConfig();
    SystemPropertyPolicy sysPropSrc = conf.getEnum( "vm.sysprop.source", SystemPropertyPolicy.values(), SystemPropertyPolicy.SELECTED);

    if (sysPropSrc == SystemPropertyPolicy.FILE){
      return getSysPropsFromFile(env);
    } else if (sysPropSrc == SystemPropertyPolicy.HOST){
      return getSysPropsFromHost(env);
    } else if (sysPropSrc == SystemPropertyPolicy.SELECTED){
      return getSelectedSysPropsFromHost(env);
    }
    
    return 0;
  }
  




  @MJI
  public long currentTimeMillis____J (MJIEnv env, int clsObjRef) {
    return env.currentTimeMillis();
  }


  @MJI
  public long nanoTime____J (MJIEnv env, int clsObjRef) {
    return env.nanoTime();
  }  
  


  @MJI
  public void exit__I__V (MJIEnv env, int clsObjRef, int ret) {
    ThreadInfo ti = env.getThreadInfo();
    env.getVM().terminateProcess(ti);
  }

  @MJI
  public void gc____V (MJIEnv env, int clsObjRef) {
    env.getSystemState().activateGC();
  }

  @MJI
  public int identityHashCode__Ljava_lang_Object_2__I (MJIEnv env, int clsObjRef, int objref) {
  	DynamicDependency resultDep = DynDepBuilder.newBuilder()
  			.appendDataDependency(env.getArgAttributes()[0])
  			.setControlDependency(getInvocationCondition(env)).build();
  	env.setReturnAttribute(resultDep);
    return (objref ^ 0xABCD);
  }
  
}
