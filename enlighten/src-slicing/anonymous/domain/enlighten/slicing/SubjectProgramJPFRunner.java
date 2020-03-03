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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import anonymous.domain.enlighten.data.TestName;
import anonymous.domain.enlighten.deptrack.DependencyCreationListener;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.InstructionDependencyListener;
import anonymous.domain.enlighten.exec.ExternalProgramInvocation;
import anonymous.domain.enlighten.subjectmodel.SubjectProgram;
import anonymous.domain.enlighten.util.StringUtils;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.vm.DependencyTrackingInstruction;
import gov.nasa.jpf.vm.VMListener;

public class SubjectProgramJPFRunner {
  
  private static PrintStream nullPrintStream = new PrintStream(NullOutputStream.instance());
  
  private SubjectProgram subjectProgram;
  private VMListener jpfListener;
  
  private boolean discardOutput = true;
  private PrintStream savedStdOut;
  private PrintStream savedStdErr;

  public SubjectProgramJPFRunner(SubjectProgram subject) {
    subjectProgram = subject;
  }
  
  public void setJpfVMListener(VMListener jpfListener) {
    this.jpfListener = jpfListener;
  }
  
  public void setDiscardJpfOutput(boolean discard) {
    discardOutput = discard;
  }
  
  public void runTestMethod(TestName testName) {
    runTestMethod(testName.getTestMethodLongName());
  }
  
  public void runTestMethod(String testMethodLongName) {
    if (discardOutput) {
      savedStdOut = System.out;
      savedStdErr = System.err;
      System.setOut(nullPrintStream);
      System.setErr(nullPrintStream);
    }
    JPF jpf = null;
    try {
	    List<String> configs = getSubjectSpecificJPFConfigs();
	    configs.add("+target=anonymous.domain.enlighten.slicing.TestMethodRunner");
	    configs.add("+target.args=" + testMethodLongName);
	    jpf = createJPF(configs);
	    if (jpfListener instanceof InstructionDependencyListener) {
	      DependencyTrackingInstruction.addInstructionDependencyListener(
	          (InstructionDependencyListener) jpfListener);
	    }
	    if (jpfListener instanceof DependencyCreationListener) {
	      DynamicDependency.addDependencyCreationListener(
	          (DependencyCreationListener) jpfListener);
	    }
    } catch (Throwable ex) {
    	if (discardOutput) {
        System.setOut(savedStdOut);
        System.setErr(savedStdErr);
      }
    	System.err.println(
    			"Cannot initialize JPF. Did you set up the site.properties file correctly?");
    	System.err.println("Execution aborted");
    	System.exit(1);
    }
    try {
      jpf.run();
    } catch (Throwable ex) {
    	PrintStream errPrinter = null;
    	if (discardOutput) {
    		errPrinter = savedStdErr;
    	} else {
    		errPrinter = System.err;
    	}
    	errPrinter.println("Failed to execute test " + testMethodLongName + " on JPF");
    	ex.printStackTrace(errPrinter);
    } finally {
      if (discardOutput) {
        System.setOut(savedStdOut);
        System.setErr(savedStdErr);
      }
      if (jpfListener instanceof InstructionDependencyListener) {
        DependencyTrackingInstruction.removeInstructionDependencyListener(
            (InstructionDependencyListener) jpfListener);
      }
      
      if (jpfListener instanceof DependencyCreationListener) {
        DynamicDependency.removeDependencyCreationListener(
            (DependencyCreationListener) jpfListener);
      }
    }
  }
  
  private JPF createJPF(List<String> configs) {
    Config conf = JPF.createConfig(configs.toArray(new String[0]));
    JPF jpf = new JPF(conf);
    if (jpfListener != null) {
      jpf.addVMListener(jpfListener);
    }
    return jpf;
  }
  
  private List<String> getSubjectSpecificJPFConfigs() {
    List<String> strConfigs = new ArrayList<>();
    strConfigs.add("+listener=gov.nasa.jpf.vm.JVMForwarder");
    strConfigs.add("+search.class=gov.nasa.jpf.search.PathSearch");
    strConfigs.add("+nhandler.delegateUnhandledNative = true");
    List<Path> classpathEntries = new ArrayList<>();
    classpathEntries.addAll(subjectProgram.getAppSourceDirs());
    classpathEntries.addAll(subjectProgram.getTestSourceDirs());
    classpathEntries.addAll(subjectProgram.getLibPaths());
    List<String> classpathEntryStrs = classpathEntries.stream()
        .map(path -> { return path.toAbsolutePath().toString(); })
        .collect(Collectors.toList());
    String fwCpStr = ExternalProgramInvocation.getFrameworkClasspathString();
    String platformPathSeperator = System.getProperty("path.separator");
    if (!platformPathSeperator.equals(";") && !platformPathSeperator.equals(",")) {
      fwCpStr = fwCpStr.replaceAll(platformPathSeperator, ",");
    }
    classpathEntryStrs.add(fwCpStr);
    String classpathStr = StringUtils.concat(classpathEntryStrs, ",");
    strConfigs.add("+classpath=" + classpathStr);
    strConfigs.add("+native_classpath=" + classpathStr); 
    return strConfigs;
  }
  
  private static class NullOutputStream extends OutputStream {
    
    private static final NullOutputStream instance = new NullOutputStream();
    
    public static NullOutputStream instance() {
      return instance;
    }
    
    private NullOutputStream() {}
    
    @Override
    public void write(int b) throws IOException {}
  }
}
