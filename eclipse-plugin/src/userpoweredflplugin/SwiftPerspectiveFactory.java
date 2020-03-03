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

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class SwiftPerspectiveFactory implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout myLayout) {
		String editorArea = myLayout.getEditorArea();

		IFolderLayout topleft = myLayout.createFolder("topleft", IPageLayout.LEFT, 0.56f, editorArea);
		topleft.addView("userpoweredflplugin.prestateview");
		topleft.addView("userpoweredflplugin.poststateview");

		IFolderLayout bottomleft = myLayout.createFolder("bottomleft", IPageLayout.BOTTOM, 0.5f, "topleft");
		bottomleft.addView("userpoweredflplugin.expctrlview");
		bottomleft.addView("userpoweredflplugin.controlview");

		
		IFolderLayout bottomright = myLayout.createFolder("bottomright", IPageLayout.BOTTOM, 0.66f, editorArea);
		bottomright.addView("org.eclipse.debug.ui.DebugView");
		bottomright.addView("org.eclipse.debug.ui.VariableView");
		bottomright.addView("org.eclipse.debug.ui.ExpressionView");
		bottomright.addPlaceholder("org.eclipse.jdt.ui.JavadocView");
		
		myLayout.getViewLayout("userpoweredflplugin.controlview").setCloseable(false);
		myLayout.getViewLayout("userpoweredflplugin.expctrlview").setCloseable(false);
		myLayout.getViewLayout("userpoweredflplugin.prestateview").setCloseable(false);
		myLayout.getViewLayout("userpoweredflplugin.poststateview").setCloseable(false);
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView("userpoweredflplugin.expctrlview");
		} catch (PartInitException e2) {
			e2.printStackTrace();
		}

	}

}
