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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;

/**
 * Internal utility to select methods by their binary signature. For performance
 * optimization matching is performed in two steps, where the first step should
 * quickly identify methods in most situations: Identification by name and
 * parameter count. Only if the first step does fails to identify a method
 * unambiguously the parameter types are resolved in a second step.
 * 
 * Modified from EclEmma source
 */
public class MethodLocator {

	/** Index on methods by name and parameter count. */
	private final Map<String, IMethod> indexParamCount = new HashMap<String, IMethod>();

	/**
	 * For the keys in this set there are multiple overloaded methods with the
	 * same name and the more expensive signature resolver must be used.
	 */
	private final Set<String> ambiguous = new HashSet<String>();

	/** Index on methods by name and parameter signature. */
	private final Map<String, IMethod> indexParamSignature = new HashMap<String, IMethod>();

	private final IType type;

	/**
	 * Initializes a new locator for method search within the given type.
	 *
	 * @param type
	 *            type to search methods in
	 * @throws JavaModelException
	 */
	public MethodLocator(final IType type) throws JavaModelException {
		this.type = type;
		for (final IMethod m : type.getMethods()) {
			addToIndex(m);
		}
	}

	/**
	 * Searches for the method with the given binary name.
	 *
	 * @param name
	 *            binary method name
	 * @param signature
	 *            binary method signature
	 * @return method or <code>null</code>
	 */
	public IMethod findMethod(String name, String signature) {
		if ("<init>".equals(name)) { //$NON-NLS-1$
			name = type.getElementName();
		}
		final String paramCountKey = createParamCountKey(name, signature);
		if (ambiguous.contains(paramCountKey)) {
			return indexParamSignature.get(createParamSignatureKey(name, signature));
		} else {
			return indexParamCount.get(paramCountKey);
		}

	}

	public JavadocBrowserInformationControlInput getMethodInfo(IMethod method) {
		ITypeRoot editorInputElement = this.type.getCompilationUnit();
		if (editorInputElement == null)
			return null;
		IJavaElement[] elements = new IJavaElement[] { method };
		return JavadocHover.getHoverInfo(elements, null, null, null);
	}

	private void addToIndex(final IMethod method) throws JavaModelException {
		final String paramCountKey = createParamCountKey(method);
		if (ambiguous.contains(paramCountKey)) {
			indexParamSignature.put(createParamSignatureKey(method), method);
		} else {
			final IMethod existing = indexParamCount.get(paramCountKey);
			if (existing == null) {
				indexParamCount.put(paramCountKey, method);
			} else {
				ambiguous.add(paramCountKey);
				indexParamSignature.put(createParamSignatureKey(existing), existing);
				indexParamSignature.put(createParamSignatureKey(method), method);
			}
		}
	}

	private String createParamCountKey(final IMethod method) {
		return method.getElementName() + "@" + method.getParameterTypes().length; //$NON-NLS-1$
	}

	private String createParamCountKey(final String name, final String fullSignature) {
		return name + "@" + Signature.getParameterCount(fullSignature); //$NON-NLS-1$
	}

	private String createParamSignatureKey(final IMethod method) throws JavaModelException {
		return method.getElementName() + "#" //$NON-NLS-1$
				+ SignatureResolver.getParameters(method);
	}

	private String createParamSignatureKey(final String name, final String fullSignature) {
		return name + "#" + SignatureResolver.getParameters(fullSignature); //$NON-NLS-1$
	}

}
