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


package instr.callback.memory;

import java.util.HashMap;
import java.util.Map;

public class InstanceField extends MemoryLocation {
  
  private static Map<InstanceField, InstanceField> cached = new HashMap<>();

  private Object objRef;
  private String fieldName;
  
  public static InstanceField get(Object objRef, String fieldName) {
    InstanceField newInstance = new InstanceField(objRef, fieldName);
    InstanceField cachedInstance = cached.get(newInstance);
    if (cachedInstance != null) {
      return cachedInstance;
    } else {
      cached.put(newInstance, newInstance);
      return newInstance;
    }
  }
  
  protected InstanceField(Object objRef, String fieldName) {
    this.objRef = objRef;
    this.fieldName = fieldName;
  }
  
  @Override
  public Object getEnclosingObject() {
    return getObjRef();
  }

  public Object getObjRef() {
    return objRef;
  }

  public void setObjRef(Object objRef) {
    this.objRef = objRef;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  
  @Override
  public int hashCode() {
    return System.identityHashCode(objRef) & fieldName.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof InstanceField) {
      InstanceField another = (InstanceField) o;
      return objRef == another.objRef && fieldName.equals(another.fieldName);
    }
    return false;
  }
  
  @Override
  public String toString() {
    return objRef.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(objRef))
        + "." + fieldName;
  }
}
