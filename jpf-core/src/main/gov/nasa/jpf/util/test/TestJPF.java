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


package gov.nasa.jpf.util.test;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFShell;
import gov.nasa.jpf.Property;
import gov.nasa.jpf.annotation.FilterField;
import gov.nasa.jpf.tool.RunTest;
import gov.nasa.jpf.util.DevNullPrintStream;
import gov.nasa.jpf.util.TypeRef;
import gov.nasa.jpf.util.JPFSiteUtils;
import gov.nasa.jpf.util.Reflection;
import gov.nasa.jpf.vm.ExceptionInfo;
import gov.nasa.jpf.vm.NoUncaughtExceptionsProperty;
import gov.nasa.jpf.vm.NotDeadlockedProperty;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


public abstract class TestJPF implements JPFShell  {
  static PrintStream out = System.out;

  public static final String UNNAMED_PACKAGE = "";
  public static final String SAME_PACKAGE = null;


  @FilterField protected static boolean globalRunDirectly, globalShowConfig;

  @FilterField protected static boolean runDirectly; 
  @FilterField protected static boolean stopOnFailure; 
  @FilterField protected static boolean showConfig; 
  @FilterField protected static boolean showConfigSources; 
  @FilterField protected static boolean hideSummary;
  
  @FilterField protected static boolean quiet; 
  
  @FilterField protected String sutClassName;

  static class GlobalArg {
    String key;
    String val;

    GlobalArg (String k, String v){
      key = k;
      val = v;
    }
  }





  @FilterField static ArrayList<GlobalArg> globalArgs;

  protected static ArrayList<GlobalArg> getGlobalArgs() {

    Config globalConf = RunTest.getConfig();
    if (globalConf != null){
      ArrayList<GlobalArg> list = new ArrayList<GlobalArg>();


      String[] testKeys = globalConf.getKeysStartingWith("test.");
      if (testKeys.length > 0){
        for (String key : testKeys){
          String val = globalConf.getString(key);


          if (val.equals("REMOVE")){
            val = null;
          }
          
          key = key.substring(5);
          
          list.add(new GlobalArg(key,val));
        }
      }

      return list;
    }

    return null;
  }

  static {
    if (!isJPFRun()){
      globalArgs = getGlobalArgs();
    }
  }



  public static void fail (String msg, String[] args, String cause){
    StringBuilder sb = new StringBuilder();

    sb.append(msg);
    if (args != null){
      for (String s : args){
        sb.append(s);
        sb.append(' ');
      }
    }

    if (cause != null){
      sb.append(':');
      sb.append(cause);
    }

    fail(sb.toString());
  }

  public static void fail (){
    throw new AssertionError();
  }

  public static void fail (String msg){
    throw new AssertionError(msg);
  }

  public void report (String[] args) {
    if (!quiet){
      out.print("  running jpf with args:");

      for (int i = 0; i < args.length; i++) {
        out.print(' ');
        out.print(args[i]);
      }

      out.println();
    }
  }

  
  protected static String getSutClassName (String testClassName, String sutPackage){

    String sutClassName = testClassName;

    int i = sutClassName.lastIndexOf('.');
    if (i >= 0){  

      if (sutPackage == null){   

      } else if (sutPackage.length() > 0) { 
        sutClassName = sutPackage + sutClassName.substring(i);

      } else { 
        sutClassName = sutClassName.substring(i+1);
      }

    } else { 
      if (sutPackage == null || sutPackage.length() == 0){   

      } else { 
        sutClassName = sutPackage + '.' + sutClassName;
      }
    }

    if (sutClassName.endsWith("JPF")) {
      sutClassName = sutClassName.substring(0, sutClassName.length() - 3);
    }

    return sutClassName;
  }





  public TestJPF () {
    sutClassName = getSutClassName(getClass().getName(), SAME_PACKAGE);
  }





  
  protected TestJPF (String sutClassName){
    this.sutClassName = sutClassName;
  }

  public static boolean isJPFRun () {
    return false;
  }

