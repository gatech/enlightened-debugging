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

import java.util.Iterator;


public final class DynamicIntArray implements Iterable<Integer> {
  final static int DEFAULT_CHUNKBITS = 8;
  final static int INIT_CHUNKS = 16;

  class DynIntIterator implements Iterator<Integer> {
    int i;
    
    @Override
	public boolean hasNext() {
      return (i<size());
    }

    @Override
	public Integer next() {
      return new Integer(get(i++));
    }

    @Override
	public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  
  
  Growth growth;
  
  
  int chunkBits;
  int nPerChunk; 
  
  
  int chunkMask;
  
  
  int[][] data;
  
  
  int maxIndex = -1;
  
  public DynamicIntArray () {
    this(Growth.defaultGrowth, DEFAULT_CHUNKBITS, INIT_CHUNKS);
  }

  public DynamicIntArray (int size) {
    this(Growth.defaultGrowth, DEFAULT_CHUNKBITS,
        (size + (1<<DEFAULT_CHUNKBITS)-1) / (1<<DEFAULT_CHUNKBITS));
  }
  
  
  public DynamicIntArray (int chunkBits, int initChunks) {
    this(Growth.defaultGrowth, chunkBits, initChunks);
  }
  
  public DynamicIntArray (Growth strategy, int chunkBits, int initChunks) {
    if (chunkBits > 20) throw new IllegalArgumentException();
    this.chunkBits = chunkBits;
    nPerChunk = 1<<chunkBits;
    this.chunkMask = nPerChunk - 1;
    data = new int[initChunks][];
    growth = strategy;
  }

  public int get (int index) {
    int i = index >> chunkBits;
    if (i < data.length && data[i] != null) {
      int j = index & chunkMask;
      return data[i][j];
    } else {
      return 0;
    }
  }


  public int size() {
    return data.length * nPerChunk;
  }
  
  public int getMaxIndex() {
    return maxIndex;
  }
  
  public void set (int index, int value) {
    if (index > maxIndex) {
      maxIndex = index;
    }
    
    int i = index >> chunkBits;
    int j = index & chunkMask;
    
    if (i >= data.length) {
      int nChunks = growth.grow(data.length, i+1);
      int[][] newChunks = new int[nChunks][];
      System.arraycopy(data, 0, newChunks, 0, data.length);
      data = newChunks;
    }
    if (data[i] == null) {
      data[i] = new int[nPerChunk];
    }
    
    data[i][j] = value;
  }

  @Override
  public String toString() {
    int length = data.length * nPerChunk;
    while (length > 1 && get(length-1) == 0) length--;

    StringBuilder sb = new StringBuilder(length);
    
    sb.append('{');
    int l = length-1;
    for (int i=0; i<l; i++) {
      sb.append(get(i));
      sb.append(',');
    }
    sb.append(get(l));
    sb.append('}');
    
    return sb.toString();
  }

  @Override
  public Iterator<Integer> iterator() {
    return new DynIntIterator();
  }


  
}

