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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import edu.gatech.arktos.userpoweredfl.FeedbackDirectedFLCore;
import edu.gatech.arktos.userpoweredfl.UserFeedback;
import edu.gatech.arktos.userpoweredfl.mcallrepr.MemberRefName;
import edu.gatech.arktos.userpoweredfl.mcallrepr.MethodCallSpecialRefName;
import edu.gatech.arktos.userpoweredfl.refpath.RefPath;
import edu.gatech.arktos.userpoweredfl.slicing.FieldSelectionResult;
import edu.gatech.arktos.userpoweredfl.util.Pair;
import userpoweredflplugin.LazyValueReprTreeNode.FeedbackType;

public class LazyTreeItemHandler {
	// TODO: implement methods to mark a node and all its decendents
	public FeedbackDirectedFLCore fbFLCore;
	public FieldSelectionResult query;
	List<Pair<RefPath, Double>> orderedCandidateFields;
	HashMap<String, Double> suspLookup;
	HashMap<String, RefPath> pathLookup;
	HashMap<String, FeedbackType> feedbackLookup;
	HashSet<String> isTransitivelySusp;
	HashSet<String> isTransitivelyMostSusp;
	boolean isParamsSuspicious;
	boolean isParamsMostSuspicious;
	boolean feedbackGiven;
	ArrayList<TreePath> mostSuspTreePaths;

	public HashSet<String> mostSuspFields;
	public HashSet<String> selectedCorrectMostSuspFields;
	public HashSet<String> selectedIncorrectFields;
	public HashSet<String> selectedImpossiblePreFields;

	public LazyTreeItemHandler(FeedbackDirectedFLCore fbcore, FieldSelectionResult query) {
		this.fbFLCore = fbcore;
		this.query = query;
		this.orderedCandidateFields = query.getOrderedSuspiciousFields();
		double maxSusp = this.orderedCandidateFields.get(0).getSecond();
		this.suspLookup = new HashMap<>();
		this.pathLookup = new HashMap<>();
		this.feedbackLookup = new HashMap<>();
		this.isTransitivelySusp = new HashSet<>();
		this.isTransitivelyMostSusp = new HashSet<>();
		this.isParamsSuspicious = false;
		this.isParamsMostSuspicious = false;
		this.feedbackGiven = false;
		this.selectedCorrectMostSuspFields = new HashSet<>();
		this.selectedIncorrectFields = new HashSet<>();
		this.selectedImpossiblePreFields = new HashSet<>();
		this.mostSuspFields = new HashSet<>();
		String retRefName = MethodCallSpecialRefName.returnValue().toString();
		String expRefName = MethodCallSpecialRefName.exceptionThrown().toString();
		for (Pair<RefPath, Double> p : this.orderedCandidateFields) {
			List<MemberRefName> components = p.getFirst().getComponents();
			StringBuilder temp = new StringBuilder();
			boolean isFirst = true;
			for (int i = 0; i < components.size() - 1; i++) {
				MemberRefName c = components.get(i);
				temp.append((isFirst ? "" : ".") + c.toString());
				isFirst = false;
				if (Math.abs(p.getSecond() - maxSusp) < 1.0e-6) {
					isTransitivelyMostSusp.add(temp.toString());
				} else {
					isTransitivelySusp.add(temp.toString());
				}
			}
			temp.append((components.size() == 1 ? "" : ".") + components.get(components.size() - 1));
			String str = temp.toString();
			if (!str.startsWith("this.") && !str.equals(retRefName) && !str.startsWith(retRefName + ".")
					&& !str.equals(expRefName) && !str.startsWith(expRefName + ".")) {
				if (Math.abs(p.getSecond() - maxSusp) < 1.0e-6) {
					this.isParamsMostSuspicious = true;
				} else {
					this.isParamsSuspicious = true;
				}
			}

			suspLookup.put(p.getFirst().toString(), p.getSecond() / maxSusp);
			pathLookup.put(p.getFirst().toString(), p.getFirst());

			if (Math.abs(p.getSecond() - maxSusp) < 1.0e-6) {
				mostSuspFields.add(p.getFirst().toString());
			}
		}

		// SwiftUtilities.log("[SWIFT] tree item handler created successfully");
	}

	public boolean isTransitivelyMostSuspNode(LazyValueReprTreeNode node) {
		if (node.isVirtualRoot)
			return true;
		if (node.isParamsList) {
			return this.isParamsMostSuspicious;
		}
		return this.isTransitivelyMostSusp.contains(node.getRefPathStr());
	}

	public boolean isTransitivelySuspNode(LazyValueReprTreeNode node) {
		if (node.isParamsList)
			return this.isParamsSuspicious;
		return this.isTransitivelySusp.contains(node.getRefPathStr());
	}

	public boolean isNodeForCandidateField(LazyValueReprTreeNode node) {
		return this.suspLookup.containsKey(node.getRefPathStr());
	}

