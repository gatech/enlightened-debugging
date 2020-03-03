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

import java.lang.reflect.Method;
import java.util.ArrayList;



public class Types {


  public static final byte T_NONE      = 0; 
  
  public static final byte T_BOOLEAN   = 4;
  public static final byte T_BYTE      = 8;
  public static final byte T_CHAR      = 5;
  public static final byte T_SHORT     = 9;
  public static final byte T_INT       = 10;
  public static final byte T_LONG      = 11;
  public static final byte T_FLOAT     = 6;
  public static final byte T_DOUBLE    = 7;
  public static final byte T_REFERENCE = 14;
  public static final byte T_ARRAY     = 13;  
  public static final byte T_VOID      = 12;

  
  public static byte[] getArgumentTypes (String signature) {
    int i;
    int j;
    int nArgs;

    for (i = 1, nArgs = 0; signature.charAt(i) != ')'; nArgs++) {
      i += getTypeLength(signature, i);
    }

    byte[] args = new byte[nArgs];

    for (i = 1, j = 0; j < nArgs; j++) {
      int    end = i + getTypeLength(signature, i);
      String arg = signature.substring(i, end);
      i = end;

      args[j] = getBuiltinTypeFromSignature(arg);
    }

    return args;
  }
  
  public static String[] getArgumentTypeSignatures(String methodSignature) {
  	int len = methodSignature.length();

    if ((len > 1) && (methodSignature.charAt(1) == ')')) {
      return new String[0]; 
    }
    
    ArrayList<String> a = new ArrayList<String>();

    for (int i = 1; methodSignature.charAt(i) != ')';) {
      int end = i + getTypeLength(methodSignature,i);
      String arg = methodSignature.substring(i, end);
      i = end;

      a.add(arg);
    }

    String[] typeSigs = new String[a.size()];
    a.toArray(typeSigs);
    
    return typeSigs;
  }

  public static String[] getArgumentTypeNames (String signature) {
    String[] typeSigs = getArgumentTypeSignatures(signature);
    String[] typeNames = new String[typeSigs.length];
    for (int i = 0; i < typeSigs.length; ++i) {
    	typeNames[i] = getTypeName(typeSigs[i]);
    }
    return typeNames;
  }
  
  public static String dequalify (String typeName){
    int idx = typeName.lastIndexOf('.');
    if (idx > 0) {
      return typeName.substring(idx + 1);
    } else {
      return typeName;
    }    
  }
  
  public static String getDequalifiedMethodSignature (String mName){
    int idx = mName.indexOf('(');
    String sig = mName.substring(idx);
    
    return mName.substring(0, idx) + getDequalifiedArgumentSignature(sig);
  }
  
  public static String getDequalifiedArgumentSignature (String sig){
    String[] argTypes = getArgumentTypeNames(sig);
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    for (int i=0; i<argTypes.length; i++){
      if (i>0){
        sb.append(',');
      }
      sb.append(dequalify(argTypes[i]));
    }
    sb.append(')');
    return sb.toString();
  }
  
  public static String getDequalifiedTypeName (String sig){
    String tn = getTypeName(sig);
    return dequalify(tn);
  }
  
  public static String getArgumentSignature (String[] typeNames, boolean qualified){
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    for (int i=0; i<typeNames.length; i++){
      if (i>0){
        sb.append(',');
      }
      
      String tn = getTypeName(typeNames[i]);
      if (!qualified){
        int idx = tn.lastIndexOf('.');
        if (idx >0){
          tn = tn.substring(idx+1);
        }
      }
      
      sb.append( tn);
    }
    sb.append(')');
    return sb.toString();
  }
  
  
  public static int getArgumentsSize (String sig) {
    int  n = 0;
    for (int i = 1; sig.charAt(i) != ')'; i++) {
      switch (sig.charAt(i)) {
      case 'L':
        do i++; while (sig.charAt(i) != ';');
        n++;
        break;
      case '[':
        do i++; while (sig.charAt(i) == '[');
        if (sig.charAt(i) == 'L') {
          do i++; while (sig.charAt(i) != ';');
        }
        n++;
        break;
      case 'J':
      case 'D':

        n += 2;
        break;
      default:

        n++;
      }
    }
    return n;
  }

  
  public static String getArrayElementType (String type) {
    if (type.charAt(0) != '[') {
      throw new JPFException("not an array type: " + type);
    }

    return type.substring(1);
  }

