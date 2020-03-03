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


import sun.misc.SharedSecrets;
import sun.misc.JavaLangAccess;

import gov.nasa.jpf.Config;
import static gov.nasa.jpf.util.OATHash.*;


public class HashedAllocationContext implements AllocationContext {
    
  static final Throwable throwable = new Throwable(); 
    
  static int mixinSUTStack (int h, ThreadInfo ti) {
    h = hashMixin( h, ti.getId());




    
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame = frame.getPrevious() ) {
      if (!(frame instanceof DirectCallStackFrame)) {
        Instruction insn = frame.getPC();
        

        
        h = hashMixin( h, insn.getMethodInfo().getGlobalId()); 
        h = hashMixin( h, insn.getInstructionIndex()); 
        h = hashMixin( h, insn.getByteCode()); 
      }
    }
    
    return h;
  }
  
   
  
   static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
   static final String ENV_CLSNAME = MJIEnv.class.getName();
  






  static int mixinJPFStack (int h) {
    throwable.fillInStackTrace();
    











    
    StackTraceElement e = JLA.getStackTraceElement(throwable, 4); 

    if (e.getClassName() == ENV_CLSNAME && e.getMethodName().startsWith("new")){

      e = JLA.getStackTraceElement(throwable, 5);
    }
          







    h = hashMixin(h, e.getClassName().hashCode());
    h = hashMixin(h, e.getMethodName().hashCode());
    h = hashMixin(h, e.getLineNumber());
    
    return h;
  }
  
  
  
  
  public static AllocationContext getSUTAllocationContext (ClassInfo ci, ThreadInfo ti) {
    int h = 0;
    

    h = hashMixin(h, ci.getUniqueId()); 
    

    h = mixinSUTStack( h, ti);
    

    h = mixinJPFStack( h);
    
    h = hashFinalize(h);
    HashedAllocationContext ctx = new HashedAllocationContext(h);

    return ctx;
  }
  
  
  public static AllocationContext getSystemAllocationContext (ClassInfo ci, ThreadInfo ti, int anchor) {
    int h = 0;
    
    h = hashMixin(h, ci.getUniqueId()); 
    

    h = hashMixin(h, 0x14040118);
    h = hashMixin(h, anchor);
    

    h = mixinJPFStack( h);
    
    h = hashFinalize(h);
    HashedAllocationContext ctx = new HashedAllocationContext(h);

    return ctx;
  }

  public static boolean init (Config conf) {

    return true;
  }
  

  

  protected final int id;

  

  
  protected HashedAllocationContext (int id) {
    this.id = id;
  }
  
  @Override
  public boolean equals (Object o) {
    if (o instanceof HashedAllocationContext) {
      HashedAllocationContext other = (HashedAllocationContext)o;
      return id == other.id; 
    }
    
    return false;
  }
  
  
  @Override
  public int hashCode() {
    return id;
  }
  

  @Override
  public AllocationContext extend (ClassInfo ci, int anchor) {

    int h = hashMixin(id, anchor);
    h = hashMixin(h, ci.getUniqueId());
    h = hashFinalize(h);
    
    return new HashedAllocationContext(h);
  }
}
