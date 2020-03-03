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



package gov.nasa.jpf.jvm;

import gov.nasa.jpf.jvm.JVMByteCodeReader;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.BailOut;
import gov.nasa.jpf.util.BinaryClassSource;

import java.io.File;


public class ClassFile extends BinaryClassSource {

  public static final int CONSTANT_UTF8 = 1;
  public static final int CONSTANT_INTEGER = 3;
  public static final int CONSTANT_FLOAT = 4;
  public static final int CONSTANT_LONG = 5;
  public static final int CONSTANT_DOUBLE = 6;
  public static final int CONSTANT_CLASS = 7;
  public static final int CONSTANT_STRING = 8;
  public static final int FIELD_REF = 9;
  public static final int METHOD_REF = 10;
  public static final int INTERFACE_METHOD_REF = 11;
  public static final int NAME_AND_TYPE = 12;
  public static final int METHOD_HANDLE = 15;
  public static final int METHOD_TYPE = 16;
  public static final int INVOKE_DYNAMIC = 18;

  public static final int REF_GETFIELD = 1;
  public static final int REF_GETSTATIC = 2;
  public static final int REF_PUTFIELD = 3;
  public static final int REF_PUTSTATIC = 4;
  public static final int REF_INVOKEVIRTUAL = 5;
  public static final int REF_INVOKESTATIC = 6;
  public static final int REF_INVOKESPECIAL = 7;
  public static final int REF_NEW_INVOKESPECIAL = 8;
  public static final int REF_INVOKEINTERFACE = 9;


  public static enum CpInfo {
    Unused_0,                 
    ConstantUtf8,             
    Unused_2,                 
    ConstantInteger,          
    ConstantFloat,            
    ConstantLong,             
    ConstantDouble,           
    ConstantClass,            
    ConstantString,           
    FieldRef,                 
    MethodRef,                
    InterfaceMethodRef,       
    NameAndType,              
    Unused_13,
    Unused_14,
    MethodHandle,             
    MethodType,               
    Unused_17,
    InvokeDynamic             
  }


  String requestedTypeName; 


  int[] cpPos;     
  Object[] cpValue; 
  

  public ClassFile (byte[] data, int offset){
    super(data,offset);
  }

  public ClassFile (byte[] data){
    super(data,0);
  }

  public ClassFile (String typeName, byte[] data){
    super(data,0);
    
    this.requestedTypeName = typeName;
  }
  
  public ClassFile (String typeName, byte[] data, int offset){
    super(data, offset);
    
    this.requestedTypeName = typeName;
  }

  public ClassFile (File file) throws ClassParseException {
    super(file);
  }

  public ClassFile (String pathName)  throws ClassParseException {
    super( new File(pathName));
  }



  
  
  public void setData(byte[] newData){
    if (cpPos != null){
      throw new JPFException("concurrent modification of ClassFile data");
    }
    
    data = newData;
  }
  
  
  public String getRequestedTypeName(){
    return requestedTypeName;
  }



  public static final String SYNTHETIC_ATTR = "Synthetic";
  public static final String DEPRECATED_ATTR = "Deprecated";
  public static final String SIGNATURE_ATTR = "Signature";
  public static final String RUNTIME_INVISIBLE_ANNOTATIONS_ATTR = "RuntimeInvisibleAnnotations";
  public static final String RUNTIME_VISIBLE_ANNOTATIONS_ATTR = "RuntimeVisibleAnnotations";
  public static final String RUNTIME_VISIBLE_TYPE_ANNOTATIONS_ATTR = "RuntimeVisibleTypeAnnotations";


  public static final String CONST_VALUE_ATTR = "ConstantValue";

  protected final static String[] stdFieldAttrs = {
    CONST_VALUE_ATTR, SYNTHETIC_ATTR, DEPRECATED_ATTR, SIGNATURE_ATTR,
    RUNTIME_INVISIBLE_ANNOTATIONS_ATTR, RUNTIME_VISIBLE_ANNOTATIONS_ATTR, RUNTIME_VISIBLE_TYPE_ANNOTATIONS_ATTR };



  public static final String CODE_ATTR = "Code";
  public static final String EXCEPTIONS_ATTR = "Exceptions";
  public static final String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS_ATTR = "RuntimeInvisibleParameterAnnotations";
  public static final String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTR = "RuntimeVisibleParameterAnnotations";
  public static final String ANNOTATIONDEFAULT_ATTR = "AnnotationDefault";

  protected final static String[] stdMethodAttrs = { 
    CODE_ATTR, EXCEPTIONS_ATTR, SYNTHETIC_ATTR, DEPRECATED_ATTR, SIGNATURE_ATTR,
    RUNTIME_INVISIBLE_ANNOTATIONS_ATTR, RUNTIME_VISIBLE_ANNOTATIONS_ATTR,
    RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS_ATTR,
    RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTR,
    RUNTIME_VISIBLE_TYPE_ANNOTATIONS_ATTR,
    ANNOTATIONDEFAULT_ATTR
  };



  public static final String LINE_NUMBER_TABLE_ATTR = "LineNumberTable";
  public static final String LOCAL_VAR_TABLE_ATTR = "LocalVariableTable";

  protected final static String[] stdCodeAttrs = { LINE_NUMBER_TABLE_ATTR, LOCAL_VAR_TABLE_ATTR, RUNTIME_VISIBLE_TYPE_ANNOTATIONS_ATTR };



  public static final String  SOURCE_FILE_ATTR = "SourceFile";
  public static final String  INNER_CLASSES_ATTR = "InnerClasses";
  public static final String  ENCLOSING_METHOD_ATTR = "EnclosingMethod";
  public static final String  BOOTSTRAP_METHOD_ATTR = "BootstrapMethods";
  
  protected final static String[] stdClassAttrs = {
    SOURCE_FILE_ATTR, DEPRECATED_ATTR, INNER_CLASSES_ATTR, DEPRECATED_ATTR, SIGNATURE_ATTR,
    RUNTIME_INVISIBLE_ANNOTATIONS_ATTR, RUNTIME_VISIBLE_ANNOTATIONS_ATTR, RUNTIME_VISIBLE_TYPE_ANNOTATIONS_ATTR,
    ENCLOSING_METHOD_ATTR, BOOTSTRAP_METHOD_ATTR };


  protected String internStdAttrName(int cpIdx, String name, String[] stdNames){
    for (int i=0; i<stdNames.length; i++){
      if (stdNames[i] == name) return name;
    }
    for (int i=0; i<stdNames.length; i++){
      String stdName = stdNames[i];
      if (stdName.equals(name)){
        cpValue[cpIdx] = stdName;
        return stdName;
      }
    }
    return name;
  }





  public String utf8At(int utf8InfoIdx){

    return (String) cpValue[utf8InfoIdx];
  }

  public int intAt(int intInfoIdx){

    return (Integer) cpValue[intInfoIdx];
  }

  public float floatAt(int floatInfoIdx){

    return (Float) cpValue[floatInfoIdx];
  }

  public long longAt(int longInfoIdx){

    return (Long) cpValue[longInfoIdx];
  }

  public double doubleAt(int doubleInfoIdx){

    return (Double) cpValue[doubleInfoIdx];
  }


  public String classNameAt(int classInfoIdx){

    return (String) cpValue[classInfoIdx];
  }

  public String stringAt(int stringInfoIdx){

    return (String) cpValue[stringInfoIdx];
  }




  public String refClassNameAt(int cpIdx){
    return (String) cpValue[ u2(cpPos[cpIdx]+1)];
  }
  public String refNameAt(int cpIdx){
    return utf8At( u2( cpPos[ u2(cpPos[cpIdx]+3)]+1));
  }
  public String refDescriptorAt(int cpIdx){
    return utf8At( u2( cpPos[ u2(cpPos[cpIdx]+3)]+3));
  }

  public int mhRefTypeAt (int methodHandleInfoIdx){
    return u1(cpPos[methodHandleInfoIdx]+1);
  }
  public int mhMethodRefIndexAt  (int methodHandleInfoIdx){
    return u2(cpPos[methodHandleInfoIdx]+2);
  }
  

  public String fieldClassNameAt(int fieldRefInfoIdx){

    return (String) cpValue[ u2(cpPos[fieldRefInfoIdx]+1)];
  }
  public String fieldNameAt(int fieldRefInfoIdx){
    return utf8At( u2( cpPos[ u2(cpPos[fieldRefInfoIdx]+3)]+1));
  }
  public String fieldDescriptorAt(int fieldRefInfoIdx){
    return utf8At( u2( cpPos[ u2(cpPos[fieldRefInfoIdx]+3)]+3));
  }

