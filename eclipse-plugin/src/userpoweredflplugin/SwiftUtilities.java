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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import edu.gatech.arktos.userpoweredfl.exec.LaunchDebuggee;

public class SwiftUtilities {

	public static Logger logger;
	// public static boolean task1started;
	// public static boolean task2started;
	public static boolean task1ended;
	public static boolean task2ended;
	public static String[] taskNamesPaths;
	public static boolean subjectIDEntered;
	public static boolean swiftTaskInProgress;
	public static boolean tradTaskInProgress;
	static IProject prj;
	static IJavaProject javaProj;
	public static int subjectID;
	public static SuspHolder suspHolderInstance;

	public static void initTestProject(String subjectProjPath) {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {

			public void run() {
				// your code

				SwiftUtilities.log("[Trad] subject project path is: " + subjectProjPath);
				try {
					if (ResourcesPlugin.getWorkspace().getRoot().getProject("subject-program") != null)
						ResourcesPlugin.getWorkspace().getRoot().delete(true, null);
				} catch (CoreException e4) {
					e4.printStackTrace();
				}
				prj = ResourcesPlugin.getWorkspace().getRoot().getProject("subject-program");
				IOverwriteQuery overwriteQuery = new IOverwriteQuery() {
					public String queryOverwrite(String file) {
						return ALL;
					}
				};

				String baseDir = subjectProjPath; // import from subject program
													// the
													// user selects

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceDescription desc = workspace.getDescription();
				boolean isAutoBuilding = desc.isAutoBuilding();
				if (isAutoBuilding != false) {
					desc.setAutoBuilding(false);
					try {
						workspace.setDescription(desc);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}

				ImportOperation importOperation = new ImportOperation(prj.getFullPath(), new File(baseDir),
						FileSystemStructureProvider.INSTANCE, overwriteQuery);
				importOperation.setCreateContainerStructure(false);
				try {
					importOperation.run(new NullProgressMonitor());
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				SwiftUtilities.log("[Trad] Test subject imported successfully.");

				try {
					prj.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
				} catch (CoreException e3) {
					e3.printStackTrace();
				}

				SwiftUtilities.log("[Trad] Test subject built successfully.");
				javaProj = JavaCore.create(prj);
				try {
					javaProj.open(null);
				} catch (JavaModelException e2) {
					e2.printStackTrace();
				}

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
			}
		});

	}

	public static void init() {
		DateFormat df = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_");
		Date dateobj = new Date();
		String currTime = df.format(dateobj);
		// System.out.println(df.format(dateobj));
		logger = Logger.getLogger("SwiftLog");
		suspHolderInstance = new SuspHolder();
		FileHandler fh;
		// task1started = false;
		// task2started = false;
		// task1ended = false;
		// task2ended = false;
		subjectIDEntered = false;
		taskNamesPaths = new String[6];
		try {

			// This block configure the logger with handler and formatter
			File f = new File(getWorkingDir() + "/logs");
			if (!f.exists())
				f.mkdir();
			fh = new FileHandler(getWorkingDir() + "/logs/" + currTime + "swiftlog");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			// the following statement is used to log any messages
			logger.info("Swift plugin logger initializing.");

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String getWorkingDir() {
		return System.getProperty("user.dir");
	}

	public static String getSubLibPaths() {
		String ans = "";
		String currentPath = getWorkingDir() + "/libs/";
		File folder = new File(currentPath);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				ans += currentPath + listOfFiles[i].getName() + File.pathSeparatorChar;
			}
		}
		// String ans = getWorkingDir() + "/libs/*";
		logger.info("Subject program external libs: " + ans);
		// System.out.println(ans);
		return ans;
	}

	public static void log(String info) {
		logger.info(info);
	}

	public static void closeLogger() {
		for (Handler h : logger.getHandlers()) {
			h.close(); // must call h.close or a .LCK file will remain.
		}
	}

	public static void zipLogs(int subjectID, String outputFolderRaw) throws IOException {
		Path path = Paths.get(outputFolderRaw);
		String zipFileName = path.resolve("Experiment_log.zip").toString();
		// log("Writing to: " + zipFileName);
		Path workdir = Paths.get(getWorkingDir());
		File dirObj = new File(workdir.resolve("logs").toString());
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
		// log("Creating : " + zipFileName);
		addDir(dirObj, out);
		out.close();
	}

	public static void addDir(File dirObj, ZipOutputStream out) throws IOException {
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[2048];

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				addDir(files[i], out);
				continue;
			}
			FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
			// log(" Adding: " + files[i].getPath());
			out.putNextEntry(new ZipEntry(files[i].getName()));
			int len;
			while ((len = in.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
	}

}
