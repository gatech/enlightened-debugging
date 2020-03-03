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



package gov.nasa.jpf.util.json;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.ObjectConverter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClinitRequired;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Fields;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.HashMap;
import java.util.Set;


public class JSONObject{

  private static final JPFLogger logger = JPF.getLogger("gov.nasa.jpf.util.json.JSONObject");

  private HashMap<String, Value> keyValues = new HashMap<String, Value>();
  private HashMap<String, CGCall> cgCalls = new HashMap<String, CGCall>();

  void addValue(String key, Value value) {
    if (keyValues.containsKey(key)) {
      throw new JPFException("Attempt to add two nodes with the same key in JSON object");
    }

    keyValues.put(key, value);
  }

  
  public Value getValue(String key) {
    return keyValues.get(key);
  }

  public String[] getValuesKeys() {
    Set<String> valuesKeys = keyValues.keySet();
    String[] result = new String[keyValues.size()];

    valuesKeys.toArray(result);
    return result;
  }

  public void addCGCall(String key, CGCall cgCall) {
    if (cgCalls.containsKey(key)) {
      throw new JPFException("Attempt to add two CG with the same key in JSON object");
    }

    cgCalls.put(key, cgCall);
  }

  public CGCall getCGCall(String key) {
    return cgCalls.get(key);
  }

  public String[] getCGCallsKeys() {
    Set<String> cgKeys = cgCalls.keySet();
    String[] result = new String[cgKeys.size()];

    cgKeys.toArray(result);
    return result;
  }

  
  public boolean requiresClinitExecution (ClassInfo ci, ThreadInfo ti){
    while (ci != null){
      if (ci.initializeClass(ti)){
        return true;
      }

      for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
        ClassInfo ciField = fi.getTypeClassInfo();
        if (requiresClinitExecution(ciField, ti)){
          return true;
        }
        if (ciField.isArray()){
          ClassInfo ciComp = ciField.getComponentClassInfo();
          if (requiresClinitExecution(ciComp, ti)) {
            return true;
          }
        }
      }
      
      ci = ci.getSuperClass();
    }
    