  public String methodClassNameAt(int methodRefInfoIdx){
    return (String) cpValue[ u2(cpPos[methodRefInfoIdx]+1)];
  }
  public String methodNameAt(int methodRefInfoIdx){
    return utf8At( u2( cpPos[ u2(cpPos[methodRefInfoIdx]+3)]+1));
  }
  public String methodDescriptorAt(int methodRefInfoIdx){
    return utf8At( u2( cpPos[ u2(cpPos[methodRefInfoIdx]+3)]+3));
  }

  public String methodTypeDescriptorAt (int methodTypeInfoIdx){
    return utf8At( u2(cpPos[methodTypeInfoIdx]+1));
  }
  
  public String interfaceMethodClassNameAt(int ifcMethodRefInfoIdx){
    return (String) cpValue[ u2(cpPos[ifcMethodRefInfoIdx]+1)];
  }
  public String interfaceMethodNameAt(int ifcMethodRefInfoIdx){
    return utf8At( u2( cpPos[ u2(cpPos[ifcMethodRefInfoIdx]+3)]+1));
  }
  public String interfaceMethodDescriptorAt(int ifcMethodRefInfoIdx){
    return utf8At( u2( cpPos[ u2(cpPos[ifcMethodRefInfoIdx]+3)]+3));
  }
  
  public int bootstrapMethodIndex (int cpInvokeDynamicIndex){
    return u2(cpPos[cpInvokeDynamicIndex]+1);
  }
  public String samMethodNameAt(int cpInvokeDynamicIndex) {
    return utf8At( u2( cpPos[ u2(cpPos[cpInvokeDynamicIndex]+3)]+1)); 
  }
  public String callSiteDescriptor(int cpInvokeDynamicIndex) {
    return utf8At( u2( cpPos[ u2(cpPos[cpInvokeDynamicIndex]+3)]+3)); 
  }
  
  public String getRefTypeName (int refCode){
    switch (refCode){
      case REF_GETFIELD:      return "getfield";
      case REF_GETSTATIC:     return "getstatic";
      case REF_PUTFIELD:      return "putfield";
      case REF_PUTSTATIC:     return "putstatic";
      case REF_INVOKEVIRTUAL: return "invokevirtual";
      case REF_INVOKESTATIC:  return "invokestatic";
      case REF_INVOKESPECIAL: return "invokespecial";
      case REF_NEW_INVOKESPECIAL: return "new-invokespecial";
      case REF_INVOKEINTERFACE: return "invokeinterface";
      default:
        return "<unknown>";
    }
  }
  
  public String getTypeName (int typeCode){
    switch(typeCode){
      case 4: return "boolean";
      case 5: return "char";
      case 6: return "float";
      case 7: return "double";
      case 8: return "byte";
      case 9: return "short";
      case 10: return "int";
      case 11: return "long";
      default:
        return "<unknown>";
    }
  }

  @Override
  public int getPos(){
    return pos;
  }

  public int getPc(){
    return pc;
  }



  public int getNumberOfCpEntries(){
    return cpValue.length;
  }

  public Object getCpValue (int i){
    return cpValue[i];
  }

  public int getCpTag (int i){
    return data[cpPos[i]];
  }

  
  public int getDataPosOfCpEntry (int i){
    return cpPos[i];
  }



  public Object getConstValueAttribute(int dataPos){
    int cpIdx = u2(dataPos);
    Object v = cpValue[cpIdx];
    return v;
  }

  public String getSourceFileAttribute(int dataPos){


    int cpIdx = u2(dataPos + 6);
    Object v = cpValue[cpIdx];
    return (String)v;
  }

  


  public final int u1(int dataIdx){
    return data[dataIdx] & 0xff;
  }

  public final int u2(int dataIdx){
    return ((data[dataIdx]&0xff) << 8) | (data[dataIdx+1]&0xff);
  }

  public final int i1(int dataIdx) {
    return data[dataIdx++];
  }

  public final int i2(int dataIdx) {
    int idx = dataIdx;
    return (data[idx++] << 8) | (data[idx]&0xff);
  }

  public final int readU2(){
    int idx = pos;
    pos += 2;
    return ((data[idx++]&0xff) << 8) | (data[idx]&0xff);
  }

  public final int readI2() {
    int idx = pos;
    pos += 2;
    return (data[idx++] << 8) | (data[idx]&0xff);
  }

  public final int readI4(){
    int idx = pos;
    pos += 4;
    byte[] data = this.data;

    return (data[idx++] <<24) | ((data[idx++]&0xff) << 16) | ((data[idx++]&0xff) << 8) | (data[idx]&0xff);
  }

  

  private void setClass(ClassFileReader reader, String clsName, String superClsName, int flags, int cpCount) throws ClassParseException {
    int p = pos;
    reader.setClass( this, clsName, superClsName, flags, cpCount);
    pos = p;
  }

  private void setInterfaceCount(ClassFileReader reader, int ifcCount){
    int p = pos;
    reader.setInterfaceCount( this, ifcCount);
    pos = p;
  }
  private void setInterface(ClassFileReader reader, int ifcIndex, String ifcName){
    int p = pos;
    reader.setInterface( this, ifcIndex, ifcName);
    pos = p;
  }
  private void setInterfacesDone(ClassFileReader reader){
    int p = pos;
    reader.setInterfacesDone( this);
    pos = p;
  }


  private void setFieldCount(ClassFileReader reader, int fieldCount){
    int p = pos;
    reader.setFieldCount( this, fieldCount);
    pos = p;

  }
  private void setField(ClassFileReader reader, int fieldIndex, int accessFlags, String name, String descriptor){
    int p = pos;
    reader.setField( this, fieldIndex, accessFlags, name, descriptor);
    pos = p;
  }
  private void setFieldAttributeCount(ClassFileReader reader, int fieldIndex, int attrCount){
    int p = pos;
    reader.setFieldAttributeCount( this, fieldIndex, attrCount);
    pos = p;
  }
  private void setFieldAttribute(ClassFileReader reader, int fieldIndex, int attrIndex, String name, int attrLength){
    int p = pos + attrLength;
    reader.setFieldAttribute( this, fieldIndex, attrIndex, name, attrLength);
    pos = p;
  }
  private void setFieldAttributesDone(ClassFileReader reader, int fieldIndex){
    int p = pos;
    reader.setFieldAttributesDone( this, fieldIndex);
    pos = p;
  }
  private void setFieldDone(ClassFileReader reader, int fieldIndex){
    int p = pos;
    reader.setFieldDone( this, fieldIndex);
    pos = p;
  }
  private void setFieldsDone(ClassFileReader reader){
    int p = pos;
    reader.setFieldsDone( this);
    pos = p;
  }
  private void setConstantValue(ClassFileReader reader, Object tag, Object value){
    int p = pos;
    reader.setConstantValue( this, tag, value);
    pos = p;
  }

