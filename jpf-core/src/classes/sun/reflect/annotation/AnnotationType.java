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



package sun.reflect.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import sun.misc.SharedSecrets;


public class AnnotationType {

  private RetentionPolicy retention = RetentionPolicy.RUNTIME;
  private boolean inherited = false;


  private final Map<String, Class<?>> memberTypes = new HashMap<String, Class<?>>();
  private final Map<String, Object> memberDefaults = new HashMap<String, Object>();
  private final Map<String, Method> members = new HashMap<String, Method>();


  public static synchronized AnnotationType getInstance (Class<?> annotationClass) {
    AnnotationType at = SharedSecrets.getJavaLangAccess().getAnnotationType(annotationClass);
    if (at == null) {
      at = new AnnotationType(annotationClass);
    }
    return at;
  }

  private AnnotationType(final Class<?> annoCls) {
    if (!annoCls.isAnnotation()) {
      throw new IllegalArgumentException("Not an annotation type");
    }

    Method[] methods = annoCls.getDeclaredMethods();

    for (Method m : methods) {
      if (m.getParameterTypes().length == 0) {

        String mname = m.getName();
        members.put(mname, m);


        Class<?> type = m.getReturnType();
        memberTypes.put(mname, invocationHandlerReturnType(type));


        Object val = m.getDefaultValue();
        if (val != null) {
          memberDefaults.put(mname, val);
        }
      } else {

      }
    }

    if ((annoCls != Retention.class) && (annoCls != Inherited.class)) { 
      inherited = annoCls.isAnnotationPresent(Inherited.class);

      Retention r = annoCls.getAnnotation(Retention.class);
      if (r == null) {
        retention = RetentionPolicy.CLASS;
      } else {
        retention = r.value();
      }
    }

    SharedSecrets.getJavaLangAccess().setAnnotationType(annoCls, this);
  }

  public static Class<?> invocationHandlerReturnType (Class<?> type) {

    if (type == boolean.class) {
      return Boolean.class;
    } else if (type == byte.class) {
      return Byte.class;
    } else if (type == char.class) {
      return Character.class;
    } else if (type == short.class) {
      return Short.class;
    } else if (type == int.class) {
      return Integer.class;
    } else if (type == long.class) {
      return Long.class;
    } else if (type == float.class) {
      return Float.class;
    } else if (type == double.class) {
      return Double.class;
    } else {
      return type;
    }
  }

  public Map<String, Class<?>> memberTypes() {
    return memberTypes;
  }

  public Map<String, Method> members() {
    return members;
  }

  public Map<String, Object> memberDefaults() {
    return memberDefaults;
  }

  public RetentionPolicy retention() {
    return retention;
  }

  public boolean isInherited() {
    return inherited;
  }

}
