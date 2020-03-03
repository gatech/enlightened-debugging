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

import java.io.PrintStream;


public class ThreadGroup implements Thread.UncaughtExceptionHandler {
  private final ThreadGroup parent;
  

  
  String name;
  int maxPriority;
 
  int nthreads;
  Thread[] threads;
  
  int ngroups;
  ThreadGroup[] groups;
  
  int nUnstartedThreads;
  
  boolean destroyed;
  boolean daemon;
  
  boolean vmAllowSuspension;
  
  public ThreadGroup (String name){
    this( Thread.currentThread().getThreadGroup(), name);
  }
  
  public ThreadGroup (ThreadGroup parent, String name){
    this.parent = parent;
    this.name = name;
    

    this.maxPriority = parent.maxPriority;
    this.daemon = parent.daemon;
    
    parent.add(this);
  }
  

  


  
  private synchronized void add (ThreadGroup childGroup){
    if (destroyed){
      throw new IllegalThreadStateException();
    }
    
    if (groups == null){
      groups = new ThreadGroup[1];
    } else {
      ThreadGroup[] ng = new ThreadGroup[ngroups+1];
      System.arraycopy(groups, 0, ng, 0, ngroups);
      groups = ng;
    }
    
    groups[ngroups++] = childGroup;
  }
  
  private synchronized void remove (ThreadGroup childGroup){
    if (!destroyed){
      for (int i=0; i<ngroups; i++){
        if (groups[i] == childGroup){
          if (ngroups > 1){
            int ngroups1 = ngroups - 1;
            ThreadGroup[] ng = new ThreadGroup[ngroups1];
            if (i > 0) {
              System.arraycopy(groups, 0, ng, 0, i);
            }
            if (i < ngroups1) {
              System.arraycopy(groups, i + 1, ng, i, ngroups - i);
            }
            groups = ng;
            ngroups = ngroups1;
            
          } else {
            groups = null;
            ngroups = 0;
          }
          
          if (nthreads == 0){
            notifyAll();
          }
          

          if (daemon){
            if ((nthreads == 0) && (nUnstartedThreads == 0) && (ngroups == 0)){
              destroy();
            }
          }
          
          break;
        }
      }
    }
  }
  
  



  
  
  synchronized void addUnstarted (){
    if (destroyed){
      throw new IllegalThreadStateException();
    }

    nUnstartedThreads++;
  }
  
  
  synchronized void add (Thread newThread){
    if (destroyed){
      throw new IllegalThreadStateException();
    }

    if (threads == null){
      threads = new Thread[1];
    } else {
      Thread[] nt = new Thread[nthreads+1];
      System.arraycopy(threads, 0, nt, 0, nthreads);
      threads = nt;
    }
    
    threads[nthreads++] = newThread;
    nUnstartedThreads--;
  }
  
  
  synchronized void threadTerminated (Thread terminatedThread){
    if (!destroyed){
      for (int i=0; i<nthreads; i++){
        if (threads[i] == terminatedThread){
          if (nthreads == 1){
            threads = null;
            nthreads = 0;
            
          } else {
            int nthreads1 = nthreads - 1;
            Thread[] a = new Thread[nthreads1];
            if (i > 0) {
              System.arraycopy(threads, 0, a, 0, i);
            }
            if (i < nthreads1) {
              System.arraycopy(groups, i + 1, a, i, ngroups - i);
            }
            threads = a;
            nthreads = nthreads1;
          }



          if (nthreads == 0) {
            notifyAll();
          }
          if (daemon) {
            if ((nthreads == 0) && (nUnstartedThreads == 0) && (ngroups == 0)) {
              destroy();
            }
          }
          
          break;
        }
      }
    }
  }
  

  public final String getName(){
    return name;
  }
  
  public final ThreadGroup getParent(){
    return parent;
  }
  
  public final int getMaxPriority(){
    return maxPriority;
  }
  
  public final boolean isDaemon(){
    return daemon;
  }
  
  public synchronized boolean isDestroyed(){
    return destroyed;
  }
  
  public final void setDaemon (boolean daemon){
    this.daemon = daemon;
  }
  
  
  public final synchronized void setMaxPriority (int newMaxPriority){

    if (newMaxPriority >= Thread.MIN_PRIORITY && newMaxPriority <= Thread.MAX_PRIORITY){
      maxPriority = (parent != null) ? Math.min(parent.maxPriority, newMaxPriority) : newMaxPriority;
      
      for (int i=0; i<groups.length; i++){
        groups[i].setMaxPriority(newMaxPriority);
      }
    }
  }
  
  public final boolean parentOf (ThreadGroup tg){
    while (true){
      if (tg == this) return true;
      ThreadGroup tgParent = tg.parent;
      if (tgParent == null) return false;
      tg = tgParent;
    }
  }
  