  public static String getComponentTerminal (String type) {
    if (type.charAt(0) != '[') {
      throw new JPFException("not an array type: " + type);
    }

    if(isReferenceSignature(type)) {
      return type.substring(type.indexOf('L') + 1 , type.indexOf(';'));
    } else {
      return type.substring(type.lastIndexOf('[') + 1);
    }
  }

  public static byte getBuiltinTypeFromSignature (String signature) {
    switch (signature.charAt(0)) {
    case 'B':
      return T_BYTE;

    case 'C':
      return T_CHAR;

    case 'D':
      return T_DOUBLE;

    case 'F':
      return T_FLOAT;

    case 'I':
      return T_INT;

    case 'J':
      return T_LONG;

    case 'L':
      return T_REFERENCE;

    case 'S':
      return T_SHORT;

    case 'V':
      return T_VOID;

    case 'Z':
      return T_BOOLEAN;

    case '[':
      return T_ARRAY;
    }

    throw new JPFException("invalid type string: " + signature);
  }

  
  public static String getJNISignature (String mangledName) {
    int    i = mangledName.indexOf("__");
    String sig = null;

    if (i > 0) {
      int k = 0;      
      int r = mangledName.indexOf("__", i+2); 
      boolean gotReturnType = false;
      int len = mangledName.length();
      char[] buf = new char[len + 2];

      buf[k++] = '(';

      for (i += 2; i < len; i++) {

        if (i == r) { 
          if ((i + 2) < len) {
            i++;
            buf[k++] = ')';
            gotReturnType = true;
            continue;
          } else {
            break;
          }
        }
        
        char c = mangledName.charAt(i);
        if (c == '_') {
          i++;

          if (i < len) {
            c = mangledName.charAt(i);

            switch (c) {
            case '1':
              buf[k++] = '_';

              break;

            case '2':
              buf[k++] = ';';

              break;

            case '3':
              buf[k++] = '[';

              break;

            default:
              buf[k++] = '/';
              buf[k++] = c;
            }
          } else {
            buf[k++] = '/';
          }
        } else {
          buf[k++] = c;
        }
      }

      if (!gotReturnType) {

        buf[k++] = ')';
        buf[k++] = 'V';
      }
        
      sig = new String(buf, 0, k);
    }


    return sig;
  }

  public static String getJNIMangledMethodName (Method m) {
    String      name = m.getName();
    Class<?>[]    pt = m.getParameterTypes();
    StringBuilder  s = new StringBuilder(name.length() + (pt.length * 16));

    s.append(name);
    s.append("__");


    for (int i = 0; i < pt.length; i++) {
      s.append(getJNITypeCode(pt[i].getName()));
    }



    Class<?> rt = m.getReturnType();
    s.append("__");
    s.append(getJNITypeCode(rt.getName()));
    
    return s.toString();
  }

  public static String getJNIMangledMethodName (String cls, String name,
                                                String signature) {
    StringBuilder s = new StringBuilder(signature.length() + 10);
    int           i;
    char          c;
    int           slen = signature.length();
    
    if (cls != null) {
      s.append(cls.replace('.', '_'));
    }

    s.append(name);
    s.append("__");


    for (i = 1; i<slen; i++) {
      c = signature.charAt(i);
      switch (c) {
      case '/':
        s.append('_');
        break;

      case '_':
        s.append("_1");
        break;

      case ';':
        s.append("_2");
        break;

      case '[':
        s.append("_3");
        break;

      case ')':


        s.append("__");
        break;
        
      default:
        s.append(c);
      }
    }

    return s.toString();
  }

  
  public static String getJNIMethodName (String mangledName) {

    int i = mangledName.indexOf("__");

    if (i > 0) {
      return mangledName.substring(0, i);
    } else {
      return mangledName;
    }
  }

  
  public static String getJNITypeCode (String type) {
    StringBuilder sb = new StringBuilder(32);
    int l = type.length() - 1;
    int i;


    for ( i=0; type.charAt(i) == '['; i++){
      sb.append("_3");
    }
    

    for (; type.charAt(l) == ']'; l -= 2) {
      sb.append("_3");
    }

    type = type.substring(i, l + 1);

    if (type.equals("int") || type.equals("I")) {
      sb.append('I');
    } else if (type.equals("long") || type.equals("J")) {
      sb.append('J');
    } else if (type.equals("boolean") || type.equals("Z")) {
      sb.append('Z');
    } else if (type.equals("char") || type.equals("C")) {
      sb.append('C');
    } else if (type.equals("byte")  || type.equals("B")) {
      sb.append('B');
    } else if (type.equals("short") || type.equals("S")) {
      sb.append('S');
    } else if (type.equals("double") || type.equals("D")) {
      sb.append('D');
    } else if (type.equals("float") || type.equals("F")) {
      sb.append('F');
    } else if (type.equals("void") || type.equals("V")) {  
      sb.append('V');
    } else { 
      if (type.charAt(0) != 'L'){
        sb.append('L');
      }

      l = type.length();
      for (i=0; i < l; i++) {
        char c = type.charAt(i);

        switch (c) {
        case '.':
          sb.append('_');
          break;

        case '_':
          sb.append("_1");
          break;
          
        case ';':
          break;
          
        default:
          sb.append(c);
        }
      }

      sb.append("_2");
      
    }

    return sb.toString();
  }