	public void storeUserFeedback(LazyValueReprTreeNode node) {
		this.feedbackLookup.put(node.getRefPathStr(), node.getFeedback());
	}

	public void resetUserFeedback(LazyValueReprTreeNode node) {
		FeedbackType fb = this.feedbackLookup.getOrDefault(node.getRefPathStr(), null);
		if (fb != null)
			node.resetFeedback(fb);
	}

	public void giveIncorrectOutputFeedbacks(boolean faultMustNotInCurrentInvocation) {
		if (this.selectedIncorrectFields.size() > 0) {
			for (String field : this.selectedIncorrectFields) {
				SwiftUtilities.log(
						"[SWIFT] Post-state field: " + field + ", Feedback: WRONG, Fault must not in correct invoc: "
								+ Boolean.toString(faultMustNotInCurrentInvocation));
				fbFLCore.incorporateIncorrectOutputValue(query, pathLookup.get(field), faultMustNotInCurrentInvocation);
			}
		}
	}

	public UserFeedback incorporateFeedback(LazyValueReprTreeNode node) {
		FeedbackType fb = node.getFeedback();
		String refPath = node.getRefPathStr();
		UserFeedback realFb = null;
		switch (fb) {
		case CORRECT:
			realFb = UserFeedback.CORRECT;
			if (this.mostSuspFields.contains(refPath)) {
				this.selectedCorrectMostSuspFields.add(refPath);
			}
			if (this.selectedIncorrectFields.contains(refPath))
				this.selectedIncorrectFields.remove(refPath);
			fbFLCore.incorporateCorrectOutputValue(query, pathLookup.get(node.getRefPathStr()));
			break;
		case WRONG:
			// this is a little bit different
			// caches the incorrect output fields and give feedback at last
			realFb = UserFeedback.INCORRECT;
			if (this.selectedCorrectMostSuspFields.contains(refPath)) {
				this.selectedCorrectMostSuspFields.remove(refPath);
			}
			this.selectedIncorrectFields.add(refPath);
			break;
		case IMPRE:
			realFb = UserFeedback.IMPOSSIBLE_PRESTATE;
			this.selectedImpossiblePreFields.add(refPath);
			// fixme: construct ref path
			RefPath refPathForPre = node.getRefPathFromBuilder();
			fbFLCore.incorporateIncorrectInputValue(query, refPathForPre);
			break;
		case NONE:
			if (this.selectedImpossiblePreFields.contains(refPath)) {
				RefPath nrefPath = node.getRefPathFromBuilder();
				fbFLCore.removeInputValueFeedback(query, nrefPath);
				this.selectedImpossiblePreFields.remove(refPath);
			}
			if (this.selectedCorrectMostSuspFields.contains(refPath)) {
				fbFLCore.removeOutputValueFeedback(query, pathLookup.get(node.getRefPathStr()));
				this.selectedCorrectMostSuspFields.remove(refPath);
			}
			if (this.selectedIncorrectFields.contains(refPath)) {
				fbFLCore.removeOutputValueFeedback(query, pathLookup.get(node.getRefPathStr()));
				this.selectedIncorrectFields.remove(refPath);
			}
			break;
		case NA:
			break;
		}

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IViewPart controlViewPart = page.findView("userpoweredflplugin.controlview");
				((ControlViewPart) controlViewPart).updateUI();

			}
		});

		return realFb;
	}

	public double getSuspOfNode(LazyValueReprTreeNode node) {
		if (this.mostSuspFields.contains(node.getRefPathStr()))
			return this.suspLookup.getOrDefault(node.getRefPathStr(), 0.0);
		else
			return 0.55 * this.suspLookup.getOrDefault(node.getRefPathStr(), 0.0);
	}

	public TreePath[] getMostSuspTreePath(LazyValueReprTreeNode root) {
		this.mostSuspTreePaths = new ArrayList<>();
		expandMostSuspPaths(root);
		return this.mostSuspTreePaths.toArray(new TreePath[this.mostSuspTreePaths.size()]);
	}

	public void expandMostSuspPaths(LazyValueReprTreeNode root) {
		Queue<TreePath> q = new LinkedList<>();
		TreePath empty = TreePath.EMPTY;
		q.add(empty.createChildPath(root));
		while (!q.isEmpty()) {
			TreePath front = q.poll();
			if (this.isTransitivelyMostSuspNode((LazyValueReprTreeNode) front.getLastSegment())) {
				this.mostSuspTreePaths.add(front);
				((LazyValueReprTreeNode) front.getLastSegment()).expand();
				for (LazyValueReprTreeNode c : ((LazyValueReprTreeNode) front.getLastSegment()).getChildren()) {
					q.add(front.createChildPath(c));
				}
			} else if (this.mostSuspFields.contains(((LazyValueReprTreeNode) front.getLastSegment()).getRefPathStr())) {
				this.mostSuspTreePaths.add(front);
			}

		}

	}

}
