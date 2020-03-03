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

import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import edu.gatech.arktos.userpoweredfl.FeedbackDirectedFLCore;
import edu.gatech.arktos.userpoweredfl.mcallrepr.MethodCallRepr;
import edu.gatech.arktos.userpoweredfl.mcallrepr.ValueGraphNode;
import edu.gatech.arktos.userpoweredfl.slicing.FieldSelectionResult;
import userpoweredflplugin.LazyValueReprTreeNode.FeedbackType;
import userpoweredflplugin.PostStateView.PostViewEditingSupport;

public class PreStateView extends ViewPart {

	public TreeViewer viewer;
	private MethodCallRepr prestate;
	private int sysFontSize = 11;
	public LazyTreeItemHandler treeItemHandler;

	public void createTreeItemHandler(FeedbackDirectedFLCore fbcore, FieldSelectionResult query) {
		this.treeItemHandler = new LazyTreeItemHandler(fbcore, query);
		viewer.refresh();
	}

	public void setState(MethodCallRepr newState) {
		prestate = newState;
		ValueGraphNode thisPtrVal = prestate.getThizz();
		Map<String, ValueGraphNode> thisParams = prestate.getParams();

		// have to add an virtual root because we could only add buttons through
		// expansion listener
		LazyValueReprTreeNode virtualRoot = new LazyValueReprTreeNode(null, "Invocation pre-state");
		LazyValueReprTreeNode thisPtrRoot;
		if (thisPtrVal != null) {
			thisPtrRoot = new LazyValueReprTreeNode(virtualRoot, thisPtrVal, "this");
			thisPtrRoot.setRefPathStr("this");
		} else {
			thisPtrRoot = null;
		}
		LazyValueReprTreeNode paramsRoot = new LazyValueReprTreeNode(virtualRoot, thisParams, "Parameters");

		virtualRoot.setRefPathStr("");
		LazyValueReprTreeNode[] rootItemList;
		if (thisPtrRoot != null) {
			rootItemList = new LazyValueReprTreeNode[] { thisPtrRoot, paramsRoot };
		} else {
			rootItemList = new LazyValueReprTreeNode[] { paramsRoot };
		}
		virtualRoot.setVirtualRootChildren(rootItemList);
		LazyValueReprTreeNode[] inputTreeItemsList = { virtualRoot };
		ViewContentProvider contentProvider = new ViewContentProvider(viewer, 40);
		viewer.setContentProvider(contentProvider);
		viewer.setInput(inputTreeItemsList);
		viewer.expandToLevel(3);
		viewer.refresh();
	}

	class PreViewEditingSupport extends EditingSupport {

		public PreViewEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			LazyValueReprTreeNode node = (LazyValueReprTreeNode) element;
			if (node.getFeedback() == FeedbackType.NA)
				return false;
			if (!node.canRecvPreviewFeedback())
				return false;
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new MyComboBoxCellEditor(((TreeViewer) viewer).getTree(), new String[] { " ", "incorrect" },
					SWT.READ_ONLY);
		}

		@Override
		protected Object getValue(Object element) {
			// is a string
			FeedbackType fb = ((LazyValueReprTreeNode) element).getFeedback();
			int comboInd = 0;
			switch (fb) {
			case IMPRE:
				comboInd = 1;
				break;
			}
			return comboInd;
		}

