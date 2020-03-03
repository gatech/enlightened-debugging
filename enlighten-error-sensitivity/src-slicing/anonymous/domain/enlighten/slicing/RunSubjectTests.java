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


package anonymous.domain.enlighten.slicing;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import anonymous.domain.enlighten.ExperimentDataLayout;
import anonymous.domain.enlighten.exec.CompileSubjectProgram;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.tool.RunJPF;

@SuppressWarnings("unused")
public class RunSubjectTests {

	public static void main(String[] args) throws Throwable {
	  
		File[] subjects = null;
    File subjectsRoot = new File(ExperimentDataLayout.SUBJECTS_ROOT);
    subjects = subjectsRoot.listFiles();
    for (File subject : subjects) {
      String subjectName = subject.getName();
      if (!subject.isDirectory()) {
        warning(subjectName, subjectName + " is not a directory", null);
        continue;
      }
      System.out.println("JPF Test: Running tests on subject " + subjectName);
      SubjectProgram faultyVersion = null;
      try {
        faultyVersion = SubjectProgram.openSubjectProgram(
            subject.toPath().resolve(ExperimentDataLayout.FAULTY_VERSION_DIR), 
            Paths.get(
                ExperimentDataLayout.DATA_ROOT, subjectName,
                ExperimentDataLayout.FAULTY_VERSION_DIR));
        List<String> testClassNames = faultyVersion.listTestClasses().stream()
        		.map(testClass -> { return testClass.getName(); })
        		.collect(Collectors.toList());
        List<Path> classpathEntries = new ArrayList<>();
        classpathEntries.addAll(faultyVersion.getAppSourceDirs());
        classpathEntries.addAll(faultyVersion.getTestSourceDirs());
        classpathEntries.addAll(faultyVersion.getLibPaths());
        List<String> classpathEntryStrs = classpathEntries.stream()
        		.map(path -> { return path.toAbsolutePath().toString(); })
        		.collect(Collectors.toList());
        String classpathStr = concatStrings(classpathEntryStrs, ",");

        for (String testClassName : testClassNames) {
          System.out.println("Test class: " + testClassName);
          List<String> extraConfigs = new ArrayList<>();
          extraConfigs.add("+listener=anonymous.domain.enlighten.slicing.TestingJPFListener,gov.nasa.jpf.vm.JVMForwarder");
          extraConfigs.add("+nhandler.delegateUnhandledNative = true");
          extraConfigs.add("+search.class=gov.nasa.jpf.search.PathSearch");
          extraConfigs.add("+classpath=" + classpathStr);
          extraConfigs.add("+native_classpath=" + classpathStr); 
          extraConfigs.add("+target=org.junit.runner.JUnitCore");
          extraConfigs.add("+target.args=" + testClassName);

          Config conf = JPF.createConfig(extraConfigs.toArray(new String[0]));
          JPF jpf = new JPF(conf);
          jpf.run();
        }
        
      } catch (Throwable ex) {
      	System.err.println("Error: " + subjectName);
      }
    }
    

	  
	}
	
	public static void runTestClass(String subjectName, String testClassName) throws Throwable {
	  SubjectProgram faultyVersion = SubjectProgram.openSubjectProgram(
	      Paths.get(
            ExperimentDataLayout.SUBJECTS_ROOT, subjectName,
            ExperimentDataLayout.FAULTY_VERSION_DIR), 
        Paths.get(
            ExperimentDataLayout.DATA_ROOT, subjectName,
            ExperimentDataLayout.FAULTY_VERSION_DIR));

    List<Path> classpathEntries = new ArrayList<>();
    classpathEntries.addAll(faultyVersion.getAppSourceDirs());
    classpathEntries.addAll(faultyVersion.getTestSourceDirs());
    classpathEntries.addAll(faultyVersion.getLibPaths());
    List<String> classpathEntryStrs = classpathEntries.stream()
        .map(path -> { return path.toAbsolutePath().toString(); })
        .collect(Collectors.toList());
    String classpathStr = concatStrings(classpathEntryStrs, ",");
    System.out.println("Test class: " + testClassName);
    List<String> extraConfigs = new ArrayList<>();

    extraConfigs.add("+listener=anonymous.domain.enlighten.slicing.TestingJPFListener,gov.nasa.jpf.vm.JVMForwarder");
    extraConfigs.add("+search.class=gov.nasa.jpf.search.PathSearch");
    extraConfigs.add("+nhandler.delegateUnhandledNative = true");
    extraConfigs.add("+classpath=" + classpathStr);
    extraConfigs.add("+native_classpath=" + classpathStr); 
    extraConfigs.add("+target=org.junit.runner.JUnitCore");
    extraConfigs.add("+target.args=" + testClassName);
    RunJPF.main(extraConfigs.toArray(new String[0]));
    
    
	}
	
  public static void warning(String subjectName, String message, Throwable exc) {
    if (exc != null) {
      exc.printStackTrace();
    }
    System.err.println(message);
    System.err.println("Skipped the subject " + subjectName);
    System.err.println();
  }
  
  private static String concatStrings(Iterable<String> strItr, String sep) {
  	StringBuilder sb = new StringBuilder();
  	Iterator<String> itr = strItr.iterator();
  	while (itr.hasNext()) {
  		sb.append(itr.next());
  		if (itr.hasNext()) {
  			sb.append(sep);
  		}
  	}
  	return sb.toString();
  }
}
