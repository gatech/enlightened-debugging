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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.ViewPart;

public class ExpCtrlPanelView extends ViewPart implements IPartListener2 {

	int testSubjectID;
	// String task1SubjectProgramPath;
	// String task1ProgramName;
	// String task2SubjectProgramPath;
	// String task2ProgramName;
	String textID;
	public Label task1StatusLabel;
	public Label task2StatusLabel;
	Text subjectIDText;
	// public boolean task1ended;
	// public boolean task2ended;
	Button submitButton;
	Button startTask1Button;
	Button startTask2Button;
	Button endTask1Button;
	Button endTask2Button;
	Button exportLogs;

	// Config files: task 1 name, task 1 path, task 2 name, task 2 path, output
	// path, type of task 1 (swift/traditional)
	public void readConfigFile(String id) {
		if (SwiftUtilities.taskNamesPaths == null)
			SwiftUtilities.taskNamesPaths = new String[6];
		this.testSubjectID = Integer.parseInt(id);
		File inFile = null;
		if (this.testSubjectID % 2 == 0) { // experiment group configuration
			inFile = new File("exp.config");
		} else { // control group configuration
			inFile = new File("ctrl.config");
		}
		// read task 1 project name, task 1 project path, task 2 project name,
		// and task 2 project path, and log output folder
		Scanner sc = null;
		try {
			sc = new Scanner(inFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int i = 0;
		while (sc.hasNext()) {
			SwiftUtilities.taskNamesPaths[i++] = sc.next();
		}
		sc.close();
	}

	@Override
	public void createPartControl(Composite parent) {

		GridLayout gLayout = new GridLayout(4, false);
		gLayout.verticalSpacing = 5;
		parent.setLayout(gLayout);

		Label label = new Label(parent, SWT.NULL);
		label.setText("Enter experiment ID: ");

		subjectIDText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		subjectIDText.setLayoutData(gridData);

		submitButton = new Button(parent, SWT.PUSH);
		submitButton.setText("Submit");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		submitButton.setLayoutData(gridData);
		submitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				textID = subjectIDText.getText();
				SwiftUtilities.log("Test Subject ID: " + textID);
				readConfigFile(textID);
				SwiftUtilities.subjectID = Integer.parseInt(textID);
				subjectIDText.setEditable(false);
				startTask1Button.setEnabled(true);
				startTask2Button.setEnabled(false);
				exportLogs.setEnabled(true);
				submitButton.setEnabled(false);
			}
		});