  public static boolean isJUnitRun() {

    Throwable t = new Throwable();
    t.fillInStackTrace();

    for (StackTraceElement se : t.getStackTrace()){
      if (se.getClassName().startsWith("org.junit.")){
        return true;
      }
    }

    return false;
  }

  public static boolean isRunTestRun() {

    Throwable t = new Throwable();
    t.fillInStackTrace();

    for (StackTraceElement se : t.getStackTrace()){
      if (se.getClassName().equals("gov.nasa.jpf.tool.RunTest")){
        return true;
      }
    }

    return false;
  }


  protected static void getOptions (String[] args){
    runDirectly = globalRunDirectly;
    showConfig = globalShowConfig;



    if (args != null){   
      for (int i=0; i<args.length; i++){
        String a = args[i];
        if (a != null){
          if (a.length() > 0){
            if (a.charAt(0) == '-'){
              a = a.substring(1);
              
              if (a.equals("d")){
                runDirectly = true;
              } else if (a.equals("s") || a.equals("show")){
                showConfig = true;
              } else if (a.equals("l") || a.equals("log")){
                showConfigSources = true;
              } else if (a.equals("q") || a.equals("quiet")){
                quiet = true;                
              } else if (a.equals("x")){
                stopOnFailure = true;
              } else if (a.equals("h")){
                hideSummary = true;
              }
              args[i] = null;  

            } else {
              break; 
            }
          }
        }
      }
    }
  }

  protected static boolean hasExplicitTestMethods(String[] args){
    for (String a : args){
      if (a != null){
        return true;
      }
    }

    return false;
  }

  protected static List<Method> getMatchingMethods(Class<? extends TestJPF> testCls,
          int setModifiers, int unsetModifiers, String[] annotationNames){
    List<Method> list = new ArrayList<Method>();
    
    for (Method m : testCls.getMethods()){
      if (isMatchingMethod(m, setModifiers, unsetModifiers, annotationNames)){
        list.add(m);
      }
    }
    
    return list;
  }

  protected static boolean isMatchingMethod(Method m, int setModifiers, int unsetModifiers, String[] annotationNames) {
    int mod = m.getModifiers();
    if (((mod & setModifiers) != 0) && ((mod & unsetModifiers) == 0)) {
      if (m.getParameterTypes().length == 0) {
        if (annotationNames != null){
          Annotation[] annotations = m.getAnnotations();
          for (int i = 0; i < annotations.length; i++) {
            String annotType = annotations[i].annotationType().getName();
            for (int j = 0; j < annotationNames.length; j++) {
              if (annotType.equals(annotationNames[j])) {
                return true;
              }
            }
          }
        } else {
          return true;
        }
      }
    }

    return false;
  }

  protected static List<Method> getContextMethods(Class<? extends TestJPF> testCls, 
                                                  int setModifiers, int unsetModifiers, String annotation){
    String[] annotations = {annotation};

    List<Method> list = new ArrayList<Method>();
    for (Method m : testCls.getMethods()){
      if (isMatchingMethod(m, setModifiers, unsetModifiers, annotations)){
        list.add(m);
      }
    }
    return list;
  }

  protected static List<Method> getBeforeMethods(Class<? extends TestJPF> testCls){
    return getContextMethods(testCls, Modifier.PUBLIC, Modifier.STATIC, "org.junit.Before");
  }

  protected static List<Method> getAfterMethods(Class<? extends TestJPF> testCls){
    return getContextMethods(testCls, Modifier.PUBLIC, Modifier.STATIC, "org.junit.After");
  }

  protected static List<Method> getBeforeClassMethods(Class<? extends TestJPF> testCls){
    return getContextMethods(testCls, Modifier.PUBLIC | Modifier.STATIC, 0, "org.junit.BeforeClass");
  }
  
  protected static List<Method> getAfterClassMethods(Class<? extends TestJPF> testCls){
    return getContextMethods(testCls, Modifier.PUBLIC | Modifier.STATIC, 0, "org.junit.AfterClass");
  }
  
