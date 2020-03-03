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

import instr.agent.TraceInstrumenter;
import instr.agent.InstrumenterConfig;
import instr.callback.memory.MemoryLocation;
import instr.staticinfo.MethodInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
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
import anonymous.domain.enlighten.mcallrepr.FieldReferenceName;
import anonymous.domain.enlighten.mcallrepr.MemberRefAccessedAnnotator;
import anonymous.domain.enlighten.mcallrepr.MemberRefName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.NullRepr;
import anonymous.domain.enlighten.mcallrepr.ProgramStateSnapshotter;
import anonymous.domain.enlighten.mcallrepr.ReferenceRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.mcallrepr.VoidRepr;


@Deprecated
public class DumpStatesListenerOld implements InstrumentationCallbackListener {

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
  
  private long debugLastTick;
  
  public DumpStatesListenerOld(TestName testName, MethodName methodName, int invocationIndex, 
      Path dataFilePath, boolean profileMemoryAccess) {
    debugLastTick = System.currentTimeMillis();
    testDesc = testName.getDescription();
    methodToDump = methodName;
    indexToDump = invocationIndex;
    this.dataFilePath = dataFilePath;
    profileMemAccess = profileMemoryAccess;
    checkInitialInstrumenterConfig();
    listenerState = ListenerState.awaitingTargetTestEnters;
    debugOut("Initialized. Awaiting target test to start.");
  }
  
  public DumpStatesListenerOld(TestName testName, MethodName methodName, int invocationIndex, 
      Path dataFilePath) {

    this(testName, methodName, invocationIndex, dataFilePath, true);
  }
  
  public void redefineClassOnTargetInvocation(String className, byte[] redefinition) {


    redefineClassName = className;
    this.redefinition = redefinition;
  }

  @Override
  public void executionStarted(String executionId) {
    if (listenerState == ListenerState.awaitingTargetTestEnters 
        && testDesc.equals(executionId)) {
      long currentTick = System.currentTimeMillis();
      System.out.println("Test start waiting time: " + (currentTick - debugLastTick));
      debugLastTick = currentTick;
      listenerState = ListenerState.awaitingPrevInvocExits;
      debugOut("Target test entered. Awaiting prev invocation of the target method to exit.");
      waitForTargetMethodAndInstrument();
    }
  }

