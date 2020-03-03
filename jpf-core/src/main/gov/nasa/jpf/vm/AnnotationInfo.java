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

import gov.nasa.jpf.JPFException;
import java.util.HashMap;


public class AnnotationInfo implements Cloneable {



  public static class Entry implements Cloneable {
    String key;
    Object value;
    
    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
    
    public Entry (String key, Object value){
      this.key = key;
      this.value = value;
    }
    
    @Override
	public Entry clone(){
      try {
        return (Entry) super.clone();
      } catch (CloneNotSupportedException cnsx){
        throw new JPFException("AnnotationInfo.Entry clone() failed");
      }
    }
  }
  
  public static class EnumValue {
    String eClassName;
    String eConst;
    
    EnumValue (String clsName, String constName){
      eClassName = clsName;
      eConst = constName;
    }
    public String getEnumClassName(){
      return eClassName;
    }
    public String getEnumConstName(){
      return eConst;
    }
    @Override
	public String toString(){
      return eClassName + '.' + eConst;
    }
  }

  public static class ClassValue {
    String name;

    ClassValue (String cn){
      name = cn;
    }

    public String getName(){
      return name;
    }
    @Override
	public String toString(){
      return name;
    }
  }

  static final Entry[] NONE = new Entry[0];
  






  static HashMap<String, AnnotationAttribute> annotationAttributes = new HashMap<String, AnnotationAttribute>();

  public static class AnnotationAttribute {
    Entry[] defaultEntries;
    boolean isInherited;

    AnnotationAttribute (Entry[] defaultEntries, boolean isInherited) {
      this.defaultEntries = defaultEntries;
      this.isInherited = isInherited;
    }
  }
  
  public static Object getEnumValue(String eType, String eConst){
    return new EnumValue( Types.getClassNameFromTypeName(eType), eConst);
  }

  public static Object getClassValue(String type){
    return new ClassValue( Types.getClassNameFromTypeName(type));
  }  
  
  protected String name;
  protected Entry[] entries;
  protected boolean isInherited = false;
    
  
  protected ClassLoaderInfo classLoader; 
  
  
  public AnnotationInfo (String name, ClassLoaderInfo classLoader, AnnotationParser parser) throws ClassParseException {
    this.name = name;
    this.classLoader = classLoader;
    
    parser.parse(this);
  }
  
  
  protected AnnotationInfo (AnnotationInfo exemplar){
    this.name = exemplar.name;
    this.classLoader = exemplar.classLoader;
    this.entries = exemplar.entries;
    this.isInherited = exemplar.isInherited;
  }
  

  public void setName (String name) throws ClassParseException {
    if (!this.name.equals(name)){
      throw new ClassParseException("wrong annotation name in classfile, expected " + this.name + ", found " + name);
    }
  }

  public void setEntries (Entry[] entries){
    this.entries = entries;
  }
  
  public void setInherited (boolean isInherited){
    this.isInherited = isInherited;
  }
  
  
  public AnnotationInfo (String name, Entry[] entries, boolean isInherited){
    this.name = name;
    this.entries = entries;
    this.isInherited = isInherited;
  }


  public boolean isInherited (){
    return this.isInherited;
  }
  
  public ClassLoaderInfo getClassLoaderInfo(){
    return classLoader;
  }

  public String getName() {
    return name;
  }
  
  protected AnnotationInfo cloneFor (ClassLoaderInfo cl){
    try {
      AnnotationInfo ai = (AnnotationInfo) clone();
      

      
      ai.classLoader = cl;
      
      return ai;
      
    } catch (CloneNotSupportedException cnsx){
      throw new JPFException("AnnotationInfo cloneFor() failed");
    }
  }
  
  
  public AnnotationInfo cloneForOverriddenValues(){
    try {
      AnnotationInfo ai = (AnnotationInfo) clone();
      ai.entries = entries.clone();
      return ai;
      
    } catch (CloneNotSupportedException cnsx){
      throw new JPFException("AnnotationInfo cloneFor() failed");
    }    
  }
  
  public void setClonedEntryValue (String key, Object newValue){
    for (int i=0; i<entries.length; i++){
      if (entries[i].getKey().equals(key)){
        entries[i] = new Entry( key, newValue);
        return;
      }
    }    
  }
  
  public Entry[] getEntries() {
    return entries;
  }
  
  
  public Object getValue (String key){    
    for (int i=0; i<entries.length; i++){
      if (entries[i].getKey().equals(key)){
        return entries[i].getValue();
      }
    }
    return null;
  }

  

  public Object value() {
    return getValue("value");
  }
  
  public String valueAsString(){
    Object v = value();
    return (v != null) ? v.toString() : null;
  }
  
  public String getValueAsString (String key){
    Object v = getValue(key);
    return (v != null) ? v.toString() : null;
  }
  
  public String[] getValueAsStringArray() {
    String a[] = null; 
    Object v = value();
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
    }
    
    return a;    
  }
  
  public String[] getValueAsStringArray (String key) {

    String a[] = null; 
    Object v = getValue(key);
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
    }
    
    return a;
  }
  
  public <T> T getValue (String key, Class<T> type){
    Object v = getValue(key);
    if (type.isInstance(v)){
      return (T)v;
    } else {
      return null;
    }
  }
  
  public boolean getValueAsBoolean (String key){
    Object v = getValue(key);
    if (v instanceof Boolean){
      return ((Boolean)v).booleanValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a boolean: " + v);
    }
  }
  
  public int getValueAsInt (String key){
    Object v = getValue(key);
    if (v instanceof Integer){
      return ((Integer)v).intValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not an int: " + v);
    }
  }

  public long getValueAsLong (String key){
    Object v = getValue(key);
    if (v instanceof Long){
      return ((Long)v).longValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a long: " + v);
    }
  }

  public float getValueAsFloat (String key){
    Object v = getValue(key);
    if (v instanceof Float){
      return ((Float)v).floatValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a float: " + v);
    }
  }
  
  public double getValueAsDouble (String key){
    Object v = getValue(key);
    if (v instanceof Double){
      return ((Double)v).doubleValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a double: " + v);
    }
  }
  
  public String asString() {
    StringBuilder sb = new StringBuilder();
    sb.append('@');
    sb.append(name);
    sb.append('[');
    for (int i=0; i<entries.length; i++){
      if (i > 0){
        sb.append(',');
      }
      sb.append(entries[i].getKey());
      sb.append('=');
      sb.append(entries[i].getValue());
    }
    sb.append(']');
    
    return sb.toString();
  }

}
