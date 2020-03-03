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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class ProgramStateSnapshotter {
  
  private IdentityHashMap<Object, ValueGraphNode> visited = new IdentityHashMap<>();
  
  public ValueGraphNode fromObject(Object value) {
    if (value == null) {
      return NullRepr.get();
    }
    if (visited.containsKey(value)) {
      return visited.get(value);
    }
    Class<?> valueClass = value.getClass();
    if (valueClass.isArray()) {
      ArrayRepr repr = new ArrayRepr(
          valueClass.getName(), System.identityHashCode(value));
      repr.setActualReference(value);
      visited.put(value, repr);
      List<ValueGraphNode> elementsReprList = new ArrayList<>();
      boolean isPrimitiveArray = valueClass.getComponentType().isPrimitive();
      for (int i = 0; i < Array.getLength(value); ++i) {
        if (isPrimitiveArray) {
          elementsReprList.add(fromBoxedPrimitive(Array.get(value, i)));
        } else {
          elementsReprList.add(fromObject(Array.get(value, i)));
        }
      }
      repr.setElements(new PrimitiveRepr("int", Array.getLength(value)), elementsReprList);
      return repr;
    } else {
      ReflectedObjectRepr repr = new ReflectedObjectRepr(
          valueClass.getName(), System.identityHashCode(value));
      repr.setActualReference(value);
      visited.put(value, repr);
      List<java.lang.reflect.Field> allFields = 
          ObjectFieldIterationUtil.getAllFieldsAndForceAccessible(valueClass);
      for (java.lang.reflect.Field field : allFields) {
        try {
          if (field.getType().isPrimitive()) {
            repr.putField(field.getName(), fromBoxedPrimitive(field.get(value)));
          } else {
            repr.putField(field.getName(), fromObject(field.get(value)));
          }
        } catch (IllegalAccessException ex) {
          throw new RuntimeException("Internal error: "
              + "Error getting field value which should have been set accessible.");
        }
      }
      return repr;
    }
  }
  
  public static ValueGraphNode fromBoxedPrimitive(Object boxedPrimitive) {
    if (boxedPrimitive instanceof Boolean) {
      return fromPrimitive(((Boolean) boxedPrimitive).booleanValue());
    } else if (boxedPrimitive instanceof Byte) {
      return fromPrimitive(((Byte) boxedPrimitive).byteValue());
    } else if (boxedPrimitive instanceof Character) {
      return fromPrimitive(((Character) boxedPrimitive).charValue());
    } else if (boxedPrimitive instanceof Short) {
      return fromPrimitive(((Short) boxedPrimitive).shortValue());
    } else if (boxedPrimitive instanceof Integer) {
      return fromPrimitive(((Integer) boxedPrimitive).intValue());
    } else if (boxedPrimitive instanceof Long) {
      return fromPrimitive(((Long) boxedPrimitive).longValue());
    } else if (boxedPrimitive instanceof Float) {
      return fromPrimitive(((Float) boxedPrimitive).floatValue());
    } else if (boxedPrimitive instanceof Double) {
      return fromPrimitive(((Double) boxedPrimitive).doubleValue());
    } else {
      throw new RuntimeException("Unexpected boxed primitive type " 
          + boxedPrimitive.getClass().getName());
    }
  }
  
  public static ValueGraphNode fromPrimitive(boolean value) {
    return new PrimitiveRepr("boolean", value);
  }
  
  public static ValueGraphNode fromPrimitive(byte value) {
    return new PrimitiveRepr("byte", value);
  }
  
  public static ValueGraphNode fromPrimitive(char value) {
    return new PrimitiveRepr("char", value);
  }
  
  public static ValueGraphNode fromPrimitive(short value) {
    return new PrimitiveRepr("short", value);
  }
  
  public static ValueGraphNode fromPrimitive(int value) {
    return new PrimitiveRepr("int", value);
  }
  
  public static ValueGraphNode fromPrimitive(long value) {
    return new PrimitiveRepr("long", value);
  }
  
  public static ValueGraphNode fromPrimitive(float value) {
    return new PrimitiveRepr("float", value);
  }
  
  public static ValueGraphNode fromPrimitive(double value) {
    return new PrimitiveRepr("double", value);
  }
  
}
