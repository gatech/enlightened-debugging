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

import java.io.PrintStream;




public abstract class ArrayFields extends Fields {

  int getNumberOfFieldsOrElements () {
    return arrayLength(); 
  }

  public abstract int arrayLength ();

  @Override
  public abstract int getHeapSize ();

  @Override
  public boolean isReferenceArray(){
    return false;
  }

  public int getNumberOfFields() {

    return 0;
  } 
  
  public abstract void copyElements (ArrayFields src, int srcPos, int dstPos, int len);
  
  public void printElements( PrintStream ps, int max){
    int len = arrayLength();
    if (max < 0) max = len;
    int i;
    for (i=0; i<max; i++){
      if (i>0){
        ps.print(',');
      }
      printValue(ps, i);
    }
    if (i < len){
      ps.print("...");
    }
  }  
  
  protected abstract void printValue (PrintStream ps, int idx);
  
  public abstract Object getValues();

  @Override
  public boolean getBooleanValue (int pos) {

      throw new JPFException( "not a boolean[]");
  }
  @Override
  public byte getByteValue (int pos) {

    throw new JPFException( "not a byte[]");
  }
  @Override
  public char getCharValue (int pos) {

    throw new JPFException( "not a char[]");
  }
  @Override
  public short getShortValue (int pos) {

    throw new JPFException( "not a short[]");
  }
  @Override
  public int getIntValue (int pos) {

    throw new JPFException( "not an int[]");
  }
  @Override
  public long getLongValue (int pos) {

    throw new JPFException( "not a long[]");
  }
  @Override
  public float getFloatValue (int pos) {

    throw new JPFException( "not a float[]");
  }
  @Override
  public double getDoubleValue (int pos) {

    throw new JPFException( "not a double[]");
  }
  @Override
  public int getReferenceValue (int pos) {

    throw new JPFException( "not a reference array");
  }

  @Override
  public void setBooleanValue (int pos, boolean newValue) {

    throw new JPFException( "not a boolean[]");
  }
  @Override
  public void setByteValue (int pos, byte newValue) {

    throw new JPFException( "not a byte[]");
  }
  @Override
  public void setCharValue (int pos, char newValue) {

    throw new JPFException( "not a char[]");
  }
  @Override
  public void setShortValue (int pos, short newValue) {

    throw new JPFException( "not a short[]");
  }
  @Override
  public void setIntValue (int pos, int newValue) {

    throw new JPFException( "not an int[]");
  }
  @Override
  public void setFloatValue (int pos, float newValue){

    throw new JPFException( "not a float[]");
  }
  @Override
  public void setLongValue (int pos, long newValue) {

    throw new JPFException( "not a long[]");
  }
  @Override
  public void setDoubleValue (int pos, double newValue){

    throw new JPFException( "not a double[]");
  }
  @Override
  public void setReferenceValue (int pos, int newValue){

    throw new JPFException( "not a reference array");
  }


  public boolean[] asBooleanArray () {

    throw new JPFException("not a boolean[]");
  }
  public byte[] asByteArray () {

    throw new JPFException("not a byte[]");
  }
  public char[] asCharArray () {

    throw new JPFException("not a char[]");
  }
  public char[] asCharArray (int offset, int length) {

    throw new JPFException("not a char[]");
  }
  public short[] asShortArray () {

    throw new JPFException("not a short[]");
  }
  public int[] asIntArray () {

    throw new JPFException("not a int[]");
  }
  public int[] asReferenceArray () {

    throw new JPFException("not a reference array");
  }
  public long[] asLongArray () {

    throw new JPFException("not a long[]");
  }
  public float[] asFloatArray () {

    throw new JPFException("not a float[]");
  }
  public double[] asDoubleArray () {

    throw new JPFException("not a double[]");
  }

  @Override
  public int[] asFieldSlots() {
    throw new JPFException("array has no field slots");
  }

}
