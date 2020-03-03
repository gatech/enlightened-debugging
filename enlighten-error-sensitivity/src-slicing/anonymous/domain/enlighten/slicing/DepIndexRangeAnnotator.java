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

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import anonymous.domain.enlighten.annotation.ValueAnnotation;
import anonymous.domain.enlighten.data.MethodInvocation;

public class DepIndexRangeAnnotator {
  
  public static void setStartIndex(MethodInvocation invocation, long startIndex) {
    DepIndexRange annotation = invocation.getAnnotation(DepIndexRange.class);
    if (annotation == null) {
      annotation = new DepIndexRange();
      invocation.addAnnotation(annotation);
    }
    annotation.startIndex = startIndex;
  }
  
  public static void setEndIndex(MethodInvocation invocation, long endIndex) {
    DepIndexRange annotation = invocation.getAnnotation(DepIndexRange.class);
    if (annotation == null) {
      annotation = new DepIndexRange();
      invocation.addAnnotation(annotation);
    }
    annotation.endIndex = endIndex;
  }
  
  public static long getStartIndex(MethodInvocation invocation) {
    DepIndexRange annotation = invocation.getAnnotation(DepIndexRange.class);
    if (annotation != null) {
      return annotation.startIndex;
    } else {
      return -1;
    }
  }
  
  public static long getEndIndex(MethodInvocation invocation) {
    DepIndexRange annotation = invocation.getAnnotation(DepIndexRange.class);
    if (annotation != null) {
      return annotation.endIndex;
    } else {
      return -1;
    }
  }
  
  public static RangeSet<Long> getDirectContainingIndices(MethodInvocation invocation) {
    if (getStartIndex(invocation) == -1 || getEndIndex(invocation) == -1) {
      throw new IllegalStateException(
          "Dependency index range of the specified invocation is incomplete.");
    }
    RangeSet<Long> directContainingRange = TreeRangeSet.create();
    long rangeStart = DepIndexRangeAnnotator.getStartIndex(invocation);
    long rangeEnd = -1;
    for (MethodInvocation child : invocation.getEnclosedInvocations()) {
      rangeEnd = DepIndexRangeAnnotator.getStartIndex(child) - 1;
      if (rangeEnd >= rangeStart) {
        directContainingRange.add(Range.closed(rangeStart, rangeEnd));
      }
      rangeStart = DepIndexRangeAnnotator.getEndIndex(child) + 1;
    }
    rangeEnd = DepIndexRangeAnnotator.getEndIndex(invocation);
    if (rangeEnd >= rangeStart) {
      directContainingRange.add(Range.closed(rangeStart, rangeEnd));
    }
    return directContainingRange;
  }

  private static final class DepIndexRange implements ValueAnnotation {
    private static final long serialVersionUID = 1L;
    private long startIndex = -1;
    private long endIndex = -1;
  }
}
