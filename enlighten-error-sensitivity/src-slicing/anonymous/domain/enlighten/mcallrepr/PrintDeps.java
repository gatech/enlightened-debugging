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


package anonymous.domain.enlighten.mcallrepr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import anonymous.domain.enlighten.deptrack.CompositeDynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.mcallrepr.ArrayElementRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayLengthRefName;
import anonymous.domain.enlighten.mcallrepr.ArrayRepr;
import anonymous.domain.enlighten.mcallrepr.FieldReferenceName;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.MethodCallSpecialRefName;
import anonymous.domain.enlighten.mcallrepr.PrimitiveRepr;
import anonymous.domain.enlighten.mcallrepr.ReferenceRepr;
import anonymous.domain.enlighten.mcallrepr.ReflectedObjectRepr;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.mcallrepr.VoidRepr;
import gov.nasa.jpf.vm.Instruction;

public class PrintDeps {

  private String sep = "\n";
  
  private Set<ValueGraphNode> visited = new HashSet<ValueGraphNode>();
  
  public static String toString(MethodCallRepr mcall) {
    return new PrintDeps().visit(mcall, "");
  }

  public String visit(MethodCallRepr mcall, String indent) {
    StringBuffer arg = new StringBuffer();
    
    arg.append(mcall.isEntry() ? "** ENTRY **" : "** EXIT **");
    arg.append(sep);
    arg.append(indent + "TEST: " + mcall.getTestName() + sep);
    arg.append(indent + "METHOD: " + mcall.getMethodName() 
        + " #" + mcall.getValueHash() + "#" + sep);
    

    arg.append(indent + "this => ");
    arg.append(visit(
        mcall.getThizz(), indent, MemberRefDepAnnotator.getDependency(
            mcall, MethodCallSpecialRefName.thisRef())));
    arg.append(sep);
    

    Map<String, ValueGraphNode> paramValues = mcall.getParams();
    Map<String, DynamicDependency> paramDeps = new HashMap<>();
    for (String paramName : paramValues.keySet()) {
      paramDeps.put(paramName, MemberRefDepAnnotator.getDependency(
          mcall, MethodCallSpecialRefName.fromParamName(paramName)));
    }
    arg.append(visitMap(paramValues,  indent, paramDeps));
    

    if (!mcall.isEntry()) {
      if (mcall.getException() == null) {
        arg.append(indent + "return => ");
        arg.append(visit(
            mcall.getReturnVal(), indent, MemberRefDepAnnotator.getDependency(
                mcall, MethodCallSpecialRefName.returnValue())));
      } else {
        arg.append(indent + "exception => ");
        arg.append(visit(
            mcall.getException(), indent, MemberRefDepAnnotator.getDependency(
                mcall,MethodCallSpecialRefName.exceptionThrown())));
      }
      arg.append(sep);
    }

    return arg.toString();
  }

  public String visit(ValueGraphNode vrepr,  String indent, DynamicDependency dep) {
    if (vrepr == null) {
      return "null";
    }
    
    if (visited.contains(vrepr)) {
      String str = "VISITED";
      if (vrepr instanceof ReflectedObjectRepr) {
        str += " [" + ((ReflectedObjectRepr) vrepr).getId() + "]";
      } else if (vrepr instanceof ArrayRepr) {
        str += " [" + ((ArrayRepr) vrepr).getId() + "]";
      }
      str += " #" + vrepr.getValueHash() + "#";
      str += " ***" + getDependencyStr(dep) + "***";
      return str;
    }
    
    StringBuffer arg = new StringBuffer();
    if (vrepr instanceof PrimitiveRepr) {
      arg.append(visit((PrimitiveRepr) vrepr, indent, dep));
    } else if (vrepr instanceof ReflectedObjectRepr) {
      visited.add(vrepr);
      arg.append(visit((ReflectedObjectRepr) vrepr, indent, dep));
    } else if (vrepr instanceof ArrayRepr) {
      visited.add(vrepr);
      arg.append(visit((ArrayRepr) vrepr, indent, dep));
    } else if (vrepr instanceof ReferenceRepr) {
      arg.append(visit((ReferenceRepr) vrepr, indent, dep));
    } else if (vrepr instanceof VoidRepr) {
      arg.append("VOID");
    } else {
      throw new UnsupportedOperationException("missing case");
    }
    
    return arg.toString();
  }

