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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrettyPrintVisitor implements Visitor<String, String> {
  
  String sep = "\n";
  
  Set<ValueGraphNode> visited = new HashSet<ValueGraphNode>();

  @Override
  public String visit(MethodCallRepr mcall, String indent) {
    StringBuffer arg = new StringBuffer();
    
    arg.append(mcall.isEntry() ? "** ENTRY **" : "** EXIT **");
    arg.append(sep);
    arg.append(indent + "TEST: " + mcall.getTestName() + sep);
    arg.append(indent + "METHOD: " + mcall.getMethodName() 
        + " #" + mcall.getValueHash() + "#" + sep);
    

    arg.append(indent + "this => ");
    arg.append(visit(mcall.getThizz(), indent));
    arg.append(sep);
    

    arg.append(visitMap(mcall.getParams(),  indent));
    

    if (!mcall.isEntry()) {
      if (mcall.getException() == null) {
        arg.append(indent + "return => ");
        arg.append(visit(mcall.getReturnVal(), indent));
      } else {
        arg.append(indent + "exception => ");
        arg.append(visit(mcall.getException(), indent));
      }
      arg.append(sep);
    }

    return arg.toString();
  }

  @Override
  public String visit(ValueGraphNode vrepr,  String indent) {
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
      return str;
    }
    
    StringBuffer arg = new StringBuffer();
    if (vrepr instanceof PrimitiveRepr) {
      arg.append(visit((PrimitiveRepr) vrepr, indent));
    } else if (vrepr instanceof ReflectedObjectRepr) {
      visited.add(vrepr);
      arg.append(visit((ReflectedObjectRepr) vrepr, indent));
    } else if (vrepr instanceof ArrayRepr) {
      visited.add(vrepr);
      arg.append(visit((ArrayRepr) vrepr, indent));
    } else if (vrepr instanceof ReferenceRepr) {
      arg.append(visit((ReferenceRepr) vrepr, indent));
    } else if (vrepr instanceof VoidRepr) {
      arg.append("VOID");
    } else {
      throw new UnsupportedOperationException("missing case");
    }
    
    return arg.toString();
  }

  @Override
  public String visit(ReflectedObjectRepr orepr,  String indent) {
    StringBuffer arg = new StringBuffer();
    arg.append(String.format("%s[%d] #%d# {\n", orepr.getType(), orepr.getId(), orepr.getValueHash()));
    arg.append(visitMap(orepr.getFields(), indent + " "));
    arg.append(indent + "}" + sep);
    return arg.toString();
  }
  
  public String visit(ArrayRepr arepr, String indent) {
    StringBuilder buf = new StringBuilder();
    buf.append(String.format("%s[%d] #%d# {\n", 
        arepr.getType(), arepr.getId(), arepr.getValueHash()));
    String elementIndent = indent + " ";
    buf.append(elementIndent + "arrayLength => " + arepr.getLength().getWrappedValue() + "\n");
    List<ValueGraphNode> elements = arepr.getElements();
    for (int index = 0; index < elements.size(); ++index) {
      buf.append(elementIndent + "[" + index + "] => " 
          + visit(elements.get(index), elementIndent + " ") + "\n");
    }
    buf.append(indent + "}\n");
    return buf.toString();
  }
 
  @Override
  public String visit(PrimitiveRepr wrepr, String indent) {
    return wrepr.getType() + ":" + wrepr.getWrappedValue().toString() + " #" + wrepr.getValueHash();
  }

  @Override
  public String visit(ReferenceRepr refRepr, String arg) {
    return refRepr.toString();
  }

  private String visitMap(Map<String, ValueGraphNode> map, String indent) {
    StringBuffer arg = new StringBuffer();
    List<String> sortedKeys = new ArrayList<>(map.keySet());
    Collections.sort(sortedKeys);
    for(String key : sortedKeys) {
      ValueGraphNode val = map.get(key);
      arg.append(indent + key + " => ");
      if (val == null) {
        arg.append("NULL");
      } else {
        arg.append(visit(val, indent));
      }
      arg.append(sep);
    }
    return arg.toString();
  }

}
