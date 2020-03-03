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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodCoverage;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.SourceLocationCoverage;
import instr.callback.memory.MemoryLocation;
import instr.staticinfo.MethodInfo;


public class DumpCoverageListener implements InstrumentationCallbackListener {

  private Path dataDir;
  
  private Set<MethodName> coveredMethods;
  
  public DumpCoverageListener(Path dataDir) {
    this.dataDir = dataDir;
  }
  
  @Override
  public void executionStarted(String executionId) {
    coveredMethods = new HashSet<>();
    SourceLocationCoverageCollector.resetCoverage();
  }

  @Override
  public void executionEnded(String executionId) {
    try {
      Path methodCoverageDataFile = dataDir.resolve(executionId + ".method.cov");
      MethodCoverage methodCoverage = new MethodCoverage();
      methodCoverage.addCoverage(coveredMethods);
      FSTSerialization.writeObjectTofile(
          MethodCoverage.class, methodCoverageDataFile, methodCoverage);
      Path lineCoverageDataFile = dataDir.resolve(executionId + ".line.cov");
      SourceLocationCoverage lineCoverage = new SourceLocationCoverage();
      lineCoverage.addCoverage(SourceLocationCoverageCollector.getCoveredSourceLocations());
      FSTSerialization.writeObjectTofile(
          SourceLocationCoverage.class, lineCoverageDataFile, lineCoverage);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to write coverage.", ex);
    }
  }

  @Override
  public void methodEntered(MethodName methodName) {
    coveredMethods.add(methodName);
  }

  @Override
  public void executingSourceLine(SourceLocation sourceLocation) {

  }

  @Override
  public void preStates(MethodInfo methodInfo, Object[] params) {

  }

  @Override
  public void methodExiting(MethodName methodName) {

  }

  @Override
  public void postStatesNormal(MethodInfo methodInfo, Object retValue,
      Object[] params) {

  }

  @Override
  public void methodExceptionExiting(MethodName methodName) {

  }

  @Override
  public void postStatesException(MethodInfo methodInfo, Object exception,
      Object[] params) {

  }

  @Override
  public void memoryRead(MemoryLocation location) {

  }

  @Override
  public void memoryWrite(MemoryLocation location) {

  }
}
