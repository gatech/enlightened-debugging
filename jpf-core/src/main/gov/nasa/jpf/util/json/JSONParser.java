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


public class JSONParser {

  JSONLexer lexer;

  Token lastReadToken;
  
  Token prevReadToken;

  int backtrack;

  public JSONParser(JSONLexer lexer) {
    this.lexer = lexer;
  }

  
  public JSONObject parse() {
    return parseObject();
  }

  
  private Token next() {
    if (lastReadToken != null && lastReadToken.getType() == Token.Type.DocumentEnd) {
      return lastReadToken;
    }

    if (backtrack == 1) {
      backtrack--;
      return lastReadToken;
    }

    if (backtrack == 2) {
      backtrack--;
      return prevReadToken;
    }

    prevReadToken = lastReadToken;
    lastReadToken = lexer.getNextToken();

    return lastReadToken;
  }

  
  private void back() {
    if (backtrack == 2) {
      throw new JPFException("Attempt to bactrack three times. Posibly an error. Please report");
    }

    if (lastReadToken == null) {
      throw new JPFException("Attempt to backtrack before starting to read token stream. Please report");
    }

    if (backtrack == 1 && prevReadToken == null) {
      throw new JPFException("Attempt to backtrack twice when less then two tokens read. Please report");
    }

    backtrack++;
  }

  
  private Token consume(Token.Type type) {
    Token t = next();

    if (t.getType() != type) {
      error("Unexpected token '" + t.getValue() + "' expected " + type);
    }

    return t;
  }

  
  private JSONObject parseObject() {
    JSONObject pn = new JSONObject();
    consume(Token.Type.ObjectStart);  
    Token t = next();


    if (t.getType() != Token.Type.ObjectEnd) {
      back();
      while (true) {
        Token key = consume(Token.Type.String);
        consume(Token.Type.KeyValueSeparator);
        

        Token posibleId = next();
        t = next();

        if (posibleId.getType() == Token.Type.Identificator &&
            t.getType() == Token.Type.CGCallParamsStart) {
            CGCall cg = parseCGCall(posibleId.getValue());
            pn.addCGCall(key.getValue(), cg);
        } else {
          back();
          back();
          Value v = parseValue();
          pn.addValue(key.getValue(), v);
        }

        t = next();

        if (t.getType() != Token.Type.Comma) {
          back();
          break;
        }
      }
      consume(Token.Type.ObjectEnd);
    }
    return pn;
  }

  
  private ArrayValue parseArray() {
    consume(Token.Type.ArrayStart);
    ArrayValue arrayValue = new ArrayValue();
    Token t = next();
    if (t.getType() != Token.Type.ArrayEnd) {
      back();
      while (true) {
        Value val = parseValue();
        arrayValue.addValue(val);

        t = next();

        if (t.getType() != Token.Type.Comma) {
          back();
          break;
        }
      }
    } else {
      back();
    }
    consume(Token.Type.ArrayEnd);
    
    return arrayValue;
  }

  
  private Value parseIdentificator() {
    Token id = consume(Token.Type.Identificator);

    String val = id.getValue();
    if (val.equals("true")) {
      return new BooleanValue(true, "true");

    } else if (val.equals("false")) {
      return new BooleanValue(false, "false");
      
    } else if (val.equals("null")) {
      return new NullValue();
    }

    error("Unknown identifier");
    return null;
  }

  private void error(String string) {
    throw new JPFException(string + "(" + lexer.getLineNumber() + ":" + lexer.getCurrentPos() + ")");
  }

  private Value parseValue() {
    Token t = next();
    switch (t.getType()) {
      case Number:
        return new DoubleValue(t.getValue());
        
      case String:
        return new StringValue(t.getValue());
        
      case ArrayStart:
        back();
        return parseArray();
        
      case ObjectStart:
        back();
        return new JSONObjectValue(parseObject());
        
      case Identificator:
        back();
        return parseIdentificator();
        
      default:
        error("Unexpected token '" + t.getValue() + "' during parsing JSON value");
        return null;
    }
    
  }

  
  private CGCall parseCGCall(String cgName) {
    
    CGCall parsedCG = new CGCall(cgName);
    Token t = next();

    if (t.getType() != Token.Type.CGCallParamsEnd) {
      back();
      while (true) {
        Value v = parseValue();
        parsedCG.addParam(v);

        t = next();
        if (t.getType() == Token.Type.CGCallParamsEnd) {
          back();
          break;
        }
        back();
        consume(Token.Type.Comma);
      }
    } else {
      back();
    }

    consume(Token.Type.CGCallParamsEnd);

    return parsedCG;
  }
}
