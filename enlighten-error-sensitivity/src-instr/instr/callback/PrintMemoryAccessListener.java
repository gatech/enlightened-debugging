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


package instr.callback;

import java.util.HashSet;
import java.util.Set;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import instr.callback.memory.ArrayBucket;
import instr.callback.memory.MemoryLocation;
import instr.staticinfo.MethodInfo;

public class PrintMemoryAccessListener implements InstrumentationCallbackListener {
  
  private Set<MemoryLocation> memoryRead;
  private Set<MemoryLocation> memoryWrite;

  @Override
  public void executionStarted(String executionId) {
    System.out.println("==============Test " + executionId + " started=================");
    memoryRead = new HashSet<>();
    memoryWrite = new HashSet<>();
  }

  @Override
  public void methodEntered(MethodName methodName) {
  }
  
  @Override
  public void preStates(MethodInfo methodInfo, Object[] params) {
  }

  @Override
  public void methodExiting(MethodName methodName) {
  }
  
  @Override
  public void postStatesNormal(
      MethodInfo methodInfo, Object retValue, Object[] params) {
  }

  @Override
  public void methodExceptionExiting(MethodName methodName) {
  }
  
  @Override
  public void postStatesException(
      MethodInfo methodInfo, Object exception, Object[] params) {
  }

  @Override
  public void executingSourceLine(SourceLocation sourceLocation) {
  }

  @Override
  public void executionEnded(String executionId) {
    System.out.println("==============Test " + executionId + " ended=================");
    System.out.println("==============Read locations: " + memoryRead.size() + "=================");
    System.out.println("==============Write locations: " + memoryWrite.size() + "=================");
  }

  @Override
  public void memoryRead(MemoryLocation location) {
    System.out.print("Memory read at " + location.toString());
    if (location instanceof ArrayBucket) {
      Object arrayRef = ((ArrayBucket) location).getArrayRef();
      if (arrayRef instanceof char[]) {
        System.out.println(": " + new String((char[]) arrayRef).trim());
      } else if (arrayRef instanceof byte[]) {
        System.out.println(": " + new String((byte[]) arrayRef).trim());
      } else {
        System.out.println();
      }
    } else {
      System.out.println();
    }
    memoryRead.add(location);
  }

  @Override
  public void memoryWrite(MemoryLocation location) {
    System.out.print("Memory write at " + location.toString());
    if (location instanceof ArrayBucket) {
      Object arrayRef = ((ArrayBucket) location).getArrayRef();
      if (arrayRef instanceof char[]) {
        System.out.println(": " + new String((char[]) arrayRef).trim());
      } else if (arrayRef instanceof byte[]) {
        System.out.println(": " + new String((byte[]) arrayRef).trim());
      } else {
        System.out.println();
      }
    } else {
      System.out.println();
    }
    memoryWrite.add(location);
  }

}
