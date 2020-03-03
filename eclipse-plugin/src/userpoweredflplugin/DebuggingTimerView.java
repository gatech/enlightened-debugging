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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.ViewPart;

import edu.gatech.arktos.userpoweredfl.util.Pair;

public class DebuggingTimerView extends ViewPart {
	Button endManualDebuggingButton;
	Button abandonManualDebButton;
	Label elapsed_time;
	TaskTimer myTimer;
	Runnable timer;

	public void startClock() {
		Display.getDefault().timerExec(1000, this.timer);
	}

	@Override
	public void createPartControl(Composite parent) {
		Shell shell = parent.getShell();

		// Draw the UI
		GridLayout gridlayout = new GridLayout(1, false);
		parent.setLayout(gridlayout);

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		label.setText("Time elapsed:");
		elapsed_time = new Label(parent, SWT.NONE);
		elapsed_time.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		elapsed_time.setText("00:00");
		elapsed_time.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		this.myTimer = new TaskTimer(30);
		timer = new Runnable() {
			public void run() {
				if (elapsed_time.isDisposed())
					return;
				if (SwiftUtilities.taskNamesPaths[5].equals("swift") && SwiftUtilities.task2ended) {
					return;
				}
				if (!SwiftUtilities.taskNamesPaths[5].equals("swift") && SwiftUtilities.task1ended) {
					return;
				}
				Pair<Integer, Integer> p = myTimer.increment();
				elapsed_time.setText(String.format("%1$02d:%2$02d", p.getFirst(), p.getSecond()));
				if (myTimer.hasExceeded()) {
					elapsed_time.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				}
				Display.getDefault().timerExec(1000, this);
			}
		};

		endManualDebuggingButton = new Button(parent, SWT.PUSH);
		endManualDebuggingButton.setText("Bug found");
		endManualDebuggingButton.setEnabled(false);
		GridData gdata = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gdata.widthHint = 180;
		endManualDebuggingButton.setLayoutData(gdata);
		endManualDebuggingButton.pack();
		endManualDebuggingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				boolean bugfound = MessageDialog.openQuestion(parent.getShell(), "Confirmation",
						"Are you sure you have found the bug using this approach?");
				if (!bugfound)
					return;
				Display.getDefault().timerExec(-1, timer);
				SwiftUtilities.log("[Trad] User claims to have found the bug in the traditional debugging session");

				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

				// open swift perspective
				try {
					workbench.showPerspective("userpoweredflplugin.swiftperspective", window);
				} catch (WorkbenchException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					window.getActivePage().showView("userpoweredflplugin.expctrlview");
				} catch (PartInitException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				IWorkbenchPage page = window.getActivePage();
				ExpCtrlPanelView expCtrlViewPart = (ExpCtrlPanelView) page.findView("userpoweredflplugin.expctrlview");
				expCtrlViewPart.finishTraditionalTask();
			}
		});

		abandonManualDebButton = new Button(parent, SWT.PUSH);
		abandonManualDebButton.setText("Cannot find the bug");
		abandonManualDebButton.setEnabled(false);
		gdata = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gdata.widthHint = 150;
		abandonManualDebButton.setLayoutData(gdata);
		abandonManualDebButton.pack();
		abandonManualDebButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean abandon = MessageDialog.openQuestion(parent.getShell(), "Abandon this task",
						"Are you sure you cannot find the bug and would like to abandon this task?");
				if (!abandon)
					return;
				Display.getDefault().timerExec(-1, timer);
				SwiftUtilities.log("[Trad] User cannot find bug the traditional debugging session");

				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

				// open swift perspective
				try {
					workbench.showPerspective("userpoweredflplugin.swiftperspective", window);
				} catch (WorkbenchException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					window.getActivePage().showView("userpoweredflplugin.expctrlview");
				} catch (PartInitException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				IWorkbenchPage page = window.getActivePage();
				ExpCtrlPanelView expCtrlViewPart = (ExpCtrlPanelView) page.findView("userpoweredflplugin.expctrlview");
				expCtrlViewPart.finishTraditionalTask();

			}
		});

	}

	@Override
	public void setFocus() {
	}

}
