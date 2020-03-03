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

import instr.callback.memory.MemoryLocation;
import instr.staticinfo.MethodInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;

public abstract class ExecutionEventAdapterListener implements InstrumentationCallbackListener {

  private static Map<String, Event> eventCache = new HashMap<String, Event>();
  
  public abstract void methodEntered(Event event);
  
  public abstract void methodExiting(Event event);
  
  public abstract void methodExceptionExiting(Event event);
  
  public abstract void executingSourceLine(Event event);
  
  protected void dumpEventsToFile(Iterable<Event> events, Path dataFile) {
    StringBuilder contentBuilder = new StringBuilder();
    for (Event event : events) {
      contentBuilder.append(event + "\n");
    }
    try {
      Files.createDirectories(dataFile.getParent());
      Files.write(dataFile, contentBuilder.toString().getBytes());
    } catch (IOException e) {
      throw new RuntimeException("Cannot write data file " + dataFile.toString(), e);
    }
  }

  @Override
  public void methodEntered(MethodName methodName) {
    methodEntered(getEvent(methodName.toString(), Event.Kind.ENTRY));
  }
  
  @Override
  public void preStates(MethodInfo methodInfo, Object[] params) {

  }

  @Override
  public void methodExiting(MethodName methodName) {
    methodExiting(getEvent(methodName.toString(), Event.Kind.EXIT));
  }
  
  @Override
  public void postStatesNormal(
      MethodInfo methodInfo, Object retValue, Object[] params) {

  }

  @Override
  public void methodExceptionExiting(MethodName methodName) {
    methodExceptionExiting(getEvent(methodName.toString(), Event.Kind.EXCEPTION_EXIT));
  }
  
  @Override
  public void postStatesException(
      MethodInfo methodInfo, Object exception, Object[] params) {

  }

  @Override
  public void executingSourceLine(SourceLocation sourceLocation) {
    executingSourceLine(getEvent(sourceLocation.toString(), Event.Kind.LINE));
  }
  
  private static Event getEvent(String m, Event.Kind entry) {
    String key = m + entry.toString();
    Event ev = eventCache.get(key);
    if(ev == null) { 
      ev = new Event(m, entry);
      eventCache.put(key, ev);
    }    
    return ev;
  }
  
  
  protected static class Event {
    public final String message;
    public final Kind kind;
    public enum Kind { ENTRY, EXIT, LINE, EXCEPTION_EXIT }
    public Event(String m, Kind k) {
      this.message = m;
      this.kind = k;
    };
    public String toString() {
      return message + "\t" + kind;
    }
    public static Event parseEvent(String str) {
      Kind kind; 
      int idx;
      if ((idx = str.indexOf(""+Kind.ENTRY)) != -1) {
        kind = Kind.ENTRY;
      } else if ((idx = str.indexOf(""+Kind.EXIT)) != -1) {
        kind = Kind.EXIT;
      } else if ((idx = str.indexOf("" + Kind.EXCEPTION_EXIT)) != -1) {
        kind = Kind.EXCEPTION_EXIT;
      } else {
        throw new RuntimeException();
      }
      return new Event(str.substring(0, idx).trim(), kind);
    }
    
    private int hash;
    public int hashCode() {
      if (hash == 0) {
       hash = (message + kind.toString()).hashCode();
      }
      return hash;
    }
  }
  
  @Override
  public void memoryRead(MemoryLocation location) {}
  
  @Override
  public void memoryWrite(MemoryLocation location) {}
}
