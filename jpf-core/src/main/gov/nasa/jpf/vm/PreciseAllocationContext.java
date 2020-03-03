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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.OATHash;

import java.util.Arrays;
import java.util.HashMap;


public class PreciseAllocationContext implements AllocationContext {



  static private HashMap<PreciseAllocationContext,PreciseAllocationContext> ccCache = new HashMap<PreciseAllocationContext,PreciseAllocationContext>();
  
  protected ThreadInfo ti;
  protected Instruction[] cc;
  protected int hashCode; 
  


  private static class LookupContext extends PreciseAllocationContext {
    int stackDepth;
    
    LookupContext (){
      cc = new Instruction[64];
    }
    
    @Override
	public int getStackDepth(){
      return stackDepth;
    }    
  }
  
  private static LookupContext lookupContext = new LookupContext();
  
  static boolean init (Config config) {
    ccCache = new HashMap<PreciseAllocationContext,PreciseAllocationContext>();
    return true;
  }
  
  public static synchronized PreciseAllocationContext getSUTExecutionContext (ClassInfo ci, ThreadInfo ti){
    int stackDepth = ti.getStackDepth();
    int h = 0;
    
    lookupContext.ti = ti;
    lookupContext.stackDepth = stackDepth;
    
    h = OATHash.hashMixin(h, ti.getId());
    
    Instruction[] cc = lookupContext.cc;
    if (cc.length < stackDepth){
      cc = new Instruction[stackDepth];
      lookupContext.cc = cc;
    }

    int i=0;
    for (StackFrame f = ti.getTopFrame(); f != null; f = f.getPrevious()){
      Instruction insn = f.getPC();
      cc[i++] = insn;
      h = OATHash.hashMixin(h, insn.hashCode());
    }
    h = OATHash.hashFinalize(h);
    lookupContext.hashCode = h;
    
    PreciseAllocationContext ec = ccCache.get(lookupContext);
    if (ec == null){
      ec = new PreciseAllocationContext(ti, Arrays.copyOf(cc, stackDepth), h);
      ccCache.put(ec, ec);
    }
    
    return ec;
  }
  
  protected PreciseAllocationContext(){

  }
  

  private PreciseAllocationContext (ThreadInfo ti, Instruction[] cc, int hashCode){
    this.ti = ti;
    this.cc = cc;
    this.hashCode = hashCode;
  }
  
  @Override
  public int hashCode(){
    return hashCode;
  }
  
  public int getStackDepth(){
    return cc.length;
  }
    
  @Override
  public boolean equals (Object o){
    if (o == this){ 
      return true;
      
    } else {
      if (o instanceof PreciseAllocationContext){
        PreciseAllocationContext other = (PreciseAllocationContext)o;
        if (hashCode == other.hashCode){ 
          if (ti.getId() == other.ti.getId()) {
            Instruction[] ccOther = other.cc;
            if (cc.length == other.getStackDepth()) {
              for (int i = 0; i < cc.length; i++) {
                if (cc[i] != ccOther[i]) {
                  return false;
                }
              }
              return true;
            }
          }
        }
      }
      
      return false;
    }
  }
  

  @Override
  public AllocationContext extend (ClassInfo ci, int anchor) {
    return new PreciseAllocationContext(ti, cc, OATHash.hash(hashCode, ci.hashCode()));
  }
  
  
  @Override
  public String toString(){
    StringBuffer sb = new StringBuffer();
    sb.append("(tid=");
    sb.append(ti.getId());
    sb.append(",stack=[");
    for (int i=0; i<cc.length; i++){
      if (i>0){
        sb.append(',');
      }
      sb.append(cc[i]);
    }
    sb.append("])");
    return sb.toString();
  }
}
