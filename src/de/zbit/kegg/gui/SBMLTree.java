/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.zbit.kegg.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.CompartmentType;
import org.sbml.jsbml.Constraint;
import org.sbml.jsbml.Delay;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.EventAssignment;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.MathContainer;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.SpeciesType;
import org.sbml.jsbml.Trigger;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;

/**
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.3
 * 
 */
@SuppressWarnings("deprecation")
class NamedMutableSBMLTreeNode extends DefaultMutableTreeNode {

	/**
	 * Generated Serial Version ID.s
	 */
	private static final long serialVersionUID = 2647365066465077496L;
	/**
	 * 
	 */
	private String name;

	/**
	 * 
	 * @param name
	 * @param sbase
	 */
	public NamedMutableSBMLTreeNode(String name, SBase sbase) {
		super(sbase);
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.tree.DefaultMutableTreeNode#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
}

/**
 * A specialized {@link JTree} that shows the elements of a JSBML model as a
 * hierarchical structure.
 * 
 * @author Simon Sch&auml;fer
 * @author Andreas Dr&auml;ger
 * @since 1.3
 */
@SuppressWarnings("deprecation")
public class SBMLTree extends JTree implements MouseListener, ActionListener {

	/**
	 * Generated serial version id
	 */
	private static final long serialVersionUID = -3081533906479036522L;

