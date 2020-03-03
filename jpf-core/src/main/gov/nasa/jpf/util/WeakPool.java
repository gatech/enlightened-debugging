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

import java.lang.ref.WeakReference;


public class WeakPool<E> {
  private static final boolean DEBUG = false; 
  
  static final double MAX_LOAD_WIPE = 0.6;
  static final double MAX_LOAD_REHASH = 0.4;
  static final int DEFAULT_POW = 10;

  WeakReference<E>[] table;
  
  int count;
  int pow;
  int mask;
  int nextWipe;
  int nextRehash;
  
  
  public WeakPool() {
    this(DEFAULT_POW);
  }
  
  
  public WeakPool(int pow) {
    this.pow = pow;
    newTable();
    count = 0;
    mask = table.length - 1;
    nextWipe = (int)(MAX_LOAD_WIPE * mask);
    nextRehash = (int)(MAX_LOAD_REHASH * mask);
  }

  @SuppressWarnings("unchecked")
  protected void newTable() {
    table = new WeakReference[1 << pow];
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
      WeakReference<E> r = table[idx];
      if (r == null) break;
      E o = r.get();
      if (o != null && e.equals(o)) {
        return o; 
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
      WeakReference<E> r = table[idx];
      if (r == null) break;
      E o = r.get();
      if (o != null && e.equals(o)) {
        return o; 
      }
      idx = (idx + delta) & mask;
      assert (idx != oidx); 
    }
    assert (table[idx] == null); 

    
    count++;
    if (count >= nextWipe) { 

      int oldCount = count;
      count = 0;
      for (int i = 0; i < table.length; i++) {
        if (table[i] != null && table[i].get() != null) {
          count++;
        }
      }
      if (DEBUG && oldCount > count) {
        System.out.println("Weak references collected: " + (oldCount - count));
      }
      if (count >= nextRehash) {
        pow++; 
      }
      WeakReference<E>[] oldTable = table;
      newTable();
      mask = table.length - 1;
      nextWipe = (int)(MAX_LOAD_WIPE * mask);
      nextRehash = (int)(MAX_LOAD_REHASH * mask);

      int oldLen = oldTable.length;
      for (int i = 0; i < oldLen; i++) {
        WeakReference<E> r = oldTable[i];
        if (r == null) continue;
        E o = r.get();
        if (o == null) continue;

        code = o.hashCode();
        idx = code & mask;
        delta = (code >> (pow - 1)) | 1; 
        while (table[idx] != null) { 
          idx = (idx + delta) & mask;
        }
        table[idx] = r;
      }

      code = e.hashCode();
      idx = code & mask;
      delta = (code >> (pow - 1)) | 1; 
      while (table[idx] != null) { 
        idx = (idx + delta) & mask;
      }
    } else {

    }

    table[idx] = new WeakReference<E>(e);
    return e;
  }
  
  

  
  public boolean isMember(E e) {
    return query(e) != null;
  }
  
  public void add(E e) {
     pool(e);
  }
  
  

  
  
  public static void main(String[] args) {
    WeakPool<Integer> pool = new WeakPool<Integer>(4);
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
