/* 
 * Copyright 2019 Georgia Institute of Technology
 * All rights reserved.
 *
 * Author(s): Xiangyu Li <xiangyu.li@cc.gatech.edu>
 *
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package anonymous.domain.enlighten.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.annotation.Annotatable;
import anonymous.domain.enlighten.annotation.DefaultAnnotationList;
import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;

public class MethodInvocation implements Serializable, Annotatable {

  private static final long serialVersionUID = 1L;
  
  private MethodName methodName;
  private Map<SourceLocation, Integer> statementsExecCountMap;

  private MethodInvocation enclosingInvocation;
  private List<MethodInvocation> enclosedInvocations;
  
  private MethodCallRepr preState;
  private MethodCallRepr postState;
  
  private int numMemoryReadLocations;
  private int numMemoryReadObjects;
  private int numMemoryWriteLocations;
  private int numMemoryWriteObjects;

  private DefaultAnnotationList annotations = new DefaultAnnotationList();
  
  public MethodInvocation(MethodName methodName) {
    this.methodName = methodName;
    statementsExecCountMap = new HashMap<>();
    enclosedInvocations = new ArrayList<>();
  }

  public MethodName getMethodName() {
    return methodName;
  }
  
  public MethodCallRepr getPreState() {
    return preState;
  }

  public void setPreState(MethodCallRepr preState) {
    this.preState = preState;
  }

  public MethodCallRepr getPostState() {
    return postState;
  }

  public void setPostState(MethodCallRepr postState) {
    this.postState = postState;
  }
  
  public MethodInvocation getEnclosingInvocation() {
    return enclosingInvocation;
  }
  
  public List<MethodInvocation> getEnclosedInvocations() {
    return enclosedInvocations;
  }
  
  public void addEnclosedInvocation(MethodInvocation invocation) {
    invocation.enclosingInvocation = this;
    enclosedInvocations.add(invocation);
  }
  
  public void removeFromEnclosingInvocation() {
    if (enclosingInvocation == null) {
      return;
    }
    List<MethodInvocation> siblings = enclosingInvocation.getEnclosedInvocations();
    for (int i = 0; i < siblings.size(); ++i) {
      if (siblings.get(i) == this) {
        siblings.remove(i);
        return;
      }
    }
    throw new RuntimeException("Internal error: method invocation tree is corrupted.");
  }
  
  public Map<SourceLocation, Integer> getStatementsExecCountMap() {
    return statementsExecCountMap;
  }

  public void addExecutionCount(SourceLocation sourceLocation) {
    if (statementsExecCountMap.containsKey(sourceLocation)) {
      statementsExecCountMap.put(sourceLocation, statementsExecCountMap.get(sourceLocation) + 1);
    } else {
      statementsExecCountMap.put(sourceLocation, 1);
    }
  }
  
  public void setStatementsExecCountMap(Map<SourceLocation, Integer> statementsExecCountMap) {
    this.statementsExecCountMap = statementsExecCountMap;
  }

  public int getNumMemoryReadLocations() {
    return numMemoryReadLocations;
  }
  
  public void setNumMemoryReadLocations(int numLocations) {
    numMemoryReadLocations = numLocations;
  }
  
  public int getNumMemoryReadObjects() {
    return numMemoryReadObjects;
  }
  
  public void setNumMemoryReadObjects(int numObjects) {
    numMemoryReadObjects = numObjects;
  }
  
  public int getNumMemoryWriteLocations() {
    return numMemoryWriteLocations;
  }
  
  public void setNumMemoryWriteLocations(int numLocations) {
    numMemoryWriteLocations = numLocations;
  }
  
  public int getNumMemoryWriteObjects() {
    return numMemoryWriteObjects;
  }
  
  public void setNumMemoryWriteObjects(int numObjects) {
    numMemoryWriteObjects = numObjects;
  }
  
  @Override
  public void addAnnotation(ValueAnnotation annotation) {
    annotations.addAnnotation(annotation);
  }
  
  @Override
  public <T extends ValueAnnotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }
  
  @Override
  public boolean removeAnnotation(ValueAnnotation annotation) {
    return annotations.removeAnnotation(annotation);
  }
  
  @Override
  public boolean removeAnnotation(Class<? extends ValueAnnotation> annotationClass) {
    return annotations.removeAnnotation(annotationClass);
  }
}
