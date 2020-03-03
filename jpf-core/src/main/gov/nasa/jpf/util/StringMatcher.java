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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StringMatcher {

  boolean isAnyPattern; 

  Pattern pattern;
  Matcher matcher;

  public static boolean hasWildcard (String patternSpec){
    return (patternSpec.indexOf('*') >= 0);
  }

  public StringMatcher (String patternSpec){
    if (patternSpec.equals("*")) {
      isAnyPattern = true;


    } else {
      Pattern p = createPattern(patternSpec);
      pattern = p;
      matcher = p.matcher(""); 
    }
  }

  protected Pattern createPattern (String s){
    Pattern p;

    StringBuilder sb = new StringBuilder();

    int len = s.length();
    for (int j=0; j<len; j++){
      char c = s.charAt(j);
      switch (c){
      case '.' : sb.append("\\."); break;
      case '$' : sb.append("\\$"); break;
      case '[' : sb.append("\\["); break;
      case ']' : sb.append("\\]"); break;
      case '*' : sb.append(".*"); break;
      case '(' : sb.append("\\("); break;
      case ')' : sb.append("\\)"); break; 

      default:   sb.append(c);
      }
    }

    p = Pattern.compile(sb.toString());
    return p;
  }

  public boolean matches (String s){
    if (isAnyPattern){
      return true;
    } else {
      matcher.reset(s);
      return matcher.matches();
    }
  }

  @Override
  public String toString() {
    if (isAnyPattern){
      return ".*";
    } else {
      return pattern.toString();
    }
  }
}
