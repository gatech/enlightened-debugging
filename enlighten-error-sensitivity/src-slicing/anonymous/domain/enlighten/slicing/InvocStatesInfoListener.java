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
import anonymous.domain.enlighten.slicing.util.DepBreadthFirstTraversal;
import anonymous.domain.enlighten.slicing.util.DepDepthFirstTraversal;
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.util.FloatComparison;
import anonymous.domain.enlighten.util.Pair;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class InvocStatesInfoListener extends ExecTreeProcessingListener {

  private TargetInvocInfo targetInvocInfo;
  
  private MethodCallRepr preStates;
  private MethodCallRepr postStates;
  private Map<RefPath, SuspInfoAnnotation> postStatesValueSusp;
  
  public InvocStatesInfoListener(SubjectProgram subject, TargetInvocInfo targetInvocInfo) {
    super(subject, targetInvocInfo.targetExecutionProfile);
    this.targetInvocInfo = targetInvocInfo;
  }
  
  public MethodCallRepr getInvocationPreStates() {
    return preStates;
  }
  
  public MethodCallRepr getInvocationPostStates() {
    return postStates;
  }
  
  public List<Pair<RefPath, Double>> getOrderedSuspiciousValuePaths() {
    List<Pair<RefPath, Double>> suspiciousValuePathList = new ArrayList<>();
    for (RefPath refPath : postStatesValueSusp.keySet()) {
      SuspInfoAnnotation suspInfo = postStatesValueSusp.get(refPath);
      double currentValueSuspiciousness = suspInfo.getAdjustedSuspiciousness();
      if (FloatComparison.compareDouble(currentValueSuspiciousness, 0) > 0) {
        suspiciousValuePathList.add(Pair.of(refPath, currentValueSuspiciousness));
      }
    }
    Collections.sort(suspiciousValuePathList, 
        Collections.reverseOrder(new FieldSuspiciousnessComparator()));
    return suspiciousValuePathList;
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {}

  @Override
  protected void invocationEntered(MethodInvocation enteredInvocation,
      ThreadInfo currentThread) {
    if (enteredInvocation == targetInvocInfo.targetInvocation) {
      JpfStateSnapshotter snapshotter = new JpfStateSnapshotter();
      preStates = snapshotter.fromStackFrame(
          currentThread, currentThread.getModifiableTopFrame());
    }
    
  }

  @Override
  protected void invocationExited(MethodInvocation exitedInvocation,
      ThreadInfo currentThread) {
    if (exitedInvocation == targetInvocInfo.targetInvocation) {
      JpfStateSnapshotter snapshotter = new JpfStateSnapshotter();
      postStates = snapshotter.fromStackFrame(
          currentThread, currentThread.getModifiableTopFrame());
      
      SuspInfoCalculator suspCalc = new SuspInfoCalculator(Range.closed(
          DepIndexRangeAnnotator.getStartIndex(exitedInvocation), 
          DepIndexRangeAnnotator.getEndIndex(exitedInvocation)));
      postStatesValueSusp = new HashMap<>();
      LinkedList<RefPath> pathsToVisit = new LinkedList<>();
      LinkedList<ValueGraphNode> parentObjects = new LinkedList<>();
      Set<ValueGraphNode> visitedValues = new HashSet<>();
      Set<RefPath> invocExcludedFields = null; 
      if (invocExcludedFields == null) {
        invocExcludedFields = Collections.emptySet();
      }
      for (MemberRefName ref : postStates.getReferencedValues().keySet()) {
        parentObjects.add(postStates);
        pathsToVisit.add(RefPath.newBuilder().appendMemberRefName(ref).build());
      }
      while (!pathsToVisit.isEmpty()) {
        RefPath currentPath = pathsToVisit.removeFirst();
        ValueGraphNode currentObj = parentObjects.removeFirst().getReferencedValue(
            currentPath.getTail());
        if (isDataFieldSelectionCandidate(currentObj) 
            && !invocExcludedFields.contains(currentPath)) {
          SuspInfoAnnotation suspInfo = 
              suspCalc.getValueSuspiciousness(postStates, currentPath);
          if (suspInfo != null && suspInfo.getAdjustedSuspiciousness() != 0) {
            postStatesValueSusp.put(currentPath, suspInfo);
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
  
  private boolean isDataFieldSelectionCandidate(ValueGraphNode value) {
    if (value == null) {
      return false;
    }


    return value instanceof PrimitiveRepr || value instanceof NullRepr;
  }
  
  private void processIncorrectValues(
      Set<RefPath> valuePaths, ThreadInfo ti, Range<Long> effectiveRange) {
    JpfStateSnapshotter stateSnapshotter = new JpfStateSnapshotter();
    MethodCallRepr preStates = stateSnapshotter.fromStackFrame(ti, ti.getModifiableTopFrame());
    IncorrectValueDependencyVisitor depVisitor = 
        new IncorrectValueDependencyVisitor(effectiveRange);
    for (RefPath incorrectValue : valuePaths) {
      try {
        DynamicDependency valueDependency = 
            MemberRefDepAnnotator.getDependency(preStates, incorrectValue);
        depVisitor.traverse(valueDependency);
      } catch (Throwable ex) {


        continue;
      }
    }
  }
  
  private int getAmplifyingFactor(DynamicDependency depNode) {
    long depIndex = depNode.getInstanceIndex();
    int amplifyingFactor = 1;
    for (Set<Long> incorrectValueBackSlice : 
        targetInvocInfo.incorrectValueRelevantDepIdList) {
      if (incorrectValueBackSlice.contains(depIndex)) {
        ++amplifyingFactor;
      }
    }
    return amplifyingFactor;
  }

  private static class SuspInfoAnnotation implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    
    private double baseSuspiciousness = 0;
    private double dynamicDependencyMultiplier = 1.0;
    
    public double getAdjustedSuspiciousness() {
      return baseSuspiciousness * dynamicDependencyMultiplier;
    }
  }
  
  private class SuspInfoCalculator extends DepDepthFirstTraversal {
    
    private Range<Long> effectiveRange;
    
    public SuspInfoCalculator(Range<Long> effectiveRange) {
      this.effectiveRange = effectiveRange;
    }
    
    public SuspInfoAnnotation getValueSuspiciousness(
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
      if (suspInfo != null) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    protected void postVisit(DynamicDependency depNode) {
      SuspInfoAnnotation suspInfo = new SuspInfoAnnotation();
      suspInfo.dynamicDependencyMultiplier = getAmplifyingFactor(depNode);
      if (depNode instanceof InstructionDependencySource) {
        SourceLocation srcLoc = JpfEntityConversion.getSourceLocationFromInstruction(
            ((InstructionDependencySource) depNode).getSourceInstruction());
        suspInfo.baseSuspiciousness = targetInvocInfo.flResults.getSuspiciousness(srcLoc);
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
              currentSusp.getAdjustedSuspiciousness(), 
              highestSusp.getAdjustedSuspiciousness()) > 0) {
            highestSusp = currentSusp;
          }
        }
        if (highestSusp != null) {
          suspInfo.baseSuspiciousness = highestSusp.baseSuspiciousness;
        }
        depNode.addAnnotation(suspInfo);
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
  
  private static class IncorrectValueDependencyVisitor extends DepBreadthFirstTraversal {
    
    private Range<Long> traversalRange;
    
    public IncorrectValueDependencyVisitor(Range<Long> traversalRange) {
      this.traversalRange = traversalRange;
    }

    @Override
    protected boolean visit(DynamicDependency depNode) {
      if (!traversalRange.contains(depNode.getInstanceIndex())) {
        return false;
      }
      SuspInfoAnnotation suspAnnotation = depNode.getAnnotation(SuspInfoAnnotation.class);
      if (suspAnnotation != null) {
        suspAnnotation.dynamicDependencyMultiplier++;
      }
      return true;
    }
  }
  
  private static class FieldSuspiciousnessComparator implements Comparator<Pair<RefPath, Double>> {

    @Override
    public int compare(Pair<RefPath, Double> o1, Pair<RefPath, Double> o2) {
      return FloatComparison.compareDouble(o1.getSecond(), o2.getSecond());
    }
    
  }
}
