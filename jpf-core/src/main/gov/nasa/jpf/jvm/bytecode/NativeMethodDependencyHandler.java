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

package gov.nasa.jpf.jvm.bytecode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import anonymous.domain.enlighten.deptrack.DynDepUtils;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.NativeStackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;

public class NativeMethodDependencyHandler {
	

	private static final Set<String> HANDLED_OWNER_CLASSES = new HashSet<>(Arrays.asList(
			"Ljava/lang/Object;", "Ljava/lang/ClassLoader;", "Ljava/lang/Thread;", "Lsun/misc/Unsafe;"));



	private static final Set<String> HANDLED_CLASSES = new HashSet<>(Arrays.asList(
			"Ljava/lang/Class;", "Ljava/lang/Boolean;",
			"Ljava/lang/Byte;", "Ljava/lang/Character;", "Ljava/lang/Short;", "Ljava/lang/Integer",
			"Ljava/lang/Long;", "Ljava/lang/Float;", "Ljava/lang/Double;", "Ljava/lang/String;",
			"Ljava/lang/reflect/Constructor;", "Ljava/lang/reflect/Field;", "Ljava/lang/reflect/Method;",
			"Ljava/lang/reflect/Parameter;", "Ljava/lang/annotation/Annotation;"));

	private NativeStackFrame frame;
	private ThreadInfo currentThread;
	
	private Boolean canHandle;
	
	public NativeMethodDependencyHandler(NativeStackFrame frame, ThreadInfo ti) {
		this.frame = frame;
		currentThread = ti;
	}
	
	public boolean canHandle() {
		if (canHandle != null) {
			return canHandle;
		}
    MethodInfo methodInfo = frame.getMethodInfo();
    String thisRefTypeSig = null;
    if (!methodInfo.isStatic()) {
    	thisRefTypeSig = methodInfo.getClassInfo().getSignature();
    }
    if (thisRefTypeSig != null && !HANDLED_OWNER_CLASSES.contains(thisRefTypeSig) 
    		&& !canHandleType(thisRefTypeSig, false)) {
    	canHandle = false;
    	return false;
    }
    String[] argTypeSigs = 
    		Types.getArgumentTypeSignatures(methodInfo.getSignature());
    for (int i = 0; i < argTypeSigs.length; ++i) {
    	if (!canHandleType(argTypeSigs[i], false)) {
    		canHandle = false;
    		return false;
    	}
    }
    String retTypeSig = methodInfo.getReturnType();
    canHandle = canHandleType(retTypeSig, true);
    return canHandle;
	}
	
	public List<DynamicDependency> extractArgDependencies() {
		if (!canHandle()) {
			throw new RuntimeException(
					"Cannot handle native method " + frame.getMethodInfo().getFullName());
		}
		List<DynamicDependency> deps = new ArrayList<>();
		MethodInfo methodInfo = frame.getMethodInfo();


		Object[] argAttrs = frame.getCallerFrame().getArgumentAttrs(methodInfo);
		byte[] argTypes = methodInfo.getArgumentTypes();
		String[] argTypeSigs = Types.getArgumentTypeSignatures(methodInfo.getSignature());
		if (!methodInfo.isStatic()) {
			String[] partialTypeSigs = argTypeSigs;
			argTypeSigs = new String[partialTypeSigs.length + 1];
			argTypeSigs[0] = methodInfo.getClassInfo().getSignature();
			System.arraycopy(partialTypeSigs, 0, argTypeSigs, 1, partialTypeSigs.length);
			byte[] partialArgTypes = argTypes;
			argTypes = new byte[partialArgTypes.length + 1];
			argTypes[0] = Types.T_REFERENCE;
			System.arraycopy(partialArgTypes, 0, argTypes, 1, partialArgTypes.length);
		}
		Object[] argValues = frame.getCallerFrame().getArgumentsValues(currentThread, argTypes);
		for (int i = 0; i < argTypes.length; ++i) {
			if (argAttrs != null && argAttrs[i] != null) {
				deps.add((DynamicDependency) argAttrs[i]);
			}
			if (argTypes[i] == Types.T_REFERENCE) {
				ElementInfo eiObject = (ElementInfo) argValues[i];
				if (eiObject != null) {
					addDepFromObject(eiObject, deps);
				}
			}
		}
		return deps;
	}
	
