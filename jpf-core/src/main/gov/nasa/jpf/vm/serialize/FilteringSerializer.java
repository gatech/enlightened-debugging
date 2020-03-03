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


package gov.nasa.jpf.vm.serialize;


import gov.nasa.jpf.util.ArrayObjectQueue;
import gov.nasa.jpf.util.BitArray;
import gov.nasa.jpf.util.FinalBitSet;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.ObjectQueue;
import gov.nasa.jpf.util.Processor;
import gov.nasa.jpf.vm.AbstractSerializer;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Fields;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.NativeStateHolder;
import gov.nasa.jpf.vm.ReferenceProcessor;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.Statics;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadList;

import java.util.HashMap;
import java.util.List;



public class FilteringSerializer extends AbstractSerializer implements ReferenceProcessor, Processor<ElementInfo> {


  final ObjVector<FramePolicy> methodCache = new ObjVector<FramePolicy>();


  final HashMap<ClassInfo,FinalBitSet> instanceRefMasks = new HashMap<ClassInfo,FinalBitSet>();
  final HashMap<ClassInfo,FinalBitSet> staticRefMasks   = new HashMap<ClassInfo,FinalBitSet>();

  final HashMap<ClassInfo,FinalBitSet> instanceFilterMasks = new HashMap<ClassInfo,FinalBitSet>();
  final HashMap<ClassInfo,FinalBitSet> staticFilterMasks   = new HashMap<ClassInfo,FinalBitSet>();

  protected FilterConfiguration filter;

  protected transient IntVector buf = new IntVector(4096);


  protected ObjectQueue<ElementInfo> refQueue;
  
  Heap heap;


  @Override
  public void attach(VM vm) {
    super.attach(vm);
    
    filter = vm.getConfig().getInstance("filter.class", FilterConfiguration.class);
    if (filter == null) {
      filter = new DefaultFilterConfiguration();
    }
    filter.init(vm.getConfig());
  }

  protected FramePolicy getFramePolicy(MethodInfo mi) {
    FramePolicy p = null;

    int mid = mi.getGlobalId();
    if (mid >= 0){
      p = methodCache.get(mid);
    if (p == null) {
      p = filter.getFramePolicy(mi);
      methodCache.set(mid, p);
    }
    } else {
      p = filter.getFramePolicy(mi);
    }

    return p;
  }