  public static int getNumberOfStackSlots (String signature, boolean isStatic) {
    int nArgSlots = 0;
    int n = isStatic ? 0 : 1;
    int sigLen = signature.length();

    for (int i = 1; i < sigLen; i++) {
      switch (signature.charAt(i)) {
      case ')' : 
        nArgSlots = n;
        n = 0;
        break;
      case 'L':   
        i = signature.indexOf(';', i);
        n++;
        break;
      case '[':
        do i++; while (signature.charAt(i) == '[');
        if (signature.charAt(i) == 'L') {
          i = signature.indexOf(';', i);
        }
        n++;
        break;
      case 'J':
      case 'D':
        n+=2;
        break;
      default:
        n++;
      }
    }
    
    return Math.max(n, nArgSlots);
  }
  
  public static int getNumberOfArguments (String signature) {
    int  i,n;
    int sigLen = signature.length();

    for (i = 1, n = 0; i<sigLen; n++) {
      switch (signature.charAt(i)) {
      case ')' :
        return n;
      case 'L':
        do i++; while (signature.charAt(i) != ';');
        break;

      case '[':
        do i++; while (signature.charAt(i) == '[');
        if (signature.charAt(i) == 'L') {
          do i++; while (signature.charAt(i) != ';');
        }
        break;

      default:

      }

      i++;
    }

    assert (false) : "malformed signature: " + signature;
    return n; 
  }

  public static boolean isReferenceSignature(String signature){
    return signature.charAt(signature.length()-1) == ';';
  }

  public static boolean isReference (String type) {
    int t = getBuiltinTypeFromSignature(type);

    return (t == T_ARRAY) || (t == T_REFERENCE);
  }

  public static boolean isArray (String type) {
    int t = getBuiltinTypeFromSignature(type);

    return (t == T_ARRAY);
  }

  public static byte getReturnBuiltinType (String signature) {
    int i = signature.indexOf(')');

    return getBuiltinTypeFromSignature(signature.substring(i + 1));
  }

  public static String getReturnTypeSignature(String signature){
    int i = signature.indexOf(')');
    return signature.substring(i + 1);
  }

  public static String getReturnTypeName (String signature){
    int i = signature.indexOf(')');
    return getTypeName(signature.substring(i+1));
  }
  
  public static String getTypeSignature (String type, boolean asDotNotation) {
    String  t = null;
    int arrayDim = 0;
    
    type = asDotNotation ? type.replace('/', '.') : type.replace('.', '/');
    
    if ((type.charAt(0) == '[') || (type.endsWith(";"))) {  
      t = type;
      
    } else {
      
      while (type.endsWith("[]")) { 
        type = type.substring(0, type.length() - 2);
        arrayDim++;
      }
      
      if (type.equals("byte")) {
        t = "B";
      } else if (type.equals("char")) {
        t = "C";
      } else if (type.equals("short")) {
        t = "S";
      } else if (type.equals("int")) {
        t = "I";
      } else if (type.equals("float")) {
        t = "F";
      } else if (type.equals("long")) {
        t = "J";
      } else if (type.equals("double")) {
        t = "D";
      } else if (type.equals("boolean")) {
        t = "Z";
      } else if (type.equals("void")) {
        t = "V";
      } else if (type.endsWith(";")) {
        t = type;
        
      } else {
        t = "L" + type + ';';
      }
      
      while (arrayDim-- > 0) {
        t = "[" + t;
      }
    }

    return t;
  }

