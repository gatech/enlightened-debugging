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


package anonymous.domain.enlighten.wrongfeedback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import anonymous.domain.enlighten.data.FSTSerialization;
import anonymous.domain.enlighten.wrongfeedback.BackfillPathInfo.PathInfo;

public class PrintExpr2Summary {
  
  public static void main(String[] args) throws Throwable {
    List<Double> wrongFeedbackProbParams = new ArrayList<>();
    wrongFeedbackProbParams.add(0.05);
    wrongFeedbackProbParams.add(0.10);
    wrongFeedbackProbParams.add(0.15);
    wrongFeedbackProbParams.add(0.20);
    wrongFeedbackProbParams.add(0.25);
    wrongFeedbackProbParams.add(0.30);
    Path dataRoot = Paths.get("../ErrorSensitivityData");
    StringBuilder successRateSummary = new StringBuilder();
    StringBuilder nQueriesSummary = new StringBuilder();
    printTitle(successRateSummary, wrongFeedbackProbParams);
    printTitle(nQueriesSummary, wrongFeedbackProbParams);
    for (File subjectDataDir : dataRoot.toFile().listFiles()) {
      String subjectName = subjectDataDir.getName();
      Path dataFile = subjectDataDir.toPath().resolve("path_info.dat");
      if (!Files.exists(dataFile)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, PathInfo> pathInfoMap = 
          FSTSerialization.readObjectFromFile(HashMap.class, dataFile);
      int basePathNumQueries = pathInfoMap.get("Path-0").numQueries;
      if (basePathNumQueries == -1) {

        continue;
      }
      successRateSummary.append(subjectName);
      successRateSummary.append('\t');
      successRateSummary.append("1.0000");
      nQueriesSummary.append(subjectName);
      nQueriesSummary.append('\t');
      nQueriesSummary.append(String.format("%d", basePathNumQueries));
      for (double wrongFeedbackProb : wrongFeedbackProbParams) {
        double totalPercentAccounted = 0;
        double successRate = 0;
        double avgNumQueries = 0;
        for (PathInfo pathInfo : pathInfoMap.values()) {
          double pathProb = getPathProbability(pathInfo, wrongFeedbackProb);
          totalPercentAccounted += pathProb;
          if (pathInfo.numQueries != -1) {
            successRate += pathProb;
            avgNumQueries += pathInfo.numQueries * pathProb;
          }
        }
        successRate /= totalPercentAccounted;
        avgNumQueries /= totalPercentAccounted;
        successRateSummary.append('\t');
        successRateSummary.append(String.format("%.4f", successRate));
        nQueriesSummary.append('\t');
        nQueriesSummary.append(String.format("%.4f", avgNumQueries));
      }
      successRateSummary.append('\n');
      nQueriesSummary.append('\n');
    }
    Path successRateReportPath = dataRoot.resolve("success_rate_report.txt");
    Path avgNumQueriesReportPath = dataRoot.resolve("avg_num_queries_report.txt");
    Files.write(successRateReportPath, successRateSummary.toString().getBytes());
    Files.write(avgNumQueriesReportPath, nQueriesSummary.toString().getBytes());
  }
  
  private static void printTitle(StringBuilder buffer, List<Double> probs) {
    buffer.append("Subject");
    buffer.append('\t');
    buffer.append("0.00");
    for (double prob : probs) {
      buffer.append('\t');
      buffer.append(String.format("%.2f", prob));
    }
    buffer.append('\n');
  }
  
  private static double getPathProbability(PathInfo path, double wrongFeedbackProb) {
    return Math.pow(1 - wrongFeedbackProb, path.numIncorrectValues) 
        * Math.pow(wrongFeedbackProb, path.numWrongFeedback);
  }
}
