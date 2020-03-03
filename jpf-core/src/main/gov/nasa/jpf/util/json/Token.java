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


package gov.nasa.jpf.util.json;


public class Token {


  public enum Type {
    DocumentEnd,
    ObjectStart, ObjectEnd,
    ArrayStart, ArrayEnd,
    CGCallParamsStart, CGCallParamsEnd,
    Comma,
    KeyValueSeparator,
    Number,
    String,
    Identificator};

  private Type type;

  private String value;

  public Token(Type type, String value) {
    this.type = type;
    this.value = value;
  }

  public Type getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Token) {
      Token token = (Token) obj;

      if (token.type != this.type) {
        return false;
      }
      if (token.value == null && this.value == null) {
        return true;
      }
      if (token.value != null && this.value != null) {
        return token.value.equals(this.value);
      }

      return false;
    }

    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Token (");
    sb.append(type);
    sb.append(", '");
    sb.append(value);
    sb.append("')");

    return sb.toString();
  }
}
