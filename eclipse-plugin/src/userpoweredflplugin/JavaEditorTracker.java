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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Tracks the workbench editors and to attach coverage annotation models.
 */
public class JavaEditorTracker {

	private final IWorkbench workbench;

	private IWindowListener windowListener = new IWindowListener() {
		public void windowOpened(IWorkbenchWindow window) {
			window.getPartService().addPartListener(partListener);
		}

		public void windowClosed(IWorkbenchWindow window) {
			window.getPartService().removePartListener(partListener);
		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
			// TODO Auto-generated method stub

		}
	};

	private IPartListener2 partListener = new IPartListener2() {
		public void partOpened(IWorkbenchPartReference partref) {
			annotateEditor(partref);
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			// TODO Auto-generated method stub

		}
	};

	public JavaEditorTracker(IWorkbench workbench) {
		// System.out.println("java editor tracker created");
		this.workbench = workbench;
		for (final IWorkbenchWindow w : workbench.getWorkbenchWindows()) {
			w.getPartService().addPartListener(partListener);
		}
		workbench.addWindowListener(windowListener);
		annotateAllEditors();
	}

	public void dispose() {
		workbench.removeWindowListener(windowListener);
		for (final IWorkbenchWindow w : workbench.getWorkbenchWindows()) {
			w.getPartService().removePartListener(partListener);
		}
	}

	private void annotateAllEditors() {
		for (final IWorkbenchWindow w : workbench.getWorkbenchWindows()) {
			for (final IWorkbenchPage p : w.getPages()) {
				for (final IEditorReference e : p.getEditorReferences()) {
					annotateEditor(e);
				}
			}
		}
	}

	private void annotateEditor(IWorkbenchPartReference partref) {
		IWorkbenchPart part = partref.getPart(false);
		if (part instanceof ITextEditor) {
			// System.out.println("annotating editor");
			SuspAnnotationModel.attach((ITextEditor) part);
			StyledText text = (StyledText) part.getAdapter(Control.class);
			text.addCaretListener(new MyCaretListener(text, part));
//			if (SwiftUtilities.swiftTaskInProgress) {
//				IEditorInput edInput = ((ITextEditor) part).getEditorInput();
//				IFile file = ResourceUtil.getFile(edInput);
//				IPath path2 = file.getFullPath();
//
//				IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
//				IPath fullPath = rootPath.append(path2);
//				File nioFile = fullPath.toFile();
//				if (nioFile.exists()) {
//					// nioFile.setWritable(false);
//					Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
//					// add owners permission
//					perms.add(PosixFilePermission.OWNER_READ);
//					// perms.add(PosixFilePermission.OWNER_WRITE);
//					perms.add(PosixFilePermission.OWNER_EXECUTE);
//					// add group permissions
//					perms.add(PosixFilePermission.GROUP_READ);
//					// perms.add(PosixFilePermission.GROUP_WRITE);
//					perms.add(PosixFilePermission.GROUP_EXECUTE);
//					// add others permissions
//					perms.add(PosixFilePermission.OTHERS_READ);
//					// perms.add(PosixFilePermission.OTHERS_WRITE);
//					perms.add(PosixFilePermission.OTHERS_EXECUTE);
//
//					try {
//						Files.setPosixFilePermissions(nioFile.toPath(), perms);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
		}
	}

	class MyCaretListener implements CaretListener {
		private StyledText s;
		private ITextEditor p;

		public MyCaretListener(StyledText swtStyledText, IWorkbenchPart part) {
			this.s = swtStyledText;
			this.p = (ITextEditor) part;
		}

		@Override
		public void caretMoved(CaretEvent event) {
			if (p != null) {
				IFile file = (IFile) p.getEditorInput().getAdapter(IFile.class);
				String fpath = file.getFullPath().toOSString();
				SwiftUtilities.log("File: " + fpath + "\tCaret at line: "
						+ Integer.toString(s.getLineAtOffset(event.caretOffset) + 1));
			}
		}
	}
}
