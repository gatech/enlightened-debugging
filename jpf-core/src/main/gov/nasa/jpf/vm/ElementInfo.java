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
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.bytecode.JVMArrayElementInstruction;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.ObjectList;
import gov.nasa.jpf.util.Processor;

import java.io.PrintWriter;

import anonymous.domain.enlighten.deptrack.DynDepBuilder;
import anonymous.domain.enlighten.deptrack.DynamicDependency;


public abstract class ElementInfo implements Cloneable {







  public static final int   ATTR_PINDOWN_MASK = 0xff;






  public static final int   ATTR_IS_FROZEN     = 0x100;


  public static final int   ATTR_IMMUTABLE     = 0x200;




  public static final int   ATTR_CONSTRUCTED   = 0x400;


  public static final int ATTR_FINALIZED = 0x800;
  
  public static final int   ATTR_EXPOSED       = 0x1000;
  

  public static final int   ATTR_SHARED        = 0x4000;
  

  public static final int   ATTR_FREEZE_SHARED = 0x8000; 
  
  





  public static final int   ATTR_TREF_CHANGED       = 0x10000; 
  public static final int   ATTR_FLI_CHANGED        = 0x20000; 
  public static final int   ATTR_ATTRIBUTE_CHANGED  = 0x80000; 

  


  static final int   ATTR_STORE_MASK = 0x0000ffff;

  static final int   ATTR_ANY_CHANGED = (ATTR_TREF_CHANGED | ATTR_FLI_CHANGED | ATTR_ATTRIBUTE_CHANGED);


  public static final int   ATTR_IS_MARKED   = 0x80000000;
  




  public static final int   ATTR_LIVE_BIT    = 0x40000000;
  
  public static final int   ATTR_MARKED_OR_LIVE_BIT = (ATTR_IS_MARKED | ATTR_LIVE_BIT);




  protected ClassInfo       ci;
  protected Fields          fields;
  protected Monitor         monitor;
  


  protected ThreadInfoSet referencingThreads;




  protected FieldLockInfo[] fLockInfo;

  



  protected int objRef;









  protected int attributes;






  protected Memento<ElementInfo> cachedMemento;



  protected int sid;




  
  static class Restorer implements Processor<ElementInfo> {
    @Override
    public void process (ElementInfo ei) {
      ei.attributes &= ElementInfo.ATTR_STORE_MASK;
      ei.sid = 0;
      ei.updateLockingInfo();
      ei.markUnchanged();
    }        
  }
  static Restorer restorer = new Restorer();
  
  static class Storer implements Processor<ElementInfo> {
    @Override
    public void process (ElementInfo ei) {
      ei.freeze();
    }
  }
  static Storer storer = new Storer();
  
  
  static boolean init (Config config) {
    return true;
  }

  protected ElementInfo (int id, ClassInfo c, Fields f, Monitor m, ThreadInfo ti) {
    objRef = id;
    ci = c;
    fields = f;
    monitor = m;

    assert ti != null; 
  }

  public abstract ElementInfo getModifiableInstance();
    

  public abstract boolean isObject();

  public abstract boolean hasFinalizer();
  
  protected ElementInfo() {
  }

  public boolean hasChanged() {
    return !isFrozen();

  }

  @Override
  public String toString() {
    return ((ci != null ? ci.getName() : "ElementInfo") + '@' + Integer.toHexString(objRef));
  }

  public FieldLockInfo getFieldLockInfo (FieldInfo fi) {
    if (fLockInfo != null){
      return fLockInfo[fi.getFieldIndex()];
    } else {
      return null;
    }
  }

  public void setFieldLockInfo (FieldInfo fi, FieldLockInfo flInfo) {
    checkIsModifiable();

    if (fLockInfo == null){
      fLockInfo = new FieldLockInfo[getNumberOfFields()];
    }
    
    fLockInfo[fi.getFieldIndex()] = flInfo;
    attributes |= ATTR_FLI_CHANGED;
  }
  
  public boolean isLockProtected (FieldInfo fi){
    if (fLockInfo != null){
      FieldLockInfo fli = fLockInfo[fi.getFieldIndex()];
      if (fli != null){
        return fli.isProtected();
      }
    }
    
    return false;
  }

  
  public void processReleaseActions(){

    ci.processReleaseActions(this);
    

    if (fields.hasObjectAttr()){
      for (ReleaseAction action : fields.objectAttrIterator(ReleaseAction.class)){
        action.release(this);
      }
    }
  }

  
  void cleanUp (Heap heap, boolean isThreadTermination, int tid) {
    if (fLockInfo != null) {
      for (int i=0; i<fLockInfo.length; i++) {
        FieldLockInfo fli = fLockInfo[i];
        if (fli != null) {
          fLockInfo[i] = fli.cleanUp(heap);
        }
      }
    }
  }
  
  

  public void setSid(int id){
    sid = id;
  }

  public int getSid() {
    return sid;
  }



  public Memento<ElementInfo> getCachedMemento(){
    return cachedMemento;
  }

  public void setCachedMemento (Memento<ElementInfo> memento){
    cachedMemento = memento;
  }

  
  public boolean hasRefField (int objRef) {
    return ci.hasRefField( objRef, fields);
  }


  public int numberOfUserThreads() {
    return referencingThreads.size();
  }


  
  void markRecursive(Heap heap) {
    int i, n;

    if (isArray()) {
      if (fields.isReferenceArray()) {
        n = ((ArrayFields)fields).arrayLength();
        for (i = 0; i < n; i++) {
          int objref = fields.getReferenceValue(i);
          if (objref != MJIEnv.NULL){
            heap.queueMark( objref);
          }
        }
      }

    } else { 
      ClassInfo ci = getClassInfo();
      boolean isWeakRef = ci.isWeakReference();

      do {
        n = ci.getNumberOfDeclaredInstanceFields();
        boolean isRef = isWeakRef && ci.isReferenceClassInfo(); 

        for (i = 0; i < n; i++) {
          FieldInfo fi = ci.getDeclaredInstanceField(i);
          if (fi.isReference()) {
            if ((i == 0) && isRef) {



              heap.registerWeakReference(this);
            } else {
              int objref = fields.getReferenceValue(fi.getStorageOffset());
              if (objref != MJIEnv.NULL){
                heap.queueMark( objref);
              }
            }
          }
        }
        ci = ci.getSuperClass();
      } while (ci != null);
    }
  }