  protected FinalBitSet getInstanceRefMask(ClassInfo ci) {
    FinalBitSet v = instanceRefMasks.get(ci);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceRefMasks.put(ci, v);
    }
    return v;
  }

  protected FinalBitSet getStaticRefMask(ClassInfo ci) {
    FinalBitSet v = staticRefMasks.get(ci);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticRefMasks.put(ci, v);
    }
    return v;
  }

  protected FinalBitSet getInstanceFilterMask(ClassInfo ci) {
    FinalBitSet v = instanceFilterMasks.get(ci);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      b.setAll();
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        int start = fi.getStorageOffset();
        int end = start + fi.getStorageSize();
        for (int i = start; i < end; i++) {
          b.clear(i);
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceFilterMasks.put(ci, v);
    }
    return v;
  }

  protected FinalBitSet getStaticFilterMask(ClassInfo ci) {
    FinalBitSet v = staticFilterMasks.get(ci);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      b.setAll();
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        int start = fi.getStorageOffset();
        int end = start + fi.getStorageSize();
        for (int i = start; i < end; i++) {
          b.clear(i);
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticFilterMasks.put(ci, v);
    }
    return v;
  }

  protected void initReferenceQueue() {



    if (refQueue == null){
      refQueue = new ArrayObjectQueue<ElementInfo>();
    } else {
      refQueue.clear();
    }
  }





  @Override
  public void processReference(int objref) {
    if (objref != MJIEnv.NULL) {
      ElementInfo ei = heap.get(objref);
      if (!ei.isMarked()) { 
        ei.setMarked();
        refQueue.add(ei);
      }
    }

    buf.add(objref);
  }

  
  protected void processArrayFields (ArrayFields afields){
    buf.add(afields.arrayLength());

    if (afields.isReferenceArray()) {
      int[] values = afields.asReferenceArray();
      for (int i = 0; i < values.length; i++) {
        processReference(values[i]);
      }
    } else {
      afields.appendTo(buf);
    }
  }
    
  protected void processNamedFields (ClassInfo ci, Fields fields){
    FinalBitSet filtered = getInstanceFilterMask(ci);
    FinalBitSet refs = getInstanceRefMask(ci);





    int[] values = fields.asFieldSlots();
    for (int i = 0; i < values.length; i++) {
      if (!filtered.get(i)) {
        int v = values[i];
        if (refs.get(i)) {
          processReference(v);
        } else {
          buf.add(v);
        }
      }
    }
  }







  @Override
  public void process (ElementInfo ei) {
    Fields fields = ei.getFields();
    ClassInfo ci = ei.getClassInfo();
    buf.add(ci.getUniqueId());

    if (fields instanceof ArrayFields) { 
      processArrayFields((ArrayFields)fields);

    } else { 
      processNamedFields(ci, fields);
    }
  }
  
  protected void processReferenceQueue () {
    refQueue.process(this);
    



    heap.unmarkAll();
  }

  protected void serializeStackFrames() {
    ThreadList tl = ks.getThreadList();

    for (ThreadInfo ti : tl) {
      if (ti.isAlive()) {
        serializeStackFrames(ti);
      }
    }
  }

  protected void serializeStackFrames(ThreadInfo ti){

    processReference( ti.getThreadObjectRef());
    
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame = frame.getPrevious()){
      serializeFrame(frame);
    }
  }

  

  protected void serializeFrame(StackFrame frame){
    buf.add(frame.getMethodInfo().getGlobalId());



    Instruction pc = frame.getPC();
    if (pc != null){
      buf.add(pc.getInstructionIndex());
    } else {
      buf.add(-1);
    }

    int len = frame.getTopPos()+1;
    buf.add(len);

    int[] slots = frame.getSlots();
    buf.append(slots,0,len);

    frame.visitReferenceSlots(this);
  }



  protected void serializeThreadState (ThreadInfo ti){
    
    buf.add( ti.getId());
    buf.add( ti.getState().ordinal());
    buf.add( ti.getStackDepth());
    




    

    ElementInfo eiLock = ti.getLockObject();
    if (eiLock != null){
      buf.add(getSerializedReferenceValue( eiLock));
    }
    



    serializeLockedObjects( ti.getLockedObjects());
  }


  protected int getSerializedReferenceValue (ElementInfo ei){
    return ei.getObjectRef();
  }
  
  protected void serializeLockedObjects(List<ElementInfo> lockedObjects){










    int n = lockedObjects.size();
    buf.add(n);
    
    if (n > 0){
      if (n == 1){ 
        buf.add( getSerializedReferenceValue( lockedObjects.get(0)));
        
      } else {

        int h = (n << 16) + (n % 3);
        for (int i=0; i<n; i++){
          int rot = (getSerializedReferenceValue( lockedObjects.get(i))) % 31;
          h ^= (h << rot) | (h >>> (32 - rot)); 
        }        
        buf.add( h);
      }
    }
  }
  
  protected void serializeThreadStates (){
    ThreadList tl = ks.getThreadList();

    for (ThreadInfo ti : tl) {
      if (ti.isAlive()) {
        serializeThreadState(ti);
      }
    }    
  }
  
  protected void serializeClassLoaders(){
    buf.add(ks.classLoaders.size());

    for (ClassLoaderInfo cl : ks.classLoaders) {
      if(cl.isAlive()) {
        serializeStatics(cl.getStatics());
      }
    }
  }

  protected void serializeStatics(Statics statics){
    buf.add(statics.size());

    for (StaticElementInfo sei : statics.liveStatics()) {
      serializeClass(sei);
    }
  }

  protected void serializeClass (StaticElementInfo sei){
    buf.add(sei.getStatus());

    Fields fields = sei.getFields();
    ClassInfo ci = sei.getClassInfo();
    FinalBitSet filtered = getStaticFilterMask(ci);
    FinalBitSet refs = getStaticRefMask(ci);
    int max = ci.getStaticDataSize();
    for (int i = 0; i < max; i++) {
      if (!filtered.get(i)) {
        int v = fields.getIntValue(i);
        if (refs.get(i)) {
          processReference(v);
        } else {
          buf.add(v);
        }
      }
    }
  }
  
  protected void serializeNativeStateHolders(){
    for (NativeStateHolder nsh : nativeStateHolders){
      serializeNativeStateHolder(nsh);
    }
  }
  
  protected void serializeNativeStateHolder (NativeStateHolder nsh){
    buf.add(nsh.getHash());
  }
  


  @Override
  protected int[] computeStoringData() {

    buf.clear();
    heap = ks.getHeap();
    initReferenceQueue();


    serializeStackFrames();
    serializeClassLoaders();
    processReferenceQueue();
    




    serializeThreadStates();


    serializeNativeStateHolders();
    
    return buf.toArray();
  }

  protected void dumpData() {
    int n = buf.size();
    System.out.print("serialized data: [");
    for (int i=0; i<n; i++) {
      if (i>0) {
        System.out.print(',');
      }
      System.out.print(buf.get(i));
    }
    System.out.println(']');
  }
}