		@Override
		protected void setValue(Object element, Object value) {
			LazyValueReprTreeNode n = (LazyValueReprTreeNode) element;
			switch ((int) value) {
			case 1:
				n.resetFeedback(FeedbackType.IMPRE);
				break;
			default:
				n.resetFeedback(FeedbackType.NONE);
			}
			SwiftUtilities
					.log("[SWIFT] Pre-state field: " + n.getRefPathStr() + ", Feedback: " + n.getFeedback().name());
			treeItemHandler.storeUserFeedback(n);
			treeItemHandler.incorporateFeedback(n);
			viewer.update(element, null);
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		Font sysFont = Display.getDefault().getSystemFont();
		FontData fontData = sysFont.getFontData()[0];
		sysFontSize = (int) (fontData.getHeight() + 0.5);

		viewer = new TreeViewer(parent, SWT.BORDER | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		// speed up tree viewer widget lookup
		viewer.setUseHashlookup(true);

		TreeViewerColumn infoColumn0 = new TreeViewerColumn(viewer, SWT.NONE);
		infoColumn0.getColumn().setAlignment(SWT.LEFT);
		infoColumn0.getColumn().setText("Fields");
		infoColumn0.getColumn().setWidth(185);
		infoColumn0.setLabelProvider(
				new DelegatingStyledCellLabelProvider(new ViewLabelProvider(createRedDotImageDescriptor())));

		TreeViewerColumn buttonsColumn2 = new TreeViewerColumn(viewer, SWT.NONE);
		buttonsColumn2.getColumn().setAlignment(SWT.LEFT);
		buttonsColumn2.getColumn().setText("Feedback");
		buttonsColumn2.getColumn().setWidth(100);
		buttonsColumn2.setLabelProvider(new DelegatingStyledCellLabelProvider(
				new buttonColumnLabelProvider(createStateFeedbackImageDescriptor())));
		buttonsColumn2.setEditingSupport(new PreViewEditingSupport(viewer));

		viewer.setContentProvider(new ViewContentProvider(viewer, Integer.MAX_VALUE));
		// viewer.setInput(null);

		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle area = parent.getClientArea();
				Point preferredSize = tree.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - 2 * tree.getBorderWidth();
				if (preferredSize.y > area.height + tree.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = tree.getVerticalBar().getSize();
					width -= vBarSize.x;
				}
				Point oldSize = tree.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns
					// smaller first and then resize the table to
					// match the client area width
					buttonsColumn2.getColumn().setWidth(145);
					infoColumn0.getColumn().setWidth(width - 145);
					tree.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table
					// bigger first and then make the columns wider
					// to match the client area width
					tree.setSize(area.width, area.height);
					buttonsColumn2.getColumn().setWidth(145);
					infoColumn0.getColumn().setWidth(width - 145);
				}
			}
		});

		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				Object ooo = sel.getFirstElement();
				LazyValueReprTreeNode obj = (LazyValueReprTreeNode) ooo;
				String msg = obj.getPrettyString();
				if (msg != null) {
					Shell shell = Display.getDefault().getActiveShell();
					MessageDialog.openInformation(shell, "String value viewer", msg.substring(1, msg.length() - 1));
				}
			}
		});
	}

	@Override
	public void setFocus() {

		viewer.getControl().setFocus();
	}

	// Provides the tree structure for the tree viewer
	class ViewContentProvider implements ILazyTreeContentProvider {
		private TreeViewer viewer;
		LazyValueReprTreeNode[] inputTreeItemsList;
		public int maxExpandLevel;

		public ViewContentProvider(TreeViewer viewer, int maxLevel) {
			this.viewer = viewer;
			this.maxExpandLevel = maxLevel;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.inputTreeItemsList = (LazyValueReprTreeNode[]) newInput;
		}

		@Override
		public Object getParent(Object element) {
			if (element == this.inputTreeItemsList) {
				return null;
			}
			return ((LazyValueReprTreeNode) element).getParent();
		}

		@Override
		public void updateElement(Object parent, int index) {
			Object element = null;
			if (parent == this.inputTreeItemsList) {
				element = this.inputTreeItemsList[index];
			} else {
				((LazyValueReprTreeNode) parent).expand();
				LazyValueReprTreeNode[] children = ((LazyValueReprTreeNode) parent).getChildren();
				element = children[index];
				treeItemHandler.resetUserFeedback(children[index]);
			}
			viewer.replace(parent, index, element);
			updateChildCount(element, -1);
		}

		@Override
		public void updateChildCount(Object element, int currentChildCount) {
			int length = 0;

			if (element == this.inputTreeItemsList) {
				length = this.inputTreeItemsList.length;
			} else {
				int occurance = StringUtils.countMatches(((LazyValueReprTreeNode) element).getRefPathStr(), ".");
				if (occurance > this.maxExpandLevel + 3) {
					viewer.setChildCount(element, 0);
					return;
				}
				((LazyValueReprTreeNode) element).expand();
				LazyValueReprTreeNode[] children = ((LazyValueReprTreeNode) element).getChildren();
				length = children == null ? 0 : children.length;
			}
			viewer.setChildCount(element, length);
		}

	}

	// Generate images that indicate transitive suspiciousness
	private ImageDescriptor createRedDotImageDescriptor() {
		Bundle bundle = FrameworkUtil.getBundle(ViewLabelProvider.class);
		URL url = FileLocator.find(bundle, new Path("icons/bullet_red.png"), null);
		return ImageDescriptor.createFromURL(url);
	}

	private ImageDescriptor[] createStateFeedbackImageDescriptor() {
		Bundle bundle = FrameworkUtil.getBundle(ViewLabelProvider.class);
		URL url0 = FileLocator.find(bundle, new Path("icons/correct.gif"), null);
		URL url1 = FileLocator.find(bundle, new Path("icons/wrong.gif"), null);
		URL url2 = FileLocator.find(bundle, new Path("icons/question.gif"), null);
		ImageDescriptor desc0, desc1, desc2;
		desc0 = ImageDescriptor.createFromURL(url0);
		desc1 = ImageDescriptor.createFromURL(url1);
		desc2 = ImageDescriptor.createFromURL(url2);
		return new ImageDescriptor[] { desc0, desc1, desc2 };
	}

	// Provide a label containing field name, type, and current value
	class ViewLabelProvider extends LabelProvider implements IStyledLabelProvider, IColorProvider {

		private ImageDescriptor redDotImage;
		private ResourceManager resourceManager;

		public ViewLabelProvider(ImageDescriptor redDotImage) {
			this.redDotImage = redDotImage;
		}

		protected ResourceManager getResourceManager() {
			if (resourceManager == null) {
				resourceManager = new LocalResourceManager(JFaceResources.getResources());
			}
			return resourceManager;
		}

		public Image getImage(Object element) {
			// if (element instanceof LazyValueReprTreeNode && treeItemHandler
			// != null) {
			// LazyValueReprTreeNode n = (LazyValueReprTreeNode) element;
			// if (treeItemHandler.isTransitivelySuspNode(n)) {
			// return getResourceManager().createImage(this.redDotImage);
			// }
			// }
			return super.getImage(element);
		}

		@Override
		public Color getForeground(Object element) {
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			// if (element instanceof LazyValueReprTreeNode) {
			// if (((IDynamicValueReprTreeItem) element).isHighlighted()) {
			// return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
			// }
			// }
			// return Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
			// if (treeItemHandler != null) {
			// int r = (int) (255.0 *
			// (treeItemHandler.getSuspOfNode((LazyValueReprTreeNode)
			// element)));
			// if (r > 0)
			// return new Color(Display.getDefault(), 255, 255 - r, 255 - r);
			// }
			return null;
		}

		@Override
		public StyledString getStyledText(Object element) {
			if (element instanceof LazyValueReprTreeNode) {
				LazyValueReprTreeNode item = (LazyValueReprTreeNode) element;
				String name = item.getString();
				String type = item.getType();
				String value = item.getValue();
				StyledString ans = new StyledString("");

				ans.append(name, new Styler() {

					@Override
					public void applyStyles(TextStyle textStyle) {
						textStyle.font = new Font(Display.getDefault(), "Arial", sysFontSize - 2, SWT.NORMAL);
					}

				});

				ans.append(type != "" ? ": " + type : "", new Styler() {

					@Override
					public void applyStyles(TextStyle textStyle) {
						textStyle.font = new Font(Display.getDefault(), "Arial", sysFontSize - 2, SWT.ITALIC);
						textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
					}

				});
				ans.append(value != "" ? " = " + value : "", new Styler() {

					@Override
					public void applyStyles(TextStyle textStyle) {
						textStyle.font = new Font(Display.getDefault(), "Courier", sysFontSize - 2, SWT.NORMAL);
						textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
					}

				});
				return ans;
			} else
				return null;

		}

	}

	class FieldStateLabelProvider extends LabelProvider implements IStyledLabelProvider, IColorProvider {

		private ImageDescriptor correctImageDescriptor;
		private ImageDescriptor wrongImageDescriptor;
		private ImageDescriptor donnoImageDescriptor;

		private ResourceManager resourceManager;

		public FieldStateLabelProvider(ImageDescriptor[] descriptors) {
			this.correctImageDescriptor = descriptors[0];
			this.wrongImageDescriptor = descriptors[1];
			this.donnoImageDescriptor = descriptors[2];
		}

		protected ResourceManager getResourceManager() {
			if (resourceManager == null) {
				resourceManager = new LocalResourceManager(JFaceResources.getResources());
			}
			return resourceManager;
		}

		public Image getImage(Object element) {
			if (element instanceof LazyValueReprTreeNode && treeItemHandler != null) {
				LazyValueReprTreeNode n = (LazyValueReprTreeNode) element;
				FeedbackType fb = n.getFeedback();
				switch (fb) {
				case IMPRE:
					return getResourceManager().createImage(this.wrongImageDescriptor);
				// case NONE:
				// return
				// getResourceManager().createImage(this.correctImageDescriptor);
				}
			}
			return super.getImage(element);
		}

		@Override
		public Color getForeground(Object element) {
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public StyledString getStyledText(Object element) {
			return new StyledString("");
		}

	}

	class buttonColumnLabelProvider extends LabelProvider implements IStyledLabelProvider, IColorProvider {

		private ImageDescriptor correctImageDescriptor;
		private ImageDescriptor wrongImageDescriptor;
		private ImageDescriptor donnoImageDescriptor;

		private ResourceManager resourceManager;

		public buttonColumnLabelProvider(ImageDescriptor[] descriptors) {
			this.correctImageDescriptor = descriptors[0];
			this.wrongImageDescriptor = descriptors[1];
			this.donnoImageDescriptor = descriptors[2];
		}

		protected ResourceManager getResourceManager() {
			if (resourceManager == null) {
				resourceManager = new LocalResourceManager(JFaceResources.getResources());
			}
			return resourceManager;
		}

		public Image getImage(Object element) {
			if (element instanceof LazyValueReprTreeNode && treeItemHandler != null) {
				LazyValueReprTreeNode n = (LazyValueReprTreeNode) element;
				FeedbackType fb = n.getFeedback();
				switch (fb) {
				case IMPRE:
					return getResourceManager().createImage(this.wrongImageDescriptor);
				// case NONE:
				// return
				// getResourceManager().createImage(this.correctImageDescriptor);
				}
			}
			return super.getImage(element);
		}

		@Override
		public Color getForeground(Object element) {
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public StyledString getStyledText(Object element) {
			String fbstr = ((LazyValueReprTreeNode) element).getFeedback().toString();
			String ans = fbstr.equals("IMPRE") ? "INCORRECT" : "";
			if (!ans.equals("INCORRECT") && !fbstr.equals("NA") && ((LazyValueReprTreeNode) element).canRecvPreviewFeedback()) {
				ans = "NOT GIVEN";
			}
			return new StyledString(ans);
		}

	}

}
