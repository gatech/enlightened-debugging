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



package gov.nasa.jpf.vm.serialize;

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;


public class AdaptiveSerializer extends CFSerializer {

  boolean traverseObjects;
  boolean isSchedulingPoint;

  @Override
  protected void initReferenceQueue() {
    super.initReferenceQueue();
    traverseObjects = true;

    ChoiceGenerator<?> nextCg = vm.getNextChoiceGenerator();
    isSchedulingPoint = (nextCg != null) && nextCg.isSchedulingPoint();
  }

  @Override
  protected void queueReference(ElementInfo ei){
    if (traverseObjects){
      refQueue.add(ei);
    }
  }

  @Override
  protected void processReferenceQueue() {
    if (isSchedulingPoint){
      traverseObjects = false;
    }
    refQueue.process(this);
  }


  @Override
  protected void serializeClassLoaders(){


    if (!isSchedulingPoint){



      super.serializeClassLoaders();
    }
  }
}
