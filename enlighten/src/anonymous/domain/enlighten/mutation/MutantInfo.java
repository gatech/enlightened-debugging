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

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.SourceLocation;

public class MutantInfo implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private String mutantDescription;
  private String srcFilePath;
  
  private String type;
  private int serialNum;
  private int lineNum;
  
  private int globalId = -1;
  private MethodName containingMethod;
  
  public MutantInfo(String logLine, String srcFilePath) {
    mutantDescription = logLine;
    this.srcFilePath = srcFilePath;
    parse();
  }

  public String getType() {
    return type;
  }

  public String getSrcFilePath() {
    return srcFilePath;
  }

  public int getSerialNum() {
    return serialNum;
  }

  public int getLineNum() {
    return lineNum;
  }
  
  
  public int getGlobalId() {
    return globalId;
  }
  
  public SourceLocation getSourceLocation() {
    String normalizedRelPath = srcFilePath.replace('\\', '/');
    return SourceLocation.get(normalizedRelPath, lineNum);
  }

  public void setGlobalId(int globalId) {
    this.globalId = globalId;
  }

  public MethodName getContainingMethod() {
    return containingMethod;
  }

  public void setContainingMethod(MethodName containingMethod) {
    this.containingMethod = containingMethod;
  }

  @Override
  public int hashCode() {
    return mutantDescription.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    MutantInfo another = (MutantInfo) o;
    return mutantDescription.equals(another.mutantDescription);
  }
  
  @Override
  public String toString() {
    return mutantDescription;
  }
  
  private void parse() {
    String[] parts = mutantDescription.split(":");
    serialNum = Integer.parseInt(parts[0]);
    type = parts[1];
    lineNum = Integer.parseInt(parts[5]);
  }
}
