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

import java.util.TreeMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ReflectedObjectRepr extends ReferenceRepr {
  
  private static final long serialVersionUID = 1L;
  
  private String type;
  private long id;
  private Map<String, ValueGraphNode> fields;
  private transient Map<MemberRefName, ValueGraphNode> referencedValues;
  
  ReflectedObjectRepr(String type, long id) {
    super();
    this.type = type;
    this.id = id;
    fields = new TreeMap<String, ValueGraphNode>();
  }
  
  public String getType() {
    return type;
  }
  
  public long getId() {
    return id;
  }
  
  public void putField(String name, ValueGraphNode value) {
    clearCachedHashCode();
    fields.put(name, value);
  }
  
  public Map<String, ValueGraphNode> getFields() {
    return Collections.unmodifiableMap(fields);
  }
  
  public ValueGraphNode getField(String fieldName) {
    return fields.get(fieldName);
  }
  
  @Override
  public Map<MemberRefName, ValueGraphNode> getReferencedValues() {
    if (referencedValues == null) {
      referencedValues = new HashMap<>();
      for (Map.Entry<String, ValueGraphNode> fieldEntry : fields.entrySet()) {
        referencedValues.put(new FieldReferenceName(
            fieldEntry.getKey()), fieldEntry.getValue());
      }
    }
    return referencedValues;
  }
  
  @Override
  public ValueGraphNode getReferencedValue(MemberRefName refName) {
    if (!(refName instanceof FieldReferenceName)) {
      throw new NoSuchFieldError(refName.toString() + " in type " + type);
    }
    String fieldName = ((FieldReferenceName) refName).getFieldName();
    if (!fields.containsKey(fieldName)) {
      throw new NoSuchFieldError(refName.toString() + " in type " + type);
    }
    return fields.get(fieldName);
  }

  @Override
  public boolean hasReferencedValues() {
    return true;
  }

  @Override
  protected int getValueHashRecursive(IdentityHashMap<ValueGraphNode, Integer> visited) {
    visited.put(this, 0);
    int hashCode = type.hashCode();
    for (Map.Entry<String, ValueGraphNode> fieldEntry : fields.entrySet()) {
      ValueGraphNode fieldValue = fieldEntry.getValue();
      if (fieldValue != null && !visited.containsKey(fieldValue)) {
        hashCode ^= Util.positionalShiftHashCode(
            fieldValue.getValueHashRecursive(visited), fieldEntry.getKey());
      }
    }
    return hashCode;
  }
}
