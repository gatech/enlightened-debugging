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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.analysis.ControlFlow.Block;

public class ControlDependencyInfo {


  private Map<Block, List<Integer>> blockDependencies;
  private RangeMap<Integer, Block> instrPosBlockMap;
  
  private List<CatchBodyInfo> exceptionHandlers = new ArrayList<>();
  
  public ControlDependencyInfo() {
    blockDependencies = new HashMap<>();
    instrPosBlockMap = TreeRangeMap.create();
  }
  
  public void addDependency(Block dependantBlock, int dependencyIndex) {
    addBlockInfo(dependantBlock);
    getOrCreateBlockDependencies(dependantBlock).add(dependencyIndex);
  }

  
  public List<Integer> getControlDependencies(int dependantIndex) {
    Block containingBlock = instrPosBlockMap.get(dependantIndex);
    return Collections.unmodifiableList(getBlockDependencies(containingBlock));
  }

  public void addExceptionHandlerInfo(CatchBodyInfo handlerInfo) {
    exceptionHandlers.add(handlerInfo);
  }
  
  
  public CatchBodyInfo getContainingHandler(int instrIndex) {
    for (CatchBodyInfo handlerInfo : exceptionHandlers) {
      if (handlerInfo.contains(instrIndex)) {
        return handlerInfo;
      }
    }
    return null;
  }
  
  public List<CatchBodyInfo> getExceptionHandlerInfo() {
    return Collections.unmodifiableList(exceptionHandlers);
  }
  
  public void debugPrint(MethodInfo methodInfo) throws BadBytecode {
    CodeIterator codeItr = methodInfo.getCodeAttribute().iterator();
    while (codeItr.hasNext()) {
      int instIndex = codeItr.next();
      List<Integer> dependencies = getControlDependencies(instIndex);
      for (Integer dependency : dependencies) {
        System.out.println(instIndex + " --> " + dependency);
      }
    }
  }
  
  public void debugPrintBySourceLines(MethodInfo methodInfo) throws Throwable {
    Map<Integer, List<Integer>> depBySourceLines = new HashMap<>();
    CodeIterator codeItr = methodInfo.getCodeAttribute().iterator();
    while (codeItr.hasNext()) {
      int instIndex = codeItr.next();
      List<Integer> dependencies = getControlDependencies(instIndex);
      int instLineNum = methodInfo.getLineNumber(instIndex);
      ArrayList<Integer> depLineNums = new ArrayList<>();
      depBySourceLines.put(instLineNum, depLineNums);
      for (Integer dependency : dependencies) {
        int depLineNum = methodInfo.getLineNumber(dependency);
        if (!depLineNums.contains(depLineNum)) {
          depLineNums.add(depLineNum);
        }
      }
    }
    List<Integer> sortedLineNums = new ArrayList<>(depBySourceLines.keySet());
    Collections.sort(sortedLineNums);
    System.out.println("Control Dependencies:");
    if (sortedLineNums.size() == 0) {
      System.out.println("(None)");
    }
    for (int ln : sortedLineNums) {
      System.out.println(ln + " --> " + depBySourceLines.get(ln));
    }
    System.out.println();
    for (CatchBodyInfo handler : exceptionHandlers) {
      System.out.println("Exception handler:");
      int entryLineNum = methodInfo.getLineNumber(handler.getEntryInstructionPosition());
      System.out.println("Handler entry: " + entryLineNum);
      Set<Integer> handlerLines = new HashSet<>();
      handler.getInstructionPositionRange().stream()
        .forEach(instrPos -> { handlerLines.add(methodInfo.getLineNumber(instrPos));});
      List<Integer> sortedHandlerLines = new ArrayList<>(handlerLines);
      Collections.sort(sortedHandlerLines);
      System.out.println("Handler body: " + sortedHandlerLines);
      System.out.println();
    }
  }
  
  private List<Integer> getOrCreateBlockDependencies(Block dependantBlock) {
    List<Integer> dependencies = blockDependencies.get(dependantBlock);
    if (dependencies == null) {
      dependencies = new ArrayList<>();
      blockDependencies.put(dependantBlock, dependencies);
    }
    return dependencies;
  }

  private void addBlockInfo(Block dependantBlock) {
    if (instrPosBlockMap.get(dependantBlock.position()) == null) {
      instrPosBlockMap.put(Range.closed(dependantBlock.position(), 
          dependantBlock.position() + dependantBlock.length() - 1), dependantBlock);
    }
  }
  
  private List<Integer> getBlockDependencies(Block containingBlock) {
    List<Integer> dependencies = blockDependencies.get(containingBlock);
    if (dependencies == null) {
      dependencies = Collections.emptyList();
    }
    return dependencies;
  }
}