  public final void checkAccess(){

  }
  
  
  public synchronized int activeCount() {
    if (destroyed){
      return 0;
      
    } else {
      int nActive = nthreads;
      if (ngroups > 0){
        for (int i=0; i<ngroups; i++){
          nActive += groups[i].activeCount();
        }
      }
      
      return nActive;
    }
  }
  
  
  public int enumerate (Thread[] dst){
    return enumerate(dst, 0, true);
  }
  
  public int enumerate (Thread[] dst, boolean recurse){
    return enumerate(dst, 0, recurse);
  }
  
  private synchronized int enumerate (Thread[] dst, int idx, boolean recurse){
    int n = 0;

    int len = nthreads;
    if ((idx + len) > dst.length){
      len = dst.length - idx;
    }
    
    for (int i = 0; i < len; i++) {
      if (threads[i].isAlive()) {
        dst[idx++] = threads[i];
        n++;
      }
    }

    if (recurse && (idx < dst.length)) {
      for (int j = 0; j < ngroups; j++) {
        n += groups[j].enumerate(dst, idx, true);
      }
    }
    
    return n;    
  }
  
  public synchronized int activeGroupCount() {
    if (destroyed){
      return 0;
      
    } else {
      int nActive = ngroups;
      if (ngroups > 0){
        for (int i=0; i<ngroups; i++){
          nActive += groups[i].activeGroupCount();
        }
      }
      
      return nActive;
    }
  }
  
  public int enumerate (ThreadGroup[] dst){
    return enumerate(dst, 0, true);
  }
  
  public int enumerate (ThreadGroup[] dst, boolean recurse){
    return enumerate(dst, 0, recurse);
  }
 
  private synchronized int enumerate (ThreadGroup[] dst, int idx, boolean recurse){
    int n = 0;

    int len = ngroups;
    if ((idx + len) > dst.length){
      len = dst.length - idx;
    }
    
    for (int i = 0; i < len; i++) {
        dst[idx++] = groups[i];
        n++;
    }

    if (recurse && (idx < dst.length)) {
      for (int j = 0; j < ngroups; j++) {
        n += groups[j].enumerate(dst, idx, true);
      }
    }
    
    return n;
  }

  static final int OP_STOP = 1;
  static final int OP_INTERRUPT = 2;
  static final int OP_SUSPEND = 3;
  static final int OP_RESUME = 4;
  
  public final void stop(){
    if (doRecursively(OP_STOP)){
      Thread.currentThread().stop();
    }
  }
  public final void interrupt(){
    doRecursively(OP_INTERRUPT);
  }
  public final void suspend(){
    if (doRecursively(OP_SUSPEND)){
      Thread.currentThread().suspend();      
    }
  }
  public final void resume(){
    doRecursively(OP_RESUME);
  }
  
  private synchronized boolean doRecursively (int op){
    boolean suicide = false;
    
    for (int i=0; i<nthreads; i++){
      Thread t = threads[i];
      switch (op){
        case OP_STOP:
          if (t == Thread.currentThread()) {
            suicide = true; 
          } else {
            t.stop();
          }
          break;
        case OP_INTERRUPT: t.interrupt(); break;
        case OP_SUSPEND:
          if (t == Thread.currentThread()) {
            suicide = true; 
          } else {
            t.suspend();
          }
          break;
        case OP_RESUME: t.resume(); break;
      }
    }
    
    for (int j=0; j<ngroups; j++){
      suicide = suicide || groups[j].doRecursively(op);
    }
    
    return suicide;
  }
  
  
  public synchronized final void destroy(){
    if (destroyed || (nthreads > 0)) {
      throw new IllegalThreadStateException();
    }

    for (int i=0; i<ngroups; i++){
      groups[i].destroy();
    }
    
    if (parent != null){ 
      nthreads = 0;
      threads = null;
      
      ngroups = 0;
      groups = null;
      
      destroyed = true;
      
      parent.remove(this);
    }
  }


  public void list(){
    list( System.out, 0);
  }
  synchronized void list (PrintStream ps, int indent){
    for (int i=0; i<indent; i++) ps.print(' ');
    ps.println(toString());
    
    indent+= 4;
    for (int j=0; j<nthreads; j++){
      for (int i=0; i<indent; i++) ps.print(' ');
      ps.println( threads[j]);
    }
    
    for (int k=0; k<ngroups; k++){
      groups[k].list( ps, indent);
    }
  }
  
  public boolean allowThreadSuspension (boolean allowSuspension){
    vmAllowSuspension = allowSuspension;
    return true;
  }
  
  @Override
  public String toString () {
    return getClass().getName() + "[name=" + name + ",maxpri=" + maxPriority + ']';
  }


  @Override
  public void uncaughtException (Thread t, Throwable x) {
    if (parent != null) { 
      parent.uncaughtException(t, x);
      
    } else { 
      Thread.UncaughtExceptionHandler xh = Thread.getDefaultUncaughtExceptionHandler();
      if (xh != null) {
        xh.uncaughtException(t, x);
        
      } else { 
        if (!(x instanceof ThreadDeath)) {
          System.err.print("Exception in thread \"" + t.getName() + '"');
          x.printStackTrace(System.err);
        }
      }
    }
  }
}
