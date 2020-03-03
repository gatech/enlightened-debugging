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
import java.util.List;

public class PrintExpr1Summary {
  
  public static void main(String[] args) throws Throwable {
    Path dataRoot = Paths.get("../ErrorSensitivityData");
    System.out.println("Subject\tPercentage explored\tSuccess rate\tAvg # Queries");
    for (File subjectDir : dataRoot.toFile().listFiles()) {
      String subjectName = subjectDir.getName();
      Path dataFile = subjectDir.toPath().resolve("wrong_feedback_expr_log.txt");
      List<String> resultLines = Files.readAllLines(dataFile);
      double percentageExplored = 0;
      double successRate = 0;
      double avgNumQueries = 0;
      for (String resultLine : resultLines) {
        int separatorIndex = resultLine.indexOf(":");
        if (separatorIndex >= 0) {
          double dataValue = Double.parseDouble(resultLine.substring(separatorIndex + 2));
          if (resultLine.startsWith("Percentage")) {
            percentageExplored = dataValue;
          } else if (resultLine.startsWith("Success rate")) {
            successRate = dataValue;
          } else if (resultLine.startsWith("Average")) {
            avgNumQueries = dataValue;
          }
        }
      }
      System.out.println(subjectName + "\t" + formatDouble(percentageExplored) 
      + "\t" + formatDouble(successRate) + "\t" + formatDouble(avgNumQueries));
    }
  }

  private static String formatDouble(double value) {
    return String.format("%.4f", value);
  }
}