  public static byte getBuiltinType(String typeName){
      if (typeName.equals("byte")) {
        return T_BYTE;
      } else if (typeName.equals("char")) {
        return T_CHAR;
      } else if (typeName.equals("short")) {
        return T_SHORT;
      } else if (typeName.equals("int")) {
        return T_INT;
      } else if (typeName.equals("float")) {
        return T_FLOAT;
      } else if (typeName.equals("long")) {
        return T_LONG;
      } else if (typeName.equals("double")) {
        return T_DOUBLE;
      } else if (typeName.equals("boolean")) {
        return T_BOOLEAN;
      } else if (typeName.equals("void")) {
        return T_VOID;
      } else {
        if (typeName.charAt(typeName.length()-1) == ']'){
          return T_ARRAY;
        } else {
          return T_REFERENCE;
        }
      }
  }

  public static String getBoxedType (byte type) {
	  switch (type) {
	  case Types.T_BOOLEAN:
		  return "Boolean";
	  case Types.T_BYTE:
		  return "Byte";
	  case Types.T_CHAR:
		  return "Character";
	  case Types.T_SHORT:
		  return "Short";
	  case Types.T_INT:
		  return "Integer";
	  case Types.T_LONG:
		  return "Long";
	  case Types.T_FLOAT:
		  return "Float";
	  case Types.T_DOUBLE:
		  return "Double";
	  default:
		  return null;
	  }
  }
  
  public static byte getUnboxedType (String typeName){
    if (typeName.startsWith("java.lang.")){
      typeName = typeName.substring(10);
      if (typeName.equals("Boolean")){
        return T_BOOLEAN;
      } else if (typeName.equals("Byte")){
        return T_BYTE;
      } else if (typeName.equals("Character")){
        return T_CHAR;
      } else if (typeName.equals("Short")){
        return T_SHORT;
      } else if (typeName.equals("Integer")){
        return T_INT;
      } else if (typeName.equals("Long")){
        return T_LONG;
      } else if (typeName.equals("Float")){
        return T_FLOAT;
      } else if (typeName.equals("Double")){
        return T_DOUBLE;
      }
    }
    

    if (typeName.charAt(0) == '[' || typeName.charAt(typeName.length()-1) == ']'){
      return T_ARRAY;
    } else {
      return T_REFERENCE;
    }
  }
  
  public static String getClassNameFromSignature (String signature){
    if (signature.charAt(signature.length()-1) == ';'){ 
      return signature.replace('/', '.');

    } else { 
      switch (signature.charAt(0)){
        case 'Z': return "boolean";
        case 'B': return "byte";
        case 'C': return "char";
        case 'S': return "short";
        case 'I': return "int";
        case 'J': return "long";
        case 'F': return "float";
        case 'D': return "double";
        default:
          throw new JPFException("illegal type signature: " + signature);
      }
    }
  }

  
  public static String getClassNameFromTypeName (String typeName) {
    typeName = typeName.replace('/','.');
    int n = typeName.length()-1;
    
    if (typeName.charAt(0) == '['){ 
      if (typeName.charAt(1) == 'L'){
        if (typeName.charAt(n) != ';'){
          typeName = typeName + ';';
        }
      }
      
      return typeName;
    }
    
    int i=typeName.indexOf('[');
    if (i>0){ 
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      for (int j=i; (j=typeName.indexOf('[',j+1)) >0;){
        sb.append('[');
      }
      
      typeName = typeName.substring(0,i);
      if (isBasicType(typeName)){
        sb.append( getTypeSignature(typeName, true));
      } else {
        sb.append('L');
        sb.append(typeName);
        sb.append(';');
      }
      
      return sb.toString();
    }
    
    if (typeName.charAt(n) == ';') {
      return typeName.substring(1,n);
    }
    
    return typeName;
  }

  
  public static boolean isTypeCode (String t) {
    char c = t.charAt(0);

    if (c == '[') {
      return true;
    }

    if ((t.length() == 1) &&
            ((c == 'B') || (c == 'I') || (c == 'S') || (c == 'C') ||
              (c == 'F') || (c == 'J') || (c == 'D') || (c == 'Z'))) {
      return true;
    }

    if (t.endsWith(";")) {
      return true;
    }

    return false;
  }

