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

import gov.nasa.jpf.util.ObjectList;

import java.util.Comparator;


public interface ChoiceGenerator<T> extends Cloneable {



  T getNextChoice();

  Class<T> getChoiceType();

  boolean hasMoreChoices();

  
  void setCurrent();
  
  
  void advance();

  void advance(int nChoices);

  void select(int nChoice);

  boolean isDone();

  void setDone();

  boolean isProcessed();

  
  void reset();

  int getTotalNumberOfChoices();

  int getProcessedNumberOfChoices();

  


  
  T getChoice(int i);
  T[] getAllChoices();
  T[] getProcessedChoices();
  T[] getUnprocessedChoices();
  
  ChoiceGenerator<?> getPreviousChoiceGenerator();

  int getNumberOfParents();
  
  
  ChoiceGenerator<T> randomize();

  ChoiceGenerator<?> clone() throws CloneNotSupportedException;

  ChoiceGenerator<?> deepClone() throws CloneNotSupportedException; 
  
  String getId();

  int getIdRef();

  void setIdRef(int idRef);

  void setId(String id);

  boolean isSchedulingPoint();


  void setThreadInfo(ThreadInfo ti);

  ThreadInfo getThreadInfo();

  void setInsn(Instruction insn);

  Instruction getInsn();

  void setContext(ThreadInfo tiCreator);

  void setStateId (int stateId);
  
  int getStateId ();
  
  String getSourceLocation();

  boolean supportsReordering();
  
  
  ChoiceGenerator<T> reorder (Comparator<T> comparator);
  
  void setPreviousChoiceGenerator(ChoiceGenerator<?> cg);

  
  void setCascaded();

  boolean isCascaded();

  <T extends ChoiceGenerator<?>> T getPreviousChoiceGeneratorOfType(Class<T> cls);

  
  ChoiceGenerator<?> getCascadedParent();

  
  ChoiceGenerator<?>[] getCascade();

  
  ChoiceGenerator<?>[] getAll();

  
  <C extends ChoiceGenerator<?>> C[] getAllOfType(Class<C> cgType);



  boolean hasAttr();

  boolean hasAttr(Class<?> attrType);

  
  Object getAttr();

  
  void setAttr(Object a);

  void addAttr(Object a);

  void removeAttr(Object a);

  void replaceAttr(Object oldAttr, Object newAttr);

  
  <A> A getAttr(Class<A> attrType);

  <A> A getNextAttr(Class<A> attrType, Object prev);

  ObjectList.Iterator attrIterator();

  <A> ObjectList.TypedIterator<A> attrIterator(Class<A> attrType);

}
