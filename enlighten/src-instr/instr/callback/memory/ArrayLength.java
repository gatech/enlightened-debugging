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

import java.util.IdentityHashMap;
import java.util.Map;

public class ArrayLength extends MemoryLocation {

  private static final String DISPLAY_NAME = "arrayLength";
  private static final int HASH_SIG = DISPLAY_NAME.hashCode();
  
  private static Map<Object, ArrayLength> cached = new IdentityHashMap<>();
  
  private Object arrayRef;
  
  public static ArrayLength get(Object arrayRef) {
    ArrayLength cachedInstance = cached.get(arrayRef);
    if (cachedInstance != null) {
      return cachedInstance;
    } else {
      ArrayLength newInstance = new ArrayLength(arrayRef);
      cached.put(arrayRef, newInstance);
      return newInstance;
    }
  }
  
  protected ArrayLength(Object arrayRef) {
    this.arrayRef = arrayRef;
  }
  
  @Override
  public Object getEnclosingObject() {
    return getArrayRef();
  }

  public Object getArrayRef() {
    return arrayRef;
  }

  public void setArrayRef(Object arrayRef) {
    this.arrayRef = arrayRef;
  }
  
  @Override
  public int hashCode() {
    return System.identityHashCode(arrayRef) ^ HASH_SIG;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    ArrayLength another = (ArrayLength) o;
    return arrayRef == another.arrayRef;
  }
  
  @Override
  public String toString() {
    return arrayRef.toString() + "." + DISPLAY_NAME;
  }
}
