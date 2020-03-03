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
import gov.nasa.jpf.util.ObjectList;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Random;


public abstract class ChoiceGeneratorBase<T> implements ChoiceGenerator<T> {

  
  static enum ChoiceRandomizationPolicy {
    VAR_SEED,    
    FIXED_SEED,  
    NONE         
  };
  
  static ChoiceRandomizationPolicy randomization;
  

  public static final char MARKER = '>';
  protected static Random random = new Random(42);
  
  

  protected String id;
  



  protected int idRef;
  

  protected boolean isDone;
  

  protected ChoiceGenerator<?> prev;


  protected Instruction insn;


  protected int stateId;
  

  protected ThreadInfo ti;


  protected Object attr;



  protected boolean isCascaded;


  public static void init(Config config) {

    randomization = config.getEnum("cg.randomize_choices", 
                                   ChoiceRandomizationPolicy.values(), ChoiceRandomizationPolicy.NONE);




    if (randomization == ChoiceRandomizationPolicy.VAR_SEED) {
      random.setSeed(System.currentTimeMillis());
    } else if (randomization == ChoiceRandomizationPolicy.FIXED_SEED){
      long seed = config.getLong("cg.seed", 42);
      random.setSeed( seed);
    }
  }
  
  public static boolean useRandomization() {
    return (randomization != ChoiceRandomizationPolicy.NONE);
  }

  
  @Deprecated
  protected ChoiceGeneratorBase() {
    id = "?";
  }

  protected ChoiceGeneratorBase(String id) {
    this.id = id;
  }

  @Override
  public ChoiceGeneratorBase<?> clone() throws CloneNotSupportedException {
    return (ChoiceGeneratorBase<?>)super.clone();
  }

  @Override
  public ChoiceGenerator<?> deepClone() throws CloneNotSupportedException {
    ChoiceGenerator<?> clone = (ChoiceGenerator<?>) super.clone();

    if (prev != null){
      clone.setPreviousChoiceGenerator( prev.deepClone());
    }
    return clone;
  }
  
  @Override
  public String getId() {
    return id;
  }

  @Override
  public int getIdRef() {
    return idRef;
  }

