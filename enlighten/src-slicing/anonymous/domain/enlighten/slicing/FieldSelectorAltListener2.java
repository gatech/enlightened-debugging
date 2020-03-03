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


package anonymous.domain.enlighten.slicing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DependencyCreationListener;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.mcallrepr.ArrayElementRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayLengthRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayRepr;
import anonymous.domain.enlighten.mcallrepr.FieldReferenceName;
import anonymous.domain.enlighten.mcallrepr.JpfStateSnapshotter;
import anonymous.domain.enlighten.mcallrepr.MemberRefDepAnnotator;
import anonymous.domain.enlighten.mcallrepr.MemberRefName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.MethodCallSpecialRefName;
import anonymous.domain.enlighten.mcallrepr.NullRepr;
import anonymous.domain.enlighten.mcallrepr.PrimitiveRepr;
import anonymous.domain.enlighten.mcallrepr.ReferenceRepr;
import anonymous.domain.enlighten.mcallrepr.ReflectedObjectRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.mcallrepr.VoidRepr;
import anonymous.domain.enlighten.publish.ExtraStats;
import anonymous.domain.enlighten.publish.ExtraStatsPublisher;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class FieldSelectorAltListener2 extends ExecTreeProcessingListener 
    implements DependencyCreationListener {
  
  private FieldSelectionCriterionAlt2 selectionCriterion;
  private boolean enableAF = true;
  
  private HashSet<MethodInvocation> activeInvocations = new HashSet<>();
  private MethodInvocation globalBest = null;
  private double globalBestSusp = 0;
  private Map<MethodInvocation, Double> localCandidateSusp = new HashMap<>();
  
  private Stats extraStats;
  
  
  
  
  public FieldSelectorAltListener2(
      SubjectProgram subject, FieldSelectionCriterionAlt2 selectionCriterion) {
    super(subject, selectionCriterion.executionProfile);
    this.selectionCriterion = selectionCriterion;
    setRequireDeterministicExecution(true);
    extraStats = new Stats();
  }
  
  public void enableAF(boolean enable) {
	  enableAF = enable;
  }
  
  @Override
  public void dependencyCreated(DynamicDependency dep) {
    ++extraStats.numDepNode;






    SuspInfoAnnotation suspInfo = new SuspInfoAnnotation();
    suspInfo.dynamicDependencyMultiplier = getAmplifyingFactor(dep);
    if (dep instanceof InstructionDependencySource) {
      DependencyTrackingInstruction insn = 
          ((InstructionDependencySource) dep).getSourceInstruction();
      SourceLocation srcLoc = JpfEntityConversion.getSourceLocationFromInstruction(insn);
      double srcSusp = selectionCriterion.flResults.getSuspiciousness(srcLoc);
      MethodInvocation currentInvoc = getCurrentInvocation();
      suspInfo.setBaseSuspiciousness(currentInvoc, srcSusp);
      ++extraStats.numSuspValues;
      double adjSusp = suspInfo.getAdjustedSuspiciousness(currentInvoc);
      if (adjSusp > globalBestSusp) {
        dep.addAnnotation(suspInfo);
        updateLocalCandidateSusp(currentInvoc, adjSusp);
      } 
      
    } else if (dep instanceof CompositeDynamicDependency) {
      boolean hasAnySuspiciousness = false;
      List<DynamicDependency> upStreams = ((CompositeDynamicDependency) dep).getAllDependencies();
      for (DynamicDependency upStream : upStreams) {
        SuspInfoAnnotation upStreamSuspInfo = upStream.getAnnotation(SuspInfoAnnotation.class);
        if (upStreamSuspInfo == null) {
          continue;
        }
        for (MethodInvocation scope : upStreamSuspInfo.getSuspScopes()) {
          if (!activeInvocations.contains(scope)) {


            continue;
          }
          double contextBaseSusp = upStreamSuspInfo.getBaseSuspiciousness(scope);
          double contextAdjSusp = upStreamSuspInfo.getAdjustedSuspiciousness(scope);
          

          if (contextAdjSusp > globalBestSusp
              && contextBaseSusp > suspInfo.getBaseSuspiciousness(scope)) {
            suspInfo.setBaseSuspiciousness(scope, contextBaseSusp);
            ++extraStats.numSuspValues;
            hasAnySuspiciousness = true;
          }
        }
      }
      if (hasAnySuspiciousness) {
        dep.addAnnotation(suspInfo);
      }
    }
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {}

  @Override
  protected void invocationEntered(MethodInvocation enteredInvocation,
      ThreadInfo currentThread) {
    activeInvocations.add(enteredInvocation);
  }

  @Override
  protected void invocationExited(MethodInvocation exitedInvocation,
      ThreadInfo currentThread) {
    activeInvocations.remove(exitedInvocation);
    if (localCandidateSusp.containsKey(exitedInvocation) 
        && localCandidateSusp.get(exitedInvocation) > globalBestSusp) {




      JpfStateSnapshotter snapshotter = new JpfStateSnapshotter();
      MethodCallRepr mcall = snapshotter.fromStackFrame(
          currentThread, currentThread.getModifiableTopFrame());
      
      LinkedList<RefPath> pathsToVisit = new LinkedList<>();
      LinkedList<ValueGraphNode> parentObjects = new LinkedList<>();
      Set<ValueGraphNode> visitedValues = new HashSet<>();
      Set<RefPath> invocExcludedFields = selectionCriterion.correctValues.get(exitedInvocation);
      if (invocExcludedFields == null) {
        invocExcludedFields = Collections.emptySet();
      }
      for (MemberRefName ref : mcall.getReferencedValues().keySet()) {
        parentObjects.add(mcall);
        pathsToVisit.add(RefPath.newBuilder().appendMemberRefName(ref).build());
      }
      while (!pathsToVisit.isEmpty()) {
        RefPath currentPath = pathsToVisit.removeFirst();
        ValueGraphNode currentObj = parentObjects.removeFirst().getReferencedValue(
            currentPath.getTail());
        if (isDataFieldSelectionCandidate(currentObj) 
            && !invocExcludedFields.contains(currentPath)) {
          DynamicDependency depNode = MemberRefDepAnnotator.getDependency(mcall, currentPath);
          if (depNode != null) {
            SuspInfoAnnotation suspInfo = depNode.getAnnotation(SuspInfoAnnotation.class);
            if (suspInfo != null) {
              double adjustedSusp = suspInfo.getAdjustedSuspiciousness(exitedInvocation);
              if (adjustedSusp > globalBestSusp) {
                updateGlobalBestSelection(exitedInvocation, adjustedSusp);
              }
            }
          }
        }
        if (visitedValues.contains(currentObj)) {
          continue;
        }
        visitedValues.add(currentObj);
        if (currentObj.hasReferencedValues()) {
          Map<MemberRefName, ValueGraphNode> membersMap = currentObj.getReferencedValues();
          for (MemberRefName member : membersMap.keySet()) {
            RefPath memberPath = currentPath.append().appendMemberRefName(member).build();
            parentObjects.add(currentObj);
            pathsToVisit.add(memberPath);
          }
        }
      }
    }
    
    
  }
  
  public MethodInvocation getSelectedInvocation() {
    if (ExtraStatsPublisher.isAcceptingReports()) {
      ExtraStatsPublisher.report(extraStats);
    }
    return globalBest;
  }
  
  private void updateLocalCandidateSusp(MethodInvocation context, double candidateSusp) {
    Double previousLocalBest = localCandidateSusp.get(context);
    if (previousLocalBest == null || previousLocalBest < candidateSusp) {
      localCandidateSusp.put(context, candidateSusp);
    }
  }
  
  private void updateGlobalBestSelection(MethodInvocation invocation, double highestSusp) {
    globalBest = invocation;
    globalBestSusp = highestSusp;
    for (MethodInvocation candidateInvoc : new ArrayList<>(localCandidateSusp.keySet())) {
      if (globalBestSusp >= localCandidateSusp.get(candidateInvoc)) {
        localCandidateSusp.remove(candidateInvoc);
      }
    }
  }
  
  private int getAmplifyingFactor(DynamicDependency depNode) {
	if (!enableAF) {
		return 1;
	}
    long depIndex = depNode.getInstanceIndex();
    int amplifyingFactor = 1;
    for (Set<Long> incorrectValueBackSlice : 
      selectionCriterion.incorrectValueRelevantDepIdList) {
      if (incorrectValueBackSlice.contains(depIndex)) {
        ++amplifyingFactor;
      }
    }
    return amplifyingFactor;
  }
  
  private static boolean isDataFieldSelectionCandidate(ValueGraphNode value) {
    if (value == null) {
      return false;
    }


    return value instanceof PrimitiveRepr || value instanceof NullRepr;
  }
  
  private static class SuspInfoAnnotation implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    
    private Map<MethodInvocation, Double> baseSuspByScope = new HashMap<>();
    private double dynamicDependencyMultiplier = 1.0;
    
    public double getAdjustedSuspiciousness(MethodInvocation scope) {
      return  getBaseSuspiciousness(scope) * dynamicDependencyMultiplier;
    }
    
    public double getBaseSuspiciousness(MethodInvocation scope) {
      Double scopedBaseSusp = baseSuspByScope.get(scope);
      if (scopedBaseSusp != null) {
        return  scopedBaseSusp;
      } else {
        return 0;
      }
    }
    
    public void setBaseSuspiciousness(MethodInvocation scope, double susp) {
      baseSuspByScope.put(scope, susp);
    }
    
    public Set<MethodInvocation> getSuspScopes() {
      return baseSuspByScope.keySet();
    }
  }
  
  public static class Stats implements ExtraStats {
    private static final long serialVersionUID = 1L;
    
    public long numDepNode;
    public long numSuspValues;
  }
  
  public static class PrintSusp {

    private String sep = "\n";
    private MethodInvocation scope;
    
    private Set<ValueGraphNode> visited = new HashSet<ValueGraphNode>();
    
    public static String toString(MethodCallRepr mcall, MethodInvocation scope) {
      return new PrintSusp(scope).visit(mcall, "");
    }
    
    public PrintSusp(MethodInvocation scope) {
      this.scope = scope;
    }

    public String visit(MethodCallRepr mcall, String indent) {
      StringBuffer arg = new StringBuffer();
      
      arg.append(mcall.isEntry() ? "** ENTRY **" : "** EXIT **");
      arg.append(sep);
      arg.append(indent + "TEST: " + mcall.getTestName() + sep);
      arg.append(indent + "METHOD: " + mcall.getMethodName() 
          + " #" + mcall.getValueHash() + "#" + sep);
      

      arg.append(indent + "this => ");
      arg.append(visit(
          mcall.getThizz(), indent, MemberRefDepAnnotator.getDependency(
              mcall, MethodCallSpecialRefName.thisRef())));
      arg.append(sep);
      

      Map<String, ValueGraphNode> paramValues = mcall.getParams();
      Map<String, DynamicDependency> paramDeps = new HashMap<>();
      for (String paramName : paramValues.keySet()) {
        paramDeps.put(paramName, MemberRefDepAnnotator.getDependency(
            mcall, MethodCallSpecialRefName.fromParamName(paramName)));
      }
      arg.append(visitMap(paramValues,  indent, paramDeps));
      

      if (!mcall.isEntry()) {
        if (mcall.getException() == null) {
          arg.append(indent + "return => ");
          arg.append(visit(
              mcall.getReturnVal(), indent, MemberRefDepAnnotator.getDependency(
                  mcall, MethodCallSpecialRefName.returnValue())));
        } else {
          arg.append(indent + "exception => ");
          arg.append(visit(
              mcall.getException(), indent, MemberRefDepAnnotator.getDependency(
                  mcall,MethodCallSpecialRefName.exceptionThrown())));
        }
        arg.append(sep);
      }

      return arg.toString();
    }

    public String visit(ValueGraphNode vrepr,  String indent, DynamicDependency dep) {
      if (vrepr == null) {
        return "null";
      }
      
      if (visited.contains(vrepr)) {
        String str = "VISITED";
        if (vrepr instanceof ReflectedObjectRepr) {
          str += " [" + ((ReflectedObjectRepr) vrepr).getId() + "]";
        } else if (vrepr instanceof ArrayRepr) {
          str += " [" + ((ArrayRepr) vrepr).getId() + "]";
        }
        str += " #" + vrepr.getValueHash() + "#";
        str += " ***" + getSuspStr(dep) + "***";
        return str;
      }
      
      StringBuffer arg = new StringBuffer();
      if (vrepr instanceof PrimitiveRepr) {
        arg.append(visit((PrimitiveRepr) vrepr, indent, dep));
      } else if (vrepr instanceof ReflectedObjectRepr) {
        visited.add(vrepr);
        arg.append(visit((ReflectedObjectRepr) vrepr, indent, dep));
      } else if (vrepr instanceof ArrayRepr) {
        visited.add(vrepr);
        arg.append(visit((ArrayRepr) vrepr, indent, dep));
      } else if (vrepr instanceof ReferenceRepr) {
        arg.append(visit((ReferenceRepr) vrepr, indent, dep));
      } else if (vrepr instanceof VoidRepr) {
        arg.append("VOID");
      } else {
        throw new UnsupportedOperationException("missing case");
      }
      
      return arg.toString();
    }

    public String visit(ReflectedObjectRepr orepr,  String indent, DynamicDependency dep) {
      StringBuffer arg = new StringBuffer();
      arg.append(String.format("%s[%d] #%d# ***%s*** {\n", 
          orepr.getType(), orepr.getId(), orepr.getValueHash(), getSuspStr(dep)));
      Map<String, ValueGraphNode> fieldValues = orepr.getFields();
      Map<String, DynamicDependency> fieldDeps = new HashMap<>();
      for (String fieldName : fieldValues.keySet()) {
        fieldDeps.put(fieldName, MemberRefDepAnnotator.getDependency(orepr, new FieldReferenceName(fieldName)));
      }
      arg.append(visitMap(orepr.getFields(), indent + " ", fieldDeps));
      arg.append(indent + "}" + sep);
      return arg.toString();
    }
    
    public String visit(ArrayRepr arepr, String indent, DynamicDependency dep) {
      StringBuilder buf = new StringBuilder();
      buf.append(String.format("%s[%d] #%d# ***%s*** {\n", 
          arepr.getType(), arepr.getId(), arepr.getValueHash(), getSuspStr(dep)));
      String elementIndent = indent + " ";
      buf.append(elementIndent + "arrayLength => "
          + visit(arepr.getLength(), elementIndent + " ", 
              MemberRefDepAnnotator.getDependency(arepr, ArrayLengthRefName.get())) + "\n");
      List<ValueGraphNode> elements = arepr.getElements();
      for (int index = 0; index < elements.size(); ++index) {
        buf.append(elementIndent + "[" + index + "] => " 
            + visit(elements.get(index), elementIndent + " ", 
                MemberRefDepAnnotator.getDependency(arepr, new ArrayElementRefName(index))) + "\n");
      }
      buf.append(indent + "}\n");
      return buf.toString();
    }
   
    public String visit(PrimitiveRepr wrepr, String indent, DynamicDependency dep) {
      return wrepr.getType() + ":" + wrepr.getWrappedValue().toString() 
          + " #" + wrepr.getValueHash()
          + " ***" + getSuspStr(dep) + "***";
    }

    public String visit(ReferenceRepr refRepr, String arg, DynamicDependency dep) {
      return refRepr.toString() + " ***" + getSuspStr(dep) + "***";
    }

    private String visitMap(Map<String, ValueGraphNode> map, String indent, Map<String, DynamicDependency> depMap) {
      StringBuffer arg = new StringBuffer();
      List<String> sortedKeys = new ArrayList<>(map.keySet());
      Collections.sort(sortedKeys);
      for(String key : sortedKeys) {
        ValueGraphNode val = map.get(key);
        DynamicDependency dep = depMap.get(key);
        arg.append(indent + key + " => ");
        if (val == null) {
          arg.append("NULL");
        } else {
          arg.append(visit(val, indent, dep));
        }
        arg.append(sep);
      }
      return arg.toString();
    }
    
    private String getSuspStr(DynamicDependency dep) {
      if (dep == null) {
        return "(No suspiciousness)";
      }
      SuspInfoAnnotation suspInfo = dep.getAnnotation(SuspInfoAnnotation.class);
      if (suspInfo == null) {
        return "(No suspiciousness)";
      }
      return "" + suspInfo.getAdjustedSuspiciousness(scope) 
          + "(" + suspInfo.getBaseSuspiciousness(scope) 
          + " * " + suspInfo.dynamicDependencyMultiplier + ")";
    }
  }
}
