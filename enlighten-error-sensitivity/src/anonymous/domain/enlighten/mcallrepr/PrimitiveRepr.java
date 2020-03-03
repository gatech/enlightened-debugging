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

import java.util.IdentityHashMap;
import java.util.Map;

public class PrimitiveRepr extends ValueGraphNode {
  
  private static final long serialVersionUID = 1L;

  private String type;
  private Object wrapped;

  public PrimitiveRepr(String type, Object val) {
    super();
    this.type = type;
    this.wrapped = val;
  }
  
  public String getType() {
    return type;
  }
  
  public Object getWrappedValue() {
    return wrapped;
  }
  
  public String toString() {
    if (type.equals("char")) {
      char value = ((Character) wrapped).charValue();
      if (!isAsciiPrintable(value) || Character.isWhitespace(value)) {
        return "0x" + Integer.toHexString(value).toUpperCase();
      }
    }
    return wrapped.toString();
  }

  @Override
  protected int getValueHashRecursive(IdentityHashMap<ValueGraphNode, Integer> visited) {
    return type.hashCode() ^ wrapped.hashCode();
  }
  
  private static boolean isAsciiPrintable(char ch) {
    return ch >= 32 && ch < 127;
}

  @Override
  public Map<MemberRefName, ValueGraphNode> getReferencedValues() {
    return null;
  }

  @Override
  public ValueGraphNode getReferencedValue(MemberRefName refName) {
    throw new NoSuchFieldError(refName.toString());
  }

  @Override
  public boolean hasReferencedValues() {
    return false;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o.getClass() == getClass()) {
      PrimitiveRepr theOther = (PrimitiveRepr) o;
      return theOther.type.equals(type) && theOther.wrapped.equals(wrapped);
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return type.hashCode() ^ wrapped.hashCode();
  }
}
