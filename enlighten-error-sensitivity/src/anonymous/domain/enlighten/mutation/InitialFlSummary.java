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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InitialFlSummary {
  
  public static void main(String[] args) throws Throwable {
    Pattern flLinePattern = Pattern.compile("Rank of the fault: \\d+ / (\\d+)");
    Path dataRoot = Paths.get("../mutation/out_full_technique");
    Path mutantsListFile = Paths.get("../mutation/mutants_list.txt");
    List<String> mutantIds = Files.readAllLines(mutantsListFile);
    StringBuilder reportContent = new StringBuilder();
    reportContent.append("Subject\tInitial Fault Rank\n");
    List<String> initialPerfectFLSubjects = new ArrayList<>();
    for (String mutantIdStr : mutantIds) {
      reportContent.append(mutantIdStr);
      reportContent.append('\t');
      Path experimentLog = 
          dataRoot.resolve(mutantIdStr).resolve("feedback_directed_fl_experiment.log");
      if (!Files.exists(experimentLog)) {
        reportContent.append("(Log file not found)\n");
        continue;
      }
      int initialWorstRank = 0;
      List<String> logLines = Files.readAllLines(experimentLog);
      for (String logLine : logLines) {
        Matcher matcher = flLinePattern.matcher(logLine);
        if (matcher.find()) {
          initialWorstRank = Integer.parseInt(matcher.group(1));
          break;
        }
      }
      if (initialWorstRank == 0) {
        reportContent.append("(Log line not found)\n");
      } else {
        reportContent.append(initialWorstRank);
        reportContent.append('\n');
        if (initialWorstRank == 1) {
          initialPerfectFLSubjects.add(mutantIdStr);
        }
      }
    }
    reportContent.append("In total, " + initialPerfectFLSubjects.size() + " subjects have perfect FL initial ranking:\n");
    for (String subjectName : initialPerfectFLSubjects) {
      reportContent.append(subjectName);
      reportContent.append('\n');
    }
    Path reportFile = Paths.get("../mutation/check_initial_fl.txt");
    Files.write(reportFile, reportContent.toString().getBytes());
  }

}
