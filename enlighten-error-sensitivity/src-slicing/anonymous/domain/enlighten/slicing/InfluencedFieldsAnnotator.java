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
import java.util.HashSet;
import java.util.Set;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.data.MethodInvocation;

public class InfluencedFieldsAnnotator {
  
  public static void addInfluencedField(MethodInvocation invocation, FieldRef field) {
    InfluencedFieldsAnnotation annotation = 
        invocation.getAnnotation(InfluencedFieldsAnnotation.class);
    if (annotation == null) {
      annotation = new InfluencedFieldsAnnotation();
      invocation.addAnnotation(annotation);
    }
    annotation.influencedFields.add(field);
  }
  
  public static void clearInfluencedField(MethodInvocation invocation) {
    InfluencedFieldsAnnotation annotation = 
        invocation.getAnnotation(InfluencedFieldsAnnotation.class);
    if (annotation != null) {
      annotation.influencedFields.clear();
    }
  }
  
  public static Set<FieldRef> getInfluencedFields(MethodInvocation invocation) {
    InfluencedFieldsAnnotation annotation = 
        invocation.getAnnotation(InfluencedFieldsAnnotation.class);
    if (annotation != null) {
      return Collections.unmodifiableSet(annotation.influencedFields);
    } else {
      return Collections.emptySet();
    }
  }

  private static final class InfluencedFieldsAnnotation implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    
    private Set<FieldRef> influencedFields = new HashSet<>();
  }
}
