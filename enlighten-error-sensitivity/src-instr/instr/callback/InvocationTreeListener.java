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
import instr.callback.memory.StaticField;
import instr.staticinfo.MethodInfo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.mcallrepr.ObjectFieldIterationUtil;

public class InvocationTreeListener implements InstrumentationCallbackListener {
  
  private Path dataDir;
  
  private MethodInvocation invocationRoot;
  
  private MethodInvocation currentInvocation;
  private Stack<Set<MemoryLocation>> memoryReadLocations;
  private Stack<Set<MemoryLocation>> memoryWriteLocations;
  
  private List<Object> potentialOutputObjects;
  
  public InvocationTreeListener(Path dataDir) {
    this.dataDir = dataDir;
  }
  
  @Override
  public void executionStarted(String executionId) {

    invocationRoot = new MethodInvocation(MethodName.get("ExecStart", executionId));
    currentInvocation = invocationRoot;
    memoryReadLocations = new Stack<>();
    memoryWriteLocations = new Stack<>();
  }

  @Override
  public void methodEntered(MethodName methodName) {
    MethodInvocation invocationNode = new MethodInvocation(methodName);
    currentInvocation.addEnclosedInvocation(invocationNode);
    currentInvocation = invocationNode;
    memoryReadLocations.push(new HashSet<MemoryLocation>());
    memoryWriteLocations.push(new HashSet<MemoryLocation>());
  }
  
  @Override
  public void preStates(MethodInfo methodInfo, Object[] params) {
  }

  @Override
  public void methodExiting(MethodName methodName) {

    if (!currentInvocation.getMethodName().equals(methodName)) {
      System.err.println("Method entry/exit event does not match.");
      System.exit(1);
    }
    Set<MemoryLocation> currentInvocationMemRead = memoryReadLocations.pop();
    Set<MemoryLocation> currentInvocationMemWrite = memoryWriteLocations.pop();
    if (potentialOutputObjects != null) {
      filterMemoryLocationsByReachableObjects(currentInvocationMemWrite, potentialOutputObjects);
    } else {
      System.err.println("Warning: output memory locations not filtered in method " + methodName);
    }
    currentInvocation.setNumMemoryReadLocations(currentInvocationMemRead.size());
    currentInvocation.setNumMemoryWriteLocations(currentInvocationMemWrite.size());
    Map<Object, Integer> memReadObjects = new IdentityHashMap<>();
    Map<Object, Integer> memWriteObjects = new IdentityHashMap<>();
    for (MemoryLocation memReadLocation : currentInvocationMemRead) {
      memReadObjects.put(memReadLocation.getEnclosingObject(), 1);
    }
    for (MemoryLocation memWriteLocation : currentInvocationMemWrite) {
      memWriteObjects.put(memWriteLocation.getEnclosingObject(), 1);
    }
    currentInvocation.setNumMemoryReadObjects(memReadObjects.size());
    currentInvocation.setNumMemoryWriteObjects(memWriteObjects.size());
    currentInvocation = currentInvocation.getEnclosingInvocation();
    if (currentInvocation != invocationRoot) {
      memoryReadLocations.peek().addAll(currentInvocationMemRead);
      memoryWriteLocations.peek().addAll(currentInvocationMemWrite);
    }
  }
  
  @Override
  public void postStatesNormal(
      MethodInfo methodInfo, Object retValue, Object[] params) {
    potentialOutputObjects = new ArrayList<>();
    if (retValue != null) {
      potentialOutputObjects.add(retValue);
    }
    for (Object param : params) {
      if (param != null) {
        potentialOutputObjects.add(param);
      }
    }
  }

  @Override
  public void methodExceptionExiting(MethodName methodName) {
    methodExiting(methodName);
  }
  
  @Override
  public void postStatesException(
      MethodInfo methodInfo, Object exception, Object[] params) {
    potentialOutputObjects = new ArrayList<>();
    if (exception != null) {
      potentialOutputObjects.add(exception);
    }
    for (Object param : params) {
      if (param != null) {
        potentialOutputObjects.add(param);
      }
    }
  }

  @Override
  public void executingSourceLine(SourceLocation sourceLocation) {
    currentInvocation.addExecutionCount(sourceLocation);
  }

  @Override
  public void executionEnded(String executionId) {

    if (currentInvocation != invocationRoot) {
      System.err.println("Method entry/exit event does not match.");
      System.exit(1);
    }
    memoryReadLocations = null;
    memoryWriteLocations = null;
    ExecutionProfile profile = new ExecutionProfile(executionId, invocationRoot);
    Path dataFilePath = dataDir.resolve(executionId + ".tree");
    try {
      FSTSerialization.writeObjectTofile(ExecutionProfile.class, dataFilePath, profile);
    } catch (IOException e) {
      System.err.println("Error writing execution profile data file " + dataFilePath.toString());
      System.exit(1);
    }
  }

  @Override
  public void memoryRead(MemoryLocation location) {
    if (memoryReadLocations != null && memoryReadLocations.size() != 0) {
      if (!memoryWriteLocations.peek().contains(location)) {




        memoryReadLocations.peek().add(location);
      }
    }
  }

  @Override
  public void memoryWrite(MemoryLocation location) {
    if (memoryWriteLocations != null && memoryWriteLocations.size() != 0) {
      memoryWriteLocations.peek().add(location);
    }
  }
  
  private static void filterMemoryLocationsByReachableObjects(
      Set<MemoryLocation> memLocations, List<Object> reachableObjectRoots) {
    Map<Object, Integer> visitedObjects = new IdentityHashMap<>();
    LinkedList<Object> objectsToVisit = new LinkedList<>(reachableObjectRoots);
    while (objectsToVisit.size() > 0) {
      Object currentObject = objectsToVisit.removeFirst();
      if (!visitedObjects.containsKey(currentObject)) {
        visitedObjects.put(currentObject, 1);
        Class<?> currentObjectCls = currentObject.getClass();
        if (currentObjectCls.isArray()) {
          Class<?> componentType = currentObjectCls.getComponentType();
          if (!componentType.isPrimitive()) {
            int arrayLength = Array.getLength(currentObject);
            for (int i = 0; i < arrayLength; ++i) {
              Object arrayBucketValue = Array.get(currentObject, i);
              if (arrayBucketValue != null) {
                objectsToVisit.add(arrayBucketValue);
              }
            }
          }
        } else {
          List<Field> allFields = 
              ObjectFieldIterationUtil.getAllFieldsAndForceAccessible(currentObjectCls);
          for (Field field : allFields) {
            if (!field.getType().isPrimitive()) {
              try {
                Object fieldValue = field.get(currentObject);
                if (fieldValue != null) {
                  objectsToVisit.add(fieldValue);
                }
              } catch (IllegalArgumentException e) {
                e.printStackTrace();
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }
    Set<Object> reachableObjects = visitedObjects.keySet();
    List<MemoryLocation> memLocsToRemove = new ArrayList<>();
    for (MemoryLocation memLoc : memLocations) {
      if (!(memLoc instanceof StaticField)) {
        Object enclosingObject = memLoc.getEnclosingObject();
        if (!reachableObjects.contains(enclosingObject)) {
          memLocsToRemove.add(memLoc);
        }
      }
    }
    for (MemoryLocation toRemove : memLocsToRemove) {
      memLocations.remove(toRemove);
    }
  }
}
