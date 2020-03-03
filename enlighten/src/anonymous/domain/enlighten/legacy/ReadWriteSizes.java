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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadWriteSizes {
  
  public static void main(String[] args) throws Throwable {
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

    List<ReadWriteSize> allRwSizes = new ArrayList<>();
    for (String subjectName : subjectNames) {
      Path summaryFilePath = Paths.get("out", subjectName, "faulty/experiment_result.txt");
      List<String> effectiveLines = new SummaryFile(summaryFilePath).getEffectiveLines();
      List<ReadWriteSize> subjectRwSizes = new ArrayList<>();
      for (int i = 0; i < effectiveLines.size() - 1; ++i) {
        String line = effectiveLines.get(i);
        subjectRwSizes.add(parseLine(line));
      }
      System.out.println(subjectName);
      printRwSizeStats(subjectRwSizes);
      allRwSizes.addAll(subjectRwSizes);
    }
    System.out.println("Combined");
    printRwSizeStats(allRwSizes);
  }
  
  private static void printRwSizeStats(List<ReadWriteSize> sizes) {
    for (ReadWriteSize size : sizes) {
      System.out.println("" + size.memLocsRead + "\t" + size.numObjsRead 
          + "\t" + size.memLocsWrite + "\t" + size.numObjsWrite);
    }
    System.out.println();
    System.out.println();
  }
  
  private static ReadWriteSize parseLine(String line) {
    String[] fields = line.split("\t");
    String readSize = fields[8];
    String writeSize = fields[9];
    ReadWriteSize sizes = new ReadWriteSize();
    Pattern sizePattern = Pattern.compile("(\\d+)\\((\\d+)\\)");
    Matcher readSizeMatcher = sizePattern.matcher(readSize);
    if (readSizeMatcher.matches()) {
      sizes.memLocsRead = Integer.parseInt(readSizeMatcher.group(1));
      sizes.numObjsRead = Integer.parseInt(readSizeMatcher.group(2));
    } else {
      throw new RuntimeException("Bad size format " + readSize);
    }
    Matcher writeSizeMatcher = sizePattern.matcher(writeSize);
    if (writeSizeMatcher.matches()) {
      sizes.memLocsWrite = Integer.parseInt(writeSizeMatcher.group(1));
      sizes.numObjsWrite = Integer.parseInt(writeSizeMatcher.group(2));
    } else {
      throw new RuntimeException("Bad size format " + writeSize);
    }
    return sizes;
  }
  
  private static class ReadWriteSize {
    public int memLocsRead;
    public int numObjsRead;
    public int memLocsWrite;
    public int numObjsWrite;
  }

}