  int getAttributes () {
    return attributes;
  }

  int getStoredAttributes() {
    return attributes & ATTR_STORE_MASK;
  }

  public boolean isImmutable() {
    return ((attributes & ATTR_IMMUTABLE) != 0);
  }


  
  public void freeze() {
    attributes |= ATTR_IS_FROZEN;
  }

  public void defreeze() {
    attributes &= ~ATTR_IS_FROZEN;
  }
  
  public boolean isFrozen() {
    return ((attributes & ATTR_IS_FROZEN) != 0);    
  }
  


  
  public void setReferencingThreads (ThreadInfoSet refThreads){
    checkIsModifiable();    
    referencingThreads = refThreads;
  }
  
  public ThreadInfoSet getReferencingThreads (){
    return referencingThreads;
  }
  
  public void freezeSharedness (boolean freeze) {
    if (freeze) {
      if ((attributes & ATTR_FREEZE_SHARED) == 0) {
        checkIsModifiable();
        attributes |= (ATTR_FREEZE_SHARED | ATTR_ATTRIBUTE_CHANGED);
      }
    } else {
      if ((attributes & ATTR_FREEZE_SHARED) != 0) {
        checkIsModifiable();
        attributes &= ~ATTR_FREEZE_SHARED;
        attributes |= ATTR_ATTRIBUTE_CHANGED;
      }
    }
  }
  
  public boolean isSharednessFrozen () {
    return (attributes & ATTR_FREEZE_SHARED) != 0;
  }
  
  public boolean isShared() {

    return ((attributes & ATTR_SHARED) != 0);
  }
  
  public void setShared (ThreadInfo ti, boolean isShared) {
    if (isShared) {
      if ((attributes & ATTR_SHARED) == 0) {
        checkIsModifiable();
        attributes |= (ATTR_SHARED | ATTR_ATTRIBUTE_CHANGED);
        

        VM.getVM().notifyObjectShared(ti, this);
      }
    } else {
      if ((attributes & ATTR_SHARED) != 0) {
        checkIsModifiable();
        attributes &= ~ATTR_SHARED;
        attributes |= ATTR_ATTRIBUTE_CHANGED;
      }
    }
  }
  
  
  void setSharednessFromReferencingThreads () {
    if (referencingThreads.isShared( null, this)) {
      if ((attributes & ATTR_SHARED) == 0) {
        checkIsModifiable();
        attributes |= (ATTR_SHARED | ATTR_ATTRIBUTE_CHANGED);
      }
    }
  }
  
  public boolean isReferencedBySameThreads (ElementInfo eiOther) {
    return referencingThreads.equals(eiOther.referencingThreads);
  }
  
  public boolean isReferencedByThread (ThreadInfo ti) {
    return referencingThreads.contains(ti);
  }
  
  public boolean isExposed(){
    return (attributes & ATTR_EXPOSED) != 0;
  }
  
  public boolean isExposedOrShared(){
    return (attributes & (ATTR_SHARED | ATTR_EXPOSED)) != 0;
  }
  
  public ElementInfo getExposedInstance (ThreadInfo ti, ElementInfo eiFieldOwner){
    ElementInfo ei = getModifiableInstance();
    ei.setExposed( ti, eiFieldOwner);
    


    
    return ei;
  }
  
  protected void setExposed (){
    attributes |= (ATTR_EXPOSED | ATTR_ATTRIBUTE_CHANGED);
  }
  
  public void setExposed (ThreadInfo ti, ElementInfo eiFieldOwner){


    attributes |= (ATTR_EXPOSED | ATTR_ATTRIBUTE_CHANGED);
    
    ti.getVM().notifyObjectExposed(ti, eiFieldOwner, this);
  }
  
  
  protected boolean recycle () {  



    if (hasVolatileFieldLockInfos()) {
      return false;
    }

    setObjectRef(MJIEnv.NULL);

    return true;
  }

  boolean hasVolatileFieldLockInfos() {
    if (fLockInfo != null) {
      for (int i=0; i<fLockInfo.length; i++) {
        FieldLockInfo fli = fLockInfo[i];
        if (fli != null) {
          if (fli.needsPindown(this)) {
            return true;
          }
        }
      }
    }

    return false;
  }
  
  public void hash(HashData hd) {
    hd.add(ci.getClassLoaderInfo().getId());
    hd.add(ci.getId());
    fields.hash(hd);
    monitor.hash(hd);
    hd.add(attributes & ATTR_STORE_MASK);
  }

  @Override
  public int hashCode() {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }

  @Override
  public boolean equals(Object o) {
    if (o != null && o instanceof ElementInfo) {
      ElementInfo other = (ElementInfo) o;

      if (ci != other.ci){
        return false;
      }

      if ((attributes & ATTR_STORE_MASK) != (other.attributes & ATTR_STORE_MASK)){
        return false;
      }
      if (!fields.equals(other.fields)) {
        return false;
      }
      if (!monitor.equals(other.monitor)){
        return false;
      }
      if (referencingThreads != other.referencingThreads){
        return false;
      }

      return true;

    } else {
      return false;
    }
  }

  public ClassInfo getClassInfo() {
    return ci;
  }

  abstract protected FieldInfo getDeclaredFieldInfo(String clsBase, String fname);

  abstract protected FieldInfo getFieldInfo(String fname);

  protected abstract int getNumberOfFieldsOrElements();

  


  public boolean hasObjectAttr(){
    return fields.hasObjectAttr();
  }
  
  public boolean hasObjectAttr(Class<?> attrType) {
    return fields.hasObjectAttr(attrType);
  }

  
  public Object getObjectAttr(){
    return fields.getObjectAttr();
  }
  
  
  public void setObjectAttr (Object a){
    checkIsModifiable();
    fields.setObjectAttr(a);
  }

  
  public void setObjectAttrNoClone (Object a){
    fields.setObjectAttr(a);
  }

  
  public void addObjectAttr(Object a){
    checkIsModifiable();
    fields.addObjectAttr(a);
  }
  public void removeObjectAttr(Object a){
    checkIsModifiable();
    fields.removeObjectAttr(a);
  }
  public void replaceObjectAttr(Object oldAttr, Object newAttr){
    checkIsModifiable();
    fields.replaceObjectAttr(oldAttr, newAttr);
  }

  
  
