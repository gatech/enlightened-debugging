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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.mcallrepr.ArrayElementRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayLengthRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayRepr;
import anonymous.domain.enlighten.mcallrepr.FieldReferenceName;
import anonymous.domain.enlighten.mcallrepr.MemberRefAccessedAnnotator;
import anonymous.domain.enlighten.mcallrepr.MemberRefName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.NullRepr;
import anonymous.domain.enlighten.mcallrepr.ProgramStateSnapshotter;
import anonymous.domain.enlighten.mcallrepr.ReferenceRepr;
import anonymous.domain.enlighten.mcallrepr.ReflectedObjectRepr;
import anonymous.domain.enlighten.mcallrepr.TransitivelyAccessedAnnotator;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.mcallrepr.VoidRepr;
import instr.agent.TraceInstrumenter;
import instr.agent.InstrumenterConfig;
import instr.callback.memory.MemoryLocation;
import instr.staticinfo.MethodInfo;

public class DumpStatesListener implements InstrumentationCallbackListener {
  
  private String testDesc;
  private MethodName methodToDump;
  private int indexToDump;
  private Path dataFilePath;
  private boolean profileMemAccess = true;
  
  private String redefineClassName;
  private byte[] redefinition;
  
  private ListenerState listenerState;
  private int currentIndex = -1;
  private int targetInvocStackDepth = 0;
  private MethodCallRepr preStates;
  private MethodCallRepr postStates;
  private Set<MemoryLocation> readLocations = new HashSet<>();
  private Set<MemoryLocation> writtenLocations = new HashSet<>();
  
  public DumpStatesListener(TestName testName, MethodName methodName, int invocationIndex, 
      Path dataFilePath, boolean profileMemoryAccess) {
    testDesc = testName.getDescription();
    methodToDump = methodName;
    indexToDump = invocationIndex;
    this.dataFilePath = dataFilePath;
    profileMemAccess = profileMemoryAccess;
    checkInitialInstrumenterConfig();
    TraceInstrumenter.getInstance().addWrappedMethod(methodName);
    debugOut("Target method wrapped.");
    listenerState = ListenerState.awaitingTargetTestEnters;
    debugOut("Initialized. Awaiting target test to start.");
  }
  
  public void redefineClassOnTargetInvocation(String className, byte[] redefinition) {
    redefineClassName = className;
    this.redefinition = redefinition;
  }

  @Override
  public void executionStarted(String executionId) {
    if (listenerState == ListenerState.awaitingTargetTestEnters 
        && testDesc.equals(executionId)) {
      listenerState = ListenerState.awaitingTargetInvocEnters;
      debugOut("Target test entered. Awaiting target method (wrapper) to enter.");
    }
  }

  @Override
  public void methodEntered(MethodName methodName) {
    if (listenerState == ListenerState.awaitingTargetInvocEnters) {
      if (methodName.equals(methodToDump)) {
        ++currentIndex;
        if (currentIndex == indexToDump) {
          debugOut("Target invocation (wrapper) entered.");
          if (redefineClassName != null) {
            System.out.println("Replacing faulty class " + redefineClassName 
                + " with the reference implementation.");
            try {
              TraceInstrumenter.getInstance().redefineClass(
                  Class.forName(redefineClassName), redefinition);
            } catch (ClassNotFoundException e) {
              halt(e, null);
            }
          }
          listenerState = ListenerState.profiling;
          debugOut("Profiling started.");
          targetInvocStackDepth = 0;
        }
      }
    } else if (listenerState == ListenerState.profiling) {
      ++targetInvocStackDepth;
    }
  }

  @Override
  public void preStates(MethodInfo methodInfo, Object[] params) {



    if (listenerState == ListenerState.profiling && targetInvocStackDepth == 0) {
      ProgramStateSnapshotter stateSnapshotter = new ProgramStateSnapshotter();
      preStates = new MethodCallRepr(testDesc, 
          methodToDump.getClassName(), 
          methodToDump.getMethodNameSig());
      int paramIndex = 0;
      if (!methodInfo.isStatic()) {
        preStates.setThizz(stateSnapshotter.fromObject(params[0]));
        ++paramIndex;
      }
      Type[] paramTypes = Type.getArgumentTypes(methodInfo.getMethodName().getMethodSignature());
      List<String> paramNames = methodInfo.getParamNames();
      int paramTypeIndex = 0;
      for (; paramIndex < params.length; ++paramIndex) {
        Type paramType = paramTypes[paramTypeIndex];
        String paramName = paramNames.get(paramTypeIndex);
        if (isPrimitiveType(paramType)) {
          preStates.setParam(paramName, 
              ProgramStateSnapshotter.fromBoxedPrimitive(params[paramIndex]));
        } else {
          preStates.setParam(
              paramName, stateSnapshotter.fromObject(params[paramIndex]));
        }
        ++paramTypeIndex;
      }
      debugOut("Target invocation pre-states captured.");
      if (profileMemAccess) {
        enableWholeSystemMemoryAccessProfiling();
        debugOut("Memory profiling enabled.");
      }
    }
  }

