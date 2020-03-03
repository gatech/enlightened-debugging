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


package anonymous.domain.enlighten.data;

import java.io.Serializable;

public class TestName implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private String testClassName;
  private String testMethodName;
  private int instanceIndex;
  
  public static TestName parseFromDescription(String testDescription) {
    int firstSeparatorIndex = testDescription.indexOf('(');
    int secondSeparatorIndex = testDescription.indexOf(')');
    if (firstSeparatorIndex == -1 || secondSeparatorIndex == -1) {
      throw new IllegalArgumentException(
          "\"" + testDescription + "\" is not a valid test description string.");
    }
    String testMethodName = testDescription.substring(0, firstSeparatorIndex);
    String testClassName = testDescription.substring(firstSeparatorIndex + 1, secondSeparatorIndex);
    String instanceIndexStr = testDescription.substring(secondSeparatorIndex + 1);
    int instanceIndex = 0;
    if (!instanceIndexStr.isEmpty()) {
      instanceIndex = Integer.parseInt(instanceIndexStr);
    }
    return new TestName(testClassName, testMethodName, instanceIndex);
  }
  
  public TestName(String testClassName, String testMethodName) {
    this(testClassName, testMethodName, 0);
  }
  
  public TestName(String testClassName, String testMethodName, int instanceIndex) {
    this.testClassName = testClassName;
    this.testMethodName = testMethodName;
    this.instanceIndex = instanceIndex;
  }

  public String getTestClassName() {
    return testClassName;
  }

  public void setTestClassName(String testClassName) {
    this.testClassName = testClassName;
  }

  public String getTestMethodName() {
    return testMethodName;
  }

  public void setTestMethodName(String testMethodName) {
    this.testMethodName = testMethodName;
  }
  
  public String getTestMethodLongName() {
    return testClassName + "." + testMethodName;
  }

  public int getInstanceIndex() {
    return instanceIndex;
  }

  public void setInstanceIndex(int instanceIndex) {
    this.instanceIndex = instanceIndex;
  }
  
  public String getDescription() {
    return testMethodName + "(" + testClassName + ")" 
        + (instanceIndex == 0 ? "" : String.valueOf(instanceIndex));
  }
  
  @Override
  public String toString() {
    return getDescription();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TestName) {
      return getTestClassName().equals(((TestName) o).getTestClassName())
          && getTestMethodName().equals(((TestName) o).getTestMethodName())
          && getInstanceIndex() == ((TestName) o).getInstanceIndex();
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return getTestClassName().hashCode() ^ getTestMethodName().hashCode() ^ getInstanceIndex();
  }
}
