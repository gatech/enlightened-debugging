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

import gov.nasa.jpf.JPFException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class JSONLexer {


  private Reader reader;

  int symbolNumber;

  int lineNumber;

  int symbolNumberInLine;


  boolean backtracked;

  int currentChar;

  private final int STREAM_END = -1;

  public JSONLexer(Reader reader) {
    this.reader = reader;
    backtracked = false;
  }

  public JSONLexer(String JSONStr) {
    this(new StringReader(JSONStr));
  }

  
  public Token getNextToken() {

    int c;

    do {
      c = next();
    } while(isSkipChar(c));

    if (c == STREAM_END) {
      return new Token(Token.Type.DocumentEnd, null);
    }

    if (c == '{') {
      return new Token(Token.Type.ObjectStart, "{");
    }

    if (c == '}') {
      return new Token(Token.Type.ObjectEnd, "}");
    }

    if (c == '[') {
      return new Token(Token.Type.ArrayStart, "[");
    }

    if (c == ']') {
      return new Token(Token.Type.ArrayEnd, "]");
    }

    if (c == ':') {
      return new Token(Token.Type.KeyValueSeparator, ":");
    }

    if (c == ',') {
      return new Token(Token.Type.Comma, ",");
    }

    if (c == '(') {
      return new Token(Token.Type.CGCallParamsStart, "(");
    }

    if (c == ')') {
      return new Token(Token.Type.CGCallParamsEnd, ")");
    }

    if (c == '\"' || c == '\'') {
      return parseString(c);
    }

    if (Character.isDigit(c) || c == '-') {
      back();
      return parseNumber();
    }

    if (isIdentifierStartSymbol(c)) {
      back();
      return parseIdentifier();
    }


    error("Unexpected sybmol");
    return null;
  }

  
  public boolean hasMore() {
    return currentChar != STREAM_END;
  }

  
  private int next() {
    try {
      if (backtracked) {
        backtracked = false;
        return currentChar;
      }

      currentChar = reader.read();
      
      symbolNumber++;
      symbolNumberInLine++;
      if (currentChar == '\n') {
        lineNumber++;
        symbolNumberInLine = 0;
      }

      return currentChar;
    } catch (IOException ex) {
      throw new JPFException("IOException during tokenizing JSON", ex);
    }
  }

  
  private void back() {
    if (backtracked) {
      throw new JPFException("Tried to return twice. Posibly an error. Please report");
    }
    backtracked = true;
  }


  private Token parseString(int delimiter) {
    StringBuilder result = new StringBuilder();
    int c;

    while((c = next()) != delimiter) {
      if (c == '\\') {
          result.append((char) readEscapedSymbol());
      } else {
         result.append((char) c);
      }
    }

    return new Token(Token.Type.String, result.toString());
  }

  private int readEscapedSymbol() {
    int escaped = next();

    int res = -1;

    switch(escaped) {
      case '\"':
      case '\\':
      case '/':
        res = escaped;
        break;

      case 'b':
        res = '\b';
        break;

      case 'f':
        res = '\f';
        break;

      case 'n':
        res = '\n';
        break;

      case 'r':
        res = '\r';
        break;

      case 't':
        res = '\t';
        break;


      case 'u': {
        String r = "";
        int i = 0;
        int c;

        while (hexadecimalChar(c = next()) && i < 4) {
          r += (char) c;
          i++;
        }


        if (i < 4) {
          error("Escaped Unicode symbol should consist of 4 hexadecimal digits");
        }
        
        back();

        res = Integer.parseInt(r, 16);
      }
      break;

      default:
        error("Illegal excape");
        break;
    }

    return res;
  }

  private Token parseNumber() {    
    StringBuilder sb = new StringBuilder();
    int c = next();


    if (c == '-') {
      sb.append('-');
    } else {

      back();
    }

    c = next();


    if (c == '0') {
      sb.append('0');
    } else {
      back();
      sb.append(readDigits());
    }

    c = next();


    if (c == '.') {
      sb.append('.');
      sb.append(readDigits());
    } else {
      back();
    }

    c = next();

    if (c == 'e' || c == 'E') {
      sb.append((char) c);
      c = next();
      if (c == '+' || c == '-') {
        sb.append((char) c);
      } else {
        back();
      }

      sb.append(readDigits());
    } else {
      back();
    }

    return new Token(Token.Type.Number, sb.toString());
  }

  
  private String readDigits() {
    StringBuilder sb = new StringBuilder();
    int c;
    int n = 0;
    while (Character.isDigit(c = next())) {
      sb.append((char) c);
      n++;
    }

    if (n == 0) {
      error("Expected not empty sequence of digits");
    }

    back();
    return sb.toString();
  }

  private Token parseIdentifier() {
    StringBuilder result = new StringBuilder();

    int c = next();

    while (Character.isJavaIdentifierPart(c)) {
      result.append((char) c);

      c = next();
    }

    back();

    return new Token(Token.Type.Identificator, result.toString());
  }

  private boolean isIdentifierStartSymbol(int c) {
    return Character.isJavaIdentifierStart(c);
  }

  private boolean isSkipChar(int currentChar) {
    return Character.isSpaceChar(currentChar);
  }

  private void error(String string) {
    throw new JPFException(string + " '" + (char) currentChar + "' charCode = " + currentChar +
                           "; in line " + lineNumber + " pos " + symbolNumberInLine);
  }

  private boolean hexadecimalChar(int i) {
    return Character.isDigit(i) || (i <= 'F' && i >= 'A') || (i <= 'f' && i >= 'a');
  }

  int getLineNumber() {
    return lineNumber;
  }

  int getCurrentPos() {
    return symbolNumberInLine;
  }
}
