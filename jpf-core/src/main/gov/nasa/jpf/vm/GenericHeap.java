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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.ArrayObjectQueue;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjectQueue;
import gov.nasa.jpf.util.Processor;


public abstract class GenericHeap implements Heap, Iterable<ElementInfo> {
  
  static abstract class GenericHeapMemento implements Memento<Heap> {

    int attributes;
    IntVector pinDownList;
    Map<Integer,IntTable<String>> internStringsMap;
    
    protected GenericHeapMemento (GenericHeap heap){

      pinDownList = heap.pinDownList;
      internStringsMap = heap.internStringsMap;
      attributes = heap.attributes & ATTR_STORE_MASK;
      
      heap.setStored();
    }
    
    @Override
    public Heap restore (Heap inSitu) {
      GenericHeap heap = (GenericHeap) inSitu;
      heap.pinDownList = pinDownList;
      heap.internStringsMap = internStringsMap;
      heap.attributes = attributes;
      heap.liveBitValue = false; 
      return inSitu;
    }
  }
  
  
  protected class ElementInfoMarker implements Processor<ElementInfo>{
    @Override
	public void process (ElementInfo ei) {
      ei.markRecursive( GenericHeap.this); 
    }
  }
  
  protected VM vm;



  protected IntVector pinDownList;




  protected Map<Integer,IntTable<String>> internStringsMap;



  protected int attributes;

  static final int ATTR_GC            = 0x0001;
  static final int ATTR_OUT_OF_MEMORY = 0x0002;
  static final int ATTR_RUN_FINALIZER = 0x0004;

  static final int ATTR_ELEMENTS_CHANGED  = 0x10000;
  static final int ATTR_PINDOWN_CHANGED   = 0x20000;
  static final int ATTR_INTERN_CHANGED    = 0x40000;
  static final int ATTR_ATTRIBUTE_CHANGED = 0x80000;


  static final int ATTR_STORE_MASK = 0x0000ffff;
  static final int ATTR_ANY_CHANGED = (ATTR_ELEMENTS_CHANGED | ATTR_PINDOWN_CHANGED | ATTR_INTERN_CHANGED | ATTR_ATTRIBUTE_CHANGED);





  protected ArrayList<ElementInfo> weakRefs;

  protected ObjectQueue<ElementInfo> markQueue = new ArrayObjectQueue<ElementInfo>();


  protected boolean liveBitValue;
  
  protected ElementInfoMarker elementInfoMarker = new ElementInfoMarker();
  


  protected int nLiveObjects;
  


  public GenericHeap (Config config, KernelState ks){
    vm = VM.getVM();

    pinDownList = new IntVector(256);
    attributes |= ATTR_PINDOWN_CHANGED; 

    if (config.getBoolean("vm.finalize", true)){
      attributes |= ATTR_RUN_FINALIZER;
    }

    if (config.getBoolean("vm.sweep",true)){
      attributes |= ATTR_GC;
    }
  }


  protected DynamicElementInfo createElementInfo (int objref, ClassInfo ci, Fields f, Monitor m, ThreadInfo ti){
    return new DynamicElementInfo( objref,ci,f,m,ti);
  }
  

  protected void addToPinDownList (int objref){
    if ((attributes & ATTR_PINDOWN_CHANGED) == 0) {
      pinDownList = pinDownList.clone();
      attributes |= ATTR_PINDOWN_CHANGED;
    }
    pinDownList.add(objref);
  }
  
  protected void removeFromPinDownList (int objref){
    if ((attributes & ATTR_PINDOWN_CHANGED) == 0) {
      pinDownList = pinDownList.clone();
      attributes |= ATTR_PINDOWN_CHANGED;
    }
    pinDownList.removeFirst(objref);    
  }

  @Override
  public void registerPinDown(int objref){
    ElementInfo ei = getModifiable(objref);
    if (ei != null) {
      if (ei.incPinDown()){
        addToPinDownList(objref);
      }
    } else {
      throw new JPFException("pinDown reference not a live object: " + objref);
    }
  }