  protected static boolean haveTestMethodSpecs( String[] args){
    if (args != null && args.length > 0){
      for (int i=0; i<args.length; i++){
        if (args[i] != null){
          return true;
        }
      }
    }
    
    return false;
  }
  
  protected static List<Method> getTestMethods(Class<? extends TestJPF> testCls, String[] args){
    String[] testAnnotations = {"org.junit.Test", "org.testng.annotations.Test"};

    if (haveTestMethodSpecs( args)){ 
      List<Method> list = new ArrayList<Method>();

      for (String test : args){
        if (test != null){

          try {
            Method m = testCls.getMethod(test);

            if (isMatchingMethod(m, Modifier.PUBLIC, Modifier.STATIC, null  )){
              list.add(m);
            } else {
              throw new RuntimeException("test method must be @Test annotated public instance method without arguments: " + test);
            }

          } catch (NoSuchMethodException x) {
            throw new RuntimeException("method: " + test
                    + "() not in test class: " + testCls.getName(), x);
          }
        }
      }
      
      return list;

    } else { 
      return getMatchingMethods(testCls, Modifier.PUBLIC, Modifier.STATIC, testAnnotations);
    }
  }


  protected static void reportTestStart(String mthName){
    if (!quiet){
      System.out.println();
      System.out.print("......................................... testing ");
      System.out.print(mthName);
      System.out.println("()");
    }
  }

  protected static void reportTestInitialization(String mthName){
    if (!quiet){
      System.out.print(".... running test initialization: ");
      System.out.print(mthName);
      System.out.println("()");
    }
  }

  protected static void reportTestCleanup(String mthName){
    if (!quiet){
      System.out.print(".... running test cleanup: ");
      System.out.print(mthName);
      System.out.println("()");
    }
  }

  protected static void reportTestFinished(String msg){
    if (!quiet){
      System.out.print("......................................... ");
      System.out.println(msg);
    }
  }

