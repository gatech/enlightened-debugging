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


package anonymous.domain.enlighten.subjectmodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class MutantSubject extends SubjectProgram {
  
  private SubjectProgram baseVersion;
  
  private Set<Class<?>> testClasses;

  public MutantSubject(Path rootDir, Path dataDir, SubjectProgram baseVersion) throws IOException {
    super(rootDir, dataDir);
    this.baseVersion = baseVersion;
  }
  
  @Override
  public Path getAppClassFilePath(String className) {
    return getRootDir().resolve("classes").resolve(className.replace('.', '/') + ".class");
  }
  
  @Override
  public boolean isAppClass(String className) {
    return baseVersion.isAppClass(className);
  }

  @Override
  public Set<Class<?>> listTestClasses() throws IOException {
    if (testClasses != null) {
      return testClasses;
    }
    testClasses = new HashSet<>();
    Set<Class<?>> baseVersionTestClasses = baseVersion.listTestClasses();
    for (Class<?> testClass : baseVersionTestClasses) {
      String testClassName = testClass.getName();
      try {
        testClasses.add(loadClass(testClassName));
      } catch (ClassNotFoundException ex) {
        throw new RuntimeException("Internal error: Could not load class " + testClassName);
      }
    }
    return testClasses;
  }
}
