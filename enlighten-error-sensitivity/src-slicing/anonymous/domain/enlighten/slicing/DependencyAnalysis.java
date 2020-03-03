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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Range;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.util.GraphViewer;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.InstructionPrinter;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.bytecode.analysis.ControlFlow.Node;



public class DependencyAnalysis {
  
  private ClassPool classPool;
  
  public DependencyAnalysis() {
    classPool = new ClassPool();
  }

  public DependencyAnalysis(SubjectProgram subject) {
    this();
    appendSubjectProgramClasspath(subject);
  }
  
  public void appendSubjectProgramClasspath(SubjectProgram subject) {
    List<Path> classpaths = new ArrayList<>();
    classpaths.addAll(subject.getAppSourceDirs());
    classpaths.addAll(subject.getTestSourceDirs());
    appendExtraClasspath(classpaths.stream()
        .map(path -> { return path.toAbsolutePath().toString(); })
        .collect(Collectors.toList()));
  }
  
  public void appendExtraClasspath(List<String> extraClasspaths) {
    for (String classpath : extraClasspaths) {
      try {
        classPool.appendClassPath(classpath);
      } catch (NotFoundException ex) {
        System.err.println("Warning: classpath entry not found " + classpath);
      }
    }
  }
  
  public CtBehavior getCtMethod(MethodName methodName) throws NotFoundException {
    CtClass clz = classPool.get(methodName.getClassName());
    CtBehavior method = null;
    if ("<init>".equals(methodName.getMethodName())) {
      method = clz.getConstructor(methodName.getMethodSignature());
    } else if ("<clinit>".equals(methodName.getMethodName())){
      method = clz.getClassInitializer();
      if (method == null) {
        throw new NotFoundException(
            "Requesting non-existing static initializer " + methodName.toString());
      }
    } else {
      method = clz.getMethod
          (methodName.getMethodName(), methodName.getMethodSignature());
    }
    return method;
  }
  
  public ControlDependencyInfo getControlDependencyInfo(
      MethodName methodName) throws NotFoundException {
    CtBehavior method = getCtMethod(methodName);
    ControlDependencyInfo result = new ControlDependencyInfo();
    CodeAttribute codeAttribute = method.getMethodInfo().getCodeAttribute();
    if (codeAttribute == null) {
      return result;
    }
    ControlFlow cfg = null;
    try {
      cfg = new ControlFlow(method.getDeclaringClass(), method.getMethodInfo());
    } catch (BadBytecode ex) {
      throw new RuntimeException(
          "Javassist could not handle method " + methodName, ex);
    }
    

    try {
      Node[] postDomTree = cfg.postDominatorTree();
      for (Block bb : cfg.basicBlocks()) {
        Node bbDtNode = postDomTree[bb.index()];
        for (int exitIndex = 0; exitIndex < bb.exits(); ++exitIndex) {
          Block successor = bb.exit(exitIndex);
          Node successorDtNode = postDomTree[successor.index()];
          if (bbDtNode.parent() != successorDtNode) {
            Node commonDominator = getLeastCommonDominator(bbDtNode, successorDtNode);
            Node dependant = successorDtNode;
            while (dependant != commonDominator && dependant != null) {
              result.addDependency(
                  dependant.block(), getLastInstructionIndex(bb, codeAttribute));
              dependant = dependant.parent();
            }
            if (dependant == bbDtNode) {
              result.addDependency(
                  dependant.block(), getLastInstructionIndex(bb, codeAttribute));
            }
          }
        }
      }
    } catch (BadBytecode ex) {
      throw new RuntimeException(
          "Javassist could not handle method " + methodName, ex);
    }
    

    Block[] bBlocks = cfg.basicBlocks();
    ExceptionTable exTable = codeAttribute.getExceptionTable();
    Set<Block> allBlocks = new HashSet<>(Arrays.asList(bBlocks));
    Block entryBlock = findBlockByFirstInstruction(Arrays.asList(bBlocks), 0);
    if (entryBlock == null) {
      throw new RuntimeException("Block at position 0 not found in method " + methodName);
    }
    Set<Block> normalFlowBlocks = getReachableBlocks(entryBlock, allBlocks);
    Set<Block> handlerBlockCandidates = new HashSet<>(allBlocks);
    handlerBlockCandidates.removeAll(normalFlowBlocks);
    List<Integer> sortedHandlerEntries = new ArrayList<>();
    for (int i = 0; i < exTable.size(); ++i) {
      int handlerEntryIndex = exTable.handlerPc(i);
      if (!sortedHandlerEntries.contains(handlerEntryIndex)) {


        sortedHandlerEntries.add(handlerEntryIndex);
      }
    }


    Collections.sort(sortedHandlerEntries);
    for (int handlerEntryPos : sortedHandlerEntries) {
      Block handlerEntryBlock = findBlockByFirstInstruction(
          handlerBlockCandidates, handlerEntryPos);
      if (handlerEntryBlock == null) {
        throw new RuntimeException("Handler entry block at position " 
            + handlerEntryPos + " not found in method " + methodName);
      }
      Set<Block> handlerBlocks = getReachableBlocks(
          handlerEntryBlock, handlerBlockCandidates);
      handlerBlockCandidates.removeAll(handlerBlocks);
      CatchBodyInfo handlerInfo = new CatchBodyInfo(handlerEntryPos);
      for (Block handlerBlock : handlerBlocks) {
        handlerInfo.add(Range.closedOpen(
            handlerBlock.position(), handlerBlock.position() + handlerBlock.length()));
      }
      result.addExceptionHandlerInfo(handlerInfo);
    }
    return result;
  }
  