  protected static void reportResults(String clsName, int nTests, int nFailures, int nErrors, List<String> results){
    System.out.println();
    System.out.print("......................................... execution of testsuite: " + clsName);
    if (nFailures > 0 || nErrors > 0){
      System.out.println(" FAILED");
    } else if (nTests > 0) {
      System.out.println(" SUCCEEDED");
    } else {
      System.out.println(" OBSOLETE");
    }

    if (!quiet){
      if (results != null) {
        int i = 0;
        for (String result : results) {
          System.out.print(".... [" + ++i + "] ");
          System.out.println(result);
        }
      }
    }

    System.out.print(".........................................");
    System.out.println(" tests: " + nTests + ", failures: " + nFailures + ", errors: " + nErrors);
  }

  
  static void invoke (Method m, Object testObject) throws IllegalAccessException, InvocationTargetException  {
    PrintStream sysOut = null;
    
    try {
      if (quiet){
        sysOut = System.out;
        System.setOut( new DevNullPrintStream());
      }
      
      m.invoke( testObject);
      
    } finally {
      if (quiet){
        System.setOut( sysOut);
      }
    }
  }
  
  
  public static void runTests (Class<? extends TestJPF> testCls, String... args){
    int nTests = 0;
    int nFailures = 0;
    int nErrors = 0;
    String testMethodName = null;
    List<String> results = null;

    getOptions(args);
    globalRunDirectly = runDirectly;
    globalShowConfig = showConfig;
    boolean globalStopOnFailure = stopOnFailure;

    try {
      List<Method> testMethods = getTestMethods(testCls, args);
      results = new ArrayList<String>(testMethods.size());



            
      List<Method> beforeClassMethods = getBeforeClassMethods(testCls);
      List<Method> afterClassMethods = getAfterClassMethods(testCls);
            
      List<Method> beforeMethods = getBeforeMethods(testCls);
      List<Method> afterMethods = getAfterMethods(testCls);

      for (Method initMethod : beforeClassMethods) {
        reportTestInitialization(initMethod.getName());
        initMethod.invoke(null);
      }
            
      for (Method testMethod : testMethods) {
        testMethodName = testMethod.getName();
        String result = testMethodName;
        try {
          Object testObject = testCls.newInstance();

          nTests++;
          reportTestStart( testMethodName);


          for (Method initMethod : beforeMethods){
            reportTestInitialization( initMethod.getName());
            invoke( initMethod, testObject);
          }


          invoke( testMethod, testObject);
          result += ": Ok";


          for (Method cleanupMethod : afterMethods){
            reportTestCleanup( cleanupMethod.getName());
            invoke( cleanupMethod, testObject);
          }

        } catch (InvocationTargetException x) {
          Throwable cause = x.getCause();
          cause.printStackTrace();
          if (cause instanceof AssertionError) {
            nFailures++;
            reportTestFinished("test method failed with: " + cause.getMessage());
            result += ": Failed";
          } else {
            nErrors++;
            reportTestFinished("unexpected error while executing test method: " + cause.getMessage());
            result += ": Error";
          }

          if (globalStopOnFailure){
            break;
          }
        }
        
        results.add(result);
        reportTestFinished(result);
      }
      
      for (Method cleanupMethod : afterClassMethods) {
        reportTestCleanup( cleanupMethod.getName());
        cleanupMethod.invoke(null);
      }



    } catch (InvocationTargetException x) {
      Throwable cause = x.getCause();
      cause.printStackTrace();
      nErrors++;
      reportTestFinished("TEST ERROR: @BeforeClass,@AfterClass method failed: " + x.getMessage());
      
    } catch (InstantiationException x) {
      nErrors++;
      reportTestFinished("TEST ERROR: cannot instantiate test class: " + x.getMessage());
    } catch (IllegalAccessException x) { 
      nErrors++;
      reportTestFinished("TEST ERROR: default constructor or test method not public: " + testMethodName);
    } catch (IllegalArgumentException x) {  
      nErrors++;
      reportTestFinished("TEST ERROR: illegal argument for test method: " + testMethodName);
    } catch (RuntimeException rx) {
      nErrors++;
      reportTestFinished("TEST ERROR: " + rx.toString());
    }

    if (!hideSummary){
      reportResults(testCls.getName(), nTests, nFailures, nErrors, results);
    }

    if (nErrors > 0 || nFailures > 0){
      if (isRunTestRun()){

        throw new RunTest.Failed();
      }
    }
  }

  static String getProperty(String key){

    return null;
  }
  
  
  static void runTestMethod(String args[]) throws Throwable {
    String testClsName = getProperty("target");
    String testMthName = getProperty("target.test_method");
    
    Class<?> testCls = Class.forName(testClsName);
    Object target = testCls.newInstance();
    
    Method method = testCls.getMethod(testMthName);

    try {
      method.invoke(target);
    } catch (InvocationTargetException e) {
      throw e.getCause(); 
    }
  }

  
  protected static void runTestsOfThisClass (String[] testMethods){

    Class<? extends TestJPF> testClass = Reflection.getCallerClass(TestJPF.class);
    runTests(testClass, testMethods);
  }

  
  protected JPF createAndRunJPF (StackTraceElement testMethod, String[] args) {
    JPF jpf = createJPF( testMethod, args);
    if (jpf != null){
      jpf.run();
    }
    return jpf;
  }

  
  protected JPF createJPF (StackTraceElement testMethod, String[] args) {
    JPF jpf = null;
    
    Config conf = new Config(args);


    if (globalArgs != null) {
      for (GlobalArg ga : globalArgs) {
        String key = ga.key;
        String val = ga.val;
        if (val != null){
          conf.put(key, val);
        } else {
          conf.remove(key);
        }
      }
    }

    setTestTargetKeys(conf, testMethod);
    

    String projectId = JPFSiteUtils.getCurrentProjectId();
    if (projectId != null) {
      String testCp = conf.getString(projectId + ".test_classpath");
      if (testCp != null) {
        conf.append("classpath", testCp, ",");
      }
    }


    conf.promotePropertyCategory("test.");

    getOptions(args);

    if (showConfig || showConfigSources) {
      PrintWriter pw = new PrintWriter(System.out, true);
      if (showConfigSources) {
        conf.printSources(pw);
      }

      if (showConfig) {
        conf.print(pw);
      }
      pw.flush();
    }

    jpf = new JPF(conf);

    return jpf;
  }

