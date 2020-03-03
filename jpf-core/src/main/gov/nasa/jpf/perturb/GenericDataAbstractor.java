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



package gov.nasa.jpf.perturb;

import java.util.Random;
import java.util.Vector;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.IntChoiceGenerator;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.choice.IntChoiceFromSet;



public class GenericDataAbstractor implements OperandPerturbator {


  public class Valuation {
    protected int valuation[] = null;

    public Valuation(int size) {
      valuation = new int[size];
    }

    public Valuation(Valuation seedValuation) {
      valuation = seedValuation.valuation.clone();
    }
    public Valuation(Valuation val, int size) {
      valuation = new int[size];
      int[] valuationArray = val.getValuation();
      for (int i = 0; i < valuationArray.length; i++)
        valuation[i] = valuationArray[i];
    }
    public int[] getValuation() {
      return valuation;
    }
    public void add(int index, int element) {
      valuation[index] = element;
    }
  }

  static long seed = 5;

  protected MethodInfo mi;
  protected StackFrame stackFrame;
  protected int nParams;
  protected byte[] paramTypes = null;
  protected String[] paramTypeNames = null;
  protected String[] paramNames = null;
  protected Vector<Valuation> valuations = new Vector<Valuation>();
  protected int choices;
  protected int operandSize;
  protected Valuation valuation = null;
  protected boolean isStatic;
  protected Random randomizer = new Random(seed);

  public GenericDataAbstractor (Config conf, String keyPrefix){


    mi = null;
    choices = 0;
  }





  public void setMethodInfo(MethodInfo m, StackFrame frame) {
    if (mi != null) 
      return;

    mi = m;
    stackFrame = frame;

    paramTypes = mi.getArgumentTypes();
    paramTypeNames = mi.getArgumentTypeNames();
    nParams = paramTypes.length;
    isStatic = mi.isStatic();


    operandSize = 0;
    for (byte type : paramTypes) {
      if (type == Types.T_LONG || type == Types.T_DOUBLE)
        operandSize += 2;
      else
        operandSize++;
    }




    paramNames = new String[nParams];
    if (nParams != 0) {
    	String[] localVars = mi.getLocalVariableNames();
    	for (int i = 0; i < nParams; i++) {
    		paramNames[i] = isStatic? localVars[i] : localVars[i + 1];
    	}
    }







    valuation = new Valuation(operandSize);
    valuations.add(valuation);
    populateValuations(frame, 0, 0);


    choices = valuations.size() - 1;
  }


  public int[] populateBoolean(MethodInfo mi, String name) {
    int[] bVec = {0 , 1 };

    return bVec;
  }


  public int[] populateChar(MethodInfo mi, String name) {
    int[] iVec = {Math.abs(randomizer.nextInt() % 255), 0, Math.abs(randomizer.nextInt() % 255)};

    return iVec;
  }


  public int[] populateByte(MethodInfo mi, String name) {
    int[] iVec = {Math.abs(randomizer.nextInt() % 128), 0, -1 * Math.abs(randomizer.nextInt() % 127)};

    return iVec;
  }


  public int[] populateInt(MethodInfo mi, String name) {
    int[] iVec = {Math.abs(randomizer.nextInt() % 100), 0, -1 * Math.abs(randomizer.nextInt() % 100)};

    return iVec;
  }


  public int[] populateShort(MethodInfo mi, String name) {
    return populateInt(mi, name);
  }


  public int[] populateLong(MethodInfo mi, String name) {
    long[] lVec = {Math.abs(randomizer.nextLong() % 100), 0, -1 * Math.abs(randomizer.nextLong() % 100)};
    int[] iVec = new int[lVec.length * 2];

    int i = 0;
    for (long l : lVec) {
      iVec[i++] = Types.hiLong(l);
      iVec[i++] = Types.loLong(l);
    }
    return iVec;
  }


  public int[] populateFloat(MethodInfo mi, String name) {
    int[] fVec = {Float.floatToIntBits(randomizer.nextFloat()), 0, 
        -1 * Float.floatToIntBits(randomizer.nextFloat())};

    return fVec;
  }


  public int[] populateDouble(MethodInfo mi, String name) {
    double[] dVec = {-1.414, 0.0, 3.141};
    int[] iVec = new int[dVec.length * 2];

    int i = 0;
    for (double d : dVec) {
      iVec[i++] = Types.hiDouble(d);
      iVec[i++] = Types.loDouble(d);
    }
    return iVec;
  }

