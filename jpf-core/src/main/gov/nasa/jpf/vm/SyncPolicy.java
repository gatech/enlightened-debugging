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


public interface SyncPolicy {


  public static String ROOT = "ROOT";
  public static String POST_FINALIZE = "POST_FINALIZE";
  

  public static String START = "START";
  public static String BLOCK = "BLOCK";
  public static String LOCK = "LOCK";
  public static String RELEASE = "RELEASE";
  public static String WAIT = "WAIT";
  public static String JOIN = "JOIN";
  public static String NOTIFY = "NOTIFY";
  public static String NOTIFYALL = "NOTIFYALL";
  public static String SLEEP = "SLEEP";
  public static String YIELD = "YIELD";
  public static String PRIORITY = "PRIORITY";
  public static String INTERRUPT = "INTERRUPT";
  public static String SUSPEND = "SUSPEND";
  public static String RESUME = "RESUME";
  public static String STOP = "STOP";
  public static String PARK = "PARK";
  public static String UNPARK = "UNPARK";
  public static String BEGIN_ATOMIC = "BEGIN_ATOMIC";
  public static String END_ATOMIC = "END_ATOMIC";
  public static String RESCHEDULE = "SCHEDULE";
  public static String TERMINATE = "TERMINATE";

  
  
  void initializeSyncPolicy (VM vm, ApplicationContext appCtx);
  
  
  void initializeThreadSync (ThreadInfo tiCurrent, ThreadInfo tiNew);
  
  
  void setRootCG ();
  

  boolean setsBlockedThreadCG (ThreadInfo ti, ElementInfo ei);
  boolean setsLockAcquisitionCG (ThreadInfo ti, ElementInfo ei);
  boolean setsLockReleaseCG (ThreadInfo ti, ElementInfo ei, boolean didUnblock);


  boolean setsTerminationCG (ThreadInfo ti);
  

  boolean setsWaitCG (ThreadInfo ti, long timeout);
  boolean setsNotifyCG (ThreadInfo ti, boolean didNotify);
  boolean setsNotifyAllCG (ThreadInfo ti, boolean didNotify);
    

  boolean setsStartCG (ThreadInfo tiCurrent, ThreadInfo tiStarted);
  boolean setsYieldCG (ThreadInfo ti);
  boolean setsPriorityCG (ThreadInfo ti);
  boolean setsSleepCG (ThreadInfo ti, long millis, int nanos);
  boolean setsSuspendCG (ThreadInfo tiCurrent, ThreadInfo tiSuspended);
  boolean setsResumeCG (ThreadInfo tiCurrent, ThreadInfo tiResumed);
  boolean setsJoinCG (ThreadInfo tiCurrent, ThreadInfo tiJoin, long timeout);
  boolean setsStopCG (ThreadInfo tiCurrent, ThreadInfo tiStopped);
  boolean setsInterruptCG (ThreadInfo tiCurrent, ThreadInfo tiInterrupted);
  

  boolean setsParkCG (ThreadInfo ti, boolean isAbsTime, long timeout);
  boolean setsUnparkCG (ThreadInfo tiCurrent, ThreadInfo tiUnparked);
  

  boolean setsBeginAtomicCG (ThreadInfo ti);
  boolean setsEndAtomicCG (ThreadInfo ti);
  

  boolean setsRescheduleCG (ThreadInfo ti, String reason);
  

  boolean setsPostFinalizeCG (ThreadInfo tiFinalizer);
}