  protected void setTestTargetKeys(Config conf, StackTraceElement testMethod) {
    conf.put("target.entry", "runTestMethod([Ljava/lang/String;)V");
    conf.put("target", testMethod.getClassName());
    conf.put("target.test_method", testMethod.getMethodName());
  }


  @Override
  public void start(String[] testMethods){
    Class<? extends TestJPF> testClass = getClass(); 
    runTests(testClass, testMethods);
  }

  protected StackTraceElement getCaller(){
    StackTraceElement[] st = (new Throwable()).getStackTrace();
    return st[2];
  }
  
  protected StackTraceElement setTestMethod (String clsName, String mthName){
    return new StackTraceElement( clsName, mthName, null, -1);
  }
  
  protected StackTraceElement setTestMethod (String mthName){
    return new StackTraceElement( getClass().getName(), mthName, null, -1);
  }
  
  


  
  protected JPF assertionError (StackTraceElement testMethod, String... args){
    return unhandledException( testMethod, "java.lang.AssertionError", null, args );    
  }
  protected JPF assertionError (String... args) {
    return unhandledException( getCaller(), "java.lang.AssertionError", null, args );
  }
  
  protected JPF assertionErrorDetails (StackTraceElement testMethod, String details, String... args) {
    return unhandledException( testMethod, "java.lang.AssertionError", details, args );
  }
  protected JPF assertionErrorDetails (String details, String... args) {
    return unhandledException( getCaller(), "java.lang.AssertionError", details, args );
  }
  protected boolean verifyAssertionErrorDetails (String details, String... args){
    if (runDirectly) {
      return true;
    } else {
      unhandledException( getCaller(), "java.lang.AssertionError", details, args);
      return false;
    }
  }
  protected boolean verifyAssertionError (String... args){
    if (runDirectly) {
      return true;
    } else {
      unhandledException( getCaller(), "java.lang.AssertionError", null, args);
      return false;
    }
  }

  
  protected JPF noPropertyViolation (StackTraceElement testMethod, String... args) {
    JPF jpf = null;

    report(args);

    try {
      jpf = createAndRunJPF( testMethod, args);
    } catch (Throwable t) {

      t.printStackTrace();
      fail("JPF internal exception executing: ", args, t.toString());
      return jpf;
    }

    List<Error> errors = jpf.getSearchErrors();
    if ((errors != null) && (errors.size() > 0)) {
      fail("JPF found unexpected errors: " + (errors.get(0)).getDescription());
    }

    return jpf;
  }
  
  protected JPF noPropertyViolation (String... args) {
    return noPropertyViolation( getCaller(), args);    
  }
  
  protected boolean verifyNoPropertyViolation (String...args){
    if (runDirectly) {
      return true;
    } else {
      noPropertyViolation( getCaller(), args);
      return false;
    }
  }

  
  protected JPF unhandledException (StackTraceElement testMethod, String xClassName, String details, String... args) {
    JPF jpf = null;

    report(args);

    try {
      jpf = createAndRunJPF(testMethod, args);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("JPF internal exception executing: ", args, t.toString());
      return jpf;
    }

    Error error = jpf.getLastError();
    if (error != null){
      Property errorProperty = error.getProperty();
      if (errorProperty instanceof NoUncaughtExceptionsProperty){ 
        ExceptionInfo xi = ((NoUncaughtExceptionsProperty)errorProperty).getUncaughtExceptionInfo();
        String xn = xi.getExceptionClassname();
        if (!xn.equals(xClassName)) {
          fail("JPF caught wrong exception: " + xn + ", expected: " + xClassName);
        }

        if (details != null) {
          String gotDetails = xi.getDetails();
          if (gotDetails == null) {
            fail("JPF caught the right exception but no details, expected: " + details);
          } else {
            if (!gotDetails.endsWith(details)) {
              fail("JPF caught the right exception but the details were wrong: " + gotDetails + ", expected: " + details);
            }
          }
        }
      } else { 
        fail("JPF failed to catch exception executing: ", args, ("expected " + xClassName));        
      }
    } else { 
      fail("JPF failed to catch exception executing: ", args, ("expected " + xClassName));
    }
    
    return jpf;
  }
  