	/**
	 * 
	 * @param m
	 * @return
	 */
	private static DefaultMutableTreeNode createNodes(Model m) {
		DefaultMutableTreeNode docNode = new DefaultMutableTreeNode(m
				.getSBMLDocument());
		DefaultMutableTreeNode modelNode = new DefaultMutableTreeNode(m);
		docNode.add(modelNode);
		DefaultMutableTreeNode node;
		if (m.getNumFunctionDefinitions() > 0) {
			node = new NamedMutableSBMLTreeNode("Function Definitions", m
					.getListOfFunctionDefinitions());
			modelNode.add(node);
			for (FunctionDefinition c : m.getListOfFunctionDefinitions()) {
				DefaultMutableTreeNode n = new DefaultMutableTreeNode(c);
				node.add(n);
				if (c.isSetMath()) {
					n.add(mathTree(c.getMath()));
				}
			}
		}
		if (m.getNumUnitDefinitions() > 0) {
			node = new NamedMutableSBMLTreeNode("Unit Definitions", m
					.getListOfUnitDefinitions());
			modelNode.add(node);
			for (UnitDefinition ud : m.getListOfUnitDefinitions()) {
				DefaultMutableTreeNode unitDefNode = new DefaultMutableTreeNode(
						ud);
				node.add(unitDefNode);
				for (Unit u : ud.getListOfUnits()) {
					unitDefNode.add(new DefaultMutableTreeNode(u));
				}
			}
		}
		if (m.getNumCompartmentTypes() > 0) {
			node = new NamedMutableSBMLTreeNode("Compartment Types", m
					.getListOfCompartmentTypes());
			modelNode.add(node);
			for (CompartmentType c : m.getListOfCompartmentTypes())
				node.add(new DefaultMutableTreeNode(c));
		}
		if (m.getNumSpeciesTypes() > 0) {
			node = new NamedMutableSBMLTreeNode("Species Types", m
					.getListOfSpeciesTypes());
			modelNode.add(node);
			for (SpeciesType c : m.getListOfSpeciesTypes())
				node.add(new DefaultMutableTreeNode(c));
		}
		if (m.getNumCompartments() > 0) {
			node = new NamedMutableSBMLTreeNode("Compartments", m
					.getListOfCompartments());
			modelNode.add(node);
			for (Compartment c : m.getListOfCompartments())
				node.add(new DefaultMutableTreeNode(c));
		}
		if (m.getNumSpecies() > 0) {
			node = new NamedMutableSBMLTreeNode("Species", m.getListOfSpecies());
			modelNode.add(node);
			for (Species s : m.getListOfSpecies())
				node.add(new DefaultMutableTreeNode(s));
		}
		if (m.getNumParameters() > 0) {
			node = new NamedMutableSBMLTreeNode("Parameters", m
					.getListOfParameters());
			modelNode.add(node);
			for (Parameter p : m.getListOfParameters())
				node.add(new DefaultMutableTreeNode(p));
		}
		if (m.getNumInitialAssignments() > 0) {
			node = new NamedMutableSBMLTreeNode("Initial Assignments", m
					.getListOfInitialAssignments());
			modelNode.add(node);
			for (InitialAssignment c : m.getListOfInitialAssignments()) {
				NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(
						"Initial Assignment", c);
				node.add(n);
				if (c.isSetMath()) {
					n.add(mathTree(c.getMath()));
				}
			}
		}
		if (m.getNumRules() > 0) {
			node = new NamedMutableSBMLTreeNode("Rules", m.getListOfRules());
			modelNode.add(node);
			String name;
			for (Rule c : m.getListOfRules()) {
				if (c.isAlgebraic()) {
					name = "Algebraic";
				} else if (c.isAssignment()) {
					name = "Assignment";
				} else {
					name = "Rate";
				}
				NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(name
						+ " Rule", c);
				node.add(n);
				if (c.isSetMath()) {
					n.add(mathTree(c.getMath()));
				}
			}
		}
		if (m.getNumConstraints() > 0) {
			node = new NamedMutableSBMLTreeNode("Constraints", m
					.getListOfConstraints());
			modelNode.add(node);
			for (Constraint c : m.getListOfConstraints()) {
				NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(c
						.getClass().getSimpleName(), c);
				node.add(n);
				if (c.isSetMath()) {
					n.add(mathTree(c.getMath()));
				}
			}
		}
		if (m.getNumReactions() > 0) {
			node = new NamedMutableSBMLTreeNode("Reactions", m
					.getListOfReactions());
			modelNode.add(node);
			for (Reaction r : m.getListOfReactions()) {
				DefaultMutableTreeNode currReacNode = new DefaultMutableTreeNode(
						r);
				node.add(currReacNode);
				if (r.getNumReactants() > 0) {
					NamedMutableSBMLTreeNode reactants = new NamedMutableSBMLTreeNode(
							"Reactants", r.getListOfReactants());
					currReacNode.add(reactants);
					for (SpeciesReference specRef : r.getListOfReactants())
						reactants.add(new DefaultMutableTreeNode(specRef));
				}
				if (r.getNumProducts() > 0) {
					NamedMutableSBMLTreeNode products = new NamedMutableSBMLTreeNode(
							"Products", r.getListOfProducts());
					currReacNode.add(products);
					for (SpeciesReference specRef : r.getListOfProducts())
						products.add(new DefaultMutableTreeNode(specRef));
				}
				if (r.getNumModifiers() > 0) {
					NamedMutableSBMLTreeNode modifiers = new NamedMutableSBMLTreeNode(
							"Modifiers", r.getListOfModifiers());
					currReacNode.add(modifiers);
					for (ModifierSpeciesReference mSpecRef : r
							.getListOfModifiers())
						modifiers.add(new DefaultMutableTreeNode(mSpecRef));
				}
				if (r.isSetKineticLaw()) {
					KineticLaw kl = r.getKineticLaw();
					NamedMutableSBMLTreeNode klNode = new NamedMutableSBMLTreeNode(
							"Kinetic Law", kl);
					currReacNode.add(klNode);
					if (kl.isSetMath()) {
						klNode.add(mathTree(kl.getMath()));
					}
					if (kl.getNumParameters() > 0) {
						NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(
								"Parameters", kl.getListOfParameters());
						klNode.add(n);
						for (LocalParameter p : kl.getListOfParameters()) {
							n.add(new DefaultMutableTreeNode(p));
						}
					}
				}
			}
		}
		if (m.getNumEvents() > 0) {
			node = new DefaultMutableTreeNode("Events");
			modelNode.add(node);
			for (Event e : m.getListOfEvents()) {
				DefaultMutableTreeNode eNode = new DefaultMutableTreeNode(e);
				node.add(eNode);
				if (e.isSetTrigger()) {
					Trigger t = e.getTrigger();
					NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(t
							.getClass().getSimpleName(), t);
					eNode.add(n);
					if (t.isSetMath()) {
						n.add(mathTree(t.getMath()));
					}
				}
				if (e.isSetDelay()) {
					Delay d = e.getDelay();
					NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(d
							.getClass().getSimpleName(), d);
					eNode.add(n);
					if (d.isSetMath()) {
						n.add(mathTree(d.getMath()));
					}
				}
				if (e.getNumEventAssignments() > 0) {
					DefaultMutableTreeNode eas = new NamedMutableSBMLTreeNode(
							"Event Assignments", e.getListOfEventAssignments());
					eNode.add(eas);
					for (EventAssignment ea : e.getListOfEventAssignments()) {
						NamedMutableSBMLTreeNode n = new NamedMutableSBMLTreeNode(
								"Event Assignment", ea);
						eas.add(n);
						if (ea.isSetMath()) {
							n.add(mathTree(ea.getMath()));
						}
					}
				}
			}
		}
		return docNode;
	}

