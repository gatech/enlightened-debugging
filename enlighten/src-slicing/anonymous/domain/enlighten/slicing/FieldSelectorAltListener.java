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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.SourceLocation;
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
import anonymous.domain.enlighten.slicing.util.JpfEntityConversion;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class FieldSelectorAltListener extends ExecTreeProcessingListener {
  
  private static final int SUSP_LOCS_CAPACITY = 100;
  
  private FieldSelectionCriterionAlt selectionCriterion;
  
  private Set<SuspLocationCandidate> visitedSuspLocs = new HashSet<>();


  private PriorityQueue<SuspLocationCandidate> mostSuspLocs = new PriorityQueue<>(
      new LocCandidateSuspComparator());
  private Map<SuspLocationCandidate, Double> candidateLocSusp = new HashMap<>();
  private Map<MethodInvocation, Set<SourceLocation>> candidateSuspLocsByInvocs = 
      new HashMap<>();
  
  public FieldSelectorAltListener(
      SubjectProgram subject, FieldSelectionCriterionAlt selectionCriterion) {
    super(subject, selectionCriterion.executionProfile);
    this.selectionCriterion = selectionCriterion;
    setRequireDeterministicExecution(true);
  }

  @Override
  public void instructionDependencySourceGenerated(
      DependencyTrackingInstruction insn, InstructionDependencySource depNode) {

    SourceLocation srcLoc = JpfEntityConversion.getSourceLocationFromInstruction(insn);
    double baseSusp = selectionCriterion.flResults.getSuspiciousness(srcLoc);
    if (baseSusp == 0) {
      return;
    }
    setDependencySuspiciousness(depNode, baseSusp);


    SuspLocationCandidate candidateLoc = 
        new SuspLocationCandidate(getCurrentInvocation(), srcLoc);
    if (visitedSuspLocs.contains(candidateLoc)) {

      return;
    }
    visitedSuspLocs.add(candidateLoc);
    addSuspiciousLocCandidate(candidateLoc, baseSusp);
  }

  @Override
  protected void invocationEntered(MethodInvocation enteredInvocation,
      ThreadInfo currentThread) {
    if (selectionCriterion.incorrectInputValues.containsKey(enteredInvocation)) {
      Set<RefPath> incorrectValues = 
          selectionCriterion.incorrectInputValues.get(enteredInvocation);
      processIncorrectValues(incorrectValues, currentThread, Range.all());
    }
  }

  @Override
  protected void invocationExited(MethodInvocation exitedInvocation,
      ThreadInfo currentThread) {
    Set<SourceLocation> suspLocations = candidateSuspLocsByInvocs.get(exitedInvocation);
    if (suspLocations != null && suspLocations.size() > 0) {




      SuspLocReachabilityVerifier suspLocVerifier = new SuspLocReachabilityVerifier(
          DepIndexRangeAnnotator.getDirectContainingIndices(exitedInvocation), suspLocations);
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
          suspLocVerifier.traverse(MemberRefDepAnnotator.getDependency(mcall, currentPath));
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
      Set<SourceLocation> verifiedSuspLocs = suspLocVerifier.foundLocs;
      if (verifiedSuspLocs.size() < suspLocations.size()) {
        candidateSuspLocsByInvocs.put(exitedInvocation, verifiedSuspLocs);
        for (SourceLocation suspLoc : suspLocations) {
          if (!verifiedSuspLocs.contains(suspLoc)) {
            removeSuspiciousLocCandidate(new SuspLocationCandidate(exitedInvocation, suspLoc));
          }
        }
      }
    }
    if (selectionCriterion.incorrectOutputValues.containsKey(exitedInvocation)) {
      Set<RefPath> incorrectValues = 
          selectionCriterion.incorrectOutputValues.get(exitedInvocation);
      Range<Long> effectiveRange = Range.closed(
          DepIndexRangeAnnotator.getStartIndex(exitedInvocation), 
          DepIndexRangeAnnotator.getEndIndex(exitedInvocation));
      processIncorrectValues(incorrectValues, currentThread, effectiveRange);
    }
    if (exitedInvocation == getTestMethodInvocation()) {
      Set<RefPath> incorrectValues = new HashSet<>();
      incorrectValues.add(RefPath.newBuilder().appendExceptionRef().build());
      processIncorrectValues(incorrectValues, currentThread, Range.all());
    }
  }
  
  public MethodInvocation getSelectedInvocation() {
    PriorityQueue<SuspLocationCandidate> sorting = new PriorityQueue<>(mostSuspLocs);
    SuspLocationCandidate bestCandidate = null;
    while (!sorting.isEmpty()) {
      bestCandidate = sorting.poll();
    }
    return bestCandidate.invocation;
  }
  
  private boolean addSuspiciousLocCandidate(SuspLocationCandidate locCandidate, double suspiciousness) {
    if (suspiciousness == 0) {
      return false;
    }
    candidateLocSusp.put(locCandidate, suspiciousness);
    if (mostSuspLocs.size() < SUSP_LOCS_CAPACITY) {
      mostSuspLocs.add(locCandidate);
      addToInvocCandidateDeps(locCandidate);
      return true;
    } else {
      SuspLocationCandidate existingLeastSuspicious = mostSuspLocs.peek();
      if (mostSuspLocs.comparator().compare(locCandidate, existingLeastSuspicious) <= 0) {
        candidateLocSusp.remove(locCandidate);
        return false;
      } else {
        SuspLocationCandidate removed = mostSuspLocs.poll();
        candidateLocSusp.remove(removed);
        removeFromInvocCandidateDeps(removed);
        mostSuspLocs.add(locCandidate);
        addToInvocCandidateDeps(locCandidate);
        return true;
      }
    }
  }
  
  private void removeSuspiciousLocCandidate(SuspLocationCandidate locCandidate) {
    mostSuspLocs.remove(locCandidate);
    candidateLocSusp.remove(locCandidate);
  }
  
  private void addToInvocCandidateDeps(SuspLocationCandidate locCandidate) {
    MethodInvocation invoc = locCandidate.invocation;
    SourceLocation srcLoc = locCandidate.srcLoc;
    Set<SourceLocation> candidates = candidateSuspLocsByInvocs.get(invoc);
    if (candidates == null) {
      candidates = new HashSet<>();
      candidateSuspLocsByInvocs.put(invoc, candidates);
    }
    candidates.add(srcLoc);
  }
  
  private void removeFromInvocCandidateDeps(SuspLocationCandidate locCandidate) {
    MethodInvocation invoc = locCandidate.invocation;
    SourceLocation srcLoc = locCandidate.srcLoc;
    Set<SourceLocation> candidates = candidateSuspLocsByInvocs.get(invoc);
    if (candidates == null || !candidates.remove(srcLoc)) {
      throw new IllegalArgumentException("Removing non-existent suspicious location candidate.");
    }
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
  
  private static void setDependencySuspiciousness(
      DynamicDependency dep, double suspiciousness) {
    SuspInfoAnnotation suspInfo = dep.getAnnotation(SuspInfoAnnotation.class);
    if (suspInfo == null) {
      suspInfo = new SuspInfoAnnotation();
      dep.addAnnotation(suspInfo);
    }
    suspInfo.baseSuspiciousness = suspiciousness;
  }
  
  private static boolean isDataFieldSelectionCandidate(ValueGraphNode value) {
    if (value == null) {
      return false;
    }


    return value instanceof PrimitiveRepr || value instanceof NullRepr;
  }

  private class LocCandidateSuspComparator implements Comparator<SuspLocationCandidate> {

    @Override
    public int compare(SuspLocationCandidate o1, SuspLocationCandidate o2) {
      double susp1 = 0;
      if (candidateLocSusp.containsKey(o1)) {
        susp1 = candidateLocSusp.get(o1);
      }
      double susp2 = 0;
      if (candidateLocSusp.containsKey(o2)) {
        susp2 = candidateLocSusp.get(o2);
      }
      if (susp1 > susp2) {
        return 1;
      } else if (susp1 < susp2) {
        return -1;
      } else {
        return 0;
      }
    }
    
  }
  
  private static class SuspInfoAnnotation implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    
    private double baseSuspiciousness = 0;
    private double dynamicDependencyMultiplier = 1.0;
    
    private boolean reachableForFeedback = false;
    
    public double getAdjustedSuspiciousness() {
      return baseSuspiciousness * dynamicDependencyMultiplier;
    }
  }
  
  private static class SuspLocationCandidate {
    
    private MethodInvocation invocation;
    private SourceLocation srcLoc;
    
    public SuspLocationCandidate(MethodInvocation invocation, SourceLocation srcLoc) {
      this.invocation = invocation;
      this.srcLoc = srcLoc;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o != null && getClass() == o.getClass()) {
        SuspLocationCandidate another = (SuspLocationCandidate) o;
        return Objects.equals(invocation, another.invocation) 
            && Objects.equals(srcLoc, another.srcLoc);
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return invocation.hashCode() ^ srcLoc.hashCode();
    }
  }
  
  private static class SuspLocReachabilityVerifier extends DepBreadthFirstTraversal {
    
    private RangeSet<Long> effectiveRange;
    private Range<Long> traversalRange;
    private Set<SourceLocation> searchedLocs;
    private Set<SourceLocation> foundLocs = new HashSet<>();
    
    public SuspLocReachabilityVerifier(
        RangeSet<Long> effectiveRange, Set<SourceLocation> searchedLocs) {
      this.effectiveRange = effectiveRange;
      this.traversalRange = effectiveRange.span();
      this.searchedLocs = searchedLocs;
    }

    @Override
    protected boolean visit(DynamicDependency depNode) {
      if (!traversalRange.contains(depNode.getInstanceIndex())) {
        return false;
      }
      if (effectiveRange.contains(depNode.getInstanceIndex()) 
          && depNode instanceof InstructionDependencySource) {
        SuspInfoAnnotation suspInfo = depNode.getAnnotation(SuspInfoAnnotation.class);
        if (suspInfo != null) {
          suspInfo.reachableForFeedback = true;
        }
        SourceLocation loc = JpfEntityConversion.getSourceLocationFromInstruction(
            ((InstructionDependencySource) depNode).getSourceInstruction());
        if (searchedLocs.contains(loc)) {
          foundLocs.add(loc);
        }
      }
      return true;
    }
  }
  
  private class IncorrectValueDependencyVisitor extends DepBreadthFirstTraversal {
    
    private Range<Long> traversalRange;
    
    public IncorrectValueDependencyVisitor(Range<Long> traversalRange) {
      this.traversalRange = traversalRange;
    }

    @Override
    protected boolean visit(DynamicDependency depNode) {
      if (!traversalRange.contains(depNode.getInstanceIndex())) {
        return false;
      }
      if (depNode instanceof InstructionDependencySource) {
        SuspInfoAnnotation suspAnnotation = depNode.getAnnotation(SuspInfoAnnotation.class);
        if (suspAnnotation != null && suspAnnotation.reachableForFeedback) {
          suspAnnotation.dynamicDependencyMultiplier++;
          MethodInvocation generatingInvocation = 
              lookupDependencyGeneratingInvocation(depNode.getInstanceIndex());
          SourceLocation srcLoc = JpfEntityConversion.getSourceLocationFromInstruction(
              ((InstructionDependencySource) depNode).getSourceInstruction());
          SuspLocationCandidate locCandidate = 
              new SuspLocationCandidate(generatingInvocation, srcLoc);
          double newSusp = suspAnnotation.getAdjustedSuspiciousness();
          if (candidateLocSusp.containsKey(locCandidate)) {
            mostSuspLocs.remove(locCandidate);
            addSuspiciousLocCandidate(locCandidate, newSusp);
          } else {
            addSuspiciousLocCandidate(locCandidate, newSusp);
          }
        }
      }
      return true;
    }
  }
}
