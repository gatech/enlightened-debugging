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

public class StaticField extends MemoryLocation {
  
  private static Map<StaticField, StaticField> cached = new HashMap<>();

  private String className;
  private String fieldName;
  
  private transient Class<?> cachedClassObject;
  
  public static StaticField get(String className, String fieldName) {
    StaticField newInstance = new StaticField(className, fieldName);
    StaticField cachedInstance = cached.get(newInstance);
    if (cachedInstance != null) {
      return cachedInstance;
    } else {
      cached.put(newInstance, newInstance);
      return newInstance;
    }
  }
  
  protected StaticField(String className, String fieldName) {
    this.className = className;
    this.fieldName = fieldName;
  }
  
  @Override
  public Object getEnclosingObject() {
    if (cachedClassObject == null) {
      try {
        cachedClassObject = Class.forName(className.replace("/", "."));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return cachedClassObject;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  
  @Override
  public int hashCode() {
    return className.hashCode() ^ fieldName.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof StaticField) {
      StaticField another = (StaticField) o;
      return className.equals(another.className) && fieldName.equals(another.fieldName);
    }
    return false;
  }
  
  @Override
  public String toString() {
    return className + "." + fieldName;
  }
}
