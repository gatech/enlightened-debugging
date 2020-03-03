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
import gov.nasa.jpf.ConfigChangeListener;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.JPF_gov_nasa_jpf_vm_Verify;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Unsafe;


@SuppressWarnings("sunapi")
public class JPF_java_util_Random extends NativePeer {

  static class Delegatee extends Random {
    @Override
	public int next (int nBits){
      return super.next(nBits);
    }
  }
  

  static class ConfigListener implements ConfigChangeListener {
    JPF_java_util_Random nativeRandom;

    public ConfigListener(JPF_java_util_Random nativeRandom) {
      this.nativeRandom = nativeRandom;
    }

    @Override
    public void propertyChanged(Config config, String key, String oldValue, String newValue) {
      if ("cg.enumerate_random".equals(key)) {
        nativeRandom.setEnumerateRandom(config);
      }
    }
    
    @Override
    public void jpfRunTerminated(Config config){
      config.removeChangeListener(this);
    }
  }

  boolean enumerateRandom;
  

  boolean reproducibleRandom;
  long constantSeed;
  int[] defaultIntSet; 
  long[] defaultLongSet;
  double[] defaultDoubleSet;
  float[] defaultFloatSet;




  static Delegatee delegatee = new Delegatee();
  




  private static Unsafe unsafe;
  private static long seedFieldOffset;
  
  static {
    try {

      Field singletonField = Unsafe.class.getDeclaredField("theUnsafe");
      singletonField.setAccessible(true);
      unsafe = (Unsafe)singletonField.get(null);
      
      seedFieldOffset = unsafe.objectFieldOffset(Random.class.getDeclaredField("seed"));
    } catch (Exception ex) {
      throw new JPFException("cannot access java.util.Random internals: " + ex); 
    }
  }
  
  private static void setNativeSeed (Random rand, long seed) {
    AtomicLong al = (AtomicLong) unsafe.getObject(rand, seedFieldOffset);
    al.set(seed);
  }

  private static long getNativeSeed (Random rand){
    AtomicLong al = (AtomicLong) unsafe.getObject(rand, seedFieldOffset);
    return al.longValue();
  }

  public JPF_java_util_Random (Config conf) {
    setEnumerateRandom(conf);
    conf.addChangeListener(new ConfigListener(this));
    
    reproducibleRandom = conf.getBoolean("vm.reproducible_random", true);
    constantSeed = conf.getLong("vm.random_seed", 42);
    defaultIntSet = conf.getIntArray("vm.random_ints", Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
    defaultDoubleSet = conf.getDoubleArray("vm.random_doubles", Double.MIN_VALUE, 0, Double.MAX_VALUE);  
    defaultLongSet = conf.getLongArray("vm.random_longs", Long.MIN_VALUE, 0, Long.MAX_VALUE);  
    defaultFloatSet = conf.getFloatArray("vm.random_floats", Float.MIN_VALUE, 0, Float.MAX_VALUE);  
  }

  void setEnumerateRandom (Config conf) {
    enumerateRandom = conf.getBoolean("cg.enumerate_random", false);

    if (enumerateRandom){
      JPF_gov_nasa_jpf_vm_Verify.init(conf);
    }    
  }
  
  long computeDefaultSeed(){
    Random rand = (reproducibleRandom) ? new Random(constantSeed) : new Random();
    return getNativeSeed( rand);
  }

  static void storeSeed (MJIEnv env, int objRef, long seed){
    env.setLongField(objRef, "seed", seed);
  }

  static long getSeed (MJIEnv env, int objRef){
    return env.getLongField(objRef, "seed");
  }
  
  static void restoreRandomState (MJIEnv env, int objRef, Random rand){
    long seed = getSeed( env, objRef);
    setNativeSeed( rand, seed);
  }
  
  static void storeRandomState (MJIEnv env, int objRef, Random rand){
    long seed = getNativeSeed( rand);
    storeSeed( env, objRef, seed);
  }
  
  


  @MJI
  public void $init____V (MJIEnv env, int objRef){
    long seed = computeDefaultSeed();
    storeSeed( env, objRef, seed);
  }
  
  @MJI
  public void $init__J__V (MJIEnv env, int objRef, long seedStarter){


    Random rand = new Random(seedStarter);
    storeRandomState(env, objRef, rand);    
  }
  
  @MJI
  public void setSeed__J__V (MJIEnv env, int objRef, long seedStarter){

    restoreRandomState( env, objRef, delegatee);
    delegatee.setSeed(seedStarter); 
    storeRandomState(env, objRef, delegatee);    
  }
  
  @MJI
  public boolean nextBoolean____Z (MJIEnv env, int objRef){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_vm_Verify.getBoolean____Z(env,-1);

    } else {
      restoreRandomState(env, objRef, delegatee);
      boolean ret = delegatee.nextBoolean();
      storeRandomState(env, objRef, delegatee);
      return ret;
    }
  }
  
