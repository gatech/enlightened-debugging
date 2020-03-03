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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class SuspAnnotationModel implements IAnnotationModel {

	/** Key used to piggyback our model to the editor's model. */
	private static final Object KEY = new Object();

	/** List of current CoverageAnnotation objects */
	private List<SuspAnnotation> annotations = new ArrayList<SuspAnnotation>(32);

	/** List of registered IAnnotationModelListener */
	private List<IAnnotationModelListener> annotationModelListeners = new ArrayList<IAnnotationModelListener>(2);

	private final ITextEditor editor;
	private final IDocument document;
	private int openConnections = 0;
	private boolean annotated = false;

	private ISuspListener suspListener = new ISuspListener() {
		public void suspChanged() {
			updateAnnotations(true);
		}
	};

	private IDocumentListener documentListener = new IDocumentListener() {
		public void documentChanged(DocumentEvent event) {
			updateAnnotations(false);
		}

		public void documentAboutToBeChanged(DocumentEvent event) {
		}
	};

	private SuspAnnotationModel(ITextEditor editor, IDocument document) {
		this.editor = editor;
		this.document = document;
		updateAnnotations(true);
		// System.out.println("SuspAnnotationModel created successfully");
	}

	/**
	 * Attaches a coverage annotation model for the given editor if the editor
	 * can be annotated. Does nothing if the model is already attached.
	 *
	 * @param editor
	 *            Editor to attach a annotation model to
	 */
	public static void attach(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		// there may be text editors without document providers (SF #1725100)
		if (provider == null)
			return;
		IAnnotationModel model = provider.getAnnotationModel(editor.getEditorInput());
		if (!(model instanceof IAnnotationModelExtension))
			return;
		IAnnotationModelExtension modelex = (IAnnotationModelExtension) model;

		IDocument document = provider.getDocument(editor.getEditorInput());

		SuspAnnotationModel suspmodel = (SuspAnnotationModel) modelex.getAnnotationModel(KEY);
		if (suspmodel == null) {
			suspmodel = new SuspAnnotationModel(editor, document);
			modelex.addAnnotationModel(KEY, suspmodel);
		}
	}

	/**
	 * Detaches the coverage annotation model from the given editor. If the
	 * editor does not have a model attached, this method does nothing.
	 *
	 * @param editor
	 *            Editor to detach the annotation model from
	 */
	public static void detach(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		// there may be text editors without document providers (SF #1725100)
		if (provider == null)
			return;
		IAnnotationModel model = provider.getAnnotationModel(editor.getEditorInput());
		if (!(model instanceof IAnnotationModelExtension))
			return;
		IAnnotationModelExtension modelex = (IAnnotationModelExtension) model;
		modelex.removeAnnotationModel(KEY);
	}

	private void updateAnnotations(boolean force) {
		final SuspHolder susps = findSuspForEditor();
		if (susps != null) {
			if (!annotated || force) {
				createAnnotations(susps);
				annotated = true;
			}
		} else {
			if (annotated) {
				clear();
				annotated = false;
			}
		}
	}

	private SuspHolder findSuspForEditor() {
		if (editor.isDirty()) {
			return null;
		}
		final IEditorInput input = editor.getEditorInput();
		if (input == null) {
			return null;
		}
		final Object element = input.getAdapter(IJavaElement.class);
		if (!hasSource((IJavaElement) element)) {
			return null;
		}
		return SwiftUtilities.suspHolderInstance;
	}

	private boolean hasSource(IJavaElement element) {
		if (element instanceof ISourceReference) {
			try {
				return ((ISourceReference) element).getSourceRange() != null;
			} catch (JavaModelException ex) {
				// we ignore this, the resource seems to have problems
			}
		}
		return false;
	}

	private void clear() {
		AnnotationModelEvent event = new AnnotationModelEvent(this);
		clear(event);
		fireModelChanged(event);
	}

	private void clear(AnnotationModelEvent event) {
		for (final SuspAnnotation ca : annotations) {
			event.annotationRemoved(ca, ca.getPosition());
		}
		annotations.clear();
	}

	private void createAnnotations(final SuspHolder linesusp) {
		AnnotationModelEvent event = new AnnotationModelEvent(this);
		clear(event);
		final int firstline = 1;
		final int lastline = document.getNumberOfLines();
		try {
			for (int l = firstline; l <= lastline; l++) {
				final ILine line = linesusp.getLine(this.editor.getEditorInput().getAdapter(IJavaElement.class), l);
				if (line.getStatus() > 0) {
					final IRegion region = document.getLineInformation(l - 1);
					final SuspAnnotation ca = new SuspAnnotation(region.getOffset(), region.getLength(), line);
					annotations.add(ca);
					event.annotationAdded(ca);
				}
			}
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
		fireModelChanged(event);
	}

	public void addAnnotationModelListener(IAnnotationModelListener listener) {
		if (!annotationModelListeners.contains(listener)) {
			annotationModelListeners.add(listener);
			fireModelChanged(new AnnotationModelEvent(this, true));
		}
	}

	public void removeAnnotationModelListener(IAnnotationModelListener listener) {
		annotationModelListeners.remove(listener);
	}

	private void fireModelChanged(AnnotationModelEvent event) {
		event.markSealed();
		if (!event.isEmpty()) {
			for (final IAnnotationModelListener l : annotationModelListeners) {
				if (l instanceof IAnnotationModelListenerExtension) {
					((IAnnotationModelListenerExtension) l).modelChanged(event);
				} else {
					l.modelChanged(this);
				}
			}
		}
	}

	public void connect(IDocument document) {
		if (this.document != document) {
			throw new IllegalArgumentException("Can't connect to different document."); //$NON-NLS-1$
		}
		for (final SuspAnnotation ca : annotations) {
			try {
				document.addPosition(ca.getPosition());
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			}
		}
		if (openConnections++ == 0) {
			SwiftUtilities.suspHolderInstance.addSuspListener(suspListener);
			document.addDocumentListener(documentListener);
		}
	}

	public void disconnect(IDocument document) {
		if (this.document != document) {
			throw new IllegalArgumentException("Can't disconnect from different document."); //$NON-NLS-1$
		}
		for (final SuspAnnotation ca : annotations) {
			document.removePosition(ca.getPosition());
		}
		if (--openConnections == 0) {
			SwiftUtilities.suspHolderInstance.removeSuspListener(suspListener);
			document.removeDocumentListener(documentListener);
		}
	}

	/**
	 * External modification is not supported.
	 */
	public void addAnnotation(Annotation annotation, Position position) {
		throw new UnsupportedOperationException();
	}

	/**
	 * External modification is not supported.
	 */
	public void removeAnnotation(Annotation annotation) {
		throw new UnsupportedOperationException();
	}

	public Iterator getAnnotationIterator() {
		return annotations.iterator();
	}

	public Position getPosition(Annotation annotation) {
		if (annotation instanceof SuspAnnotation) {
			return ((SuspAnnotation) annotation).getPosition();
		} else {
			return null;
		}
	}

}
