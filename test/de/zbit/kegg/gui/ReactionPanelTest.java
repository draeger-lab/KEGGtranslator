/*
 * Copyright (c) 2011-2015 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package de.zbit.kegg.gui;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.TidySBMLWriter;

import de.zbit.sbml.gui.ReactionPanel;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.0
 * @version $Rev$
 */
public class ReactionPanelTest {
  
  /**
   * @param args
   * @throws SBMLException
   * @throws XMLStreamException
   */
  public static void main(String[] args) throws XMLStreamException,
  SBMLException {
    SBMLDocument doc = createSimpleSBMLDocument(1, 2, 2, false);
    TidySBMLWriter.write(doc, System.out, null, null);
    JFrame f = new JFrame("Reaction test");
    JPanel panel = new ReactionPanel(doc.getModel().getReaction(0), false);
    panel.setBackground(Color.WHITE);
    f.getContentPane().add(panel);
    f.setBackground(Color.WHITE);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.pack();
    //    f.setMinimumSize(new Dimension(200, 200));
    f.setLocationRelativeTo(null);
    f.setVisible(true);
  }
  
  /**
   * For testing
   * 
   * @return
   * @throws XMLStreamException
   */
  private static SBMLDocument createSimpleSBMLDocument(int numReactants,
    int numProducts, int numModifiers, boolean reversible) throws XMLStreamException {
    int i;
    SBMLDocument doc = new SBMLDocument(2, 4);
    Model model = doc.createModel("MyModel");
    model.setNotes("<notes><body xmlns=\"http://www.w3.org/1999/xhtml\">Bla bla</body></notes>");
    model.appendNotes("<notes><body xmlns=\"http://www.w3.org/1999/xhtml\"> foo bar</body></notes>");
    Compartment c = model.createCompartment("default");
    Species substrates[] = new Species[numReactants];
    Species products[] = new Species[numProducts];
    Species modifiers[] = new Species[numModifiers];
    for (i = 0; i < substrates.length; i++) {
      substrates[i] = model.createSpecies("s" + model.getNumSpecies(), c);
    }
    for (i = 0; i < products.length; i++) {
      products[i] = model.createSpecies("s" + model.getNumSpecies(), c);
    }
    for (i = 0; i < modifiers.length; i++) {
      modifiers[i] = model.createSpecies("s" + model.getNumSpecies(), c);
    }
    Reaction r = model.createReaction("R1");
    SpeciesReference specRef;
    for (Species s : substrates) {
      specRef = r.createReactant(s);
      if (Math.random() > .5) {
        specRef.setStoichiometry(Math.round(Math.random() * 10));
      }
    }
    for (Species s : products) {
      specRef = r.createProduct(s);
      if (Math.random() > .5) {
        specRef.setStoichiometry(Math.round(Math.random() * 10));
      }
    }
    for (Species s : modifiers) {
      r.createModifier(s);
    }
    r.setReversible(reversible);
    return doc;
  }
  
}