  public String visit(ReflectedObjectRepr orepr,  String indent, DynamicDependency dep) {
    StringBuffer arg = new StringBuffer();
    arg.append(String.format("%s[%d] #%d# ***%s*** {\n", 
        orepr.getType(), orepr.getId(), orepr.getValueHash(), getDependencyStr(dep)));
    Map<String, ValueGraphNode> fieldValues = orepr.getFields();
    Map<String, DynamicDependency> fieldDeps = new HashMap<>();
    for (String fieldName : fieldValues.keySet()) {
      fieldDeps.put(fieldName, MemberRefDepAnnotator.getDependency(orepr, new FieldReferenceName(fieldName)));
    }
    arg.append(visitMap(orepr.getFields(), indent + " ", fieldDeps));
    arg.append(indent + "}" + sep);
    return arg.toString();
  }
  
  public String visit(ArrayRepr arepr, String indent, DynamicDependency dep) {
    StringBuilder buf = new StringBuilder();
    buf.append(String.format("%s[%d] #%d# ***%s*** {\n", 
        arepr.getType(), arepr.getId(), arepr.getValueHash(), getDependencyStr(dep)));
    String elementIndent = indent + " ";
    buf.append(elementIndent + "arrayLength => "
        + visit(arepr.getLength(), elementIndent + " ", 
            MemberRefDepAnnotator.getDependency(arepr, ArrayLengthRefName.get())) + "\n");
    List<ValueGraphNode> elements = arepr.getElements();
    for (int index = 0; index < elements.size(); ++index) {
      buf.append(elementIndent + "[" + index + "] => " 
          + visit(elements.get(index), elementIndent + " ", 
              MemberRefDepAnnotator.getDependency(arepr, new ArrayElementRefName(index))) + "\n");
    }
    buf.append(indent + "}\n");
    return buf.toString();
  }
 
  public String visit(PrimitiveRepr wrepr, String indent, DynamicDependency dep) {
    return wrepr.getType() + ":" + wrepr.getWrappedValue().toString() 
        + " #" + wrepr.getValueHash()
        + " ***" + getDependencyStr(dep) + "***";
  }

  public String visit(ReferenceRepr refRepr, String arg, DynamicDependency dep) {
    return refRepr.toString() + " ***" + getDependencyStr(dep) + "***";
  }

  private String visitMap(Map<String, ValueGraphNode> map, String indent, Map<String, DynamicDependency> depMap) {
    StringBuffer arg = new StringBuffer();
    List<String> sortedKeys = new ArrayList<>(map.keySet());
    Collections.sort(sortedKeys);
    for(String key : sortedKeys) {
      ValueGraphNode val = map.get(key);
      DynamicDependency dep = depMap.get(key);
      arg.append(indent + key + " => ");
      if (val == null) {
        arg.append("NULL");
      } else {
        arg.append(visit(val, indent, dep));
      }
      arg.append(sep);
    }
    return arg.toString();
  }
  
  private String getDependencyStr(DynamicDependency dep) {
    if (dep == null) {
      return "No dependency";
    }
    HashMap<String, HashSet<Integer>> depLines = getDepLines(dep);
    StringBuilder sb = new StringBuilder();
    for (String sourceFile : depLines.keySet()) {
      sb.append(sourceFile);
      sb.append(':');
      List<Integer> sortedLineNums = new ArrayList<>(depLines.get(sourceFile));
      Collections.sort(sortedLineNums);
      for (int lineNum : sortedLineNums) {
        sb.append(lineNum);
        sb.append(",");
      }
    }
    return sb.toString();
  }
  
  private HashMap<String, HashSet<Integer>> getDepLines(DynamicDependency dep) {
    HashMap<String, HashSet<Integer>> result = new HashMap<>();
    getDepLinesRecursive(dep, result);
    return result;
  }
  
  private void getDepLinesRecursive(DynamicDependency dep, HashMap<String, HashSet<Integer>> deps) {
    LinkedList<DynamicDependency> workingList = new LinkedList<>();
    Set<DynamicDependency> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    workingList.add(dep);
    while (!workingList.isEmpty()) {
      dep = workingList.removeFirst();
      if (dep instanceof InstructionDependencySource) {
        Instruction depInstr = ((InstructionDependencySource) dep).getSourceInstruction();
        String sourceFile = depInstr.getMethodInfo().getSourceFileName();
        int lineNum = depInstr.getLineNumber();
        HashSet<Integer> sfLines = deps.get(sourceFile);
        if (sfLines == null) {
          sfLines = new HashSet<>();
          deps.put(sourceFile, sfLines);
        }
        sfLines.add(lineNum);
      } else if (dep instanceof CompositeDynamicDependency) {
        visited.add(dep);
        for (DynamicDependency depSource : ((CompositeDynamicDependency) dep).getDataDependencies()) {
          if (!visited.contains(depSource)){
            workingList.add(depSource);
          }
        }
        DynamicDependency controlDep = ((CompositeDynamicDependency) dep).getControlDependency();
        if (!visited.contains(controlDep)) {
          workingList.add(controlDep);
        }
      }
    }
  }
}