  public static boolean isBasicType (String typeName){
    return ("boolean".equals(typeName) ||
        "byte".equals(typeName) ||
        "char".equals(typeName) ||
        "int".equals(typeName) ||
        "long".equals(typeName) ||
        "double".equals(typeName) ||
        "short".equals(typeName) ||
        "float".equals(typeName));
  }

  public static byte getTypeCode (String signature){
    char c = signature.charAt(0);

    switch (c) {
      case 'B':
        return T_BYTE;

      case 'C':
        return T_CHAR;

      case 'D':
        return T_DOUBLE;

      case 'F':
        return T_FLOAT;

      case 'I':
        return T_INT;

      case 'J':
        return T_LONG;

      case 'L':
        return T_REFERENCE;

      case 'S':
        return T_SHORT;

      case 'V':
        return T_VOID;

      case 'Z':
        return T_BOOLEAN;

      case '[':
        return T_ARRAY;

      default:
        throw new JPFException("unknow typecode: " + signature);
    }
  }
  
  
  public static String getTypeName (String signature) {
    int  len = signature.length();
    char c = signature.charAt(0);

    if (len == 1) {
      switch (c) {
      case 'B':
        return "byte";

      case 'C':
        return "char";

      case 'D':
        return "double";

      case 'F':
        return "float";

      case 'I':
        return "int";

      case 'J':
        return "long";

      case 'S':
        return "short";

      case 'V':
        return "void";

      case 'Z':
        return "boolean";
      }
    }

    if (c == '[') {
      return getTypeName(signature.substring(1)) + "[]";
    }

    int len1 = len-1;
    if (signature.charAt(len1) == ';') {
      return signature.substring(1, len1).replace('/', '.');
    }

    throw new JPFException("invalid type string: " + signature);
  }

  
  public static String getElementDescriptorOfType (int arrayType){
    switch (arrayType){
      case 4: return "Z";
      case 5: return "C";
      case 6: return "F";
      case 7: return "D";
      case 8: return "B";
      case 9: return "S";
      case 10: return "I";
      case 11: return "J";
    }
    return null;
  }

  
  public static int getTypeSizeInBytes (String signature) {
    switch (signature.charAt(0)) {
      case 'V':
        return 0;
        
      case 'Z': 
      case 'B':
        return 1;
        
      case 'S':
      case 'C':
        return 2;
        
      case 'L':
      case '[':
      case 'F':
      case 'I':
        return 4;
        
      case 'D':
      case 'J':
        return 8;
    }

    throw new JPFException("invalid type string: " + signature);
  }
  
  public static int getTypeSize (String signature) {
    switch (signature.charAt(0)) {
    case 'V':
      return 0;

    case 'B':
    case 'C':
    case 'F':
    case 'I':
    case 'L':
    case 'S':
    case 'Z':
    case '[':
      return 1;

    case 'D':
    case 'J':
      return 2;
    }

    throw new JPFException("invalid type string: " + signature);
  }

  public static int getTypeSize (byte typeCategory){
    if (typeCategory == T_LONG || typeCategory == T_DOUBLE){
      return 2;
    } else {
      return 1;
    }
  }
  
  public static String asTypeName (String type) {
    if (type.startsWith("[") || type.endsWith(";")) {
      return getTypeName(type);
    }

    return type;
  }

  public static int booleanToInt (boolean b) {
    return b ? 1 : 0;
  }

  public static long doubleToLong (double d) {
    return Double.doubleToLongBits(d);
  }

  public static int floatToInt (float f) {
    return Float.floatToIntBits(f);
  }

  public static int hiDouble (double d) {
    return hiLong(Double.doubleToLongBits(d));
  }

  public static int hiLong (long l) {
    return (int) (l >> 32);
  }

