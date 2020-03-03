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


package instr.agent;

import java.io.IOException;
import java.nio.file.Path;

import anonymous.domain.enlighten.ExtProperties;

public class InstrumenterConfig extends ExtProperties {

  private static final long serialVersionUID = 1L;
  
  private InstrumenterType type;

  public static InstrumenterConfig readInstrumenterConfigFromFile(Path configFile) 
      throws IOException {
    InstrumenterConfig config = new InstrumenterConfig();
    config.loadFromFile(configFile.toFile());
    if (config.getDebuggedInvocation() == null) {
      config.type = InstrumenterType.TRACE_INSTRUMENTER;
    } else {
      config.type = InstrumenterType.DEBUGGEE_INSTRUMENTER;
    }
    return config;
  }
  
  private InstrumenterConfig() {}
  
  public InstrumenterConfig(InstrumenterType type) {
    this.type = type;
    switch (type) {
    case TRACE_INSTRUMENTER:
      setInstrumentedPackage("");
      instrumentLineNumber(true);
      instrumentMemoryAccess(true);
      instrumentStateCapture(true);
      break;
    case DEBUGGEE_INSTRUMENTER:
      setDebuggedInvocation("");
      break;
    }
  }
  
  public InstrumenterType getInstrumenterType() {
    return type;
  }
  
  public String getInstrumentedPackage() {
    return getProperty("instrumented_package");
  }
  
  public void setInstrumentedPackage(String packagePrefix) {
    checkType(InstrumenterType.TRACE_INSTRUMENTER);
    setProperty("instrumented_package", packagePrefix);
  }
  
  public boolean instrumentLineNumber() {
    return Boolean.parseBoolean(getProperty("instrument_line_number"));
  }
  
  public void instrumentLineNumber(boolean instrument) {
    checkType(InstrumenterType.TRACE_INSTRUMENTER);
    setProperty("instrument_line_number", instrument ? "true" : "false");
  }
  
  public boolean instrumentMemoryAccess() {
    return Boolean.parseBoolean(getProperty("instrument_memory_access"));
  }
  
  public void instrumentMemoryAccess(boolean instrument) {
    checkType(InstrumenterType.TRACE_INSTRUMENTER);
    setProperty("instrument_memory_access", instrument ? "true" : "false");
  }
  
  public boolean instrumentStateCapture() {
    return Boolean.parseBoolean(getProperty("instrument_state_capture"));
  }
  
  public void instrumentStateCapture(boolean instrument) {
    checkType(InstrumenterType.TRACE_INSTRUMENTER);
    setProperty("instrument_state_capture", instrument ? "true" : "false");
  }
  
  public void setDebuggedInvocation(String invocationDesc) {
    checkType(InstrumenterType.DEBUGGEE_INSTRUMENTER);
    setProperty("debugged_invocation", invocationDesc);
  }
  
  public String getDebuggedInvocation() {
    return getProperty("debugged_invocation");
  }
  
  private void checkType(InstrumenterType type) {
    
    if (this.type != type) {
      throw new RuntimeException("Setting property not in this type of instrumenter config.");
    }
  }
  
  public static enum InstrumenterType {
    TRACE_INSTRUMENTER,
    DEBUGGEE_INSTRUMENTER
  }
}