  @Override
  public void releasePinDown(int objref){
    ElementInfo ei = getModifiable(objref);
    if (ei != null) {
      if (ei.decPinDown()){
        removeFromPinDownList(objref);
      }
    } else {
      throw new JPFException("pinDown reference not a live object: " + objref);
    }
  }  
  void markPinDownList (){
    if (pinDownList != null){
      int len = pinDownList.size();
      for (int i=0; i<len; i++){
        int objref = pinDownList.get(i);
        queueMark(objref);
      }
    }
  }
  

  
  @Override
  public void registerWeakReference (ElementInfo ei) {
    if (weakRefs == null) {
      weakRefs = new ArrayList<ElementInfo>();
    }

    weakRefs.add(ei);
  }
  
  
  protected void cleanupWeakRefs () {
    if (weakRefs != null) {
      for (ElementInfo ei : weakRefs) {
        Fields f = ei.getFields();
        int    ref = f.getIntValue(0); 
        if (ref != MJIEnv.NULL) {
          ElementInfo refEi = get(ref);
          if ((refEi == null) || (refEi.isNull())) {
            ei = ei.getModifiableInstance();

            ei.setReferenceField(ei.getFieldInfo(0), MJIEnv.NULL);
          }
        }
      }

      weakRefs = null;
    }
  }
  


  
  protected abstract AllocationContext getSUTAllocationContext (ClassInfo ci, ThreadInfo ti);
  protected abstract AllocationContext getSystemAllocationContext (ClassInfo ci, ThreadInfo ti, int anchor);
  
  
  protected abstract int getNewElementInfoIndex (AllocationContext ctx);
  

    
  protected ElementInfo createObject (ClassInfo ci, ThreadInfo ti, int objref) {

    Fields f = ci.createInstanceFields();
    Monitor m = new Monitor();
    ElementInfo ei = createElementInfo( objref, ci, f, m, ti);
    
    set(objref, ei);

    attributes |= ATTR_ELEMENTS_CHANGED;


    ci.initializeInstanceData(ei, ti);

    vm.notifyObjectCreated(ti, ei);
    




    
    return ei;    
  }
    
  @Override
  public ElementInfo newObject(ClassInfo ci, ThreadInfo ti) {
    AllocationContext ctx = getSUTAllocationContext( ci, ti);
    int index = getNewElementInfoIndex( ctx);
    ElementInfo ei = createObject( ci, ti, index);

    return ei;
  }

  @Override
  public ElementInfo newSystemObject (ClassInfo ci, ThreadInfo ti, int anchor) {
    AllocationContext ctx = getSystemAllocationContext( ci, ti, anchor);
    int index = getNewElementInfoIndex( ctx);
    ElementInfo ei = createObject( ci, ti, index);
    return ei;
  }
  
  protected ElementInfo createArray (String elementType, int nElements, ClassInfo ci, ThreadInfo ti, int objref) {

    Fields f = ci.createArrayFields(ci.getName(), nElements, Types.getTypeSize(elementType), Types.isReference(elementType));
    Monitor m = new Monitor();
    DynamicElementInfo ei = createElementInfo( objref, ci, f, m, ti);

    set(objref, ei);

    attributes |= ATTR_ELEMENTS_CHANGED;

    vm.notifyObjectCreated(ti, ei);

    return ei;
  }
  
  protected ClassInfo getArrayClassInfo (ThreadInfo ti, String elementType) {
    String type = "[" + elementType;
    SystemClassLoaderInfo sysCl = ti.getSystemClassLoaderInfo();
    ClassInfo ciArray = sysCl.getResolvedClassInfo(type);

    if (!ciArray.isInitialized()) {

      ciArray.registerClass(ti);
      ciArray.setInitialized();
    }

    return ciArray;
  }
  
  @Override
  public ElementInfo newArray(String elementType, int nElements, ThreadInfo ti) {

    ClassInfo ci = getArrayClassInfo(ti, elementType);
    AllocationContext ctx = getSUTAllocationContext( ci, ti);
    
    int index = getNewElementInfoIndex( ctx);
    ElementInfo ei = createArray( elementType, nElements, ci, ti, index);

    return ei;
  }

  @Override
  public ElementInfo newSystemArray(String elementType, int nElements, ThreadInfo ti, int anchor) {

    ClassInfo ci = getArrayClassInfo(ti, elementType);
    AllocationContext ctx = getSystemAllocationContext( ci, ti, anchor);
    
    int index = getNewElementInfoIndex( ctx);
    ElementInfo ei = createArray( elementType, nElements, ci, ti, index);

    return ei;
  }

  
  
  protected ElementInfo initializeStringObject( String str, int index, int vref) {
    ElementInfo ei = getModifiable(index);
    ei.setReferenceField("value", vref);

    ElementInfo eVal = getModifiable(vref);
    CharArrayFields cf = (CharArrayFields)eVal.getFields();
    cf.setCharValues(str.toCharArray());
    
    return ei;
  }
  