  public <T> T getObjectAttr (Class<T> attrType) {
    return fields.getObjectAttr(attrType);
  }
  public <T> T getNextObjectAttr (Class<T> attrType, Object prev) {
    return fields.getNextObjectAttr(attrType, prev);
  }
  public ObjectList.Iterator objectAttrIterator(){
    return fields.objectAttrIterator();
  }
  public <T> ObjectList.TypedIterator<T> objectAttrIterator(Class<T> type){
    return fields.objectAttrIterator(type);
  }
  

  
  public boolean hasFieldAttr() {
    return fields.hasFieldAttr();
  }

  public boolean hasFieldAttr (Class<?> attrType){
    return fields.hasFieldAttr(attrType);
  }

  
  public Object getFieldAttr (FieldInfo fi){
    return fields.getFieldAttr(fi.getFieldIndex());
  }

  
  public void setFieldAttr (FieldInfo fi, Object attr){
    checkIsModifiable();
    
    int nFields = getNumberOfFieldsOrElements();
    fields.setFieldAttr( nFields, fi.getFieldIndex(), attr);    
  }

  
  public void addFieldAttr (FieldInfo fi, Object a){
    checkIsModifiable();
    
    int nFields = getNumberOfFieldsOrElements();    
    fields.addFieldAttr( nFields, fi.getFieldIndex(), a);
  }
  public void removeFieldAttr (FieldInfo fi, Object a){
    checkIsModifiable();
    fields.removeFieldAttr(fi.getFieldIndex(), a);
  }
  public void replaceFieldAttr (FieldInfo fi, Object oldAttr, Object newAttr){
    checkIsModifiable();    
    fields.replaceFieldAttr(fi.getFieldIndex(), oldAttr, newAttr);
  }
  
  
  public <T> T getFieldAttr (FieldInfo fi, Class<T> attrType) {
    return fields.getFieldAttr(fi.getFieldIndex(), attrType);
  }
  public <T> T getNextFieldAttr (FieldInfo fi, Class<T> attrType, Object prev) {
    return fields.getNextFieldAttr(fi.getFieldIndex(), attrType, prev);
  }
  public ObjectList.Iterator fieldAttrIterator (FieldInfo fi){
    return fields.fieldAttrIterator(fi.getFieldIndex());
  }
  public <T> ObjectList.TypedIterator<T> fieldAttrIterator (FieldInfo fi, Class<T> type){
    return fields.fieldAttrIterator(fi.getFieldIndex(), type);
  }
  

  

  
  public boolean hasElementAttr() {
    return fields.hasFieldAttr();
  }

  public boolean hasElementAttr (Class<?> attrType){
    return fields.hasFieldAttr(attrType);
  }

  
  
  public Object getElementAttr (int idx){
    return fields.getFieldAttr(idx);
  }

  
  public void setElementAttr (int idx, Object attr){
    int nElements = getNumberOfFieldsOrElements();
    checkIsModifiable();
    fields.setFieldAttr( nElements, idx, attr);
  }

  
  public void setElementAttrNoClone (int idx, Object attr){
    int nElements = getNumberOfFieldsOrElements();
    fields.setFieldAttr(nElements,idx, attr);
  }

  
  public void addElementAttr (int idx, Object a){
    checkIsModifiable();
    
    int nElements = getNumberOfFieldsOrElements();   
    fields.addFieldAttr( nElements, idx, a);
  }
  public void removeElementAttr (int idx, Object a){
    checkIsModifiable();
    fields.removeFieldAttr(idx, a);
  }
  public void replaceElementAttr (int idx, Object oldAttr, Object newAttr){
    checkIsModifiable();
    fields.replaceFieldAttr(idx, oldAttr, newAttr);
  }


  public void addElementAttrNoClone (int idx, Object a){
    int nElements = getNumberOfFieldsOrElements();   
    fields.addFieldAttr( nElements, idx, a);
  }
  public void removeElementAttrNoClone (int idx, Object a){
    fields.removeFieldAttr(idx, a);
  }
  public void replaceElementAttrNoClone (int idx, Object oldAttr, Object newAttr){
    fields.replaceFieldAttr(idx, oldAttr, newAttr);
  }
  
  
  public <T> T getElementAttr (int idx, Class<T> attrType) {
    return fields.getFieldAttr(idx, attrType);
  }
  public <T> T getNextElementAttr (int idx, Class<T> attrType, Object prev) {
    return fields.getNextFieldAttr(idx, attrType, prev);
  }
  public ObjectList.Iterator elementAttrIterator (int idx){
    return fields.fieldAttrIterator(idx);
  }
  public <T> ObjectList.TypedIterator<T> elementAttrIterator (int idx, Class<T> type){
    return fields.fieldAttrIterator(idx, type);
  }


  
  
  public void setDeclaredIntField(String fname, String clsBase, int value) {
    setIntField(getDeclaredFieldInfo(clsBase, fname), value);
  }

  public void setBooleanField (String fname, boolean value) {
    setBooleanField( getFieldInfo(fname), value);
  }
  public void setByteField (String fname, byte value) {
    setByteField( getFieldInfo(fname), value);
  }
  public void setCharField (String fname, char value) {
    setCharField( getFieldInfo(fname), value);
  }
  public void setShortField (String fname, short value) {
    setShortField( getFieldInfo(fname), value);
  }
  public void setIntField(String fname, int value) {
    setIntField(getFieldInfo(fname), value);
  }
  public void setLongField (String fname, long value) {
    setLongField( getFieldInfo(fname), value);
  }
  public void setFloatField (String fname, float value) {
    setFloatField( getFieldInfo(fname), value);
  }
  public void setDoubleField (String fname, double value) {
    setDoubleField( getFieldInfo(fname), value);
  }
  public void setReferenceField (String fname, int value) {
    setReferenceField( getFieldInfo(fname), value);
  }



  public Object getFieldValueObject (String fname) {
    Object ret = null;
    FieldInfo fi = getFieldInfo(fname);

    if (fi != null){
      ret = fi.getValueObject(fields);

    } else {

      ElementInfo eiEnclosing = getEnclosingElementInfo();
      if (eiEnclosing != null){
        ret = eiEnclosing.getFieldValueObject(fname);

      } else {



      }
    }

    return ret;
  }

