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


package anonymous.domain.enlighten.refpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import anonymous.domain.enlighten.mcallrepr.ArrayElementRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayLengthRefName;
import anonymous.domain.enlighten.mcallrepr.FieldReferenceName;
import anonymous.domain.enlighten.mcallrepr.MemberRefName;
import anonymous.domain.enlighten.mcallrepr.MethodCallSpecialRefName;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.util.StringUtils;

public class RefPath implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  
  private List<MemberRefName> components;
  
  public static Builder newBuilder() {
    return new Builder();
  }
  
  public RefPath() {
    components = new ArrayList<>();
  }
  
  public RefPath(List<MemberRefName> components) {
    this.components = components;
  }
  
  public Builder append() {
    return new Builder(this);
  }
  
  public ValueGraphNode getValue(ValueGraphNode startNode) {
    ValueGraphNode currentNode = startNode;
    for (MemberRefName component : components) {
      currentNode = currentNode.getReferencedValue(component);
    }
    return currentNode;
  }
  

  public ValueGraphNode getLastButOne(ValueGraphNode startNode) {
    if (components.size() == 0) {
      throw new ArrayIndexOutOfBoundsException(-1);
    }
    ValueGraphNode currentNode = startNode;
    for (int i = 0; i < components.size() - 1; ++i) {
      currentNode = currentNode.getReferencedValue(components.get(i));
    }
    return currentNode;
  }
  
  public List<MemberRefName> getComponents() {
    return Collections.unmodifiableList(components);
  }
  
  public int getLength() {
    return components.size();
  }
  
  public MemberRefName getTail() {
    return components.get(components.size() - 1);
  }
  
  public RefPath getParent() {
    if (components.size() == 0) {
      return null;
    }
    RefPath parentPath = new RefPath(new ArrayList<>(components));
    parentPath.components.remove(components.size() - 1);
    return parentPath;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o.getClass() == getClass()) {
      RefPath theOther = (RefPath) o;
      return components.equals(theOther.components);
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return components.hashCode();
  }
  
  @Override
  public String toString() {
    return StringUtils.concat(components.stream().map(
        component -> { return component.toString(); }).collect(Collectors.toList()), ".");
  }
  
  public static class Builder {
    
    private List<MemberRefName> tempComponents;
    
    public Builder() {
      tempComponents = new ArrayList<>();
    }
    
    public Builder(RefPath prefix) {
      tempComponents = new ArrayList<>(prefix.components);
    }
    
    public RefPath build() {
      RefPath refPath = new RefPath(tempComponents);
      return refPath;
    }
    
    public Builder appendMemberRefName(MemberRefName refName) {
      tempComponents.add(refName);
      return this;
    }
    
    public Builder appendFieldNameRef(String fieldName) {
      tempComponents.add(new FieldReferenceName(fieldName));
      return this;
    }
    
    public Builder appendArrayIndexRef(int index) {
      tempComponents.add(new ArrayElementRefName(index));
      return this;
    }
    
    public Builder appendArrayLengthRef() {
      tempComponents.add(ArrayLengthRefName.get());
      return this;
    }
    
    public Builder appendThisRef() {
      tempComponents.add(MethodCallSpecialRefName.thisRef());
      return this;
    }
    
    public Builder appendParamRef(String paramName) {
      tempComponents.add(MethodCallSpecialRefName.fromParamName(paramName));
      return this;
    }
    
    public Builder appendReturnValueRef() {
      tempComponents.add(MethodCallSpecialRefName.returnValue());
      return this;
    }
    
    public Builder appendExceptionRef() {
      tempComponents.add(MethodCallSpecialRefName.exceptionThrown());
      return this;
    }
  }
}
