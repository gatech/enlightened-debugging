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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StmtProgressionDiagrams {
  
  public static void main(String[] args) throws Throwable{
    String[] subjectNames = { "commons-math-v1-C_AK_1",
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
    for (String subjectName : subjectNames) {
      Path resultFilePath = Paths.get("out", subjectName, "faulty/experiment_result.txt");
      List<Integer> worstStmtRankProgression = new ArrayList<>();
      int largestRank = 0;
      List<String> lines = Files.readAllLines(resultFilePath);
      for (int i = 1; i < lines.size(); ++i) {
        String currentLine = lines.get(i);
        String[] fields = currentLine.split(Pattern.quote("\t"));
        int rank = Integer.parseInt(fields[3]);
        worstStmtRankProgression.add(rank);
        if (rank > largestRank) {
          largestRank = rank;
        }
      }
      int lastRank = worstStmtRankProgression.get(worstStmtRankProgression.size() - 1);
      for (int i = worstStmtRankProgression.size() - 2; i >=0; --i) {
        if (worstStmtRankProgression.get(i) == lastRank) {
          worstStmtRankProgression.remove(i);
        } else {
          break;
        }
      }
      int steps = worstStmtRankProgression.size();
      Path outputFile = Paths.get("papers/R", subjectName + ".r");
      StringBuilder contentBuilder = new StringBuilder();
      contentBuilder.append("## define output file\n");
      contentBuilder.append("pdf(\"" + subjectName + ".pdf\", height=3.5, width=4, family=\"Times\")\n");
      contentBuilder.append("##################\n");
      contentBuilder.append("y0 <- c(");
      for (int i = 0; i < steps - 1; ++i) {
        contentBuilder.append(worstStmtRankProgression.get(i));
        contentBuilder.append(", ");
      }
      contentBuilder.append(worstStmtRankProgression.get(steps - 1));
      contentBuilder.append(")\n");
      contentBuilder.append("##################\n");
      contentBuilder.append("sfun0 <- stepfun(1:" + (steps - 1) + ", y0, f=0)\n");
      contentBuilder.append("# Extend and/or restrict 'viewport':\n");
      contentBuilder.append("plot(sfun0, xlim = c(0," + steps
          + "), ylim = c(0, " + ((int) (1.1 * largestRank)) 
          + "), main = \"\", xlab = \"\", ylab = \"\")\n");
      contentBuilder.append("text(3, 10, \"" + subjectName + "\")\n");
      Files.write(outputFile, contentBuilder.toString().getBytes());
    }
  }

}
