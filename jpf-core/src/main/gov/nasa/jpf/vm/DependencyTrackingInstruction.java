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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import anonymous.domain.enlighten.deptrack.ActiveCondition;
import anonymous.domain.enlighten.deptrack.DynamicDependency;
import anonymous.domain.enlighten.deptrack.DynamicDependencySource;
import anonymous.domain.enlighten.deptrack.InstructionDependencyListener;
import anonymous.domain.enlighten.deptrack.InstructionDependencySource;
import anonymous.domain.enlighten.deptrack.MethodInvocationAttr;

public abstract class DependencyTrackingInstruction extends Instruction {
	
	private static List<InstructionDependencyListener> insnDepListeners = new ArrayList<>();
	
	private List<DependencyTrackingInstruction> controlDependencies;
	
	public static void addInstructionDependencyListener(InstructionDependencyListener listener) {
		if (!insnDepListeners.contains(listener)) {
			insnDepListeners.add(listener);
		}
	}
	
	public static boolean removeInstructionDependencyListener(InstructionDependencyListener listener) {
		return insnDepListeners.remove(listener);
	}

	public void addControlDependency(DependencyTrackingInstruction instruction) {
		if (controlDependencies == null) {
			controlDependencies = new ArrayList<>();
		}
		if (!controlDependencies.contains(instruction)) {
			controlDependencies.add(instruction);
		}
	}
	
	public void setControlDependency(List<DependencyTrackingInstruction> dependencies) {
		controlDependencies = dependencies;
	}
	
	public List<DependencyTrackingInstruction> getControlDependencyInstructions() {
		if (controlDependencies == null) {
			return Collections.emptyList();
		}
		return controlDependencies;
	}
	
	protected DynamicDependencySource getInstructionDepSource(ThreadInfo ti) {
		return getInstructionDepSource(ti.getTopFrame());
	}
	
	protected DynamicDependencySource getInstructionDepSource(StackFrame frame) {
		MethodInvocationAttr frameAttr = frame.getFrameAttr(MethodInvocationAttr.class);
		if (frameAttr != null && frameAttr.getGenInstrDep()) {
			InstructionDependencySource depNode = new InstructionDependencySource(this);
			notifyInstructionDependencyListeners(depNode);
			return depNode;
		} else {
			return null;
		}
	}
	
	protected DynamicDependency getControlDependencyCondition(ThreadInfo ti) {
		List<DependencyTrackingInstruction> cdList = getControlDependencyInstructions();
		if (cdList.size() > 0) {
			List<ActiveCondition> candidateDeps = new ArrayList<>();
			for (DependencyTrackingInstruction cd : cdList) {
				ActiveCondition activeCondition = cd.getActiveCondition(ti);
				if (activeCondition != null) {
					candidateDeps.add(activeCondition);
				}
			}
			if (candidateDeps.size() == 0) {
				return null;
			} else if (candidateDeps.size() == 1) {
				return candidateDeps.get(0).getDependency();
			}
			long mostRecentSerial = -1;
			ActiveCondition mostRecentCondition = null;
			for (ActiveCondition current : candidateDeps) {
				if (current.getSerialNum() > mostRecentSerial) {
					mostRecentCondition = current;
					mostRecentSerial = current.getSerialNum();
				}
			}
			return mostRecentCondition.getDependency();
		} else {
			MethodInvocationAttr frameAttr = 
					ti.getTopFrame().getFrameAttr(MethodInvocationAttr.class);
			if (frameAttr != null) {
				return frameAttr.getInvocationCondition();
			} else {
				return null;
			}
		}
	}
	
	protected void setActiveCondition(ThreadInfo ti, DynamicDependency condition) {
		MethodInvocationAttr frameAttr = ti.getTopFrame().getFrameAttr(MethodInvocationAttr.class);
		if (frameAttr != null) {
			frameAttr.setInstrActiveCondition(this, condition);
		}
	}
	
	protected ActiveCondition getActiveCondition(ThreadInfo ti) {
		MethodInvocationAttr frameAttr = ti.getTopFrame().getFrameAttr(MethodInvocationAttr.class);
		if (frameAttr != null) {
			return frameAttr.getInstrActiveCondition(this);
		}
		return null;
	}
	
	protected DynamicDependency[] convertToDep(Object... attrs) {
		DynamicDependency[] deps = new DynamicDependency[attrs.length];
		for (int i = 0; i < attrs.length; ++i) {
			deps[i] = (DynamicDependency) attrs[i];
		}
		return deps;
	}
	
	private void notifyInstructionDependencyListeners(InstructionDependencySource dep) {
		for (InstructionDependencyListener listener : insnDepListeners) {
			listener.instructionDependencySourceGenerated(this, dep);
		}
	}
}
