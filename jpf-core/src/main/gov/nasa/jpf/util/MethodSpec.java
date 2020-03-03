/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */



package gov.nasa.jpf.util;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Types;

import java.util.BitSet;


public class MethodSpec extends FeatureSpec {

  static class MethodParseData extends ParseData {
    String sigSpec;
  }

  static final char MARK = '^';  

  String  sigSpec;  
  BitSet  markedArgs;

  
  public static MethodSpec createMethodSpec (String s){
    MethodParseData d = new MethodParseData();

    s = s.trim();
    String src = s;

    s = parseInversion(s,d);

    int i = s.indexOf(('('));
    if (i >= 0){ 
      int j = s.lastIndexOf(')');
      if (j > i){
        d.sigSpec = s.substring(i, j+1);
        s = s.substring(0, i);

      } else {
        return null; 
      }
    }

    parseTypeAndName(s,d);

    try {
      return new MethodSpec(src, d.typeSpec, d.nameSpec, d.sigSpec, d.matchInverted);
    } catch (IllegalArgumentException iax){
      return null;
    }
  }


  public MethodSpec (String rawSpec, String cls, String name, String argSig, boolean inverted){
    super(rawSpec,cls,name,inverted);

    if (argSig != null){
      parseSignature(argSig);
    }
  }

  
  void parseSignature (String spec){
    BitSet m = null;
    StringBuilder sb = new StringBuilder();
    String al = spec.substring(1, spec.length()-1);
    String[] args = al.split(",");

    sb.append('(');
    int i=0;
    for (String a : args){
      a = a.trim();
      if (a.length() > 0){
        if (a.charAt(0) == MARK){
          if (m == null){
            m = new BitSet(args.length);
          }
          m.set(i);
          a = a.substring(1);
        }
        String tc = Types.getTypeSignature(a, false);
        sb.append(tc);
        i++;

      } else {

      }
    }
    sb.append(')');

    sigSpec = sb.toString();
    markedArgs = m;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MethodSpec {");
    sb.append("matchInverted:");
    sb.append(matchInverted);
    if (clsSpec != null){
      sb.append(",clsSpec:\"");
      sb.append(clsSpec);
      sb.append('"');
    }
    if (nameSpec != null){
      sb.append(",nameSpec:\"");
      sb.append(nameSpec);
      sb.append('"');
    }
    if (sigSpec != null){
      sb.append(",sigSpec:\"");
      sb.append(sigSpec);
      sb.append('"');
    }
    if (markedArgs != null){
      sb.append(",marked:");
      sb.append(markedArgs);
    }
    sb.append('}');
    return sb.toString();
  }

  
  public BitSet getMarkedArgs () {
    return markedArgs;
  }

  public boolean isMarkedArg(int idx){
    return (markedArgs == null || markedArgs.get(idx));
  }




  @Override
  public boolean matches (Object feature){
    if (feature instanceof MethodInfo){
      return matches((MethodInfo)feature);
    } else {
      return false;
    }
  }

  public boolean matches (MethodInfo mi){
    boolean isMatch = false;

    ClassInfo ci = mi.getClassInfo();
    if (isMatchingType(ci)){
      if (nameSpec.matches(mi.getName())){
        if (sigSpec != null){

          isMatch = mi.getSignature().startsWith(sigSpec);
        } else { 
          isMatch = true;
        }
      }
    }

    return (isMatch != matchInverted);
  }

  public boolean matches (String clsName, String mthName){
    boolean isMatch = clsSpec.matches(clsName) && nameSpec.matches(mthName);
    return isMatch != matchInverted;
  }

  public boolean matchesClass (String clsName){
    return clsSpec.matches(clsName) != matchInverted;
  }
}
