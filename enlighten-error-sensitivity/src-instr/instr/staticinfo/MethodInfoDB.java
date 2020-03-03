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


package instr.staticinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.data.MethodName;

public class MethodInfoDB {

  private static List<MethodInfo> methodInfos = new ArrayList<>();
  private static Map<MethodName, Integer> infoNumMapping = new HashMap<>();
  
  public synchronized static boolean contains(MethodName method) {
    return infoNumMapping.containsKey(method);
  }
  
  public synchronized static int addMethodInfo(MethodInfo methodInfo) {
    if (contains(methodInfo.getMethodName())) {
      throw new RuntimeException("Adding an existing method info entry.");
    }
    methodInfos.add(methodInfo);
    int methodInfoId = methodInfos.size() - 1;
    infoNumMapping.put(methodInfo.getMethodName(), methodInfoId);
    return methodInfoId;
  }
  
  public synchronized static int getMethodInfoId(MethodName methodName) {
    if (!contains(methodName)) {
      return -1;
    } else {
      return infoNumMapping.get(methodName);
    }
  }
  
  public synchronized static MethodInfo getMethodInfoById(int methodInfoId) {
    return methodInfos.get(methodInfoId);
  }
}
