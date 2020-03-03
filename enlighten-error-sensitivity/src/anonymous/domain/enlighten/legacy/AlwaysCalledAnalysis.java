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


package anonymous.domain.enlighten.legacy;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class AlwaysCalledAnalysis {
  
  private SubjectProgram subject;
  private ExecutionProfile profile;
  
  private Map<MethodName, Set<MethodName>> alwaysCalledByMap;

  public AlwaysCalledAnalysis(SubjectProgram subject, ExecutionProfile profile) {
    this.subject = subject;
    this.profile = profile;
    init();
  }

  public static void main(String[] args) throws IOException {
    SubjectProgram subject = SubjectProgram.openSubjectProgram(
        Paths.get("subjects", args[0]), Paths.get("out", args[0]));
    Path executionProfileDir = subject.getExecutionProfileDir();
    PrintWriter writer = new PrintWriter(new FileWriter(executionProfileDir.resolve("always_called.txt").toString()));
    File[] profiles = executionProfileDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".profile");
      }
    });
    for (File profileFile : profiles) {
      ExecutionProfile profileData = ExecutionProfile.readFromDataFile(profileFile.toPath());
      Map<MethodName, Set<MethodName>> alwaysCalledBy = 
          new AlwaysCalledAnalysis(subject, profileData).getAlwaysCalledRelations();
      writer.println("===============Execution: " + profileData.getExecutionId() + "=====================");
      for (MethodName method : alwaysCalledBy.keySet()) {
        writer.print(method.toString() + ": [");
        for (MethodName caller : alwaysCalledBy.get(method)) {
          writer.print(caller.toString() + ", ");
        }
        writer.println("]");
      }
      writer.println("===============End " + profileData.getExecutionId() + "=====================");
    }
    writer.close();
  }

  public Map<MethodName, Set<MethodName>> getAlwaysCalledRelations() {
    return alwaysCalledByMap;
  }

  private void init() {
    alwaysCalledByMap = new HashMap<>();
    traverse(profile.getInvocationTreeRoot(), new ArrayList<MethodName>());
  }
  
  private void traverse(MethodInvocation node, List<MethodName> parentCalls) {
    MethodName methodName = node.getMethodName();
    if (methodName.getMethodNameSig().contains("isKeyword") && node.getPostState() == null) {
      System.out.println("xxx");
    }
    Set<MethodName> alwaysCalledBy = alwaysCalledByMap.get(methodName);
    if (alwaysCalledBy == null) {
      alwaysCalledBy = new HashSet<>();
      for (MethodName parentCall : parentCalls) {
        if (subject.isAppClass(parentCall.getClassName())) {
          alwaysCalledBy.add(parentCall);
        }
      }
      alwaysCalledByMap.put(methodName, alwaysCalledBy);
    } else {
      List<MethodName> entriesToRemove = new ArrayList<>();
      for (MethodName caller : alwaysCalledBy) {
        if (!parentCalls.contains(caller)) {
          entriesToRemove.add(caller);
        }
      }
      for (MethodName toRemove : entriesToRemove) {
        alwaysCalledBy.remove(toRemove);
      }
    }
    parentCalls.add(methodName);
    for (MethodInvocation invoked : node.getEnclosedInvocations()) {
      traverse(invoked, parentCalls);
    }
    MethodName verify = parentCalls.remove(parentCalls.size() - 1);
    if (verify != methodName) {
      throw new RuntimeException("???");
    }
  }
}
