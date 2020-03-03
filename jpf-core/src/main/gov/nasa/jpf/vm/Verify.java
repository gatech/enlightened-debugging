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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.Random;



public class Verify {
  static final int MAX_COUNTERS = 10;
  static int[] counter;  

  private static Random random;

  
  static Class<?> peer;

  private static Random getRandom() {
    if (random == null) {
      random = new Random(42);
    }
    return random;
  }

  
  public static void setPeerClass (Class<?> cls) {
    peer = cls;
  }




  public static int getCounter (int id) {
    if (peer != null) {

      return JPF_gov_nasa_jpf_vm_Verify.getCounter__I__I(null, 0, id);
    } else {
      if (counter == null) {
        counter = new int[id >= MAX_COUNTERS ? (id+1) : MAX_COUNTERS];
      }
      if ((id < 0) || (id >= counter.length)) {
        return 0;
      }

      return counter[id];
    }
  }

  public static void resetCounter (int id) {
    if (peer != null){
      JPF_gov_nasa_jpf_vm_Verify.resetCounter__I__V(null, 0, id);
    } else {
      if ((counter != null) && (id >= 0) && (id < counter.length)) {
        counter[id] = 0;
      }
    }
  }

  public static void setCounter (int id, int val) {
    if (peer != null){
      JPF_gov_nasa_jpf_vm_Verify.setCounter__II__V(null, 0, id, val);
    } else {
      if ((counter != null) && (id >= 0) && (id < counter.length)) {
        counter[id] = val;
      }
    }
  }

  
  public static int incrementCounter (int id) {
    if (peer != null){
      return JPF_gov_nasa_jpf_vm_Verify.incrementCounter__I__I(null, 0, id);
    } else {
      if (counter == null) {
        counter = new int[(id >= MAX_COUNTERS) ? id+1 : MAX_COUNTERS];
      } else if (id >= counter.length) {
        int[] newCounter = new int[id+1];
        System.arraycopy(counter, 0, newCounter, 0, counter.length);
        counter = newCounter;
      }

      if ((id >= 0) && (id < counter.length)) {
        return ++counter[id];
      }

      return 0;
    }
  }

  public static final int NO_VALUE = -1;
  
  public static void putValue (String key, int value) {
    throw new UnsupportedOperationException("putValue requires JPF execution");
  }
  
  public static int getValue (String key) {
    throw new UnsupportedOperationException("getValue requires JPF execution");    
  }
  



  static BitSet[] bitSets;

  private static void checkBitSetId(int id) {
    if (bitSets == null) {
      bitSets = new BitSet[id + 1];
    } else if (id >= bitSets.length) {
      BitSet[] newBitSets = new BitSet[id + 1];
      System.arraycopy(bitSets, 0, newBitSets, 0, bitSets.length);
      bitSets = newBitSets;
    }

    if (bitSets[id] == null) {
      bitSets[id] = new BitSet();
    }
  }


  public static void setBitInBitSet(int id, int bit, boolean value) {
    if (peer != null){

      JPF_gov_nasa_jpf_vm_Verify.setBitInBitSet__IIZ__V(null, 0, id, bit, value);
    } else {

      checkBitSetId(id);
      bitSets[id].set(bit, value);
    }
  }

  public static boolean getBitInBitSet(int id, int bit) {
    if (peer != null){

      return JPF_gov_nasa_jpf_vm_Verify.getBitInBitSet__II__Z(null, 0, id, bit);

    } else {

      checkBitSetId(id);
      return bitSets[id].get(bit);
    }
  }

  
  public static void addComment (String s) {}

  
  @Deprecated
  public static void assertTrue (String s, boolean cond) {
    if (!cond) {
      System.out.println(s);
      assertTrue(cond);
    }
  }

  
  @Deprecated
  public static void assertTrue (boolean cond) {
    if (!cond) {
      throw new AssertionError("Verify.assertTrue failed");
    }
  }

  public static void atLabel (String label) {}

  public static void atLabel (int label) {}

  
  public static void beginAtomic () {}

  
  public static void endAtomic () {}

  public static void boring (boolean cond) {}

  public static void busyWait (long duration) {

    while (duration > 0) {
      duration--;
    }
  }

  public static boolean isCalledFromClass (String refClsName) {
    Throwable t = new Throwable();
    StackTraceElement[] st = t.getStackTrace();

    if (st.length < 3) {

      return false;
    }

    try {
      Class<?> refClazz = Class.forName(refClsName);
      Class<?> callClazz = Class.forName(st[2].getClassName());

      return (refClazz.isAssignableFrom(callClazz));

    } catch (ClassNotFoundException cfnx) {
      return false;
    }
  }

  public static void ignoreIf (boolean cond) {}

  public static void instrumentPoint (String label) {}

  public static void instrumentPointDeep (String label) {}

  public static void instrumentPointDeepRecur (String label, int depth) {}

  public static void interesting (boolean cond) {}

  public static void breakTransition (String reason) {}

 
  public static int breakTransition (String reason, int min, int max) {
    return -1;
  }

  
  public static void printPathOutput(String msg) {}
  public static void printPathOutput(boolean cond, String msg) {}

  public static void threadPrint (String s) {
    System.out.print( Thread.currentThread().getName());
    System.out.print(": ");
    System.out.print(s);
  }

  public static void threadPrintln (String s) {
    threadPrint(s);
    System.out.println();
  }
  
  public static void print (String s) {
    System.out.print(s);
  }

  public static void println (String s) {
    System.out.println(s);
  }
  