  @Override
  public void methodExiting(MethodName methodName) {
    if (listenerState == ListenerState.profiling) {
      --targetInvocStackDepth;
      if (targetInvocStackDepth == 0) {

        if (!methodName.getClassName().equals(methodToDump.getClassName()) 
            || !methodName.getMethodNameSig().equals(
                "SWIFT_INSTR_WRAPPED_METHOD_" + methodToDump.getMethodNameSig())) {
          halt(new RuntimeException("Invocation entries/exits tracking error during profiling."), null);
        }


        if (profileMemAccess) {
          annotateStatesWithMemAccessInfo(preStates, readLocations);
          annotateStatesWithMemAccessInfo(postStates, writtenLocations);
          annotateTransitiveAccessInfo(postStates);
        }
        MethodInvocation states = new MethodInvocation(methodToDump);
        states.setPreState(preStates);
        states.setPostState(postStates);
        try {
          FSTSerialization.writeObjectTofile(MethodInvocation.class, dataFilePath, states);
        } catch (IOException e) {
          halt(e, null);
        }
        debugOut("Target invocation exited.");
        debugOut("Captured states dumped successfully. Exting.");
        System.exit(0);
      }
    }
  }

  @Override
  public void postStatesNormal(MethodInfo methodInfo,
      Object retValue, Object[] params) {



    if (listenerState == ListenerState.profiling && targetInvocStackDepth == 1) {
      disableInstrumentation();
      ProgramStateSnapshotter stateSnapshotter = new ProgramStateSnapshotter();
      postStates = new MethodCallRepr(testDesc, 
          methodToDump.getClassName(), 
          methodToDump.getMethodNameSig());
      String methodSig = methodInfo.getMethodName().getMethodSignature();
      Type returnType = Type.getReturnType(methodSig);
      if (isPrimitiveType(returnType)) {
        postStates.setReturnVal(ProgramStateSnapshotter.fromBoxedPrimitive(retValue));
      } else if (returnType.getSort() == Type.VOID) {
        postStates.setReturnVal(VoidRepr.get());
      } else {
        postStates.setReturnVal(stateSnapshotter.fromObject(retValue));
      }
      int paramIndex = 0;
      if (!methodInfo.isStatic()) {
        postStates.setThizz(stateSnapshotter.fromObject(params[0]));
        ++paramIndex;
      }
      Type[] paramTypes = Type.getArgumentTypes(methodSig);
      List<String> paramNames = methodInfo.getParamNames();
      int paramTypeIndex = 0;
      for (; paramIndex < params.length; ++paramIndex) {
        Type paramType = paramTypes[paramTypeIndex];
        String paramName = paramNames.get(paramTypeIndex);
        if (isPrimitiveType(paramType)) {
          postStates.setParam(paramName, 
              ProgramStateSnapshotter.fromBoxedPrimitive(params[paramIndex]));
        } else {
          postStates.setParam(
              paramName, stateSnapshotter.fromObject(params[paramIndex]));
        }
        ++paramTypeIndex;
      }
      debugOut("Target invocation post-states captured.");
    }
  }

  @Override
  public void methodExceptionExiting(MethodName methodName) {
    methodExiting(methodName);
  }

