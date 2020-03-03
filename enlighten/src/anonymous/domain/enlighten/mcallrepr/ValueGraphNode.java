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


package anonymous.domain.enlighten.mcallrepr;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;

import anonymous.domain.enlighten.annotation.Annotatable;
import anonymous.domain.enlighten.annotation.DefaultAnnotationList;
import anonymous.domain.enlighten.annotation.ValueAnnotation;

public abstract class ValueGraphNode implements Serializable, Annotatable {
  
  private static final long serialVersionUID = 1L;
  
  private DefaultAnnotationList annotations = new DefaultAnnotationList();
  
  private Integer cachedHashCode;
  
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
  
  public int getValueHash() {
    if (cachedHashCode == null) {
      cachedHashCode = getValueHashRecursive(new IdentityHashMap<ValueGraphNode, Integer>());
    }
    return cachedHashCode;
  }
  
  protected void clearCachedHashCode() {
    cachedHashCode = null;
  }
  
  protected abstract int getValueHashRecursive(IdentityHashMap<ValueGraphNode, Integer> visited);
  
  public abstract Map<MemberRefName, ValueGraphNode> getReferencedValues();
  
  public abstract ValueGraphNode getReferencedValue(MemberRefName refName);
  
  public abstract boolean hasReferencedValues();
}
