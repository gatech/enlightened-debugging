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


package anonymous.domain.enlighten.legacy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ExperimentResultSummary {
  private static String[] subjectNames = { "commons-math-v1-C_AK_1",
      "commons-math-v1-EDI_AK_1",
      "commons-math-v1-F_AK_1",
      "commons-math-v1-M_AK_1",
      "commons-math-v1-VS_AK_1",
      "commons-math-v2-CDI_AK_1",
      "commons-math-v2-F_AK_2",
      "commons-math-v2-MU_AK_1",
      "commons-math-v2-MU_AK_4",
      "commons-math-v2-URSU_AK_1",
      "jsoup-1_3_4-3",
      "jsoup-1_4_2-2",
      "Jsoup-1_5_2-2",
      "Jsoup-1_5_2-5",
      "jsoup-1_6_1-1CR1",
      "jsoup-1_6_3-3",
      "jtopas-v1-fault2",
      "jtopas-v1-fault6",
      "Lang-10",
      "Lang-16",
      "Lang-24",
      "Lang-26",
      "Lang-39",
      "Lang-6",
      "Lang-9",
      "xml-security-v1-CN2_AK_2",
      "xml-security-v2-C2E_AK_1",
  };
  
  public static void main(String[] args) {
    for (String subjectName : subjectNames) {
      try {
        Path summaryFilePath = Paths.get("out", subjectName, "faulty/experiment_result.txt");
        List<String> effectiveLines = new SummaryFile(summaryFilePath).getEffectiveLines();
        System.out.println("Subject name: " + subjectName);
        System.out.println("#Queries till faulty invocation: " + (effectiveLines.size() - 1));
        String lastLine = effectiveLines.get(effectiveLines.size() - 1);
        String[] fields = lastLine.split("\t");
        int finalBestRank = Integer.parseInt(fields[2]);
        int finalWorstRank = Integer.parseInt(fields[3]);
        System.out.println("Stmt final rank: " + finalBestRank + "/" + finalWorstRank);
        System.out.println();
      } catch (Throwable ex) {
        ex.printStackTrace();
      }
    }
  }
}