  private void setMethodCount(ClassFileReader reader, int methodCount){
    int p = pos;
    reader.setMethodCount( this, methodCount);
    pos = p;
  }
  private void setMethod(ClassFileReader reader, int methodIndex, int accessFlags, String name, String descriptor){
    int p = pos;
    reader.setMethod( this, methodIndex, accessFlags, name, descriptor);
    pos = p;
  }
  private void setMethodAttributeCount(ClassFileReader reader, int methodIndex, int attrCount){
    int p = pos;
    reader.setMethodAttributeCount( this, methodIndex, attrCount);
    pos = p;
  }
  private void setMethodAttribute(ClassFileReader reader, int methodIndex, int attrIndex, String name, int attrLength){
    int p = pos + attrLength;
    reader.setMethodAttribute( this, methodIndex, attrIndex, name, attrLength);
    pos = p;
  }
  private void setMethodAttributesDone(ClassFileReader reader, int methodIndex){
    int p = pos;
    reader.setMethodAttributesDone( this, methodIndex);
    pos = p;
  }
  private void setMethodDone(ClassFileReader reader, int methodIndex){
    int p = pos;
    reader.setMethodDone( this, methodIndex);
    pos = p;
  }
  private void setMethodsDone(ClassFileReader reader){
    int p = pos;
    reader.setMethodsDone( this);
    pos = p;
  }
  private void setExceptionCount(ClassFileReader reader, Object tag, int exceptionCount){
    int p = pos;
    reader.setExceptionCount( this, tag, exceptionCount);
    pos = p;
  }
  private void setExceptionsDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setExceptionsDone( this, tag);
    pos = p;
  }
  private void setException(ClassFileReader reader, Object tag, int exceptionIndex, String exceptionType){
    int p = pos;
    reader.setException( this, tag, exceptionIndex, exceptionType);
    pos = p;
  }
  private void setCode(ClassFileReader reader, Object tag, int maxStack, int maxLocals, int codeLength){
    int p = pos + codeLength;
    reader.setCode( this, tag, maxStack, maxLocals, codeLength);
    pos = p;
  }
  private void setExceptionTableCount(ClassFileReader reader, Object tag, int exceptionTableCount){
    int p = pos;
    reader.setExceptionHandlerTableCount( this, tag, exceptionTableCount);
    pos = p;
  }
  private void setExceptionTableEntry(ClassFileReader reader, Object tag, int exceptionIndex,
          int startPc, int endPc, int handlerPc, String catchType){
    int p = pos;
    reader.setExceptionHandler( this, tag, exceptionIndex, startPc, endPc, handlerPc, catchType);
    pos = p;
  }
  private void setExceptionTableDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setExceptionHandlerTableDone( this, tag);
    pos = p;
  }

  private void setCodeAttributeCount(ClassFileReader reader, Object tag, int attrCount){
    int p = pos;
    reader.setCodeAttributeCount( this, tag, attrCount);
    pos = p;
  }
  private void setCodeAttribute(ClassFileReader reader, Object tag, int attrIndex, String name, int attrLength){
    int p = pos + attrLength;
    reader.setCodeAttribute( this, tag, attrIndex, name, attrLength);
    pos = p;
  }
  private void setCodeAttributesDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setCodeAttributesDone( this, tag);
    pos = p;
  }
          
  private void setLineNumberTableCount(ClassFileReader reader, Object tag, int lineNumberCount){
    int p = pos;
    reader.setLineNumberTableCount( this, tag, lineNumberCount);
    pos = p;
  }
  private void setLineNumber(ClassFileReader reader, Object tag, int lineIndex, int lineNumber, int startPc){
    int p = pos;
    reader.setLineNumber( this, tag, lineIndex, lineNumber, startPc);
    pos = p;
  }
  private void setLineNumberTableDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setLineNumberTableDone( this, tag);
    pos = p;
  }

  private void setLocalVarTableCount(ClassFileReader reader, Object tag, int localVarCount){
    int p = pos;
    reader.setLocalVarTableCount( this, tag, localVarCount);
    pos = p;
  }
  private void setLocalVar(ClassFileReader reader, Object tag, int localVarIndex, String varName, String descriptor,
                      int scopeStartPc, int scopeEndPc, int slotIndex){
    int p = pos;
    reader.setLocalVar( this, tag, localVarIndex, varName, descriptor, scopeStartPc, scopeEndPc, slotIndex);
    pos = p;
  }
  private void setLocalVarTableDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setLocalVarTableDone( this, tag);
    pos = p;
  }


  private void setClassAttributeCount(ClassFileReader reader, int attrCount){
    int p = pos;
    reader.setClassAttributeCount( this, attrCount);
    pos = p;
  }
  private void setClassAttribute(ClassFileReader reader, int attrIndex, String name, int attrLength){
    int p = pos + attrLength;
    reader.setClassAttribute( this, attrIndex, name, attrLength);
    pos = p;
  }
  private void setClassAttributesDone(ClassFileReader reader){
    int p = pos;
    reader.setClassAttributesDone(this);
    pos = p;
  }

  private void setSourceFile(ClassFileReader reader, Object tag, String pathName){
    int p = pos;
    reader.setSourceFile( this, tag, pathName);
    pos = p;
  }
  
  private void setBootstrapMethodCount (ClassFileReader reader, Object tag, int bootstrapMethodCount){
    int p = pos;
    reader.setBootstrapMethodCount( this, tag, bootstrapMethodCount);
    pos = p;    
  }
  private void setBootstrapMethod (ClassFileReader reader, Object tag, int idx, 
                                   int refKind, String cls, String mth, String descriptor, int[] cpArgs){
    int p = pos;
    reader.setBootstrapMethod( this, tag, idx, refKind, cls, mth, descriptor, cpArgs);
    pos = p;    
  }
  private void setBootstrapMethodsDone (ClassFileReader reader, Object tag){
    int p = pos;
    reader.setBootstrapMethodsDone( this, tag);
    pos = p;    
  }
  
  private void setInnerClassCount(ClassFileReader reader, Object tag, int innerClsCount){
    int p = pos;
    reader.setInnerClassCount( this, tag, innerClsCount);
    pos = p;
  }
  private void setInnerClass(ClassFileReader reader, Object tag, int innerClsIndex, String outerName, String innerName,
          String innerSimpleName, int accessFlags){
    int p = pos;
    reader.setInnerClass( this, tag, innerClsIndex, outerName, innerName, innerSimpleName, accessFlags);
    pos = p;
  }
  private void setEnclosingMethod(ClassFileReader reader, Object tag, String enclosingClass, String enclosedMethod, String descriptor){
    int p = pos;
	  reader.setEnclosingMethod( this, tag, enclosingClass, enclosedMethod, descriptor);
	  pos = p;
  }
  private void setInnerClassesDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setInnerClassesDone(this, tag);
    pos = p;
  }

  private void setAnnotationCount(ClassFileReader reader, Object tag, int annotationCount){
    int p = pos;
    reader.setAnnotationCount( this, tag, annotationCount);
    pos = p;
  }
  private void setAnnotation(ClassFileReader reader, Object tag, int annotationIndex, String annotationType){
    int p = pos;
    reader.setAnnotation( this, tag, annotationIndex, annotationType);
    pos = p;
  }
  private void setAnnotationsDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setAnnotationsDone(this, tag);
    pos = p;
  }

  private void setTypeAnnotationCount(ClassFileReader reader, Object tag, int annotationCount){
    int p = pos;
    reader.setTypeAnnotationCount( this, tag, annotationCount);
    pos = p;
  }
  private void setTypeAnnotationsDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setTypeAnnotationsDone(this, tag);
    pos = p;
  }

  
  private void setAnnotationValueCount(ClassFileReader reader, Object tag, int annotationIndex, int nValuePairs){
    int p = pos;
    reader.setAnnotationValueCount( this, tag, annotationIndex, nValuePairs);
    pos = p;
  }
  private void setPrimitiveAnnotationValue(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, Object val){
    int p = pos;
    reader.setPrimitiveAnnotationValue( this, tag, annotationIndex, valueIndex, elementName, arrayIndex, val);
    pos = p;
  }
  private void setStringAnnotationValue(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String s){
    int p = pos;
    reader.setStringAnnotationValue( this, tag, annotationIndex, valueIndex, elementName, arrayIndex, s);
    pos = p;
  }
  private void setClassAnnotationValue(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String typeName){
    int p = pos;
    reader.setClassAnnotationValue( this, tag, annotationIndex, valueIndex, elementName, arrayIndex, typeName);
    pos = p;
  }
  private void setEnumAnnotationValue(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String enumType, String enumValue){
    int p = pos;
    reader.setEnumAnnotationValue( this, tag, annotationIndex, valueIndex, elementName, arrayIndex, enumType, enumValue);
    pos = p;
  }

  private void setAnnotationValueElementCount(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex,
          String elementName, int elementCount){
    int p = pos;
    reader.setAnnotationValueElementCount(this, tag, annotationIndex, valueIndex, elementName, elementCount);
    pos = p;
  }
  private void setAnnotationValueElementsDone(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex,
          String elementName){
    int p = pos;
    reader.setAnnotationValueElementsDone(this, tag, annotationIndex, valueIndex, elementName);
    pos = p;
  }

  public void setAnnotationValuesDone(ClassFileReader reader, Object tag, int annotationIndex){
    int p = pos;
    reader.setAnnotationValuesDone(this, tag, annotationIndex);
    pos = p;
  }

  private void setParameterCount(ClassFileReader reader, Object tag, int parameterCount){
    int p = pos;
    reader.setParameterCount(this, tag, parameterCount);
    pos = p;
  }
  private void setParameterAnnotationCount(ClassFileReader reader, Object tag, int paramIndex, int annotationCount){
    int p = pos;
    reader.setParameterAnnotationCount(this, tag, paramIndex, annotationCount);
    pos = p;
  }
  private void setParameterAnnotation(ClassFileReader reader, Object tag, int annotationIndex, String annotationType){
    int p = pos;
    reader.setParameterAnnotation( this, tag, annotationIndex, annotationType);
    pos = p;
  }
  private void setParameterAnnotationsDone(ClassFileReader reader, Object tag, int paramIndex){
    int p = pos;
    reader.setParameterAnnotationsDone(this, tag, paramIndex);
    pos = p;
  }
  private void setParametersDone(ClassFileReader reader, Object tag){
    int p = pos;
    reader.setParametersDone(this, tag);
    pos = p;
  }

  public void setSignature(ClassFileReader reader, Object tag, String signature){
    int p = pos;
    reader.setSignature(this, tag, signature);
    pos = p;
  }



  
  public void parse( ClassFileReader reader)  throws ClassParseException {
    int cpIdx;

    try {

      int magic = readI4();
      if (magic != 0xCAFEBABE) {
        error("wrong magic: " + Integer.toHexString(magic));
      }


      int minor = readU2();
      int major = readU2();


      int cpCount = readU2();
      cpPos = new int[cpCount];
      cpValue = new Object[cpCount];
      parseCp(cpCount);


      int accessFlags = readU2();

      cpIdx = readU2();
      String clsName = (String) cpValue[cpIdx];

      cpIdx = readU2();
      String superClsName = (String) cpValue[cpIdx];


      setClass(reader, clsName, superClsName, accessFlags, cpCount);


      int ifcCount = readU2();
      parseInterfaces(reader, ifcCount);


      int fieldCount = readU2();
      parseFields(reader, fieldCount);


      int methodCount = readU2();
      parseMethods(reader, methodCount);


      int classAttrCount = readU2();
      parseClassAttributes(reader, classAttrCount);

    } catch (BailOut x){

    }
  }




  public static String readModifiedUTF8String( byte[] data, int pos, int len) throws ClassParseException {
    
    int n = 0; 
    char[] buf = new char[len]; 
    



    
    int max = pos+len;
    for (int i=pos; i<max; i++){
      int c = data[i] & 0xff;
      if ((c & 0x80) == 0){ 
        buf[n++] = (char)c;
        
      } else {
        if ((c & 0x40) != 0){      
          


          if ((c & 0x20) == 0) {   
            buf[n++] = (char) (((c & 0x1f) << 6) | (data[++i] & 0x3f));
            
          } else {                 
            buf[n++] = (char) (((c & 0x0f) << 12) | ((data[++i] & 0x3f) << 6) | (data[++i] & 0x3f));
          }
          
        } else {
          throw new ClassParseException("malformed modified UTF-8 input: ");
        }
      }
    }
    
    return new String(buf, 0, n);
  }

  





  protected void parseCp(int cpCount)  throws ClassParseException {
    int j = pos;

    byte[] data = this.data;
    int[] dataIdx = this.cpPos;
    Object[] values = this.cpValue;



    for (int i=1; i<cpCount; i++) {
      switch (data[j]){
        case 0:
          error("illegal constpool tag");

        case CONSTANT_UTF8:  
          dataIdx[i] = j++;
          int len = ((data[j++]&0xff) <<8) | (data[j++]&0xff);

          String s = readModifiedUTF8String( data, j, len);
          values[i] = s;

          j += len;
          break;

        case 2:
          error("illegal constpool tag");

        case CONSTANT_INTEGER:  
          dataIdx[i] = j++;

          int iVal = (data[j++]&0xff)<<24 | (data[j++]&0xff)<<16 | (data[j++]&0xff)<<8 | (data[j++]&0xff);
          values[i] = new Integer(iVal);
          break;

        case CONSTANT_FLOAT:  
          dataIdx[i] = j++;

          int iBits = (data[j++]&0xff)<<24 | (data[j++]&0xff)<<16 | (data[j++]&0xff)<<8 | (data[j++]&0xff);
          float fVal = Float.intBitsToFloat(iBits);
          values[i] = new Float(fVal);
          break;

        case CONSTANT_LONG:  
          dataIdx[i] = j++;
          long lVal =  (data[j++]&0xffL)<<56 | (data[j++]&0xffL)<<48 | (data[j++]&0xffL)<<40 | (data[j++]&0xffL)<<32
                    | (data[j++]&0xffL)<<24 | (data[j++]&0xffL)<<16 | (data[j++]&0xffL)<<8 | (data[j++]&0xffL);
          values[i] = new Long(lVal);

          dataIdx[++i] = -1;  
          break;

        case CONSTANT_DOUBLE:  
          dataIdx[i] = j++;

          long lBits = (data[j++]&0xffL)<<56 | (data[j++]&0xffL)<<48 | (data[j++]&0xffL)<<40 | (data[j++]&0xffL)<<32
                    | (data[j++]&0xffL)<<24 | (data[j++]&0xffL)<<16 | (data[j++]&0xffL)<<8 | (data[j++]&0xffL);
          double dVal = Double.longBitsToDouble(lBits);
          values[i] = new Double(dVal);

          dataIdx[++i] = -1;  
          break;

        case CONSTANT_CLASS:  
          dataIdx[i] = j;
          values[i] = CpInfo.ConstantClass;

          j += 3;
          break;

        case CONSTANT_STRING:  
          dataIdx[i] = j;
          values[i] = CpInfo.ConstantString;

          j += 3;
          break;

        case FIELD_REF:  
          dataIdx[i] = j;
          values[i] = CpInfo.FieldRef;
          j += 5;
          break;

        case METHOD_REF: 
          dataIdx[i] = j;
          values[i] = CpInfo.MethodRef;
          j += 5;
          break;

        case INTERFACE_METHOD_REF: 
          dataIdx[i] = j;
          values[i] = CpInfo.InterfaceMethodRef;
          j += 5;
          break;

        case NAME_AND_TYPE: 
          dataIdx[i] = j;
          values[i] = CpInfo.NameAndType;

          j += 5;
          break;


          
        case METHOD_HANDLE: 
          dataIdx[i] = j;
          values[i] = CpInfo.MethodHandle;
          j += 4;
          break;
          
        case METHOD_TYPE:  
          dataIdx[i] = j;
          values[i] = CpInfo.MethodType;
          j += 3;
          break;

        case INVOKE_DYNAMIC: 
          dataIdx[i] = j;
          values[i] = CpInfo.InvokeDynamic;
          j += 5;
          break;
          
        default:
          error("illegal constpool tag: " + data[j]);
      }
    }

    pos = j;


    for (int i=1; i<cpCount; i++){
      Object v = cpValue[i];


      if (v == CpInfo.ConstantClass || v == CpInfo.ConstantString){
         cpValue[i] = cpValue[u2(cpPos[i]+1)];
      }
    }
  }

  protected void parseInterfaces(ClassFileReader reader, int ifcCount){

    setInterfaceCount(reader, ifcCount);

    for (int i=0; i<ifcCount; i++){
      int cpIdx = readU2();
      setInterface(reader, i, classNameAt(cpIdx));
    }

    setInterfacesDone(reader);
  }


  protected void parseFields(ClassFileReader reader, int fieldCount) {

    setFieldCount(reader, fieldCount);

    for (int i=0; i<fieldCount; i++){
      int accessFlags = readU2();

      int cpIdx = readU2();
      String name = utf8At(cpIdx);

      cpIdx = readU2();
      String descriptor = utf8At(cpIdx);

      setField(reader, i, accessFlags, name, descriptor);

      int attrCount = readU2();
      parseFieldAttributes(reader, i, attrCount);

      setFieldDone(reader, i);
    }

    setFieldsDone(reader);
  }

  protected void parseFieldAttributes(ClassFileReader reader, int fieldIdx, int attrCount){
    setFieldAttributeCount(reader, fieldIdx, attrCount);

    for (int i=0; i<attrCount; i++){
      int cpIdx = readU2();
      String name = utf8At(cpIdx);

      name = internStdAttrName(cpIdx, name, stdFieldAttrs);

      int attrLength = readI4(); 
      setFieldAttribute(reader, fieldIdx, i, name, attrLength);
    }

    setFieldAttributesDone(reader, fieldIdx);
  }

  
  public void parseConstValueAttr(ClassFileReader reader, Object tag){
    int cpIdx = readU2();
    setConstantValue(reader, tag, cpValue[cpIdx]);
  }



  protected void parseMethods(ClassFileReader reader, int methodCount) {

    setMethodCount(reader, methodCount);

    for (int i=0; i<methodCount; i++){
      int accessFlags = readU2();

      int cpIdx = readU2();
      String name = utf8At(cpIdx);

      cpIdx = readU2();
      String descriptor = utf8At(cpIdx);

      setMethod(reader, i, accessFlags, name, descriptor);

      int attrCount = readU2();
      parseMethodAttributes(reader, i, attrCount);

      setMethodDone(reader, i);
    }

    setMethodsDone(reader);
  }

  protected void parseMethodAttributes(ClassFileReader reader, int methodIdx, int attrCount){
    setMethodAttributeCount(reader, methodIdx, attrCount);

    for (int i=0; i<attrCount; i++){
      int cpIdx = readU2();
      String name = utf8At(cpIdx);

      name = internStdAttrName(cpIdx, name, stdMethodAttrs);

      int attrLength = readI4(); 
      setMethodAttribute(reader, methodIdx, i, name, attrLength);
    }

    setMethodAttributesDone(reader, methodIdx);
  }

  public void parseExceptionAttr (ClassFileReader reader, Object tag){
    int exceptionCount = readU2();
    setExceptionCount(reader, tag, exceptionCount);

    for (int i=0; i<exceptionCount; i++){
      int cpIdx = readU2();
      String exceptionType = classNameAt(cpIdx);
      setException(reader, tag, i, exceptionType);
    }

    setExceptionsDone(reader, tag);
  }

  
  public void parseCodeAttr (ClassFileReader reader, Object tag){
    int maxStack = readU2();
    int maxLocals = readU2();
    int codeLength = readI4();  
    int codeStartPos = pos;

    setCode(reader, tag, maxStack, maxLocals, codeLength);

    int exceptionCount = readU2();
    setExceptionTableCount(reader, tag, exceptionCount);

    for (int i = 0; i < exceptionCount; i++) {
      int startPc = readU2();
      int endPc = readU2();
      int handlerPc = readU2();

      int cpIdx = readU2();
      String catchType = (String) cpValue[cpIdx]; 

      setExceptionTableEntry(reader, tag, i, startPc, endPc, handlerPc, catchType);
    }
    setExceptionTableDone(reader, tag);

    int attrCount = readU2();
    parseCodeAttrAttributes(reader, tag, attrCount);
  }


  protected void parseCodeAttrAttributes(ClassFileReader reader, Object tag, int attrCount){

    setCodeAttributeCount(reader, tag, attrCount);

    for (int i=0; i<attrCount; i++){
      int cpIdx = readU2();
      String name = utf8At(cpIdx);

      name = internStdAttrName(cpIdx, name, stdCodeAttrs);

      int attrLength = readI4(); 
      setCodeAttribute(reader, tag, i, name, attrLength);
    }

    setCodeAttributesDone(reader, tag);
  }

  
  public void parseLineNumberTableAttr(ClassFileReader reader, Object tag){
    int lineCount = readU2();
    setLineNumberTableCount(reader, tag, lineCount);
    
    for (int i=0; i<lineCount; i++){
      int startPc = readU2();
      int lineNumber = readU2();
      setLineNumber(reader, tag, i, lineNumber, startPc);
    }

    setLineNumberTableDone(reader, tag);
  }

  
  
  public void parseLocalVarTableAttr(ClassFileReader reader, Object tag){
    int localVarCount = readU2();
    setLocalVarTableCount(reader, tag, localVarCount);
    
    for (int i=0; i<localVarCount; i++){
      int startPc = readU2();
      int length = readU2();
      int cpIdx = readU2();
      String varName = (String) cpValue[cpIdx];
      cpIdx = readU2();
      String descriptor = (String)  cpValue[cpIdx];
      int slotIndex = readU2();
      
      setLocalVar(reader, tag, i, varName, descriptor, startPc, startPc+length-1, slotIndex );
    }

    setLocalVarTableDone(reader, tag);
  }


  protected void parseClassAttributes(ClassFileReader reader, int attrCount){

    setClassAttributeCount(reader, attrCount);

    for (int i=0; i<attrCount; i++){
      int cpIdx = readU2();
      String name = utf8At(cpIdx);

      name = internStdAttrName(cpIdx, name, stdClassAttrs);

      int attrLength = readI4(); 
      setClassAttribute(reader, i, name, attrLength);
    }

    setClassAttributesDone(reader);
  }


  
  public void parseSourceFileAttr(ClassFileReader reader, Object tag){
    int cpIdx = readU2();
    String pathName = utf8At(cpIdx);
    setSourceFile(reader, tag, pathName);
  }

  
  public void parseInnerClassesAttr(ClassFileReader reader, Object tag){
    int innerClsCount = readU2();    
    setInnerClassCount(reader, tag, innerClsCount);

    for (int i = 0; i < innerClsCount; i++) {
      int cpIdx = readU2();
      String innerClsName = (cpIdx != 0) ? (String) cpValue[cpIdx] : null;
      cpIdx = readU2();
      String outerClsName = (cpIdx != 0) ? (String) cpValue[cpIdx] : null;
      cpIdx = readU2();
      String innerSimpleName = (cpIdx != 0) ? (String) cpValue[cpIdx] : null;
      int accessFlags = readU2();

      setInnerClass(reader, tag, i, outerClsName, innerClsName, innerSimpleName, accessFlags);
    }

    setInnerClassesDone(reader, tag);
  }
  
  
  public void parseEnclosingMethodAttr(ClassFileReader reader, Object tag){
    String enclosedMethod = null;
    String descriptor = null;
    
    int cpIdx = readU2(); 
    String enclosingClass =  nameAt(cpIdx);
    
    cpIdx = readU2(); 
    


    if (cpIdx != 0){
      enclosedMethod = nameAt(cpIdx);    
      descriptor = descriptorAt(cpIdx);
    }
    
    setEnclosingMethod(reader, tag, enclosingClass, enclosedMethod, descriptor);
  }
  
  
  public void parseBootstrapMethodAttr (ClassFileReader reader, Object tag){
    int nBootstrapMethods = readU2();
    
    setBootstrapMethodCount(reader, tag, nBootstrapMethods);
    
    for (int i=0; i<nBootstrapMethods; i++){
      int cpMhIdx = readU2();
      int nArgs = readU2();
      int[] bmArgs = new int[nArgs];
      for (int j=0; j<nArgs; j++){
        bmArgs[j] = readU2();
      }
      

      int refKind = mhRefTypeAt(cpMhIdx);
      

      int mrefIdx = mhMethodRefIndexAt(cpMhIdx);
      
      String clsName = methodClassNameAt(mrefIdx);
      String mthName = methodNameAt(mrefIdx);
      String descriptor = methodDescriptorAt(mrefIdx);
      
      setBootstrapMethod(reader, tag, i, refKind, clsName, mthName, descriptor, bmArgs);
    }
    
    setBootstrapMethodsDone( reader, tag);
  }
  
  String nameAt(int nameTypeInfoIdx) {
    return utf8At(u2(cpPos[nameTypeInfoIdx] + 1));
  }
  
  String descriptorAt (int nameTypeInfoIdx){
    return utf8At( u2( cpPos[nameTypeInfoIdx]+3));
  }



  
  void parseAnnotationValue(ClassFileReader reader, Object tag, int annotationIndex, int valueIndex, String elementName, int arrayIndex){
    int cpIdx;
    Object val;

    int t = readUByte();
    switch (t){
      case 'Z':


        cpIdx = readU2();
        val = cpValue[cpIdx];
        val = Boolean.valueOf((Integer)val == 1);
        setPrimitiveAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, val);
        break;        

      case 'B':
        cpIdx = readU2();
        val = cpValue[cpIdx];
        val = Byte.valueOf(((Integer)val).byteValue());
        setPrimitiveAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, val);
        break;
        
      case 'C':
        cpIdx = readU2();
        val = cpValue[cpIdx];
        val = Character.valueOf((char)((Integer)val).shortValue());
        setPrimitiveAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, val);
        break;
        
      case 'S':
        cpIdx = readU2();
        val = cpValue[cpIdx];
        val = Short.valueOf(((Integer)val).shortValue());
        setPrimitiveAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, val);
        break;

      case 'I':
      case 'F':
      case 'D':
      case 'J':
        cpIdx = readU2();
        val = cpValue[cpIdx];
        setPrimitiveAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, val);
        break;

      case 's':
        cpIdx = readU2();
        String s = (String) cpValue[cpIdx];
        setStringAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, s);
        break;

      case 'e':
        cpIdx = readU2();
        String enumTypeName = (String)cpValue[cpIdx];
        cpIdx = readU2();
        String enumConstName = (String)cpValue[cpIdx];
        setEnumAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, enumTypeName, enumConstName);
        break;

      case 'c':
        cpIdx = readU2();
        String className = (String)cpValue[cpIdx];
        setClassAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, arrayIndex, className);
        break;

      case '@':
        parseAnnotation(reader, tag, 0, false);  
        break;

      case '[':
        int arrayLen = readU2();
        setAnnotationValueElementCount(reader, tag, annotationIndex, valueIndex, elementName, arrayLen);
        for (int i=0; i<arrayLen; i++){
          parseAnnotationValue(reader, tag, annotationIndex, valueIndex, elementName, i);
        }
        setAnnotationValueElementsDone(reader, tag, annotationIndex, valueIndex, elementName);
        break;
    }
  }

  
  void parseAnnotation (ClassFileReader reader, Object tag, int annotationIndex, boolean isParameterAnnotation){
    int cpIdx = readU2();
    String annotationType = (String)cpValue[cpIdx];

    if (isParameterAnnotation){
      setParameterAnnotation(reader, tag, annotationIndex, annotationType);
    } else {
      setAnnotation(reader, tag, annotationIndex, annotationType);
    }

    parseAnnotationValues(reader, tag, annotationIndex);
  }

  void parseAnnotationValues (ClassFileReader reader, Object tag, int annotationIndex){
    int nValuePairs = readU2();
    setAnnotationValueCount(reader, tag, annotationIndex, nValuePairs);

    for (int i=0; i<nValuePairs; i++){
      int cpIdx = readU2();
      String elementName = (String)cpValue[cpIdx];
      parseAnnotationValue(reader, tag, annotationIndex, i, elementName, -1);
    }

    setAnnotationValuesDone(reader, tag, annotationIndex);
  }
  
  
  public void parseAnnotationsAttr (ClassFileReader reader, Object tag){
    int numAnnotations = readU2();
    setAnnotationCount(reader, tag, numAnnotations);

    for (int i=0; i<numAnnotations; i++){
      parseAnnotation(reader, tag, i, false);
    }

    setAnnotationsDone(reader, tag);
  }

  

  public static final int CLASS_TYPE_PARAMETER                 = 0x00;
  public static final int METHOD_TYPE_PARAMETER                = 0x01;
  public static final int CLASS_EXTENDS                        = 0x10;
  public static final int CLASS_TYPE_PARAMETER_BOUND           = 0x11;
  public static final int METHOD_TYPE_PARAMETER_BOUND          = 0x12;
  public static final int FIELD                                = 0x13;
  public static final int METHOD_RETURN                        = 0x14;
  public static final int METHOD_RECEIVER                      = 0x15;
  public static final int METHOD_FORMAL_PARAMETER              = 0x16;
  public static final int THROWS                               = 0x17;
  public static final int LOCAL_VARIABLE                       = 0x40;
  public static final int RESOURCE_VARIABLE                    = 0x41;
  public static final int EXCEPTION_PARAMETER                  = 0x42;
  public static final int INSTANCEOF                           = 0x43;
  public static final int NEW                                  = 0x44;
  public static final int CONSTRUCTOR_REFERENCE                = 0x45;
  public static final int METHOD_REFERENCE                     = 0x46;
  public static final int CAST                                 = 0x47;
  public static final int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;
  public static final int METHOD_INVOCATION_TYPE_ARGUMENT      = 0x49;
  public static final int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT  = 0x4a;
  public static final int METHOD_REFERENCE_TYPE_ARGUMENT       = 0x4b;  
  
  public static String getTargetTypeName (int targetType){
    switch (targetType){
      case CLASS_TYPE_PARAMETER: return "class type parameter";
      case METHOD_TYPE_PARAMETER: return "method type parameter";
      case CLASS_EXTENDS: return "super class";
      case CLASS_TYPE_PARAMETER_BOUND: return "class type parameter bound";
      case METHOD_TYPE_PARAMETER_BOUND: return "method type parameter bound";
      case FIELD: return "field";
      case METHOD_RETURN: return "method return";
      case METHOD_RECEIVER: return "method receiver";
      case METHOD_FORMAL_PARAMETER: return "method formal parameter";
      case THROWS: return "throws";
      case LOCAL_VARIABLE: return "local variable";
      case RESOURCE_VARIABLE: return "resource variable";
      case EXCEPTION_PARAMETER: return "exception parameter";
      case INSTANCEOF: return "instanceof";
      case NEW: return "new";
      case CONSTRUCTOR_REFERENCE: return "ctor reference";
      case METHOD_REFERENCE: return "method reference";
      case CAST: return "case";
      case METHOD_INVOCATION_TYPE_ARGUMENT: return "method invocation type argument";
      case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT: return "ctor reference type argument";
      case METHOD_REFERENCE_TYPE_ARGUMENT: return "method reference type argument";
      default:
        return "<unknown target type 0x" + Integer.toHexString(targetType);
    }
  }
  
  public static String getTypePathEncoding (short[] typePath){
    if (typePath == null){
      return "()";
    }
    
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<typePath.length;i++){
      int e = typePath[i];
      sb.append('(');
      sb.append( Integer.toString((e>>8) & 0xff));
      sb.append( Integer.toString(e & 0xff));
      sb.append(')');
    }
    
    return sb.toString();
  }
  
  public static String getScopeEncoding (long[] scopeEntries){
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<scopeEntries.length;i++){
      long e = scopeEntries[i];
      int slotIndex = (int)(e & 0xffff);
      int length = (int)((e >> 16) & 0xffff);
      int startPc = (int)((e >> 32) & 0xffff);
      
      if (i>0){
        sb.append(',');
      }
      
      sb.append('[');
      sb.append( Integer.toString(startPc));
      sb.append("..");
      sb.append( Integer.toString(startPc + length-1));
      sb.append("]#");
      sb.append(slotIndex);
    }
    
    return sb.toString();
  }
  











































  
  int getTargetInfoSize (int targetType){
    int len = 3; 
    if (targetType == LOCAL_VARIABLE || targetType == RESOURCE_VARIABLE){
      len = Math.max( len, u2(pos) * 6); 
    }
    
    return len;
  }

  int getTypePathSize (short[] typePath){
    int typePathSize = 1;
    if (typePath != null) {
      typePathSize += typePath.length * 2;
    }
    return typePathSize;
  }
  
  
  short[] readTypePath (){
    short[] typePath = null;
    
    int pathLength = readUByte();
    if (pathLength > 0){
      typePath = new short[pathLength];
      for (int i=0; i<pathLength; i++){
        int pathKind = (short)readUByte();
        int argIdx = (short)readUByte();
        typePath[i]= (short)((pathKind << 8) | argIdx);
      }
    }
    
    return typePath;
  }

  String readAnnotationType (){
    int cpIdx = readU2();
    String annotationType = (String)cpValue[cpIdx];
    return annotationType;
  }

  void setTypeAnnotation (ClassFileReader reader, Object tag, int annotationIndex) {
    int targetType = readUByte();
    
    switch (targetType){
      case CLASS_TYPE_PARAMETER:
      case METHOD_TYPE_PARAMETER: {

        int typeParamIdx = readUByte();
        reader.setTypeParameterAnnotation( this, tag, annotationIndex, targetType, typeParamIdx, readTypePath(), readAnnotationType());
        break;
      } 
      case CLASS_EXTENDS: {

        int superTypeIdx = readU2();
        reader.setSuperTypeAnnotation( this, tag, annotationIndex, targetType, superTypeIdx, readTypePath(), readAnnotationType());
        break;
      }
      case CLASS_TYPE_PARAMETER_BOUND:
      case METHOD_TYPE_PARAMETER_BOUND: {

        int typeParamIdx = readUByte();
        int boundIdx = readUByte();
        reader.setTypeParameterBoundAnnotation(this, tag, annotationIndex, targetType, typeParamIdx, boundIdx, readTypePath(), readAnnotationType());
        break;
      }
      case METHOD_RETURN:
      case METHOD_RECEIVER:
      case FIELD:

        reader.setTypeAnnotation( this, tag, annotationIndex, targetType, readTypePath(), readAnnotationType());
        break;
        
      case METHOD_FORMAL_PARAMETER: {

        int formalParamIdx = readUByte();
        reader.setFormalParameterAnnotation( this, tag, annotationIndex, targetType, formalParamIdx, readTypePath(), readAnnotationType());
        break;
      }
      case THROWS: {

        int throwsTypeIdx = readU2();
        reader.setThrowsAnnotation( this, tag, annotationIndex, targetType, throwsTypeIdx, readTypePath(), readAnnotationType());        
        break;
      } 
      case LOCAL_VARIABLE:
      case RESOURCE_VARIABLE: {










        int tableLength = readU2();
        long[] scopeEntries = new long[tableLength];
        for (int i=0; i<tableLength; i++){
          int startPc = readU2();
          int length = readU2();
          int slotIdx = readU2();
          scopeEntries[i] = ((long)startPc << 32) | ((long)length << 16) | slotIdx;
        }
        reader.setVariableAnnotation( this, tag, annotationIndex, targetType, scopeEntries, readTypePath(), readAnnotationType());
        break;
      }
      case EXCEPTION_PARAMETER: {

        int exceptionIdx = readU2();
        reader.setExceptionParameterAnnotation( this, tag, annotationIndex, targetType, exceptionIdx, readTypePath(), readAnnotationType());        
        break;
      }
      case INSTANCEOF:
      case METHOD_REFERENCE:
      case CONSTRUCTOR_REFERENCE:
      case NEW: {

        int offset = readU2();
        reader.setBytecodeAnnotation(this, tag, annotationIndex, targetType, offset, readTypePath(), readAnnotationType());
        break;
      }
      case CAST:
      case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
      case METHOD_INVOCATION_TYPE_ARGUMENT:
      case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
      case METHOD_REFERENCE_TYPE_ARGUMENT: {




        int offset = readU2();
        int typeArgIdx = readUByte();
        reader.setBytecodeTypeParameterAnnotation(this, tag, annotationIndex, targetType, offset, typeArgIdx, readTypePath(), readAnnotationType());
        break;
      }
      
      default:

        throw new RuntimeException("unknown type annotation target: 0x" + Integer.toHexString(targetType));
    }
  }

  
  void parseTypeAnnotation (ClassFileReader reader, Object tag, int annotationIndex) {
   


    setTypeAnnotation(reader, tag, annotationIndex);
    

    parseAnnotationValues( reader, tag, annotationIndex);
  }
  
  
  public void parseTypeAnnotationsAttr (ClassFileReader reader, Object tag) {
    int numAnnotations = readU2();
    setTypeAnnotationCount(reader, tag, numAnnotations);

    for (int i=0; i<numAnnotations; i++){
      parseTypeAnnotation(reader, tag, i);
    }

    setTypeAnnotationsDone(reader, tag);
  }
  
  
   public void parseParameterAnnotationsAttr(ClassFileReader reader, Object tag){
     int numParameters = readUByte();
     setParameterCount(reader, tag, numParameters);
     for (int i=0; i<numParameters; i++){
       int numAnnotations = readU2();

       setParameterAnnotationCount(reader, tag, i, numAnnotations);
       for (int j=0; j<numAnnotations; j++){
         parseAnnotation(reader, tag, j, true);
       }
       setParameterAnnotationsDone(reader, tag, i);
     }
     setParametersDone(reader, tag);
   }

  
   public void parseSignatureAttr(ClassFileReader reader, Object tag){
     int cpIdx = readU2();
     setSignature(reader, tag, utf8At(cpIdx));
   }


  
   public void parseAnnotationDefaultAttr(ClassFileReader reader, Object tag){
     parseAnnotationValue(reader, tag, -1, -1, null, -1);
   }

























  public void parseBytecode(JVMByteCodeReader reader, Object tag, int codeLength){
    int localVarIndex;
    int cpIdx;
    int constVal;
    int offset;
    int defaultOffset;

    boolean isWide = false; 

    int startPos = pos;
    int endPos = pos+codeLength;
    int nextPos;


    while (pos < endPos){
      pc = pos - startPos;

      int opcode = readUByte();
      switch (opcode){
        case 0: 
          reader.nop();
          break;
        case 1:  
          reader.aconst_null();
          break;
        case 2: 
          reader.iconst_m1();
          break;
        case 3: 
          reader.iconst_0();
          break;
        case 4: 
          reader.iconst_1();
          break;
        case 5: 
          reader.iconst_2();
          break;
        case 6: 
          reader.iconst_3();
          break;
        case 7: 
          reader.iconst_4();
          break;
        case 8: 
          reader.iconst_5();
          break;
        case 9: 
          reader.lconst_0();
          break;
        case 10: 
          reader.lconst_1();
          break;
        case 11: 
          reader.fconst_0();
          break;
        case 12: 
          reader.fconst_1();
          break;
        case 13: 
          reader.fconst_2();
          break;
        case 14: 
          reader.dconst_0();
          break;
        case 15: 
          reader.dconst_1();
          break;
        case 16: 
          constVal = readByte();
          reader.bipush(constVal);
          break;
        case 17: 
          constVal = readI2();
          reader.sipush(constVal);
          break;
        case 18: 
          cpIdx = readUByte();
          reader.ldc_(cpIdx);
          break;
        case 19: 
          cpIdx = readU2();
          reader.ldc_w_(cpIdx);
          break;
        case 20: 
          cpIdx = readU2();
          reader.ldc2_w(cpIdx);
          break;
        case 21: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.iload(localVarIndex);
          break;
        case 22: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.lload(localVarIndex);
          break;
        case 23: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.fload(localVarIndex);
          break;
        case 24: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.dload(localVarIndex);
          break;
        case 25: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.aload(localVarIndex);
          break;
        case 26: 
          reader.iload_0();
          break;
        case 27: 
          reader.iload_1();
          break;
        case 28: 
          reader.iload_2();
          break;
        case 29: 
          reader.iload_3();
          break;
        case 30: 
          reader.lload_0();
          break;
        case 31: 
          reader.lload_1();
          break;
        case 32: 
          reader.lload_2();
          break;
        case 33: 
          reader.lload_3();
          break;
        case 34: 
          reader.fload_0();
          break;
        case 35: 
          reader.fload_1();
          break;
        case 36: 
          reader.fload_2();
          break;
        case 37: 
          reader.fload_3();
          break;
        case 38: 
          reader.dload_0();
          break;
        case 39: 
          reader.dload_1();
          break;
        case 40: 
          reader.dload_2();
          break;
        case 41: 
          reader.dload_3();
          break;
        case 42: 
          reader.aload_0();
          break;
        case 43: 
          reader.aload_1();
          break;
        case 44: 
          reader.aload_2();
          break;
        case 45: 
          reader.aload_3();
          break;
        case 46: 
          reader.iaload();
          break;
        case 47: 
          reader.laload();
          break;
        case 48: 
          reader.faload();
          break;
        case 49: 
          reader.daload();
          break;
        case 50: 
          reader.aaload();
          break;
        case 51: 
          reader.baload();
          break;
        case 52: 
          reader.caload();
          break;
        case 53: 
          reader.saload();
          break;
        case 54: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.istore(localVarIndex);
          break;
        case 55: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.lstore(localVarIndex);
          break;
        case 56: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.fstore(localVarIndex);
          break;
        case 57: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.dstore(localVarIndex);
          break;
        case 58: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.astore(localVarIndex);
          break;
        case 59: 
          reader.istore_0();
          break;
        case 60: 
          reader.istore_1();
          break;
        case 61: 
          reader.istore_2();
          break;
        case 62: 
          reader.istore_3();
          break;
        case 63: 
          reader.lstore_0();
          break;
        case 64: 
          reader.lstore_1();
          break;
        case 65: 
          reader.lstore_2();
          break;
        case 66: 
          reader.lstore_3();
          break;
        case 67: 
          reader.fstore_0();
          break;
        case 68: 
          reader.fstore_1();
          break;
        case 69: 
          reader.fstore_2();
          break;
        case 70: 
          reader.fstore_3();
          break;
        case 71: 
          reader.dstore_0();
          break;
        case 72: 
          reader.dstore_1();
          break;
        case 73: 
          reader.dstore_2();
          break;
        case 74: 
          reader.dstore_3();
          break;
        case 75: 
          reader.astore_0();
          break;
        case 76: 
          reader.astore_1();
          break;
        case 77: 
          reader.astore_2();
          break;
        case 78: 
          reader.astore_3();
          break;
        case 79: 
          reader.iastore();
          break;
        case 80: 
          reader.lastore();
          break;
        case 81: 
          reader.fastore();
          break;
        case 82: 
          reader.dastore();
          break;
        case 83: 
          reader.aastore();
          break;
        case 84: 
          reader.bastore();
          break;
        case 85: 
          reader.castore();
          break;
        case 86: 
          reader.sastore();
          break;
        case 87: 
          reader.pop();
          break;
        case 88: 
          reader.pop2();
          break;
        case 89: 
          reader.dup();
          break;
        case 90: 
          reader.dup_x1();
          break;
        case 91: 
          reader.dup_x2();
          break;
        case 92: 
          reader.dup2();
          break;
        case 93: 
          reader.dup2_x1();
          break;
        case 94: 
          reader.dup2_x2();
          break;
        case 95: 
          reader.swap();
          break;
        case 96: 
          reader.iadd();
          break;
        case 97: 
          reader.ladd();
          break;
        case 98: 
          reader.fadd();
          break;
        case 99: 
          reader.dadd();
          break;
        case 100: 
          reader.isub();
          break;
        case 101: 
          reader.lsub();
          break;
        case 102: 
          reader.fsub();
          break;
        case 103: 
          reader.dsub();
          break;
        case 104: 
          reader.imul();
          break;
        case 105: 
          reader.lmul();
          break;
        case 106: 
          reader.fmul();
          break;
        case 107: 
          reader.dmul();
          break;
        case 108: 
          reader.idiv();
          break;
        case 109: 
          reader.ldiv();
          break;
        case 110: 
          reader.fdiv();
          break;
        case 111: 
          reader.ddiv();
          break;
        case 112: 
          reader.irem();
          break;
        case 113: 
          reader.lrem();
          break;
        case 114: 
          reader.frem();
          break;
        case 115: 
          reader.drem();
          break;
        case 116: 
          reader.ineg();
          break;
        case 117: 
          reader.lneg();
          break;
        case 118: 
          reader.fneg();
          break;
        case 119: 
          reader.dneg();
          break;
        case 120: 
          reader.ishl();
          break;
        case 121: 
          reader.lshl();
          break;
        case 122: 
          reader.ishr();
          break;
        case 123: 
          reader.lshr();
          break;
        case 124: 
          reader.iushr();
          break;
        case 125: 
          reader.lushr();
          break;
        case 126: 
          reader.iand();
          break;
        case 127: 
          reader.land();
          break;
        case 128: 
          reader.ior();
          break;
        case 129: 
          reader.lor();
          break;
        case 130: 
          reader.ixor();
          break;
        case 131: 
          reader.lxor();
          break;
        case 132: 
          if (isWide){
            localVarIndex = readU2();
            constVal = readI2();
          } else {
            localVarIndex = readUByte();
            constVal = readByte();
          }
          reader.iinc(localVarIndex, constVal);
          break;
        case 133: 
          reader.i2l();
          break;
        case 134: 
          reader.i2f();
          break;
        case 135: 
          reader.i2d();
          break;
        case 136: 
          reader.l2i();
          break;
        case 137: 
          reader.l2f();
          break;
        case 138: 
          reader.l2d();
          break;
        case 139: 
          reader.f2i();
          break;
        case 140: 
          reader.f2l();
          break;
        case 141: 
          reader.f2d();
          break;
        case 142: 
          reader.d2i();
          break;
        case 143: 
          reader.d2l();
          break;
        case 144: 
          reader.d2f();
          break;
        case 145: 
          reader.i2b();
          break;
        case 146: 
          reader.i2c();
          break;
        case 147: 
          reader.i2s();
          break;
        case 148: 
          reader.lcmp();
          break;
        case 149: 
          reader.fcmpl();
          break;
        case 150: 
          reader.fcmpg();
          break;
        case 151: 
          reader.dcmpl();
          break;
        case 152: 
          reader.dcmpg();
          break;
        case 153: 
          offset = readI2();
          reader.ifeq(offset);
          break;
        case 154: 
          offset = readI2();
          reader.ifne(offset);
          break;
        case 155: 
          offset = readI2();
          reader.iflt(offset);
          break;
        case 156: 
          offset = readI2();
          reader.ifge(offset);
          break;
        case 157: 
          offset = readI2();
          reader.ifgt(offset);
          break;
        case 158: 
          offset = readI2();
          reader.ifle(offset);
          break;
        case 159: 
          offset = readI2();
          reader.if_icmpeq(offset);
          break;
        case 160: 
          offset = readI2();
          reader.if_icmpne(offset);
          break;
        case 161: 
          offset = readI2();
          reader.if_icmplt(offset);
          break;
        case 162: 
          offset = readI2();
          reader.if_icmpge(offset);
          break;
        case 163: 
          offset = readI2();
          reader.if_icmpgt(offset);
          break;
        case 164: 
          offset = readI2();
          reader.if_icmple(offset);
          break;
        case 165: 
          offset = readI2();
          reader.if_acmpeq(offset);
          break;
        case 166: 
          offset = readI2();
          reader.if_acmpne(offset);
          break;
        case 167: 
          offset = readI2();
          reader.goto_(offset);
          break;
        case 168: 
          offset = readI2();
          reader.jsr(offset);
          break;
        case 169: 
          localVarIndex = isWide ? readU2() : readUByte();
          reader.ret(localVarIndex);
          break;
        case 170: 
          pos = (((pc+4)>>2)<<2)+startPos; 

          defaultOffset = readI4();
          int low = readI4();
          int high = readI4();

          int len = high-low+1;
          nextPos = pos + len*4;
          reader.tableswitch(defaultOffset, low, high);
          pos = nextPos;
          break;
        case 171: 
          pos = (((pc+4)>>2)<<2)+startPos; 

          defaultOffset = readI4();
          int nPairs = readI4();

          nextPos = pos + (nPairs*8);
          reader.lookupswitch(defaultOffset, nPairs);
          pos = nextPos;
          break;
        case 172: 
          reader.ireturn();
          break;
        case 173: 
          reader.lreturn();
          break;
        case 174: 
          reader.freturn();
          break;
        case 175: 
          reader.dreturn();
          break;
        case 176: 
          reader.areturn();
          break;
        case 177: 
          reader.return_();
          break;
        case 178: 
          cpIdx = readU2(); 
          reader.getstatic(cpIdx);
          break;
        case 179: 
          cpIdx = readU2(); 
          reader.putstatic(cpIdx);
          break;
        case 180: 
          cpIdx = readU2(); 
          reader.getfield(cpIdx);
          break;
        case 181: 
          cpIdx = readU2(); 
          reader.putfield(cpIdx);
          break;
        case 182: 
          cpIdx = readU2(); 
          reader.invokevirtual(cpIdx);
          break;
        case 183: 
          cpIdx = readU2(); 
          reader.invokespecial(cpIdx);
          break;
        case 184: 
          cpIdx = readU2(); 
          reader.invokestatic(cpIdx);
          break;
        case 185: 
          cpIdx = readU2(); 
          int count = readUByte();
          int zero = readUByte(); 
          reader.invokeinterface(cpIdx, count, zero);
          break;
        case 186: 
          cpIdx = readU2(); 
          readUByte();  
          readUByte(); 
          reader.invokedynamic(cpIdx);
          break;
        case 187: 
          cpIdx = readU2();
          reader.new_(cpIdx);
          break;
        case 188: 
          int aType = readUByte();
          reader.newarray(aType);
          break;
        case 189: 
          cpIdx = readU2(); 
          reader.anewarray(cpIdx);
          break;
        case 190: 
          reader.arraylength();
          break;
        case 191: 
          reader.athrow();
          break;
        case 192: 
          cpIdx = readU2(); 
          reader.checkcast(cpIdx);
          break;
        case 193: 
          cpIdx = readU2(); 
          reader.instanceof_(cpIdx);
          break;
        case 194: 
          reader.monitorenter();
          break;
        case 195: 
          reader.monitorexit();
          break;
        case 196: 
          isWide = true;




          reader.wide();
          continue;
        case 197: 
          cpIdx = readU2();
          int dimensions = readUByte();
          reader.multianewarray(cpIdx, dimensions);
          break;
        case 198: 
          offset = readI2();
          reader.ifnull(offset);
          break;
        case 199: 
          offset = readI2();
          reader.ifnonnull(offset);
          break;
        case 200: 
          offset = readI4();
          reader.goto_w(offset);
          break;
        case 201: 
          offset = readI4();
          reader.jsr_w(offset);
          break;
          
          
        default:
          reader.unknown(opcode);
      }

      isWide = false; 
    }

  }


  public void parseTableSwitchEntries(JVMByteCodeReader reader, int low, int high){
    for (int val=low; val<=high; val++){
      int offset = readI4();
      reader.tableswitchEntry(val, offset);
    }
  }
  public int getTableSwitchOffset(int low, int high, int defaultOffset, int val){
    if (val < low || val > high){
      return defaultOffset;
    }

    int n = Math.abs(val - low);
    pos += n*4;
    int pcOffset = readI4();

    return pcOffset;
  }


  public void parseLookupSwitchEntries(JVMByteCodeReader reader, int nEntries){
    for (int i=0; i<nEntries; i++){
      int value = readI4();
      int offset = readI4();
      reader.lookupswitchEntry(i, value, offset);
    }
  }
  public int getLookupSwitchOffset(int nEntries, int defaultOffset, int val){
    for (int i=0; i<nEntries; i++){
      int match = readI4();
      if (val > match){
        pos +=4;
      } else if (val == match) {
        int offset = readI4();
        return offset;
      } else {
        break;
      }
    }
    return defaultOffset;
  }

}