  @MJI
  public int nextInt__I__I (MJIEnv env, int objRef, int n){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_vm_Verify.getInt__II__I(env,-1,0,n-1);
      
    } else {
      restoreRandomState(env, objRef, delegatee);
      int ret = delegatee.nextInt(n);
      storeRandomState(env, objRef, delegatee);
      return ret;
    }
  }
  
  @MJI
  public int nextInt____I (MJIEnv env, int objRef){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_vm_Verify.getIntFromList(env, defaultIntSet);
      
    } else {
      restoreRandomState(env, objRef, delegatee);
      int ret = delegatee.nextInt();
      storeRandomState(env, objRef, delegatee);
      return ret;
    }
  }
  
  @MJI
  public int next__I__I (MJIEnv env, int objRef, int nBits){
    if (enumerateRandom){

      return JPF_gov_nasa_jpf_vm_Verify.getIntFromList(env, defaultIntSet);
      
    } else {
      restoreRandomState(env, objRef, delegatee);
      int ret = delegatee.next( nBits);
      storeRandomState(env, objRef, delegatee);
      return ret;
    }
  }
  
  @MJI
  public void nextBytes___3B__V (MJIEnv env, int objRef, int dataRef){


    
    int n = env.getArrayLength(dataRef);
    byte[] data = new byte[n];

    restoreRandomState(env, objRef, delegatee);
    delegatee.nextBytes(data);
    storeRandomState(env, objRef, delegatee);

    for (int i = 0; i < n; i++) {
      env.setByteArrayElement(dataRef, i, data[i]);
    }
  }
  
  @MJI
  public long nextLong____J (MJIEnv env, int objRef){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_vm_Verify.getLongFromList(env, defaultLongSet);
      
    } else {
      restoreRandomState(env, objRef, delegatee);
      long ret = delegatee.nextLong();
      storeRandomState(env, objRef, delegatee);
      return ret;
    }    
  }

  @MJI
  public float nextFloat____F (MJIEnv env, int objRef){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_vm_Verify.getFloatFromList(env, defaultFloatSet);
      
    } else {
      restoreRandomState(env, objRef, delegatee);
      float ret = delegatee.nextFloat();
      storeRandomState(env, objRef, delegatee);
      return ret;
    }    
  }

  @MJI
  public double nextDouble____D (MJIEnv env, int objRef){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_vm_Verify.getDoubleFromList(env, defaultDoubleSet);
      
    } else {
      restoreRandomState(env, objRef, delegatee);
      double ret = delegatee.nextDouble();
      storeRandomState(env, objRef, delegatee);
      return ret;
    }    
  }

  @MJI
  public double nextGaussian____D (MJIEnv env, int objRef){


    restoreRandomState(env, objRef, delegatee);
    double ret = delegatee.nextGaussian();
    storeRandomState(env, objRef, delegatee);
    return ret;
  }
}
