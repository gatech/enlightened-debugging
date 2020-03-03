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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecutionProfile implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private String executionId;
  private MethodInvocation methodInvocationTreeRoot;
  


  private transient Map<MethodName, List<MethodInvocation>> invocationIndexMap;
  private transient Map<SourceLocation, List<MethodInvocation>> sourceCoverageInvocationMap;
  private transient int numInvocations;
  



  public ExecutionProfile(String executionId, MethodInvocation invocationRoot) {
    this.executionId = executionId;
    methodInvocationTreeRoot = invocationRoot;
    refreshInvocationIndexMap();
  }
  
  public String getExecutionId() {
    return executionId;
  }
  
  public MethodInvocation getInvocationTreeRoot() {
    return methodInvocationTreeRoot;
  }
  
  public int getNumInvocations() {
    return numInvocations;
  }
  
  public Set<MethodName> getInvokedMethods() {
    return invocationIndexMap.keySet();
  }
  
  public List<MethodInvocation> lookupInvocation(MethodName method) {
    return invocationIndexMap.get(method);
  }
  
  public MethodInvocation lookupInvocation(MethodName method, int invocationSerialNum) {
    List<MethodInvocation> invocations = lookupInvocation(method);
    if (invocations != null && invocationSerialNum >= 0 && invocationSerialNum < invocations.size()) {
      return invocations.get(invocationSerialNum);
    } else {
      return null;
    }
  }
  
  public int lookupInvocationIndex(MethodInvocation invocation) {
    return lookupInvocation(invocation.getMethodName()).indexOf(invocation);
  }
  
  public List<MethodInvocation> lookupInvocation(SourceLocation sourceLocation) {
    if (sourceCoverageInvocationMap.containsKey(sourceLocation)) {
      return sourceCoverageInvocationMap.get(sourceLocation);
    } else {
      return Collections.emptyList();
    }
  }
  
  public void refreshInvocationIndexMap() {
    invocationIndexMap = new HashMap<>();
    sourceCoverageInvocationMap = new HashMap<>();
    numInvocations = 0;
    traverseAndAddInvocations(methodInvocationTreeRoot);
  }
  
  public void writeToDataFile(Path dataFile) throws IOException {
    FSTSerialization.writeObjectTofile(ExecutionProfile.class, dataFile, this);
  }
  
  public static ExecutionProfile readFromDataFile(Path dataFile) throws IOException {
    return FSTSerialization.readObjectFromFile(ExecutionProfile.class, dataFile);
  }
  
  private void traverseAndAddInvocations(MethodInvocation root) {
    ++numInvocations;
    List<MethodInvocation> currentMethodInvocations = invocationIndexMap.get(root.getMethodName());
    if (currentMethodInvocations == null) {
      currentMethodInvocations = new ArrayList<>();
      invocationIndexMap.put(root.getMethodName(), currentMethodInvocations);
    }
    currentMethodInvocations.add(root);
    if (root.getStatementsExecCountMap() != null) {
      for (SourceLocation sourceLocation : root.getStatementsExecCountMap()
          .keySet()) {
        List<MethodInvocation> currentSourceLocationInvocations = sourceCoverageInvocationMap
            .get(sourceLocation);
        if (currentSourceLocationInvocations == null) {
          currentSourceLocationInvocations = new ArrayList<>();
          sourceCoverageInvocationMap.put(sourceLocation,
              currentSourceLocationInvocations);
        }
        currentSourceLocationInvocations.add(root);
      }
    }
    for (MethodInvocation enclosedInvocation : root.getEnclosedInvocations()) {
      traverseAndAddInvocations(enclosedInvocation);
    }
  }
  
  private Object readResolve() {
    refreshInvocationIndexMap();
    return this;
  }
}
