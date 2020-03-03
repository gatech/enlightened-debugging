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

public class VoidRepr extends ValueGraphNode {
  
  private static final long serialVersionUID = 1L;
  
  private static final int valueHash = "VoidRepr".hashCode();
  private static final VoidRepr instance = new VoidRepr();
  
  public static VoidRepr get() {
    return instance;
  }
  
  private VoidRepr() {}

  @Override
  protected int getValueHashRecursive(IdentityHashMap<ValueGraphNode, Integer> visited) {
    return valueHash;
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
  public String toString() {
    return "void";
  }
  
  @Override
  public boolean equals(Object o) {
    return o.getClass() == getClass();
  }
  
  @Override
  public int hashCode() {
    return "void".hashCode();
  }
}
