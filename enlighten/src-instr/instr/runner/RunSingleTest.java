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

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import anonymous.domain.enlighten.data.MethodName;

public class RunSingleTest {
  
  public static void main(String[] args) {
    if (args.length != 1) {
      printUsage();
      System.exit(1);
    }
    String testMethodDescription = args[0];
    MethodName testMethodName = MethodName.get(testMethodDescription);
    JUnitCore junit = new JUnitCore();
    try {
      junit.run(Request.method(
          Class.forName(testMethodName.getClassName()), testMethodName.getMethodNameSig()));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.err.println("Usage: RunSingleTest <test method long name>");
  }
}