	public void setReturnValueDependency(DynamicDependency dep) {
		if (!canHandle()) {
			throw new RuntimeException(
					"Cannot handle native method " + frame.getMethodInfo().getFullName());
		}
		String returnTypeSig = frame.getMethodInfo().getReturnType();
		if (Types.getTypeCode(returnTypeSig) == Types.T_VOID) {
			return;
		}
		frame.setReturnAttr(dep);
		if (Types.getTypeCode(returnTypeSig) == Types.T_REFERENCE) {
			ElementInfo eiRet = currentThread.getElementInfo(frame.getReferenceResult());
			if (eiRet != null) {
				setDepOnObject(eiRet, dep);
			}
		} else if (Types.getTypeCode(returnTypeSig) == Types.T_ARRAY) {
			ElementInfo eiRetArray = currentThread.getElementInfo(frame.getReferenceResult());
			boolean isRefArray = eiRetArray.isReferenceArray();
			if (eiRetArray != null) {
				for (int i = 0; i < eiRetArray.arrayLength(); ++i) {
					eiRetArray.setElementAttr(i, dep);
					if (isRefArray) {
						ElementInfo eiElement = 
								currentThread.getElementInfo(eiRetArray.getReferenceElement(i));
						if (eiElement != null) {

							setDepOnObject(eiElement, dep);
						}
					}
				}
			}
		}
	}
	
	private void addDepFromObject(ElementInfo eiObject, List<DynamicDependency> deps) {
		if (eiObject.isBoxObject()) {
			FieldInfo valueField = eiObject.getClassInfo().getDeclaredInstanceField("value");
			DynamicDependency dep = FieldInstruction.getFieldDependency(eiObject, valueField);
			if (dep != null) {
				deps.add(dep);
			}
		} else if (eiObject.isStringObject()) {
			FieldInfo valueField = eiObject.getClassInfo().getDeclaredInstanceField("value");
			DynamicDependency dep = FieldInstruction.getFieldDependency(eiObject, valueField);
			if (dep != null) {
				deps.add(dep);
			}
			ElementInfo charArray = 
					currentThread.getElementInfo(eiObject.getReferenceField(valueField));
			addDepFromArray(charArray, deps);
		}
	}
	
	private void addDepFromArray(ElementInfo eiArray, List<DynamicDependency> deps) {
		ClassInfo componentClassInfo = eiArray.getClassInfo().getComponentClassInfo();
		if (componentClassInfo.isArray()) {
			throw new RuntimeException("Does not handle mutli-dimensional array yet.");
		}
		boolean isReferenceArray = eiArray.isReferenceArray();
		for (int index = 0; index < eiArray.arrayLength(); ++index) {
			DynamicDependency elementDep = 
					JVMArrayElementInstruction.getArrayElementDependency(eiArray, index);
			if (elementDep != null) {
				deps.add(elementDep);
			}
			if (isReferenceArray) {
				ElementInfo refElement = 
						currentThread.getElementInfo(eiArray.getReferenceElement(index));
				if (refElement != null) {
					addDepFromObject(refElement, deps);
				}
			}
		}
	}
	
	private void setDepOnObject(ElementInfo eiObject, DynamicDependency dep) {
		if (eiObject.isBoxObject()) {
			FieldInfo valueField = eiObject.getClassInfo().getDeclaredInstanceField("value");
			eiObject.setFieldAttr(valueField, dep);
		} else if (eiObject.isStringObject()) {
			DynDepUtils.setDependencyOnString(currentThread, eiObject, dep);
		}
	}
	
	private static boolean canHandleType(String typeSig, boolean include1dArray) {
		if (typeSig == null) {
			return true;
		}
		if (!Types.isReference(typeSig)) {
			return true;
		} else if (Types.isArray(typeSig)) {
			if (include1dArray) {
				String componentTypeSig = Types.getArrayElementType(typeSig);
				if (!Types.isReference(componentTypeSig) || HANDLED_CLASSES.contains(componentTypeSig)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else { 
			return HANDLED_CLASSES.contains(typeSig);
		}
	}
	
}