  public void populateValuations(StackFrame frame, int paramIndex, int dataIndex) {
    if (paramIndex == nParams) {  		




      valuation = new Valuation(valuation);
      valuations.add(valuation);
    } else {
      switch (paramTypes[paramIndex]) {
        case Types.T_ARRAY:
          populateValuations(frame, paramIndex + 1, dataIndex + 1);
          break;
        case Types.T_BOOLEAN:
          int[] bVec = populateBoolean(mi, paramNames[paramIndex]);
          for (int i = 0; i < bVec.length; i++) {
            valuation.add(dataIndex, bVec[i]);
            populateValuations(frame, paramIndex + 1, dataIndex + 1);
          }
          break;
        case Types.T_FLOAT:
          int[] fVec = populateFloat(mi, paramNames[paramIndex]);
          for (int i = 0; i < fVec.length; i++) {
            valuation.add(dataIndex, fVec[i]);
            populateValuations(frame, paramIndex + 1, dataIndex + 1);
          }
          break;
        case Types.T_CHAR:
          int[] iVec = populateChar(mi, paramNames[paramIndex]);
          for (int i = 0; i < iVec.length; i++) {
            valuation.add(dataIndex, iVec[i]);
            populateValuations(frame, paramIndex + 1, dataIndex + 1);
          }
          break;
        case Types.T_BYTE:
          iVec = populateByte(mi, paramNames[paramIndex]);
          for (int i = 0; i < iVec.length; i++) {
            valuation.add(dataIndex, iVec[i]);
            populateValuations(frame, paramIndex + 1, dataIndex + 1);
          }
          break;
        case Types.T_INT:
          iVec = populateInt(mi, paramNames[paramIndex]);
          for (int i = 0; i < iVec.length; i++) {
            valuation.add(dataIndex, iVec[i]);
            populateValuations(frame, paramIndex + 1, dataIndex + 1);
          }
          break;
        case Types.T_SHORT:
          iVec = populateShort(mi, paramNames[paramIndex]);
          for (int i = 0; i < iVec.length; i++) {
            valuation.add(dataIndex, iVec[i]);
            populateValuations(frame, paramIndex + 1, dataIndex + 1);
          }
          break;
        case Types.T_LONG:
          int[] lVec = populateLong(mi, paramNames[paramIndex]);
          int i = 0;
          while (i < lVec.length) {
            valuation.add(dataIndex, lVec[i++]);
            valuation.add(dataIndex + 1, lVec[i++]);
            populateValuations(frame, paramIndex + 1, dataIndex + 2);
          }
          break;
        case Types.T_DOUBLE:
          int[] dVec = populateDouble(mi, paramNames[paramIndex]);
          i = 0;
          while (i < dVec.length) {
            valuation.add(dataIndex, dVec[i++]);
            valuation.add(dataIndex + 1, dVec[i++]);
            populateValuations(frame, paramIndex + 1, dataIndex + 2);
          }
          break;
      }
    }
  }

  @Override
  public ChoiceGenerator<?> createChoiceGenerator (String id, StackFrame frame, Object refObject) {


    assert refObject instanceof MethodInfo : "wrong refObject type for GenericDataAbstractor: " + 
    refObject.getClass().getName();

  setMethodInfo((MethodInfo)refObject, frame);

  if (choices > 0) {



    int[] indices = new int[choices];
    for (int i = 0; i < choices; i++) {
      indices[i] = i;
    }
    return new IntChoiceFromSet(id, indices);
  } else
    return null;
  }

  @Override
  public boolean perturb(ChoiceGenerator<?>cg, StackFrame frame) {
    assert cg instanceof IntChoiceGenerator : "wrong choice generator type for GenericDataAbstractor: " + cg.getClass().getName();

  int choice = ((IntChoiceGenerator)cg).getNextChoice();
  Valuation valuation = valuations.get(choice);



  int val = 0;

  int top = frame.getTopPos();
  int stackIdx = frame.getLocalVariableCount() + ((isStatic)? 0 : 1);
  int argSize = paramTypes.length;

  for (int j = 0; j < argSize; j++) { 
    if (!frame.isOperandRef(top - stackIdx)) {
      frame.setOperand(top - stackIdx++, valuation.getValuation()[val++], false);
      if (paramTypes[j] == Types.T_LONG || paramTypes[j] == Types.T_DOUBLE) {
        frame.setOperand(top - stackIdx++, valuation.getValuation()[val++], false);
      }
    }
  }

  return cg.hasMoreChoices();
  }

  @Override
  public Class<? extends ChoiceGenerator<?>> getChoiceGeneratorType(){
    return IntChoiceFromSet.class;
  }
}
