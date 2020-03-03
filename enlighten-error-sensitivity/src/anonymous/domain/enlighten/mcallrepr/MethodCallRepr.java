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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class MethodCallRepr extends ValueGraphNode {

  private static final long serialVersionUID = 1L;
  
  private String testName;
  private String className;
  private String methodName;
  private ValueGraphNode thizz;
  private Map<String, ValueGraphNode> params = new HashMap<String, ValueGraphNode>();
  private ValueGraphNode returnVal;
  private ValueGraphNode exception;
  
  private transient Map<MemberRefName, ValueGraphNode> referencedValues;

  public MethodCallRepr(String testName, String className, String methodName) {
    super();
    this.testName = testName;
    this.className = className;
    this.methodName = methodName;
  }
  
  public String getTestName() {
    return testName;
  }
  
  public String getClassName() {
    return className;
  }
  
  public String getMethodName() {
    return methodName;
  }  
  
  public void setReturnVal(ValueGraphNode returnVal) {
    this.returnVal = returnVal;
  }
  
  public ValueGraphNode getReturnVal() {
    return returnVal;
  }

  public void setThizz(ValueGraphNode thizz) {
    this.thizz = thizz;
  }
  
  public ValueGraphNode getThizz() {
    return thizz;
  }
  
  public boolean isStatic() {
    return thizz == null;
  }

  public void setParam(String key, ValueGraphNode val) {
    params.put(key, val);
  }
  
  public Map<String, ValueGraphNode> getParams() {
    return params;
  }
  
  public ValueGraphNode getException() {
    return exception;
  }
  
  public void setException(ValueGraphNode exception) {
    this.exception = exception;
  }
  
  public boolean isEntry() {
    return returnVal == null && exception == null;
  }

  @Override
  protected int getValueHashRecursive(
      IdentityHashMap<ValueGraphNode, Integer> visited) {
    int hashCode = getClass().getName().hashCode();
    if (getThizz() != null) {
      hashCode ^= Util.positionalShiftHashCode(getThizz().getValueHash(), "this_pointer");
    }
    if (getReturnVal() != null) {
      hashCode ^= Util.positionalShiftHashCode(getReturnVal().getValueHash(), "return_value");
    }
    if (getException() != null) {
      hashCode ^= Util.positionalShiftHashCode(getException().getValueHash(), "exception_thrown");
    }
    for (Map.Entry<String, ValueGraphNode> paramEntry : getParams().entrySet()) {
      if (paramEntry.getValue() != null) {
        hashCode ^= Util.positionalShiftHashCode(
            paramEntry.getValue().getValueHash(), paramEntry.getKey());
      }
    }
    return hashCode;
  }

  @Override
  public Map<MemberRefName, ValueGraphNode> getReferencedValues() {
    if (referencedValues == null) {
      referencedValues = new HashMap<>();
      if (!isStatic()) {
        referencedValues.put(MethodCallSpecialRefName.thisRef(), thizz);
      }
      for (String paramName : params.keySet()) {
        referencedValues.put(
            MethodCallSpecialRefName.fromParamName(paramName), params.get(paramName));
      }
      if (returnVal != null) {
        referencedValues.put(MethodCallSpecialRefName.returnValue(), returnVal);
      } else if (exception != null) {
        referencedValues.put(MethodCallSpecialRefName.exceptionThrown(), exception);
      }
    }
    return referencedValues;
  }

  @Override
  public ValueGraphNode getReferencedValue(MemberRefName refName) {
    ValueGraphNode valueNode = getReferencedValues().get(refName);
    if (valueNode == null) {
      throw new NoSuchFieldError(refName.toString());
    }
    return valueNode;
  }

  @Override
  public boolean hasReferencedValues() {
    return true;
  }
}
