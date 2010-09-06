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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.jdom.JDOMException;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Constraint;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.EventAssignment;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.History;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.MathContainer;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.StoichiometryMath;
import org.sbml.jsbml.Symbol;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.util.compilers.LaTeX;

import atp.sHotEqn;
import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.SystemBrowser;
import de.zbit.io.LaTeXExport;
import de.zbit.io.MIRIAMparser;
import de.zbit.resources.Resource;

/**
 * A specialized {@link JPanel} that displays all available properties of a
 * given {@link SBase} in a GUI.
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.3
 */
@SuppressWarnings("deprecation")
public class SBasePanel extends JPanel {

	/**
	 * Generated serial version id.
	 */
	private static final long serialVersionUID = -4969096536922920641L;

	private LayoutHelper lh;

	private LaTeX latex;

	private static final int preferedWidth = 450;

	private boolean editable;

	private int row;

	/**
	 * 
	 */
	private static final MIRIAMparser miriam = new MIRIAMparser();

	static {
		try {
			miriam.setMIRIAMfile(Resource.class.getResource("cfg/MIRIAM.xml")
					.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param sbase
	 * @throws SBMLException 
	 */
	public SBasePanel(SBase sbase) throws SBMLException {
		super();
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		latex = new LaTeX(false);
		lh = new LayoutHelper(this, gbl);
		editable = false;
		row = -1;
		String className = sbase.getClass().getCanonicalName();
		className = className.substring(className.lastIndexOf('.') + 1);
		setBorder(BorderFactory.createTitledBorder(" " + className + " "));
		lh.add(new JPanel(), 0, ++row, 5, 1, 0, 0);
		if (sbase instanceof NamedSBase) {
			addProperties((NamedSBase) sbase);
		}
		addProperties(sbase);
		if (sbase instanceof SimpleSpeciesReference) {
			addProperties((SimpleSpeciesReference) sbase);
		}
		if (sbase instanceof MathContainer) {
			addProperties((MathContainer) sbase);
		}
		if (sbase instanceof ListOf<?>) {
			addProperties((ListOf<?>) sbase);
			// ListOf<?> list = (ListOf<?>) sbase;
			// for (SBase s : list) {
			// lh.add(new SBasePanel(s, settings));
			// }
		} else if (sbase instanceof Model) {
			addProperties((Model) sbase);
		} else if (sbase instanceof UnitDefinition) {
			addProperties((UnitDefinition) sbase);
		} else if (sbase instanceof Unit) {
			addProperties((Unit) sbase);
		} else if (sbase instanceof Compartment) {
			addProperties((Compartment) sbase);
		} else if (sbase instanceof Species) {
			addProperties((Species) sbase);
		} else if (sbase instanceof Parameter) {
			addProperties((Parameter) sbase);
		} else if (sbase instanceof Constraint) {
			addProperties((Constraint) sbase);
		} else if (sbase instanceof Reaction) {
			addProperties((Reaction) sbase);
		} else if (sbase instanceof Event) {
			addProperties((Event) sbase);
		}
	}

	/**
	 * 
	 * @param c
	 */
	private void addProperties(Compartment c) {
		if (c.isSetCompartmentType() || editable) {
			lh.add(new JLabel("Compartment type: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(c.getCompartmentTypeInstance()
					.toString());
			tf.setEditable(editable);
			lh.add(tf, 3, ++row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (c.isSetOutside() || editable) {
			lh.add(new JLabel("Outside: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(c.getOutsideInstance().toString());
			tf.setEditable(editable);
			lh.add(tf, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		lh.add(new JLabel("Spatial dimensions: "), 1, ++row, 1, 1, 1, 1);
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(c
				.getSpatialDimensions(), 0, 3, 1));
		spinner.setEnabled(editable);
		lh.add(spinner, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		addProperties((Symbol) c);
	}

	/**
	 * 
	 * @param c
	 */
	private void addProperties(Constraint c) {
		if (c.isSetMessage() || editable) {
			lh.add(new JLabel("Message: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(c.getMessage());
			tf.setEditable(editable);
			lh.add(tf, 3, ++row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
	}

	/**
	 * 
	 * @param e
	 * @throws SBMLException 
	 */
	private void addProperties(Event e) throws SBMLException {
		JCheckBox check = new JCheckBox("Uses values from trigger time", e
				.getUseValuesFromTriggerTime());
		check.setEnabled(editable);
		lh.add(check, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		if (e.isSetTrigger()) {
			lh.add(new SBasePanel(e.getTrigger()), 1, ++row, 3, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (e.isSetDelay()) {
			lh.add(new SBasePanel(e.getDelay()), 1, ++row, 3, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (e.isSetTimeUnits()) {
			lh.add(new SBasePanel(e.getTimeUnitsInstance()), 1, ++row, 3, 1, 1,
					1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		for (EventAssignment ea : e.getListOfEventAssignments()) {
			lh.add(new SBasePanel(ea), 1, ++row, 3, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
	}

	/**
	 * 
	 * @param list
	 */
	private void addProperties(ListOf<? extends SBase> list) {
		// TODO
		list.size();
	}

	/**
	 * 
	 * @param mc
	 * @throws SBMLException 
	 */
	private void addProperties(MathContainer mc) throws SBMLException {
		if (mc.isSetMath()) {
			StringBuffer laTeXpreview = new StringBuffer();
			laTeXpreview.append(LaTeX.eqBegin);
			if (mc instanceof KineticLaw) {
				KineticLaw k = (KineticLaw) mc;
				laTeXpreview.append("v_");
				laTeXpreview
						.append(latex.mbox(k.getParentSBMLObject().getId()));
				laTeXpreview.append('=');
			} else if (mc instanceof FunctionDefinition) {
				FunctionDefinition f = (FunctionDefinition) mc;
				laTeXpreview.append(latex.mbox(f.getId()));
			} else if (mc instanceof EventAssignment) {
				EventAssignment ea = (EventAssignment) mc;
				laTeXpreview.append(latex.mbox(ea.getVariable()));
				laTeXpreview.append('=');
			} else if (mc instanceof AssignmentRule) {
				AssignmentRule ar = (AssignmentRule) mc;
				laTeXpreview.append(latex.mbox(ar.getVariable()));
				laTeXpreview.append('=');
			} else if (mc instanceof RateRule) {
				RateRule rr = (RateRule) mc;
				laTeXpreview.append(latex.timeDerivative(rr.getVariable()));
				laTeXpreview.append('=');
			}
			try {
				laTeXpreview.append(mc.getMath().compile(latex).toString()
						.replace("mathrm", "mbox").replace("text", "mbox")
						.replace("mathtt", "mbox"));
			} catch (SBMLException e) {
				e.printStackTrace();
			}
			laTeXpreview.append(LaTeX.eqEnd);
			JPanel preview = new JPanel(new BorderLayout());
			preview.add(new sHotEqn(laTeXpreview.toString()),
					BorderLayout.CENTER);
			preview.setBackground(Color.WHITE);
			preview.setBorder(BorderFactory.createLoweredBevelBorder());
			Dimension d = new Dimension(preferedWidth, 120);
			JScrollPane scroll = new JScrollPane(preview,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setPreferredSize(new Dimension((int) d.getWidth() + 10,
					(int) d.getHeight() + 10));
			lh.add(scroll, 1, ++row, 3, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
			if (mc instanceof EventAssignment)
				lh.add(new SBasePanel(((EventAssignment) mc)
						.getVariableInstance()), 1, ++row, 3, 1, 1, 1);
			else if (mc instanceof InitialAssignment)
				lh.add(new SBasePanel(((InitialAssignment) mc)
						.getVariableInstance()), 1, ++row, 3, 1, 1, 1);
			else if (mc instanceof AssignmentRule)
				lh.add(new SBasePanel(((AssignmentRule) mc)
						.getVariableInstance()), 1, ++row, 3, 1, 1, 1);
			else if (mc instanceof RateRule)
				lh.add(new SBasePanel(((RateRule) mc).getVariableInstance()),
						1, ++row, 3, 1, 1, 1);
		}
	}

	/**
	 * 
	 * @param m
	 */
	private void addProperties(Model m) {
		if (m.isSetModelHistory()) {
			History hist = m.getModelHistory();
			lh.add(new JLabel("Model creators: "), 1, ++row, 1, 1, 1, 1);
			String columnNames[] = new String[] { "Given name", "Family name",
					"E-mail", "Organization" };
			String rowData[][] = new String[hist.getNumCreators()][4];
			int i = 0;
			for (Creator mc : hist.getListCreators()) {
				rowData[i][0] = mc.getGivenName();
				rowData[i][1] = mc.getFamilyName();
				rowData[i][2] = mc.getEmail();
				rowData[i][3] = mc.getOrganization();
				i++;
			}
			JTable table = new JTable(rowData, columnNames);
			table.setEnabled(editable);
			table.setPreferredScrollableViewportSize(new Dimension(200, (table
					.getRowCount() + 1)
					* table.getRowHeight()));
			JScrollPane scroll = new JScrollPane(table,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			Dimension dim = table.getPreferredScrollableViewportSize();
			scroll.setPreferredSize(new Dimension((int) dim.getWidth() + 10,
					(int) dim.getHeight() + 18));
			lh.add(scroll, 1, ++row, 3, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
			if (hist.isSetCreatedDate()) {
				lh.add(new JLabel("Model creation: "), 1, ++row, 1, 1, 1, 1);
				JTextField tf = new JTextField(hist.getCreatedDate().toString());
				tf.setEditable(editable);
				lh.add(tf, 3, row, 1, 1, 1, 1);
				lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
			}
			Vector<Date> modification = new Vector<Date>();
			if (hist.isSetModifiedDate())
				modification.add(hist.getModifiedDate());
			for (i = 0; i < hist.getNumModifiedDates(); i++)
				if (!modification.contains(hist.getModifiedDate(i)))
					modification.add(hist.getModifiedDate(i));
			if (modification.size() > 0) {
				lh.add(new JLabel("Modification: "), 1, ++row, 1, 1, 1, 1);
				JList l = new JList(modification);
				l.setEnabled(editable);
				JScrollPane scroll2 = new JScrollPane(l,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scroll2.setPreferredSize(new Dimension(preferedWidth,
						modification.size() * 20));
				scroll2.setBorder(BorderFactory.createLoweredBevelBorder());
				lh.add(scroll2, 3, row, 1, 1, 1, 1);
				lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
			}
		}
		String columnNames[] = new String[] { "Element", "Quantity" };
		String rowData[][] = new String[][] {
				{ "Function definitions",
						Integer.toString(m.getNumFunctionDefinitions()) },
				{ "Unit definitions",
						Integer.toString(m.getNumUnitDefinitions()) },
				{ "Compartment types",
						Integer.toString(m.getNumCompartmentTypes()) },
				{ "Species types", Integer.toString(m.getNumSpeciesTypes()) },
				{ "Compartments", Integer.toString(m.getNumCompartments()) },
				{ "Species", Integer.toString(m.getNumSpecies()) },
				{ "Global parameters", Integer.toString(m.getNumParameters()) },
				{ "Local parameters",
						Integer.toString(m.getNumLocalParameters()) },
				{ "Initial assignments",
						Integer.toString(m.getNumInitialAssignments()) },
				{ "Rules", Integer.toString(m.getNumRules()) },
				{ "Constraints", Integer.toString(m.getNumConstraints()) },
				{ "Reactions", Integer.toString(m.getNumReactions()) },
				{ "Events", Integer.toString(m.getNumEvents()) } };
		JTable table = new JTable(rowData, columnNames);
		table.setEnabled(editable);
		table.setPreferredScrollableViewportSize(new Dimension(200, table
				.getRowCount()
				* table.getRowHeight()));
		JScrollPane scroll = new JScrollPane(table,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Dimension dim = table.getPreferredScrollableViewportSize();
		scroll.setPreferredSize(new Dimension((int) dim.getWidth() + 10,
				(int) dim.getHeight() + 18));
		lh.add(scroll, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
	}

	/**
	 * 
	 * @param sbase
	 */
	private void addProperties(ModifierSpeciesReference msr) {
		// TODO Auto-generated method stub
	}

	/**
	 * 
	 * @param nsb
	 */
	private void addProperties(NamedSBase nsb) {
		if (nsb.isSetId() || editable) {
			lh.add(new JLabel("Identifier: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(nsb.getId());
			tf.setEditable(editable);
			lh.add(tf, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (nsb.isSetName() || editable) {
			lh.add(new JLabel("Name: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(nsb.getName());
			tf.setEditable(editable);
			lh.add(tf, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
	}

	/**
	 * 
	 * @param sbase
	 */
	private void addProperties(Parameter p) {
		addProperties((Symbol) p);
	}

	/**
	 * 
	 * @param sbase
	 * @throws SBMLException 
	 */
	private void addProperties(Reaction reaction) throws SBMLException {
		JCheckBox check = new JCheckBox("Reversible", reaction.getReversible());
		check.setEnabled(editable);
		lh.add(check, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		check = new JCheckBox("Fast", reaction.getFast());
		check.setEnabled(editable);
		lh.add(check, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);

		// Create Table of reactants, modifiers and products
		String rmp[][] = new String[Math.max(reaction.getNumReactants(), Math
				.max(reaction.getNumModifiers(), reaction.getNumProducts()))][3];
		String colNames[] = new String[] { "Reactants", "Modifiers", "Products" };
		int count = 0;
		for (SpeciesReference specRef : reaction.getListOfReactants())
			rmp[count++][0] = specRef.getSpeciesInstance().toString();
		count = 0;
		for (ModifierSpeciesReference mSpecRef : reaction.getListOfModifiers())
			rmp[count++][1] = mSpecRef.getSpeciesInstance().toString();
		count = 0;
		for (SpeciesReference specRef : reaction.getListOfProducts())
			rmp[count++][2] = specRef.getSpeciesInstance().toString();
		JTable table = new JTable(rmp, colNames);
		table.setPreferredScrollableViewportSize(new Dimension(200, (table
				.getRowCount() + 1)
				* table.getRowHeight()));
		table.setEnabled(editable);
		JScrollPane scroll = new JScrollPane(table,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Dimension dim = table.getPreferredScrollableViewportSize();
		scroll.setPreferredSize(new Dimension((int) dim.getWidth() + 10,
				(int) dim.getHeight() + 18));
		lh.add(scroll, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		JPanel rEqPanel = new JPanel(new BorderLayout());
		sHotEqn rEqn = new sHotEqn((new LaTeXExport(false, false, true, false,
				(short) 11, "a4")).reactionEquation(reaction));
		JScrollPane s = new JScrollPane(rEqn,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		s.setBorder(BorderFactory.createLoweredBevelBorder());
		s.setPreferredSize(new Dimension(preferedWidth, 50));
		rEqPanel.add(s, BorderLayout.CENTER);
		rEqPanel.setBorder(BorderFactory
				.createTitledBorder(" Reaction equation "));
		lh.add(rEqPanel, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		if (reaction.isSetKineticLaw())
			lh.add(new SBasePanel(reaction.getKineticLaw()), 1, ++row, 3, 1, 1,
					1);
	}

	/**
	 * 
	 * @param sbase
	 */
	private void addProperties(SBase sbase) {
		lh.add(new JPanel(), 0, row, 1, 1, 0, 0);
		lh.add(new JPanel(), 4, row, 1, 1, 0, 0);
		lh.add(new JPanel(), 2, row, 1, 1, 0, 0);
		if (sbase.isSetMetaId() || editable) {
			lh.add(new JLabel("Meta identifier: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(sbase.getMetaId());
			tf.setEditable(editable);
			lh.add(tf, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (sbase.isSetNotes() || editable) {
			lh.add(new JLabel("Notes: "), 1, ++row, 1, 1, 1, 1);
			String text = sbase.getNotesString();
			if (text.startsWith("<notes") && text.endsWith("notes>"))
				text = text.substring(sbase.getNotesString().indexOf('>') + 1,
						sbase.getNotesString().lastIndexOf('/') - 1);
			text = text.trim();
			if (!text.startsWith("<body") && !text.endsWith("</body>"))
				text = "<body>" + text + "</body>";
			JEditorPane notesArea = new JEditorPane("text/html",
					"<html><head></head>" + text + "</html>");
			notesArea.setEditable(editable);
			notesArea.addHyperlinkListener(new SystemBrowser());
			notesArea.setMaximumSize(new Dimension(preferedWidth, 200));
			notesArea.setDoubleBuffered(true);
			notesArea.setBorder(BorderFactory.createLoweredBevelBorder());
			JScrollPane scroll = new JScrollPane(notesArea,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setPreferredSize(new Dimension(preferedWidth, 200));
			lh.add(scroll, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (sbase.getNumCVTerms() > 0) {
			lh.add(new JLabel("MIRIAM annotation: "), 1, ++row, 1, 1, 1, 1);
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");
			if (sbase.getNumCVTerms() > 1)
				sb.append("<ul>");
			for (CVTerm cvt : sbase.getCVTerms()) {
				if (sbase.getNumCVTerms() > 1)
					sb.append("<li>");
				String cvtString = cvt.toString();
				for (int k = 0; k < cvt.getNumResources(); k++) {
					String uri = cvt.getResourceURI(k);
					String loc[] = miriam.getLocations(uri);
					if (loc.length > 0) {
						String split[] = cvtString.split(uri);
						StringBuilder sbu = new StringBuilder();
						for (int l = 0; l < split.length - 1; l++) {
							if (l > 0) {
								sbu.append(", ");
							}
							StringTools.append(sbu, split[l], "<a href=\"",
									loc[0], "\">", uri, "</a>");
						}
						sbu.append(split[split.length - 1]);
						cvtString = sbu.toString();
					}
				}
				sb.append(cvtString);
				if (sbase.getNumCVTerms() > 1) {
					sb.append("</li>");
				}
			}
			if (sbase.getNumCVTerms() > 1) {
				sb.append("</ul>");
			}
			sb.append("</body></html>");
			JEditorPane l = new JEditorPane("text/html", sb.toString());
			l.addHyperlinkListener(new SystemBrowser());
			l.setEditable(editable);
			l.setBackground(Color.WHITE);
			Dimension dim = new Dimension(preferedWidth, 125);
			l.setMaximumSize(dim);
			JScrollPane scroll = new JScrollPane(l,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setMaximumSize(dim);
			scroll.setBorder(BorderFactory.createLoweredBevelBorder());
			lh.add(scroll, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		lh.add(new JLabel("SBO term: "), 1, ++row, 1, 1, 1, 1);
		JTextField sboTermField = new JTextField();
		sboTermField.setEditable(editable);
		if (sbase.isSetSBOTerm()) {
			sboTermField.setText(SBO.getTerm(sbase.getSBOTerm())
					.getDescription().replace("\\,", ","));
			sboTermField.setColumns(sboTermField.getText().length());
		}
		lh.add(sboTermField, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
	}

	/**
	 * 
	 * @param ssr
	 * @throws SBMLException 
	 */
	private void addProperties(SimpleSpeciesReference ssr) throws SBMLException {
		if (ssr.isSetSpecies()) {
			Model m = ssr.getModel();
			String idsOrNames[] = new String[m.getNumSpecies()];
			int index = 0;
			for (int i = 0; i < m.getNumSpecies(); i++) {
				Species s = m.getSpecies(i);
				idsOrNames[i] = s.isSetName() ? s.getName() : s.getId();
				if (s.getId().equals(ssr.getSpecies()))
					index = i;
			}
			JComboBox combo = new JComboBox(idsOrNames);
			combo.setSelectedIndex(index);
			combo.setEnabled(editable);
			lh.add(new JLabel("Species"), 1, ++row, 1, 1, 1, 1);
			lh.add(combo, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		if (ssr instanceof SpeciesReference)
			addProperties((SpeciesReference) ssr);
		else if (ssr instanceof ModifierSpeciesReference)
			addProperties((ModifierSpeciesReference) ssr);
		if (ssr.isSetSpecies())
			lh.add(new SBasePanel(ssr.getSpeciesInstance()), 1, ++row, 3, 1, 1,
					1);
	}

	/**
	 * 
	 * @param sbase
	 */
	private void addProperties(Species species) {
		if (species.isSetSpeciesType()) {
			lh.add(new JLabel("Species type: "), 1, ++row, 1, 1, 1, 1);
			JTextField tf = new JTextField(species.getSpeciesTypeInstance()
					.toString());
			tf.setEditable(editable);
			lh.add(tf, 1, ++row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		lh.add(new JLabel("Compartment: "), 1, ++row, 1, 1, 1, 1);
		JTextField tf = new JTextField(species.getCompartmentInstance()
				.toString());
		tf.setEditable(editable);
		lh.add(tf, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		if (species.isSetSpeciesType() || editable) {
			lh.add(new JLabel("Species type: "), 1, row, 1, 1, 1, 1);
			tf = new JTextField(species.getSpeciesTypeInstance().toString());
			tf.setEditable(editable);
			lh.add(tf, 1, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		JSpinner spinCharge = new JSpinner(new SpinnerNumberModel(species
				.getCharge(), -10, 10, 1));
		lh.add(new JLabel("Charge: "), 1, ++row, 1, 1, 1, 1);
		spinCharge.setEnabled(editable);
		lh.add(spinCharge, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		addProperties((Symbol) species);
		JCheckBox check = new JCheckBox("Boundary condition", species
				.getBoundaryCondition());
		check.setEnabled(editable);
		lh.add(check, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		check = new JCheckBox("Has only substance units", species
				.getHasOnlySubstanceUnits());
		check.setEnabled(editable);
		lh.add(check, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
	}

	/**
	 * 
	 * @param sbase
	 */
	private void addProperties(SpeciesReference specRef) {
		if (specRef.isSetStoichiometryMath()) {
			StoichiometryMath sMath = specRef.getStoichiometryMath();
			JPanel p = new JPanel(new GridLayout(1, 1));
			p.setBorder(BorderFactory.createTitledBorder(" "
					+ sMath.getClass().getCanonicalName() + ' '));
			sHotEqn eqn;
			try {
				eqn = new sHotEqn(sMath.getMath().compile(latex).toString()
						.replace("\\\\", "\\"));

				eqn.setBorder(BorderFactory.createLoweredBevelBorder());
				p.add(eqn);
			} catch (SBMLException e) {
				e.printStackTrace();
			}
			lh.add(p, 3, ++row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		} else {
			lh.add(new JLabel("Stoichiometry"), 1, ++row, 1, 1, 1, 1);
			JSpinner spinner = new JSpinner(new SpinnerNumberModel(specRef
					.getStoichiometry(), specRef.getStoichiometry() - 1000,
					specRef.getStoichiometry() + 1000, .1d));
			spinner.setEnabled(editable);
			lh.add(spinner, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
	}

	/**
	 * 
	 * @param s
	 */
	private void addProperties(Symbol s) {
		double val = Double.NaN;
		double min = 0d;
		double max = 9999.9;
		if (s instanceof Species) {
			Species species = (Species) s;
			String types[] = new String[] { "Initial amount",
					"Initial concentration" };
			boolean amount = true;
			if (species.isSetInitialAmount()) {
				val = species.getInitialAmount();
			} else if (species.isSetInitialConcentration()) {
				val = species.getInitialConcentration();
				amount = false;
			}
			JComboBox type = new JComboBox(types);
			type.setSelectedIndex(amount ? 0 : 1);
			type.setEnabled(editable);
			lh.add(type, 1, ++row, 1, 1, 1, 1);
		} else {
			String label = null;
			if (s instanceof Compartment) {
				Compartment c = (Compartment) s;
				if (c.isSetSize())
					val = c.getSize();
				label = "Size: ";
			} else {
				Parameter p = (Parameter) s;
				if (p.isSetValue())
					val = p.getValue();
				label = "Value: ";
			}
			lh.add(new JLabel(label), 1, ++row, 1, 1, 1, 1);
		}
		JSpinner spinValue = new JSpinner(new SpinnerNumberModel(val, Math.min(
				val, min), Math.max(val, max), .1d));
		spinValue.setEnabled(editable);
		lh.add(spinValue, 3, row, 1, 1, 0, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		lh
				.add(new JLabel(s instanceof Species ? "Substance unit: "
						: "Unit: "), 1, ++row, 1, 1, 1, 1);
		lh.add(unitPreview(s.getUnitsInstance()), 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		JCheckBox check = new JCheckBox("Constant", s.isConstant());
		check.setEnabled(editable);
		lh.add(check, 1, ++row, 3, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
	}

	/**
	 * 
	 * @param unit
	 */
	private void addProperties(Unit unit) {
		lh.add(new JLabel("Kind: "), 1, ++row, 1, 1, 1, 1);
		JComboBox unitSelection = new JComboBox();
		for (Unit.Kind unitKind : Unit.Kind.values()) {
			unitSelection.addItem(unitKind);
			if (unitKind.equals(unit.getKind()))
				unitSelection.setSelectedItem(unitKind);
		}
		unitSelection.setEditable(false);
		unitSelection.setEnabled(editable);
		lh.add(unitSelection, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		lh.add(new JLabel("Mutiplier: "), 1, ++row, 1, 1, 1, 1);
		JSpinner sMultiplier = new JSpinner(new SpinnerNumberModel(unit
				.getMultiplier(), -1000, 1000, 1));
		sMultiplier.setEnabled(editable);
		lh.add(sMultiplier, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		if (unit.getLevel() == 1
				|| (unit.getLevel() == 2 && unit.getVersion() == 1)) {
			lh.add(new JLabel("Offset: "), 1, ++row, 1, 1, 1, 1);
			JSpinner sOffset = new JSpinner(new SpinnerNumberModel(unit
					.getOffset(), -1000, 1000, 1));
			sOffset.setEnabled(editable);
			lh.add(sOffset, 3, row, 1, 1, 1, 1);
			lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		}
		lh.add(new JLabel("Scale: "), 1, ++row, 1, 1, 1, 1);
		JSpinner sScale = new JSpinner(new SpinnerNumberModel(unit.getScale(),
				-1000, 1000, 1));
		sScale.setEnabled(editable);
		lh.add(sScale, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		JSpinner sExponent = new JSpinner(new SpinnerNumberModel(unit
				.getExponent(), -24, 24, 1));
		sExponent.setEnabled(editable);
		lh.add(new JLabel("Exponent: "), 1, ++row, 1, 1, 1, 1);
		lh.add(sExponent, 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
	}

	/**
	 * 
	 * @param ud
	 * @throws SBMLException 
	 */
	private void addProperties(UnitDefinition ud) throws SBMLException {
		lh.add(new JLabel("Definition: "), 1, ++row, 1, 1, 1, 1);
		lh.add(unitPreview(ud), 3, row, 1, 1, 1, 1);
		lh.add(new JPanel(), 1, ++row, 5, 1, 0, 0);
		for (Unit u : ud.getListOfUnits())
			lh.add(new SBasePanel(u), 1, ++row, 3, 1, 1, 1);
	}

	/**
	 * Creates a JEditorPane that displays the given UnitDefinition as a HTML.
	 * 
	 * @param ud
	 * @return
	 */
	private JEditorPane unitPreview(UnitDefinition ud) {
		JEditorPane preview = new JEditorPane("text/html", GUITools
				.toHTML(ud != null ? HTMLFormula.toHTML(ud) : ""));
		preview.setEditable(false);
		preview.setBorder(BorderFactory.createLoweredBevelBorder());
		return preview;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * 
	 * @param editable
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
}
