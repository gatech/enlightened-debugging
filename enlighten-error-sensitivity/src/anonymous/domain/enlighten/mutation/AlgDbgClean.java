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


package anonymous.domain.enlighten.mutation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import anonymous.domain.enlighten.files.RemoveDirTreeFileVisitor;

public class AlgDbgClean {
  
  public static void main(String[] args) throws Throwable {
    for (File mutantDataDir : new File("../mutation/out").listFiles()) {
      String mutantName = mutantDataDir.getName();
      try {
        System.out.print("Processing " + mutantName + " ...");
        Path mutantDataPath = mutantDataDir.toPath();
        Path stateCapDir1 = mutantDataPath.resolve("state_capturing");
        Path stateCapDir2 = mutantDataPath.resolve("state_capturing_on_ref");
        RemoveDirTreeFileVisitor removeDir = new RemoveDirTreeFileVisitor();
        if (Files.isDirectory(stateCapDir1)) {
          Files.walkFileTree(stateCapDir1, removeDir);
        }
        if (Files.isDirectory(stateCapDir2)) {
          Files.walkFileTree(stateCapDir2, removeDir);
        }
        Path algDbgDatFile = mutantDataPath.resolve("alg_dbg_result.dat");
        if (Files.isRegularFile(algDbgDatFile)) {
          Files.delete(algDbgDatFile);
        }
        System.out.println("Done.");
      } catch (Throwable ex) {
        System.err.println("\nFailed on " + mutantName);
      }
    }
  }

}
