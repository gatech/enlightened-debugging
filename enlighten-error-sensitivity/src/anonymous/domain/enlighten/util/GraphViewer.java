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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GraphViewer<NodeType> {
  
  private List<NodeWrapper> startNodes = new ArrayList<>();
  
  public GraphViewer(NodeType startNode) {
    startNodes.add(new NodeWrapper(startNode));
  }
  
  public GraphViewer(Iterable<NodeType> startNodes) {
    Set<NodeWrapper> added = new HashSet<>();
    for (NodeType startNode : startNodes) {
      NodeWrapper wrappedNode = new NodeWrapper(startNode);
      if (added.add(wrappedNode)) {
        this.startNodes.add(wrappedNode);
      }
    }
  }
  
  
  protected abstract List<NodeType> getSuccessors(NodeType node);
  
  
  protected abstract List<String> getSuccessorEdgeDescriptions(NodeType node);
  
  
  protected abstract String getDescription(NodeType node);
  
  public String printAsDotFile() {
    LinkedList<NodeWrapper> workingList = new LinkedList<>();
    Set<NodeWrapper> visited = new HashSet<>();
    List<NodeType> nodesList = new ArrayList<>();
    List<Pair<Edge, String>> edgesList = new ArrayList<>();
    for (NodeWrapper wrappedNode : startNodes) {
      workingList.add(wrappedNode);
      visited.add(wrappedNode);
    }
    while (!workingList.isEmpty()) {
      NodeWrapper wrappedNode = workingList.removeFirst();
      nodesList.add(wrappedNode.node);
      List<NodeType> successors = getSuccessors(wrappedNode.node);
      List<String> outEdgeLabels = getSuccessorEdgeDescriptions(wrappedNode.node);
      for (int i = 0; i < successors.size(); ++i) {
        NodeType successor = successors.get(i);
        String label = outEdgeLabels.get(i);
        edgesList.add(Pair.of(new Edge(wrappedNode.node, successor), label));
        NodeWrapper wrappedSuccessor = new NodeWrapper(successor);
        if (!visited.contains(wrappedSuccessor)) {
          workingList.add(wrappedSuccessor);
          visited.add(wrappedSuccessor);
        }
      }
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append("digraph {\n");
    buffer.append("\tnode[shape=box];\n");
    Map<NodeWrapper, String> nodeNames = new HashMap<>(); 
    for (int nodeCount = 0; nodeCount < nodesList.size(); ++nodeCount) {
      NodeType node = nodesList.get(nodeCount);
      String nodeName = "node" + nodeCount;
      nodeNames.put(new NodeWrapper(node), nodeName);
      buffer.append('\t');
      buffer.append(nodeName);
      buffer.append("[label=\"");
      buffer.append(escape(getDescription(node)));
      buffer.append("\"];\n");
    }
    for (int edgeCount = 0; edgeCount < edgesList.size(); ++edgeCount) {
      Pair<Edge, String> edgeInfo = edgesList.get(edgeCount);
      Edge edge = edgeInfo.getFirst();
      String label = edgeInfo.getSecond();
      String fromNodeName = nodeNames.get(new NodeWrapper(edge.from));
      String toNodeName = nodeNames.get(new NodeWrapper(edge.to));
      buffer.append('\t');
      buffer.append(fromNodeName);
      buffer.append(" -> ");
      buffer.append(toNodeName);
      if (label != null) {
        buffer.append("[label=\"");
        buffer.append(escape(label));
        buffer.append("\"]");
      }
      buffer.append(";\n");
    }
    buffer.append("}\n");
    return buffer.toString();
  }
  
  private static String escape(String original) {
    String escaped = original.replace("\r", "")
        .replace("\n", "\\n")
        .replace("\"", "\\\"");
    return escaped;
  }
  
  private class NodeWrapper {
    
    private NodeType node;
    
    public NodeWrapper(NodeType node) {
      this.node = node;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o.getClass() == getClass()) {
        @SuppressWarnings("unchecked")
        NodeWrapper another = (NodeWrapper) o;
        return node == another.node;
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return System.identityHashCode(node);
    }
  }
  
  private class Edge {
    
    private NodeType from;
    private NodeType to;
    
    public Edge(NodeType from, NodeType to) {
      this.from = from;
      this.to = to;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o.getClass() == getClass()) {
        @SuppressWarnings("unchecked")
        Edge another = (Edge) o;
        return from == another.from && to == another.to;
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      int fromHash = System.identityHashCode(from);
      int toHash = System.identityHashCode(to);


      int toHashTrans = toHash << 16 | toHash >>> 16;
      return fromHash ^ toHashTrans;
    }
  }
}