  protected ElementInfo newString (ClassInfo ciString, ClassInfo ciChars, String str, ThreadInfo ti, AllocationContext ctx) {
    

    int sRef = getNewElementInfoIndex( ctx);
    createObject( ciString, ti, sRef);
    

    ctx = ctx.extend(ciChars, sRef);
    int vRef = getNewElementInfoIndex( ctx);
    createArray( "C", str.length(), ciChars, ti, vRef);
    
    ElementInfo ei = initializeStringObject(str, sRef, vRef);      
    return ei;
  }

  @Override
  public ElementInfo newString(String str, ThreadInfo ti){
    if (str != null) {
      SystemClassLoaderInfo sysCl = ti.getSystemClassLoaderInfo();
      ClassInfo ciString = sysCl.getStringClassInfo();
      ClassInfo ciChars = sysCl.getCharArrayClassInfo();
      
      AllocationContext ctx = getSUTAllocationContext( ciString, ti);
      return newString( ciString, ciChars, str, ti, ctx);
      
    } else {
      return null;
    }
  }
  
  @Override
  public ElementInfo newSystemString (String str, ThreadInfo ti, int anchor) {
    if (str != null) {
      SystemClassLoaderInfo sysCl = ti.getSystemClassLoaderInfo();
      ClassInfo ciString = sysCl.getStringClassInfo();
      ClassInfo ciChars = sysCl.getCharArrayClassInfo();
      
      AllocationContext ctx = getSystemAllocationContext( ciString, ti, anchor);
      return newString( ciString, ciChars, str, ti, ctx);
      
    } else {
      return null;
    }    
  }

  @Override
  public ElementInfo newInternString (String str, ThreadInfo ti) {
    if(internStringsMap==null) {
      internStringsMap = vm.getInitialInternStringsMap();
    }
    
    int prcId = ti.getApplicationContext().getId();
    IntTable.Entry<String> e = internStringsMap.get(prcId).get(str);
    
    if (e == null){
      if (str != null) {
        ElementInfo ei = newString( str, ti);
        int index = ei.getObjectRef();
        

        ei.incPinDown();
        addToPinDownList(index);
        addToInternStrings(str, index, prcId);

        return ei;
      
      } else {
        return null;
      }

    } else {
      return get(e.val);
    }
  }

  protected void addToInternStrings (String str, int objref, int prcId) {
    if ((attributes & ATTR_INTERN_CHANGED) == 0){

      internStringsMap = new HashMap<Integer,IntTable<String>>(internStringsMap);
      

      internStringsMap.put(prcId, internStringsMap.get(prcId).clone());
      

      attributes |= ATTR_INTERN_CHANGED;
    }
    internStringsMap.get(prcId).add(str, objref);
  }
  
  
  @Override
  public ElementInfo newSystemThrowable (ClassInfo ciThrowable, String details, int[] stackSnapshot, int causeRef,
                                 ThreadInfo ti, int anchor) {
    SystemClassLoaderInfo sysCl = ti.getSystemClassLoaderInfo(); 
    ClassInfo ciString = sysCl.getStringClassInfo();
    ClassInfo ciChars = sysCl.getCharArrayClassInfo();
    

    AllocationContext ctx = getSystemAllocationContext( ciThrowable, ti, anchor);
    int xRef = getNewElementInfoIndex( ctx);
    ElementInfo eiThrowable = createObject( ciThrowable, ti, xRef);
    

    if (details != null) {
      AllocationContext ctxString = ctx.extend( ciString, xRef);
      ElementInfo eiMsg = newString( ciString, ciChars, details, ti, ctxString);
      eiThrowable.setReferenceField("detailMessage", eiMsg.getObjectRef());
    }


    ClassInfo ciSnap = getArrayClassInfo(ti, "I");
    AllocationContext ctxSnap = ctx.extend(ciSnap, xRef);
    int snapRef = getNewElementInfoIndex( ctxSnap);
    ElementInfo eiSnap = createArray( "I", stackSnapshot.length, ciSnap, ti, snapRef);
    int[] snap = eiSnap.asIntArray();
    System.arraycopy( stackSnapshot, 0, snap, 0, stackSnapshot.length);
    eiThrowable.setReferenceField("snapshot", snapRef);


    eiThrowable.setReferenceField("cause", (causeRef != MJIEnv.NULL)? causeRef : xRef);

    return eiThrowable;
  }

  


  
  
    
  protected abstract void set (int index, ElementInfo ei);

  
  @Override
  public abstract ElementInfo get (int ref);
  
  
  
  @Override
  public abstract ElementInfo getModifiable (int ref);
  
  
  
