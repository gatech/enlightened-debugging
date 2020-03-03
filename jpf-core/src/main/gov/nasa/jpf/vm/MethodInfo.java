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
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.LocationSpec;
import gov.nasa.jpf.vm.bytecode.ReturnInstruction;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class MethodInfo extends InfoObject implements GenericSignatureHolder  {

  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.vm.MethodInfo");
  
  static final int INIT_MTH_SIZE = 4096;
  protected static final ArrayList<MethodInfo> mthTable = new ArrayList<MethodInfo>(INIT_MTH_SIZE);
  

  static final int DIRECT_CALL = -1;

  static final LocalVarInfo[] EMPTY = new LocalVarInfo[0];
  
  static final int[] EMPTY_INT = new int[0];
  
  
  protected static boolean warnedLocalInfo = false;
  

  static final int  EXEC_ATOMIC = 0x10000; 
  static final int  EXEC_HIDDEN = 0x20000; 
  static final int  FIREWALL    = 0x40000; 

  static final int  IS_CLINIT   = 0x80000;
  static final int  IS_INIT     = 0x100000;
  
  static final int  IS_REFLECTION = 0x200000; 
  static final int  IS_DIRECT_CALL = 0x400000;
  
  
  protected int globalId = -1;

  
  protected String uniqueName;

  
  protected String name;

  
  protected String signature;

  
  protected String genericSignature;

  
  protected ClassInfo ci;

  
  protected Instruction[] code;

  
  protected ExceptionHandler[] exceptionHandlers;

  
  protected String[] thrownExceptionClassNames;

  
  protected int[] lineNumbers;
  
  
  protected LocalVarInfo localVars[] = null;

  
  protected int maxLocals;

  
  protected int maxStack;

  
  AnnotationInfo[][] parameterAnnotations;


  
  
  protected int modifiers;
   
  
  protected int attributes;
      




  
  protected int argSize = -1;

  
  protected int nArgs = -1;

  
  protected byte returnType = -1;

  
  protected int retSize = -1;

  
  protected byte[] argTypes = null;
  
  static boolean init (Config config) {
    mthTable.clear();    
    return true;
  }

  public static MethodInfo getMethodInfo (int globalId){
    if (globalId >=0 && globalId <mthTable.size()){
      return mthTable.get(globalId);
    } else {
      return null;
    }
  }
  
  public static MethodInfo create (String name, String signature, int modifiers){
    return new MethodInfo( name, signature, modifiers);
  }
  
  public static MethodInfo create (ClassInfo ci, String name, String signature, int modifiers){
    return new MethodInfo( ci, name, signature, modifiers);
  }
  
  static MethodInfo create (ClassInfo ci, String name, String signature, int modifiers, int maxLocals, int maxStack){
    return new MethodInfo( ci, name, signature, modifiers, maxLocals, maxStack);
  }

  
  public MethodInfo (MethodInfo callee, int nLocals, int nOperands) {
    globalId = DIRECT_CALL;

    
    ci = callee.ci;
    name = "[" + callee.name + ']'; 
    signature = "()V";
    genericSignature = "";
    maxLocals = nLocals;
    maxStack = nOperands;  
    localVars = EMPTY;
    lineNumbers = null;
    exceptionHandlers = null;
    thrownExceptionClassNames = null;
    uniqueName = name;
    

    ci = callee.ci;
    
    attributes |= IS_DIRECT_CALL;
    modifiers = Modifier.STATIC;   
    

  }
  
  
  public MethodInfo(String name, String signature, int modifiers, int nLocals, int nOperands) {
    this( name, signature, modifiers);
    maxLocals = nLocals;
    maxStack = nOperands;
    localVars = EMPTY;
  }
  
  
  public MethodInfo (MethodInfo mi) {
    globalId = mi.globalId;
    uniqueName = mi.uniqueName;
    name = mi.name;
    signature = mi.signature;
    genericSignature = mi.genericSignature;
    ci = mi.ci;
    modifiers = mi.modifiers;
    attributes = mi.attributes;
    thrownExceptionClassNames = mi.thrownExceptionClassNames;
    parameterAnnotations = mi.parameterAnnotations;

    annotations = mi.annotations;
    
    localVars = null; 

  }
  

  public MethodInfo (ClassInfo ci, String name, String signature, int modifiers, int maxLocals, int maxStack){
    this.ci = ci;
    this.name = name;
    this.signature = signature;
    this.uniqueName = getUniqueName(name, signature);
    this.genericSignature = "";
    this.maxLocals = maxLocals;
    this.maxStack = maxStack;
    this.modifiers = modifiers;

    this.lineNumbers = null;
    this.exceptionHandlers = null;
    this.thrownExceptionClassNames = null;


    if (ci != null){
      if (name.equals("<init>")) {
        attributes |= IS_INIT;
      } else if (name.equals("<clinit>")) {
        this.modifiers |= Modifier.SYNCHRONIZED;
        attributes |= IS_CLINIT | FIREWALL;
      }
      if (ci.isInterface()) { 
        this.modifiers |= Modifier.PUBLIC;
      }
    }

    this.globalId = mthTable.size();
    mthTable.add(this);
  }

  
  public MethodInfo (String name, String signature, int modifiers){
    this.name = name;
    this.signature = signature;
    this.modifiers = modifiers;
    this.uniqueName = getUniqueName(name, signature);
    this.genericSignature = "";

    if (name.equals("<init>")) {
      attributes |= IS_INIT;
    } else if (name.equals("<clinit>")) {


      this.modifiers |= Modifier.SYNCHRONIZED;
      attributes |= IS_CLINIT | FIREWALL;
    }
    
    this.globalId = mthTable.size();
    mthTable.add(this);    
  }

  public MethodInfo (ClassInfo ci, String name, String signature, int modifiers){
    this(name, signature, modifiers);
    
    this.ci = ci;
  }
  

  
  public void linkToClass (ClassInfo ci){
    this.ci = ci;
    
    if (ci.isInterface()) { 
      this.modifiers |= Modifier.PUBLIC;
    }
  }
  
  public void setMaxLocals(int maxLocals){
    this.maxLocals = maxLocals;
  }

  public void setMaxStack(int maxStack){
    this.maxStack = maxStack;
  }
  
  public void setCode (Instruction[] code){
    for (int i=0; i<code.length; i++){
      code[i].setMethodInfo(this);
    }
    this.code = code;
  }
  
  
  public boolean hasParameterAnnotations() {
    return (parameterAnnotations != null);
  }


  static AnnotationInfo[][] NO_PARAMETER_ANNOTATIONS_0 = new AnnotationInfo[0][];
  static AnnotationInfo[][] NO_PARAMETER_ANNOTATIONS_1 = { new AnnotationInfo[0] };
  static AnnotationInfo[][] NO_PARAMETER_ANNOTATIONS_2 = { new AnnotationInfo[0], new AnnotationInfo[0] };
  static AnnotationInfo[][] NO_PARAMETER_ANNOTATIONS_3 = { new AnnotationInfo[0], new AnnotationInfo[0], new AnnotationInfo[0] };  
  
  public AnnotationInfo[][] getParameterAnnotations() {
    if (parameterAnnotations == null){ 
      int n = getNumberOfArguments();
      switch (n){
      case 0: return NO_PARAMETER_ANNOTATIONS_0;
      case 1: return NO_PARAMETER_ANNOTATIONS_1;
      case 2: return NO_PARAMETER_ANNOTATIONS_2;
      case 3: return NO_PARAMETER_ANNOTATIONS_3;
      default:
        AnnotationInfo[][] pai = new AnnotationInfo[n][];
        for (int i=0; i<n; i++){
          pai[i] = new AnnotationInfo[0];
        }
        return pai;
      }
      
    } else {
      return parameterAnnotations;
    }
  }

  
  public AnnotationInfo[] getParameterAnnotations(int parameterIndex){
    if (parameterAnnotations == null){
      return null;
    } else {
      if (parameterIndex >= getNumberOfArguments()){
        return null;
      } else {
        return parameterAnnotations[parameterIndex];
      }
    }
  }


  
  public static int getNumberOfLoadedMethods () {
    return mthTable.size();
  }

  void setAtomic (boolean isAtomic) {
    if (isAtomic) {
      attributes |= EXEC_ATOMIC;
    } else {
      attributes &= ~EXEC_ATOMIC;
    }
  }
  public boolean isAtomic () {
    return ((attributes & EXEC_ATOMIC) != 0);
  }
  
  void setHidden (boolean isHidden) {
    if (isHidden) {
      attributes |= EXEC_HIDDEN;
    } else {
      attributes &= ~EXEC_HIDDEN;
    }
  }
  public boolean isHidden () {
    return ((attributes & EXEC_HIDDEN) != 0);    
  }
  
  
  public void setFirewall (boolean isFirewalled) {
    if (isFirewalled) {
      attributes |= FIREWALL;
    } else {
      attributes &= ~FIREWALL;
    }
  }
  public boolean isFirewall () {
    return ((attributes & FIREWALL) != 0);    
  }
  
  
  
  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException cnx) {
      return null;
    }
  }
  
  public int getGlobalId() {
    return globalId;
  }

  public DirectCallStackFrame createRunStartStackFrame (ThreadInfo ti){
    return ci.createRunStartStackFrame( ti, this);
  }

  public DirectCallStackFrame createDirectCallStackFrame (ThreadInfo ti, int nLocals){
    return ci.createDirectCallStackFrame(ti, this, nLocals);
  }

  public boolean isSyncRelevant () {
    return (name.charAt(0) != '<');
  }
  
  public boolean isInitOrClinit (){
    return ((attributes & (IS_CLINIT | IS_INIT)) != 0);
  }
  
  public boolean isClinit () {
    return ((attributes & IS_CLINIT) != 0);
  }

  public boolean isClinit (ClassInfo ci) {
    return (((attributes & IS_CLINIT) != 0) && (this.ci == ci));
  }

  public boolean isInit() {
    return ((attributes & IS_INIT) != 0);
  }
  
  public boolean isDirectCallStub(){
    return ((attributes & IS_DIRECT_CALL) != 0);    
  }
  
  
  public String getLongName () {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    
    sb.append('(');
    String[] argTypeNames = getArgumentTypeNames();
    for (int i=0; i<argTypeNames.length; i++) {
      String a = argTypeNames[i];
      int idx = a.lastIndexOf('.');
      if (idx > 0) {
        a = a.substring(idx+1);
      }
      if (i>0) {
        sb.append(',');
      }
      sb.append(a);
    }
    sb.append(')');
    
    return sb.toString();
  }
  
  
  public static String getUniqueName (String mname, String signature) {
    return (mname + signature);
  }

  public String getStackTraceSource() {
    return getSourceFileName();
  }

  public byte[] getArgumentTypes () {
    if (argTypes == null) {
      argTypes = Types.getArgumentTypes(signature);
      nArgs = argTypes.length;
    }

    return argTypes;
  }

  public String[] getArgumentTypeNames () {
    return Types.getArgumentTypeNames(signature);
  }
  
  public int getArgumentsSize () {
    if (argSize < 0) {
      argSize = Types.getArgumentsSize(signature);

      if (!isStatic()) {
        argSize++;
      }
    }

    return argSize;
  }
  
  
  public LocalVarInfo[] getArgumentLocalVars(){
    if (localVars == null){ 
      return null;
    }
    
    int nArgs = getNumberOfStackArguments(); 
    if (nArgs == 0){
      return new LocalVarInfo[0]; 
    }

    LocalVarInfo[] argLvis = new LocalVarInfo[nArgs];
    int n = 0; 
    
    for (LocalVarInfo lvi : localVars){

      if (lvi.getStartPC() == 0){
        if (n == nArgs){ 
          throw new JPFException("inconsistent localVar table for method " + getFullName());
        }
        




        int slotIdx = lvi.getSlotIndex();

        int i;
        for (i = 0; i < n; i++) {
          if (slotIdx < argLvis[i].getSlotIndex()) {
            for (int j=n; j>i; j--){
              argLvis[j] = argLvis[j-1];
            }
            argLvis[i] = lvi;
            n++;
            break;
          }
        }
        if (i == n) { 
          argLvis[n++] = lvi;
        }
      }
    }
    
    return argLvis;
  }
  
  
  public String getReturnType () {
    return Types.getReturnTypeSignature(signature);
  }

  public String getReturnTypeName () {
    return Types.getReturnTypeName(signature);
  }
  
  public String getSourceFileName () {
    if (ci != null) {
      return ci.getSourceFileName();
    } else {
      return "[VM]";
    }
  }

  public String getClassName () {
    if (ci != null) {
      return ci.getName();
    } else {
      return "[VM]";
    }
  }
  
  
  public ClassInfo getClassInfo () {
    return ci;
  }

  
  @Deprecated
