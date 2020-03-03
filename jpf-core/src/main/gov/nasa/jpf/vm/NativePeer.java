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
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.util.JPFLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;



public class NativePeer implements Cloneable {

  static final String MODEL_PACKAGE = "<model>";
  static final String DEFAULT_PACKAGE = "<default>";

  static JPFLogger logger = JPF.getLogger("class");

  static ClassLoader loader;
  static HashMap<ClassInfo, NativePeer> peers;
  static Config config;
  static boolean noOrphanMethods;

  static String[] peerPackages;

  ClassInfo ci;
  Class<?> peerClass;
  HashMap<String, Method> methods;


  public static boolean init (Config conf) {
    loader = conf.getClassLoader();
    peers = new HashMap<ClassInfo, NativePeer>();

    peerPackages = getPeerPackages(conf);

    config = conf;
    noOrphanMethods = conf.getBoolean("vm.no_orphan_methods", false);

    return true;
  }

  static String[] getPeerPackages (Config conf) {
    String[] defPeerPackages = { MODEL_PACKAGE, "gov.nasa.jpf.vm", DEFAULT_PACKAGE };
    String[] packages = conf.getStringArray("peer_packages", defPeerPackages);


    for (int i=0; i<packages.length; i++) {
      if (packages[i].equals(MODEL_PACKAGE)) {
        packages[i] = MODEL_PACKAGE;
      } else if (packages[i].equals(DEFAULT_PACKAGE)) {
        packages[i] = DEFAULT_PACKAGE;
      }
    }

    return packages;
  }

  static Class<?> locatePeerCls (String clsName) {
    String cn = "JPF_" + clsName.replace('.', '_');

    for (int i=0; i<peerPackages.length; i++) {
      String pcn;
      String pkg = peerPackages[i];

      if (pkg == MODEL_PACKAGE) {
        int j = clsName.lastIndexOf('.');
        pcn = clsName.substring(0, j+1) + cn;
      } else if (pkg == DEFAULT_PACKAGE) {
        pcn = cn;
      } else {
        pcn = pkg + '.' + cn;
      }
     
      try {
        Class<?> peerCls = loader.loadClass(pcn);
        
        if ((peerCls.getModifiers() & Modifier.PUBLIC) == 0) {
          logger.warning("non-public peer class: ", pcn);
          continue; 
        }
        
        logger.info("loaded peer class: ", pcn);
        
        return peerCls;
      } catch (ClassNotFoundException cnfx) {

      }
    }

    return null; 
  }

  
  static NativePeer getNativePeer (ClassInfo ci) {
    String     clsName = ci.getName();
    NativePeer peer = peers.get(ci);
    Class<?>      peerCls = null;

    if (peer == null) {
      peerCls = locatePeerCls(clsName);

      if (peerCls != null) {
        initializePeerClass( peerCls);
                
        if (logger.isLoggable(Level.INFO)) {
          logger.info("load peer: ", peerCls.getName());
        }

        peer = getInstance(peerCls, NativePeer.class);
        peer.initialize(peerCls, ci, true);

        peers.put(ci, peer);
      }
    }

    return peer;
  }

  public static <T> T getInstance(Class<?> cls, Class<T> type) throws JPFException {
    Class<?>[] argTypes = Config.CONFIG_ARGTYPES;
    Object[] args = config.CONFIG_ARGS;

    return getInstance(cls, type, argTypes, args);
  }

  public static <T> T getInstance(Class<?> cls, Class<T> type, Class<?>[] argTypes,
                     Object[] args) throws JPFException {
    Object o = null;
    Constructor<?> ctor = null;

    if (cls == null) {
      return null;
    }

    while (o == null) {
      try {
        ctor = cls.getConstructor(argTypes);
        o = ctor.newInstance(args);
      } catch (NoSuchMethodException nmx) {
         
        if ((argTypes.length > 1) || ((argTypes.length == 1) && (argTypes[0] != Config.class))) {

          argTypes = Config.CONFIG_ARGTYPES;
          args = config.CONFIG_ARGS;
        } else if (argTypes.length > 0) {

          argTypes = Config.NO_ARGTYPES;
          args = Config.NO_ARGS;

        } else {

          throw new JPFException("no suitable ctor found for the peer class " + cls.getName());
        }
      } catch (IllegalAccessException iacc) {
        throw new JPFException("ctor not accessible: "
            + config.getMethodSignature(ctor));
      } catch (IllegalArgumentException iarg) {
        throw new JPFException("illegal constructor arguments: "
            + config.getMethodSignature(ctor));
      } catch (InvocationTargetException ix) {
        Throwable tx = ix.getCause();
        throw new JPFException("exception " + tx + " occured in " 
            + config.getMethodSignature(ctor));
      } catch (InstantiationException ivt) {
        throw new JPFException("abstract class cannot be instantiated");
      } catch (ExceptionInInitializerError eie) {
        throw new JPFException("static initialization failed:\n>> "
            + eie.getException(), eie.getException());
      }
    }


    if (!cls.isInstance(o)) {
      throw new JPFException("instance not of type: "
          + cls.getName());
    }

    return type.cast(o); 
  }

  static String getPeerDispatcherClassName (String clsName) {
    return (clsName + '$');
  }

