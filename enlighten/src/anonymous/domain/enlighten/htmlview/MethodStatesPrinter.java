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


package anonymous.domain.enlighten.htmlview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.objectweb.asm.Type;

import anonymous.domain.enlighten.mcallrepr.ArrayRepr;
import anonymous.domain.enlighten.mcallrepr.MemberRefAccessedAnnotator;
import anonymous.domain.enlighten.mcallrepr.MethodCallRepr;
import anonymous.domain.enlighten.mcallrepr.PrimitiveRepr;
import anonymous.domain.enlighten.mcallrepr.ReferenceRepr;
import anonymous.domain.enlighten.mcallrepr.ReflectedObjectRepr;
import anonymous.domain.enlighten.mcallrepr.TransitivelyAccessedAnnotator;
import anonymous.domain.enlighten.mcallrepr.ValueGraphNode;
import anonymous.domain.enlighten.mcallrepr.VoidRepr;

public class MethodStatesPrinter {
  
  private Set<ValueGraphNode> visited = new HashSet<ValueGraphNode>();
  private String highlightColorStr = "yellow";
  
  public static String getHtmlViewContent(MethodCallRepr mcall) {
    MethodStatesPrinter printer = new MethodStatesPrinter();
    if (mcall.isEntry()) {
      printer.setHighlightColor("#99ccff"); 
    } else {
      printer.setHighlightColor("yellow");
    }
    return printer.visit(mcall);
  }
  
  public void setHighlightColor(String colorDescription) {
    highlightColorStr = colorDescription;
  }

  public String visit(MethodCallRepr mcall) {
    StringBuffer arg = new StringBuffer();
    arg.append("<ul class=\"collapsibleList\">\n");
    arg.append("<li>\n");
    arg.append(mcall.isEntry() ? "** ENTRY **" : "** EXIT **");
    arg.append("<br/>\n");
    arg.append("TEST: " + escapeHtml(mcall.getTestName()) + "<br/>\n");
    arg.append("METHOD: " + escapeHtml(getReadableMethodName(
        mcall.getClassName(), mcall.getMethodName())) 
        + " #" + mcall.getValueHash() + "#" + "\n");
    
    arg.append("<ul>\n");

    if (!mcall.isStatic()) {
      arg.append("<li>\n");
      if (mcall.isEntry()) {
        arg.append(highlightedText("this"));
      } else {
        arg.append("this");
      }
      arg.append(" => ");
      arg.append(visit(mcall.getThizz()));
      arg.append("</li>\n");
    }
    

    Map<String, ValueGraphNode> params = mcall.getParams();
    Set<String> highlightParams = new HashSet<>();
    if (mcall.isEntry()) {
      for (String paramName : params.keySet()) {
        highlightParams.add(paramName);
      }
    }
    arg.append(visitMap(params, highlightParams));
    

    if (!mcall.isEntry()) {
      arg.append("<li>\n");
      if (mcall.getException() == null) {
        arg.append(highlightedText("return"));
        arg.append(" => ");
        arg.append(visit(mcall.getReturnVal()));
      } else {
        arg.append(highlightedText("exception"));
        arg.append(" => ");
        arg.append(visit(mcall.getException()));
      }
      arg.append("</li>\n");
    }

    arg.append("</ul>\n");
    arg.append("</li>\n");
    arg.append("</ul>");
    return arg.toString();
  }

  public String visit(ValueGraphNode vrepr) {
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
      arg.append(visit((PrimitiveRepr) vrepr));
    } else if (vrepr instanceof ReflectedObjectRepr) {
      visited.add(vrepr);
      arg.append(visit((ReflectedObjectRepr) vrepr));
    } else if (vrepr instanceof ArrayRepr) {
      visited.add(vrepr);
      arg.append(visit((ArrayRepr) vrepr));
    } else if (vrepr instanceof ReferenceRepr) {
      arg.append(visit((ReferenceRepr) vrepr));
    } else if (vrepr instanceof VoidRepr) {
      arg.append("VOID");
    } else {
      throw new UnsupportedOperationException("missing case");
    }
    