  @Override
  public void setIdRef(int idRef) {
    this.idRef = idRef;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public boolean isSchedulingPoint() {
    return false;
  }


  @Override
  public void setThreadInfo(ThreadInfo ti) {
    this.ti = ti;
  }

  @Override
  public ThreadInfo getThreadInfo() {
    return ti;
  }

  @Override
  public void setInsn(Instruction insn) {
    this.insn = insn;
  }

  @Override
  public Instruction getInsn() {
    return insn;
  }

  @Override
  public void setContext(ThreadInfo tiCreator) {
    ti = tiCreator;
    insn = tiCreator.getPC();
  }
  
  @Override
  public void setStateId(int stateId){
    this.stateId = stateId;

    if (isCascaded){
      getCascadedParent().setStateId(stateId);
    }
  }
  
  @Override
  public int getStateId(){
    return stateId;
  }

  @Override
  public String getSourceLocation() {
    return insn.getSourceLocation();
  }

  @Override
  public boolean supportsReordering(){
    return false;
  }
  
  
  @Override
  public ChoiceGenerator<T> reorder (Comparator<T> comparator){
    return null;
  }
  
  @Override
  public void setPreviousChoiceGenerator(ChoiceGenerator<?> cg) {
    prev = cg;
  }

  @Override
  public void setCascaded() {
    isCascaded = true;
  }

  @Override
  public boolean isCascaded() {
    return isCascaded;
  }

  @Override
  public <C extends ChoiceGenerator<?>> C getPreviousChoiceGeneratorOfType(Class<C> cls) {
    ChoiceGenerator<?> cg = prev;

    while (cg != null) {
      if (cls.isInstance(cg)) {
        return (C) cg;
      }
      cg = cg.getPreviousChoiceGenerator();
    }
    return null;
  }

  
  @Override
  public ChoiceGenerator<?> getCascadedParent() {
    if (prev != null) {
      if (prev.isCascaded()) {
        return prev;
      }
    }

    return null;
  }

  
  @Override
  public ChoiceGenerator<?>[] getCascade() {
    int n = 0;
    for (ChoiceGenerator<?> cg = this; cg != null; cg = cg.getCascadedParent()) {
      n++;
    }

    ChoiceGenerator<?>[] a = new ChoiceGenerator<?>[n];

    for (ChoiceGenerator<?> cg = this; cg != null; cg = cg.getCascadedParent()) {
      a[--n] = cg;
    }

    return a;
  }

  
  @Override
  public ChoiceGenerator<?>[] getAll() {
    int n = 0;
    for (ChoiceGenerator<?> cg = this; cg != null; cg = cg.getPreviousChoiceGenerator()) {
      n++;
    }

    ChoiceGenerator<?>[] a = new ChoiceGenerator<?>[n];

    for (ChoiceGenerator<?> cg = this; cg != null; cg = cg.getPreviousChoiceGenerator()) {
      a[--n] = cg;
    }

    return a;
  }

  
  @Override
  public <C extends ChoiceGenerator<?>> C[] getAllOfType(Class<C> cgType) {
    int n = 0;
    for (ChoiceGenerator<?> cg = this; cg != null; cg = cg.getPreviousChoiceGenerator()) {
      if (cgType.isAssignableFrom(cg.getClass())) {
        n++;
      }
    }

    C[] a = (C[]) Array.newInstance(cgType, n);

    for (ChoiceGenerator<?> cg = this; cg != null; cg = cg.getPreviousChoiceGenerator()) {
      if (cgType.isAssignableFrom(cg.getClass())) {
        a[--n] = (C) cg;
      }
    }

    return a;
  }

  @Override
  public int getNumberOfParents(){
    int n=0;
    for (ChoiceGenerator cg = prev; cg != null; cg = cg.getPreviousChoiceGenerator()){
      n++;
    }
    return n;
  }
  
  @Override
  public void setCurrent(){


  }
  


  
  @Override
  public void advance(int nChoices) {
    while (nChoices-- > 0) {
      advance();
    }
  }

  @Override
  public void select (int choiceIndex) {
    reset();
    advance(choiceIndex+1);
    setDone();
  }


  
  
  @Override
  public T getChoice (int idx){
    return null;
  }
  


  
  @Override
  public T[] getAllChoices(){
    int n = getTotalNumberOfChoices();
    T[] a = (T[]) new Object[n];
    for (int i=0; i<n; i++){
      T c = getChoice(i);
      if (c == null){
        return null; 
      } else {
        a[i] = c;
      }
    }
    return a;
  }
  
  @Override
  public T[] getProcessedChoices(){
    int n = getProcessedNumberOfChoices();
    T[] a = (T[]) new Object[n];
    for (int i=0; i<n; i++){
      T c = getChoice(i);
      if (c == null){
        return null; 
      } else {
        a[i] = c;
      }
    }
    return a;    
  }
  
  @Override
  public T[] getUnprocessedChoices(){
    int n = getTotalNumberOfChoices();
    int m = getProcessedNumberOfChoices();
    T[] a = (T[]) new Object[n];
    for (int i=m-1; i<n; i++){
      T c = getChoice(i);
      if (c == null){
        return null; 
      } else {
        a[i] = c;
      }
    }
    return a;    
  }
  
  
  @Override
  public boolean isDone() {
    return isDone;
  }

  @Override
  public void setDone() {
    isDone = true;
  }

  @Override
  public boolean isProcessed() {
    return isDone || !hasMoreChoices();
  }


  @Override
  public boolean hasAttr() {
    return (attr != null);
  }

  @Override
  public boolean hasAttr(Class<?> attrType) {
    return ObjectList.containsType(attr, attrType);
  }

  public boolean hasAttrValue (Object a){
    return ObjectList.contains(attr, a);
  }
  
  
  @Override
  public Object getAttr() {
    return attr;
  }

  
  @Override
  public void setAttr(Object a) {
    attr = a;
  }

  @Override
  public void addAttr(Object a) {
    attr = ObjectList.add(attr, a);
  }

  @Override
  public void removeAttr(Object a) {
    attr = ObjectList.remove(attr, a);
  }

  @Override
  public void replaceAttr(Object oldAttr, Object newAttr) {
    attr = ObjectList.replace(attr, oldAttr, newAttr);
  }

  
  @Override
  public <T> T getAttr(Class<T> attrType) {
    return ObjectList.getFirst(attr, attrType);
  }

  @Override
  public <T> T getNextAttr(Class<T> attrType, Object prev) {
    return ObjectList.getNext(attr, attrType, prev);
  }

  @Override
  public ObjectList.Iterator attrIterator() {
    return ObjectList.iterator(attr);
  }

  @Override
  public <T> ObjectList.TypedIterator<T> attrIterator(Class<T> attrType) {
    return ObjectList.typedIterator(attr, attrType);
  }


  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(getClass().getName());
    b.append(" {id:\"");
    b.append(id);
    b.append("\" ,");
    b.append(getProcessedNumberOfChoices());
    b.append('/');
    b.append(getTotalNumberOfChoices());
    b.append(",isCascaded:");
    b.append(isCascaded);

    if (attr != null) {
      b.append(",attrs:[");
      int i = 0;
      for (Object a : ObjectList.iterator(attr)) {
        if (i++ > 1) {
          b.append(',');
        }
        b.append(a);
      }
      b.append(']');
    }

    b.append('}');

    return b.toString();
  }

  @Override
  public ChoiceGenerator<?> getPreviousChoiceGenerator() {
    return prev;
  }


  @Override
  public ChoiceGenerator<T> randomize(){
    return this;
  }
}
