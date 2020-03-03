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


package java.lang;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import sun.reflect.ConstantPool;
import sun.reflect.annotation.AnnotationType;


@SuppressWarnings("unused")  
public final class Class<T> implements Serializable, GenericDeclaration, Type, AnnotatedElement {

  
  private static final long serialVersionUID = 3206093459760846163L + 1;


  private static Annotation[] emptyAnnotations; 
  
  private String name;

  private ClassLoader classLoader;
  
  
  private int nativeId;

  
  private boolean isPrimitive;
  
  private Class() {}

  public native boolean isArray ();

  @Override
  public native Annotation[] getAnnotations();

  @Override
  public native <A extends Annotation> A getAnnotation( Class<A> annotationCls);


  public native boolean isAnnotation ();
  @Override
  public native boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

  public native Class<?> getComponentType ();

  public native Field[] getFields() throws SecurityException;
  
  public native Field getDeclaredField (String fieldName) throws NoSuchFieldException,
                                                          SecurityException;

  public native Field[] getDeclaredFields () throws SecurityException;

  public native Method getDeclaredMethod (String mthName, Class<?>... paramTypes)
                            throws NoSuchMethodException, SecurityException;

  public native Method getMethod (String mthName, Class<?>... paramTypes)
                    throws NoSuchMethodException, SecurityException;

  public native Method[] getDeclaredMethods () throws SecurityException;

  public native Method[] getMethods () throws SecurityException;
    
  public native Constructor<?>[] getDeclaredConstructors() throws SecurityException;

  public native Constructor<?>[] getConstructors() throws SecurityException;

  private native byte[] getByteArrayFromResourceStream(String name);

  public InputStream getResourceAsStream (String name) {
    return getClassLoader().getResourceAsStream(getResolvedName(name));
  }

  private native String getResolvedName (String rname);

  public URL getResource (String rname) {
    String resolvedName = getResolvedName(rname);
    return getClassLoader().getResource(resolvedName);
  }

  public Package getPackage() {

    String pkgName = null;

    int idx = name.lastIndexOf('.');
    if (idx >=0){
      pkgName = name.substring(0,idx);

      Package pkg = new Package(pkgName,
                                "spectitle", "specversion", "specvendor",
                                "impltitle", "implversion", "implvendor",
                                 null, getClassLoader());
        return pkg;

    } else { 
      return null;
    }

  }



  public native T[] getEnumConstants();


  T[] getEnumConstantsShared() {
    return getEnumConstants();
  }
  


  private transient Map<String, T> enumConstantDirectory = null;


  Map<String,T> enumConstantDirectory() {
    if (enumConstantDirectory == null) {
      Map<String,T> map = new HashMap<String,T>();

      T[] ae = getEnumConstants();
      for (T e: ae) {
        map.put(((Enum)e).name(), e);
      }

      enumConstantDirectory = map;
    }
    return enumConstantDirectory;
  }


  public native Constructor<T> getDeclaredConstructor (Class<?>... paramTypes)
              throws NoSuchMethodException, SecurityException;

  public native Field getField (String fieldName) throws NoSuchFieldException,
                                                  SecurityException;

  public native boolean isInstance (Object o);

  public native boolean isAssignableFrom (Class<?> clazz);

  public native boolean isInterface();

  public native Constructor<T> getConstructor (Class<?>... argTypes) throws NoSuchMethodException, SecurityException;

  public native int getModifiers();

  public native Class<?>[] getInterfaces();


  public String getName () {
    return name;
  }

  public String getSimpleName () {
    int idx; 
    Class<?> enclosingClass = getEnclosingClass();
    
    if(enclosingClass!=null){
      idx = enclosingClass.getName().length();
    } else{
      idx = name.lastIndexOf('.');
    }
    
    return name.substring(idx+1);
  }

  static native Class<?> getPrimitiveClass (String clsName);

   
  public static native Class<?> forName (String clsName) throws ClassNotFoundException;

  public static Class<?> forName (String clsName, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
    Class<?> cls;
    if (loader == null){
      cls = forName(clsName);
    } else {
      cls = loader.loadClass(clsName);
    }
    
    if (initialize) {
      cls.initialize0();
    }
    return cls;
  }

  
  private native void initialize0 ();
  
  public boolean isPrimitive () {
    return isPrimitive;
  }

  public native Class<? super T> getSuperclass ();

  public native T newInstance () throws InstantiationException,
                                      IllegalAccessException;

  @Override
  public String toString () {
    return (isInterface() ? "interface " : "class ") + name;
  }

  @SuppressWarnings("unchecked")
  public T cast(Object o) {
    if (o != null && !isInstance(o)) throw new ClassCastException();
    return (T) o;
  }
  
  @SuppressWarnings("unchecked")
  public <U> Class<? extends U> asSubclass(Class<U> clazz) {
    if (clazz.isAssignableFrom(this)) {
      return (Class<? extends U>) this;
    } else {
      throw new ClassCastException("" + this + " is not a " + clazz);
    }
  }

  native public boolean desiredAssertionStatus ();

  public ClassLoader getClassLoader() {
    return classLoader;
  }
  
  ClassLoader getClassLoader0() {
  	return classLoader;
  }

  native ConstantPool getConstantPool();

  native void setAnnotationType (AnnotationType at);

  native AnnotationType getAnnotationType();
  
  @Override
  public TypeVariable<Class<T>>[] getTypeParameters() {
    throw new UnsupportedOperationException();
  }
  
  public Type getGenericSuperclass() {
    throw new UnsupportedOperationException();
  }
  
  public Type[] getGenericInterfaces() {
    throw new UnsupportedOperationException();
  }

  public Object[] getSigners() {
    throw new UnsupportedOperationException();
  }

  void setSigners(Object[] signers) {
    signers = null;  
    throw new UnsupportedOperationException();
  }
  
  public Method getEnclosingMethod() {
    throw new UnsupportedOperationException();
  }

  public Constructor<?> getEnclosingConstructor() {
    throw new UnsupportedOperationException();
  }

  public Class<?> getDeclaringClass() {
    throw new UnsupportedOperationException();
  }

  public native Class<?> getEnclosingClass();
  
  public String getCanonicalName() {
    throw new UnsupportedOperationException();
  }

  public boolean isAnonymousClass() {
    throw new UnsupportedOperationException();
  }

  public boolean isLocalClass() {
    throw new UnsupportedOperationException();
  }

  public boolean isMemberClass() {
    throw new UnsupportedOperationException();
  }

  public Class<?>[] getClasses() {
    throw new UnsupportedOperationException();
  }
  
  public Class<?>[] getDeclaredClasses() throws SecurityException {
    throw new UnsupportedOperationException();
  }
  
  public java.security.ProtectionDomain getProtectionDomain() {
    throw new UnsupportedOperationException();
  }

  void setProtectionDomain0(java.security.ProtectionDomain pd) {
    pd = null;  
    throw new UnsupportedOperationException();
  }

  public boolean isEnum() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public Annotation[] getDeclaredAnnotations() {
    throw new UnsupportedOperationException();
  }

  public boolean isSynthetic (){
    final int SYNTHETIC = 0x00001000;
    return (getModifiers() & SYNTHETIC) != 0;
  }
}