  @Override
  public void postStatesException(MethodInfo methodInfo,
      Object exception, Object[] params) {
    if (listenerState == ListenerState.profiling && targetInvocStackDepth == 1) {
      disableInstrumentation();
      ProgramStateSnapshotter stateSnapshotter = new ProgramStateSnapshotter();
      postStates = new MethodCallRepr(testDesc, 
          methodToDump.getClassName(), 
          methodToDump.getMethodNameSig());
      postStates.setException(stateSnapshotter.fromObject(exception));
      int paramIndex = 0;
      if (!methodInfo.isStatic()) {
        postStates.setThizz(stateSnapshotter.fromObject(params[0]));
        ++paramIndex;
      }
      Type[] paramTypes = Type.getArgumentTypes(methodInfo.getMethodName().getMethodSignature());
      List<String> paramNames = methodInfo.getParamNames();
      int paramTypeIndex = 0;
      for (; paramIndex < params.length; ++paramIndex) {
        Type paramType = paramTypes[paramTypeIndex];
        String paramName = paramNames.get(paramTypeIndex);
        if (isPrimitiveType(paramType)) {
          postStates.setParam(paramName, 
              ProgramStateSnapshotter.fromBoxedPrimitive(params[paramIndex]));
        } else {
          postStates.setParam(
              paramName, stateSnapshotter.fromObject(params[paramIndex]));
        }
        ++paramTypeIndex;
      }
      debugOut("Target invocation post-states captured.");
    }
  }

  @Override
  public void executingSourceLine(SourceLocation sourceLocation) {


  }

  @Override
  public void executionEnded(String executionId) {
    if (listenerState == ListenerState.awaitingTargetInvocEnters) {

      System.exit(-1);
    }
  }

  @Override
  public void memoryRead(MemoryLocation location) {
    if (profileMemAccess && listenerState == ListenerState.profiling) {
      if (!writtenLocations.contains(location)) {
        readLocations.add(location);
      }
    }
  }

  @Override
  public void memoryWrite(MemoryLocation location) {
    if (profileMemAccess && listenerState == ListenerState.profiling) {
      writtenLocations.add(location);
    }
  }
  
  private void checkInitialInstrumenterConfig() {
    TraceInstrumenter instrumenter = TraceInstrumenter.getInstance();
    InstrumenterConfig instrConfig = instrumenter.getInstrumenterConfig();
    boolean reconfiguredInstr = false;
    if (instrConfig.instrumentLineNumber()) {
      instrConfig.instrumentLineNumber(false);
      reconfiguredInstr = true;
    }
    if (!instrConfig.instrumentStateCapture()) {
      instrConfig.instrumentStateCapture(true);
      reconfiguredInstr = true;
    }
    if (instrConfig.instrumentMemoryAccess()) {
      instrConfig.instrumentMemoryAccess(false);
      reconfiguredInstr = true;
    }
    if (reconfiguredInstr) {
      System.err.println("Warning: Initial instrumenter config incompatible.");
      System.err.println("Re-configured instrumenter and continuing.");
      instrumenter.setInstrumenterConfig(instrConfig);
      instrumenter.reinstrumentAllClasses();
    }
  }
  
  private void enableWholeSystemMemoryAccessProfiling() {
    long t1 = System.currentTimeMillis();
    TraceInstrumenter instrumenter = TraceInstrumenter.getInstance();
    InstrumenterConfig config = instrumenter.getInstrumenterConfig();
    config.instrumentMemoryAccess(true);
    instrumenter.setInstrumenterConfig(config);
    instrumenter.reinstrumentAllClasses();
    System.out.println("Instrumentation time for memory monitoring: " 
        + (System.currentTimeMillis() - t1));
  }
  
  private void disableInstrumentation() {
    long t1 = System.currentTimeMillis();
    TraceInstrumenter instrumenter = TraceInstrumenter.getInstance();
    InstrumenterConfig config = instrumenter.getInstrumenterConfig();
    config.instrumentLineNumber(false);
    config.instrumentMemoryAccess(false);
    config.instrumentStateCapture(false);
    instrumenter.setInstrumenterConfig(config);
    if (profileMemAccess) {


      instrumenter.reinstrumentAllClasses();
    }
    System.out.println("Instrumentation time: " + (System.currentTimeMillis() - t1));
  }
  
  private static void halt(Throwable ex, String extraMessage) {
    if (ex != null) {
      ex.printStackTrace();
    }
    if (extraMessage != null) {
      System.err.println(extraMessage);
    }
    System.err.println("Halting");
    System.exit(1);
  }
  
  private void debugOut(String msg) {
    System.out.println("DumpStatesListener: " + msg);
  }
  
  private boolean isPrimitiveType(Type paramType) {
    switch (paramType.getSort()) {
    case Type.BOOLEAN:
    case Type.BYTE:
    case Type.CHAR:
    case Type.SHORT:
    case Type.INT:
    case Type.FLOAT:
    case Type.LONG:
    case Type.DOUBLE:
      return true;
    }
    return false;
  }
  