  public Class<?> getPeerClass() {
    return peerClass;
  }

  public String getPeerClassName() {
    return peerClass.getName();
  }

  public void initialize (Class<?> peerClass, ClassInfo ci, boolean cacheMethods) {
    if ((this.ci != null) || (this.peerClass != null)) {
      throw new RuntimeException("cannot re-initialize NativePeer: " +
                                 peerClass.getName());
    }

    this.ci = ci;
    this.peerClass = peerClass;

    loadMethods(cacheMethods);
  }

  protected static void initializePeerClass( Class<?> cls) {
    try {
      Method m = cls.getDeclaredMethod("init", Config.class );
      try {
        m.invoke(null, config);
      } catch (IllegalArgumentException iax){

      } catch (IllegalAccessException iacx) {
        throw new RuntimeException("peer initialization method not accessible: "
                                   + cls.getName());
      } catch (InvocationTargetException itx){
        throw new RuntimeException("initialization of peer " +
                                   cls.getName() + " failed: " + itx.getCause());

      }
    } catch (NoSuchMethodException nsmx){

    }
  }

  private static boolean isMJICandidate (Method mth) {


    if(!mth.isAnnotationPresent(MJI.class)) {
      return false;
    }


    if(!Modifier.isPublic(mth.getModifiers())) {
      return false;
    }


    Class<?>[] argTypes = mth.getParameterTypes();
    if ((argTypes.length >= 2) && (argTypes[0] == MJIEnv.class) && (argTypes[1] == int.class) ) {
      return true;
    } else {
      return false;
    }
  }


  private Method getMethod (MethodInfo mi) {
    return getMethod(null, mi);
  }

  private Method getMethod (String prefix, MethodInfo mi) {
    String name = mi.getUniqueName();

    if (prefix != null) {
      name = prefix + name;
    }

    return methods.get(name);
  }

  
  protected void loadMethods (boolean cacheMethods) {


    Method[] m = peerClass.getMethods();
    
    methods = new HashMap<String, Method>(m.length);

    Map<String,MethodInfo> methodInfos = ci.getDeclaredMethods();
    MethodInfo[] mis = null;

    for (int i = 0; i < m.length; i++) {
      Method  mth = m[i];

      if (isMJICandidate(mth)) {




        String mn = mth.getName();



        if (mn.startsWith("$clinit")) {
          mn = "<clinit>";
        } else if (mn.startsWith("$init")) {
          mn = "<init>" + mn.substring(5);
        }

        String mname = Types.getJNIMethodName(mn);
        String sig = Types.getJNISignature(mn);

        if (sig != null) {
          mname += sig;
        }







        MethodInfo mi = methodInfos.get(mname);

        if ((mi == null) && (sig == null)) {



          if (mis == null) { 
            mis = new MethodInfo[methodInfos.size()];
            methodInfos.values().toArray(mis);
          }

          mi = searchMethod(mname, mis);
        }

        if (mi != null) {
          logger.info("load MJI method: ", mname);

          NativeMethodInfo miNative = new NativeMethodInfo(mi, mth, this);
          miNative.replace(mi);

        } else {
          checkOrphan(mth, mname);
        }
      }
    }
  }

  protected void checkOrphan (Method mth, String mname){
    if (!ignoreOrphan(mth)) {




      Class<?> implCls = mth.getDeclaringClass();
      if (implCls != peerClass) {
        ClassInfo ciSuper = ci.getSuperClass();
        if (ciSuper != null){
          MethodInfo mi = ciSuper.getMethod(mname, true);
          if (mi != null){
            if (mi instanceof NativeMethodInfo){
              NativeMethodInfo nmi = (NativeMethodInfo)mi;
              if (nmi.getMethod().equals(mth)){
                return;
              }
            }
          }
        }
      }

      String message = "orphan NativePeer method: " + ci.getName() + '.' + mname;

      if (noOrphanMethods) {
        throw new JPFException(message);
      } else {



        logger.warning(message);
      }
    }
  }
  
  protected boolean ignoreOrphan (Method m){
    MJI annotation = m.getAnnotation(MJI.class);
    return annotation.noOrphanWarning();
  }
  
  private MethodInfo searchMethod (String mname, MethodInfo[] methods) {
    int idx = -1;

    for (int j = 0; j < methods.length; j++) {
      if (methods[j].getName().equals(mname)) {




        if (idx == -1) {
          idx = j;
        } else {
          throw new JPFException("overloaded native method without signature: " + ci.getName() + '.' + mname);
        }
      }
    }

    if (idx >= 0) {
      return methods[idx];
    } else {
      return null;
    }
  }
  
  
  protected static DynamicDependency getInvocationCondition(MJIEnv env) {
  	NativeStackFrame nativeFrame = null;
  	StackFrame current = env.getThreadInfo().getTopFrame();
  	while (current != null) {
  		if (current instanceof NativeStackFrame) {
  			nativeFrame = (NativeStackFrame) current;
  			break;
  		}
  		current = current.getPrevious();
  	}
  	if (nativeFrame != null) {
  		MethodInvocationAttr invocAttr = (MethodInvocationAttr) nativeFrame.getFrameAttr();
  		if (invocAttr != null) {
  			return invocAttr.getInvocationCondition();
  		}
  	}
  	return null;
  }
}

