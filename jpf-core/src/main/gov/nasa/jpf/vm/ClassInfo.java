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
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFListener;
import gov.nasa.jpf.util.ImmutableList;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.LocationSpec;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.OATHash;
import gov.nasa.jpf.util.Source;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;



public class ClassInfo extends InfoObject implements Iterable<MethodInfo>, GenericSignatureHolder {









  

  public static final int UNINITIALIZED = -1;


  public static final int INITIALIZED = -2;

  protected static final String ID_FIELD = "nativeId"; 

  protected static JPFLogger logger = JPF.getLogger("class");

  protected static int nClassInfos; 
  
  protected static Config config;

  
  protected static final ClassLoader thisClassLoader = ClassInfo.class.getClassLoader();  
  
  
  protected static FieldsFactory fieldsFactory;

  
  protected static final FieldInfo[] EMPTY_FIELDINFO_ARRAY = new FieldInfo[0];
  protected static final String[] EMPTY_STRING_ARRAY = new String[0];
  protected static final String UNINITIALIZED_STRING = "UNINITIALIZED"; 
  protected static final Map<String,MethodInfo> NO_METHODS = Collections.emptyMap();
  protected static final Set<ClassInfo> NO_INTERFACES = new HashSet<ClassInfo>();
  
  
  protected static HashSet<String> autoloadAnnotations;
  protected static HashSet<String> autoloaded;

  
  protected String name;
  
  
  protected String signature;

  
  protected String genericSignature;

  
  protected ClassLoaderInfo classLoader;
  

  protected boolean      isClass = true;
  protected boolean      isWeakReference = false;
  protected boolean      isObjectClassInfo = false;
  protected boolean      isStringClassInfo = false;
  protected boolean      isThreadClassInfo = false;
  protected boolean      isRefClassInfo = false;
  protected boolean      isArray = false;
  protected boolean      isEnum = false;
  protected boolean      isReferenceArray = false;
  protected boolean      isAbstract = false;
  protected boolean      isBuiltin = false;



  protected int modifiers;

  protected MethodInfo   finalizer = null;

  
  protected int elementInfoAttrs = 0;

  
  protected Map<String, MethodInfo> methods;

  
  protected FieldInfo[] iFields;

  
  protected int instanceDataSize;

  
  protected int instanceDataOffset;

  
  protected int nInstanceFields;

  
  protected FieldInfo[] sFields;

  
  protected int staticDataSize;

  
  protected ClassInfo  superClass;
  protected String superClassName;

  protected String enclosingClassName;
  protected String enclosingMethodName;

  protected String[] innerClassNames = EMPTY_STRING_ARRAY;
  protected BootstrapMethodInfo[] bootstrapMethods;
    
  
  protected String[] interfaceNames;

  protected Set<ClassInfo> interfaces = new HashSet<ClassInfo>();
  
  
  protected Set<ClassInfo> allInterfaces;
  
  
  protected String packageName;

  
  protected String sourceFileName;

   
  protected String classFileUrl;

  
  protected gov.nasa.jpf.vm.ClassFileContainer container;

  
  
  protected int  id = -1;

  
  protected long uniqueId = -1;

  
  protected NativePeer nativePeer;

  
  protected Source source;

  protected boolean enableAssertions;

  
  protected ImmutableList<ReleaseAction> releaseActions; 
          
  
  static boolean init (Config config) {

    ClassInfo.config = config;
    
    setSourceRoots(config);


    fieldsFactory = config.getEssentialInstance("vm.fields_factory.class",
                                                FieldsFactory.class);

    autoloadAnnotations = config.getNonEmptyStringSet("listener.autoload");
    if (autoloadAnnotations != null) {
      autoloaded = new HashSet<String>();

      if (logger.isLoggable(Level.INFO)) {
        for (String s : autoloadAnnotations){
          logger.info("watching for autoload annotation @" + s);
        }
      }
    }

    return true;
  }

  public static boolean isObjectClassInfo (ClassInfo ci){
    return ci.isObjectClassInfo();
  }

  public static boolean isStringClassInfo (ClassInfo ci){
    return ci.isStringClassInfo();
  }

  

    
  protected void setClass(String clsName, String superClsName, int flags, int cpCount) throws ClassParseException {
    String parsedName = Types.getClassNameFromTypeName(clsName);

    if (name != null && !name.equals(parsedName)){
      throw new ClassParseException("wrong class name (expected: " + name + ", found: " + parsedName + ')');
    }
    name = parsedName;
    



    int i = name.lastIndexOf('.');
    packageName = (i > 0) ? name.substring(0, i) : "";

    modifiers = flags;
    

    isClass = ((flags & Modifier.INTERFACE) == 0);

    superClassName = superClsName;
  }

  public void setInnerClassNames(String[] clsNames) {
    innerClassNames = clsNames;
  }

  public void setEnclosingClass (String clsName) {
    enclosingClassName = clsName;
  }
  
  public void setEnclosingMethod (String mthName){
    enclosingMethodName = mthName;    
  }

  public void setInterfaceNames(String[] ifcNames) {
    interfaceNames = ifcNames;
  }
  
  public void setSourceFile (String fileName){

    if (packageName.length() > 0) {

      sourceFileName = packageName.replace('.', '/') + '/' + fileName;
    } else {
      sourceFileName = fileName;
    }
  }

  public void setFields(FieldInfo[] fields) {
    if (fields == null){
      iFields = EMPTY_FIELDINFO_ARRAY;
      sFields = EMPTY_FIELDINFO_ARRAY;
      
    } else { 
      int nInstance = 0, nStatic = 0;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isStatic()) {
          nStatic++;
        } else {
          nInstance++;
        }
      }

      FieldInfo[] instanceFields = (nInstance > 0) ? new FieldInfo[nInstance] : EMPTY_FIELDINFO_ARRAY;
      FieldInfo[] staticFields = (nStatic > 0) ? new FieldInfo[nStatic] : EMPTY_FIELDINFO_ARRAY;

      int iInstance = 0;
      int iStatic = 0;
      for (int i = 0; i < fields.length; i++) {
        FieldInfo fi = fields[i];

        if (fi.isStatic()) {
          staticFields[iStatic++] = fi;
        } else {
          instanceFields[iInstance++] = fi;
        }
        
        processJPFAnnotations(fi);
      }