  public static boolean instanceOf (String type, String ofType) {
    int bType = getBuiltinTypeFromSignature(type);

    if ((bType == T_ARRAY) && ofType.equals("Ljava.lang.Object;")) {
      return true;
    }

    int bOfType = getBuiltinTypeFromSignature(ofType);

    if (bType != bOfType) {
      return false;
    }

    switch (bType) {
    case T_ARRAY:
      return instanceOf(type.substring(1), ofType.substring(1));

    case T_REFERENCE:
      ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(getTypeName(type));
      return ci.isInstanceOf(getTypeName(ofType));

    default:
      return true;
    }
  }

  public static boolean intToBoolean (int i) {
    return i != 0;
  }

  public static float intToFloat (int i) {
    return Float.intBitsToFloat(i);
  }

  public static double intsToDouble (int l, int h) {
    long bits = ((long) h << 32) | ( l & 0xFFFFFFFFL);
    return Double.longBitsToDouble(bits);
  }

  public static long intsToLong (int l, int h) {
    return ((long) h << 32) | ( l & 0xFFFFFFFFL);
  }

  public static int loDouble (double d) {
    return loLong(Double.doubleToLongBits(d));
  }

  public static int loLong (long l) {
    return (int) (l & 0xFFFFFFFFL);
  }

  public static double longToDouble (long l) {
    return Double.longBitsToDouble(l);
  }

  private static int getTypeLength (String signature, int idx) {
    switch (signature.charAt(idx)) {
    case 'B':
    case 'C':
    case 'D':
    case 'F':
    case 'I':
    case 'J':
    case 'S':
    case 'V':
    case 'Z':
      return 1;

    case '[':
      return 1 + getTypeLength(signature, idx + 1);

    case 'L':

      int semicolon = signature.indexOf(';', idx);

      if (semicolon == -1) {
        throw new JPFException("invalid type signature: " +
                                         signature);
      }

      return semicolon - idx + 1;
    }

    throw new JPFException("invalid type signature");
  }

  
  public static String getSignatureName (String methodDecl) {

    StringBuffer sb = new StringBuffer(128);
    String retType = null;

    int i = methodDecl.indexOf('(');
    if (i>0){


      String[] a = methodDecl.substring(0, i).split(" ");
      if (a.length > 0){
        sb.append(a[a.length-1]);

        if (a.length > 1){
          retType = getTypeSignature(a[a.length-2], false);
        }
      }


      int j = methodDecl.lastIndexOf(')');
      if (j > 0){
        sb.append('(');
        for (String type : methodDecl.substring(i+1,j).split(",")){
          if (!type.isEmpty()){
            type = type.trim();
            if (!type.isEmpty()){
              sb.append( getTypeSignature(type,false));
            }
          }
        }
        sb.append(')');

        if (retType != null){
          sb.append(retType);
        }

        return sb.toString();
      }
    }

    throw new JPFException("invalid method declaration: " + methodDecl);
  }
  
  public static Class<?>[] getArgumentClasses(String signature) {
		int len = signature.length();
		if ((len > 1) && (signature.charAt(1) == ')')) {
			return new Class<?>[0]; 
		}
		ArrayList<Class<?>> argumentClasses = new ArrayList<Class<?>>();
		for (int i = 1; signature.charAt(i) != ')';) {
			int end = i + getTypeLength(signature, i);
			String arg = signature.substring(i, end);
			i = end;
			try {
				if (arg.charAt(0) == '[') {
					argumentClasses.add(Class.forName(arg.replace('/', '.')));
				} else if (arg.length() == 1) {
					switch (arg.charAt(0)) {
					case 'B':
						argumentClasses.add(byte.class);
						break;
					case 'C':
						argumentClasses.add(char.class);
						break;
					case 'D':
						argumentClasses.add(double.class);
						break;
					case 'F':
						argumentClasses.add(float.class);
						break;
					case 'I':
						argumentClasses.add(int.class);
						break;
					case 'J':
						argumentClasses.add(long.class);
						break;
					case 'S':
						argumentClasses.add(short.class);
						break;
					case 'V':
						argumentClasses.add(void.class);
						break;
					case 'Z':
						argumentClasses.add(boolean.class);
						break;
					default:
						throw new RuntimeException("Illegal primitive type signature " + arg);
					}
				} else {
					String typeName = getTypeName(arg);
					argumentClasses.add(Class.forName(typeName));
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return argumentClasses.toArray(new Class<?>[argumentClasses.size()]);
	}
}
