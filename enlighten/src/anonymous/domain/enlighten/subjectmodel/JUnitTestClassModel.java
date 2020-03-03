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


package anonymous.domain.enlighten.subjectmodel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


public class JUnitTestClassModel {

  private Class<?> testClass;
  private boolean isJUnit3TestClass;

  private List<Method> testMethods;
  
  
  public static JUnitTestClassModel getJUnitTestClassModel(Class<?> clazz) {
    if ((clazz.getModifiers() & Modifier.ABSTRACT) != 0) {
      return null;
    }
    JUnitTestClassModel model = new JUnitTestClassModel(clazz);
    if (model.isTestClass()) {
      return model;
    } else {
      return null;
    }
  }
  
  protected JUnitTestClassModel(Class<?> testClass) {
    this.testClass = testClass;
    isJUnit3TestClass = junit.framework.TestCase.class.isAssignableFrom(testClass);
  }

  
  public List<Method> getTestMethods() {
    if (testMethods == null) {
      doGetTestMethods();
    }
    return testMethods;
  }
  
  public Class<?> getTestClassObject() {
    return testClass;
  }

  public String getTestClassName() {
    return testClass.getName();
  }
  
  public boolean isTestClass() {
    return isJUnit3StyleTestClass() || (getTestMethods() != null && getTestMethods().size() > 0);
  }

  
  public boolean isJUnit3StyleTestClass() {
    return isJUnit3TestClass;
  }
  
  private void doGetTestMethods() {
    testMethods = new ArrayList<>();
    Method[] allPublicMethods = testClass.getMethods();
    for (Method method : allPublicMethods) {
      if (method.getAnnotation(org.junit.Test.class) != null) {

        testMethods.add(method);
      } else if (isJUnit3TestClass && method.getName().startsWith("test")
          && method.getParameterTypes().length == 0 && method.getReturnType().equals(Void.TYPE)) {





        testMethods.add(method);
      }
    }
  }
}
