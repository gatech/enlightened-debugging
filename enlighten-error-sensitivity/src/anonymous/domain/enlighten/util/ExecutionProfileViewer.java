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


package anonymous.domain.enlighten.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import anonymous.domain.enlighten.data.ExecutionProfile;
import anonymous.domain.enlighten.data.MethodInvocation;
import anonymous.domain.enlighten.data.MethodName;

public class ExecutionProfileViewer {
  
  private MethodInvocation root;
  private PrintWriter writer;
  
  private Map<MethodName, Integer> nextInvocationIndex;

  public static void main(String[] args) throws IOException {
    Path dataFilePath = Paths.get(args[0]);
    Path outputFile = 
        dataFilePath.getParent().resolve(dataFilePath.getFileName().toString() + ".txt");
    ExecutionProfile profile = ExecutionProfile.readFromDataFile(dataFilePath);
    new ExecutionProfileViewer(
        profile.getInvocationTreeRoot(), 
        new PrintWriter(new FileWriter(outputFile.toString())))
          .printMethodInvocationTree();
  }
  
  public ExecutionProfileViewer(MethodInvocation root, PrintWriter writer) {
    this.root = root;
    this.writer = writer;
  }
  
  public void printMethodInvocationTree() {
    nextInvocationIndex = new HashMap<>();
    printMethodEntryRecursively(root, 0);
    writer.flush();
  }
  
  private void printMethodEntryRecursively(MethodInvocation invocationNode, int depth) {
    for (int i = 0; i < depth; ++i) {
      writer.print(" ");
    }
    MethodName methodName = invocationNode.getMethodName();
    int invocationIndex = 
        nextInvocationIndex.containsKey(methodName) ? nextInvocationIndex.get(methodName) : 0;
    nextInvocationIndex.put(methodName, invocationIndex + 1);
    String preStateHash = invocationNode.getPreState() != null ? 
        String.valueOf(invocationNode.getPreState().hashCode()) : "null";
    String postStateHash = invocationNode.getPostState() != null ? 
        String.valueOf(invocationNode.getPostState().hashCode()) : "null";
    writer.println(methodName + " #" + invocationIndex 
        + " [PRE:" + preStateHash + "]"
        + " [POST:" + postStateHash + "]");
    for (MethodInvocation child : invocationNode.getEnclosedInvocations()) {
      printMethodEntryRecursively(child, depth + 1);
    }
  }
}