  protected JPF unhandledException (String xClassName, String details, String... args) {
    return unhandledException( getCaller(), xClassName, details, args);
  }

    
  protected boolean verifyUnhandledExceptionDetails (String xClassName, String details, String... args){
    if (runDirectly) {
      return true;
    } else {
      unhandledException( getCaller(), xClassName, details, args);
      return false;
    }
  }
  protected boolean verifyUnhandledException (String xClassName, String... args){
    if (runDirectly) {
      return true;
    } else {
      unhandledException( getCaller(), xClassName, null, args);
      return false;
    }
  }


  
  protected JPF jpfException (StackTraceElement testMethod, Class<? extends Throwable> xCls, String... args) {
    JPF jpf = null;
    Throwable exception = null;

    report(args);

    try {
      jpf = createAndRunJPF( testMethod, args);
    } catch (JPF.ExitException xx) {
      exception = xx.getCause();
    } catch (Throwable x) {
      exception = x;
    }

    if (exception != null){
      if (!xCls.isAssignableFrom(exception.getClass())){
        fail("JPF produced wrong exception: " + exception + ", expected: " + xCls.getName());
      }
    } else {
      fail("JPF failed to produce exception, expected: " + xCls.getName());
    }

    return jpf;
  }
  
  protected JPF jpfException (Class<? extends Throwable> xCls, String... args) {
    return jpfException( getCaller(), xCls, args);
  }  
  
  protected boolean verifyJPFException (TypeRef xClsSpec, String... args){
    if (runDirectly) {
      return true;

    } else {
      try {
        Class<? extends Throwable> xCls = xClsSpec.asNativeSubclass(Throwable.class);

        jpfException( getCaller(), xCls, args);

      } catch (ClassCastException ccx){
        fail("not a property type: " + xClsSpec);
      } catch (ClassNotFoundException cnfx){
        fail("property class not found: " + xClsSpec);
      }
      return false;
    }
  }

  
  
  
  protected JPF propertyViolation (StackTraceElement testMethod, Class<? extends Property> propertyCls, String... args ){
    JPF jpf = null;

    report(args);

    try {
      jpf = createAndRunJPF( testMethod, args);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("JPF internal exception executing: ", args, t.toString());
    }

    List<Error> errors = jpf.getSearchErrors();
    if (errors != null) {
      for (Error e : errors) {
        if (propertyCls == e.getProperty().getClass()) {
          return jpf; 
        }
      }
    }

    fail("JPF failed to detect error: " + propertyCls.getName());
    return jpf;
  }
  
  protected JPF propertyViolation (Class<? extends Property> propertyCls, String... args ){
    return propertyViolation( getCaller(), propertyCls, args);
  }
  
  protected boolean verifyPropertyViolation (TypeRef propertyClsSpec, String... args){
    if (runDirectly) {
      return true;

    } else {
      try {
        Class<? extends Property> propertyCls = propertyClsSpec.asNativeSubclass(Property.class);
        propertyViolation( getCaller(), propertyCls, args);

      } catch (ClassCastException ccx){
        fail("not a property type: " + propertyClsSpec);
      } catch (ClassNotFoundException cnfx){
        fail("property class not found: " + propertyClsSpec);
      }
      return false;
    }
  }


  
  protected JPF deadlock (String... args) {
    return propertyViolation( getCaller(), NotDeadlockedProperty.class, args );
  }
  
