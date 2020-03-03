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


@SuppressWarnings("unchecked")
public class SimplePool<E> {
  static final double MAX_LOAD = 0.7;
  static final int DEFAULT_POW = 10;

  Object[] table;
  
  int count;
  int pow;
  int mask;
  int nextRehash;
  
  
  public SimplePool() {
    this(DEFAULT_POW);
  }
  
  
  public SimplePool(int pow) {
    table = new Object[1 << pow];
    count = 0;
    this.pow = pow;
    mask = table.length - 1;
    nextRehash = (int)(MAX_LOAD * mask);
  }


  
  
  public boolean isPooled(E e) {
    return e == null || query(e) != null;
  }
  
  
  public E query(E e) {
    if (e == null) return null;
    int code = e.hashCode();
    int idx = code & mask;
    int delta = (code >> (pow - 1)) | 1; 
    int oidx = idx;

    for(;;) {
      Object o = table[idx];
      if (o == null) break;
      if (e.equals(o)) {
        return (E) o; 
      }
      idx = (idx + delta) & mask;
      assert (idx != oidx); 
    }
    return null;
  }

  
  public E pool(E e) {
    if (e == null) return null;
    int code = e.hashCode();
    int idx = code & mask;
    int delta = (code >> (pow - 1)) | 1; 
    int oidx = idx;

    for(;;) {
      Object o = table[idx];
      if (o == null) break;
      if (e.equals(o)) {
        return (E) o; 
      }
      idx = (idx + delta) & mask;
      assert (idx != oidx); 
    }
    assert (table[idx] == null); 

    
    count++;
    if (count >= nextRehash) { 
      Object[] oldTable = table;
      pow++;
      table = new Object[1 << pow];
      mask = table.length - 1;
      nextRehash = (int)(MAX_LOAD * mask);

      int oldLen = oldTable.length;
      for (int i = 0; i < oldLen; i++) {
        Object o = oldTable[i];
        if (o != null) {
          code = o.hashCode();
          idx = code & mask;
          delta = (code >> (pow - 1)) | 1; 
          while (table[idx] != null) { 
            idx = (idx + delta) & mask;
          }
          table[idx] = o;
        }
      }

      code = e.hashCode();
      idx = code & mask;
      delta = (code >> (pow - 1)) | 1; 
      while (table[idx] != null) { 
        idx = (idx + delta) & mask;
      }
    } else {

    }

    table[idx] = e;
    return e;
  }
  
  

  
  public boolean isMember(E e) {
    return query(e) != null;
  }
  
  public void add(E e) {
     pool(e);
  }
  
  

  
  
  public static void main(String[] args) {
    SimplePool<Integer> pool = new SimplePool<Integer>(4);
    for (int i = 0; i < 1000000; i += 42) {
      Integer o = new Integer(i);
      Integer p = pool.pool(o);
      if (o != p) throw new RuntimeException();
      Integer q = pool.pool(p);
      if (q != p) throw new RuntimeException();
    }
    for (int i = 0; i < 1000000; i += 42) {
      Integer o = new Integer(i);
      Integer p = pool.pool(o);
      if (o == p) throw new RuntimeException();
      if (!o.equals(p)) throw new RuntimeException();
    }
    for (int i = 1; i < 1000000; i += 42) {
      Integer o = new Integer(i);
      Integer p = pool.pool(o);
      if (o != p) throw new RuntimeException();
    }
  }
}
