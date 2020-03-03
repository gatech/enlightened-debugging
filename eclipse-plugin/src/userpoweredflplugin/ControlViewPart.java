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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.objectweb.asm.Type;

import edu.gatech.arktos.userpoweredfl.FeedbackDirectedFLCore;
import edu.gatech.arktos.userpoweredfl.MethodInvocationSelection;
import edu.gatech.arktos.userpoweredfl.data.MethodInvocation;
import edu.gatech.arktos.userpoweredfl.data.MethodName;
import edu.gatech.arktos.userpoweredfl.data.SourceLocation;
import edu.gatech.arktos.userpoweredfl.exec.CaptureInvocationStates;
import edu.gatech.arktos.userpoweredfl.exec.ExternalProgramInvocation;
import edu.gatech.arktos.userpoweredfl.exec.InstrumentationJars;
import edu.gatech.arktos.userpoweredfl.exec.LaunchDebuggee;
import edu.gatech.arktos.userpoweredfl.mcallrepr.MethodCallRepr;
import edu.gatech.arktos.userpoweredfl.slicing.FieldSelectionResult;
import edu.gatech.arktos.userpoweredfl.subjectmodel.SubjectProgram;
import edu.gatech.arktos.userpoweredfl.susp.FilteredFaultLocalization;
import edu.gatech.arktos.userpoweredfl.util.Pair;

public class ControlViewPart extends ViewPart {

	Composite topContainer;
	Composite selectSubjectComp;
	Composite methodNameComp;
	Composite instructionsComp; // contains buttons when incorrect outputs are
								// indicated
	Composite incorrectOutputButtonsComp;
	Composite buttonsComp;

	Label testNameLabel;
	Label methodNameLabel;
	Label selectSubjectProgramLabel;
	Label instructionsLabel;
	Button selectSubjectButton;
	Button continueButton;
	// Button endSessionButton;

	Button mustNotInCurrButton;
	Button mightNotInCurrButton;
	Button faultInCurrButton;
	Button gotoCurrQueryButton;

	String subjectProgramPath = "";
	String srcRelativePath;
	String subjectProgramName = "";
	String lastClassName;
	String lastMethodName;
	String lastParamSignature;

	IProject prj;
	IJavaProject javaProj;
	SubjectProgram testSubject;
	CoreRunner myCoreRunner;
	// MyCaretListener listener;
	boolean hasIncorrectFeedbackButtons = false;
	boolean isCurrentInvocationALeaf = false;
	FilteredFaultLocalization<SourceLocation> lastFFL;
	Runnable timer;
	Label elapsed_time;
	TaskTimer myTimer;
	IMethod currentlyQueriedMethod;

	protected SourceDisplayHandler srcDisplayHandler;

	protected String lastMethodSignature;

	private Button startDebuggerButton;
	MethodInvocationSelection currentInvocationSelection;
	MethodInvocation mInvocNode;

	private Composite container;

	public void startClock() {
		Display.getDefault().timerExec(1000, this.timer);
	}

	public void resetClock() {
		this.myTimer = new TaskTimer(30);
		Display.getDefault().timerExec(1000, this.timer);
	}

	public void stopClock() {
		Display.getDefault().timerExec(-1, this.timer);
	}