  private void annotateStatesWithMemAccessInfo(
      MethodCallRepr states, Set<MemoryLocation> accessedLocations) {
    LinkedList<ValueGraphNode> workingList = new LinkedList<>();
    Set<ValueGraphNode> visitedValueReprs = 
        Collections.newSetFromMap(new IdentityHashMap<ValueGraphNode, Boolean>());


    ValueGraphNode thisRef = states.getThizz();
    if (thisRef != null) {
      workingList.add(thisRef);
    }
    for (ValueGraphNode param : states.getParams().values()) {
      if (param != null && isObjectRepresentation(param)) {
        workingList.add(param);
      }
    }
    ValueGraphNode returnVal = states.getReturnVal();
    if (returnVal != null && isObjectRepresentation(returnVal)) {
      workingList.add(returnVal);
    }
    ValueGraphNode exceptionThrown = states.getException();
    if (exceptionThrown != null) {
      workingList.add(exceptionThrown);
    }
    visitedValueReprs.addAll(workingList);
    while (workingList.size() > 0) {
      ValueGraphNode objRepr = workingList.removeFirst();
      if (!(objRepr instanceof ReferenceRepr)) {
        halt(null, "Unexpected object representation type " + objRepr.getClass().getName());
      }
      ReferenceRepr refRepr = (ReferenceRepr) objRepr;
      Map<MemberRefName, ValueGraphNode> referencedMembers = refRepr.getReferencedValues();
      for (Map.Entry<MemberRefName, ValueGraphNode> memberEntry : referencedMembers.entrySet()) {
        MemberRefName refName = memberEntry.getKey();
        MemoryLocation memberMemLocation = getMemberMemLocation(refRepr, refName);
        if (accessedLocations.contains(memberMemLocation)) {
          MemberRefAccessedAnnotator.markAccessMember(refRepr, refName);
        }
        ValueGraphNode memberValue = memberEntry.getValue();
        if (isObjectRepresentation(memberValue) && !visitedValueReprs.contains(memberValue)) {
          workingList.add(memberValue);
          visitedValueReprs.add(memberValue);
        }
      }
    }
  }
  
  private void annotateTransitiveAccessInfo(MethodCallRepr states) {
    List<ReferenceRepr> objectRoots = new ArrayList<>();
    ValueGraphNode thisRef = states.getThizz();
    if (thisRef != null && thisRef instanceof ReferenceRepr) {
      objectRoots.add((ReferenceRepr) thisRef);
    }
    for (ValueGraphNode paramValue : states.getParams().values()) {
      if (paramValue == null) {
        continue;
      }
      if (paramValue instanceof ReflectedObjectRepr || paramValue instanceof ArrayRepr) {
        objectRoots.add((ReferenceRepr) paramValue);
      }
    }
    ValueGraphNode retVal = states.getReturnVal();
    if (retVal != null && retVal instanceof ReferenceRepr) {
      objectRoots.add((ReferenceRepr) retVal);
    }
    ValueGraphNode exception = states.getException();
    if (exception != null && exception instanceof ReferenceRepr) {
      objectRoots.add((ReferenceRepr) exception);
    }
    TransitivelyAccessedAnnotator.annotateObjectSet(objectRoots);
  }
  
  private static boolean isObjectRepresentation(ValueGraphNode value) {
    return value instanceof ReferenceRepr && !NullRepr.get().equals(value);
  }
  
  private static MemoryLocation getMemberMemLocation(
      ReferenceRepr refRepr, MemberRefName refName) {
    Object actualRef = refRepr.getActualReference();
    if (actualRef == null) {
      halt(null, "Internal error: object representation not properly initialized.");
    }
    if (refName instanceof FieldReferenceName) {
      return MemoryLocation.getInstanceFieldLocation(
          actualRef, ((FieldReferenceName) refName).getFieldName());
    } else if (refName instanceof ArrayElementRefName) {
      return MemoryLocation.getArrayBucketLocation(
          actualRef, ((ArrayElementRefName) refName).getIndex());
    } else if (refName instanceof ArrayLengthRefName) {
      return MemoryLocation.getArrayLengthLocation(actualRef);
    } else {
      halt(null, "Unknown member reference name type: " + refName.getClass().getName());
    }

    return null;
  }
  
  private enum ListenerState {
    awaitingTargetTestEnters,
    awaitingTargetInvocEnters,
    profiling
  }
}
