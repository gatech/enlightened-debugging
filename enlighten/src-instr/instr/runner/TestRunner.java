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


package instr.runner;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import anonymous.domain.enlighten.data.MethodName;
import anonymous.domain.enlighten.data.TestName;
import instr.callback.DumpCoverageListener;
import instr.callback.DumpStatesListener;
import instr.callback.DumpTraceListener;
import instr.callback.InstrumentationCallback;
import instr.callback.InvocationTreeListener;
import instr.callback.PrintMemoryAccessListener;
import instr.callback.SingleThreadCallbackImpl;



public class TestRunner {

  public static final String TEST_OUTCOME_FILE_NAME = "test-outcomes.dat";
  
  public static String CURRENT_TEST;
  
  private static Path dataDirPath;
  private static Path testResultFilePath;

  


  public static void main(String[] args) throws ParseException, ClassNotFoundException, IOException {

    Option clazz = Option.builder("testclasses")
        .longOpt("testclasses")
        .type(String.class)
        .desc("comma separated list of test classes to run")
        .hasArg().build();
    Option testMethodsOption = Option.builder("test_methods")
        .longOpt("test_methods")
        .type(String.class)
        .desc("comma separated list of fully qualified names of the test methods to run")
        .hasArg().build();
    Option covDataDirOption = Option.builder("data_dir")
        .longOpt("data_dir")
        .required()
        .type(String.class)
        .desc("Path to the directory to store the coverage data files")
        .hasArg().build();
    Option writeTestOutcomesOption = Option.builder("write_test_outcomes")
        .longOpt("write_test_outcomes")
        .desc("whether or not the test outcomes should be written").build();
    Option writeCoverageOption = Option.builder("write_coverage")
        .longOpt("write_coverage")
        .desc("whether or not the coverage data should be written").build();
    Option writeInvocationTreeOption = Option.builder("write_invocation_tree")
        .longOpt("write_invocation_tree")
        .desc("whether or not the execution tree should be written").build();
    Option writeTraceOption = Option.builder("write_trace")
        .longOpt("write_trace")
        .desc("whether or not the execution trace should be written").build();
    Option writeMemAccessDevOption = Option.builder("write_mem_access_dev")
        .longOpt("write_mem_access_dev")
        .build();
    Option captureInvocStates = Option.builder("capture_invocation_states")
        .longOpt("capture_invocation_states")
        .build();
    Option captureStatesDataFile = Option.builder("capture_states_data_file")
        .longOpt("capture_states_data_file")
        .hasArg(true).type(String.class)
        .build();
    Option captureStatesTestDesc = Option.builder("capture_states_test_desc")
        .longOpt("capture_states_test_desc")
        .hasArg(true).type(String.class)
        .build();
    Option captureStatesMethodName = Option.builder("capture_states_method_name")
        .longOpt("capture_states_method_name")
        .hasArg(true).type(String.class)
        .build();
    Option captureStatesInvocIndex = Option.builder("capture_states_invocation_index")
        .longOpt("capture_states_invocation_index")
        .hasArg(true).type(Integer.class)
        .build();
    Option captureStatesProfileMem = Option.builder("capture_states_profile_mem")
        .longOpt("capture_states_profile_mem")
        .build();
    Option captureStatesRedefineClassOnTargetInvoc = Option.builder("capture_states_redefine_classname")
        .longOpt("capture_states_redefine_classname")
        .hasArg(true).type(String.class)
        .build();
    Option captureStatesRedefineClassFile = Option.builder("capture_states_redefine_classfile")
        .longOpt("capture_states_redefine_classfile")
        .hasArg(true).type(String.class)
        .build();
    final CommandLine cmd = Config.init(args, clazz, testMethodsOption, covDataDirOption, 
        writeTestOutcomesOption,  writeCoverageOption, writeInvocationTreeOption, 
        writeTraceOption, writeMemAccessDevOption, captureInvocStates, 
        captureStatesDataFile, captureStatesTestDesc, captureStatesMethodName, 
        captureStatesInvocIndex, captureStatesProfileMem, captureStatesRedefineClassOnTargetInvoc,
        captureStatesRedefineClassFile);
    
    dataDirPath = Paths.get(cmd.getOptionValue("data_dir"));
    Files.createDirectories(dataDirPath);
    if (cmd.hasOption("write_test_outcomes")) {
      testResultFilePath = dataDirPath.resolve(TEST_OUTCOME_FILE_NAME);
    }
    InstrumentationCallback.init(new SingleThreadCallbackImpl());
    if (cmd.hasOption("write_coverage")) {
      InstrumentationCallback.addCallbackListener(new DumpCoverageListener(dataDirPath));
    }
    if (cmd.hasOption("write_invocation_tree")) {
      InstrumentationCallback.addCallbackListener(new InvocationTreeListener(dataDirPath));
    }
    if (cmd.hasOption("write_trace")) {
      InstrumentationCallback.addCallbackListener(new DumpTraceListener(dataDirPath));
    }
    if (cmd.hasOption("write_mem_access_dev")) {
      InstrumentationCallback.addCallbackListener(new PrintMemoryAccessListener());
    }
    if (cmd.hasOption("capture_invocation_states")) {
      String dataFile = cmd.getOptionValue("capture_states_data_file");
      String targetTest = cmd.getOptionValue("capture_states_test_desc");
      String targetMethod = cmd.getOptionValue("capture_states_method_name");
      String targetInvocIndexStr = cmd.getOptionValue("capture_states_invocation_index");
      int targetInvocIndex = 0;
      try {
        targetInvocIndex = Integer.parseInt(targetInvocIndexStr);
      } catch (NumberFormatException ex) {
        throw new ParseException(
            "Invalid capture_states_invocation_index: " + targetInvocIndexStr);
      }
      boolean profileMemAccess = cmd.hasOption("capture_states_profile_mem");
      String redefineClassName = null;
      String redefineClassFile = null;
      if (cmd.hasOption("capture_states_redefine_classname")) {
        redefineClassName = cmd.getOptionValue("capture_states_redefine_classname");
        redefineClassFile = cmd.getOptionValue("capture_states_redefine_classfile");
        if (redefineClassFile == null) {
          throw new ParseException(
              "Class file path should be specified for the class to be re-defined.");
        }
      }
      if (dataFile == null || targetTest == null || targetMethod == null) {
        throw new ParseException(
            "Arguments for capturing invocations states not fully specified.");
      }
      DumpStatesListener dumpStatesListener = new DumpStatesListener(
          TestName.parseFromDescription(targetTest), 
          MethodName.get(targetMethod), 
          targetInvocIndex, 
          Paths.get(dataFile),
          profileMemAccess);
      if (redefineClassName != null) {
        byte[] redefinedClassContent = Files.readAllBytes(Paths.get(redefineClassFile));
        dumpStatesListener.redefineClassOnTargetInvocation(
            redefineClassName, redefinedClassContent);
      }
      InstrumentationCallback.addCallbackListener(dumpStatesListener);
    }


    JUnitCore junit = new JUnitCore();
    
    final Map<String, Integer> testNameCache = new HashMap<String, Integer>();
    
    junit.addListener(new RunListener(){

      

      @Override
      public void testStarted(Description desc) throws IOException{
        failed = false;
        failure = null;
        String name = desc.getDisplayName();
        if (name.contains("warning")) {
          System.out.println("DEBUG: " + TestName.parseFromDescription(name).toString());
        }
        if (name == null || name.equals("null") 
            || TestName.parseFromDescription(name).getTestClassName()
              .contains("junit.framework.TestSuite")) {
          
          return;
        }
        Integer id = testNameCache.get(name);
        if (id == null) {
          id = 1;
          testNameCache.put(name, id);
        } else {
          testNameCache.put(name, ++id);
          name = name + id;
        }
        CURRENT_TEST = name;
        System.out.println("Starting test: " + CURRENT_TEST);
        InstrumentationCallback.executionStarted(CURRENT_TEST);
      }

      

      boolean failed;
      Failure failure;

      @Override
      public void testFailure(Failure failure) throws Exception { 
        super.testFailure(failure);
        failed = true;
        this.failure = failure;
      }

      @Override
      public void testFinished(Description result) throws Exception { 
        super.testFinished(result);
        String name = result.getDisplayName();
        if (name == null || name.equals("null") 
            || TestName.parseFromDescription(name).getTestClassName()
              .contains("junit.framework.TestSuite")) {
          
          return;
        }
        passFail(!failed, result);
        InstrumentationCallback.executionEnded(CURRENT_TEST);
        if (failed) {





          System.err.println(failure.toString());
          System.err.println(failure.getTrace());
        }
        System.out.println("Test: " + CURRENT_TEST + " ended.");
        CURRENT_TEST = null;
      }

      void passFail(boolean isPassing, Description desc) {
        if (testResultFilePath == null) {
          return;
        }
        try {
          
          BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(testResultFilePath.toString(), true)));
          String s = String.format("%s [%s]\n", CURRENT_TEST,  isPassing?"PASSED":"FAILED");
          bw.write(s);
          bw.flush();
          bw.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    
    String testMethods = cmd.getOptionValue("test_methods");
    String testClassesList = cmd.getOptionValue("testclasses");
    if (testMethods != null && testMethods.length() > 0) {
      String[] testMethodsArray = testMethods.split(Pattern.quote(","));
      for (String testMethod : testMethodsArray) {
        if (testMethod != null && testMethod.length() > 0) {
          MethodName testMethodName = MethodName.get(testMethod);
          junit.run(Request.method(
              Class.forName(testMethodName.getClassName()), testMethodName.getMethodNameSig()));
        }
      }
    } else if (testClassesList != null) {
      String[] testClassNames = testClassesList.split(Pattern.quote(","));
      Class<?>[] testClasses = new Class<?>[testClassNames.length];
      for (int i = 0; i < testClassNames.length; ++i) {
        testClasses[i] = Class.forName(testClassNames[i]);
      }
      junit.run(testClasses);
    } else {
      throw new ParseException("Either test_descs or testclass option should be specified.");
    }
  }
}
