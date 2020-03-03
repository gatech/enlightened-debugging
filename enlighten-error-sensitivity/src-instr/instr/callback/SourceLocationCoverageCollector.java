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


package instr.callback;

import java.util.ArrayList;
import java.util.List;

import anonymous.domain.enlighten.data.SourceLocation;
import instr.staticinfo.SourceLocationDB;

public class SourceLocationCoverageCollector {
  
  private static final int RECORD_BLOCK_BITS = 16;
  
  private static final int RECORD_BLOCK_SIZE = 1 << RECORD_BLOCK_BITS; 
  private static final int RECORD_INDEX_MASK = RECORD_BLOCK_SIZE - 1;

  private static final int NUM_BLOCKS = 1 << (32 - RECORD_BLOCK_BITS);
  
  private static final int DATA_UNIT_ADDR_BITS = 5; 
  private static final int DATA_UNIT_ADDR_MASK = (1 << DATA_UNIT_ADDR_BITS) - 1;
  private static final int RECORD_DATA_ARRAY_SIZE = RECORD_BLOCK_SIZE >> DATA_UNIT_ADDR_BITS;

  private static int[][] recordBlocks = null;
  
  public static void executingSourceLocation(int sourceLocationId) {
    if (recordBlocks == null) {
      return;
    }
    int blockIndex = sourceLocationId >>> RECORD_BLOCK_BITS;
    int recordIndex = sourceLocationId & RECORD_INDEX_MASK;
    int recordDataArrayIndex = recordIndex >>> DATA_UNIT_ADDR_BITS;
    int dataUnitOffset = recordIndex & DATA_UNIT_ADDR_MASK;
    int[] recordBlock = recordBlocks[blockIndex];
    if (recordBlock == null) {
      recordBlock = createRecordDataBlock();
      recordBlocks[blockIndex] = recordBlock;
    }
    recordBlock[recordDataArrayIndex] |= (1 << dataUnitOffset);
  }
  
  public static List<SourceLocation> getCoveredSourceLocations() {
    List<SourceLocation> coveredLocs = new ArrayList<>();
    for (int blockIndex = 0; blockIndex < NUM_BLOCKS; ++blockIndex) {
      if (recordBlocks[blockIndex] == null) {
        continue;
      }
      for (int recordDataArrayIndex = 0; recordDataArrayIndex < RECORD_DATA_ARRAY_SIZE; 
          ++recordDataArrayIndex) {
        for (int dataUnitOffset = 0; dataUnitOffset < (1 << DATA_UNIT_ADDR_BITS); 
            ++dataUnitOffset) {
          if ((recordBlocks[blockIndex][recordDataArrayIndex] & (1 << dataUnitOffset)) != 0) {
            int sourceLocationId = (blockIndex << RECORD_BLOCK_BITS)
                | (recordDataArrayIndex << DATA_UNIT_ADDR_BITS) | dataUnitOffset;
            coveredLocs.add(SourceLocationDB.getSourceLocationById(sourceLocationId));
          }
        }
      }
    }
    return coveredLocs;
  }
  
  public static void resetCoverage() {
    recordBlocks = new int[NUM_BLOCKS][];
  }
  
  private static int[] createRecordDataBlock() {
    return new int[RECORD_DATA_ARRAY_SIZE];
  }
}
