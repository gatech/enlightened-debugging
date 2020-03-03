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


package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.HashData;



public class AtomicData {
  
  public MethodInfo currentMethod;

  
  public int line;

  
  public boolean inSameMethod;

  @Override
  public Object clone () {
    AtomicData a = new AtomicData();

    a.currentMethod = currentMethod;
    a.line = line;
    a.inSameMethod = inSameMethod;

    return a;
  }

  @Override
  public boolean equals (Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof AtomicData)) {
      return false;
    }

    AtomicData a = (AtomicData) o;

    if (currentMethod != a.currentMethod) {
      return false;
    }

    if (line != a.line) {
      return false;
    }

    if (inSameMethod != a.inSameMethod) {
      return false;
    }

    return true;
  }

  
  public void hash (HashData hd) {
    hd.add(line);
    hd.add(inSameMethod ? 1 : 0);
  }

  
  @Override
  public int hashCode () {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }
}
