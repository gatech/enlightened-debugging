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



package gov.nasa.jpf.util;

import gov.nasa.jpf.vm.ClassParseException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class BinaryClassSource {

  protected byte[] data;
  
  protected int pos; 
  protected int pc; 
  
  protected int[] posStack;
  protected int top;
  
  protected ByteReader byteReader;
  

  
  public interface ByteReader {
    int peekI2();
    int peekU2();
    int peekI4();
    long peekU4();
    
    int readI2();
    int readU2();
    int readI4();
    long readU4();
    
    void makeLittleEndian (short[] data);
  }

  public class LittleEndianReader implements ByteReader {
    
    @Override
	public final int peekI2 () {
      int idx = pos;
      return (data[idx++] & 0xff) | (data[idx] << 8);
    }

    @Override
	public final int peekU2 () {
      int idx = pos;
      return (data[idx++] & 0xff) | ((data[idx] & 0xff)<< 8);
    }
    
    @Override
	public final int peekI4 () {
      int idx = pos;
      byte[] data = BinaryClassSource.this.data;
      return (data[idx++] & 0xff) | ((data[idx++] & 0xff) << 8) | ((data[idx++] & 0xff) << 16) | (data[idx] << 24);
    }

    @Override
	public final long peekU4 () {
      int idx = pos;
      byte[] data = BinaryClassSource.this.data;
      return (data[idx++] & 0xff) | ((data[idx++] & 0xff) << 8) | ((data[idx++] & 0xff) << 16) | ((data[idx] & 0xff) << 24);
    }
    
    
    @Override
	public final int readI2 () {
      int idx = pos;
      pos += 2;
      return (data[idx++] & 0xff) | (data[idx] << 8);
    }

    @Override
	public final int readU2 () {
      int idx = pos;
      pos += 2;
      return (data[idx++] & 0xff) | ((data[idx] & 0xff)<< 8);
    }
    
    @Override
	public final int readI4 () {
      int idx = pos;
      pos += 4;
      byte[] data = BinaryClassSource.this.data;

      return (data[idx++] & 0xff) | ((data[idx++] & 0xff) << 8) | ((data[idx++] & 0xff) << 16) | (data[idx] << 24);
    }

    @Override
	public final long readU4 () {
      int idx = pos;
      pos += 4;
      byte[] data = BinaryClassSource.this.data;

      return (data[idx++] & 0xff) | ((data[idx++] & 0xff) << 8) | ((data[idx++] & 0xff) << 16) | ((data[idx] & 0xff) << 24);
    }
    
    @Override
	public final void makeLittleEndian (short[] data){

    }
  }

  
  public class BigEndianReader implements ByteReader {
    
    @Override
	public final int peekI2 () {
      int idx = pos;
      return (data[idx++] << 8) | (data[idx] & 0xff);
    }

    @Override
	public final int peekU2 () {
      int idx = pos;
      return ((data[idx++] & 0xff) << 8) | (data[idx] & 0xff);
    }
    
    @Override
	public final int peekI4 () {
      int idx = pos;
      byte[] data = BinaryClassSource.this.data;

      return (data[idx++] << 24) | ((data[idx++] & 0xff) << 16) | ((data[idx++] & 0xff) << 8) | (data[idx] & 0xff);
    }

    @Override
	public final long peekU4 () {
      int idx = pos;
      byte[] data = BinaryClassSource.this.data;

      return ((data[idx++] & 0xff) << 24) | ((data[idx++] & 0xff) << 16) | ((data[idx++] & 0xff) << 8) | (data[idx] & 0xff);
    }

    
    @Override
	public final int readI2 () {
      int idx = pos;
      pos += 2;
      return (data[idx++] << 8) | (data[idx] & 0xff);
    }

    @Override
	public final int readU2 () {
      int idx = pos;
      pos += 2;
      return ((data[idx++] & 0xff) << 8) | (data[idx] & 0xff);
    }
    
    @Override
	public final int readI4 () {
      int idx = pos;
      pos += 4;
      byte[] data = BinaryClassSource.this.data;

      return (data[idx++] << 24) | ((data[idx++] & 0xff) << 16) | ((data[idx++] & 0xff) << 8) | (data[idx] & 0xff);
    }

    @Override
	public final long readU4 () {
      int idx = pos;
      pos += 4;
      byte[] data = BinaryClassSource.this.data;

      return ((data[idx++] & 0xff) << 24) | ((data[idx++] & 0xff) << 16) | ((data[idx++] & 0xff) << 8) | (data[idx] & 0xff);
    }
    
    @Override
	public final void makeLittleEndian (short[] data){
      for (int i=0; i<data.length; i++){
        short s = data[i];
        s = (short) (((s & 0xFF00) >> 8) | (s << 8));
        data[i] = s;
      }
    }
  }
  


  
  protected BinaryClassSource (byte[] data, int pos){
   this.data = data;
   this.pos = pos;
   
   this.byteReader = initializeByteReader();
  }
  
  protected BinaryClassSource (File file) throws ClassParseException {
    FileInputStream is = null;
    try {
      is = new FileInputStream(file);
      long len = file.length();
      if (len > Integer.MAX_VALUE || len <= 0){   
        error("cannot read file of size: " + len);
      }
      data = new byte[(int)len];
      readData(is);
      
    } catch (FileNotFoundException fnfx) {
      error("classfile not found: " + file.getPath());

    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException iox) {
          error("failed to close file: " + file.getPath());
        }
      }
    }
    
    this.byteReader = initializeByteReader();
  }
  
  protected ByteReader initializeByteReader(){

    return new BigEndianReader();
  }
  
  protected void readData (InputStream is) throws ClassParseException {
    try {
      int nRead = 0;

      while (nRead < data.length){
        int n = is.read(data, nRead, (data.length - nRead));
        if (n < 0){
          error("premature end of dex file: " + data.length + '/' + nRead);
        }
        nRead += n;
      }

    } catch (IOException iox){
      error("failed to read dex file");
    }
  }
  
  public void stopParsing(){
    throw new BailOut();
  }

  protected void error(String msg) throws ClassParseException {
    throw new ClassParseException(msg);
  }
  
  
  public byte[] getData(){
    return data;
  }
  
  public int getPos(){
    return pos;
  }
  
  public boolean hasMoreData(){
    return pos < data.length;
  }
  

  public void setPos (int newPos){
    pos = newPos;
  }
  
  public void pushPos(){
    if (posStack == null){
      posStack = new int[4];
      posStack[0] = pos;
      top = 0;
    } else {
      top++;
      if (top == posStack.length){
        int[] newStack = new int[posStack.length * 2];
        System.arraycopy(posStack, 0, newStack, 0, posStack.length);
        posStack = newStack;
      }
      posStack[top] = pos;
    }
  }
  
  public void popPos(){
    if (top >= 0){
      pos = posStack[top];
      top--;
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
  
  public final int readByte(){
    return data[pos++];
  }

  public final int readUByte(){
    return (data[pos++] & 0xff);
  }
  
  public final byte[] read (int n){
    byte[] b = new byte[n];
    System.arraycopy(data,pos,b,0,n);
    pos += n;
    return b;
  }
  
  public String readByteString(int nChars){
    char[] buf = new char[nChars];
    for (int i=0; i<nChars; i++){
      buf[i] = (char)data[pos++];
    }
    return new String(buf);
  }
  

  protected void dumpData (int startPos, int nBytes){
    System.out.printf("%d +%d: [", startPos, nBytes);
    for (int i=0; i<nBytes; i++){
      System.out.printf("%02X ", data[startPos+i]);
    }
    System.out.println(']');
  }
    
  protected String dataToString (int startPos, int nBytes){
    StringBuilder sb = new StringBuilder();
    int i1 = startPos + nBytes;
    for (int i=startPos; i<i1; i++){
      sb.append( Integer.toHexString(data[i]));
      sb.append(' ');
    }

    return sb.toString();
  }
}
