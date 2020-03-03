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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TestOutcomes implements Serializable {

  private static final long serialVersionUID = 1L;

  private Map<TestName, Boolean> testOutcomes = new HashMap<>();
  
  public static TestOutcomes readTestOutcomesFromFile(Path dataFile) throws IOException {
    TestOutcomes testOutcomes = new TestOutcomes();
    List<String> lines = Files.readAllLines(dataFile);
    for (String testOutcomeRecord : lines) {
      if (testOutcomeRecord.isEmpty()) {
        continue;
      }
      String[] components = testOutcomeRecord.split(Pattern.quote(" "));
      if (components.length != 2) {
        DataFormatErrorUtils.dataFormatError("test outcomes", dataFile, testOutcomeRecord);
      }
      TestName testName = null;
      try {
        testName = TestName.parseFromDescription(components[0]);
      } catch (IllegalArgumentException e) {
        DataFormatErrorUtils.dataFormatError("test outcomes", dataFile, testOutcomeRecord);
      }
      boolean passed = false;
      if ("[PASSED]".equals(components[1])) {
        passed = true;
      } else if (!"[FAILED]".equals(components[1])) {
        DataFormatErrorUtils.dataFormatError("test outcomes", dataFile, testOutcomeRecord);
      }
      testOutcomes.addTestOutcome(testName, passed);
    }
    return testOutcomes;
  }
  
  public void addTestOutcome(TestName test, boolean passed) {
    testOutcomes.put(test, passed);
  }
  
  public void removeTestOutcome(TestName test) {
    testOutcomes.remove(test);
  }
  
  public Set<TestName> getTestSet() {
    return testOutcomes.keySet();
  }
  
  public boolean isPassed(TestName test) {
    if (!testOutcomes.containsKey(test)) {
      throw new IllegalArgumentException(
          "Test " + test.toString() + " is not included in this TestOutcomes object.");
    }
    return testOutcomes.get(test);
  }
  
  public void writeTestOutcomes(Path dataFile) throws IOException {
    StringBuilder buffer = new StringBuilder();
    for (TestName test : testOutcomes.keySet()) {
      boolean passed = testOutcomes.get(test);
      buffer.append(test.getDescription());
      buffer.append(" ");
      if (passed) {
        buffer.append("[PASSED]");
      } else {
        buffer.append("[FAILED]");
      }
      buffer.append('\n');
    }
    Files.createDirectories(dataFile.getParent());
    Files.write(dataFile, buffer.toString().getBytes());
  }
}
