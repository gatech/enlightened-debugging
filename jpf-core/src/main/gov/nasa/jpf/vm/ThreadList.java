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


import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.Predicate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class ThreadList implements Cloneable, Iterable<ThreadInfo>, Restorable<ThreadList> {

  public static class Count {
    public final int alive;
    public final int runnableNonDaemons;
    public final int runnableDaemons;
    public final int blocked;
    
    Count (int alive, int runnableNonDaemons, int runnableDaemons, int blocked){
      this.alive = alive;
      this.runnableNonDaemons = runnableNonDaemons;
      this.runnableDaemons = runnableDaemons;
      this.blocked = blocked;
    }
  }
  
  protected boolean reuseTid;
  

  protected ThreadInfo[] threads;
  

  protected int maxTid;


  static class TListMemento implements Memento<ThreadList> {

    Memento<ThreadInfo>[] tiMementos;
    int maxTid;

    TListMemento(ThreadList tl) {
      ThreadInfo[] threads = tl.threads;
      int len = threads.length;

      maxTid = tl.maxTid;
      tiMementos = new Memento[len];
      for (int i=0; i<len; i++){
        ThreadInfo ti = threads[i];
        Memento<ThreadInfo> m = null;

        if (!ti.hasChanged()){
          m = ti.cachedMemento;
        }
        if (m == null){
          m = ti.getMemento();
          ti.cachedMemento = m;
        }
        tiMementos[i] = m;
      }
    }

    @Override
	public ThreadList restore(ThreadList tl){
      int len = tiMementos.length;
      ThreadInfo[] threads = new ThreadInfo[len];
      for (int i=0; i<len; i++){
        Memento<ThreadInfo> m = tiMementos[i];
        ThreadInfo ti = m.restore(null);
        ti.cachedMemento = m;
        threads[i] = ti;
        ti.tlIdx = i;
      }
      tl.threads = threads;
      tl.maxTid = maxTid;

      return tl;
    }
  }


  protected ThreadList() {

  }

  
  public ThreadList (Config config, KernelState ks) {
    threads = new ThreadInfo[0];
    
    reuseTid = config.getBoolean("vm.reuse_tid", false);
  }

  @Override
  public Memento<ThreadList> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }
  public Memento<ThreadList> getMemento(){
    return new TListMemento(this);
  }

  @Override
  public Object clone() {
    ThreadList other = new ThreadList();
    other.threads = new ThreadInfo[threads.length];

    for (int i=0; i<threads.length; i++) {
      other.threads[i] = (ThreadInfo) threads[i].clone();
    }

    return other;
  }

  
  public int add (ThreadInfo ti) {
    int n = threads.length;

    BitSet ids = new BitSet();   
    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t == ti) {
        return t.getId();
      }
      
      ids.set( t.getId());
    }


    ThreadInfo[] newThreads = new ThreadInfo[n+1];
    System.arraycopy(threads, 0, newThreads, 0, n);
    
    newThreads[n] = ti;
    ti.tlIdx = n;
    
    threads = newThreads;
    
    if (reuseTid){
      return ids.nextClearBit(0);
    } else {
      return maxTid++;
    }
  }
  
  public boolean remove (ThreadInfo ti){
    int n = threads.length;
    
    for (int i=0; i<n; i++) {
      if (ti == threads[i]){
        int n1 = n-1;
        ThreadInfo[] newThreads = new ThreadInfo[n1];
        if (i>0){
          System.arraycopy(threads, 0, newThreads, 0, i);
        }
        if (i<n1){
          System.arraycopy(threads, i+1, newThreads, i, (n1-i));
          

          for (int j=i; j<n1; j++){
            ThreadInfo t = threads[j];
            if (t != null){
              t.tlIdx = j;
            }
          }
        }
        
        threads = newThreads;        
        return true;
      }
    }
    
    return false;
  }

  
  public ThreadInfo[] getThreads() {
    return threads.clone();
  }

  public void hash (HashData hd) {
    for (int i=0; i<threads.length; i++){
      threads[i].hash(hd);
    }
  }
  
  public ThreadInfo getThreadInfoForId (int tid){
    for (int i=0; i<threads.length; i++){
      ThreadInfo ti = threads[i];
      if (ti.getId() == tid){
        return ti;
      }
    }
    
    return null;
  }

  public ThreadInfo getThreadInfoForObjRef (int objRef){
    for (int i=0; i<threads.length; i++){
      ThreadInfo ti = threads[i];
      if (ti.getThreadObjectRef() == objRef){
        return ti;
      }
    }
    
    return null;
  }
  
  public boolean contains (ThreadInfo ti){
    int idx = ti.tlIdx;
    
    if (idx < threads.length){
      return threads[idx] == ti;
    }
    
    return false;
  }
  
  
  public int length () {
    return threads.length;
  }

  
  public void setAll(ThreadInfo[] threads) {
    this.threads = threads;
  }

  public ThreadInfo locate (int objref) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].getThreadObjectRef() == objref) {
        return threads[i];
      }
    }

    return null;
  }

  public void markRoots (Heap heap) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].isAlive()) {
        threads[i].markRoots(heap);
      }
    }
  }
  
  public boolean hasProcessTimeoutRunnables (ApplicationContext appCtx){
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      if (ti.isTimeoutRunnable() && ti.getApplicationContext() == appCtx) {
        return true;
      }
    }
    return false;
  }
  
  public ThreadInfo[] getProcessTimeoutRunnables (ApplicationContext appCtx){
    ArrayList<ThreadInfo> list = new ArrayList<ThreadInfo>();
    
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      if (ti.isTimeoutRunnable() && ti.getApplicationContext() == appCtx) {
        list.add(ti);
      }
    }
    
    return list.toArray( new ThreadInfo[list.size()]);
  }
  
  public boolean hasLiveThreads(){
    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isAlive()) {
        return true;
      }
    }
    return false;    
  }
  
  public boolean hasTimeoutRunnables (){
    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isRunnable()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean hasUnblockedThreads(){
    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isUnblocked()) {
        return true;
      }
    }
    return false;    
  }

  public ThreadInfo[] getTimeoutRunnables (){
    ArrayList<ThreadInfo> list = new ArrayList<ThreadInfo>();
    
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      if (ti.isTimeoutRunnable()) {
        list.add(ti);
      }
    }
    
    return list.toArray( new ThreadInfo[list.size()]);
  }

  

  public boolean hasAnyMatching(Predicate<ThreadInfo> predicate) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (predicate.isTrue(threads[i])) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean hasAnyMatchingOtherThan(ThreadInfo ti, Predicate<ThreadInfo> predicate) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if(ti != threads[i]) {
        if (predicate.isTrue(threads[i])) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  public boolean hasOnlyMatching(Predicate<ThreadInfo> predicate) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (!predicate.isTrue(threads[i])) {
        return false;
      }
    }
    
    return true;
  }
  
  public boolean hasOnlyMatchingOtherThan(ThreadInfo ti, Predicate<ThreadInfo> predicate) {
    int n=0;
    for (int i = 0, l = threads.length; i < l; i++) {
      if(ti != threads[i]) {
        if (!predicate.isTrue(threads[i])) {
          return false;
        } else {
          n++;
        }
      }
    }
    
    return (n>0);
  }
  
  public ThreadInfo[] getAllMatching(Predicate<ThreadInfo> predicate) {
    List<ThreadInfo> list = new ArrayList<ThreadInfo>();
    
    int n = 0;
    for (int i = 0, l = threads.length; i < l; i++) {
      ThreadInfo ti = threads[i];
      if (predicate.isTrue(ti)) {
        list.add(ti);
        n++;
      }
    }
    
    return list.toArray(new ThreadInfo[n]);
  }

  public ThreadInfo[] getAllMatchingWith(final ThreadInfo ti, Predicate<ThreadInfo> predicate) {
    List<ThreadInfo> list = new ArrayList<ThreadInfo>();
    
    int n = 0;
    for (int i = 0, l = threads.length; i < l; i++) {
      ThreadInfo t = threads[i];
      if (predicate.isTrue(t) || (ti==t)) {
        list.add(t);
        n++;
      }
    }
    
    return list.toArray(new ThreadInfo[n]);
  }
  
  public ThreadInfo[] getAllMatchingWithout(final ThreadInfo ti, Predicate<ThreadInfo> predicate) {
    List<ThreadInfo> list = new ArrayList<ThreadInfo>();
    
    int n = 0;
    for (int i = 0, l = threads.length; i < l; i++) {
      ThreadInfo t = threads[i];
      if (predicate.isTrue(t) && (ti != t)) {
        list.add(t);
        n++;
      }
    }
    
    return list.toArray(new ThreadInfo[n]);
  }
  
  public int getMatchingCount(Predicate<ThreadInfo> predicate) {
    int n = 0;
    for (int i = 0, l = threads.length; i < l; i++) {
      ThreadInfo ti = threads[i];
      if (predicate.isTrue(ti)) {
        n++;
      }
    }
    
    return n;
  }
  
  public ThreadInfo getFirstMatching(Predicate<ThreadInfo> predicate) {
    for (int i = 0, l = threads.length; i < l; i++) {
      ThreadInfo t = threads[i];
      if (predicate.isTrue(t)) {
        return t;
      }
    }
    
    return null;
  }
  
  public Count getCountWithout (ThreadInfo tiExclude){
    int alive=0, runnableNonDaemons=0, runnableDaemons=0, blocked=0;
    
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
  
      if (ti != tiExclude){
        if (ti.isAlive()) {
          alive++;

          if (ti.isRunnable()) {
            if (ti.isDaemon()) {
              runnableDaemons++;
            } else {
              runnableNonDaemons++;
            }
          } else {
            blocked++;
          }
        }
      }
    }
    
    return new Count(alive, runnableNonDaemons, runnableDaemons, blocked);
  }

  public Count getCount(){
    return getCountWithout(null);
  }

  public void dump () {
    int i=0;
    for (ThreadInfo t : threads) {
      System.err.println("[" + i++ + "] " + t);
    }
  }

  @Override
  public Iterator<ThreadInfo> iterator() {
    return new Iterator<ThreadInfo>() {
      int i = 0;

      @Override
	public boolean hasNext() {
        return threads != null && threads.length>0 && i<threads.length;
      }

      @Override
	public ThreadInfo next() {
        if (threads != null && threads.length>0 && i<threads.length){
          return threads[i++];
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
	public void remove() {
        throw new UnsupportedOperationException("Iterator<ThreadInfo>.remove()");
      }
    };
  }

  
  class CanonicalLiveIterator implements Iterator<ThreadInfo> {
    
    int nextGid = -1;
    int nextIdx = -1;
    
    CanonicalLiveIterator(){
      setNext();
    }
    

    void setNext (){
      int lastGid = nextGid;
      int nextGid = Integer.MAX_VALUE;
      int nextIdx = -1;
      
      for (int i=0; i<threads.length; i++){
        ThreadInfo ti = threads[i];
        if (ti.isAlive()){
          int gid = ti.getGlobalId();
          if ((gid > lastGid) && (gid < nextGid)){
            nextGid = gid;
            nextIdx = i;
          }
        }
      }
      
      CanonicalLiveIterator.this.nextGid = nextGid;
      CanonicalLiveIterator.this.nextIdx = nextIdx;
    }
    
    @Override
	public boolean hasNext() {
      return (nextIdx >= 0);
    }

    @Override
	public ThreadInfo next() {
      if (nextIdx >= 0){
        ThreadInfo tiNext = threads[nextIdx];
        setNext();
        return tiNext;
        
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
	public void remove() {
      throw new UnsupportedOperationException("Iterator<ThreadInfo>.remove()");
    }
  }
  
  
  public Iterator<ThreadInfo> canonicalLiveIterator(){
    return new CanonicalLiveIterator();     
  }
  
  
  
  public void checkConsistency(boolean isStore) {
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      
      ti.checkConsistency(isStore);
    }
  }
}