      iFields = instanceFields;
      sFields = staticFields;


    }
  }

  protected void setMethod (MethodInfo mi){
    mi.linkToClass(this);
    methods.put( mi.getUniqueName(), mi);
    processJPFAnnotations(mi);
  }
  
  public void setMethods (MethodInfo[] newMethods) {
    if (newMethods != null && newMethods.length > 0) {
      methods = new LinkedHashMap<String, MethodInfo>();

      for (int i = 0; i < newMethods.length; i++) {
        setMethod( newMethods[i]);
      }
    }
  }
 
  protected void processJPFAttrAnnotation(InfoObject infoObj){
    AnnotationInfo ai = infoObj.getAnnotation("gov.nasa.jpf.annotation.JPFAttribute");
    if (ai != null){
      String[] attrTypes = ai.getValueAsStringArray();
      if (attrTypes != null){
        ClassLoader loader = config.getClassLoader();

        for (String clsName : attrTypes){
          try {
            Class<?> attrCls = loader.loadClass(clsName);
            Object attr = attrCls.newInstance(); 
            infoObj.addAttr(attr);
            
          } catch (ClassNotFoundException cnfx){
            logger.warning("attribute class not found: " + clsName);
            
          } catch (IllegalAccessException iax){
            logger.warning("attribute class has no public default ctor: " + clsName);            
            
          } catch (InstantiationException ix){
            logger.warning("attribute class has no default ctor: " + clsName);            
          }
        }
      }
    }    
  }

  protected void processNoJPFExecutionAnnotation(InfoObject infoObj) {
    AnnotationInfo ai = infoObj.getAnnotation("gov.nasa.jpf.annotation.NoJPFExecution");
    if (ai != null) {
      infoObj.addAttr(NoJPFExec.SINGLETON);
    }
  }

  protected void processJPFAnnotations(InfoObject infoObj) {
    processJPFAttrAnnotation(infoObj);
    processNoJPFExecutionAnnotation(infoObj);
  }

    public AnnotationInfo getResolvedAnnotationInfo (String typeName){
    return classLoader.getResolvedAnnotationInfo( typeName);
  }
  
  @Override
  public void setAnnotations(AnnotationInfo[] annotations) {
    this.annotations = annotations;
  }
  

 


  
  @Override
  public boolean hasAnnotations(){
    if (annotations.length > 0){
      return true;
    }
    
    for (ClassInfo ci = superClass; ci != null; ci = ci.superClass){
      AnnotationInfo[] a = ci.annotations;
      for (int j=0; j<a.length; j++){
        if (a[j].isInherited()){
          return true;
        }
      }
    }
    
    return false;
  }
  
  
  @Override
  public AnnotationInfo[] getAnnotations() {
    int nAnnotations = annotations.length;
    for (ClassInfo ci = superClass; ci != null; ci = ci.superClass){
      AnnotationInfo[] a = ci.annotations;
      for (int i=0; i<a.length; i++){
        if (a[i].isInherited()){
          nAnnotations++;
        }
      }
    }
    
    AnnotationInfo[] allAnnotations = new AnnotationInfo[nAnnotations];
    System.arraycopy(annotations, 0, allAnnotations, 0, annotations.length);
    int idx=annotations.length;
    for (ClassInfo ci = superClass; ci != null; ci = ci.superClass){
      AnnotationInfo[] a = ci.annotations;
      for (int i=0; i<a.length; i++){
        if (a[i].isInherited()){
          allAnnotations[idx++] = a[i];
        }
      }
    }
    
    return allAnnotations;
  }
    
  @Override
  public AnnotationInfo getAnnotation (String annotationName){
    AnnotationInfo[] a = annotations;
    for (int i=0; i<a.length; i++){
      if (a[i].getName().equals(annotationName)){
        return a[i];
      }
    }
    
    for (ClassInfo ci = superClass; ci != null; ci = ci.superClass){
      a = ci.annotations;
      for (int i=0; i<a.length; i++){
        AnnotationInfo ai = a[i];
        if (ai.getName().equals(annotationName) && ai.isInherited()){
          return ai;
        }
      }
    }
    
    return null;
  }
  
  protected ClassInfo (String name, ClassLoaderInfo cli, String classFileUrl){
    nClassInfos++;
    
    this.name = name;
    this.classLoader = cli;
    this.classFileUrl = classFileUrl;
    
    this.methods = NO_METHODS;  


  }
  
  
  protected void resolveAndLink () throws ClassParseException {
    

    isStringClassInfo = isStringClassInfo0();
    isThreadClassInfo = isThreadClassInfo0();
    isObjectClassInfo = isObjectClassInfo0();
    isRefClassInfo = isRefClassInfo0();

    isAbstract = (modifiers & Modifier.ABSTRACT) != 0;

    
    finalizer = getFinalizer0();

    resolveClass(); 




    nativePeer = loadNativePeer();

    checkUnresolvedNativeMethods();

    linkFields(); 
    
    setAssertionStatus();
    processJPFConfigAnnotation();
    processJPFAnnotations(this);
    loadAnnotationListeners();    
  }
  
  private void addUndeclaredModelClassMethods() {
  	try {
  		Class<?> originalClass = ClassLoader.getSystemClassLoader().loadClass(getName());
  		if (originalClass.isArray()) {
  			return;
  		}
  		Method[] mList = originalClass.getDeclaredMethods();
  		for (Method m : mList) {
  			Class<?>[] parameterTypes = m.getParameterTypes();
  			Class<?> returnType = m.getReturnType();
  			StringBuilder signatureBuilder = new StringBuilder();
  			signatureBuilder.append('(');
  			for (Class<?> paramType : parameterTypes) {
  				signatureBuilder.append(Types.getTypeSignature(paramType.getName(), false));
  			}
  			signatureBuilder.append(')');
  			signatureBuilder.append(Types.getTypeSignature(returnType.getName(), false));
  			String uniqueName = m.getName() + signatureBuilder.toString();
  			if (!methods.containsKey(uniqueName)) {
  				System.out.println("Adding undeclared method:");
  				System.out.println(getName() + "." + uniqueName);
  				MethodInfo undeclaredMethod = new MethodInfo(
  						m.getName(), signatureBuilder.toString(), m.getModifiers() | Modifier.NATIVE);
  				setMethod(undeclaredMethod);
  			}
  		}
  	} catch (ClassNotFoundException ex) {

  	} catch (java.lang.SecurityException ex) {


  	}
	}

	protected ClassInfo(){
    nClassInfos++;
    

  }
  
  
  protected ClassInfo (String builtinClassName, ClassLoaderInfo classLoader) {
    nClassInfos++;

    this.classLoader = classLoader;

    isArray = (builtinClassName.charAt(0) == '[');
    isReferenceArray = isArray && (builtinClassName.endsWith(";") || builtinClassName.charAt(1) == '[');
    isBuiltin = true;

    name = builtinClassName;

    logger.log(Level.FINE, "generating builtin class: %1$s", name);

    packageName = ""; 
    sourceFileName = null;
    source = null;
    genericSignature = "";


    iFields = EMPTY_FIELDINFO_ARRAY;
    sFields = EMPTY_FIELDINFO_ARRAY;

    if (isArray) {
      if(classLoader.isSystemClassLoader()) {
        superClass = ((SystemClassLoaderInfo)classLoader).getObjectClassInfo();
      } else {
        superClass = ClassLoaderInfo.getCurrentSystemClassLoader().getObjectClassInfo();
      }
      interfaceNames = loadArrayInterfaces();
      methods = loadArrayMethods();
    } else {
      superClass = null; 
      interfaceNames = loadBuiltinInterfaces(name);
      methods = loadBuiltinMethods(name);
    }

    enableAssertions = true; 

    classFileUrl = name;
    

  }
  
  public static int getNumberOfLoadedClasses(){
    return nClassInfos;
  }
  


  
  protected void setAnnotationValueGetterCode (MethodInfo pmi, FieldInfo fi){

  }
  
  protected void setDirectCallCode (MethodInfo miCallee, MethodInfo miStub){

  }
  
  protected void setLambdaDirectCallCode (MethodInfo miDirectCall, BootstrapMethodInfo bootstrapMethod){

  }
  
  protected void setNativeCallCode (NativeMethodInfo miNative){

  }
  
  protected void setRunStartCode (MethodInfo miStub, MethodInfo miRun){

  }
  
  
  protected ClassInfo (ClassInfo annotationCls, String name, ClassLoaderInfo classLoader, String url) {
    this.classLoader = classLoader;
    
    this.name = name;
    isClass = true;


    superClass = ClassLoaderInfo.getSystemResolvedClassInfo("gov.nasa.jpf.AnnotationProxyBase");

    interfaceNames = new String[]{ annotationCls.name };    
    packageName = annotationCls.packageName;
    sourceFileName = annotationCls.sourceFileName;
    genericSignature = annotationCls.genericSignature;

    sFields = new FieldInfo[0]; 
    staticDataSize = 0;

    methods = new HashMap<String, MethodInfo>();
    iFields = new FieldInfo[annotationCls.methods.size()];
    nInstanceFields = iFields.length;


    int idx = 0;
    int off = 0;  
    for (MethodInfo mi : annotationCls.getDeclaredMethodInfos()) {
      String mname = mi.getName();
      String mtype = mi.getReturnType();
      String genericSignature = mi.getGenericSignature();


      FieldInfo fi = FieldInfo.create(mname, mtype, 0);
      fi.linkToClass(this, idx, off);
      fi.setGenericSignature(genericSignature);
      iFields[idx++] = fi;
      off += fi.getStorageSize();

      MethodInfo pmi = new MethodInfo(this, mname, mi.getSignature(), Modifier.PUBLIC, 1, 2);
      pmi.setGenericSignature(genericSignature);
      
      setAnnotationValueGetterCode( pmi, fi);
      methods.put(pmi.getUniqueName(), pmi);
    }

    instanceDataSize = computeInstanceDataSize();
    instanceDataOffset = 0;

    classFileUrl = url;
    linkFields();
  }
  
  

  protected ClassInfo createFuncObjClassInfo (BootstrapMethodInfo bootstrapMethod, String name, String samUniqueName, String[] fieldTypesName) {
   return null;
 }
 
 protected ClassInfo (ClassInfo funcInterface, BootstrapMethodInfo bootstrapMethod, String name, String[] fieldTypesName) {
   ClassInfo enclosingClass = bootstrapMethod.enclosingClass;
   this.classLoader = enclosingClass.classLoader;

   this.name = name;
   isClass = true;

   superClassName = "java.lang.Object";

   interfaceNames = new String[]{ funcInterface.name };    
   packageName = enclosingClass.getPackageName();


   int n = fieldTypesName.length;
   
   iFields = new FieldInfo[n];
   nInstanceFields = n;
   
   sFields = new FieldInfo[0];
   staticDataSize = 0;
   
   int idx = 0;
   int off = 0;  
   
   int i = 0;
   for(String type: fieldTypesName) {
     FieldInfo fi = FieldInfo.create("arg" + i++, type, 0);
     fi.linkToClass(this, idx, off);
     iFields[idx++] = fi;
     off += fi.getStorageSize();
   }
   
   linkFields();
 }
  

  
  @Override
  public int hashCode() {
    return OATHash.hash(name.hashCode(), classLoader.hashCode());
  }
  
  @Override
  public boolean equals (Object o) {
    if (o instanceof ClassInfo) {
      ClassInfo other = (ClassInfo)o;
      if (classLoader == other.classLoader) {

        if (name.equals(other.name)) {
          return true;
        }
      }
    }
    
    return false;
  }

  protected String computeSourceFileName(){
    return name.replace('.', '/') + ".java";
  }

  protected void checkUnresolvedNativeMethods(){
  	for (MethodInfo mi : methods.values()){
      if (mi.isUnresolvedNativeMethod()){
        NativeMethodInfo nmi = new NativeMethodInfo(mi, null, nativePeer);
        nmi.replace(mi);
      }
    }
  }

  protected void processJPFConfigAnnotation() {
    AnnotationInfo ai = getAnnotation("gov.nasa.jpf.annotation.JPFConfig");
    if (ai != null) {
      for (String s : ai.getValueAsStringArray()) {
        config.parse(s);
      }
    }
  }

  protected void loadAnnotationListeners () {
    if (autoloadAnnotations != null) {
      autoloadListeners(annotations); 

      for (int i=0; i<sFields.length; i++) {
        autoloadListeners(sFields[i].getAnnotations());
      }

      for (int i=0; i<iFields.length; i++) {
        autoloadListeners(iFields[i].getAnnotations());
      }



    }
  }

  void autoloadListeners(AnnotationInfo[] annos) {
    if ((annos != null) && (autoloadAnnotations != null)) {
      for (AnnotationInfo ai : annos) {
        String aName = ai.getName();
        if (autoloadAnnotations.contains(aName)) {
          if (!autoloaded.contains(aName)) {
            autoloaded.add(aName);
            String key = "listener." + aName;
            String defClsName = aName + "Checker";
            try {
              JPFListener listener = config.getInstance(key, JPFListener.class, defClsName);
              
              JPF jpf = VM.getVM().getJPF(); 
              jpf.addUniqueTypeListener(listener);

              if (logger.isLoggable(Level.INFO)){
                logger.info("autoload annotation listener: @", aName, " => ", listener.getClass().getName());
              }

            } catch (JPFConfigException cx) {
              logger.warning("no autoload listener class for annotation " + aName +
                             " : " + cx.getMessage());
              autoloadAnnotations.remove(aName);
            }
          }
        }
      }

      if (autoloadAnnotations.isEmpty()) {
        autoloadAnnotations = null;
      }
    }
  }

  protected NativePeer loadNativePeer(){
    return NativePeer.getNativePeer(this);
  }
  
  
  public ClassLoaderInfo getClassLoaderInfo() {
    return classLoader;
  }

  
  public Statics getStatics() {
    return classLoader.getStatics();
  }
  
  
  public ClassInfo getClassInfo() {
    return this;
  }

  protected void setAssertionStatus() {
    if(isInitialized()) {
      return;
    } else {
      enableAssertions = classLoader.desiredAssertionStatus(name);
    }
  }

  boolean getAssertionStatus () {
    return enableAssertions;
  }

  public boolean desiredAssertionStatus() {
    return classLoader.desiredAssertionStatus(name);
  }

  @Override
  public String getGenericSignature() {
    return genericSignature;
  }

  @Override
  public void setGenericSignature(String sig){
    genericSignature = sig;
  }
  
  public boolean isArray () {
    return isArray;
  }

  public boolean isEnum () {
    return isEnum;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isBuiltin(){
    return isBuiltin;
  }
  
  public boolean isInterface() {
    return ((modifiers & Modifier.INTERFACE) != 0);
  }

  public boolean isReferenceArray () {
    return isReferenceArray;
  }

  public boolean isObjectClassInfo() {
    return isObjectClassInfo;
  }

  public boolean isStringClassInfo() {
    return isStringClassInfo;
  }

  public boolean isThreadClassInfo() {
    return isThreadClassInfo;
  }

  protected void checkNoClinitInitialization(){
    if (!isInitialized()){
      ThreadInfo ti = ThreadInfo.getCurrentThread();
      registerClass(ti);
      setInitialized(); 
    }
  }
  
  protected ClassInfo createAnnotationProxy (String proxyName){

    return null;
  }
  
  public ClassInfo getAnnotationProxy (){

    
    checkNoClinitInitialization(); 
    
    ClassInfo ciProxy = classLoader.getResolvedAnnotationProxy(this);
    ciProxy.checkNoClinitInitialization();
    
    return ciProxy;
  }
  


  public boolean areAssertionsEnabled() {
    return enableAssertions;
  }

  public boolean hasInstanceFields () {
    return (instanceDataSize > 0);
  }

  public ElementInfo getClassObject(){
    StaticElementInfo sei = getStaticElementInfo();
    
    if (sei != null){
      int objref = sei.getClassObjectRef();
      return VM.getVM().getElementInfo(objref);
    }

    return null;
  }
  
  public ElementInfo getModifiableClassObject(){
    StaticElementInfo sei = getStaticElementInfo();
    
    if (sei != null){
      int objref = sei.getClassObjectRef();
      return VM.getVM().getModifiableElementInfo(objref);
    }

    return null;
  }
  

  public int getClassObjectRef () {
    StaticElementInfo sei = getStaticElementInfo();    
    return (sei != null) ? sei.getClassObjectRef() : MJIEnv.NULL;
  }

  public gov.nasa.jpf.vm.ClassFileContainer getContainer(){
    return container;
  }
  
  public String getClassFileUrl (){
    return classFileUrl;
  }


  
  public boolean hasReleaseAction (ReleaseAction action){
    return (releaseActions != null) && releaseActions.contains(action);
  }
  
  
  public void addReleaseAction (ReleaseAction action){

    releaseActions = new ImmutableList<ReleaseAction>( action, releaseActions);
  }
  
  
  public void processReleaseActions (ElementInfo ei){
    if (superClass != null){
      superClass.processReleaseActions(ei);
    }
    
    if (releaseActions != null) {
      for (ReleaseAction action : releaseActions) {
        action.release(ei);
      }
    }
  }
  
  public int getModifiers() {
    return modifiers;
  }

  
  public MethodInfo getMethod (String uniqueName, boolean isRecursiveLookup) {
    MethodInfo mi = methods.get(uniqueName);

    if ((mi == null) && isRecursiveLookup && (superClass != null)) {
      mi = superClass.getMethod(uniqueName, true);
    }

    return mi;
  }

  
  public MethodInfo getMethod (String name, String signature, boolean isRecursiveLookup) {
    MethodInfo mi = null;
    String matchName = name + signature;

    for (Map.Entry<String, MethodInfo>e : methods.entrySet()) {
      if (e.getKey().startsWith(matchName)){
        mi = e.getValue();
        break;
      }
    }

    if ((mi == null) && isRecursiveLookup && (superClass != null)) {
      mi = superClass.getMethod(name, signature, true);
    }

    return mi;
  }

  
  public MethodInfo getDefaultMethod (String uniqueName) {
    MethodInfo mi = null;
    
    for (ClassInfo ci = this; ci != null; ci = ci.superClass){
      for (ClassInfo ciIfc : ci.interfaces){
        MethodInfo miIfc = ciIfc.getMethod(uniqueName, true);
        if (miIfc != null && !miIfc.isAbstract()){
          if (mi != null){

            String msg = "Conflicting default methods: " + mi.getFullName() + ", " + miIfc.getFullName();
            throw new ClassChangeException(msg);
          } else {
            mi = miIfc;
          }
        }
      }
    }
    
    return mi;
  }
  
  
  public MethodInfo getInterfaceAbstractMethod () {
    ClassInfo objCi = ClassLoaderInfo.getCurrentResolvedClassInfo("java.lang.Object");
    
    for(MethodInfo mi: this.methods.values()) {
      if(mi.isAbstract() && objCi.getMethod(mi.getUniqueName(), false)==null) {
        return mi;
      }
    }
    
    for (ClassInfo ifc : this.interfaces){
      MethodInfo mi = ifc.getInterfaceAbstractMethod();
      if(mi!=null) {
        return mi;
      }
    }
    
    return null;
  }

  
  public MethodInfo getReflectionMethod (String fullName, boolean isRecursiveLookup) {
        

    for (ClassInfo ci = this; ci != null; ci = ci.superClass){
      for (Map.Entry<String, MethodInfo>e : ci.methods.entrySet()) {
        String name = e.getKey();
        if (name.startsWith(fullName)) {
          return e.getValue();
        }
      }
      if (!isRecursiveLookup){
        return null;
      }
    }


    for (ClassInfo ci : getAllInterfaces() ){
      for (Map.Entry<String, MethodInfo>e : ci.methods.entrySet()) {
        String name = e.getKey();
        if (name.startsWith(fullName)) {
          return e.getValue();
        }
      }      
    }    

    return null;
  }
  
  
  public void matchMethods (MethodLocator loc) {
    for (MethodInfo mi : methods.values()) {
      if (loc.match(mi)) {
        return;
      }
    }
    if (superClass != null) {
      superClass.matchMethods(loc);
    }
  }

  
  public void matchDeclaredMethods (MethodLocator loc) {
    for (MethodInfo mi : methods.values()) {
      if (loc.match(mi)) {
        return;
      }
    }
  }

  @Override
  public Iterator<MethodInfo> iterator() {
    return new Iterator<MethodInfo>() {
      ClassInfo ci = ClassInfo.this;
      Iterator<MethodInfo> it = ci.methods.values().iterator();

      @Override
	public boolean hasNext() {
        if (it.hasNext()) {
          return true;
        } else {
          if (ci.superClass != null) {
            ci = ci.superClass;
            it = ci.methods.values().iterator();
            return it.hasNext();
          } else {
            return false;
          }
        }
      }

      @Override
	public MethodInfo next() {
        if (hasNext()) {
          return it.next();
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
	public void remove() {

        throw new UnsupportedOperationException("can't remove methods");
      }
    };
  }
  
  public Iterator<MethodInfo> declaredMethodIterator() {
    return methods.values().iterator();
  }

  
  public FieldInfo getStaticField (String fName) {
    FieldInfo fi;
    ClassInfo c = this;

    while (c != null) {
      fi = c.getDeclaredStaticField(fName);
      if (fi != null) {
        return fi;
      }
      c = c.superClass;
    }



    for (ClassInfo ci : getAllInterfaces()) {
      fi = ci.getDeclaredStaticField(fName);
      if (fi != null) {
        return fi;
      }
    }

    return null;
  }

  public Object getStaticFieldValueObject (String id){
    ClassInfo c = this;
    Object v;

    while (c != null){
      ElementInfo sei = c.getStaticElementInfo();
      v = sei.getFieldValueObject(id);
      if (v != null){
        return v;
      }
      c = c.getSuperClass();
    }

    return null;
  }

  public FieldInfo[] getDeclaredStaticFields() {
    return sFields;
  }

  public FieldInfo[] getDeclaredInstanceFields() {
    return iFields;
  }

  
  public FieldInfo getDeclaredStaticField (String fName) {
    for (int i=0; i<sFields.length; i++) {
      if (sFields[i].getName().equals(fName)) return sFields[i];
    }

    return null;
  }

  
  public FieldInfo getInstanceField (String fName) {
    FieldInfo fi;
    ClassInfo c = this;

    while (c != null) {
      fi = c.getDeclaredInstanceField(fName);
      if (fi != null) return fi;
      c = c.superClass;
    }

    return null;
  }

  
  public FieldInfo getDeclaredInstanceField (String fName) {
    for (int i=0; i<iFields.length; i++) {
      if (iFields[i].getName().equals(fName)) return iFields[i];
    }

    return null;
  }
  
  public String getSignature() {
    if (signature == null) {
      signature = Types.getTypeSignature(name, false);
    }
    
    return signature;     
  }

  
  public String getName () {
    return name;
  }

  public String getSimpleName () {
    int i;
    String enclosingClassName = getEnclosingClassName();
    
    if(enclosingClassName!=null){
      i = enclosingClassName.length();      
    } else{
      i = name.lastIndexOf('.');
    }
    
    return name.substring(i+1);
  }

  public String getPackageName () {
    return packageName;
  }

  public int getId() {
    return id;
  }

  public long getUniqueId() {
    return uniqueId;
  }

  public int getFieldAttrs (int fieldIndex) {
    fieldIndex = 0; 
     
    return 0;
  }

  public void setElementInfoAttrs (int attrs){
    elementInfoAttrs = attrs;
  }

  public void addElementInfoAttr (int attr){
    elementInfoAttrs |= attr;
  }

  public int getElementInfoAttrs () {
    return elementInfoAttrs;
  }

  public Source getSource () {
    if (source == null) {
      source = loadSource();
    }

    return source;
  }

  public String getSourceFileName () {
    return sourceFileName;
  }

  
  public FieldInfo getStaticField (int index) {
    return sFields[index];
  }

  
  public String getStaticFieldName (int index) {
    return getStaticField(index).getName();
  }

  
  public boolean isStaticMethodAbstractionDeterministic (ThreadInfo th,
                                                         MethodInfo mi) {



     
    th = null;  
    mi = null;
     
    return true;
  }

  public String getSuperClassName() {
    return superClassName;
  }

  
  public ClassInfo getSuperClass () {
    return superClass;
  }

  
  public ClassInfo getSuperClass (String clsName) {
    if (clsName.equals(name)) return this;

    if (superClass != null) {
      return superClass.getSuperClass(clsName);
    } else {
      return null;
    }
  }

  public int getNumberOfSuperClasses(){
    int n = 0;
    for (ClassInfo ci = superClass; ci != null; ci = ci.superClass){
      n++;
    }
    return n;
  }
  
  
  public String getEnclosingClassName(){
    return enclosingClassName;
  }
  
  
  public ClassInfo getEnclosingClassInfo() {
    String enclName = getEnclosingClassName();
    return (enclName == null ? null : classLoader.getResolvedClassInfo(enclName)); 
  }

  public String getEnclosingMethodName(){
    return enclosingMethodName;
  }

  
  public MethodInfo getEnclosingMethodInfo(){
    MethodInfo miEncl = null;
    
    if (enclosingMethodName != null){
      ClassInfo ciIncl = getEnclosingClassInfo();
      miEncl = ciIncl.getMethod( enclosingMethodName, false);
    }
    
    return miEncl;
  }
  
  
  public boolean isSystemClass () {
    return name.startsWith("java.") || name.startsWith("javax.");
  }

  
  public boolean isBoxClass () {
    if (name.startsWith("java.lang.")) {
      String rawType = name.substring(10);
      if (rawType.startsWith("Boolean") ||
          rawType.startsWith("Byte") ||
          rawType.startsWith("Character") ||
          rawType.startsWith("Integer") ||
          rawType.startsWith("Float") ||
          rawType.startsWith("Long") ||
          rawType.startsWith("Double")) {
        return true;
      }
    }
    return false;
  }

  
  public String getType () {
    if (!isArray) {
      return "L" + name.replace('.', '/') + ";";
    } else {
      return name;
    }
  }

  
  public boolean isWeakReference () {
    return isWeakReference;
  }

  
  public boolean isReferenceClassInfo () {
    return isRefClassInfo;
  }

  
  public boolean isPrimitive() {
    return superClass == null && !isObjectClassInfo();
  }


  boolean hasRefField (int ref, Fields fv) {
    ClassInfo c = this;

    do {
      FieldInfo[] fia = c.iFields;
      for (int i=0; i<fia.length; i++) {
        FieldInfo fi = c.iFields[i];
        if (fi.isReference() && (fv.getIntValue( fi.getStorageOffset()) == ref)) return true;
      }
      c = c.superClass;
    } while (c != null);

    return false;
  }

  boolean hasImmutableInstances () {
    return ((elementInfoAttrs & ElementInfo.ATTR_IMMUTABLE) != 0);
  }

  public boolean hasInstanceFieldInfoAttr (Class<?> type){
    for (int i=0; i<nInstanceFields; i++){
      if (getInstanceField(i).hasAttr(type)){
        return true;
      }
    }
    
    return false;
  }
  
  public NativePeer getNativePeer () {
    return nativePeer;
  }
  
  
  public boolean isInstanceOf (String cname) {
    if (isPrimitive()) {
      return Types.getJNITypeCode(name).equals(cname);

    } else {
      cname = Types.getClassNameFromTypeName(cname);
      ClassInfo ci = this.classLoader.getResolvedClassInfo(cname);
      return isInstanceOf(ci);
    }
  }

  
  public boolean isInstanceOf (ClassInfo ci) {
    if (isPrimitive()) { 
      return (this==ci);
    } else {
      for (ClassInfo c = this; c != null; c = c.superClass) {
        if (c==ci) {
          return true;
        }
      }

      return getAllInterfaces().contains(ci);
    }
  }

  public boolean isInnerClassOf (String enclosingName){

    ClassInfo ciEncl = classLoader.tryGetResolvedClassInfo( enclosingName);
    if (ciEncl != null){
      return ciEncl.hasInnerClass(name);
    } else {
      return false;
    }
  }
  
  public boolean hasInnerClass (String innerName){
    for (int i=0; i<innerClassNames.length; i++){
      if (innerClassNames[i].equals(innerName)){
        return true;
      }
    }
    
    return false;
  }


  public static String makeModelClassPath (Config config) {
    StringBuilder buf = new StringBuilder(256);
    String ps = File.pathSeparator;
    String v;

    for (File f : config.getPathArray("boot_classpath")){
      buf.append(f.getAbsolutePath());
      buf.append(ps);
    }

    for (File f : config.getPathArray("classpath")){
      buf.append(f.getAbsolutePath());
      buf.append(ps);
    }


    v = System.getProperty("sun.boot.class.path");
    if (v != null) {
      buf.append(v);
    }
    
    return buf.toString();
  }
  
  protected static String[] loadArrayInterfaces () {
    return new String[] {"java.lang.Cloneable", "java.io.Serializable"};
  }

  protected static String[] loadBuiltinInterfaces (String type) {
    return EMPTY_STRING_ARRAY;
  }


  
  void loadInterfaceRec (Set<ClassInfo> set, String[] interfaces) throws ClassInfoException {
    if (interfaces != null) {
      for (String iname : interfaces) {

        ClassInfo ci = classLoader.getResolvedClassInfo(iname);

        if (set != null){
          set.add(ci);
        }

        loadInterfaceRec(set, ci.interfaceNames);
      }
    }
  }

  int computeInstanceDataOffset () {
    if (superClass == null) {
      return 0;
    } else {
      return superClass.getInstanceDataSize();
    }
  }

  int getInstanceDataOffset () {
    return instanceDataOffset;
  }

  ClassInfo getClassBase (String clsBase) {
    if ((clsBase == null) || (name.equals(clsBase))) return this;

    if (superClass != null) {
      return superClass.getClassBase(clsBase);
    }

    return null; 
  }

  int computeInstanceDataSize () {
    int n = getDataSize( iFields);

    for (ClassInfo c=superClass; c!= null; c=c.superClass) {
      n += c.getDataSize(c.iFields);
    }

    return n;
  }

  public int getInstanceDataSize () {
    return instanceDataSize;
  }

  int getDataSize (FieldInfo[] fields) {
    int n=0;
    for (int i=0; i<fields.length; i++) {
      n += fields[i].getStorageSize();
    }

    return n;
  }

  public int getNumberOfDeclaredInstanceFields () {
    return iFields.length;
  }

  public FieldInfo getDeclaredInstanceField (int i) {
    return iFields[i];
  }

  public int getNumberOfInstanceFields () {
    return nInstanceFields;
  }

  public FieldInfo getInstanceField (int i) {
    int idx = i - (nInstanceFields - iFields.length);
    if (idx >= 0) {
      return ((idx < iFields.length) ? iFields[idx] : null);
    } else {
      return ((superClass != null) ? superClass.getInstanceField(i) : null);
    }
  }

  public FieldInfo[] getInstanceFields(){
    FieldInfo[] fields = new FieldInfo[nInstanceFields];
    
    for (int i=0; i<fields.length; i++){
      fields[i] = getInstanceField(i);
    }
    
    return fields;
  }

  public int getStaticDataSize () {
    return staticDataSize;
  }

  int computeStaticDataSize () {
    return getDataSize(sFields);
  }

  public int getNumberOfStaticFields () {
    return sFields.length;
  }

  protected Source loadSource () {
    return Source.getSource(sourceFileName);
  }

  public static boolean isBuiltinClass (String cname) {
    char c = cname.charAt(0);


    if ((c == '[') || cname.endsWith("[]")) {
      return true;
    }


    if (Character.isLowerCase(c)) {
      if ("int".equals(cname) || "byte".equals(cname) ||
          "boolean".equals(cname) || "double".equals(cname) ||
          "long".equals(cname) || "char".equals(cname) ||
          "short".equals(cname) || "float".equals(cname) || "void".equals(cname)) {
        return true;
      }
    }

    return false;
  }

  
  static void setSourceRoots (Config config) {
    Source.init(config);
  }

  
  public Set<ClassInfo> getAllInterfaces () {
    if (allInterfaces == null) {
      HashSet<ClassInfo> set = new HashSet<ClassInfo>();

      for (ClassInfo ci=this; ci != null; ci=ci.superClass) {
        loadInterfaceRec(set, ci.interfaceNames);
      }

      allInterfaces = Collections.unmodifiableSet(set);
    }

    return allInterfaces;
  }

  
  public String[] getDirectInterfaceNames () {
    return interfaceNames;
  }

  public Set<ClassInfo> getInterfaceClassInfos() {
    return interfaces;
  }

  public Set<ClassInfo> getAllInterfaceClassInfos() {
    return getAllInterfaces();
  }

  
  
  public String[] getInnerClasses(){
    return innerClassNames;
  }
  
  public ClassInfo[] getInnerClassInfos(){
    ClassInfo[] innerClassInfos = new ClassInfo[innerClassNames.length];
    
    for (int i=0; i< innerClassNames.length; i++){
      innerClassInfos[i] = classLoader.getResolvedClassInfo(innerClassNames[i]); 
    }
    
    return innerClassInfos;
  }
  
  public BootstrapMethodInfo getBootstrapMethodInfo(int index) {
    return bootstrapMethods[index];
  }

  public ClassInfo getComponentClassInfo () {
    if (isArray()) {
      String cn = name.substring(1);

      if (cn.charAt(0) != '[') {
        cn = Types.getTypeName(cn);
      }

      ClassInfo cci = classLoader.getResolvedClassInfo(cn);

      return cci;
    }

    return null;
  }

  
  protected Map<String, MethodInfo> getDeclaredMethods () {
    return methods;
  }

  
  public MethodInfo putDeclaredMethod (MethodInfo mi){
    return methods.put(mi.getUniqueName(), mi);
  }

  public MethodInfo[] getDeclaredMethodInfos() {
    MethodInfo[] a = new MethodInfo[methods.size()];
    methods.values().toArray(a);
    return a;
  }

  public Instruction[] getMatchingInstructions (LocationSpec lspec){
    Instruction[] insns = null;

    if (lspec.matchesFile(sourceFileName)){
      for (MethodInfo mi : methods.values()) {
        Instruction[] a = mi.getMatchingInstructions(lspec);
        if (a != null){
          if (insns != null) {

            insns = Misc.appendArray(insns, a);
          } else {
            insns = a;
          }


          if (!lspec.isLineInterval()) {
            break;
          }
        }
      }
    }

    return insns;
  }

  public List<MethodInfo> getMatchingMethodInfos (MethodSpec mspec){
    ArrayList<MethodInfo> list = null;
    if (mspec.matchesClass(name)) {
      for (MethodInfo mi : methods.values()) {
        if (mspec.matches(mi)) {
          if (list == null) {
            list = new ArrayList<MethodInfo>();
          }
          list.add(mi);
        }
      }
    }
    return list;
  }

  public MethodInfo getFinalizer () {
    return finalizer;
  }

  public MethodInfo getClinit() {

    for (MethodInfo mi : methods.values()) {
      if ("<clinit>".equals(mi.getName())) {
        return mi;
      }
    }
    return null;
  }

  public boolean hasCtors() {

    for (MethodInfo mi : methods.values()) {
      if ("<init>".equals(mi.getName())) {
        return true;
      }
    }
    return false;
  }

  
  public static ClassInfo getInitializedSystemClassInfo (String clsName, ThreadInfo ti){
    ClassLoaderInfo systemLoader = ClassLoaderInfo.getCurrentSystemClassLoader();
    ClassInfo ci = systemLoader.getResolvedClassInfo(clsName);
    ci.initializeClassAtomic(ti);

    return ci;
  }

  
  public static ClassInfo getInitializedClassInfo (String clsName, ThreadInfo ti){
    ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();
    ClassInfo ci = cl.getResolvedClassInfo(clsName);
    ci.initializeClassAtomic(ti);

    return ci;
  }

  public boolean isRegistered () {

    return getStaticElementInfo() != null;
  }
  
  
  public StaticElementInfo registerClass (ThreadInfo ti){
    StaticElementInfo sei = getStaticElementInfo();
    
    if (sei == null) {


      
      if (superClass != null) {
        superClass.registerClass(ti);
      }

      for (ClassInfo ifc : interfaces) {
        ifc.registerClass(ti);
      }
      
      ClassInfo.logger.finer("registering class: ", name);
      
      ElementInfo ei = createClassObject( ti);
      sei = createAndLinkStaticElementInfo( ti, ei);
      

      ti.getVM().notifyClassLoaded(this);
    }
    
    return sei;
  }

  ElementInfo createClassObject (ThreadInfo ti){
    Heap heap = VM.getVM().getHeap(); 

    int anchor = name.hashCode(); 

    SystemClassLoaderInfo systemClassLoader = ti.getSystemClassLoaderInfo();

    ClassInfo classClassInfo = systemClassLoader.getClassClassInfo();    
    ElementInfo ei = heap.newSystemObject(classClassInfo, ti, anchor);
    int clsObjRef = ei.getObjectRef();
    
    ElementInfo eiClsName = heap.newSystemString(name, ti, clsObjRef);
    ei.setReferenceField("name", eiClsName.getObjectRef());

    ei.setBooleanField("isPrimitive", isPrimitive());
    



    ei.setReferenceField("classLoader", classLoader.getClassLoaderObjectRef());
    
    return ei;
  }
  
  StaticElementInfo createAndLinkStaticElementInfo (ThreadInfo ti, ElementInfo eiClsObj) {
    Statics statics = classLoader.getStatics();
    StaticElementInfo sei = statics.newClass(this, ti, eiClsObj);
    
    id = sei.getObjectRef();  
    uniqueId = ((long)classLoader.getId() << 32) | id;
    
    eiClsObj.setIntField( ID_FIELD, id);      
    
    return sei;
  }

  


  
  void registerStartupClass(ThreadInfo ti, List<ClassInfo> list) {
    if (!isRegistered()) {




      if (superClass != null) {
        superClass.registerStartupClass(ti, list);
      }

      for (ClassInfo ifc : interfaces) {
        ifc.registerStartupClass(ti, list);
      }
    }

    if (!list.contains(this)) {
      list.add(this);
      ClassInfo.logger.finer("registering startup class: ", name);
      createStartupStaticElementInfo(ti);
    }
    

      ti.getVM().notifyClassLoaded(this);
  }
  
  StaticElementInfo createStartupStaticElementInfo (ThreadInfo ti) {
    Statics statics = classLoader.getStatics();
    StaticElementInfo sei = statics.newStartupClass(this, ti);
    
    id = sei.getObjectRef();  
    uniqueId = ((long)classLoader.getId() << 32) | id;
    
    return sei;
  }
  
  ElementInfo createAndLinkStartupClassObject (ThreadInfo ti) {
    StaticElementInfo sei = getStaticElementInfo();
    ElementInfo ei = createClassObject(ti);
    
    sei.setClassObjectRef(ei.getObjectRef());
    ei.setIntField( ID_FIELD, id);      
    
    return ei;
  }
  
  boolean checkIfValidClassClassInfo() {
    return getDeclaredInstanceField( ID_FIELD) != null;
  }
  
  public boolean isInitializing () {
    StaticElementInfo sei = getStaticElementInfo();
    return ((sei != null) && (sei.getStatus() >= 0));
  }

  
  public boolean isInitialized () {
    for (ClassInfo ci = this; ci != null; ci = ci.superClass){
      StaticElementInfo sei = ci.getStaticElementInfo();
      if (sei == null || sei.getStatus() != INITIALIZED){
        return false;
      }
    }
    
    return true;
  }

  public boolean isResolved () {
    return (!isObjectClassInfo() && superClass != null);
  }

  public boolean needsInitialization (ThreadInfo ti){
    StaticElementInfo sei = getStaticElementInfo();
    if (sei != null){
      int status = sei.getStatus();
      if (status == INITIALIZED || status == ti.getId()){
        return false;
      }
    }

    return true;
  }

  public void setInitializing(ThreadInfo ti) {
    StaticElementInfo sei = getModifiableStaticElementInfo();
    sei.setStatus(ti.getId());
  }
  
  
  public boolean initializeClass(ThreadInfo ti){
    int pushedFrames = 0;


    for (ClassInfo ci = this; ci != null; ci = ci.getSuperClass()) {
      StaticElementInfo sei = ci.getStaticElementInfo();
      if (sei == null){
        sei = ci.registerClass(ti);
      }

      int status = sei.getStatus();
      if (status != INITIALIZED){






        if (status != ti.getId()) {


          MethodInfo mi = ci.getMethod("<clinit>()V", false);
          if (mi != null) {
            DirectCallStackFrame frame = ci.createDirectCallStackFrame(ti, mi, 0);
            ti.pushFrame( frame);
            pushedFrames++;

          } else {

            ci.setInitialized();
          }
        } else {

        }
      } else {
        break; 
      }
    }

    return (pushedFrames > 0);
  }

  
  public void initializeClassAtomic (ThreadInfo ti){
    for (ClassInfo ci = this; ci != null; ci = ci.getSuperClass()) {
      StaticElementInfo sei = ci.getStaticElementInfo();
      if (sei == null){
        sei = ci.registerClass(ti);
      }

      int status = sei.getStatus();
      if (status != INITIALIZED && status != ti.getId()){
          MethodInfo mi = ci.getMethod("<clinit>()V", false);
          if (mi != null) {
            DirectCallStackFrame frame = ci.createDirectCallStackFrame(ti, mi, 0);
            ti.executeMethodAtomic(frame);
          } else {
            ci.setInitialized();
          }
      } else {
        break; 
      }
    }
  }

  public void setInitialized() {
    StaticElementInfo sei = getStaticElementInfo();
    if (sei != null && sei.getStatus() != INITIALIZED){
      sei = getModifiableStaticElementInfo();
      sei.setStatus(INITIALIZED);




    }
  }

  public StaticElementInfo getStaticElementInfo() {
    if (id != -1) {
      return classLoader.getStatics().get( id);
    } else {
      return null;
    }
  }

  public StaticElementInfo getModifiableStaticElementInfo() {
    if (id != -1) {
      return classLoader.getStatics().getModifiable( id);
    } else {
      return null;      
    }
  }

  Fields createArrayFields (String type, int nElements, int typeSize, boolean isReferenceArray) {
    return fieldsFactory.createArrayFields( type, this,
                                            nElements, typeSize, isReferenceArray);
  }

  
  Fields createStaticFields () {
    return fieldsFactory.createStaticFields(this);
  }

  void initializeStaticData (ElementInfo ei, ThreadInfo ti) {
    for (int i=0; i<sFields.length; i++) {
      FieldInfo fi = sFields[i];
      fi.initialize(ei, ti);
    }
  }

  
  public Fields createInstanceFields () {
    return fieldsFactory.createInstanceFields(this);
  }

  void initializeInstanceData (ElementInfo ei, ThreadInfo ti) {









    if (superClass != null) { 
      superClass.initializeInstanceData(ei, ti);
    }

    for (int i=0; i<iFields.length; i++) {
      FieldInfo fi = iFields[i];
      fi.initialize(ei, ti);
    }
  }

  Map<String, MethodInfo> loadArrayMethods () {
    return new HashMap<String, MethodInfo>(0);
  }

  Map<String, MethodInfo> loadBuiltinMethods (String type) {
    type = null;  
     
    return new HashMap<String, MethodInfo>(0);
  }

  protected ClassInfo loadSuperClass (String superName) throws ClassInfoException {
    if (isObjectClassInfo()) {
      return null;
    }

    logger.finer("resolving superclass: ", superName, " of ", name);


    ClassInfo sci = resolveReferencedClass(superName);

    return sci;
  }

  protected Set<ClassInfo> loadInterfaces (String[] ifcNames) throws ClassInfoException {
    if (ifcNames == null || ifcNames.length == 0){
      return NO_INTERFACES;
      
    } else {
      Set<ClassInfo> set = new HashSet<ClassInfo>();

      for (String ifcName : ifcNames) {
        ClassInfo.logger.finer("resolving interface: ", ifcName, " of ", name);
        ClassInfo ifc = resolveReferencedClass(ifcName);
        set.add(ifc);
      }

      return set;
    }
  }
  
  
  protected void resolveClass() {
    if (!isObjectClassInfo){
      superClass = loadSuperClass(superClassName);
      releaseActions = superClass.releaseActions;
    }
    interfaces = loadInterfaces(interfaceNames);



    isWeakReference = isWeakReference0();
    isEnum = isEnum0();
  }

  
  public ClassInfo resolveReferencedClass(String cname) {
    if(name.equals(cname)) {
      return this;
    }


    ClassInfo ci = classLoader.getAlreadyResolvedClassInfo(cname);
    if(ci != null) {
      return ci;
    }
 

    ci = classLoader.loadClass(cname);
    classLoader.addResolvedClass(ci);

    return ci;
  }

  protected int linkFields (FieldInfo[] fields, int idx, int off){
    for (FieldInfo fi: fields) {      
      fi.linkToClass(this, idx, off);
      
      int storageSize = fi.getStorageSize();      
      off += storageSize;
      idx++;
    }
    
    return off;
  }
  
  protected void linkFields() {

    if(superClass != null) {
      int superDataSize = superClass.instanceDataSize;
      instanceDataSize = linkFields( iFields,  superClass.nInstanceFields, superDataSize);
      nInstanceFields = superClass.nInstanceFields + iFields.length;
      instanceDataOffset = superClass.instanceDataSize;
      
    } else {
      instanceDataSize = linkFields( iFields, 0, 0);
      nInstanceFields = iFields.length;
      instanceDataOffset = 0;
    }
    

    staticDataSize = linkFields( sFields, 0, 0);
  }


  protected void checkInheritedAnnotations (){
    
  }
  
  @Override
  public String toString() {
    return "ClassInfo[name=" + name + "]";
  }

  protected MethodInfo getFinalizer0 () {
    MethodInfo mi = getMethod("finalize()V", true);



    if ((mi != null) && (!mi.getClassInfo().isObjectClassInfo())) {
      return mi;
    }

    return null;
  }

  protected boolean isObjectClassInfo0 () {
	if (name.equals("java.lang.Object")) {
	  return true;
	}
	return false;
  }

  protected boolean isStringClassInfo0 () {
    if(name.equals("java.lang.String")) {
      return true;
    }
    return false;
  }

  protected boolean isRefClassInfo0 () {
    if(name.equals("java.lang.ref.Reference")) {
      return true;
    }
    return false;
  }

  protected boolean isWeakReference0 () {
	if(name.equals("java.lang.ref.WeakReference")) {
      return true;
	}

    for (ClassInfo ci = this; !ci.isObjectClassInfo(); ci = ci.superClass) {
      if (ci.isWeakReference()) {
        return true;
      }
    }

    return false;
  }

  protected boolean isEnum0 () {
	if(name.equals("java.lang.Enum")) {
      return true;
	}

    for (ClassInfo ci = this; !ci.isObjectClassInfo(); ci = ci.superClass) {
      if (ci.isEnum()) {
        return true;
      }
    }

    return false;
  }

  protected boolean isThreadClassInfo0 () {
    if(name.equals("java.lang.Thread")) {
      return true;
    }
    return false;
  }


  
  public ClassInfo cloneFor (ClassLoaderInfo cl) {
    ClassInfo ci;

    try {
      ci = (ClassInfo)clone();

      ci.classLoader = cl;
      ci.interfaces = new HashSet<ClassInfo>();
      ci.resolveClass();

      ci.id = -1;
      ci.uniqueId = -1;

      if (methods != Collections.EMPTY_MAP){
        ci.methods = (Map<String, MethodInfo>)((HashMap<String, MethodInfo>) methods).clone();
      }

      for(Map.Entry<String, MethodInfo> e: ci.methods.entrySet()) {
        MethodInfo mi = e.getValue();
        e.setValue(mi.getInstanceFor(ci));
      }

      ci.iFields = new FieldInfo[iFields.length];
      for(int i=0; i<iFields.length; i++) {
        ci.iFields[i] = iFields[i].getInstanceFor(ci);
      }

      ci.sFields = new FieldInfo[sFields.length];
      for(int i=0; i<sFields.length; i++) {
        ci.sFields[i] = sFields[i].getInstanceFor(ci);
      }

      if(nativePeer != null) {
        ci.nativePeer = NativePeer.getNativePeer(ci);
      }

      ci.setAssertionStatus();

    } catch (CloneNotSupportedException cnsx){
      cnsx.printStackTrace();
      return null;
    }

    VM.getVM().notifyClassLoaded(ci);
    return ci;
  }
  

  public StackFrame createStackFrame (ThreadInfo ti, MethodInfo callee){
    return null;
  }
  
  public DirectCallStackFrame createDirectCallStackFrame (ThreadInfo ti, MethodInfo callee, int nLocalSlots){
    return null;
  }
  
  public DirectCallStackFrame createRunStartStackFrame (ThreadInfo ti, MethodInfo miRun){
    return null;
  }
}


