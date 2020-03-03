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



public abstract class FieldInfo extends InfoObject implements GenericSignatureHolder {



  static final int NEVER_BREAK = 0x10000;
  


  static final int BREAK_SHARED = 0x20000;


  protected int attributes;

  
  protected final String name;
  protected String type;  
  protected final String signature; 
  protected int storageSize;

  protected ClassInfo ci; 
  protected int fieldIndex; 



  protected int storageOffset;



  protected  Object cv;

  protected String genericSignature;

  protected int modifiers;
  
  public static FieldInfo create (String name, String signature, int modifiers){
    switch(signature.charAt(0)){
      case 'Z':
        return new BooleanFieldInfo(name, modifiers);
      case 'B':
        return new ByteFieldInfo(name, modifiers);
      case 'S':
        return new ShortFieldInfo(name, modifiers);
      case 'C':
        return new CharFieldInfo(name, modifiers);
      case 'I':
        return new IntegerFieldInfo(name, modifiers);
      case 'J':
        return new LongFieldInfo(name, modifiers);
      case 'F':
        return new FloatFieldInfo(name, modifiers);
      case 'D':
        return new DoubleFieldInfo(name, modifiers);
      default:
        return new ReferenceFieldInfo(name, signature, modifiers);
    }
  }
  
  protected FieldInfo(String name, String signature, int modifiers) {
    this.name = name;
    this.signature = signature;
    this.modifiers = modifiers;
  }

  protected void linkToClass (ClassInfo ci, int idx, int off){
    this.ci = ci;
    this.fieldIndex = idx;
    this.storageOffset = off;
  }
  

  public void setConstantValue(Object constValue){
    cv = constValue;
  }

  public abstract String valueToString (Fields f);

  public boolean is1SlotField(){
    return false;
  }
  public boolean is2SlotField(){
    return false;
  }

  public boolean isBooleanField() {
    return false;
  }
  public boolean isByteField() {
    return false;
  }
  public boolean isCharField() {
    return false;
  }
  public boolean isShortField() {
    return false;
  }
  public boolean isIntField() {
    return false;
  }
  public boolean isLongField() {
    return false;
  }
  public boolean isFloatField(){
    return false;
  }
  public boolean isDoubleField(){
    return false;
  }

  public boolean isNumericField(){
    return false;
  }

  public boolean isFloatingPointField(){
    return false;
  }

  public boolean isReference () {
    return false;
  }

  public boolean isArrayField () {
    return false;
  }

  
  public ClassInfo getClassInfo () {
    return ci;
  }

  public Object getConstantValue () {
    return cv;
  }

  public abstract Object getValueObject (Fields data);

  public int getModifiers() {
    return modifiers;
  }

  public int getFieldIndex () {
    return fieldIndex;
  }

  
  public boolean isStatic () {
    return (modifiers & Modifier.STATIC) != 0;
  }

  
  public boolean isFinal () {
    return (modifiers & Modifier.FINAL) != 0;
  }

  public boolean isVolatile () {
    return (modifiers & Modifier.VOLATILE) != 0;
  }

  public boolean isTransient () {
    return (modifiers & Modifier.TRANSIENT) != 0;
  }

  public boolean isPublic () {
    return (modifiers & Modifier.PUBLIC) != 0;
  }

  public boolean isPrivate () {
    return (modifiers & Modifier.PRIVATE) != 0;
  }

  public boolean isProtected () {
    return (modifiers & Modifier.PROTECTED) != 0;
  }

  public boolean isPackagePrivate() {
    return (modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
  }
  
  
  public String getName () {
    return name;
  }

  
  public int getStorageSize () {
    return 1;
  }

  
  public String getType () {
    if (type == null){
      type = Types.getTypeName(signature);
    }
    return type;
  }
  
  public byte getTypeCode (){
    return Types.getTypeCode(signature);
  }

  public String getSignature(){
    return signature;
  }

  @Override
  public String getGenericSignature() {
    return genericSignature; 
  }

  @Override
  public void setGenericSignature(String sig){
    genericSignature = sig;
  }

  public ClassInfo getTypeClassInfo () {
    return ClassLoaderInfo.getCurrentResolvedClassInfo(getType());
  }

  public Class<? extends ChoiceGenerator<?>> getChoiceGeneratorType (){
    return null;
  }

  
  public abstract void initialize (ElementInfo ei, ThreadInfo ti);


  
  @Override
  public String toString () {
    StringBuilder sb = new StringBuilder();

    if (isStatic()) {
      sb.append("static ");
    }
    if (isFinal()) {
      sb.append("final ");
    }


    sb.append(getType());
    sb.append(' ');
    if (ci != null){ 
      sb.append(ci.getName());
    }
    sb.append('.');
    sb.append(name);

    return sb.toString();
  }
  


  void setAttributes (int a) {
    attributes = a;
  }

  public void addAttribute (int a){
    attributes |= a;
  }

  public int getAttributes () {
    return attributes;
  }

  public boolean breakShared() {
    return ((attributes & BREAK_SHARED) != 0);
  }
  
  public boolean neverBreak() {
    return ((attributes & NEVER_BREAK) != 0);    
  }
  
  public int getStorageOffset () {
    return storageOffset;
  }

  public String getFullName() {
    return ci.getName() + '.' + name;
  }

  
  public FieldInfo getInstanceFor(ClassInfo ci) {
    FieldInfo clone;

    try {
      clone = (FieldInfo)clone();
      clone.ci = ci;
    } catch (CloneNotSupportedException cnsx){
      cnsx.printStackTrace();
      return null;
    }

    return clone;
  }
}
