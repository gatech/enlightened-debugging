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


package java.lang;

import gov.nasa.jpf.annotation.NeverBreak;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Supplier;


public class ThreadLocal<T> {

  static class Entry<E> extends WeakReference<ThreadLocal<E>> {
    @NeverBreak
    E val;
    
    Entry (ThreadLocal<E> key, E val){
      super(key);
      this.val = val;
    }
    
    Entry<E> getChildEntry (){
      ThreadLocal<E> loc = get();
      if (loc instanceof InheritableThreadLocal){
        return new Entry<E>( loc, ((InheritableThreadLocal<E>)loc).childValue(val));
      } else {
        return null;
      }
    }
  }
  
  public ThreadLocal() {
  }
  
  
  protected T initialValue() {
    return null;
  }
    
  private native Entry<T> getEntry();
  private native void addEntry (Entry<T> e);
  private native void removeEntry (Entry<T> e);
  
  public T get() {
    Entry<T> e = getEntry();
    
    if (e == null){
      T v = initialValue();
      e = new Entry<T>(this, v);
      addEntry(e);
    }
    
    return e.val;
  }
  
  public void set (T v){
    Entry<T> e = getEntry();
    
    if (e != null){
      e.val = v;
      
    } else {
      e = new Entry<T>(this, v);
      addEntry(e);      
    }
  }
  
  public void remove(){
    Entry<T> e = getEntry();
    if (e != null){
      removeEntry(e);
    }
  }

  


  static final class SuppliedThreadLocal<E> extends ThreadLocal<E> {


    private final Supplier<? extends E> sup;

    SuppliedThreadLocal(Supplier<? extends E> supplier) {
      sup = Objects.requireNonNull(supplier);
    }

    @Override
    protected E initialValue() {
      return sup.get();
    }
  }
}
