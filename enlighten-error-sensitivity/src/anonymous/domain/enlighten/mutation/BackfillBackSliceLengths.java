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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.data.FSTSerialization;

public class BackfillBackSliceLengths {
  
  public static void main(String[] args) throws Throwable {
    Map<MutantVersionId, Integer> backtraceLengthMap = new HashMap<>(); 
    Path logFile = Paths.get("../mutation/backtrace_length/run_backtrace_length.log");
    List<String> logLines = Files.readAllLines(logFile);
    for (String logLine : logLines) {
      String[] parts = logLine.split("\t");
      MutantVersionId mutantId = MutantVersionId.parseFromMutantVersionName(parts[0]);
      String result = parts[1];
      int length = Integer.MAX_VALUE;
      if (result.equals("Failed")) {
        length = -1;
      } else if (!result.equals("Unreachable")) {
        length = Integer.parseInt(result);
      }
      backtraceLengthMap.put(mutantId, length);
    }
    Path dataFile = Paths.get("../mutation/backtrace_length/results.dat");
    FSTSerialization.writeObjectTofile(Map.class, dataFile, backtraceLengthMap);
  }
}