    return arg.toString();
  }

  public String visit(ReflectedObjectRepr orepr) {
    StringBuffer arg = new StringBuffer();
    String objName = String.format(
        "%s[%d] #%d#\n", escapeHtml(orepr.getType()), orepr.getId(), orepr.getValueHash());
    if (TransitivelyAccessedAnnotator.isTransitivelyAccessed(orepr)) {
      objName = highlightedText(objName);
    }
    arg.append(objName);
    arg.append("<ul>\n");
    Map<String, ValueGraphNode> fieldsMap = orepr.getFields();
    Set<String> highlightedKeys = new HashSet<>();
    for (String fieldName : fieldsMap.keySet()) {
      if (MemberRefAccessedAnnotator.isMemberAccessed(orepr, fieldName)) {
        highlightedKeys.add(fieldName);
      }
    }
    arg.append(visitMap(fieldsMap, highlightedKeys));
    arg.append("</ul>\n");
    return arg.toString();
  }
  
  public String visit(ArrayRepr arepr) {
    StringBuilder buf = new StringBuilder();
    String objName = String.format("%s[%d] #%d#\n", 
        arepr.getType(), arepr.getId(), arepr.getValueHash());
    if (TransitivelyAccessedAnnotator.isTransitivelyAccessed(arepr)) {
      objName = highlightedText(objName);
    }
    buf.append(objName);
    List<ValueGraphNode> elements = arepr.getElements();
    buf.append("<ul>\n");
    buf.append("<li>\narrayLength => " 
        + (Integer) arepr.getLength().getWrappedValue() + "</li>\n");
    for (int index = 0; index < elements.size(); ++index) {
      buf.append("<li>\n");
      String indexHtml = "[" + index + "]";
      if (MemberRefAccessedAnnotator.isMemberAccessed(arepr, index)) {
        indexHtml = highlightedText(indexHtml);
      }
      buf.append(indexHtml + " => " 
          + visit(elements.get(index)) + "</li>\n");
    }
    buf.append("</ul>\n");
    return buf.toString();
  }
 
  public String visit(PrimitiveRepr wrepr) {
    return wrepr.getType() + ": " + wrepr + " #" + wrepr.getValueHash();
  }

  public String visit(ReferenceRepr refRepr) {
    return refRepr.toString();
  }

  private String visitMap(Map<String, ValueGraphNode> map, Set<String> highlightedKeys) {
    StringBuffer arg = new StringBuffer();
    List<String> sortedKeys = new ArrayList<>(map.keySet());
    Collections.sort(sortedKeys);
    for(String key : sortedKeys) {
      ValueGraphNode val = map.get(key);
      arg.append("<li>\n");
      String keyHtml = escapeHtml(key);
      if (highlightedKeys.contains(key)) {
        keyHtml = highlightedText(keyHtml);
      }
      arg.append(keyHtml + " => ");
      if (val == null) {
        arg.append("NULL");
      } else {
        arg.append(visit(val));
      }
      arg.append("\n");
      arg.append("</li>\n");
    }
    return arg.toString();
  }
  
  private String escapeHtml(String content) {
    return StringEscapeUtils.escapeHtml4(content);
  }
  
  private String highlightedText(String text) {
    return "<span style=\"background:" + highlightColorStr + "\">" + text + "</span>";
  }
  
  private String getReadableMethodName(String className, String mNameSig) {
    int nameSigSepIndex = mNameSig.indexOf('(');
    String mSimpleName = mNameSig.substring(0, nameSigSepIndex);
    String mSig = mNameSig.substring(nameSigSepIndex);
    Type retType = Type.getReturnType(mSig);
    Type[] argTypes = Type.getArgumentTypes(mSig);
    String readableName = retType.getClassName() + " " + className + "." 
        + mSimpleName + "(";
    for (int i = 0; i < argTypes.length; ++i) {
      readableName += argTypes[i].getClassName();
      if (i != argTypes.length - 1) {
        readableName += ", ";
      }
    }
    readableName += ")";
    return readableName;
  }
}