	public void setParams(String subjectName, String subjectPath) {
		this.subjectProgramName = subjectName;
		this.subjectProgramPath = subjectPath;
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				selectSubjectProgramLabel.setText("Subject Program: " + subjectProgramName);
			}
		});
	}

	// update UI based on feedbacks given in pre- and post-field viewers
	public void updateUI() {

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		PostStateView postview = (PostStateView) page.findView("userpoweredflplugin.poststateview");
		PreStateView preview = (PreStateView) page.findView("userpoweredflplugin.prestateview");

		// no effective feedbacks have been given
		if (preview.treeItemHandler != null && preview.treeItemHandler.selectedImpossiblePreFields.size() == 0
				&& postview.treeItemHandler != null
				&& postview.treeItemHandler.selectedCorrectMostSuspFields.size() == 0
				&& postview.treeItemHandler.selectedIncorrectFields.size() == 0) {
			postview.enableComboButtons();
			// this.instructionsLabel.setText(
			// "Please give some constructive feedbacks. (Correctness of at
			// least one most suspicious post-state field, any incorrect
			// post-state field or pre-state fields, etc.)");
			this.instructionsLabel.setText("Waiting for feedbacks.");
			this.instructionsLabel.pack();
			this.topContainer.layout(true, true);
			this.continueButton.setEnabled(false);
			if (this.hasIncorrectFeedbackButtons) {
				this.hasIncorrectFeedbackButtons = false;
				this.mustNotInCurrButton.dispose();
				this.mightNotInCurrButton.dispose();
				this.faultInCurrButton.dispose();
				this.topContainer.layout(true, true);
			}
			return;
		}

		// check if impossible pre condition is given
		if (preview.treeItemHandler != null && preview.treeItemHandler.selectedImpossiblePreFields.size() > 0) {
			postview.disableComboButtons();
			if (this.hasIncorrectFeedbackButtons) {
				this.hasIncorrectFeedbackButtons = false;
				this.mustNotInCurrButton.dispose();
				this.mightNotInCurrButton.dispose();
				this.faultInCurrButton.dispose();
				this.topContainer.layout(true, true);
			}
			this.instructionsLabel.setText("Incorrect or impossible pre-state given.");
			this.instructionsLabel.pack();
			this.topContainer.layout(true, true);
			this.continueButton.setEnabled(true);
			return;
		}

		// if there is no impossible pre, should restore the buttons
		if (preview.treeItemHandler != null && preview.treeItemHandler.selectedImpossiblePreFields.size() == 0) {
			postview.enableComboButtons();
			this.continueButton.setEnabled(false);
		}

		// if incorrect fields selected
		if (postview.treeItemHandler != null) {
			if (postview.treeItemHandler.selectedIncorrectFields.size() > 0) {

				this.topContainer.layout(true, true);
				this.continueButton.setEnabled(true);
				// if the buttons haven't been generated before
				if (!this.hasIncorrectFeedbackButtons) {
					this.hasIncorrectFeedbackButtons = true;
					mustNotInCurrButton = new Button(incorrectOutputButtonsComp, SWT.RADIO);
					mustNotInCurrButton.setText("Fault is not in the current invocation.");
					mustNotInCurrButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
					mustNotInCurrButton.pack();
					mightNotInCurrButton = new Button(incorrectOutputButtonsComp, SWT.RADIO);
					mightNotInCurrButton.setText("Not sure about where the fault is yet.");
					mightNotInCurrButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
					mightNotInCurrButton.pack();
					faultInCurrButton = new Button(incorrectOutputButtonsComp, SWT.RADIO);
					faultInCurrButton.setText("Fault FOUND in the current invocation!");
					faultInCurrButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
					faultInCurrButton.pack();
					incorrectOutputButtonsComp.pack();
					this.topContainer.layout(true, true);
				}
				// if selected invocation does not include child invocations,
				// user cannot select the other 2 buttons
				if (isCurrentInvocationALeaf) {
					this.instructionsLabel.setText(
							"You\'ve marked some incorrect fields. Current invocation does not call other methods, thus the fault should be in the current method.");
					mustNotInCurrButton.setEnabled(false);
					mightNotInCurrButton.setEnabled(false);
				} else {
					mustNotInCurrButton.setEnabled(true);
					mightNotInCurrButton.setEnabled(true);
					this.instructionsLabel.setText("You\'ve marked some incorrect fields. Please choose one below:");
				}
			} else if (postview.treeItemHandler.selectedCorrectMostSuspFields.size() > 0) {
				if (this.hasIncorrectFeedbackButtons) {
					this.hasIncorrectFeedbackButtons = false;
					this.mustNotInCurrButton.dispose();
					this.mightNotInCurrButton.dispose();
					this.faultInCurrButton.dispose();
					this.topContainer.layout(true, true);
				}
				this.instructionsLabel.setText("Give more feedbacks or press continue to examine next invocation.");
				this.topContainer.layout(true, true);
				this.continueButton.setEnabled(true);
			}
		}

	}

	public void displayError(String msg) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", msg);
			}
		});
	}

	// TODO: refactor. copied from htmlview
	private String getReadableMethodName(String className, String mNameSig) {
		int nameSigSepIndex = mNameSig.indexOf('(');
		String mSimpleName = mNameSig.substring(0, nameSigSepIndex);
		String mSig = mNameSig.substring(nameSigSepIndex);
		Type retType = Type.getReturnType(mSig);
		Type[] argTypes = Type.getArgumentTypes(mSig);
		String readableName = retType.getClassName() + " " + className + "." + mSimpleName + "(";
		for (int i = 0; i < argTypes.length; ++i) {
			readableName += argTypes[i].getClassName();
			if (i != argTypes.length - 1) {
				readableName += ", ";
			}
		}
		readableName += ")";
		return readableName;
	}

	public void initTestProject() {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {

			public void run() {
				try {
					if (ResourcesPlugin.getWorkspace().getRoot().getProject("subject-program") != null)
						ResourcesPlugin.getWorkspace().getRoot().getProject("subject-program").delete(true, null);
				} catch (CoreException e4) {
					displayError("Cannot delete existing test project");
					e4.printStackTrace();
				}
				prj = ResourcesPlugin.getWorkspace().getRoot().getProject("subject-program");
				IOverwriteQuery overwriteQuery = new IOverwriteQuery() {
					public String queryOverwrite(String file) {
						return ALL;
					}
				};

				String baseDir = subjectProgramPath; // import from subject
														// program the
														// user selects

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceDescription desc = workspace.getDescription();
				boolean isAutoBuilding = desc.isAutoBuilding();
				if (isAutoBuilding != false) {
					desc.setAutoBuilding(false);
					try {
						workspace.setDescription(desc);
					} catch (CoreException e) {
						displayError(
								"Cannot disable auto building feature. Please do it manually through Eclipse preference settings.");
						e.printStackTrace();
					}
				}

				ImportOperation importOperation = new ImportOperation(prj.getFullPath(), new File(baseDir),
						FileSystemStructureProvider.INSTANCE, overwriteQuery);
				importOperation.setCreateContainerStructure(false);
				try {
					importOperation.run(new NullProgressMonitor());
				} catch (InvocationTargetException e1) {
					displayError("Incorrect import target!");
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					displayError("Import disrupted");
					e1.printStackTrace();
				}

				SwiftUtilities.log("Test subject imported successfully.");

				try {
					prj.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
				} catch (CoreException e3) {
					displayError("Cannot automatically build subject program, please build manually");
					e3.printStackTrace();
				}

				SwiftUtilities.log("Test subject built successfully.");
				javaProj = JavaCore.create(prj);
				try {
					javaProj.open(null);
				} catch (JavaModelException e2) {
					displayError("Cannot open test subject program!");
					e2.printStackTrace();
				}

				// IPath fullOutDir = null;
				// try {
				// fullOutDir = javaProj.getOutputLocation();
				// // System.out.println(fullOutDir);
				// } catch (JavaModelException e1) {
				// displayError(
				// "Cannot get default output location for the subject, please
				// set using project properties dialog");
				// e1.printStackTrace();
				// }
				//
				// int ind =
				// fullOutDir.toFile().toString().indexOf("subject-program");
				// String origOutDir =
				// fullOutDir.toFile().toString().substring(ind +
				// "subject-program".length() + 1);

				// debug info
				// SwiftUtilities.log("[SWIFT] Full output dir is: " +
				// fullOutDir.toFile().toString());
				// SwiftUtilities.log("[SWIFT] Truncated output dir is: " +
				// origOutDir);
				// SwiftUtilities
				// .log("[SWIFT] src out dir is:" +
				// Paths.get(subjectProgramPath).resolve(origOutDir).toString());
				// SwiftUtilities.log("[SWIFT] dest out dir is:"
				// +
				// Paths.get(subjectProgramPath).resolve("compiled_classes").toString());
				//
				// File origOutDirFile = new
				// File(Paths.get(subjectProgramPath).resolve(origOutDir).toString());
				// File destOutDirFile = new
				// File(Paths.get(subjectProgramPath).resolve("compiled_classes").toString());
				// if (!destOutDirFile.isDirectory())
				// try {
				// FileUtils.copyDirectory(origOutDirFile, destOutDirFile);
				// } catch (IOException e1) {
				// displayError("[SWIFT] Haven't found built classes or writing
				// to disk denied!");
				// e1.printStackTrace();
				// }
				// else {
				// SwiftUtilities.log("[SWIFT] compiled_classes already exists.
				// Skip copying");
				// }

				importOperation = new ImportOperation(prj.getFullPath(), new File(baseDir),
						FileSystemStructureProvider.INSTANCE, overwriteQuery);
				importOperation.setCreateContainerStructure(false);
				try {
					importOperation.run(new NullProgressMonitor());
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				// IFolder folder = prj.getFolder("compiled_classes");
				// classFilesPackageFragmentRoot =
				// javaProj.getPackageFragmentRoot(folder);
				// try {
				// correctCP = javaProj.getRawClasspath();
				// } catch (JavaModelException e) {
				// SwiftUtilities.log("[SWIFT] cannot get raw class paths");
				// displayError("Cannot get subject program class paths. Please
				// check");
				// e.printStackTrace();
				// }

				srcDisplayHandler = new SourceDisplayHandler(javaProj);

			}
		});
	}

	public void disableUIElements() {

		// disable UI elements to prevent users from playing around
		selectSubjectButton.setEnabled(false);
		continueButton.setEnabled(false);
	}

	public void enableUIElements() {
		selectSubjectButton.setEnabled(true);
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage("Please provide feedbacks for the current invocation.");

	}

	public void startSwiftSession() {
		if (subjectProgramPath == null)
			return;
		disableUIElements();
		IActionBars bars = getViewSite().getActionBars();

		// SwiftUtilities.log("[SWIFT] SWIFT session begins.");
		SwiftUtilities.log(subjectProgramPath);

		// compile subject program and collect compiled classes
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				bars.getStatusLineManager().setMessage("Preparing subject program, please wait ...");
				instructionsLabel.setText("Preparing subject program, please wait ...");
			}
		});
		initTestProject();

		// start a swift debugging session
		SwiftUtilities.log("[SWIFT] Swift debugging session started.");
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				bars.getStatusLineManager().setMessage("Calculating next invocation to inspect, please wait ...");
				instructionsLabel.setText("Calculating next invocation to inspect, please wait ...");
			}
		});

		// Let user select the subject program path
		testSubject = null;
		int hashedTrace = subjectProgramPath.hashCode() % Integer.MAX_VALUE;
		try {
			testSubject = SubjectProgram.openSubjectProgram(Paths.get(subjectProgramPath),
					Paths.get("./trace/" + hashedTrace));
		} catch (IOException ee) {
			displayError("Error when opening subject program. Please remove traces/" + hashedTrace
					+ " directory and try again!");
			ee.printStackTrace();
		}

		myCoreRunner = new CoreRunner(testSubject);
		new Thread(myCoreRunner).start();
	}

	@Override
	public void createPartControl(Composite grand) {
		Shell shell = grand.getShell();
		// shell.setMinimumSize(400, 660);
		container = new Composite(grand, SWT.NONE);
		container.setLayout(new FillLayout());

		final ScrolledComposite sc = new ScrolledComposite(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setLayout(new FillLayout());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		final Composite parent = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();

		sc.setContent(parent);

		sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = sc.getClientArea();
				sc.layout(true, true);
				sc.setMinSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		this.topContainer = container;
		GridLayout gLayout = new GridLayout(3, false);
		gLayout.verticalSpacing = 7;
		parent.setLayout(gLayout);

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
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
				if (SwiftUtilities.taskNamesPaths[5].equals("swift") && SwiftUtilities.task1ended) {
					return;
				}
				if (!SwiftUtilities.taskNamesPaths[5].equals("swift") && SwiftUtilities.task2ended) {
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

		// create select subject composite
		selectSubjectComp = new Composite(parent, SWT.BORDER);
		selectSubjectComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		selectSubjectComp.setLayout(new GridLayout(2, false));

		methodNameComp = new Composite(parent, SWT.BORDER);
		methodNameComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 2));
		methodNameComp.setLayout(new GridLayout(2, false));

		instructionsComp = new Composite(parent, SWT.BORDER);
		instructionsComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 4));
		instructionsComp.setLayout(new GridLayout(1, false));

		instructionsLabel = new Label(instructionsComp, SWT.WRAP);
		// instructionsLabel.setText("Please select a subject to continue.");
		instructionsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		incorrectOutputButtonsComp = new Composite(instructionsComp, SWT.NONE);
		incorrectOutputButtonsComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		incorrectOutputButtonsComp.setLayout(new GridLayout(1, false));

		buttonsComp = new Composite(instructionsComp, SWT.NONE);
		buttonsComp.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 3, 1));
		buttonsComp.setLayout(new GridLayout(3, false));

		// Draw the UI
		subjectProgramPath = "                                                          ";
		selectSubjectProgramLabel = new Label(selectSubjectComp, SWT.NONE);
		selectSubjectProgramLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		selectSubjectProgramLabel.setText("                                                            ");

		selectSubjectButton = new Button(selectSubjectComp, SWT.PUSH);
		selectSubjectButton.setText("Restart task");

		selectSubjectButton.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, true, false, 1, 1));
		selectSubjectButton.pack();
		selectSubjectButton.setEnabled(false);
		selectSubjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean restart = MessageDialog.openQuestion(parent.getShell(), "Restart Swift task",
						"All progress you made with this task will be lost. Proceed to restart?");
				if (!restart)
					return;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						IViewPart postview = page.findView("userpoweredflplugin.poststateview");
						((PostStateView) postview).viewer.setInput(null);
						IViewPart preview = page.findView("userpoweredflplugin.prestateview");
						((PreStateView) preview).viewer.setInput(null);
					}
				});
				resetClock();
				SwiftUtilities.log("[SWIFT] SWIFT task restarted.");
				SwiftUtilities.log(subjectProgramPath);
				myCoreRunner = null;
				startSwiftSession();
			}
		});

		// Label label;

		label = new Label(methodNameComp, SWT.NONE);
		label.setText("Method being queried:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		gotoCurrQueryButton = new Button(methodNameComp, SWT.PUSH);
		gotoCurrQueryButton.setText("Method Code");
		gotoCurrQueryButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		gotoCurrQueryButton.pack();
		gotoCurrQueryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (lastFFL != null)
					srcDisplayHandler.displaySourceForInvocation(lastFFL, lastClassName, lastMethodName,
							lastMethodSignature, false);
				else {
					MessageDialog.openInformation(shell, "Notice", "There is no method being queried.");
				}
			}
		});

		methodNameLabel = new Label(methodNameComp, SWT.NONE);
		methodNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		startDebuggerButton = new Button(methodNameComp, SWT.PUSH);
		startDebuggerButton.setText("Debug Method");
		startDebuggerButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		startDebuggerButton.pack();
		startDebuggerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (currentInvocationSelection == null) {
					MessageDialog.openError(parent.getShell(), "No invocation available",
							"No method invocation is available at this point. Please try again after a new query is given.");
					return;
				}

				int methodOffset;
				int methodFirstLine = 0;
				int bodyFirstLine = 0;
				try {
					methodOffset = currentlyQueriedMethod.getSourceRange().getOffset()
							+ (currentlyQueriedMethod.getJavadocRange() != null
									? currentlyQueriedMethod.getJavadocRange().getLength() : 0);
					IResource resource = currentlyQueriedMethod.getCompilationUnit().getCorrespondingResource();
					IDocumentProvider provider = new TextFileDocumentProvider();
					provider.connect(resource);
					IDocument document = provider.getDocument(resource);
					methodFirstLine = document.getLineOfOffset(methodOffset) + 1;
					bodyFirstLine = methodFirstLine;

				} catch (JavaModelException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (BadLocationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				Set<SourceLocation> stmtSet = mInvocNode.getStatementsExecCountMap().keySet();
				int minLineNum = Integer.MAX_VALUE;
				String[] pathNames = ((PackageFragment) currentlyQueriedMethod.getCompilationUnit().getParent()).names;
				String fullNameString = String.join("/", pathNames) + "/"
						+ currentlyQueriedMethod.getCompilationUnit().getElementName();
				for (SourceLocation srcloc : stmtSet) {
					if (srcloc.getSourceFile().equals(fullNameString)) {
						if (srcloc.getLineNumber() > bodyFirstLine && srcloc.getLineNumber() < minLineNum) {
							minLineNum = srcloc.getLineNumber();
						}
					}
				}

				if (LaunchDebuggee.getLivingDebuggeeProcess() != null) {
					boolean restartDebug = MessageDialog.openQuestion(parent.getShell(), "Restart Method Debugging",
							"Current running threads will be terminated and a new debugger will be launched. Continue?");
					if (!restartDebug)
						return;
					try {
						LaunchDebuggee.getLivingDebuggeeProcess().destroyForcibly().waitFor();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
				debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW,
						MessageDialogWithToggle.ALWAYS);

				LaunchDebuggee launcher = new LaunchDebuggee(testSubject);
				int debugPort = 0;
				try {
					debugPort = launcher.launch(currentInvocationSelection);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println("Debugging port is: " + Integer.toString(debugPort));
				ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
				ILaunchConfigurationType type = manager
						.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);

				ILaunchConfigurationWorkingCopy remoteDebugConfig = null;
				try {
					remoteDebugConfig = type.newInstance(null, "remote debug");
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// Set project that contains sources of a remote program
				remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "subject-program");

				// Set JVM debugger connection parameters
				Map<String, String> connectionParameters = new HashMap<String, String>();
				connectionParameters.put("hostname", "localhost");
				connectionParameters.put("port", Integer.toString(debugPort));
				remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP,
						connectionParameters);
				remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
						"org.eclipse.jdt.launching.socketAttachConnector");
				remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, true);

				IBreakpoint existing;
				try {
					existing = JDIDebugModel.lineBreakpointExists(prj, lastClassName, minLineNum);
					if (existing != null) {
						DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(existing, true);
					}
				} catch (CoreException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				}

				IJavaBreakpoint breakpoint = null;
				try {
					breakpoint = JDIDebugModel.createLineBreakpoint(prj, lastClassName, minLineNum, -1, -1, 0, true,
							null);
					// breakpoint =
					// JDIDebugModel.createMethodEntryBreakpoint(prj,
					// lastClassName, lastMethodName,
					// lastMethodSignature, 0, -1, -1, 0, true, null);
					breakpoint.addBreakpointListener("userpoweredflplugin.methodbreakpointlistener");
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				IBreakpointManager bpmgr = DebugPlugin.getDefault().getBreakpointManager();

				try {
					bpmgr.addBreakpoint(breakpoint);
				} catch (CoreException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

				IJavaBreakpoint endingbp = null;
				try {
					endingbp = JDIDebugModel.createExceptionBreakpoint(prj, "instr.callback.TrapTargetInvocationSignal",
							true, true, true, true, null);
					endingbp.addBreakpointListener("userpoweredflplugin.exceptionbreakpointlistener");
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				ILaunch myLaunch = null;
				try {
					myLaunch = DebugUITools.buildAndLaunch(remoteDebugConfig, ILaunchManager.DEBUG_MODE,
							new NullProgressMonitor());
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				IDebugTarget mainTarget = myLaunch.getDebugTarget();
				try {
					mainTarget.resume();
				} catch (DebugException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});

		continueButton = new Button(buttonsComp, SWT.PUSH);
		continueButton.setText("Continue");
		continueButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		continueButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				// user has given some incorrect field feedbacks but didn't
				// select any of the button
				if (hasIncorrectFeedbackButtons && !mustNotInCurrButton.getSelection()
						&& !mightNotInCurrButton.getSelection() && !faultInCurrButton.getSelection()) {
					Shell shell = Display.getDefault().getActiveShell();
					MessageDialog.openInformation(shell, "Warning",
							"You have marked some fields in the post-state view as wrong. Please choose the most appropriate answer among the radio buttons on the control panel.");
					return;
				}
				lastFFL = null;
				lastClassName = null;
				lastMethodName = null;
				lastMethodSignature = null;
				currentInvocationSelection = null;
				currentlyQueriedMethod = null;
				SwiftUtilities.suspHolderInstance.setInvalid();
				if (LaunchDebuggee.getLivingDebuggeeProcess() != null) {
					try {
						LaunchDebuggee.getLivingDebuggeeProcess().destroyForcibly().waitFor();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

				// user claims to have found the fault in current invocation
				if (hasIncorrectFeedbackButtons && faultInCurrButton.getSelection()) {
					stopClock();
					SwiftUtilities.log("[SWIFT] User claims to have found the fault in the current invocation.");
					Shell shell = Display.getDefault().getActiveShell();
					MessageDialog.openInformation(shell, "Congratulations!",
							"You indicated that the fault has been found.\nSwift debugging task concluded.");
					if (myCoreRunner != null) {
						myCoreRunner.finished = true;
						synchronized (myCoreRunner) {
							myCoreRunner.notifyAll();
						}
					}
					disableUIElements();
					selectSubjectButton.setEnabled(false);

					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					// SwiftUtilities.task2ended = true;
					try {
						page.showView("userpoweredflplugin.expctrlview");
					} catch (PartInitException e1) {
						e1.printStackTrace();
					}

					ExpCtrlPanelView expCtrlViewPart = (ExpCtrlPanelView) page
							.findView("userpoweredflplugin.expctrlview");

					expCtrlViewPart.finishSwiftTask();
					return;
				}

				System.out.println("Control view before incorporating wrong feedbacks");

				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						// give incorrect output values feedbacks
						System.out.println("Control view before incorporating wrong feedbacks");
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						PostStateView postview = (PostStateView) page.findView("userpoweredflplugin.poststateview");
						if (postview.treeItemHandler != null && hasIncorrectFeedbackButtons)
							postview.treeItemHandler.giveIncorrectOutputFeedbacks(mustNotInCurrButton.getSelection());
						System.out.println("Control view after incorporating wrong feedbacks");

						if (myCoreRunner != null) {
							disableUIElements();
							SwiftUtilities.log("User goes to the next invocation.");
							IActionBars bars = getViewSite().getActionBars();
							bars.getStatusLineManager()
									.setMessage("Calculating next invocation to inspect, please stand by ...");
							instructionsLabel.setText("Calculating next invocation to inspect, please stand by ...");
							synchronized (myCoreRunner) {
								myCoreRunner.notifyAll();
							}
						}
					}
				});

			}
		});

		selectSubjectComp.pack();
		methodNameComp.pack();
		instructionsComp.pack();

		disableUIElements();
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage("Please start the experiment using the Experiment Control Panel");
		instructionsLabel.setText("Please start the experiment using the Experiment Control Panel");

		topContainer.layout(true, true);

		// Set framework classpath
		// this is completed before the listeners to the buttons are used
		ExternalProgramInvocation.setFrameworkClasspathString(SwiftUtilities.getSubLibPaths());
		InstrumentationJars.setCallbackJarPath(Paths.get(SwiftUtilities.getWorkingDir() + "/swiftlibs/callback.jar"));
		InstrumentationJars.setInstrumenterJarPath(Paths.get(SwiftUtilities.getWorkingDir() + "/swiftlibs/iagent.jar"));

	}

	@Override
	public void setFocus() {
	}

	public class CoreRunner implements Runnable {
		FeedbackDirectedFLCore fbFLCore;
		private SubjectProgram testSubject;
		MethodCallRepr invocPostStates;
		MethodCallRepr invocPreStates;
		boolean finished;

		public CoreRunner(SubjectProgram subjectProgram) {
			this.testSubject = subjectProgram;
			this.finished = false;
		}

		@Override
		public void run() {
			// while the user has not claimed to have found the fault, looping
			while (!this.finished) {
				try {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							Shell shell = Display.getDefault().getActiveShell();
							Cursor cursor = Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT);
							if (shell != null)
								shell.setCursor(cursor);
						}
					});
					if (fbFLCore == null) {
						// SwiftUtilities.log("[SWIFT] Trying to create
						// fbflcore");
						fbFLCore = new FeedbackDirectedFLCore(testSubject);
					}
					// SwiftUtilities.log("[SWIFT] fbflcore exists or created
					// successfully");
					FieldSelectionResult query = fbFLCore.selectNextInvocationForFeedback();
					if (query == null) {
						SwiftUtilities.log("[SWIFT] generated query is null");
						break;
					}
					// SwiftUtilities.log("[SWIFT] query given");
					MethodInvocationSelection selectedInvocation = query.getInvocation();
					currentInvocationSelection = selectedInvocation;
					mInvocNode = fbFLCore.getMethodInvocationNode(selectedInvocation);
					List<MethodInvocation> childInvocs = mInvocNode.getEnclosedInvocations();
					if (childInvocs == null || childInvocs.size() == 0) {
						SwiftUtilities.log("[SWIFT] current invocation is a leaf, user should find the fault");
						isCurrentInvocationALeaf = true;
					} else {
						SwiftUtilities.log("[SWIFT] current invocation is not a leaf");
						isCurrentInvocationALeaf = false;
					}
					// SwiftUtilities.log("[SWIFT] selected invocation got");
					invocPostStates = null;
					invocPreStates = null;
					CaptureInvocationStates statesSnapshotter = null;
					try {
						statesSnapshotter = new CaptureInvocationStates(testSubject);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					// SwiftUtilities.log("[SWIFT] capture invocation states
					// created successfully");
					try {
						invocPostStates = statesSnapshotter
								.getInvocationStates(selectedInvocation.getTestName(),
										selectedInvocation.getMethodName(), selectedInvocation.getInvocationIndex())
								.getPostState();
						invocPreStates = statesSnapshotter
								.getInvocationStates(selectedInvocation.getTestName(),
										selectedInvocation.getMethodName(), selectedInvocation.getInvocationIndex())
								.getPreState();
					} catch (IOException e1) {

						e1.printStackTrace();
					}
					// SwiftUtilities.log("[SWIFT] pre and post states
					// obtained");

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {

							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

							IViewPart postview = page.findView("userpoweredflplugin.poststateview");
							((PostStateView) postview).createTreeItemHandler(fbFLCore, query);
							((PostStateView) postview).setState(invocPostStates);

							IViewPart preview = page.findView("userpoweredflplugin.prestateview");
							((PreStateView) preview).createTreeItemHandler(fbFLCore, query);
							((PreStateView) preview).setState(invocPreStates);

							MethodName methodName = selectedInvocation.getMethodName();
							String methodNameStr = methodName.getMethodName();
							String className = methodName.getClassName();
							String methodSignature = methodName.getMethodSignature();
							Type[] paramTypes = Type.getArgumentTypes(methodSignature);
							String[] paramSignatures = new String[paramTypes.length];
							int i = 0;
							for (Type pType : paramTypes) {
								paramSignatures[i++] = pType.getDescriptor();
								paramSignatures[i - 1] = paramSignatures[i - 1].replace('/', '.');
							}
							FilteredFaultLocalization<SourceLocation> ffl = FilteredFaultLocalization
									.filteredSrcLocFlByInvocationCov(fbFLCore.getCurrentFlResult(),
											fbFLCore.getMethodInvocationNode(selectedInvocation));
							SwiftUtilities.suspHolderInstance.updateSuspMap(ffl.getSuspiciousness());
							currentlyQueriedMethod = srcDisplayHandler.getMethodInstance(className, methodNameStr,
									methodSignature);

							String readableMethodName = getReadableMethodName(methodNameStr, methodSignature);
							methodNameLabel.setText(readableMethodName);

							lastFFL = ffl;
							lastClassName = className;
							lastMethodName = methodNameStr;
							lastMethodSignature = methodSignature;

							String invocationStr = query.getInvocation().toString();
							SwiftUtilities.log("[SWIFT] Invocation: " + invocationStr);
							enableUIElements();
							updateUI();
							boolean success = srcDisplayHandler.displayJavaDocForMethod(className, methodNameStr,
									methodSignature);

							if (lastFFL != null)
								srcDisplayHandler.displaySourceForInvocation(lastFFL, lastClassName, lastMethodName,
										lastMethodSignature, false);
							else {
								MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Notice",
										"There is no method being queried.");
							}

						}
					});
					// until feedback is given, the core process will be
					// suspended
					synchronized (this) {
						try {
							this.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				} catch (Exception e) {
					displayError("Swift internal error!");
					SwiftUtilities.log("[SWIFT] swift internal error caught");
					e.printStackTrace();
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							selectSubjectButton.setEnabled(true);
							instructionsLabel.setText("Please restart this task from the SWIFT control panel.");
						}
					});
					break;
				}
			}
			// swift cannot select more invocations, could only display current
			// unfiltered fl results
			if (LaunchDebuggee.getLivingDebuggeeProcess() != null) {
				try {
					LaunchDebuggee.getLivingDebuggeeProcess().destroyForcibly().waitFor();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			if (!finished) {
				SwiftUtilities.log("[SWIFT] not finished but can select no more invocs");
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						// displaySourceForInvocation(fbFLCore.getCurrentFlResult(),
						// lastClassName, lastMethodName,
						// lastParamSignatures, true);
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Warning!",
								"Swift cannot find more suspicious methods.\nPlease restart task or manually end task with the Experiment Control Panel.");
						methodNameLabel.setText("No more suspicious methods.");
						selectSubjectButton.setEnabled(true);
					}
				});
			}

		}

	}

}
