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


package anonymous.domain.enlighten.mcallrepr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.annotation.ValueAnnotation;

public class TransitivelyAccessedAnnotator {
  
  public static boolean isTransitivelyAccessed(ReferenceRepr refRepr) {
    TransitivelyAccessed annotationData = refRepr.getAnnotation(TransitivelyAccessed.class);
    return annotationData != null;
  }
  
  public static void annotateObjectSet(List<ReferenceRepr> objectSet) {
    LinkedList<ReferenceRepr> workingList = new LinkedList<>();
    for (ReferenceRepr refRepr : objectSet) {
      if (!isObjectRepr(refRepr)) {
        continue;
      }
      workingList.addLast(refRepr);
    }
    LinkedList<ReferenceRepr> accessStatusChangedObjects = new LinkedList<>();
    while (!workingList.isEmpty()) {
      ReferenceRepr currentObj = workingList.removeFirst();
      TransitivelyAccessed transAccessedData = new TransitivelyAccessed();
      currentObj.addAnnotation(transAccessedData);
      Map<MemberRefName, ValueGraphNode> membersMap = currentObj.getReferencedValues();
      for (MemberRefName fieldName : membersMap.keySet()) {
        ValueGraphNode fieldValue = membersMap.get(fieldName);
        if (MemberRefAccessedAnnotator.isMemberAccessed(currentObj, fieldName)) {
          transAccessedData.isAccessed = true;
          accessStatusChangedObjects.addLast(currentObj);
        }
        if (isObjectRepr(fieldValue)) {
          ReferenceRepr referencedObject = (ReferenceRepr) fieldValue;
          getOrInitReferencedByList(referencedObject).add(currentObj);
          if (referencedObject.getAnnotation(TransitivelyAccessed.class) == null) {
            workingList.addLast(referencedObject);
          }
        }
      }
    }
    while (!accessStatusChangedObjects.isEmpty()) {
      ReferenceRepr currentObj = accessStatusChangedObjects.removeFirst();
      List<ReferenceRepr> referencedByList = getOrInitReferencedByList(currentObj);
      for (ReferenceRepr predObj : referencedByList) {
        TransitivelyAccessed predObjAccessStatus = 
            predObj.getAnnotation(TransitivelyAccessed.class);
        if (!predObjAccessStatus.isAccessed) {
          predObjAccessStatus.isAccessed = true;
          accessStatusChangedObjects.addLast(predObj);
        }
      }
    }
    workingList.clear();
    for (ReferenceRepr refRepr : objectSet) {
      if (!isObjectRepr(refRepr)) {
        continue;
      }
      workingList.addLast(refRepr);
    }
    Set<ReferenceRepr> visited = Collections.newSetFromMap(
        new IdentityHashMap<ReferenceRepr, Boolean>());
    while (!workingList.isEmpty()) {
      ReferenceRepr currentObj = workingList.removeFirst();
      visited.add(currentObj);
      currentObj.removeAnnotation(ReferencedBy.class);
      TransitivelyAccessed transAccessedAnnotation = 
          currentObj.getAnnotation(TransitivelyAccessed.class);
      if (!transAccessedAnnotation.isAccessed) {
        currentObj.removeAnnotation(transAccessedAnnotation);
      }
      for (ValueGraphNode fieldValue : currentObj.getReferencedValues().values()) {
        if (isObjectRepr(fieldValue)) {
          if (!visited.contains(fieldValue)) {
            workingList.addLast((ReferenceRepr) fieldValue);
          }
        }
      }
    }
  }
  
  private static List<ReferenceRepr> getOrInitReferencedByList(ReferenceRepr refRepr) {
    ReferencedBy referencedByData = refRepr.getAnnotation(ReferencedBy.class);
    if (referencedByData == null) {
      referencedByData = new ReferencedBy();
      refRepr.addAnnotation(referencedByData);
    }
    return referencedByData.referencedBy;
  }
  
  private static boolean isObjectRepr(ValueGraphNode value) {
    return value instanceof ReflectedObjectRepr || value instanceof ArrayRepr;
  }

  private static final class TransitivelyAccessed implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    private transient boolean isAccessed = false;
  }
  
  private static final class ReferencedBy implements ValueAnnotation {

    private static final long serialVersionUID = 1L;
    private transient List<ReferenceRepr> referencedBy = new ArrayList<>();
  }
}