	/**
	 * Constructs a tree of math objects.
	 * 
	 * @param math
	 * @return
	 */
	private static MutableTreeNode mathTree(ASTNode math) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(math);
		for (ASTNode child : math.getListOfNodes()) {
			node.add(mathTree(child));
		}
		return node;
	}

	/**
	 * 
	 */
	private SBase currSBase;

	/**
	 * 
	 */
	private JPopupMenu popup;

	/**
	 * 
	 */
	private Set<ActionListener> setOfActionListeners;

	/**
	 * Generate a tree that displays an ASTNode tree.
	 * 
	 * @param math
	 */
	public SBMLTree(ASTNode math) {
		super(math);
		init();
	}

	/**
	 * 
	 * @param m
	 */
	public SBMLTree(Model m) {
		super(createNodes(m));
		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (popup != null) {
			popup.setVisible(false);
		}
		for (ActionListener al : setOfActionListeners) {
			e.setSource(currSBase);
			al.actionPerformed(e);
		}
	}

	/**
	 * 
	 * @param al
	 */
	public void addActionListener(ActionListener al) {
		setOfActionListeners.add(al);
	}

	/**
	 * Initializes this object.
	 */
	private void init() {
		setOfActionListeners = new HashSet<ActionListener>();
		// popup = new JPopupMenu();
		addMouseListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
		if ((popup != null) && popup.isVisible()) {
			currSBase = null;
			popup.setVisible(false);
		}
		Object clickedOn = getClosestPathForLocation(e.getX(), e.getY())
				.getLastPathComponent();
		if (clickedOn instanceof ASTNode) {
			ASTNode ast = (ASTNode) clickedOn;
			System.out.println(ast.getType());
		} else if ((e.getClickCount() == 2)
				|| (e.getButton() == MouseEvent.BUTTON3)
				&& setOfActionListeners.size() > 0) {
			if (clickedOn instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) getSelectionPath()
						.getLastPathComponent();
				Object userObject = node.getUserObject();
				if (userObject instanceof Reaction
						|| userObject instanceof Model
						|| userObject instanceof SBMLDocument) {
					if (userObject instanceof SBMLDocument) {
						currSBase = ((SBMLDocument) userObject).getModel();
					} else {
						currSBase = (SBase) userObject;
					}
					if (popup != null) {
						popup.setLocation(e.getX()
								+ ((int) getLocationOnScreen().getX()), e
								.getY()
								+ ((int) getLocationOnScreen().getY()));// e.getLocationOnScreen());
						popup.setVisible(true);
					}
				}
				if (((DefaultMutableTreeNode) clickedOn).getUserObject() instanceof MathContainer) {
					MathContainer mc = (MathContainer) ((DefaultMutableTreeNode) clickedOn)
							.getUserObject();
					JDialog dialog = new JDialog();
					JScrollPane scroll = new JScrollPane(new SBMLTree(mc
							.getMath()),
							JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
							JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
					dialog.getContentPane().add(scroll);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.pack();
					dialog.setModal(true);
					dialog.setLocationRelativeTo(null);
					dialog.setVisible(true);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
	}
}
