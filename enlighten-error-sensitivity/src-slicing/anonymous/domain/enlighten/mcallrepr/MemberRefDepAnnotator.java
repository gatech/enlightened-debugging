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

import java.util.HashMap;
import java.util.Map;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.mcallrepr.MemberRefName;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.refpath.RefPath;

public class MemberRefDepAnnotator {
  
  public static void annotateDependency(
      ValueGraphNode obj, MemberRefName ref, DynamicDependency dep) {
    getOrCreateDependencyAnnotation(obj).memberDeps.put(ref, dep);
  }
  
  public static void annotateDependency(
      ValueGraphNode obj, RefPath refPath, DynamicDependency dep) {
    annotateDependency(refPath.getLastButOne(obj), refPath.getTail(), dep);
  }
  
  public static DynamicDependency getDependency(ValueGraphNode obj, MemberRefName ref) {
    AnnotationData annotation = obj.getAnnotation(AnnotationData.class);
    if (annotation != null) {
      return annotation.memberDeps.get(ref);
    }
    return null;
  }
  
  public static DynamicDependency getDependency(ValueGraphNode obj, RefPath refPath) {
    return getDependency(refPath.getLastButOne(obj), refPath.getTail());
  }
  
  private static AnnotationData getOrCreateDependencyAnnotation(ValueGraphNode obj) {
    AnnotationData annotation = obj.getAnnotation(AnnotationData.class);
    if (annotation == null) {
      annotation = new AnnotationData();
      obj.addAnnotation(annotation);
    }
    return annotation;
  }

  private static final class AnnotationData implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    private Map<MemberRefName, DynamicDependency> memberDeps = new HashMap<>();
  }
}
