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


package anonymous.domain.enlighten.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MethodName implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private static Map<MethodName, MethodName> createdMethodNames = new HashMap<>();

  private String className;
  private String simpleName; 
  private String signature;
  

  private String mNameSig; 
  
  public static MethodName get(String methodDesc) {
    int separatorIndex = methodDesc.lastIndexOf('.');
    if (separatorIndex <= 0 || separatorIndex >= methodDesc.length() - 1) {
      throw new IllegalArgumentException("\"" + methodDesc 
          + "\" is not a valid method name description string.");
    }
    String className = methodDesc.substring(0, separatorIndex);
    String mNameSig = methodDesc.substring(separatorIndex + 1);
    return get(className, mNameSig);
  }
  
  public static MethodName get(String className, String methodNameSig) {
    String simpleName = null;
    String signature = null;
    int sigSepIndex = methodNameSig.indexOf('(');
    if (sigSepIndex >= 0) {
      simpleName = methodNameSig.substring(0, sigSepIndex);
      signature = methodNameSig.substring(sigSepIndex);
    } else {
      simpleName = methodNameSig;
      signature = "";
    }
    return get(className, simpleName, signature);
  }
  
  public static MethodName get(String className, String simpleName, String signature) {

    MethodName methodName = new MethodName(className, simpleName, signature);
    if (createdMethodNames.containsKey(methodName)) {
      methodName = createdMethodNames.get(methodName);
    } else {
      createdMethodNames.put(methodName, methodName);
    }
    return methodName;
  }
  
  protected MethodName(String className, String simpleName, String signature) {
    this.className = className;
    this.simpleName = simpleName;
    this.signature = signature;
  }
  
  public String getClassName() {
    return className;
  }
  
  public void setClassName(String className) {
    this.className = className;
  }
  
  public String getMethodNameSig() {
    if (mNameSig == null) {
      mNameSig = simpleName + signature;
    }
    return mNameSig;
  }
  
  
  public String getMethodName() {
    return simpleName;
  }
  
  public String getMethodSignature() {
    return signature;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof MethodName) {
      return getClassName().equals(((MethodName) o).getClassName()) 
          && getMethodName().equals(((MethodName) o).getMethodName())
          && getMethodSignature().equals(((MethodName) o).getMethodSignature());
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return getClassName().hashCode() ^ getMethodNameSig().hashCode();
  }
  
  @Override
  public String toString() {
    return getClassName() + "." + getMethodNameSig();
  }
}
