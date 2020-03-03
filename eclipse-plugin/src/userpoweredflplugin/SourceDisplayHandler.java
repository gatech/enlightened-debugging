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

import java.util.TreeMap;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.infoviews.JavadocView;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenAttachedJavadocAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.gatech.arktos.userpoweredfl.data.SourceLocation;
import edu.gatech.arktos.userpoweredfl.susp.FaultLocalization;

public class SourceDisplayHandler {

	IJavaProject javaProj;
	protected JavaEditor jedit;
	protected TreeMap<Integer, Double> outerdict;
	IMethod myMethod;
	MethodLocator mlocator;

	public SourceDisplayHandler(IJavaProject proj) {
		this.javaProj = proj;
	}

	public IMethod getMethodInstance(String className, String methodName, String methodSignature) {
		IType myType = null;
		try {
			myType = this.javaProj.findType(className);
		} catch (JavaModelException e1) {
			// displayError("Cannot find the class returned by swift.");
			e1.printStackTrace();
		}
		mlocator = null;
		try {
			mlocator = new MethodLocator(myType);
		} catch (JavaModelException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (mlocator == null) {
			return null;
		}
		myMethod = mlocator.findMethod(methodName, methodSignature);
		return myMethod;
	}

	public boolean displayJavaDocForMethod(String className, String methodName, String methodSignature) {
		myMethod = getMethodInstance(className, methodName, methodSignature);
		if (myMethod == null) {
			System.out.println("the method cannot be located");
			return false;
		}
		JavadocBrowserInformationControlInput javadocinfo = mlocator.getMethodInfo(myMethod);
		if (javadocinfo == null)
			return false;
		// System.out.println(javadocinfo);
		try {

			JavadocView view = (JavadocView) JavaPlugin.getActivePage().showView(JavaUI.ID_JAVADOC_VIEW);
			view.setInput(javadocinfo);
			
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void displaySourceForInvocation(FaultLocalization<SourceLocation> fl, String className, String methodName,
			String methodSignature, boolean displayHighest) {
		myMethod = getMethodInstance(className, methodName, methodSignature);

		if (myMethod == null) {
			System.out.println("the method cannot be located");
			return;
		}

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IEditorPart editor = null;
				try {
					editor = JavaUI.openInEditor((IJavaElement) myMethod);
				} catch (PartInitException e1) {
					e1.printStackTrace();
				} catch (JavaModelException e1) {
					// displayError("Cannot open the found class in java
					// editor!");
					e1.printStackTrace();
				}
//				ITextEditor txtEditor = (ITextEditor) editor;
//				OpenAttachedJavadocAction openAttachedJavadocAction = new OpenAttachedJavadocAction(editor.getSite());
//				openAttachedJavadocAction.run(txtEditor.getSelectionProvider().getSelection());
			}
		});
	}
}
