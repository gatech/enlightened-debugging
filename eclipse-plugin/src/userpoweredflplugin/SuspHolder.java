/* 
 * Copyright 2019 Georgia Institute of Technology
 * All rights reserved.
 *
 * Author(s): Shaowei Zhu <swzhu@cc.gatech.edu>
 *
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package userpoweredflplugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.core.PackageFragment;

import edu.gatech.arktos.userpoweredfl.data.SourceLocation;

public class SuspHolder {
	public Map<SourceLocation, Double> suspMap;
	public Set<String> suspFilesSet;
	public boolean isValid;
	public List<Double> sortedSuspList;
	public List<ISuspListener> suspListeners;
	public int firstNoneZeroIndex;

	public SuspHolder() {
		isValid = false;
		suspListeners = new ArrayList<ISuspListener>();
	}

	public void addSuspListener(ISuspListener listener) {
		this.suspListeners.add(listener);
	}

	public void removeSuspListener(ISuspListener listener) {
		this.suspListeners.remove(listener);
	}

	public void updateSuspMap(Map<SourceLocation, Double> newMap) {
		System.out.println("updating suspiciousness maps");
		this.suspMap = newMap;
		isValid = true;
		this.suspFilesSet = new HashSet<String>();
		for (Map.Entry<SourceLocation, Double> entry : suspMap.entrySet()) {
			this.suspFilesSet.add(entry.getKey().getSourceFile());
		}
		sortedSuspList = new ArrayList<Double>(suspMap.values());
		java.util.Collections.sort(sortedSuspList);
		firstNoneZeroIndex = -Collections.binarySearch(sortedSuspList, 1E-5) - 1;
		for (ISuspListener list : this.suspListeners) {
			list.suspChanged();
		}
	}

	public void setInvalid() {
		this.isValid = false;
		this.suspFilesSet = null;
		this.sortedSuspList = null;
	}

	/**
	 * Get suspiciousness level of a source file line
	 * 
	 * @param fileName
	 * @param lineNumber
	 * @return -1 if no susp avilable, 0 if not susp, 1 if low susp, 2 if mid
	 *         susp, 3 if high susp
	 */
	public int getSuspLevel(String fileName, int lineNumber) {
		if (!isValid)
			return -1;
		if (!this.suspFilesSet.contains(fileName)) {
			return 0;
		}
		SourceLocation srcLoc = SourceLocation.get(fileName, lineNumber);
		double susp = this.suspMap.getOrDefault(srcLoc, -1.0);
		// System.out.println(fileName + ": " + Integer.toString(lineNumber) + "
		// Susp: " + Double.toString(susp));

		if (susp <= 1E-3)
			return 0;
		else if (Math.abs(susp - this.sortedSuspList.get(this.sortedSuspList.size() - 1)) < 1e-5) {
			return 3;
		} else if (susp < this.sortedSuspList
				.get(this.firstNoneZeroIndex + (this.sortedSuspList.size() - this.firstNoneZeroIndex) / 3 * 2)) {
			return 1;
		} else
			return 2;

	}

	public ILine getLine(IJavaElement e, int lineNumber) {
		String[] pathNames = ((PackageFragment) e.getParent()).names;
		String fullNameString = String.join("/", pathNames) + "/" + e.getElementName();
		return new ILine(lineNumber, this.getSuspLevel(fullNameString, lineNumber));
	}
}