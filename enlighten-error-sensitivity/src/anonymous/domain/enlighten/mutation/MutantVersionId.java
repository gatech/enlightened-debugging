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


package anonymous.domain.enlighten.mutation;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MutantVersionId implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("(.*)-mutant-(\\d+)");
  
  private String subjectName;
  private int mutantId;
  
  public static MutantVersionId parseFromMutantVersionName(String versionName) {
    Matcher versionNameMatcher = VERSION_NAME_PATTERN.matcher(versionName);
    if (versionNameMatcher.find()) {
      String subjectName = versionNameMatcher.group(1);
      int mutantId = Integer.parseInt(versionNameMatcher.group(2));
      return new MutantVersionId(subjectName, mutantId);
    } else {
      throw new IllegalArgumentException("Invalid mutant version name format: " + versionName);
    }
  }
  
  public MutantVersionId(String subjectName, int mutantId) {
    this.subjectName = subjectName;
    this.mutantId = mutantId;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public int getMutantId() {
    return mutantId;
  }
  
  @Override
  public int hashCode() {
    return subjectName.hashCode() ^ mutantId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    MutantVersionId another = (MutantVersionId) o;
    return subjectName.equals(another.subjectName) && mutantId == another.mutantId;
  }
  
  @Override
  public String toString() {
    return subjectName + "-mutant-" + mutantId;
  }
}