		startTask1Button = new Button(parent, SWT.PUSH);
		startTask1Button.setText("Start Task 1");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		startTask1Button.setLayoutData(gridData);
		startTask1Button.setEnabled(false);
		startTask1Button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				task1StatusLabel.setText("Task 1 Status: In progress");
				startTask1Button.setEnabled(false);
				endTask1Button.setEnabled(true);
				SwiftUtilities.log("Task 1 started");
				if (SwiftUtilities.taskNamesPaths[5].equals("traditional")) {
					startTraditionalTask();
				} else {
					startSwiftTask();
				}
			}
		});

		task1StatusLabel = new Label(parent, SWT.NONE);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		task1StatusLabel.setLayoutData(gridData);
		task1StatusLabel.setText("Task 1 Status: Not completed");

		endTask1Button = new Button(parent, SWT.PUSH);
		endTask1Button.setText("Abandon task 1");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		endTask1Button.setLayoutData(gridData);
		endTask1Button.setEnabled(false);
		endTask1Button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean abandon = MessageDialog.openQuestion(parent.getShell(), "Abandon this task",
						"Are you sure you cannot find the bug using this method and would like to abandon this task?");
				if (!abandon)
					return;
				task1StatusLabel.setText("Task 1 Status: Ended Manually");
				SwiftUtilities.log("Task 1 ended manually.");
				SwiftUtilities.task1ended = true;
				endTask1Button.setEnabled(false);
				startTask2Button.setEnabled(true);
				saveStates();
				if (SwiftUtilities.swiftTaskInProgress) {
					SwiftUtilities.swiftTaskInProgress = false;
					IEditorRegistry reg = PlatformUI.getWorkbench().getEditorRegistry();
					reg.setDefaultEditor("*.java", "org.eclipse.jdt.ui.CompilationUnitEditor");
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {

							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

							IViewPart postview = page.findView("userpoweredflplugin.poststateview");
							((PostStateView) postview).viewer.setInput(null);

							IViewPart preview = page.findView("userpoweredflplugin.prestateview");
							((PreStateView) preview).viewer.setInput(null);
						}
					});
				}
			}
		});

		startTask2Button = new Button(parent, SWT.PUSH);
		startTask2Button.setText("Start Task 2");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		startTask2Button.setLayoutData(gridData);
		startTask2Button.setEnabled(false);
		startTask2Button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				task2StatusLabel.setText("Task 2 Status: In progress");
				startTask2Button.setEnabled(false);
				endTask2Button.setEnabled(true);
				if (SwiftUtilities.taskNamesPaths[5].equals("swift")) {
					startTraditionalTask();
				} else {
					startSwiftTask();
				}
			}
		});

		task2StatusLabel = new Label(parent, SWT.NONE);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		task2StatusLabel.setLayoutData(gridData);
		task2StatusLabel.setText("Task 2 Status: Not completed");

		endTask2Button = new Button(parent, SWT.PUSH);
		endTask2Button.setText("Abandon task 2");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		endTask2Button.setLayoutData(gridData);
		endTask2Button.setEnabled(false);
		endTask2Button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean abandon = MessageDialog.openQuestion(parent.getShell(), "Abandon this task",
						"Are you sure you cannot find the bug using this method and would like to abandon this task?");
				if (!abandon)
					return;
				task2StatusLabel.setText("Task 2 Status: Ended Manually");
				SwiftUtilities.log("Task 2 ended manually.");
				SwiftUtilities.task2ended = true;
				endTask2Button.setEnabled(false);
				exportLogsProc();
				saveStates();
				if (SwiftUtilities.swiftTaskInProgress) {
					SwiftUtilities.swiftTaskInProgress = false;
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {

							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

							IViewPart postview = page.findView("userpoweredflplugin.poststateview");
							((PostStateView) postview).viewer.setInput(null);

							IViewPart preview = page.findView("userpoweredflplugin.prestateview");
							((PreStateView) preview).viewer.setInput(null);
						}
					});
				}
			}
		});

		exportLogs = new Button(parent, SWT.PUSH);
		exportLogs.setText("Export log files");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 4;
		exportLogs.setLayoutData(gridData);
		exportLogs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				exportLogsProc();
				// exportLogs.setEnabled(false);
			}
		});

		// try to load the settings
		DialogSettings settings = (DialogSettings) Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection("panelstates");
		IDialogSettings section2 = settings.getSection("utilitystates");
		SwiftUtilities.swiftTaskInProgress = false;
		SwiftUtilities.tradTaskInProgress = false;
		if (section != null && section2 != null) {
			// see if the user would like to restore
			boolean restore = MessageDialog.openQuestion(parent.getShell(), "Restore experiment session",
					"An existing experiment session has been detected. Resume session?");
			if (restore) {
				// restore subject id, task 1 and 2 status
				// section.put("subjectID", this.testSubjectID);
				this.textID = section2.get("textuserid");
				this.testSubjectID = section.getInt("subjectID");
				SwiftUtilities.log("Restored subject ID: " + Integer.toString(this.testSubjectID));
				this.subjectIDText.setText(Integer.toString(testSubjectID));
				this.task1StatusLabel.setText(section.get("task1labelstatus"));
				this.task2StatusLabel.setText(section.get("task2labelstatus"));
				this.subjectIDText.setEditable(false);

				this.submitButton.setEnabled(section.getBoolean("submitbut"));

				SwiftUtilities.task1ended = section2.getBoolean("task1ended");
				SwiftUtilities.task2ended = section2.getBoolean("task2ended");

				SwiftUtilities.log("Restoring task completion states:");
				SwiftUtilities.log("1: " + Boolean.toString(SwiftUtilities.task1ended));
				SwiftUtilities.log("2: " + Boolean.toString(SwiftUtilities.task2ended));

				if (!SwiftUtilities.task1ended) {
					this.startTask1Button.setEnabled(true);
					this.endTask1Button.setEnabled(false);
					this.task1StatusLabel.setText("Task 1 Status: Not Completed");
				} else {
					this.startTask1Button.setEnabled(section.getBoolean("startTask1But"));
					this.endTask1Button.setEnabled(section.getBoolean("endTask1But"));
				}
				if (!SwiftUtilities.task2ended) {
					if (SwiftUtilities.task1ended) {
						this.startTask2Button.setEnabled(true);
						this.endTask2Button.setEnabled(false);
						this.task2StatusLabel.setText("Task 2 Status: Not Completed");
					}
				} else {
					this.startTask2Button.setEnabled(section.getBoolean("startTask2But"));
					this.endTask2Button.setEnabled(section.getBoolean("endTask2But"));
				}
				SwiftUtilities.taskNamesPaths[0] = section2.get("task1name");
				SwiftUtilities.taskNamesPaths[1] = section2.get("task1path");
				SwiftUtilities.taskNamesPaths[2] = section2.get("task2name");
				SwiftUtilities.taskNamesPaths[3] = section2.get("task2path");
				SwiftUtilities.taskNamesPaths[4] = section2.get("outputpath");
				SwiftUtilities.taskNamesPaths[5] = section2.get("task1type");
			} else {
				textID = null;
				settings.removeSection("panelstates");
				settings.removeSection("utilitystates");
				SwiftUtilities.task1ended = false;
				SwiftUtilities.task2ended = false;
			}
		}

		// register the part listener
		getViewSite().getWorkbenchWindow().getPartService().addPartListener(this);

	}

	@Override
	public void setFocus() {
	}

	public void startTraditionalTask() {

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		// open the java perspective
		try {
			workbench.showPerspective("org.eclipse.jdt.ui.JavaPerspective", window);
		} catch (WorkbenchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			window.getActivePage().showView("userpoweredflplugin.timerview");
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DebuggingTimerView dtv = (DebuggingTimerView) window.getActivePage().findView("userpoweredflplugin.timerview");
		dtv.endManualDebuggingButton.setEnabled(true);
		dtv.abandonManualDebButton.setEnabled(true);
		SwiftUtilities.initTestProject(SwiftUtilities.taskNamesPaths[1]);
		dtv.startClock();
		SwiftUtilities.log("[Trad] Traditional debugging task started.");
		SwiftUtilities.tradTaskInProgress = true;
	}

	public void finishTraditionalTask() {
		SwiftUtilities.log("[Trad] Traditional debugging task ended.");
		SwiftUtilities.tradTaskInProgress = false;
		if (SwiftUtilities.taskNamesPaths[5].equals("traditional")) {
			SwiftUtilities.task1ended = true;
			task1StatusLabel.setText("Task 1 Status: Completed");
			startTask1Button.setEnabled(false);
			endTask1Button.setEnabled(false);
			startTask2Button.setEnabled(true);
		} else {
			SwiftUtilities.task2ended = true;
			task2StatusLabel.setText("Task 2 Status: Completed");
			startTask2Button.setEnabled(false);
			endTask2Button.setEnabled(false);
			exportLogsProc();
		}
		saveStates();
	}

	public void startSwiftTask() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		// open swift perspective
		try {
			workbench.showPerspective("userpoweredflplugin.swiftperspective", window);
		} catch (WorkbenchException e1) {
			e1.printStackTrace();
		}

		try {
			window.getActivePage().showView("userpoweredflplugin.controlview");
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}

		IWorkbenchPage page = window.getActivePage();
		ControlViewPart controlViewPart = (ControlViewPart) page.findView("userpoweredflplugin.controlview");
		controlViewPart.setParams(SwiftUtilities.taskNamesPaths[2], SwiftUtilities.taskNamesPaths[3]);
		SwiftUtilities.log("[SWIFT] Swift debugging task started.");
		SwiftUtilities.swiftTaskInProgress = true;
		controlViewPart.startClock();
		controlViewPart.startSwiftSession();
		IEditorRegistry reg = PlatformUI.getWorkbench().getEditorRegistry();
		reg.setDefaultEditor("*.java", "userpoweredflplugin.myjavaeditor");
	}

	public void finishSwiftTask() {
		SwiftUtilities.swiftTaskInProgress = false;
		IEditorRegistry reg = PlatformUI.getWorkbench().getEditorRegistry();
		reg.setDefaultEditor("*.java", "org.eclipse.jdt.ui.CompilationUnitEditor");
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

				IViewPart postview = page.findView("userpoweredflplugin.poststateview");
				((PostStateView) postview).viewer.setInput(null);

				IViewPart preview = page.findView("userpoweredflplugin.prestateview");
				((PreStateView) preview).viewer.setInput(null);
			}
		});

		SwiftUtilities.log("[SWIFT] Swift debugging task ended.");
		// System.out.println(SwiftUtilities.taskNamesPaths[5]);
		if (SwiftUtilities.taskNamesPaths[5].equals("swift")) {
			SwiftUtilities.task1ended = true;
			task1StatusLabel.setText("Task 1 Status: Completed");
			startTask1Button.setEnabled(false);
			endTask1Button.setEnabled(false);
			startTask2Button.setEnabled(true);
		} else {
			SwiftUtilities.task2ended = true;
			task2StatusLabel.setText("Task 2 Status: Completed");
			startTask2Button.setEnabled(false);
			endTask2Button.setEnabled(false);
			exportLogsProc();
		}
		saveStates();
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// remove part listener from part service
		getViewSite().getWorkbenchWindow().getPartService().removePartListener(this);
		saveStates();
	}

	public void saveStates() {
		if (textID == null)
			return;
		// SwiftUtilities.log("Saving exp control panel states");
		IDialogSettings settings = Activator.getDefault().getDialogSettings();

		// get the section of the text
		IDialogSettings section = settings.getSection("panelstates");

		// if it doesn't exist, create it
		if (section == null) {
			section = settings.addNewSection("panelstates");
		}

		// store subject id, task 1 and 2 status
		section.put("subjectID", this.testSubjectID);

		section.put("task1labelstatus", this.task1StatusLabel.getText());
		section.put("task2labelstatus", this.task2StatusLabel.getText());

		section.put("submitbut", this.submitButton.getEnabled());
		section.put("startTask1But", this.startTask1Button.getEnabled());
		section.put("endTask1But", this.endTask1Button.getEnabled());
		section.put("startTask2But", this.startTask2Button.getEnabled());
		section.put("endTask2But", this.endTask2Button.getEnabled());

		// get the utility states section
		IDialogSettings section2 = settings.getSection("utilitystates");

		// if it doesn't exist, create it
		if (section2 == null) {
			section2 = settings.addNewSection("utilitystates");
		}

		section2.put("textuserid", textID);
		section2.put("task1ended", SwiftUtilities.task1ended);
		section2.put("task2ended", SwiftUtilities.task2ended);
		section2.put("task1name", SwiftUtilities.taskNamesPaths[0]);
		section2.put("task1path", SwiftUtilities.taskNamesPaths[1]);
		section2.put("task2name", SwiftUtilities.taskNamesPaths[2]);
		section2.put("task2path", SwiftUtilities.taskNamesPaths[3]);
		section2.put("outputpath", SwiftUtilities.taskNamesPaths[4]);
		section2.put("task1type", SwiftUtilities.taskNamesPaths[5]);
	}

	public void exportLogsProc() {
		if (!SwiftUtilities.task1ended || !SwiftUtilities.task2ended) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Warning!",
					"Please complete both tasks before exporting logs.\n" + "Task 1 Completed: "
							+ Boolean.toString(SwiftUtilities.task1ended) + "\nTask 2 Completed: "
							+ Boolean.toString(SwiftUtilities.task2ended));
			return;
		}
		SwiftUtilities.log("Both tasks completed. Begin log exporting.");
		SwiftUtilities.closeLogger();
		try {
			SwiftUtilities.zipLogs(testSubjectID, SwiftUtilities.taskNamesPaths[4]);
		} catch (IOException e1) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Error!",
					"Cannot zip logs to desktop.");
			e1.printStackTrace();
		}
		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Logs exported successfully!",
				"Logs have been exported to Desktop. Thank you for participating the experiment. You may now close Eclipse.");
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// System.out.println("Exp contrl view deactivated. Save states as
		// well.");
		saveStates();
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		saveStates();
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

}
