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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ArrayRepr extends ReferenceRepr {
  
  private static final long serialVersionUID = 1L;
  
  private String type;
  private long id;
  private PrimitiveRepr length;
  private List<ValueGraphNode> elements = new ArrayList<>();
  
  private transient Map<MemberRefName, ValueGraphNode> referencedValues;
  
  public ArrayRepr(String type, long id) {
    this.type = type;
    this.id = id;
  }
  
  public String getType() {
    return type;
  }
  
  public long getId() {
    return id;
  }
  
  public void setElements(PrimitiveRepr length, List<ValueGraphNode> elements) {
    if (!length.getType().equals("int")) {
      throw new RuntimeException("Type of array length should be int");
    } else {
      int iLength = (Integer) length.getWrappedValue();
      if (iLength != elements.size()) {
        throw new RuntimeException("Array lengt does not match size of elements.");
      }
    }
    this.length = length;
    this.elements = new ArrayList<>(elements);
  }
  
  public PrimitiveRepr getLength() {
    return length;
  }
  
  public List<ValueGraphNode> getElements() {
    return Collections.unmodifiableList(elements);
  }
  
  @Override
  public Map<MemberRefName, ValueGraphNode> getReferencedValues() {
    if (referencedValues == null) {
      referencedValues = new HashMap<>();
      referencedValues.put(ArrayLengthRefName.get(), getLength());
      for (int index = 0; index < elements.size(); ++index) {
        referencedValues.put(new ArrayElementRefName(index), elements.get(index));
      }
    }
    return referencedValues;
  }
  
  @Override
  public ValueGraphNode getReferencedValue(MemberRefName refName) {
    if (refName == ArrayLengthRefName.get()) {
      return length;
    }
    if (!(refName instanceof ArrayElementRefName)) {
      throw new NoSuchFieldError(refName.toString());
    }
    return elements.get(((ArrayElementRefName) refName).getIndex());
  }

  @Override
  public boolean hasReferencedValues() {
    return true;
  }

  @Override
  protected int getValueHashRecursive(
      IdentityHashMap<ValueGraphNode, Integer> visited) {
    visited.put(this, 0);
    int hashCode = type.hashCode();
    hashCode ^= Util.positionalShiftHashCode(
        length.getValueHashRecursive(visited), "arrayLength");
    for (int index = 0; index < elements.size(); ++index) {
      ValueGraphNode element = elements.get(index);
      if (element != null && !visited.containsKey(element)) {
        hashCode ^= Util.positionalShiftHashCode(
            element.getValueHashRecursive(visited), "array_index_" + index);
      }
    }
    return hashCode;
  }
}