  public ElementInfo getEnclosingElementInfo() {
    return null; 
  }

  public void setBooleanField(FieldInfo fi, boolean newValue) {
    checkIsModifiable();
    
    if (fi.isBooleanField()) {
      int offset = fi.getStorageOffset();
      fields.setBooleanValue( offset, newValue);
    } else {
      throw new JPFException("not a boolean field: " + fi.getFullName());
    }
  }

  public void setByteField(FieldInfo fi, byte newValue) {
    checkIsModifiable();
    
    if (fi.isByteField()) {
      int offset = fi.getStorageOffset();
      fields.setByteValue( offset, newValue);
    } else {
      throw new JPFException("not a byte field: " + fi.getFullName());
    }
  }

  public void setCharField(FieldInfo fi, char newValue) {
    checkIsModifiable();
    
    if (fi.isCharField()) {
      int offset = fi.getStorageOffset();
      fields.setCharValue( offset, newValue);
    } else {
      throw new JPFException("not a char field: " + fi.getFullName());
    }
  }

  public void setShortField(FieldInfo fi, short newValue) {
    checkIsModifiable();

    if (fi.isShortField()) {
      int offset = fi.getStorageOffset();
      fields.setShortValue( offset, newValue);
    } else {
      throw new JPFException("not a short field: " + fi.getFullName());
    }
  }

  public void setIntField(FieldInfo fi, int newValue) {
    checkIsModifiable();

    if (fi.isIntField()) {
      int offset = fi.getStorageOffset();
      fields.setIntValue( offset, newValue);
    } else {
      throw new JPFException("not an int field: " + fi.getFullName());
    }
  }

  public void setLongField(FieldInfo fi, long newValue) {
    checkIsModifiable();

    if (fi.isLongField()) {
      int offset = fi.getStorageOffset();
      fields.setLongValue( offset, newValue);
    } else {
      throw new JPFException("not a long field: " + fi.getFullName());
    }
  }

  public void setFloatField(FieldInfo fi, float newValue) {
    checkIsModifiable();

    if (fi.isFloatField()) {
      int offset = fi.getStorageOffset();
      fields.setFloatValue( offset, newValue);
    } else {
      throw new JPFException("not a float field: " + fi.getFullName());
    }
  }

  public void setDoubleField(FieldInfo fi, double newValue) {
    checkIsModifiable();

    if (fi.isDoubleField()) {
      int offset = fi.getStorageOffset();
      fields.setDoubleValue( offset, newValue);
    } else {
      throw new JPFException("not a double field: " + fi.getFullName());
    }
  }

  public void setReferenceField(FieldInfo fi, int newValue) {
    checkIsModifiable();

    if (fi.isReference()) {
      int offset = fi.getStorageOffset();
      fields.setReferenceValue( offset, newValue);
    } else {
      throw new JPFException("not a reference field: " + fi.getFullName());
    }
  }

  public void set1SlotField(FieldInfo fi, int newValue) {
    checkIsModifiable();

    if (fi.is1SlotField()) {
      int offset = fi.getStorageOffset();
      fields.setIntValue( offset, newValue);
    } else {
      throw new JPFException("not a 1 slot field: " + fi.getFullName());
    }
  }

  public void set2SlotField(FieldInfo fi, long newValue) {
    checkIsModifiable();

    if (fi.is2SlotField()) {
      int offset = fi.getStorageOffset();
      fields.setLongValue( offset, newValue);
    } else {
      throw new JPFException("not a 2 slot field: " + fi.getFullName());
    }
  }


  public void setDeclaredReferenceField(String fname, String clsBase, int value) {
    setReferenceField(getDeclaredFieldInfo(clsBase, fname), value);
  }

  public int getDeclaredReferenceField(String fname, String clsBase) {
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    return getReferenceField( fi);
  }

