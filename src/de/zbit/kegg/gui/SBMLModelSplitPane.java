/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.gui;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.util.SBaseChangedEvent;
import org.sbml.jsbml.util.SBaseChangedListener;

import de.zbit.gui.GUITools;

/**
 * A specialized {@link JSplitPane} that displays a {@link JTree} containing all
 * model elements of a JSBML model on the left hand side and an
 * {@link SBasePanel} showing details of the active element in the tree on the
 * right hand side.
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.0 (originates from SBMLsqueezer 1.3)
 * @version $Rev$
 */
public class SBMLModelSplitPane extends JSplitPane implements
		TreeSelectionListener, SBaseChangedListener {
	
	/**
	 * Generated serial version id.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private final Set<ActionListener> actionListeners;
	
	/**
	 * 
	 */
	private SBMLTree tree;
	
	/**
	 * @param model
	 * @throws SBMLException
	 * @throws IOException
	 */
	public SBMLModelSplitPane(SBMLDocument document) throws SBMLException,
		IOException {
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		actionListeners = new HashSet<ActionListener>();
		document.addChangeListener(this);
		init(document, false);
	}
	
	/**
	 * @param al
	 */
	public void addActionListener(ActionListener al) {
		tree.addActionListener(al);
		actionListeners.add(al);
	}
	
	/**
	 * Function to display the properties of {@link ASTNode} objects.
	 * 
	 * @param nodeInfo
	 * @return
	 * @throws SBMLException
	 * @throws IOException
	 */
	private JScrollPane createRightComponent(ASTNode node) throws SBMLException,
		IOException {
		return new JScrollPane(new ASTNodePanel(node));
	}
	
	/**
	 * @param sbase
	 * @return
	 * @throws SBMLException
	 * @throws IOException
	 */
	private JScrollPane createRightComponent(SBase sbase) throws SBMLException,
		IOException {
		JPanel p = new JPanel();
		p.add(new SBasePanel(sbase));
		JScrollPane scroll = new JScrollPane(p);
		return scroll;
	}
	
	/**
	 * 
	 * @return
	 */
	public SBMLDocument getSBMLDocument() {
		return (SBMLDocument) tree.getModel().getRoot();
	}
	
	/**
	 * @param doc
	 * @param keepDivider
	 * @throws SBMLException
	 * @throws IOException
	 */
	public void init(SBMLDocument doc, boolean keepDivider) throws SBMLException,
		IOException {
		int proportionalLocation = getDividerLocation();
		TreePath path = null;
		if (tree != null) {
			path = tree.getSelectionPath();
		}
		tree = new SBMLTree(doc.getSBMLDocument());
		tree.setShowsRootHandles(true);
		tree.setScrollsOnExpand(true);
		for (ActionListener al : actionListeners) {
			tree.addActionListener(al);
		}
		if (path != null) {
			// tree.setSelectionPath(path);
			tree.setExpandsSelectedPaths(true);
			tree.expandPath(path);
		}
		tree.addTreeSelectionListener(this);
		tree.setSelectionRow(0);
		setLeftComponent(new JScrollPane(tree,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		setRightComponent(createRightComponent(doc));
		if (keepDivider) {
			setDividerLocation(proportionalLocation);
		}
		validate();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.squeezer.io.SBaseChangedListener#sbaseAdded(org.sbml.jlibsbml
	 * .SBase)
	 */
	public void sbaseAdded(SBase sb) {
		// TreePath path = tree.getSelectionPath();
		// init(sb.getModel(), true);
		// tree.setSelectionPath(path);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.squeezer.io.SBaseChangedListener#sbaseRemoved(org.sbml.jlibsbml
	 * .SBase)
	 */
	public void sbaseRemoved(SBase sb) {
		// TreePath path = tree.getSelectionPath();
		// init(sb.getModel(), true);
		// tree.setSelectionPath(path);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sbml.jsbml.SBaseChangedListener#stateChanged(org.sbml.jsbml.SBaseChangedEvent)
	 */
	public void stateChanged(SBaseChangedEvent ev) {
		// TreePath path = tree.getSelectionPath();
		// init(sb.getModel(), true);
		// tree.setSelectionPath(path);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event
	 * .TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent e) {
		TreeNode node = (TreeNode) tree.getLastSelectedPathComponent();
		if (node == null)
		// Nothing is selected.
			return;
		if (node instanceof SBase) {
			int proportionalLocation = getDividerLocation();
			try {
				setRightComponent(createRightComponent((SBase) node));
			} catch (Exception e1) {
				GUITools.showErrorMessage(this, e1);
			}
			setDividerLocation(proportionalLocation);
		} else if (node instanceof ASTNode) {
			int proportionalLocation = getDividerLocation();
			try {
				setRightComponent(createRightComponent((ASTNode) node));
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			}
			setDividerLocation(proportionalLocation);
		} else {
		  System.out.println("node class " + node.getClass().getName() + " is unknown.");
			// displayURL(helpURL);
		}
	}
}