  public static void print (String s, int i) {
    System.out.print(s + " : " + i);
  }

  public static void print (String s, boolean b) {
    System.out.print(s + " : " + b);
  }

  public static void println() {
    System.out.println();
  }

  
  public static void print (String... args){
    for (String s : args){
      System.out.print(s);
    }
  }

  
  

  public static void setFieldAttribute (Object o, String fieldName, int val) {}
  public static int getFieldAttribute (Object o, String fieldName) { return 0; }
  

  public static void addFieldAttribute (Object o, String fieldName, int val) {}
  public static int[] getFieldAttributes (Object o, String fieldName) { return new int[0]; }

  public static void setLocalAttribute (String varName, int val) {}
  public static int getLocalAttribute (String varName) { return 0; }

  public static void addLocalAttribute (String varName, int val) {}
  public static int[] getLocalAttributes (String varName) { return new int[0]; }

  public static void setElementAttribute (Object arr, int idx, int val){}
  public static int getElementAttribute (Object arr, int idx) { return 0; }
  
  public static void addElementAttribute (Object arr, int idx, int val){}
  public static int[] getElementAttributes (Object arr, int idx) { return new int[0]; }

  public static void setObjectAttribute (Object o, int val) {}
  public static int getObjectAttribute (Object o) { return 0; }
  
  public static void addObjectAttribute (Object o, int val) {}
  public static int[] getObjectAttributes (Object o) { return new int[0]; }

  
  
  public static boolean getBoolean () {

    return ((System.currentTimeMillis() & 1) != 0);
  }

  
  public static boolean getBoolean (boolean falseFirst) {

    return getBoolean();
  }


  
  public static int getInt (int min, int max) {

    return getRandom().nextInt((max-min+1)) + min;
  }

  public static int getIntFromList (int... values){
    if (values != null && values.length > 0) {
      int i = getRandom().nextInt(values.length);
      return values[i];
    } else {
      return getRandom().nextInt();
    }
  }

  public static Object getObject (String key) {
    return "?";
  }

  
  public static int getInt (String key){

    return getRandom().nextInt();
  }

  
  public static double getDouble (String key){

    return getRandom().nextDouble();
  }

  public static double getDoubleFromList (double... values){
    if (values != null && values.length > 0) {
      int i = getRandom().nextInt(values.length);
      return values[i];
    } else {
      return getRandom().nextDouble();
    }
  }
  
  public static long getLongFromList (long...values){
    if (values != null && values.length > 0) {
      int i = getRandom().nextInt(values.length);
      return values[i];
    } else {
      return getRandom().nextLong();
    }    
  }

  public static float getFloatFromList (float...values){
    if (values != null && values.length > 0) {
      int i = getRandom().nextInt(values.length);
      return values[i];
    } else {
      return getRandom().nextFloat();
    }    
  }

  
  
  public static int random (int max) {

    return getRandom().nextInt(max + 1);
  }

  
  public static boolean randomBool () {

    return getRandom().nextBoolean();
  }

  public static long currentTimeMillis () {
    return System.currentTimeMillis();
  }


  public static Object randomObject (String type) {
    return null;
  }

  public static boolean isRunningInJPF() {
    return false;
  }

  
  public static boolean vmIsMatchingStates() {
    return false;
  }

  public static void storeTrace (String fileName, String comment) {

  }

  public static void storeTraceIf (boolean cond, String fileName, String comment) {
    if (cond) {
      storeTrace(fileName, comment);
    }
  }

  public static void storeTraceAndTerminate (String fileName, String comment) {
    storeTrace(fileName, comment);
    terminateSearch();
  }

  public static void storeTraceAndTerminateIf (boolean cond, String fileName, String comment) {
    if (cond) {
      storeTrace(fileName, comment);
      terminateSearch();
    }
  }

  public static boolean isTraceReplay () {
    return false; 
  }

  public static boolean isShared (Object o){
    return false; 
  }
  
  public static void setShared (Object o, boolean isShared) {

  }
  
  public static void freezeSharedness (Object o, boolean freeze) {

  }
  
  public static void terminateSearch () {

  }

  public static void setHeuristicSearchValue (int n){

  }

  public static void resetHeuristicSearchValue (){

  }

  public static int getHeuristicSearchValue (){

    return 0;
  }

  public static void setProperties (String... p) {

  }

  public static String getProperty (String key) {

    return null;
  }

  public static <T> T createFromJSON(Class<T> clazz, String json){
    return null;
  }

  public static void writeObjectToFile(Object object, String fileName) {
    try {
      FileOutputStream fso = new FileOutputStream(fileName);
      ObjectOutputStream oos = new ObjectOutputStream(fso);
      oos.writeObject(object);
      oos.flush();
      oos.close();

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  public static <T> T readObjectFromFile(Class<T> clazz, String fileName) {
    try
    {
      FileInputStream fis = new FileInputStream(fileName);
      ObjectInputStream ois = new ObjectInputStream(fis);

      Object read = ois.readObject();
      
      return (T) read;
      
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }

  }
  
  

  
  
  public static final int SEVERE = 1;
  public static final int WARNING = 2;
  public static final int INFO = 3;
  public static final int FINE = 4;
  public static final int FINER = 5;
  public static final int FINEST = 6;
  
  public static void log( String loggerId, int logLevel, String msg){
    System.err.println(msg);
  }


  public static void log( String loggerId, int logLevel, String msg, String arg){
    System.err.println(msg);
  }

  public static void log( String loggerId, int logLevel, String format, Object... args){
    System.err.printf(format, args);
  }

  
}
