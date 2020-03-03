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
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anonymous.domain.enlighten.data.FSTSerialization;

public class BackfillPathInfo {
  
  private static Pattern summaryLinePattern = Pattern.compile("(Path-\\d+)\t(\\d\\.\\d+)\t([-\\d]+)");
  private static String summaryEndLinePrefix = "Average number of queries";
  private static String incorrectFeedbackPrefix = "Its value is incorrect";
  private static double wrongFeedbackProb = 0.2;
  
  public static void main(String[] args) throws Throwable {


    File dataRoot = new File("../ErrorSensitivityData");
    for (File subjectDataDir : dataRoot.listFiles()) {
      Path subjectDataPath = subjectDataDir.toPath();
      System.out.println("Processing " + subjectDataDir.getName() + "...");
      try {
        backfillPathInfo(subjectDataPath);
      } catch (Throwable ex) {
        System.err.println("Failed on " + subjectDataDir.getName());
        ex.printStackTrace();
      }
    }
  }
  
  public static void backfillPathInfo(Path dataDir) throws IOException {
    Path summaryFile = dataDir.resolve("wrong_feedback_expr_log.txt");
    List<String> summaryLines = Files.readAllLines(summaryFile);
    Map<String, PathInfo> pathInfoMap = new HashMap<>();
    boolean endLinePresent = false;
    for (String summaryLine : summaryLines) {
      Matcher matcher = summaryLinePattern.matcher(summaryLine);
      if (matcher.find()) {
        String pathId = matcher.group(1);
        double precomputedProb = Double.parseDouble(matcher.group(2));
        int nQueries = Integer.parseInt(matcher.group(3));
        PathInfo pathInfo = new PathInfo();
        pathInfo.pathId = pathId;
        pathInfo.numQueries = nQueries;
        int numIncorrectValues = 0;
        Path pathLogFile = dataDir.resolve("PathLogs").resolve(pathId + ".txt");
        List<String> pathLogLines = Files.readAllLines(pathLogFile);
        for (String pathLogLine : pathLogLines) {
          if (pathLogLine.startsWith(incorrectFeedbackPrefix)) {
            ++numIncorrectValues;
          }
        }
        double wrongFeedbackAccProb = 
            precomputedProb / Math.pow(1 - wrongFeedbackProb, numIncorrectValues);
        double dWrongFeedback = Math.log(wrongFeedbackAccProb) / Math.log(wrongFeedbackProb);
        int numWrongFeedback = (int) Math.round(dWrongFeedback);
        if (Math.abs(dWrongFeedback - numWrongFeedback) > 0.2) {
          System.err.println("Warning: infered # of wrong answers diverted from nearest integer too much.");
        }
        pathInfo.numIncorrectValues = numIncorrectValues;
        pathInfo.numWrongFeedback = numWrongFeedback;
        if (nQueries != -1 && 
            !doubleEquals(recoverPathProb(pathInfo, wrongFeedbackProb), precomputedProb)) {
          throw new RuntimeException(pathId + ": Path probability recovery failed.");
        }
        pathInfoMap.put(pathId, pathInfo);
      }
      if (summaryLine.startsWith(summaryEndLinePrefix)) {
        endLinePresent = true;
      }
    }
    if (!endLinePresent) {
      throw new RuntimeException("Exploration aborted.");
    }
    Path pathInfoFile = dataDir.resolve("path_info.dat");
    FSTSerialization.writeObjectTofile(Map.class, pathInfoFile, pathInfoMap);
  }
  
  
  
  private static double recoverPathProb(PathInfo path, double wrongFeedbackProb) {
    return Math.pow(1 - wrongFeedbackProb, path.numIncorrectValues) 
        * Math.pow(wrongFeedbackProb, path.numWrongFeedback);
  }
  
  private static boolean doubleEquals(double d1, double d2) {
    return Math.abs(d1 - d2) < 0.000001;
  }

  public static class PathInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public String pathId;
    public int numQueries;
    public int numIncorrectValues;
    public int numWrongFeedback;
  }
}
