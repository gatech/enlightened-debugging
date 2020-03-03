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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public abstract class InfoObject implements Cloneable {

  static AnnotationInfo[] NO_ANNOTATIONS = new AnnotationInfo[0];
  static AbstractTypeAnnotationInfo[] NO_TYPE_ANNOTATIONS = new AbstractTypeAnnotationInfo[0];
  


  protected AnnotationInfo[] annotations = NO_ANNOTATIONS;

  protected AbstractTypeAnnotationInfo[] typeAnnotations = NO_TYPE_ANNOTATIONS;
  
  
  protected Object attr;
  
  
  public void setAnnotations (AnnotationInfo[] annotations){
    this.annotations = annotations;
  }
  
  public void addAnnotations (AnnotationInfo[] annotations){
    if (annotations == null){
      this.annotations = annotations;
    } else {
      AnnotationInfo[] newAi = new AnnotationInfo[this.annotations.length + annotations.length];
      System.arraycopy(this.annotations,0,newAi, 0, this.annotations.length);
      System.arraycopy(annotations, 0, newAi, this.annotations.length, annotations.length);
      this.annotations = newAi;
    }
  }
  
  public void addAnnotation (AnnotationInfo newAnnotation){
    AnnotationInfo[] ai = annotations;
    if (ai == null){
      ai = new AnnotationInfo[1];
      ai[0] = newAnnotation;

    } else {
      int len = annotations.length;
      ai = new AnnotationInfo[len+1];
      System.arraycopy(annotations, 0, ai, 0, len);
      ai[len] = newAnnotation;
    }

    annotations = ai;
  }


  public boolean hasAnnotations(){
    return (annotations != NO_ANNOTATIONS);
  }
  

  public AnnotationInfo[] getAnnotations() {
    return annotations;
  }
      

  public AnnotationInfo getAnnotation (String name){    
    AnnotationInfo[] ai = annotations;
    if (ai != NO_ANNOTATIONS){
      for (int i=0; i<ai.length; i++){
        if (ai[i].getName().equals(name)){
          return ai[i];
        }
      }
    }
    return null;
  }
  
  public boolean hasAnnotation (String name){
    return getAnnotation(name) != null;    
  }
  
  public AnnotationInfo[] getDeclaredAnnotations(){
    return annotations;
  }


  
  public void setTypeAnnotations (AbstractTypeAnnotationInfo[] typeAnnotations){
    this.typeAnnotations = typeAnnotations;
  }

  public void addTypeAnnotations (AbstractTypeAnnotationInfo[] tas){
    if (typeAnnotations == NO_TYPE_ANNOTATIONS){
      typeAnnotations = tas;
      
    } else {
      int oldLen = typeAnnotations.length;
      AbstractTypeAnnotationInfo[] newTA = new AbstractTypeAnnotationInfo[oldLen + tas.length];
      System.arraycopy(typeAnnotations, 0, newTA, 0, oldLen);
      System.arraycopy(tas, 0, newTA, oldLen, tas.length);
      typeAnnotations = newTA;
    }
  }
  
  public void addTypeAnnotation (AbstractTypeAnnotationInfo newAnnotation){
    AbstractTypeAnnotationInfo[] ai = typeAnnotations;
    if (ai == null){
      ai = new AbstractTypeAnnotationInfo[1];
      ai[0] = newAnnotation;

    } else {
      int len = annotations.length;
      ai = new AbstractTypeAnnotationInfo[len+1];
      System.arraycopy(annotations, 0, ai, 0, len);
      ai[len] = newAnnotation;
    }

    typeAnnotations = ai;
  }

  
  public AbstractTypeAnnotationInfo[] getTypeAnnotations() {
    return typeAnnotations;
  }
  
  public boolean hasTypeAnnotations(){
    return (typeAnnotations != NO_TYPE_ANNOTATIONS);    
  }
  
  public boolean hasTypeAnnotation (String name){
    return getTypeAnnotation(name) != null;    
  }
  
  public AbstractTypeAnnotationInfo getTypeAnnotation (String annoClsName){    
    AbstractTypeAnnotationInfo[] ai = typeAnnotations;
    if (ai != NO_TYPE_ANNOTATIONS){
      for (int i=0; i<ai.length; i++){
        if (ai[i].getName().equals(annoClsName)){
          return ai[i];
        }
      }
    }
    return null;
  }

  public <T extends AbstractTypeAnnotationInfo> List<T> getTargetTypeAnnotations (Class<T> targetType){
    List<T> list = null;
    
    AbstractTypeAnnotationInfo[] ais = typeAnnotations;
    if (ais != NO_TYPE_ANNOTATIONS){
      for (AbstractTypeAnnotationInfo ai : ais){
        if (targetType.isAssignableFrom(ai.getClass())){
          if (list == null){
            list = new ArrayList();
          }
          list.add((T)ai);
        }
      }
    }
    
    if (list != null){
      return list;
    } else {
      return Collections.emptyList();
    }
  }
  


  public boolean hasAttr () {
    return (attr != null);
  }

  public boolean hasAttr (Class<?> attrType){
    return ObjectList.containsType(attr, attrType);
  }

  public boolean hasAttrValue (Object a){
    return ObjectList.contains(attr, a);
  }
  
  
  public Object getAttr(){
    return attr;
  }

  
  public void setAttr (Object a){
    attr = a;    
  }

  public void addAttr (Object a){
    attr = ObjectList.add(attr, a);
  }

  public void removeAttr (Object a){
    attr = ObjectList.remove(attr, a);
  }

  public void replaceAttr (Object oldAttr, Object newAttr){
    attr = ObjectList.replace(attr, oldAttr, newAttr);
  }

  
  public <T> T getAttr (Class<T> attrType) {
    return ObjectList.getFirst(attr, attrType);
  }

  public <T> T getNextAttr (Class<T> attrType, Object prev) {
    return ObjectList.getNext(attr, attrType, prev);
  }

  public ObjectList.Iterator attrIterator(){
    return ObjectList.iterator(attr);
  }
  
  public <T> ObjectList.TypedIterator<T> attrIterator(Class<T> attrType){
    return ObjectList.typedIterator(attr, attrType);
  }
}
