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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.SystemAttribute;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.SparseIntVector;
import gov.nasa.jpf.util.StringSetMatcher;


public class ClassLoaderInfo 
     implements Iterable<ClassInfo>, Comparable<ClassLoaderInfo>, Cloneable, Restorable<ClassLoaderInfo> {

  static JPFLogger log = JPF.getLogger("class");

  

  protected static final String ID_FIELD = "nativeId";

  protected static Config config;


  protected static SparseIntVector globalCLids;
  
  
  protected static Map<String,ClassInfo> loadedClasses;
  
  
  protected static Map<String,AnnotationInfo> loadedAnnotations;
  


  protected Map<String,ClassInfo> resolvedClasses;


  protected Map<String,AnnotationInfo> resolvedAnnotations;
  

  protected ClassPath cp;


  protected ClassInfo classInfo;


  protected Statics statics;

  protected boolean roundTripRequired = false;


  protected int id;


  protected int objRef;

  protected ClassLoaderInfo parent;

  
  static class ClMemento implements Memento<ClassLoaderInfo> {

    ClassLoaderInfo cl;
    Memento<Statics> staticsMemento;
    Memento<ClassPath> cpMemento;
    Map<String, Boolean> classAssertionStatus;
    Map<String, Boolean> packageAssertionStatus;
    boolean defaultAssertionStatus;
    boolean isDefaultSet;

    ClMemento (ClassLoaderInfo cl){
      this.cl = cl;
      staticsMemento = cl.statics.getMemento();
      cpMemento = cl.cp.getMemento();
      classAssertionStatus = new HashMap<String, Boolean>(cl.classAssertionStatus);
      packageAssertionStatus = new HashMap<String, Boolean>(cl.packageAssertionStatus);
      defaultAssertionStatus = cl.defaultAssertionStatus;
      isDefaultSet = cl.isDefaultSet;
    }

    @Override
	public ClassLoaderInfo restore(ClassLoaderInfo ignored) {
      staticsMemento.restore(cl.statics);
      cpMemento.restore(null);
      cl.classAssertionStatus = this.classAssertionStatus;
      cl.packageAssertionStatus = this.packageAssertionStatus;
      cl.defaultAssertionStatus = this.defaultAssertionStatus;
      cl.isDefaultSet = this.isDefaultSet;
      return cl;
    }
  }

  
  static void init (Config config) {
    ClassLoaderInfo.config = config;

    globalCLids = new SparseIntVector();
    loadedClasses = new HashMap<String,ClassInfo>(); 
    loadedAnnotations = new HashMap<String,AnnotationInfo>();
    
    enabledAssertionPatterns = StringSetMatcher.getNonEmpty(config.getStringArray("vm.enable_assertions"));
    disabledAssertionPatterns = StringSetMatcher.getNonEmpty(config.getStringArray("vm.disable_assertions"));
  }
    
  public static int getNumberOfLoadedClasses (){
    return loadedClasses.size();
  }
  
  public static ClassInfo getCurrentResolvedClassInfo (String clsName){
    ClassLoaderInfo cl = getCurrentClassLoader();
    return cl.getResolvedClassInfo(clsName);
  }

  public static ClassInfo getSystemResolvedClassInfo (String clsName){
    ClassLoaderInfo cl = getCurrentSystemClassLoader();
    return cl.getResolvedClassInfo(clsName);
  }
   
  
  protected ClassLoaderInfo (VM vm){
    resolvedClasses = new HashMap<String,ClassInfo>();
    resolvedAnnotations = new HashMap<String,AnnotationInfo>();
    
    this.statics = createStatics(vm);

    cp = new ClassPath();
    


  }
  
  
  protected ClassLoaderInfo (VM vm, int objRef, ClassPath cp, ClassLoaderInfo parent) {
    resolvedClasses = new HashMap<String,ClassInfo>();
    resolvedAnnotations = new HashMap<String,AnnotationInfo>();

    this.parent = parent;
    this.objRef = objRef;
    this.cp = cp;
    this.statics = createStatics(vm);

    this.id = computeId(objRef);
    ElementInfo ei = vm.getModifiableElementInfo(objRef);

    ei.setIntField(ID_FIELD, id);
    if (parent != null) {
      ei.setReferenceField("parent", parent.objRef);
    }
    classInfo = ei.getClassInfo();
    roundTripRequired = isRoundTripRequired();

    vm.registerClassLoader(this);
  }
  
  @Override
  public Memento<ClassLoaderInfo> getMemento (MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<ClassLoaderInfo> getMemento(){
    return new ClMemento(this);
  }

  protected Statics createStatics (VM vm){
    Class<?>[] argTypes = { Config.class, KernelState.class };
    Object[] args = { config, vm.getKernelState() };
    
    return config.getEssentialInstance("vm.statics.class", Statics.class, argTypes, args);
  }
  
  
  public int getId() {
    return id;
  }

  
  public ClassInfo getClassInfo () {
    return classInfo;
  }

  
  public int getClassLoaderObjectRef () {
    return objRef;
  }

  protected int computeId (int objRef) {
    int id = globalCLids.get(objRef);
    if (id == 0) {
      id = globalCLids.size() + 1; 
      globalCLids.set(objRef, id);
    }
    return id;
  }

  
  protected boolean isRoundTripRequired() {
    return (parent!=null? parent.roundTripRequired: true)  || !hasOriginalLoadingImp();
  }

  private boolean hasOriginalLoadingImp() {
    String signature = "(Ljava/lang/String;)Ljava/lang/Class;";
    MethodInfo loadClass = classInfo.getMethod("loadClass" + signature, true);
    MethodInfo findClass = classInfo.getMethod("findClass" + signature, true);
  
    return (loadClass.getClassName().equals("java.lang.ClassLoader") &&
        findClass.getClassName().equals("java.net.URLClassLoader"));
  }

  public boolean isSystemClassLoader() {
    return false;
  }
  
  public static ClassLoaderInfo getCurrentClassLoader() {
    return getCurrentClassLoader( ThreadInfo.getCurrentThread());
  }

  public static ClassLoaderInfo getCurrentClassLoader (ThreadInfo ti) {
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame = frame.getPrevious()){
      MethodInfo miFrame = frame.getMethodInfo();
      ClassInfo ciFrame =  miFrame.getClassInfo();
      if (ciFrame != null){
        return ciFrame.getClassLoaderInfo();
      }
    }

    return ti.getSystemClassLoaderInfo();
  }
  
  public static SystemClassLoaderInfo getCurrentSystemClassLoader() {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    if (ti != null){
      return ti.getSystemClassLoaderInfo();
    } else {


      return SystemClassLoaderInfo.lastInstance;
    }
  }

  public SystemClassLoaderInfo getSystemClassLoader() {
    return getCurrentSystemClassLoader();
  }
  
  protected ClassInfo loadSystemClass (String clsName){
    return getCurrentSystemClassLoader().loadSystemClass(clsName);
  }

  protected ClassInfo createClassInfo (String typeName, ClassFileMatch match, ClassLoaderInfo definingLoader) throws ClassParseException {
    return getCurrentSystemClassLoader().createClassInfo( typeName, match, definingLoader);
  }
  
  protected ClassInfo createClassInfo (String typeName, String url, byte[] data, ClassLoaderInfo definingLoader) throws ClassParseException {
    return getCurrentSystemClassLoader().createClassInfo( typeName, url, data, definingLoader);
  }

  protected void setAttributes (ClassInfo ci){
    getCurrentSystemClassLoader().setAttributes(ci);
  }
  
  
  
  public ClassInfo getResolvedClassInfo (String className) throws ClassInfoException {
    String typeName = Types.getClassNameFromTypeName( className);
    
    ClassInfo ci = resolvedClasses.get( typeName);
    if (ci == null) {
      if (ClassInfo.isBuiltinClass( typeName)){
        ci = loadSystemClass( typeName);

      } else {
        ClassFileMatch match = getMatch( typeName);
        if (match != null){
          String url = match.getClassURL();
          ci = loadedClasses.get( url); 
          if (ci != null){
            if (ci.getClassLoaderInfo() != this){ 
              ci = ci.cloneFor(this);
            }
          } else {
            try {
              log.info("loading class ", typeName, " from ",  url);
              ci = match.createClassInfo(this);
              
            } catch (ClassParseException cpx){
              throw new ClassInfoException( "error parsing class", this, "java.lang.NoClassDefFoundError", typeName, cpx);
            }
            
            loadedClasses.put( url, ci);
          }
          
        } else { 
          throw new ClassInfoException("class not found: " + typeName, this, "java.lang.ClassNotFoundException", typeName);
        }
      }
      
      setAttributes(ci);
      resolvedClasses.put(typeName, ci);
    }
    
    return ci;
  }
  
  
  public ClassInfo getResolvedClassInfo (String className, byte[] data, int offset, int length) throws ClassInfoException {
    String typeName = Types.getClassNameFromTypeName( className);
    ClassInfo ci = resolvedClasses.get( typeName);    
    
    if (ci == null) {        
      try {

        String url = typeName; 
        SystemClassLoaderInfo sysCl = getCurrentSystemClassLoader();
        ci = sysCl.createClassInfo(typeName, url, data, this);



      } catch (ClassParseException cpx) {
        throw new ClassInfoException("error parsing class", this, "java.lang.NoClassDefFoundError", typeName, cpx);
      }

      setAttributes(ci);
      resolvedClasses.put( typeName, ci);
    }
    
    return ci;
  }
    
  public AnnotationInfo getResolvedAnnotationInfo (String typeName) throws ClassInfoException {
    AnnotationInfo ai = resolvedAnnotations.get(typeName);
    
    if (ai == null){
      ClassFileMatch match = getMatch( typeName);
      if (match != null){
        String url = match.getClassURL();
        ai = loadedAnnotations.get(url); 
        if (ai != null) {
          if (ai.getClassLoaderInfo() != this) { 
            ai = ai.cloneFor(this);
          }
          
        } else {
          try {
            ai = match.createAnnotationInfo(this);
            
          } catch (ClassParseException cpx) {
            throw new ClassInfoException("error parsing class", this, "java.lang.NoClassDefFoundError", typeName, cpx);
          }
            
          loadedAnnotations.put( url, ai);
        } 
        
      } else { 
        throw new ClassInfoException("class not found: " + typeName, this, "java.lang.ClassNotFoundException", typeName);
      }
      
      resolvedAnnotations.put( typeName, ai);
    }
    
    return ai;
  }
  
  public ClassInfo getResolvedAnnotationProxy (ClassInfo ciAnnotation){
    String typeName = ciAnnotation.getName() + "$Proxy";
    
    ClassInfo ci = resolvedClasses.get( typeName);
    if (ci == null) {
      ci = ciAnnotation.createAnnotationProxy(typeName);      
      resolvedClasses.put( typeName, ci);
    }

    return ci;
  }

  
  public ClassInfo getResolvedFuncObjType (int bsIdx, ClassInfo fiClassInfo, String samUniqueName, BootstrapMethodInfo bmi, String[] freeVariableTypeNames) {
    String typeName = bmi.enclosingClass.getName() + "$$Lambda$" + bsIdx;
    
    ClassInfo funcObjType = resolvedClasses.get( typeName);
    
    if (funcObjType == null) {
      funcObjType = fiClassInfo.createFuncObjClassInfo(bmi, typeName, samUniqueName, freeVariableTypeNames);
      resolvedClasses.put( typeName, funcObjType);
    }
    
    return funcObjType;
  }
  
  protected ClassInfo getAlreadyResolvedClassInfo(String cname) {
    return resolvedClasses.get(cname);
  }

  protected void addResolvedClass(ClassInfo ci) {
    resolvedClasses.put(ci.getName(), ci);
  }

  protected boolean hasResolved(String cname) {
    return (resolvedClasses.get(cname)!=null);
  }

  
  public ClassInfo getInitializedClassInfo (String clsName, ThreadInfo ti){
    ClassInfo ci = getResolvedClassInfo(clsName);
    ci.initializeClassAtomic(ti);
    return ci;
  }

  
  public ClassInfo tryGetResolvedClassInfo (String className){
    try {
      return getResolvedClassInfo(className);
    } catch (ClassInfoException cx){
      return null;
    }
  }

  public ClassInfo getClassInfo (int id) {
    ElementInfo ei = statics.get(id);
    if (ei != null) {
      return ei.getClassInfo();
    } else {
      return null;
    }
  }


  public ClassInfo loadClass(String cname) {
    ClassInfo ci = null;
    if(roundTripRequired) {

      ci = loadClassOnJPF(cname);
    } else {


      ci = loadClassOnJVM(cname);
    }

    return ci;
  }

  protected ClassInfo loadClassOnJVM(String cname) {
    String className = Types.getClassNameFromTypeName(cname);

    ClassInfo ci = getAlreadyResolvedClassInfo(className);

    if (ci == null) {
      try {
        if(parent != null) {
          ci = parent.loadClassOnJVM(cname);
        } else {
          ClassLoaderInfo systemClassLoader = getCurrentSystemClassLoader();
          ci = systemClassLoader.getResolvedClassInfo(cname);
        }
      } catch(ClassInfoException cie) {
        if(cie.getExceptionClass().equals("java.lang.ClassNotFoundException")) {
          ci = getResolvedClassInfo(cname);
        } else {
          throw cie;
        }
      }
    }

    return ci;
  }


  class LoadClassRequest implements SystemAttribute {
    String typeName;
    
    LoadClassRequest (String typeName){
      this.typeName = typeName;
    }
    
    boolean isRequestFor( String typeName){
      return this.typeName.equals( typeName);
    }
  }
  
  protected ClassInfo loadClassOnJPF (String typeName) {
    String className = Types.getClassNameFromTypeName(typeName);

    ClassInfo ci = getAlreadyResolvedClassInfo(className);

    if(ci != null) { 
      return ci;
      
    } else {   
      ThreadInfo ti = VM.getVM().getCurrentThread();  
      StackFrame frame = ti.getReturnedDirectCall();
      
      if (frame != null){ 
        LoadClassRequest a = frame.getFrameAttr(LoadClassRequest.class);
        if (a != null && a.isRequestFor(typeName)){ 
          int clsObjRef = frame.pop();

          if (clsObjRef == MJIEnv.NULL) {
            throw new ClassInfoException("class not found: " + typeName, this, "java.lang.NoClassDefFoundError", typeName);
          } else {
            return ti.getEnv().getReferredClassInfo(clsObjRef);
          }          
        }
      }
      

      pushloadClassFrame(typeName);
      throw new LoadOnJPFRequired(typeName);
    }
  }

  protected void pushloadClassFrame (String typeName) {
    ThreadInfo ti = VM.getVM().getCurrentThread();


    ClassInfo clClass = VM.getVM().getClassInfo(objRef);


    MethodInfo miLoadClass = clClass.getMethod("loadClass(Ljava/lang/String;)Ljava/lang/Class;", true);


    DirectCallStackFrame frame = miLoadClass.createDirectCallStackFrame( ti, 0);

    String clsName = typeName.replace('/', '.');
    int sRef = ti.getEnv().newString( clsName);
    int argOffset = frame.setReferenceArgument( 0, objRef, null);
    frame.setReferenceArgument( argOffset, sRef, null);

    frame.setFrameAttr( new LoadClassRequest(typeName));
    
    ti.pushFrame(frame);
  }

  protected ClassInfo getDefinedClassInfo(String typeName){
    ClassInfo ci = resolvedClasses.get(typeName);
    if(ci != null && ci.classLoader == this) {
      return ci;
    } else {
      return null;
    }
  }
  
  public ElementInfo getElementInfo (String typeName) {
    ClassInfo ci = resolvedClasses.get(typeName);
    if (ci != null) {
      ClassLoaderInfo cli = ci.classLoader;
      Statics st = cli.statics;
      return st.get(ci.getId());
      
    } else {
      return null; 
    }
  }

  public ElementInfo getModifiableElementInfo (String typeName) {
    ClassInfo ci = resolvedClasses.get(typeName);
    if (ci != null) {
      ClassLoaderInfo cli = ci.classLoader;
      Statics st = cli.statics;
      return st.getModifiable(ci.getId());
      
    } else {
      return null; 
    }
  }

  protected ClassFileMatch getMatch(String typeName) {
    if(ClassInfo.isBuiltinClass(typeName)) {
      return null;
    }

    ClassFileMatch match;
    try {
      match = cp.findMatch(typeName); 
    } catch (ClassParseException cfx){
      throw new JPFException("error reading class " + typeName, cfx);
    }

    return match;
  }

  
  public String findResource (String resourceName){
    for (String cpe : getClassPathElements()) {
      String URL = getResourceURL(cpe, resourceName);
      if(URL != null) {
        return URL;
      }
    }
    return null;
  }

  
  public String[] findResources (String resourceName){
    ArrayList<String> resources = new ArrayList(0);
    for (String cpe : getClassPathElements()) {
      String URL = getResourceURL(cpe, resourceName);
      if(URL != null) {
        if(!resources.contains(URL)) {
          resources.add(URL);
        }
      }
    }
    return resources.toArray(new String[resources.size()]);
  }
  
  protected String getResourceURL(String path, String resource) {
    if(resource != null) {
      try {
        if (path.endsWith(".jar")){
          JarFile jar = new JarFile(path);
          JarEntry e = jar.getJarEntry(resource);
          if (e != null){
            File f = new File(path);
            return "jar:" + f.toURI().toURL().toString() + "!/" + resource;
          }
        } else {
          File f = new File(path, resource);
          if (f.exists()){
            return f.toURI().toURL().toString();
          }
        }
      } catch (MalformedURLException mfx){
        return null;
      } catch (IOException iox){
        return null;
      }
    }

    return null;
  }

  public Statics getStatics() {
    return statics;
  }

  public ClassPath getClassPath() {
    return cp;
  }

  public String[] getClassPathElements() {
    return cp.getPathNames();
  }

  protected ClassFileContainer createClassFileContainer (String path){
    return getCurrentSystemClassLoader().createClassFileContainer(path);
  }
  
  public void addClassPathElement (String path){
    ClassFileContainer cfc = createClassFileContainer(path);
    
    if (cfc != null){
      cp.addClassFileContainer(cfc);
    } else {
      log.warning("unknown classpath element: ", path);
    }
  }
  
  
  @Override
  public int compareTo (ClassLoaderInfo that) {
    return this.id - that.id;
  }

  
  @Override
  public Iterator<ClassInfo> iterator () {
    return resolvedClasses.values().iterator();
  }

  
  public boolean isAlive () {
    return true;
  }

  public Map<String, ClassLoaderInfo> getPackages() {
    Map<String, ClassLoaderInfo> pkgs = new HashMap<String, ClassLoaderInfo>();
    for(String cname: resolvedClasses.keySet()) {
      if(!ClassInfo.isBuiltinClass(cname) && cname.indexOf('.')!=-1) {
        pkgs.put(cname.substring(0, cname.lastIndexOf('.')), this);
      }
    }

    Map<String, ClassLoaderInfo> parentPkgs = null;
    if(parent!=null) {
      parentPkgs = parent.getPackages();
    }

    if (parentPkgs != null) {
      for (String pName: parentPkgs.keySet()) {
        if (pkgs.get(pName) == null) {
          pkgs.put(pName, parentPkgs.get(pName));
        }
      }
    }
    return pkgs;
  }


  

  static StringSetMatcher enabledAssertionPatterns;
  static StringSetMatcher disabledAssertionPatterns;

  protected Map<String, Boolean> classAssertionStatus = new HashMap<String, Boolean>();
  protected Map<String, Boolean> packageAssertionStatus = new HashMap<String, Boolean>();
  protected boolean defaultAssertionStatus = false;
  protected boolean isDefaultSet = false;

  protected boolean desiredAssertionStatus(String cname) {

    Boolean result = classAssertionStatus.get(cname);
    if (result != null) {
      return result.booleanValue();
    }


    int dotIndex = cname.lastIndexOf(".");
    if (dotIndex < 0) { 
      result = packageAssertionStatus.get(null);
      if (result != null) {
        return result.booleanValue();
      }
    }

    if(dotIndex > 0) {
      String pkgName = cname;
      while(dotIndex > 0) { 
        pkgName = pkgName.substring(0, dotIndex);
        result = packageAssertionStatus.get(pkgName);
        if (result != null) {
          return result.booleanValue();
        }
        dotIndex = pkgName.lastIndexOf(".", dotIndex-1);
      }
    }



    if(isDefaultSet) {
      return defaultAssertionStatus;
    } else {
      return StringSetMatcher.isMatch(cname, enabledAssertionPatterns, disabledAssertionPatterns);
    }
  }

  public void setDefaultAssertionStatus(boolean enabled) {
    isDefaultSet = true;
    defaultAssertionStatus = enabled;
  }

  public void setClassAssertionStatus(String cname, boolean enabled) {
    classAssertionStatus.put(cname, enabled);
  }

  public void setPackageAssertionStatus(String pname, boolean enabled) {
    packageAssertionStatus.put(pname, enabled);
  }

  public void clearAssertionStatus() {
    classAssertionStatus = new HashMap<String, Boolean>();
    packageAssertionStatus = new HashMap<String, Boolean>();
    defaultAssertionStatus = false;
  }
}
