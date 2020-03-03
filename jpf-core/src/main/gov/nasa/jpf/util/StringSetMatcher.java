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


public class StringSetMatcher {

  public static final char WILDCARD = '*';
  public static final char INVERTED = '!';
  
  boolean hasAnyPattern; 

  Pattern[] pattern;
  Matcher[] matcher;
  boolean[] inverted;

  
  public static boolean isMatch (String s, StringSetMatcher includes, StringSetMatcher excludes){
    if (excludes != null) {
      if (excludes.matchesAny(s)){
        return false;
      }
    }

    if (includes != null) {
      if (!includes.matchesAny(s)){
        return false;
      }
    }

    return true;
  }

  public static StringSetMatcher getNonEmpty(String[] set){
    if (set != null && set.length > 0){
      return new StringSetMatcher(set);
    } else {
      return null;
    }
  }

  public StringSetMatcher (String... set){
    int n = set.length;
    pattern = new Pattern[n];
    matcher = new Matcher[n];
    inverted = new boolean[n];

    for (int i=0; i<n; i++){
      String s = set[i];

      if (s.equals("*")) {
        hasAnyPattern = true;


      } else {
        Pattern p =  createPattern(s);
        pattern[i] = p;
        matcher[i] = p.matcher(""); 
        inverted[i] = isInverted(s);
      }
    }
  }

  @Override
  public String toString() {
    int n=0;
    StringBuilder sb = new StringBuilder(64);
    sb.append("StringSetMatcher {patterns=");

    if (hasAnyPattern) {
      sb.append(".*");
      n++;
    }

    for (int i=0; i<pattern.length; i++) {
      if (pattern[i] != null) {
        if (n++>0) {
          sb.append(',');
        }
        if (inverted[i]){
          sb.append(INVERTED);
        }
        sb.append(pattern[i]);
      }
    }
    sb.append('}');
    return sb.toString();
  }

  public void addPattern (String s){

    if (s.equals("*")) { 

      hasAnyPattern = true;

    } else {
      int n = pattern.length;

      Pattern[] pNew = new Pattern[n+1];
      System.arraycopy(pattern, 0, pNew, 0, n);
      pNew[n] = createPattern(s);

      Matcher[] mNew = new Matcher[pNew.length];
      System.arraycopy(matcher, 0, mNew, 0, n);
      mNew[n] = pNew[n].matcher("");

      boolean[] iNew = new boolean[pNew.length];
      System.arraycopy( inverted, 0, iNew, 0, n);
      iNew[n] = isInverted(s);
      
      pattern = pNew;
      matcher = mNew;
      inverted = iNew;
    }
  }

  public static boolean isInverted (String s){
    return (!s.isEmpty() && s.charAt(0) == INVERTED);
  }
  
  protected Pattern createPattern (String s){
    Pattern p;
    int j = 0;
    int len = s.length();


    if ((len > 0) && s.charAt(0) == INVERTED){
      j++; 
    }
    
    StringBuilder sb = new StringBuilder();
        
    for (; j<len; j++){
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

  
  public boolean matchesAny (String s){
    if (s != null) {
      if (hasAnyPattern) {
        return true; 
      }

      for (int i=0; i<matcher.length; i++){
        Matcher m = matcher[i];
        m.reset(s);

        if (m.matches() != inverted[i]){
          return true;
        }
      }
    }

    return false;
  }

  
  public boolean matchesAll (String s){
    if (s != null) {
      if (hasAnyPattern && pattern.length == 1) { 
        return true; 
      }

      for (int i=0; i<pattern.length; i++){
        Pattern p = pattern[i];
        if (p != null){
          Matcher m = matcher[i];
          m.reset(s);

          if (m.matches() == inverted[i]){
            return false;
          }
        } else {
          if (inverted[i]){
            return false;
          }
        }
      }

      return true;

    } else {
      return false;
    }
  }

  
  public boolean allMatch (String[] set){
    if (hasAnyPattern) {
      return true;
    }

    for (int i=0; i<set.length; i++){
      if (!matchesAny(set[i])){
        return false;
      }
    }
    return true;
  }


  public static void main (String[] args){
    String[] p = args[0].split(":");
    String[] s = args[1].split(":");

    StringSetMatcher sm = new StringSetMatcher(p);
    if (sm.matchesAny(s[0])){
      System.out.println("Bingo, \"" + s[0] + "\" matches " + sm);
    } else {
      System.out.println("nope, \"" + s[0] + "\" doesn't match " + sm);
    }
  }
}