  @Override
  public void methodEntered(MethodName methodName) {
    if (listenerState == ListenerState.awaitingPrevInvocExits) {
      if (methodName.equals(methodToDump)) {
        ++currentIndex;
      }
    } else if (listenerState == ListenerState.awaitingTargetInvocEnters) {
      if (methodName.equals(methodToDump)) {
        long currentTick = System.currentTimeMillis();
        System.out.println("Target method enter waiting time: " + (currentTick - debugLastTick));
        debugLastTick = currentTick;

        if (++currentIndex != indexToDump) {
          halt(null, "Internal error: unexpected invocation index.");
        }

        debugOut("Target invocation entered.");
        listenerState = ListenerState.profiling;


        debugOut("Profiling started.");
        targetInvocStackDepth = 0;
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
      int paramTypeIndex = 0;
      for (; paramIndex < params.length; ++paramIndex) {
        Type paramType = paramTypes[paramTypeIndex];
        if (isPrimitiveType(paramType)) {
          preStates.setParam("param_" + paramIndex, 
              ProgramStateSnapshotter.fromBoxedPrimitive(params[paramIndex]));
        } else {
          preStates.setParam(
              "param_" + paramIndex, stateSnapshotter.fromObject(params[paramIndex]));
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
    if (listenerState == ListenerState.awaitingPrevInvocExits) {
      waitForTargetMethodAndInstrument();
    } else if (listenerState == ListenerState.profiling) {
      --targetInvocStackDepth;
      if (targetInvocStackDepth == -1) {
        long currentTick = System.currentTimeMillis();
        System.out.println("Target method execution time: " + (currentTick - debugLastTick));
        debugLastTick = currentTick;

        if (!methodName.equals(methodToDump)) {
          halt(new RuntimeException("Invocation entries/exits tracking error during profiling."), null);
        }


        if (profileMemAccess) {
          annotateStates(preStates, readLocations);
          annotateStates(postStates, writtenLocations);
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
    if (listenerState == ListenerState.profiling && targetInvocStackDepth == 0) {
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
      int paramTypeIndex = 0;
      for (; paramIndex < params.length; ++paramIndex) {
        Type paramType = paramTypes[paramTypeIndex];
        if (isPrimitiveType(paramType)) {
          postStates.setParam("param_" + paramIndex, 
              ProgramStateSnapshotter.fromBoxedPrimitive(params[paramIndex]));
        } else {
          postStates.setParam(
              "param_" + paramIndex, stateSnapshotter.fromObject(params[paramIndex]));
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
    if (listenerState == ListenerState.profiling && targetInvocStackDepth == 0) {
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
      int paramTypeIndex = 0;
      for (; paramIndex < params.length; ++paramIndex) {
        Type paramType = paramTypes[paramTypeIndex];
        if (isPrimitiveType(paramType)) {
          postStates.setParam("param_" + paramIndex, 
              ProgramStateSnapshotter.fromBoxedPrimitive(params[paramIndex]));
        } else {
          postStates.setParam(
              "param_" + paramIndex, stateSnapshotter.fromObject(params[paramIndex]));
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
  public void executionEnded(String executionId) {}

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
    if (instrConfig.instrumentStateCapture()) {
      instrConfig.instrumentStateCapture(false);
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
  
  private void waitForTargetMethodAndInstrument() {
    if (listenerState == ListenerState.awaitingPrevInvocExits 
        && currentIndex == indexToDump - 1) {
      debugOut("Prev invocation of target method exited.");
      long t1 = System.currentTimeMillis();
      TraceInstrumenter instrumenter = TraceInstrumenter.getInstance();
      if (redefineClassName != null) {
        System.out.println("Replacing faulty class " + redefineClassName 
            + " with the reference implementation.");
        try {
          instrumenter.redefineClass(Class.forName(redefineClassName),
              redefinition);
        } catch (ClassNotFoundException e) {
          halt(e, null);
        }
      }
      InstrumenterConfig config = instrumenter.getInstrumenterConfig();

      config.instrumentStateCapture(true);
      instrumenter.setInstrumenterConfig(config);
      instrumenter.reinstrumentAllClasses();
      if (profileMemAccess) {

        try {
          

          Class<?> targetClass = Class.forName(methodToDump.getClassName());
          enableMemoryAccessProfiling(targetClass);
        } catch (ClassNotFoundException e) {
          halt(e, null);
        }
      }
      System.out.println("Instrumentation time: " + (System.currentTimeMillis() - t1));
      listenerState = ListenerState.awaitingTargetInvocEnters;
      debugOut("State capture code instrumented. Awaiting target invocation.");
    }
  }
  
  private void enableMemoryAccessProfiling(Class<?> profiledClass) {
    TraceInstrumenter instrumenter = TraceInstrumenter.getInstance();
    InstrumenterConfig config = instrumenter.getInstrumenterConfig();
    config.instrumentMemoryAccess(true);
    instrumenter.setInstrumenterConfig(config);
    instrumenter.reinstrumentClass(profiledClass);
  }
  
  private void enableWholeSystemMemoryAccessProfiling() {
    long t1 = System.currentTimeMillis();
    TraceInstrumenter instrumenter = TraceInstrumenter.getInstance();
    InstrumenterConfig config = instrumenter.getInstrumenterConfig();
    config.instrumentMemoryAccess(true);
    instrumenter.setInstrumenterConfig(config);
    instrumenter.reinstrumentAllClasses();
    System.out.println("Instrumentation time: " + (System.currentTimeMillis() - t1));
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
  
  private void annotateStates(MethodCallRepr states, Set<MemoryLocation> accessedLocations) {
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
      return MemoryLocation.getArrayBucketLocation(actualRef, ((ArrayElementRefName) refName).getIndex());
    } else if (refName instanceof ArrayLengthRefName) {
      return MemoryLocation.getArrayLengthLocation(actualRef);
    } else {
      halt(null, "Unknown member reference name type: " + refName.getClass().getName());
    }

    return null;
  }
  
  protected static void halt(Throwable ex, String extraMessage) {
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
    System.out.println("DumpStateListener: " + msg);
  }
  
  private enum ListenerState {
    awaitingTargetTestEnters,
    awaitingPrevInvocExits,
    awaitingTargetInvocEnters,
    profiling
  }
}
