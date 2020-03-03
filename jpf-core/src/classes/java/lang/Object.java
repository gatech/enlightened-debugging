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


package java.lang;


public class Object {
  public Object() {

  }
  
  public final native Class<?> getClass();
  public native int hashCode();
  
  
  public boolean equals (Object o) {
    return o == this;
  }

  
  public String toString() {

    return null;
  }

  protected native Object clone() throws java.lang.CloneNotSupportedException;
  
  public final native void notify();
  public final native void notifyAll();
  
  public final native void wait (long timeout) throws java.lang.InterruptedException;
  
  public final void wait (long timeout, int nanos) throws java.lang.InterruptedException{

  }
  
  public final void wait() throws java.lang.InterruptedException {

  }
  
  protected void finalize() throws java.lang.Throwable {
    
  }
}
