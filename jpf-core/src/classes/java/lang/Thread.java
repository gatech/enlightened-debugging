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
import sun.nio.ch.Interruptible;


public class Thread implements Runnable {

  public interface UncaughtExceptionHandler {

    void uncaughtException (Thread t, Throwable x);
  }
  
  static int nameThreadNum; 

  public static final int MIN_PRIORITY = 1;
  public static final int NORM_PRIORITY = 5;
  public static final int MAX_PRIORITY = 10;


  private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler; 
  

  ThreadGroup group;
  Runnable target;
  String name;
  int priority;
  boolean isDaemon;
  



  boolean             interrupted;
  

  @NeverBreak
  ThreadLocal.Entry<?>[] threadLocals;
  


  static class Permit {
    boolean blockPark = true; 
  }
  Permit permit; 



  volatile Object parkBlocker;


  Throwable stopException;
  
  private volatile UncaughtExceptionHandler uncaughtExceptionHandler; 
  
  private ClassLoader contextClassLoader = null;

  
  public enum State { BLOCKED, NEW, RUNNABLE, TERMINATED, TIMED_WAITING, WAITING }

  
  public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler xh) {
    defaultUncaughtExceptionHandler = xh;
  }
  
  public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
    return defaultUncaughtExceptionHandler;
  }
  
  
  public Thread () {
    this(null, null, null, 0L);
  }

  public Thread (Runnable target) {
    this(null, target, null, 0L);
  }

  public Thread (Runnable target, String name) {
    this(null, target, name, 0L);
  }

  public Thread (String name) {
    this(null, null, name, 0L);
  }

  public Thread (ThreadGroup group, String name) {
    this(group, null, name, 0L);
  }
  
  public Thread (ThreadGroup group, Runnable target) {
    this(group, target, null, 0L);
  }

  public Thread (ThreadGroup group, Runnable target, String name) {
    this(group, target, name, 0L);
  }

  public Thread (ThreadGroup group, Runnable target, String name, long stackSize) {
    Thread cur = currentThread();

    if (group == null) {
      this.group = cur.getThreadGroup();
    } else {
      this.group = group;
    }

    this.group.add(this);

    if (name == null) {
      this.name = "Thread-" + ++nameThreadNum;
    } else {
      this.name = name;
    }

    this.permit = new Permit();


    this.priority = cur.getPriority();
    this.isDaemon = cur.isDaemon();

    this.target = target;


    init0(this.group, target, this.name, stackSize);
    
    initThreadLocals(cur);
  }


  native void init0 (ThreadGroup group, Runnable target, String name, long stackSize);
  




  private void initThreadLocals (Thread parent){
    ThreadLocal.Entry<?>[] tl = parent.threadLocals;
    if (tl != null){
      int len = tl.length;
      ThreadLocal.Entry<?>[] inherited = null;
      int j=0;
      
      for (int i=0; i<len; i++){
        ThreadLocal.Entry<?> e = tl[i];
        ThreadLocal.Entry<?> ec = e.getChildEntry();
        if (ec != null){
          if (inherited == null){
            inherited = new ThreadLocal.Entry<?>[len];
          }
          inherited[j++] = ec;
        }
      }
      
      if (inherited != null){
        ThreadLocal.Entry<?>[] a = new ThreadLocal.Entry<?>[j];
        System.arraycopy(inherited,0,a,0,j);
        threadLocals = a;
      }
    }
  }
  
  public static int activeCount () {
    return 0;
  }

  public void setUncaughtExceptionHandler(UncaughtExceptionHandler xh) {
    uncaughtExceptionHandler = xh;
  }
  
  public UncaughtExceptionHandler getUncaughtExceptionHandler(){
    if (uncaughtExceptionHandler != null){
      return uncaughtExceptionHandler;
    } else {
      return group;
    }
  }
  
  public void setContextClassLoader (ClassLoader cl) {
  	contextClassLoader = cl;
  }

  public ClassLoader getContextClassLoader () {
    if (contextClassLoader == null) {
    	return ClassLoader.getSystemClassLoader();
    }
    return contextClassLoader;
  }

  public synchronized void setDaemon (boolean isDaemon) {
    this.isDaemon = isDaemon;
    setDaemon0(isDaemon);
  }

  public boolean isDaemon () {
    return isDaemon;
  }

  public native long getId();

  public StackTraceElement[] getStackTrace() {
    return null; 
  }

  public native int getState0();

  public Thread.State getState() {
    int i = getState0();
    switch (i) {
    case 0: return State.BLOCKED;
    case 1: return State.NEW;
    case 2: return State.RUNNABLE;
    case 3: return State.TERMINATED;
    case 4: return State.TIMED_WAITING;
    case 5: return State.WAITING;
    }

    return null; 
  }

  public synchronized void setName (String name) {
    if (name == null) {
      throw new IllegalArgumentException("thread name can't be null");
    }

    this.name = name;
    setName0(name);
  }

  public String getName () {
    return name;
  }

  public void setPriority (int priority) {
    if ((priority < MIN_PRIORITY) || (priority > MAX_PRIORITY)) {
      throw new IllegalArgumentException("thread priority out of range");
    }

    this.priority = priority;
    setPriority0(priority);
  }

  public int getPriority () {
    return priority;
  }

  public ThreadGroup getThreadGroup () {
    return group;
  }

  public void checkAccess () {

  }

  public native int countStackFrames ();

  public static native Thread currentThread ();

  public void destroy () {
  }

  public static void dumpStack () {
  }

  public static int enumerate (Thread[] tarray) {
    Thread cur = currentThread();

    return cur.group.enumerate(tarray);
  }

  public static native boolean holdsLock (Object obj);


  public native void interrupt ();


  public static native boolean interrupted ();
  public native boolean isInterrupted ();

  public native boolean isAlive ();


  
  public void join () throws InterruptedException {
    synchronized(this){

      if (interrupted()) {
        throw new InterruptedException();
      }

      while (isAlive()) {


        wait();
      }
    }
  }

  public void join (long millis) throws InterruptedException {
    join(millis, 0);
  }

  public void join (long millis, int nanos) throws InterruptedException {

    if (millis < 0){
      throw new java.lang.IllegalArgumentException("timeout value is negative");

    } else if (millis == 0){
      join();

    } else {
      synchronized(this){
        if (interrupted()){
          throw new InterruptedException();
        }

        wait(millis);
      }
    }
  }

    

  @Override
  public void run () {
    if (target != null) {
      target.run();
    }
  }

  public static void sleep (long millis) throws InterruptedException {
    sleep(millis, 0);
  }

  public static native void sleep (long millis, int nanos)
                            throws InterruptedException;

  public native void start();
  public native void stop();
  public native void stop(Throwable obj);

  public native void suspend();
  public native void resume();


  @Override
  public String toString () {
    return ("Thread[" + name + ',' + priority + ',' + (group == null ? "" : group.getName()) + ']');
  }

  public static native void yield ();

  native void setDaemon0 (boolean on);

  native void setName0 (String name);

  native void setPriority0 (int priority);



  
  private void exit () {
    if (group != null){
      group.threadTerminated(this);
      group = null;
    }
    
    threadLocals = null;    
    parkBlocker = null;
    uncaughtExceptionHandler = null;
  }



  native void blockedOn (Interruptible b);

  


  long threadLocalRandomSeed;
  int threadLocalRandomProbe;
  int threadLocalRandomSecondarySeed;
}