  protected boolean verifyDeadlock (String... args){
    if (runDirectly) {
      return true;
    } else {
      propertyViolation( getCaller(), NotDeadlockedProperty.class, args);
      return false;
    }
  }
    



  public static void assertEquals(String msg, Object expected, Object actual){
    if (expected == null && actual == null) { 
      return; 
    }
    
    if (expected != null && expected.equals(actual)) {
      return; 
    }
    
    fail(msg);
  }

  public static void assertEquals(Object expected, Object actual){
  	try {
  		assertEquals("", expected, actual);
  	} catch (AssertionError ex) {
  		System.out.println("expected: " + expected.toString());
  		System.out.println("actual: " + actual.toString());
  		throw ex;
  	}
  }

  public static void assertEquals(String msg, int expected, int actual){
    if (expected != actual) {
      fail(msg);
    }
  }

  public static void assertEquals(int expected, int actual){    
    assertEquals("expected != actual : " + expected + " != " + actual, expected, actual);
  }  

  public static void assertEquals(String msg, long expected, long actual){
    if (expected != actual) {
      fail(msg);
    }
  }

  public static void assertEquals(long expected, long actual){    
      assertEquals("expected != actual : " + expected + " != " + actual,
                   expected, actual);
  }

  public static void assertEquals(double expected, double actual){
    if (expected != actual){
      fail("expected != actual : " + expected + " != " + actual);
    }
  }

  public static void assertEquals(String msg, double expected, double actual){
    if (expected != actual){
      fail(msg);
    }
  }

  public static void assertEquals(float expected, float actual){
    if (expected != actual){
      fail("expected != actual : " + expected + " != " + actual);
    }
  }

  public static void assertEquals(String msg, float expected, float actual){
    if (expected != actual){
      fail(msg);
    }
  }

  public static void assertEquals(String msg, double expected, double actual, double delta){
    if (Math.abs(expected - actual) > delta) {
      fail(msg);
    }
  }

  public static void assertEquals(double expected, double actual, double delta){    
    assertEquals("Math.abs(expected - actual) > delta : " + "Math.abs(" + expected + " - " + actual + ") > " + delta,
                 expected, actual, delta);
  }

  public static void assertEquals(String msg, float expected, float actual, float delta){
    if (Math.abs(expected - actual) > delta) {
      fail(msg);
    }
  }

  public static void assertEquals(float expected, float actual, float delta){    
      assertEquals("Math.abs(expected - actual) > delta : " + "Math.abs(" + expected + " - " + actual + ") > " + delta,
                   expected, actual, delta);
  }

  public static void assertArrayEquals(byte[] expected, byte[] actual){
    if (((expected == null) != (actual == null)) ||
        (expected.length != actual.length)){
      fail("array sizes different");
    }

    for (int i=0; i<expected.length; i++){
      if (expected[i] != actual[i]){
        fail("array element" + i + " different, expected " + expected[i] + ", actual " + actual[i]);
      }
    }
  }

  public static void assertNotNull(String msg, Object o) {
    if (o == null) {
      fail(msg);
    }
  }

  public static void assertNotNull(Object o){
    assertNotNull("o == null", o);
  }

  public static void assertNull(String msg, Object o){
    if (o != null) {
      fail(msg);
    }
  }

  public static void assertNull(Object o){    
    assertNull("o != null", o);
  }

  public static void assertSame(String msg, Object expected, Object actual){
    if (expected != actual) {
      fail(msg);
    }
  }

  public static void assertSame(Object expected, Object actual){
    assertSame("expected != actual : " + expected + " != " + actual, expected, actual);
  }

  public static void assertFalse (String msg, boolean cond){
    if (cond) {
      fail(msg);
    }
  }

  public static void assertFalse (boolean cond){
    assertFalse("", cond);
  }

  public static void assertTrue (String msg, boolean cond){
    if (!cond) {
      fail(msg);
    }
  }

  public static void assertTrue (boolean cond){
    assertTrue("", cond);
  }
}
