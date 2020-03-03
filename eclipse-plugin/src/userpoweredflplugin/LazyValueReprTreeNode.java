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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.gatech.arktos.userpoweredfl.mcallrepr.ArrayRepr;
import edu.gatech.arktos.userpoweredfl.mcallrepr.MethodCallSpecialRefName;
import edu.gatech.arktos.userpoweredfl.mcallrepr.NullRepr;
import edu.gatech.arktos.userpoweredfl.mcallrepr.PrimitiveRepr;
import edu.gatech.arktos.userpoweredfl.mcallrepr.ReferenceRepr;
import edu.gatech.arktos.userpoweredfl.mcallrepr.ReflectedObjectRepr;
import edu.gatech.arktos.userpoweredfl.mcallrepr.ValueGraphNode;
import edu.gatech.arktos.userpoweredfl.mcallrepr.VoidRepr;
import edu.gatech.arktos.userpoweredfl.refpath.RefPath;
import edu.gatech.arktos.userpoweredfl.refpath.RefPath.Builder;

public class LazyValueReprTreeNode {
	public ValueGraphNode value;
	private ArrayList<LazyValueReprTreeNode> children;
	private ArrayList<LazyValueReprTreeNode> keepRootChildren;
	private Map<String, ValueGraphNode> params;
	private LazyValueReprTreeNode parent;
	private String refPathStr;
	private String info;
	public boolean isParamsList;
	public boolean expanded;
	public boolean isVirtualRoot;
	public Builder refBuilder;

	public enum FeedbackType {
		// NONE indicates no feedback is given
		// NA denotes special node types that does not accept feedbacks
		CORRECT, WRONG, IMPRE, NONE, NA
	}

	private FeedbackType fieldFeedback;

	// Constructor for virtual root node
	public LazyValueReprTreeNode(LazyValueReprTreeNode parent, String infoFromParent) {
		this.parent = parent;
		this.value = null;
		this.children = new ArrayList<>();
		this.keepRootChildren = new ArrayList<>();
		this.isParamsList = false;
		this.info = infoFromParent;
		this.refPathStr = "";
		this.refBuilder = null;
		this.fieldFeedback = FeedbackType.NA;
		this.expanded = false;
		this.isVirtualRoot = true;
	}

	// Constructor for tree nodes that do not represent parameters list
	public LazyValueReprTreeNode(LazyValueReprTreeNode parent, ValueGraphNode thisValue, String infoFromParent) {
		this.parent = parent;
		this.value = thisValue;
		this.children = new ArrayList<>();
		this.isParamsList = false;
		this.info = infoFromParent;
		this.refPathStr = null;
		if (parent.refBuilder == null) {
			this.refBuilder = new Builder();
			if (this.info.equals(MethodCallSpecialRefName.thisRef().toString())) {
				this.refBuilder.appendThisRef();
			} else if (this.info.equals(MethodCallSpecialRefName.returnValue().toString())) {
				this.refBuilder.appendReturnValueRef();
			} else if (this.info.equals(MethodCallSpecialRefName.exceptionThrown().toString())) {
				this.refBuilder.appendExceptionRef();
			}
		} else {
			this.refBuilder = new Builder(parent.refBuilder.build());
		}
		this.fieldFeedback = parent == null ? FeedbackType.NA : FeedbackType.NONE;
		this.expanded = false;
	}

	// Constructor for parameters list node
	public LazyValueReprTreeNode(LazyValueReprTreeNode parent, Map<String, ValueGraphNode> inputParams,
			String infoFromParent) {
		this.parent = parent;
		this.children = new ArrayList<>();
		this.value = null;
		this.params = inputParams;
		this.info = infoFromParent;
		this.refPathStr = "";
		this.refBuilder = null;
		this.isParamsList = true;
		this.fieldFeedback = FeedbackType.NA;
		this.isVirtualRoot = false;
		this.expanded = false;
	}

	public void setVirtualRootChildren(LazyValueReprTreeNode[] rootChildren) {
		for (LazyValueReprTreeNode c : rootChildren) {
			this.keepRootChildren.add(c);
		}
	}

	public void setRefPathStr(String s) {
		this.refPathStr = s;
	}

	public String getRefPathStr() {
		// System.out.println(this.refPathStr);
		return this.refPathStr;
	}

	public RefPath getRefPathFromBuilder() {
		if (this.refBuilder != null)
			return this.refBuilder.build();
		return null;
	}

	public FeedbackType getFeedback() {
		return this.fieldFeedback;
	}

	public void resetFeedback(FeedbackType fb) {
		this.fieldFeedback = fb;
	}

	// if the node has children, based on type of this node
	public boolean hasChildren() {
		if (this.isVirtualRoot)
			return true;
		if (this.isParamsList && this.params != null && this.params.size() > 0)
			return true;
		if (value instanceof ArrayRepr || value instanceof ReflectedObjectRepr) {
			return true;
		}
		return false;
	}

	// children must have been generated by the expand method
	public LazyValueReprTreeNode[] getChildren() {
		if (!hasChildren())
			return null;
		this.expanded = true;
		LazyValueReprTreeNode[] ans = new LazyValueReprTreeNode[children.size()];
		int ind = 0;
		for (LazyValueReprTreeNode child : children) {
			ans[ind++] = child;
		}
		return ans;
	}

	public LazyValueReprTreeNode getParent() {
		return this.parent;
	}

	public String getString() {
		if (this.isParamsList) {
			if (this.params.size() == 0)
				return "Parameters (none)";
			else
				return "Parameters";
		} else if (info == "this") {
			return info;
		}
		String ans = info == "" ? "" : (info + " ");
		return ans;
	}