  public String printControlFlowGraph(MethodName method) throws NotFoundException, BadBytecode {
    CfgViewer viewer = new CfgViewer(getCtMethod(method));
    return viewer.printAsDotFile();
  }
  
  private static Node getLeastCommonDominator(Node n1, Node n2) {
    if (n1 == null || n2 == null) {
      return null;
    }
    int n1Height = 0;
    Node itr = n1;
    while (itr != null) {
      ++n1Height;
      itr = itr.parent();
    }
    int n2Height = 0;
    itr = n2;
    while (itr != null) {
      ++n2Height;
      itr = itr.parent();
    }
    if (n1Height > n2Height) {
      for (int counter = 0; counter < n1Height - n2Height; ++counter) {
        n1 = n1.parent();
      }
    } else {
      for (int counter = 0; counter < n2Height - n1Height; ++counter) {
        n2 = n2.parent();
      }
    }
    while (n1 != null && n1 != n2) {
      n1 = n1.parent();
      n2 = n2.parent();
    }
    return n1;
  }
  
  private static int getLastInstructionIndex(
      Block bb, CodeAttribute code) throws BadBytecode {
    int posUpperBound = bb.position() + bb.length() - 1;
    CodeIterator cItr = code.iterator();
    cItr.move(bb.position());
    int lastInstructionPos = cItr.next();
    while (cItr.lookAhead() <= posUpperBound) {
      lastInstructionPos = cItr.next();
    }
    return lastInstructionPos;
  }
  
  private static Set<Block> getReachableBlocks(
      Block start, Set<Block> allBlocks) {
    if (!allBlocks.contains(start)) {
      return Collections.emptySet();
    }
    Set<Block> visited = new HashSet<>();
    LinkedList<Block> workingList = new LinkedList<>();
    workingList.add(start);
    visited.add(start);
    while (!workingList.isEmpty()) {
      Block current = workingList.removeFirst();
      for (int i = 0; i < current.exits(); ++i) {
        Block successor = current.exit(i);
        if (!visited.contains(successor) && allBlocks.contains(successor)) {
          workingList.add(successor);
          visited.add(successor);
        }
      }
    }
    return visited;
  }
  
  private static Block findBlockByFirstInstruction(
      Iterable<Block> blocks, int firstInstrPos) {
    for (Block block : blocks) {
      if (block.position() == firstInstrPos) {
        return block;
      }
    }
    return null;
  }
  
  private static class CfgViewer extends GraphViewer<Block> {
    
    private MethodInfo methodInfo;

    public CfgViewer(CtBehavior method) throws BadBytecode {
      super(getSortedBlocks(method));
      methodInfo = method.getMethodInfo();
    }

    @Override
    protected List<Block> getSuccessors(Block node) {
      List<Block> successors = new ArrayList<>();
      for (int i = 0; i < node.exits(); ++i) {
        successors.add(node.exit(i));
      }
      return successors;
    }

    @Override
    protected List<String> getSuccessorEdgeDescriptions(Block node) {
      List<String> outEdgeLabels = new ArrayList<>();
      for (int i = 0; i < node.exits(); ++i) {
        outEdgeLabels.add("");
      }
      return outEdgeLabels;
    }

    @Override
    protected String getDescription(Block node) {
      try {
        StringBuilder buffer = new StringBuilder();
        CodeIterator ci = methodInfo.getCodeAttribute().iterator();
        ci.move(node.position());
        int positionUpperBound = node.position() + node.length() - 1;
        int currentInstrPos = ci.next();
        while (currentInstrPos <= positionUpperBound) {
          String instStr = InstructionPrinter.instructionString(
              ci, currentInstrPos, methodInfo.getConstPool());
          buffer.append(currentInstrPos);
          buffer.append(": ");
          buffer.append(instStr);
          int srcLineNum = methodInfo.getLineNumber(currentInstrPos);
          if (srcLineNum != -1) {
            buffer.append("{#");
            buffer.append(srcLineNum);
            buffer.append("}");
          }
          buffer.append('\n');
          if (ci.hasNext()) {
            currentInstrPos = ci.next();
          } else {
            break;
          }
        }
        buffer.deleteCharAt(buffer.length() - 1);
        return buffer.toString();
      } catch (BadBytecode ex) {
        return "Bad bytecode";
      }
    }
    
    private static List<Block> getSortedBlocks(CtBehavior method) throws BadBytecode {
      ControlFlow cfg = new ControlFlow(method.getDeclaringClass(), method.getMethodInfo());
      List<Block> sortedBlocks = new ArrayList<>(Arrays.asList(cfg.basicBlocks()));
      Collections.sort(sortedBlocks, (block1, block2) -> { return block1.position() - block2.position(); });
      return sortedBlocks;
    }
  }
}
