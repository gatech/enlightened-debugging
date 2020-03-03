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


package anonymous.domain.enlighten.exec;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public abstract class ExternalProgramInvocation {
  
  private static String cpStr;
  
  public static String getFrameworkClasspathString() {
    if (cpStr == null) {
    	String classpathStr = System.getProperty("java.class.path");
    	// Converting class path entries into absolute paths since the new process
    	// might run in a different working directory.
    	String[] cpEntries = classpathStr.split(Pattern.quote(File.pathSeparator));
    	StringBuilder buffer = new StringBuilder();
    	for (String cpEntry : cpEntries) {
    		String absCpEntry = Paths.get(cpEntry).toAbsolutePath().toString();
    		buffer.append(absCpEntry);
    		buffer.append(File.pathSeparatorChar);
    	}
      return buffer.toString();
    } else {
      return cpStr;
    }
  }
  
  public static void setFrameworkClasspathString(String frameworkClasspathStr) {
    cpStr = frameworkClasspathStr;
  }

  protected abstract Path getLogFilePath();
  protected abstract Path getWorkingDirectory();
  
  protected ProcessBuilder newOutputRedirectedProcessBuilder() {
    File outputFile = getLogFilePath().toFile();
    return new ProcessBuilder()
        .redirectErrorStream(true)
        .redirectOutput(Redirect.appendTo(outputFile))
        .directory(getWorkingDirectory().toFile());
  }
  
  protected String concatPaths(List<Path> paths, String separator) {
    StringBuilder pathsStrBuilder = new StringBuilder();
    for (Path path : paths) {



      pathsStrBuilder.append(path.toAbsolutePath().toString());

      pathsStrBuilder.append(separator);
    }
    return pathsStrBuilder.toString();
  }
}
