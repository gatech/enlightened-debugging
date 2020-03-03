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


package java.io;

import java.nio.channels.FileChannel;

import java.io.FileDescriptor;

@SuppressWarnings("unused")
public class RandomAccessFile {
  public RandomAccessFile(File name, String permissions
                         ) throws FileNotFoundException {
    filename = name;
    isOpen = true;
    isReadOnly = "r".equals(permissions);
    setDataMap();
  }

  public RandomAccessFile(String name, String permissions
                         ) throws FileNotFoundException {
    this(new File(name), permissions);
  }

  public void seek(long posn) throws IOException {
    currentPosition = posn;
  }

  public long length() throws IOException {
    return currentLength;
  }

  public native void setDataMap();
  
  public native void writeByte(int data) throws IOException;

  public native void write(byte[] data, int start, int len
                          ) throws IOException;


  public native void setLength(long len) throws IOException;

  public native int read(byte[] data, int start, int len
                         ) throws IOException;

  public native byte readByte() throws IOException;

  public void close() throws IOException {
    isOpen = false;
  }

  public FileChannel getChannel(){
    return null;
  }

  public FileDescriptor getFD(){
    return null;
  }

  private static class DataRepresentation {
    DataRepresentation next;
    long chunk_index;
    int[] data;
  }

  private final static void printList(DataRepresentation node) {
    DataRepresentation cur = node;
    System.out.print("Chunks:");
    while (cur != null) {
      System.out.print(" " + cur.chunk_index);
      cur = cur.next;
    }
    System.out.println();
  }

  private static final int CHUNK_SIZE = 256;
  private File filename;
  private boolean isOpen;
  private boolean isReadOnly;
  private long currentLength;
  private long currentPosition;
  private DataRepresentation data_root = null;
}