	public String removePackageName(String fullName) {
		int ind = fullName.lastIndexOf(".");
		if (ind == -1) {
			return fullName;
		} else {
			return fullName.substring(ind + 1);
		}
	}

	public String getType() {
		if (this.isParamsList || this.isVirtualRoot)
			return "";
		if (this.info == "this")
			if (value != null)
				return removePackageName(((ReflectedObjectRepr) value).getType());
		if (value == null) {
			return "null";
		} else if (value instanceof PrimitiveRepr) {
			return ((PrimitiveRepr) value).getType();
		} else if (value instanceof ArrayRepr) {
			return removePackageName(((ArrayRepr) value).getType()) + "[]";
		} else if (value instanceof ReflectedObjectRepr) {
			return removePackageName(((ReflectedObjectRepr) value).getType());
		} else if (value instanceof ReferenceRepr) {
			return removePackageName(((ReferenceRepr) value).toString());
		} else if (value instanceof VoidRepr) {
			return "void";
		} else
			return "unknown type";
	}

	public String getPrettyString() {
		if (value != null && value instanceof ReflectedObjectRepr) {
			ReflectedObjectRepr robj = (ReflectedObjectRepr) value;
			if (robj.getType().equals("java.lang.String")) {
				List<ValueGraphNode> charArray = ((ArrayRepr) (robj.getFields().get("value"))).getElements();
				String ans = "";
				for (ValueGraphNode ch : charArray) {
					ans += ((PrimitiveRepr) ch).getWrappedValue().toString();
				}
				ans = StringEscapeUtils.escapeJava(ans);
				ans = "\"" + ans + "\"";
				return ans;
			}
		}
		return null;
	}
	
	public boolean canRecvPreviewFeedback() {
		if (this.value==null) {
			return false;
		}
		return this.value instanceof PrimitiveRepr || this.value instanceof NullRepr;
	}

	public String getValue() {
		if (this.isVirtualRoot || this.isParamsList || this.info == "this")
			return "";
		if (value != null && value instanceof PrimitiveRepr) {
			return ((PrimitiveRepr) value).getWrappedValue().toString();
		} else if (value instanceof ArrayRepr && ((ArrayRepr) value).getElements().size() == 0)
			return "(empty)";
		else if (this.getPrettyString() != null)
			return this.getPrettyString();
		else
			return "";
	}

	public void collapse() {
		if (!this.expanded)
			return;
		this.expanded = false;
		this.children = new ArrayList<>();
	}

	public void expand() {
		if (!hasChildren() || this.expanded) {
			return;
		}
		this.expanded = true;
		if (this.isVirtualRoot) {
			this.children = this.keepRootChildren;
			return;
		}
		if (this.isParamsList) {
			Map<String, ValueGraphNode> map = this.params;
			if (map == null)
				return;
			List<String> sortedKeys = new ArrayList<String>(map.keySet());
			Collections.sort(sortedKeys);
			for (String key : sortedKeys) {
				ValueGraphNode newChildValue = map.get(key);
				String childInfo = key;
				LazyValueReprTreeNode newChild = new LazyValueReprTreeNode(this, newChildValue, childInfo);
				newChild.setRefPathStr((this.getRefPathStr() == "" ? "" : (this.getRefPathStr() + ".")) + key);
				newChild.refBuilder.appendParamRef(childInfo);
				children.add(newChild);
			}
			return;
		}
		if (value != null && value instanceof ArrayRepr) {
			List<ValueGraphNode> elements = ((ArrayRepr) value).getElements();
			LazyValueReprTreeNode lengthChild = new LazyValueReprTreeNode(this, ((ArrayRepr) value).getLength(),
					"arrayLength");
			lengthChild.setRefPathStr((this.getRefPathStr() == "" ? "" : (this.getRefPathStr() + ".")) + "arrayLength");
			lengthChild.refBuilder.appendArrayLengthRef();
			children.add(lengthChild);
			for (int ind = 0; ind < elements.size(); ++ind) {
				String msg = "[" + ind + "]";
				LazyValueReprTreeNode newChild = new LazyValueReprTreeNode(this, elements.get(ind), msg);
				newChild.setRefPathStr((this.getRefPathStr() == "" ? "" : (this.getRefPathStr() + ".")) + msg);
				newChild.refBuilder.appendArrayIndexRef(ind);
				children.add(newChild);
			}
			return;
		} else if (value != null && value instanceof ReflectedObjectRepr) {
			Map<String, ValueGraphNode> map = ((ReflectedObjectRepr) value).getFields();
			List<String> sortedKeys = new ArrayList<String>(map.keySet());
			Collections.sort(sortedKeys);
			for (String key : sortedKeys) {
				ValueGraphNode newChildValue = map.get(key);
				String childInfo = key;
				LazyValueReprTreeNode newChild = new LazyValueReprTreeNode(this, newChildValue, childInfo);
				newChild.setRefPathStr((this.getRefPathStr() == "" ? "" : (this.getRefPathStr() + ".")) + key);
				newChild.refBuilder.appendFieldNameRef(childInfo);
				children.add(newChild);
			}
		}
	}

	@Override
	public int hashCode() {
		return this.getRefPathStr().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof LazyValueReprTreeNode))
			return false;
		if (this.isVirtualRoot || ((LazyValueReprTreeNode) other).isVirtualRoot) {
			return this.isVirtualRoot == ((LazyValueReprTreeNode) other).isVirtualRoot;
		}
		if (this.isParamsList || ((LazyValueReprTreeNode) other).isParamsList) {
			return this.isParamsList == ((LazyValueReprTreeNode) other).isParamsList;
		}
		return this.getRefPathStr().equals(((LazyValueReprTreeNode) other).getRefPathStr());
	}

	public String toString() {
		return this.getRefPathStr();
	}
}