  protected abstract void remove (int ref);

  

  
  
  @Override
  public abstract Iterator<ElementInfo> iterator();
  
  @Override
  public abstract Iterable<ElementInfo> liveObjects();
  
  

  
  public boolean isGcEnabled (){
    return (attributes & ATTR_GC) != 0;
  }

  public void setGcEnabled (boolean doGC) {
    if (doGC != isGcEnabled()) {
      if (doGC) {
        attributes |= ATTR_GC;
      } else {
        attributes &= ~ATTR_GC;
      }
      attributes |= ATTR_ATTRIBUTE_CHANGED;
    }
  }
  
  @Override
  public void unmarkAll(){
    for (ElementInfo ei : liveObjects()){
      ei.setUnmarked();
    }
  }
  
  
  @Override
  public void queueMark (int objref){
    if (objref == MJIEnv.NULL) {
      return;
    }

    ElementInfo ei = get(objref);
    if (!ei.isMarked()){ 
      ei.setMarked();
      markQueue.add(ei);
    }
  }
  
  
  @Override
  public void markStaticRoot (int objref) {
    if (objref != MJIEnv.NULL) {
      queueMark(objref);
    }
  }

  
  @Override
  public void markThreadRoot (int objref, int tid) {
    if (objref != MJIEnv.NULL) {
      queueMark(objref);
    }
  }
  
  
  protected void sweep () {
    ThreadInfo ti = vm.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();
    int n = 0;
    
    if(vm.finalizersEnabled()) {
      markFinalizableObjects();
    }
    

    for (ElementInfo ei : this){
      
      if (ei.isMarked()){ 
        ei.setUnmarked();
        ei.setAlive(liveBitValue);
        
        ei.cleanUp(this, isThreadTermination, tid);
        n++;
        
      } else {
        ei.processReleaseActions();
        
        vm.notifyObjectReleased(ti, ei);
        remove(ei.getObjectRef());
      }
    }
    
    nLiveObjects = n;
  }
  
  protected void markFinalizableObjects () {
    FinalizerThreadInfo tiFinalizer = vm.getFinalizerThread();
    
    if (tiFinalizer != null){
      for (ElementInfo ei : this) {
        if (!ei.isMarked() && ei.hasFinalizer() && !ei.isFinalized()) {
          ei = tiFinalizer.getFinalizerQueuedInstance(ei);
          ei.setMarked(); 
          ei.markRecursive(this);
        }
      }
    }
  }
  
  protected void mark () {
    markQueue.clear();
    

    markPinDownList();
    vm.getThreadList().markRoots(this);      
    vm.getClassLoaderList().markRoots(this); 


    markQueue.process(elementInfoMarker);    
  }
  
  @Override
  public void gc() {
    vm.notifyGCBegin();

    weakRefs = null;
    liveBitValue = !liveBitValue;

    mark();
    

    sweep();

    cleanupWeakRefs(); 

    vm.processPostGcActions();
    vm.notifyGCEnd();
  }

  
  @Override
  public void cleanUpDanglingReferences() {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();
    
    for (ElementInfo e : this) {
      if (e != null) {
        e.cleanUp(this, isThreadTermination, tid);
      }
    }
  }
  
  
  @Override
  public boolean isAlive (ElementInfo ei){
    return (ei == null || ei.isMarkedOrAlive(liveBitValue));
  }
  

  


  
  @Override
  public abstract void resetVolatiles();

  @Override
  public abstract void restoreVolatiles();
  
  @Override
  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }
  
  @Override
  public void markChanged(int objref) {
    attributes |= ATTR_ELEMENTS_CHANGED;
  }

  public void setStored() {
    attributes &= ~ATTR_ANY_CHANGED;
  }
  
  @Override
  public abstract Memento<Heap> getMemento(MementoFactory factory);

  @Override
  public abstract Memento<Heap> getMemento();

  

  
  @Override
  public boolean isOutOfMemory() {
    return (attributes & ATTR_OUT_OF_MEMORY) != 0;
  }

  @Override
  public void setOutOfMemory(boolean isOutOfMemory) {
    if (isOutOfMemory != isOutOfMemory()) {
      if (isOutOfMemory) {
        attributes |= ATTR_OUT_OF_MEMORY;
      } else {
        attributes &= ~ATTR_OUT_OF_MEMORY;
      }
      attributes |= ATTR_ATTRIBUTE_CHANGED;
    }
  }


  


  @Override
  public void checkConsistency(boolean isStateStore) {
    for (ElementInfo ei : this){
      ei.checkConsistency();
    }
  }
}
