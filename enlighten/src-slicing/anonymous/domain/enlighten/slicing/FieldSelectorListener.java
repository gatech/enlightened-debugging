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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Range;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.mcallrepr.JpfStateSnapshotter;
import anonymous.domain.enlighten.mcallrepr.MemberRefDepAnnotator;
import anonymous.domain.enlighten.mcallrepr.MemberRefName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.NullRepr;
import anonymous.domain.enlighten.mcallrepr.PrimitiveRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.refpath.RefPath;
import anonymous.domain.enlighten.slicing.util.DepDepthFirstTraversal;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.susp.FaultLocalization;
import anonymous.domain.enlighten.util.FloatComparison;
import anonymous.domain.enlighten.util.Pair;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class FieldSelectorListener extends ExecTreeProcessingListener {

  private Set<MethodInvocation> monitoredInvocations;
  private Set<SourceLocation> selectionCriterionStmts;
  private FaultLocalization<SourceLocation> flResults;
  private Map<MethodInvocation, Set<RefPath>> excludedFields;
  
  private MethodInvocation selectedInvocation;
  private MethodCallRepr selectedInvocationPostStates;
  private List<Pair<RefPath, Double>> orderedSuspiciousFields;
  
  public FieldSelectorListener(SubjectProgram subject, 
      FieldSelectionCriterion selectionCriterion) {
    super(subject, selectionCriterion.executionProfile);
    monitoredInvocations = new HashSet<>(selectionCriterion.invocations);
    selectionCriterionStmts = selectionCriterion.statements;
    flResults = selectionCriterion.flResults;
    excludedFields = selectionCriterion.answeredFields;
  }
  
  public MethodInvocation getSelectedInvocation() {
    return selectedInvocation;
  }
  
  public MethodCallRepr getSelectedInvocationPostStates() {
    return selectedInvocationPostStates;
  }
  
  public List<Pair<RefPath, Double>> getOrderedSuspiciousFields() {
    return orderedSuspiciousFields;
  }

  @Override
  protected void invocationEntered(
      MethodInvocation enteredInvocation, ThreadInfo currentThread) {}

  @Override
  protected void invocationExited(
      MethodInvocation exitedInvocation, ThreadInfo currentThread) {
    if (monitoredInvocations.contains(exitedInvocation)) {
      SuspInfoCalculator suspCalc = new SuspInfoCalculator(Range.closed(
          DepIndexRangeAnnotator.getStartIndex(exitedInvocation), 
          DepIndexRangeAnnotator.getEndIndex(exitedInvocation)));
      JpfStateSnapshotter snapshotter = new JpfStateSnapshotter();
      MethodCallRepr mcall = snapshotter.fromStackFrame(
          currentThread, currentThread.getModifiableTopFrame());
      
      Map<RefPath, SuspInfoAnnotation> dataFieldSusp = new HashMap<>();
      LinkedList<RefPath> pathsToVisit = new LinkedList<>();
      LinkedList<ValueGraphNode> parentObjects = new LinkedList<>();
      Set<ValueGraphNode> visitedValues = new HashSet<>();
      Set<RefPath> invocExcludedFields = excludedFields.get(exitedInvocation);
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
          SuspInfoAnnotation suspInfo = 
              suspCalc.getDataFieldSuspiciousness(mcall, currentPath);
          if (suspInfo != null) {
            dataFieldSusp.put(currentPath, suspInfo);
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
      List<Pair<RefPath, Double>> suspiciousFieldsList = new ArrayList<>();
      boolean isQualifiedInvocation = false;
      for (RefPath refPath : dataFieldSusp.keySet()) {
        SuspInfoAnnotation suspInfo = dataFieldSusp.get(refPath);
        double currentFieldSuspiciousness = suspInfo.suspiciousness;
        if (FloatComparison.compareDouble(currentFieldSuspiciousness, 0) > 0) {
          suspiciousFieldsList.add(Pair.of(refPath, currentFieldSuspiciousness));
          if (selectionCriterionStmts.contains(suspInfo.suspReason)) {
            isQualifiedInvocation = true;
          }
        }
      }
      if (isQualifiedInvocation) {
        Collections.sort(suspiciousFieldsList, 
            Collections.reverseOrder(new FieldSuspiciousnessComparator()));
        selectedInvocation = exitedInvocation;
        selectedInvocationPostStates = mcall;
        orderedSuspiciousFields = suspiciousFieldsList;
        if (!InfluencedFieldsAnnotator.getInfluencedFields(exitedInvocation).isEmpty()) {
          monitoredInvocations.clear();
        } else {
          monitoredInvocations.removeIf(
              invoc -> { return InfluencedFieldsAnnotator.getInfluencedFields(invoc).isEmpty(); });
        }
        if (monitoredInvocations.isEmpty()) {
          disableListener();
          currentThread.getVM().terminateProcess(currentThread);
        }
      }
    }
  }
  
  private boolean isDataFieldSelectionCandidate(ValueGraphNode value) {
    if (value == null) {
      return false;
    }


    return value instanceof PrimitiveRepr || value instanceof NullRepr;
  }
  
  private static final class SuspInfoAnnotation implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    private double suspiciousness;
    private SourceLocation suspReason;
    private Range<Long> effectiveRange;
  }
  
  private class SuspInfoCalculator extends DepDepthFirstTraversal {
    
    private Range<Long> effectiveRange;
    
    public SuspInfoCalculator(Range<Long> effectiveRange) {
      this.effectiveRange = effectiveRange;
    }
    
    public SuspInfoAnnotation getDataFieldSuspiciousness(
        MethodCallRepr mcall, RefPath fieldPath) {
      DynamicDependency dep = MemberRefDepAnnotator.getDependency(mcall, fieldPath);
      if (dep == null) {
        return null;
      }
      traverse(dep);
      return dep.getAnnotation(SuspInfoAnnotation.class);
    }

    @Override
    protected boolean preVisit(DynamicDependency depNode) {
      if (!effectiveRange.contains(depNode.getInstanceIndex())) {
        return false;
      }
      SuspInfoAnnotation suspInfo = depNode.getAnnotation(SuspInfoAnnotation.class);
      if (suspInfo != null && Objects.equals(suspInfo.effectiveRange, effectiveRange)) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    protected void postVisit(DynamicDependency depNode) {
      depNode.removeAnnotation(SuspInfoAnnotation.class);
      if (depNode instanceof InstructionDependencySource) {
        SuspInfoAnnotation suspInfo = new SuspInfoAnnotation();
        suspInfo.effectiveRange = effectiveRange;
        suspInfo.suspReason = JpfEntityConversion.getSourceLocationFromInstruction(
            ((InstructionDependencySource) depNode).getSourceInstruction());
        suspInfo.suspiciousness = flResults.getSuspiciousness(suspInfo.suspReason);
        depNode.addAnnotation(suspInfo);
      } else if (depNode instanceof CompositeDynamicDependency) {
        SuspInfoAnnotation highestSusp = null;
        List<DynamicDependency> upStreams = 
            getChildrenDepNodes((CompositeDynamicDependency) depNode);
        for (DynamicDependency upStream : upStreams) {
          SuspInfoAnnotation currentSusp = upStream.getAnnotation(SuspInfoAnnotation.class);
          if (currentSusp == null) {
            continue;
          }
          if (highestSusp == null || FloatComparison.compareDouble(
              currentSusp.suspiciousness, highestSusp.suspiciousness) > 0) {
            highestSusp = currentSusp;
          } else if (!selectionCriterionStmts.contains(highestSusp.suspReason) 
              && selectionCriterionStmts.contains(currentSusp.suspReason)) {
            highestSusp = currentSusp;
          }
        }
        if (highestSusp == null) {
          highestSusp = new SuspInfoAnnotation();
          highestSusp.effectiveRange = effectiveRange;
        }
        depNode.addAnnotation(highestSusp);
      }
    }
    
    private List<DynamicDependency> getChildrenDepNodes(CompositeDynamicDependency dep) {
      if (followControlDependencies()) {
        return dep.getAllDependencies();
      } else {
        return dep.getDataDependencies();
      }
    }
  }
  
  private static class FieldSuspiciousnessComparator implements Comparator<Pair<RefPath, Double>> {

    @Override
    public int compare(Pair<RefPath, Double> o1, Pair<RefPath, Double> o2) {
      return FloatComparison.compareDouble(o1.getSecond(), o2.getSecond());
    }
    
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {}
}