    return false;
  }
  

  


  
  public int fillObject (MJIEnv env, ClassInfo ci, ChoiceGenerator<?>[] cgs, String prefix) throws ClinitRequired {
    int newObjRef = env.newObject(ci);
    ElementInfo ei = env.getHeap().getModifiable(newObjRef);


    while (ci != null) {
      FieldInfo[] fields = ci.getDeclaredInstanceFields();

      for (FieldInfo fi : fields) {
        String fieldName = fi.getName();
        Value val = getValue(fieldName);
        CGCall cgCall = getCGCall(fieldName);


        if (val != null) {
          fillFromValue(fi, ei, val, env, cgs, prefix);
          
        } else if (cgCall != null) {

          String cgId = prefix + fieldName;
          ChoiceGenerator<?> cg = getCGByID(cgs, cgId);
          assert cg != null : "Expected CG with id " + cgId;
          
          Object cgResult = cg.getNextChoice();

          if (!fi.isReference()) {
            convertPrimititve(ei, fi, cgResult);
          } else {
            int newFieldRef = ObjectConverter.JPFObjectFromJavaObject(env, cgResult);
            ei.setReferenceField(fi, newFieldRef);
          }
        } else {
          logger.warning("Value for field ", fi.getFullName(), " isn't specified");
        }
      }

      ci = ci.getSuperClass();
    }

    return newObjRef;
  }

  private void fillFromValue(FieldInfo fi, ElementInfo ei, Value val, MJIEnv env, ChoiceGenerator<?>[] cgs, String prefix) {
    String fieldName = fi.getName();

    if (!fi.isReference()) {
      fillPrimitive(ei, fi, val);
      
    } else {
      if (isArrayType(fi.getType())) {
        int newArrRef = createArray(env, fi.getTypeClassInfo(), val, cgs, prefix + fieldName);
        ei.setReferenceField(fi, newArrRef);

      } else {
        Creator creator = CreatorsFactory.getCreator(fi.getType());
        if (creator != null) {
          int newSubObjRef = creator.create(env, fi.getType(), val);
          ei.setReferenceField(fi, newSubObjRef);
          
        } else {

          ClassInfo ciField = fi.getTypeClassInfo();
          if (ciField.initializeClass(env.getThreadInfo())){
            throw new ClinitRequired(ciField);
          }
          
          JSONObject jsonObj = val.getObject();
          int fieldRef = MJIEnv.NULL;
          if (jsonObj != null) {
            fieldRef = jsonObj.fillObject(env, ciField, cgs, prefix + fieldName);
          }
          ei.setReferenceField(fi.getName(), fieldRef);
        }
      }
    }
  }


  private static void fillPrimitive(ElementInfo ei, FieldInfo fi, Value val) {
    String primitiveName = fi.getType();

    if (primitiveName.equals("boolean")) {
      ei.setBooleanField(fi, val.getBoolean());

    } else if (primitiveName.equals("byte")) {
      ei.setByteField(fi, val.getDouble().byteValue());

    } else if (primitiveName.equals("short")) {
      ei.setShortField(fi, val.getDouble().shortValue());

    } else if (primitiveName.equals("int")) {
      ei.setIntField(fi, val.getDouble().intValue());

    } else if (primitiveName.equals("long")) {
      ei.setLongField(fi, val.getDouble().longValue());

    } else if (primitiveName.equals("float")) {
      ei.setFloatField(fi, val.getDouble().floatValue());

    } else if (primitiveName.equals("double")) {
      ei.setDoubleField(fi, val.getDouble());
    }
  }

  public int createArray(MJIEnv env, ClassInfo ciArray, Value value, ChoiceGenerator<?>[] cgs, String prefix) {
    Value vals[] = value.getArray();

    ClassInfo ciElement = ciArray.getComponentClassInfo();
    String arrayElementType = ciElement.getName();
    int arrayRef;


    if (arrayElementType.equals("boolean")) {
       arrayRef = env.newBooleanArray(vals.length);
       ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
       boolean bools[] = arrayEI.asBooleanArray();

       for (int i = 0; i < vals.length; i++) {
        bools[i] = vals[i].getBoolean();
      }
    } else if (arrayElementType.equals("byte")) {
       arrayRef = env.newByteArray(vals.length);
       ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
       byte bytes[] = arrayEI.asByteArray();

       for (int i = 0; i < vals.length; i++) {
        bytes[i] = vals[i].getDouble().byteValue();
      }
    } else if (arrayElementType.equals("short")) {
       arrayRef = env.newShortArray(vals.length);
       ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
       short shorts[] = arrayEI.asShortArray();

       for (int i = 0; i < vals.length; i++) {
        shorts[i] = vals[i].getDouble().shortValue();
      }
    } else if (arrayElementType.equals("int")) {
      arrayRef = env.newIntArray(vals.length);
      ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
      int[] ints = arrayEI.asIntArray();

      for (int i = 0; i < vals.length; i++) {
        ints[i] = vals[i].getDouble().intValue();
      }
    } else if (arrayElementType.equals("long")) {
      arrayRef = env.newLongArray(vals.length);
      ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
      long[] longs = arrayEI.asLongArray();

      for (int i = 0; i < vals.length; i++) {
        longs[i] = vals[i].getDouble().longValue();
      }
    } else if (arrayElementType.equals("float")) {
      arrayRef = env.newFloatArray(vals.length);
      ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
      float[] floats = arrayEI.asFloatArray();

      for (int i = 0; i < vals.length; i++) {
        floats[i] = vals[i].getDouble().floatValue();
      }
    } else if (arrayElementType.equals("double")) {
      arrayRef = env.newDoubleArray(vals.length);
      ElementInfo arrayEI = env.getHeap().getModifiable(arrayRef);
      double[] doubles = arrayEI.asDoubleArray();

      for (int i = 0; i < vals.length; i++) {
        doubles[i] = vals[i].getDouble();
      }
    } else {

      arrayRef = env.newObjectArray(arrayElementType, vals.length);
      ElementInfo arrayEI = env.getModifiableElementInfo(arrayRef);

      Fields fields = arrayEI.getFields();

      Creator creator = CreatorsFactory.getCreator(arrayElementType);
      for (int i = 0; i < vals.length; i++) {

        int newObjRef;
        if (creator != null) {
          newObjRef = creator.create(env, arrayElementType, vals[i]);
        } else{
          if (isArrayType(arrayElementType)) {
            newObjRef = createArray(env, ciElement, vals[i], cgs, prefix + "[" + i);
          } else {
            JSONObject jsonObj = vals[i].getObject();
            if (jsonObj != null) {
              newObjRef = jsonObj.fillObject(env, ciElement, cgs, prefix + "[" + i);
            } else {
              newObjRef = MJIEnv.NULL;
            }
          }
        }

        fields.setReferenceValue(i, newObjRef);
      }
    }

    return arrayRef;
  }


  private boolean isArrayType(String typeName) {
    return typeName.lastIndexOf('[') >= 0;
  }

  
  private void convertPrimititve(ElementInfo ei, FieldInfo fi, Object cgResult) {
    String primitiveName = fi.getType();

    if (primitiveName.equals("boolean") && cgResult instanceof Boolean) {
      Boolean bool = (Boolean) cgResult;
      ei.setBooleanField(fi, bool.booleanValue());
    } else if (cgResult instanceof Number) {
      Number number = (Number) cgResult;

      if (primitiveName.equals("byte")) {
        ei.setByteField(fi, number.byteValue());

      } else if (primitiveName.equals("short")) {
        ei.setShortField(fi, number.shortValue());

      } else if (primitiveName.equals("int")) {
        ei.setIntField(fi, number.intValue());

      } else if (primitiveName.equals("long")) {
        ei.setLongField(fi, number.longValue());

      } else if (primitiveName.equals("float")) {
        ei.setFloatField(fi, number.floatValue());

      } else if (primitiveName.equals("double")) {
        ei.setDoubleField(fi, number.doubleValue());
      }
    } else if (cgResult instanceof Character) {
      Character c = (Character) cgResult;
      ei.setCharField(fi, c);
      
    } else {
      throw new JPFException("Can't convert " + cgResult.getClass().getCanonicalName() +
                             " to " + primitiveName);
    }
  }

  
  private ChoiceGenerator<?> getCGByID(ChoiceGenerator<?>[] cgs, String id) {
    if (cgs == null) {
      return null;
    }
    
    for (int i = 0; i < cgs.length; i++) {
      if (cgs[i].getId().equals(id)) {
        return cgs[i];
      }
    }

    return null;
  }
}