public String getCompleteName () {
    return getFullName();
  }

  
  public String getBaseName() {
    return getClassName() + '.' + name;
  }
    
  public boolean isCtor () {
    return (name.equals("<init>"));
  }
  
  public boolean isInternalMethod () {

    return (name.equals("<clinit>") || uniqueName.equals("finalize()V"));
  }
  
  public boolean isThreadEntry (ThreadInfo ti) {
    return (uniqueName.equals("run()V") && (ti.countStackFrames() == 1));
  }
  
  
  public String getFullName () {
    if (ci != null) {
      return ci.getName() + '.' + getUniqueName();
    } else {
      return getUniqueName();
    }
  }

  
  public String getStackTraceName(){
    if (ci != null) {
      return ci.getName() + '.' + name;
    } else {
      return name;
    }
  }
  
  
  public int getNumberOfInstructions() {
    if (code == null){
      return 0;
    }
    
    return code.length;
  }
  
  
  public Instruction getInstruction (int i) {
    if (code == null) {
      return null;
    }

    if ((i < 0) || (i >= code.length)) {
      return null;
    }

    return code[i];
  }

  
  public Instruction getInstructionAt (int position) {
    if (code == null) {
      return null;
    }

    for (int i = 0, l = code.length; i < l; i++) {
      if ((code[i] != null) && (code[i].getPosition() == position)) {
        return code[i];
      }
    }

    throw new JPFException("instruction not found");
  }

  
  public Instruction[] getInstructions () {
    return code;
  }
  
  public boolean includesLine (int line){
    int len = code.length;
    return (code[0].getLineNumber() <= line) && (code[len].getLineNumber() >= line);
  }

  public Instruction[] getInstructionsForLine (int line){
    return getInstructionsForLineInterval(line,line);
  }

  public Instruction[] getInstructionsForLineInterval (int l1, int l2){
    Instruction[] c = code;
       


    
    if (c != null){
       ArrayList<Instruction> matchingInsns = null;
       
       for (int i = 0; i < c.length; i++) {
        Instruction insn = c[i];
        int line = insn.getLineNumber();
        if (line == l1 || line == l2 || (line > l1 && line < l2)) {
          if (matchingInsns == null) {
            matchingInsns = new ArrayList<Instruction>();
          }
          matchingInsns.add(insn);
        }
      }
      
      if (matchingInsns == null) {
        return null;
      } else {
        return matchingInsns.toArray(new Instruction[matchingInsns.size()]);
      }
            
    } else {
      return null;
    }
  }

  public Instruction[] getMatchingInstructions (LocationSpec lspec){
    return getInstructionsForLineInterval(lspec.getFromLine(), lspec.getToLine());
  }


  
  public int getLineNumber (Instruction pc) {
    if (lineNumbers == null) {
      if (pc == null)
        return -1;
      else
        return pc.getPosition();
    }

    if (pc != null) {
      int idx = pc.getInstructionIndex();
      if (idx < 0) idx = 0;
      return lineNumbers[idx];
    } else {
      return -1;
    }
  }

  
  public int[] getLineNumbers () {
    return lineNumbers;
  }

  public boolean containsLineNumber (int n){
    if (lineNumbers != null){
      return (lineNumbers[0] <= n) && (lineNumbers[lineNumbers.length-1] <= n);
    }
    
    return false;
  }
  
  public boolean intersectsLineNumbers( int first, int last){
    if (lineNumbers != null){
      if ((last < lineNumbers[0]) || (first > lineNumbers[lineNumbers.length-1])){
        return false;
      }
      return true;
    }
    
    return false;
  }
  
  public ExceptionHandler getHandlerFor (ClassInfo ciException, Instruction insn){
    if (exceptionHandlers != null){
      int position = insn.getPosition();
      for (int i=0; i<exceptionHandlers.length; i++){
        ExceptionHandler handler = exceptionHandlers[i];
        if ((position >= handler.getBegin()) && (position < handler.getEnd())) {

          String handledType = handler.getName();
          if ((handledType == null)   
                  || ciException.isInstanceOf(handledType)) {
            return handler;
          }
        }          
      }      
    }
    
    return null;
  }
  
  public boolean isMJI () {
    return false;
  }

  public int getMaxLocals () {
    return maxLocals;
  }

  public int getMaxStack () {
    return maxStack;
  }

  public ExceptionHandler[] getExceptions () {
    return exceptionHandlers;
  }

  public String[] getThrownExceptionClassNames () {
    return thrownExceptionClassNames;
  }


  public LocalVarInfo getLocalVar(String name, int pc){
    LocalVarInfo[] vars = localVars;
    if (vars != null){
      for (int i = 0; i < vars.length; i++) {
        LocalVarInfo lv = vars[i];
        if (lv.matches(name, pc)) {
          return lv;
        }
      }
    }

    return null;

  }

  public LocalVarInfo getLocalVar (int slotIdx, int pc){
    LocalVarInfo[] vars = localVars;

    if (vars != null){
      for (int i = 0; i < vars.length; i++) {
        LocalVarInfo lv = vars[i];
        if (lv.matches(slotIdx, pc)) {
          return lv;
        }
      }
    }

    return null;
  }

  public LocalVarInfo[] getLocalVars() {
    return localVars; 
  }


  
  public String[] getLocalVariableNames() {
    String[] names = new String[localVars.length];

    for (int i=0; i<localVars.length; i++){
      names[i] = localVars[i].getName();
    }

    return names;
  }


  public MethodInfo getOverriddenMethodInfo(){
    MethodInfo smi = null;
    
    if (ci != null) {
      ClassInfo sci = ci.getSuperClass();
      if (sci != null){
        smi = sci.getMethod(getUniqueName(), true);
      }
    }
    
    return smi;
  }
  
  
  public String getName () {
    return name;
  }

  public String getJNIName () {
    return Types.getJNIMangledMethodName(null, name, signature);
  }
  
  public int getModifiers () {
    return modifiers;
  }
  
  
  public boolean isNative () {
    return ((modifiers & Modifier.NATIVE) != 0);
  }

  public boolean isAbstract () {
    return ((modifiers & Modifier.ABSTRACT) != 0);
  }
  

  public boolean isUnresolvedNativeMethod(){
    return ((modifiers & Modifier.NATIVE) != 0);
  }


  public boolean isJPFExecutable (){
    return !hasAttr(NoJPFExec.class);
  }

  public int getNumberOfArguments () {
    if (nArgs < 0) {
      nArgs = Types.getNumberOfArguments(signature);
    }

    return nArgs;
  }

  
  public int getNumberOfStackArguments () {
    int n = getNumberOfArguments();

    return isStatic() ? n : n + 1;
  }

  public int getNumberOfCallerStackSlots () {
    return Types.getNumberOfStackSlots(signature, isStatic()); 
  }

  public Instruction getFirstInsn(){
    if (code != null){
      return code[0];
    }
    return null;    
  }
  
  public Instruction getLastInsn() {
    if (code != null){
      return code[code.length-1];
    }
    return null;
  }

  
  public boolean isReferenceReturnType () {
    int r = getReturnTypeCode();

    return ((r == Types.T_REFERENCE) || (r == Types.T_ARRAY));
  }

  public byte getReturnTypeCode () {
    if (returnType < 0) {
      returnType = Types.getReturnBuiltinType(signature);
    }

    return returnType;
  }

  
  public int getReturnSize() {
    if (retSize == -1){
      switch (getReturnTypeCode()) {
        case Types.T_VOID:
          retSize = 0;
          break;

        case Types.T_LONG:
        case Types.T_DOUBLE:
          retSize = 2;
          break;

        default:
          retSize = 1;
          break;
      }
    }

    return retSize;
  }

  public Class<? extends ChoiceGenerator<?>> getReturnChoiceGeneratorType (){
    switch (getReturnTypeCode()){
      case Types.T_BOOLEAN:
        return BooleanChoiceGenerator.class;

      case Types.T_BYTE:
      case Types.T_CHAR:
      case Types.T_SHORT:
      case Types.T_INT:
        return IntChoiceGenerator.class;

      case Types.T_LONG:
        return LongChoiceGenerator.class;

      case Types.T_FLOAT:
        return FloatChoiceGenerator.class;

      case Types.T_DOUBLE:
        return DoubleChoiceGenerator.class;

      case Types.T_ARRAY:
      case Types.T_REFERENCE:
      case Types.T_VOID:
        return ReferenceChoiceGenerator.class;
    }

    return null;
  }

  
  public String getSignature () {
    return signature;
  }

  @Override
  public String getGenericSignature() {
    return genericSignature;
  }

  @Override
  public void setGenericSignature(String sig){
    genericSignature = sig;
  }

  
  public boolean isStatic () {
    return ((modifiers & Modifier.STATIC) != 0);
  }

  
  public boolean isPublic() {
    return ((modifiers & Modifier.PUBLIC) != 0);
  }
  
  public boolean isPrivate() {
    return ((modifiers & Modifier.PRIVATE) != 0);
  }
  
  public boolean isProtected() {
    return ((modifiers & Modifier.PROTECTED) != 0);
  }

  
  public boolean isSynchronized () {
    return ((modifiers & Modifier.SYNCHRONIZED) != 0);
  }





  
  public boolean isSynthetic(){
    return ((modifiers & 0x00001000) != 0);    
  } 
  public boolean isVarargs(){
    return ((modifiers & 0x00000080) != 0);        
  }
  
  
  public boolean isJPFInternal(){




    return (ci == null);
  }
  
  public String getUniqueName () {
    return uniqueName;
  }
  
  public boolean hasCode(){
    return (code != null);
  }
  
  public boolean hasEmptyBody (){

    return (code.length == 1 && (code[0] instanceof ReturnInstruction));
  }




  protected void startParameterAnnotations(int annotationCount){
    parameterAnnotations = new AnnotationInfo[annotationCount][];
  }
  protected void setParameterAnnotations(int index, AnnotationInfo[] ai){
    parameterAnnotations[index] = ai;
  }
  protected void finishParameterAnnotations(){

  }

  public void setParameterAnnotations (AnnotationInfo[][] parameterAnnotations){
    this.parameterAnnotations = parameterAnnotations;
  }
  


  protected void startTrownExceptions (int exceptionCount){
    thrownExceptionClassNames = new String[exceptionCount];
  }
  protected void setException (int index, String exceptionType){
    thrownExceptionClassNames[index] = Types.getClassNameFromTypeName(exceptionType);
  }
  protected void finishThrownExceptions(){

  }

  public void setThrownExceptions (String[] exceptions){
    thrownExceptionClassNames = exceptions;
  }
  



  protected void startExceptionHandlerTable (int handlerCount){
    exceptionHandlers = new ExceptionHandler[handlerCount];
  }
  protected void setExceptionHandler (int index, int startPc, int endPc, int handlerPc, String catchType){
    exceptionHandlers[index] = new ExceptionHandler(catchType, startPc, endPc, handlerPc);
  }
  protected void finishExceptionHandlerTable(){

  }

  public void setExceptionHandlers (ExceptionHandler[] handlers){
    exceptionHandlers = handlers;
  }
  


  protected void startLocalVarTable (int localVarCount){
    localVars = new LocalVarInfo[localVarCount];
  }
  protected void setLocalVar(int index, String varName, String descriptor, int scopeStartPc, int scopeEndPc, int slotIndex){
    localVars[index] = new LocalVarInfo(varName, descriptor, "", scopeStartPc, scopeEndPc, slotIndex);
  }
  protected void finishLocalVarTable(){

  }

  public void setLocalVarTable (LocalVarInfo[] locals){
    localVars = locals;
  }
  
  public void setLocalVarAnnotations (){
    if (localVars != null){
      for (VariableAnnotationInfo ai : getTargetTypeAnnotations(VariableAnnotationInfo.class)){
        for (int i = 0; i < ai.getNumberOfScopeEntries(); i++) {
          for (LocalVarInfo lv : localVars) {
            if (lv.getStartPC() == ai.getStartPC(i) && lv.getSlotIndex() == ai.getSlotIndex(i)) {
              lv.addTypeAnnotation(ai);
            }
          }
        }
      }
    }
  }
  
  public boolean hasTypeAnnotatedLocalVars (){
    if (localVars != null){
      for (LocalVarInfo lv : localVars){
        if (lv.hasTypeAnnotations()){
          return true;
        }
      }
    }
    
    return false;
  }
  
  public List<LocalVarInfo> getTypeAnnotatedLocalVars (){
    List<LocalVarInfo> list = null;
    
    if (localVars != null){
      for (LocalVarInfo lv : localVars){
        if (lv.hasTypeAnnotations()){
          if (list == null){
            list = new ArrayList<LocalVarInfo>();
          }
          list.add(lv);
        }
      }
    }
    
    if (list == null){
      list = Collections.emptyList();
    }
    
    return list;
  }
  
  public List<LocalVarInfo> getTypeAnnotatedLocalVars (String annotationClsName){
    List<LocalVarInfo> list = null;
    
    if (localVars != null){
      for (LocalVarInfo lv : localVars){
        AbstractTypeAnnotationInfo tai = lv.getTypeAnnotation(annotationClsName);
        if (tai != null){
          if (list == null){
            list = new ArrayList<LocalVarInfo>();
          }
          list.add(lv);
        }
      }
    }
    
    if (list == null){
      list = Collections.emptyList();
    }
    
    return list;
  }
  
  


  protected void startLineNumberTable(int lineNumberCount){
    int len = code.length;
    int[] ln = new int[len];

    lineNumbers = ln;
  }
  protected void setLineNumber(int index, int lineNumber, int startPc){
    int len = code.length;
    int[] ln = lineNumbers;

    for (int i=0; i<len; i++){
      Instruction insn = code[i];
      int pc = insn.getPosition();

      if (pc == startPc){ 
        ln[i] = lineNumber;
        return;
      }
    }
  }
  protected void finishLineNumberTable (){
    int len = code.length;
    int[] ln = lineNumbers;
    int lastLine = ln[0];

    for (int i=1; i<len; i++){
      if (ln[i] == 0){
        ln[i] = lastLine;
      } else {
        lastLine = ln[i];
      }
    }
  }

  
  public void setLineNumbers (int[] lines, int[] startPcs){
    int j=0;
    int lastLine = -1;
    
    int len = code.length;
    int[] ln = new int[len];

    for (int i=0; i<len; i++){
      Instruction insn = code[i];
      int pc = insn.getPosition();
      
      if ((j < startPcs.length) && pc == startPcs[j]){
        lastLine = lines[j];
        j++;
      }
      
      ln[i] = lastLine;
    }
    
    lineNumbers = ln;
  }

  
  public void setLineNumbers (int[] lines){
    if (lines.length != code.length){
      throw new JPFException("inconsitent code/line number size");
    }
    lineNumbers = lines;
  }
  
  @Override
  public String toString() {
    return "MethodInfo[" + getFullName() + ']';
  }
  

  public void dump(){
    System.out.println("--- " + this);
    for (int i = 0; i < code.length; i++) {
      System.out.printf("%2d [%d]: %s\n", i, code[i].getPosition(), code[i].toString());
    }
  }

  
  public MethodInfo getInstanceFor(ClassInfo ci) {
    MethodInfo clone;

    try {
      clone = (MethodInfo)super.clone();
      clone.ci = ci;

      clone.globalId = mthTable.size();
      mthTable.add(this);

      if(code == null) {
        clone.code = null;
      } else {
        clone.code = new Instruction[code.length];

        for(int i=0; i<code.length; i++) {
          clone.code[i] = code[i].typeSafeClone(clone);
        }
      }

    } catch (CloneNotSupportedException cnsx){
      cnsx.printStackTrace();
      return null;
    }

    return clone;
  }
}
