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
import java.util.List;
import java.util.regex.Pattern;

import anonymous.domain.enlighten.subjectmodel.SubjectProgram;

public class ResultVector {

  public static void main(String[] args) throws Throwable {
    String[] subjectNames = { 
        "jtopas-v1-fault2",
        "jtopas-v1-fault5",
        "jtopas-v1-fault6",
        "commons-math-v1-C_AK_1",
        "commons-math-v1-EDI_AK_1",
        "commons-math-v1-F_AK_1",
        "commons-math-v1-M_AK_1",
        "commons-math-v1-VS_AK_1",
        "commons-math-v2-CDI_AK_1",
        "commons-math-v2-F_AK_2",
        "commons-math-v2-MU_AK_1",
        "commons-math-v2-MU_AK_4",
        "commons-math-v2-URSU_AK_1"
        };
    for (String subjectName : subjectNames) {
      SubjectProgram subject = SubjectProgram.openSubjectProgram(
          Paths.get("subjects", subjectName, "faulty"), Paths.get("out", subjectName, "faulty"));
      Path resultFile = subject.getDataDirRoot().resolve("experiment_result.txt");
      List<String> lines = Files.readAllLines(resultFile);
      System.out.println(subjectName + ": ");
      for (int i = 1; i < lines.size(); ++i) {
        String line = lines.get(i);
        String[] fields = line.split(Pattern.quote("\t"));
        int ranking = Integer.parseInt(fields[2]);
        System.out.print("" + ranking + ", ");
      }
      System.out.println();
    }
  }
  
}
