/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */



package gov.nasa.jpf.util;

import gov.nasa.jpf.vm.ClassInfo;


public class ClassInfoFilter {


  protected StringSetMatcher includes;  
  protected StringSetMatcher excludes;  


  ClassInfo ciLeaf;
  ClassInfo ciRoot;

  public ClassInfoFilter (String[] includeCls, String[] excludeCls,
                                   ClassInfo rootCls, ClassInfo leafCls) {
    includes = StringSetMatcher.getNonEmpty(includeCls);
    excludes = StringSetMatcher.getNonEmpty(excludeCls);

    ciRoot = rootCls;
    ciLeaf = leafCls;
  }


  public boolean isPassing (ClassInfo ci){
    if (ci == null){




      return true;

    } else {
      String clsName = ci.getName();

      if (StringSetMatcher.isMatch(clsName, includes, excludes)){
        if (ciLeaf == null || ciLeaf.isInstanceOf(ci)){
          if (ciRoot == null || ci.isInstanceOf(ciRoot)){
            return true;
          }
        }
      }
    }

    return false;
  }

}