  public int getReferenceField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getReferenceField( fi);
  }


  public int getDeclaredIntField(String fname, String clsBase) {


    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    return getIntField( fi);
  }

  public int getIntField(String fname) {


    FieldInfo fi = getFieldInfo(fname);
    return getIntField( fi);
  }

  public void setDeclaredLongField(String fname, String clsBase, long value) {
    checkIsModifiable();
    
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    fields.setLongValue( fi.getStorageOffset(), value);
  }

  public long getDeclaredLongField(String fname, String clsBase) {
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    return getLongField( fi);
  }

  public long getLongField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getLongField( fi);
  }

  public boolean getDeclaredBooleanField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    return getBooleanField( fi);
  }

  public boolean getBooleanField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getBooleanField( fi);
  }

  public byte getDeclaredByteField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    return getByteField( fi);
  }

  public byte getByteField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getByteField( fi);
  }

  public char getDeclaredCharField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    return getCharField( fi);
  }

  public char getCharField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getCharField( fi);
  }

  public double getDeclaredDoubleField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    return getDoubleField( fi);
  }

  public double getDoubleField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getDoubleField( fi);
  }

  public float getDeclaredFloatField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    return getFloatField( fi);
  }

  public float getFloatField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getFloatField( fi);
  }

  public short getDeclaredShortField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    return getShortField( fi);
  }

  public short getShortField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    return getShortField( fi);
  }

  
  private void checkFieldInfo(FieldInfo fi) {
    if (!getClassInfo().isInstanceOf(fi.getClassInfo())) {
      throw new JPFException("wrong FieldInfo : " + fi.getName()
          + " , no such field in " + getClassInfo().getName());
    }
  }




  public boolean getBooleanField(FieldInfo fi) {
    if (fi.isBooleanField()){
      return fields.getBooleanValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a boolean field: " + fi.getName());
    }
  }
  public byte getByteField(FieldInfo fi) {
    if (fi.isByteField()){
      return fields.getByteValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a byte field: " + fi.getName());
    }
  }
  public char getCharField(FieldInfo fi) {
    if (fi.isCharField()){
      return fields.getCharValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a char field: " + fi.getName());
    }
  }
  public short getShortField(FieldInfo fi) {
    if (fi.isShortField()){
      return fields.getShortValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a short field: " + fi.getName());
    }
  }
  public int getIntField(FieldInfo fi) {
    if (fi.isIntField()){
      return fields.getIntValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a int field: " + fi.getName());
    }
  }
  public long getLongField(FieldInfo fi) {
    if (fi.isLongField()){
      return fields.getLongValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a long field: " + fi.getName());
    }
  }
  public float getFloatField (FieldInfo fi){
    if (fi.isFloatField()){
      return fields.getFloatValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a float field: " + fi.getName());
    }
  }
  public double getDoubleField (FieldInfo fi){
    if (fi.isDoubleField()){
      return fields.getDoubleValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a double field: " + fi.getName());
    }
  }
  public int getReferenceField (FieldInfo fi) {
    if (fi.isReference()){
      return fields.getReferenceValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a reference field: " + fi.getName());
    }
  }

  public int get1SlotField(FieldInfo fi) {
    if (fi.is1SlotField()){
      return fields.getIntValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a 1 slot field: " + fi.getName());
    }
  }
  public long get2SlotField(FieldInfo fi) {
    if (fi.is2SlotField()){
      return fields.getLongValue(fi.getStorageOffset());
    } else {
      throw new JPFException("not a 2 slot field: " + fi.getName());
    }
  }

  protected void checkArray(int index) {
    if (fields instanceof ArrayFields) { 
      if ((index < 0) || (index >= ((ArrayFields)fields).arrayLength())) {
        throw new JPFException("illegal array offset: " + index);
      }
    } else {
      throw new JPFException("cannot access non array objects by index");
    }
  }

  public boolean isReferenceArray() {
    return getClassInfo().isReferenceArray();
  }

  
  public void copyElements(ThreadInfo ti, ElementInfo eiSrc, int srcIdx, int dstIdx, int length){
  	copyElements(ti, eiSrc, srcIdx, dstIdx, length, new DynamicDependency[6]);
  }
  
  
  
  public void copyElements(ThreadInfo ti, ElementInfo eiSrc, 
  		int srcIdx, int dstIdx, int length, DynamicDependency[] argAttrs){

    if (!isArray()){
      throw new ArrayStoreException("destination object not an array: " + ci.getName());
    }
    if (!eiSrc.isArray()){
      throw new ArrayStoreException("source object not an array: " + eiSrc.getClassInfo().getName());
    }

    boolean isRefArray = isReferenceArray();
    if (eiSrc.isReferenceArray() != isRefArray){
      throw new ArrayStoreException("array types not compatible: " + eiSrc.getClassInfo().getName() + " and " + ci.getName());
    }






    if (isRefArray){
      ClassInfo dstElementCi = ci.getComponentClassInfo();
      int[] srcRefs = ((ArrayFields)eiSrc.fields).asReferenceArray();
      int max = srcIdx + length;

      for (int i=srcIdx; i<max; i++){
        int eref = srcRefs[i];
        if (eref != MJIEnv.NULL){
          ClassInfo srcElementCi = ti.getClassInfo(eref);
          if (!srcElementCi.isInstanceOf(dstElementCi)) {
            throw new ArrayStoreException("incompatible reference array element type (required " + dstElementCi.getName() +
                    ", found " + srcElementCi.getName());
          }
        }
      }
    }





    checkIsModifiable();

    Object srcVals = ((ArrayFields)eiSrc.getFields()).getValues();
    Object dstVals = ((ArrayFields)fields).getValues();


    System.arraycopy(srcVals, srcIdx, dstVals, dstIdx, length);



    DynamicDependency srcRefDep = argAttrs[0];
    DynamicDependency srcIdxDep = argAttrs[1];
    DynamicDependency dstRefDep = argAttrs[2];
    DynamicDependency dstIdxDep = argAttrs[3];
    DynamicDependency lengthDep = argAttrs[4];
    DynamicDependency invocationCondition = argAttrs[5];
    DynamicDependency commonDep = DynDepBuilder.newBuilder()
    		.appendDataDependency(srcRefDep, srcIdxDep, dstRefDep, dstIdxDep, lengthDep)
    		.setControlDependency(invocationCondition).build();
    if (eiSrc == this && srcIdx < dstIdx) { 
      for (int i = length - 1; i >= 0; i--) {
        DynamicDependency elementDep = 
        		JVMArrayElementInstruction.getArrayElementDependency(eiSrc, srcIdx + i);
        DynamicDependency resultDep = DynDepBuilder.newBuilder()
        		.appendDataDependency(commonDep, elementDep).build();
        JVMArrayElementInstruction.setArrayElementDependency(this, dstIdx + i, resultDep);
      }
    } else {
      for (int i = 0; i < length; i++) {
        DynamicDependency elementDep = 
        		JVMArrayElementInstruction.getArrayElementDependency(eiSrc, srcIdx + i);
        DynamicDependency resultDep = DynDepBuilder.newBuilder()
        		.appendDataDependency(commonDep, elementDep).build();
        JVMArrayElementInstruction.setArrayElementDependency(this, dstIdx + i, resultDep);
      }
    }
    
  }

  public void setBooleanElement(int idx, boolean value){
    checkArray(idx);
    checkIsModifiable();
    fields.setBooleanValue(idx, value);
  }
  public void setByteElement(int idx, byte value){
    checkArray(idx);
    checkIsModifiable();
    fields.setByteValue(idx, value);
  }
  public void setCharElement(int idx, char value){
    checkArray(idx);
    checkIsModifiable();
    fields.setCharValue(idx, value);
  }
  public void setShortElement(int idx, short value){
    checkArray(idx);
    checkIsModifiable();
    fields.setShortValue(idx, value);
  }
  public void setIntElement(int idx, int value){
    checkArray(idx);
    checkIsModifiable();
    fields.setIntValue(idx, value);
  }
  public void setLongElement(int idx, long value) {
    checkArray(idx);
    checkIsModifiable();
    fields.setLongValue(idx, value);
  }
  public void setFloatElement(int idx, float value){
    checkArray(idx);
    checkIsModifiable();
    fields.setFloatValue(idx, value);
  }
  public void setDoubleElement(int idx, double value){
    checkArray(idx);
    checkIsModifiable();
    fields.setDoubleValue(idx, value);
  }
  public void setReferenceElement(int idx, int value){
    checkArray(idx);
    checkIsModifiable();
    fields.setReferenceValue(idx, value);
  }

  
  public void arrayCopy (ElementInfo src, int srcPos, int dstPos, int len){
    checkArray(dstPos+len-1);
    src.checkArray(srcPos+len-1);
    checkIsModifiable();
    
    ArrayFields da = (ArrayFields)fields;
    ArrayFields sa = (ArrayFields)src.fields;
    
    da.copyElements(sa, srcPos, dstPos, len);
  }

  public boolean getBooleanElement(int idx) {
    checkArray(idx);
    return fields.getBooleanValue(idx);
  }
  public byte getByteElement(int idx) {
    checkArray(idx);
    return fields.getByteValue(idx);
  }
  public char getCharElement(int idx) {
    checkArray(idx);
    return fields.getCharValue(idx);
  }
  public short getShortElement(int idx) {
    checkArray(idx);
    return fields.getShortValue(idx);
  }
  public int getIntElement(int idx) {
    checkArray(idx);
    return fields.getIntValue(idx);
  }
  public long getLongElement(int idx) {
    checkArray(idx);
    return fields.getLongValue(idx);
  }
  public float getFloatElement(int idx) {
    checkArray(idx);
    return fields.getFloatValue(idx);
  }
  public double getDoubleElement(int idx) {
    checkArray(idx);
    return fields.getDoubleValue(idx);
  }
  public int getReferenceElement(int idx) {
    checkArray(idx);
    return fields.getReferenceValue(idx);
  }

  public void setObjectRef(int newObjRef) {
    objRef = newObjRef;
  }

  public int getObjectRef() {
    return objRef;
  }

  
  @Deprecated
  public int getIndex(){
    return objRef;
  }

  public int getLockCount() {
    return monitor.getLockCount();
  }

  public ThreadInfo getLockingThread() {
    return monitor.getLockingThread();
  }

  public boolean isLocked() {
    return (monitor.getLockCount() > 0);
  }

  public boolean isArray() {
    return ci.isArray();
  }

  public boolean isCharArray(){
    return (fields instanceof CharArrayFields);
  }
  
  public boolean isFloatArray(){
    return (fields instanceof FloatArrayFields);
  }

  public boolean isDoubleArray(){
    return (fields instanceof DoubleArrayFields);
  }

  
  public String getArrayType() {
    if (!ci.isArray()) {
      throw new JPFException("object is not an array");
    }

    return Types.getArrayElementType(ci.getType());
  }

  public Object getBacktrackData() {
    return null;
  }



  public boolean[] asBooleanArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asBooleanArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public byte[] asByteArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asByteArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public short[] asShortArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asShortArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public char[] asCharArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asCharArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public int[] asIntArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asIntArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public long[] asLongArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asLongArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public float[] asFloatArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asFloatArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public double[] asDoubleArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asDoubleArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public int[] asReferenceArray() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).asReferenceArray();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }
    
  public boolean isNull() {
    return (objRef == MJIEnv.NULL);
  }

  public ElementInfo getDeclaredObjectField(String fname, String referenceType) {
    return VM.getVM().getHeap().get(getDeclaredReferenceField(fname, referenceType));
  }

  public ElementInfo getObjectField(String fname) {
    return VM.getVM().getHeap().get(getReferenceField(fname));
  }


  
  public int getHeapSize() {
    return fields.getHeapSize();
  }

  public String getStringField(String fname) {
    int ref = getReferenceField(fname);

    if (ref != MJIEnv.NULL) {
      ElementInfo ei = VM.getVM().getHeap().get(ref);
      return ei.asString();
    } else {
      return "null";
    }
  }

  public String getType() {
    return ci.getType();
  }

  
  public ThreadInfo[] getLockedThreads() {
    return monitor.getLockedThreads();
  }
  
  
  public ThreadInfo[] getWaitingThreads() {
    return monitor.getWaitingThreads();
  }

  public boolean hasWaitingThreads() {
    return monitor.hasWaitingThreads();
  }

  public ThreadInfo[] getBlockedThreads() {
    return monitor.getBlockedThreads();
  }

  public ThreadInfo[] getBlockedOrWaitingThreads() {
    return monitor.getBlockedOrWaitingThreads();
  }
    
  public int arrayLength() {
    if (fields instanceof ArrayFields){
      return ((ArrayFields)fields).arrayLength();
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }

  public boolean isStringObject() {
    return ClassInfo.isStringClassInfo(ci);
  }

  public String asString() {
    throw new JPFException("not a String object: " + this);
  }

  public char[] getStringChars(){
    throw new JPFException("not a String object: " + this);    
  }
  
  
  public boolean equalsString (String s) {
    throw new JPFException("not a String object: " + this);
  }

  
  public boolean isBoxObject(){
    return false;
  }
  
  public Object asBoxObject(){
    throw new JPFException("not a box object: " + this);    
  }
  
  void updateLockingInfo() {
    int i;

    ThreadInfo ti = monitor.getLockingThread();
    if (ti != null) {









      ti.updateLockedObject(this);
    }

    if (monitor.hasLockedThreads()) {
      ThreadInfo[] lockedThreads = monitor.getLockedThreads();
      for (i=0; i<lockedThreads.length; i++) {
        ti = lockedThreads[i];

        


        if (!ti.isRunnable()){
          ti.setLockRef(objRef);
        }
      }
    }
  }

  public boolean canLock(ThreadInfo th) {
    return monitor.canLock(th);
  }

  public boolean checkArrayBounds(int index) {
    if (fields instanceof ArrayFields) {
      if (index < 0 || index >= ((ArrayFields)fields).arrayLength()){
      	return false;



      }
      return true;
    } else {
      throw new JPFException("object is not an array: " + this);
    }
  }

  @Override
  public ElementInfo clone() {
    try {
      ElementInfo ei = (ElementInfo) super.clone();
      ei.fields = fields.clone();
      ei.monitor = monitor.clone();

      return ei;
      
    } catch (CloneNotSupportedException e) {
      throw new InternalError("should not happen");
    }
  }


  public ElementInfo deepClone() {
    try {
      ElementInfo ei = (ElementInfo) super.clone();
      ei.fields = fields.clone();
      ei.monitor = monitor.clone();
      

      
      ei.cachedMemento = null;
      ei.defreeze();
      
      return ei;
      
    } catch (CloneNotSupportedException e) {
      throw new InternalError("should not happen");
    }
    
  }

  public boolean instanceOf(String type) {
    return Types.instanceOf(ci.getType(), type);
  }

  abstract public int getNumberOfFields();

  abstract public FieldInfo getFieldInfo(int fieldIndex);

  
  public void registerLockContender (ThreadInfo ti) {

    assert ti.lockRef == MJIEnv.NULL || ti.lockRef == objRef :
      "thread " + ti + " trying to register for : " + this +
      " ,but already blocked on: " + ti.getElementInfo(ti.lockRef);




    setMonitorWithLocked(ti);



  }

  public boolean isRegisteredLockContender (ThreadInfo ti){
    return monitor.isLocking(ti);
  }
  
  
  public void unregisterLockContender (ThreadInfo ti) {
    setMonitorWithoutLocked(ti);



  }

  void blockLockContenders () {


    ThreadInfo[] lockedThreads = monitor.getLockedThreads();
    for (int i=0; i<lockedThreads.length; i++) {
      ThreadInfo ti = lockedThreads[i];
      if (ti.isRunnable()) {
        ti.setBlockedState(objRef);
      }
    }
  }

  
  public void block (ThreadInfo ti) {
    assert (monitor.getLockingThread() != null) && (monitor.getLockingThread() != ti) :
          "attempt to block " + ti.getName() + " on unlocked or own locked object: " + this;

    setMonitorWithLocked(ti);
    
    ti.setBlockedState(objRef);    
  }

  
  public void lock (ThreadInfo ti) {

    if ((monitor.getLockingThread() != null) &&  (monitor.getLockingThread() != ti)){
      throw new JPFException("thread " + ti.getName() + " tries to lock object: "
              + this + " which is locked by: " + monitor.getLockingThread().getName());
    }
    


    setMonitorWithoutLocked(ti);
    monitor.setLockingThread(ti);
    monitor.incLockCount();


    ti.resetLockRef();

    ThreadInfo.State state = ti.getState();
    if (state == ThreadInfo.State.UNBLOCKED) {
      ti.setState(ThreadInfo.State.RUNNING);
    }


    if (monitor.getLockCount() == 1) {
      ti.addLockedObject(this);
    }



    blockLockContenders();
  }

  
  public boolean unlock (ThreadInfo ti) {
    boolean didUnblock = false;

    checkIsModifiable();
    
    
    if ((monitor.getLockCount() <= 0) || (monitor.getLockingThread() != ti)){
      throw new JPFException("thread " + ti.getName() + " tries to release non-owned lock for object: " + this);
    }

    if (monitor.getLockCount() == 1) {
      ti.removeLockedObject(this);

      ThreadInfo[] lockedThreads = monitor.getLockedThreads();
      for (int i = 0; i < lockedThreads.length; i++) {
        ThreadInfo lti = lockedThreads[i];
        switch (lti.getState()) {

        case BLOCKED:
        case NOTIFIED:
        case TIMEDOUT:
        case INTERRUPTED:

          lti.resetLockRef();
          lti.setState(ThreadInfo.State.UNBLOCKED);
          didUnblock = true;
          break;

        case WAITING:
        case TIMEOUT_WAITING:

          break;

        default:
          assert false : "Monitor.lockedThreads<->ThreadData.status inconsistency! " + lockedThreads[i].getStateName();

        }
      }



      monitor.decLockCount();
      monitor.setLockingThread(null);

    } else { 
      monitor.decLockCount();
    }
    
    return didUnblock;
  }

  
  public boolean notifies(SystemState ss, ThreadInfo ti){
    return notifies(ss, ti, true);
  }
  
  
  private void notifies0 (ThreadInfo tiWaiter){
    if (tiWaiter.isWaitingOrTimedOut()){
      if (tiWaiter.getLockCount() > 0) {

        tiWaiter.setNotifiedState();

      } else {

        setMonitorWithoutLocked(tiWaiter);
        tiWaiter.resetLockRef();
        tiWaiter.setRunning();
      }
    }
  }

  
  
  public boolean notifies (SystemState ss, ThreadInfo ti, boolean hasToHoldLock){
    if (hasToHoldLock){
      assert monitor.getLockingThread() != null : "notify on unlocked object: " + this;
    }

    ThreadInfo[] locked = monitor.getLockedThreads();
    int i, nWaiters=0, iWaiter=0;

    for (i=0; i<locked.length; i++) {
      if (locked[i].isWaitingOrTimedOut() ) {
        nWaiters++;
        iWaiter = i;
      }
    }

    if (nWaiters == 0) {

    } else if (nWaiters == 1) {

      notifies0(locked[iWaiter]);

    } else {

      ThreadChoiceGenerator cg = ss.getCurrentChoiceGeneratorOfType(ThreadChoiceGenerator.class);

      assert (cg != null) : "no ThreadChoiceGenerator in notify";

      notifies0(cg.getNextChoice());
    }

    ti.getVM().notifyObjectNotifies(ti, this);
    return (nWaiters > 0);
  }

  
  public boolean notifiesAll() {
    assert monitor.getLockingThread() != null : "notifyAll on unlocked object: " + this;

    ThreadInfo[] locked = monitor.getLockedThreads();
    for (int i=0; i<locked.length; i++) {


      notifies0(locked[i]);
    }

    VM.getVM().notifyObjectNotifiesAll(ThreadInfo.currentThread, this);
    return (locked.length > 0);
  }


  
  public void wait(ThreadInfo ti, long timeout){
    wait(ti,timeout,true);
  }


  public void wait (ThreadInfo ti, long timeout, boolean hasToHoldLock){
    checkIsModifiable();
    
    boolean holdsLock = monitor.getLockingThread() == ti;

    if (hasToHoldLock){
      assert holdsLock : "wait on unlocked object: " + this;
    }

    setMonitorWithLocked(ti);
    ti.setLockRef(objRef);
    
    if (timeout == 0) {
      ti.setState(ThreadInfo.State.WAITING);
    } else {
      ti.setState(ThreadInfo.State.TIMEOUT_WAITING);
    }

    if (holdsLock) {
      ti.setLockCount(monitor.getLockCount());

      monitor.setLockingThread(null);
      monitor.setLockCount(0);

      ti.removeLockedObject(this);


      ThreadInfo[] lockedThreads = monitor.getLockedThreads();
      for (int i = 0; i < lockedThreads.length; i++) {
        ThreadInfo lti = lockedThreads[i];
        switch (lti.getState()) {
          case NOTIFIED:
          case BLOCKED:
          case INTERRUPTED:
            lti.resetLockRef();
            lti.setState(ThreadInfo.State.UNBLOCKED);
            break;
        }
      }
    }


    ti.getVM().notifyObjectWait(ti, this);
  }


  
  public void lockNotified (ThreadInfo ti) {
    assert ti.isUnblocked() : "resume waiting thread " + ti.getName() + " which is not unblocked";

    setMonitorWithoutLocked(ti);
    monitor.setLockingThread( ti);
    monitor.setLockCount( ti.getLockCount());

    ti.setLockCount(0);
    ti.resetLockRef();
    ti.setState( ThreadInfo.State.RUNNING);

    blockLockContenders();




    ti.addLockedObject(this); 
  }

  
  public void resumeNonlockedWaiter (ThreadInfo ti){
    setMonitorWithoutLocked(ti);

    ti.setLockCount(0);
    ti.resetLockRef();
    ti.setRunning();
  }


  void dumpMonitor () {
    PrintWriter pw = new PrintWriter(System.out, true);
    pw.print( "monitor ");

    monitor.printFields(pw);
    pw.flush();
  }

  
  boolean incPinDown() {
    int pdCount = (attributes & ATTR_PINDOWN_MASK);

    pdCount++;
    if ((pdCount & ~ATTR_PINDOWN_MASK) != 0){
      throw new JPFException("pinDown limit exceeded: " + this);
    } else {
      int a = (attributes & ~ATTR_PINDOWN_MASK);
      a |= pdCount;
      a |= ATTR_ATTRIBUTE_CHANGED;
      attributes = a;
      
      return (pdCount == 1);
    }
  }

  
  boolean decPinDown() {
    int pdCount = (attributes & ATTR_PINDOWN_MASK);

    if (pdCount > 0){
      pdCount--;
      int a = (attributes & ~ATTR_PINDOWN_MASK);
      a |= pdCount;
      a |= ATTR_ATTRIBUTE_CHANGED;
      attributes = a;

      return (pdCount == 0);
      
    } else {
      return false;
    }
  }

  public int getPinDownCount() {
    return (attributes & ATTR_PINDOWN_MASK);
  }

  public boolean isPinnedDown() {
    return (attributes & ATTR_PINDOWN_MASK) != 0;
  }


  public boolean isConstructed() {
    return (attributes & ATTR_CONSTRUCTED) != 0;
  }

  public void setConstructed() {
    attributes |= (ATTR_CONSTRUCTED | ATTR_ATTRIBUTE_CHANGED);
  }

  public void restoreFields(Fields f) {
    fields = f;
  }

  
  public Fields getFields() {
    return fields;
  }

  public ArrayFields getArrayFields(){
    if (fields instanceof ArrayFields){
      return (ArrayFields)fields;
    } else {
      throw new JPFException("not an array: " + ci.getName());
    }
  }
  
  public void restore(int index, int attributes, Fields fields, Monitor monitor){
    markUnchanged();
    
    this.objRef = index;
    this.attributes = attributes;
    this.fields = fields;
    this.monitor = monitor;
  }

  public void restoreMonitor(Monitor m) {
    monitor = m;
  }

  
  public Monitor getMonitor() {
    return monitor;
  }

  public void restoreAttributes(int a) {
    attributes = a;
  }

  public boolean isAlive(boolean liveBitValue) {
    return ((attributes & ATTR_LIVE_BIT) == 0) ^ liveBitValue;
  }

  public void setAlive(boolean liveBitValue){
    if (liveBitValue){
      attributes |= ATTR_LIVE_BIT;
    } else {
      attributes &= ~ATTR_LIVE_BIT;
    }
  }

  public boolean isMarked() {
    return (attributes & ATTR_IS_MARKED) != 0;
  }

  public boolean isFinalized() {
    return (attributes & ATTR_FINALIZED) != 0;
  }
  
  public void setFinalized() {
    attributes |= ATTR_FINALIZED;
  }
  
  public void setMarked() {
    attributes |= ATTR_IS_MARKED;
  }

  public boolean isMarkedOrAlive (boolean liveBitValue){
    return ((attributes & ATTR_IS_MARKED) != 0) | (((attributes & ATTR_LIVE_BIT) == 0) ^ liveBitValue);
  }

  public void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;
  }

  public void setUnmarked() {
    attributes &= ~ATTR_IS_MARKED;
  }


  protected void checkIsModifiable() {
    if ((attributes & ATTR_IS_FROZEN) != 0) {
      throw new JPFException("attempt to modify frozen object: " + this);
    }
  }

  void setMonitorWithLocked( ThreadInfo ti) {
    checkIsModifiable();
    monitor.addLocked(ti);
  }

  void setMonitorWithoutLocked (ThreadInfo ti) {
    checkIsModifiable();    
    monitor.removeLocked(ti);
  }

  public boolean isLockedBy(ThreadInfo ti) {
    return ((monitor != null) && (monitor.getLockingThread() == ti));
  }

  public boolean isLocking(ThreadInfo ti){
    return (monitor != null) && monitor.isLocking(ti);
  }
  
  void _printAttributes(String cls, String msg, int oldAttrs) {
    if (getClassInfo().getName().equals(cls)) {
      System.out.println(msg + " " + this + " attributes: "
          + Integer.toHexString(attributes) + " was: "
          + Integer.toHexString(oldAttrs));
    }
  }

    
  public void checkConsistency() {

  }
  
  protected void checkAssertion(boolean cond, String failMsg){
    if (!cond){
      System.out.println("!!!!!! failed ElementInfo consistency: "  + this + ": " + failMsg);

      System.out.println("object: " + this);
      System.out.println("usingTi: " + referencingThreads);
      
      ThreadInfo tiLock = getLockingThread();
      if (tiLock != null) System.out.println("locked by: " + tiLock);
      
      if (monitor.hasLockedThreads()){
        System.out.println("lock contenders:");
        for (ThreadInfo ti : monitor.getLockedThreads()){
          System.out.println("  " + ti + " = " + ti.getState());
        }
      }
      
      VM.getVM().dumpThreadStates();
      assert false;
    }
  }

}

