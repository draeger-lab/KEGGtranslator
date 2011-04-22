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
package de.zbit.kegg.io;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;

import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.parser.pathway.Pathway;

/**
 * @author Martijn van Iersel
 * @author Andreas Dr&auml;ger
 * @date 2011-04-22
 * @version $Rev$
 */
public class KEGG2SBGN extends AbstractKEGGtranslator {

	/**
	 * @param manager
	 */
	public KEGG2SBGN(KeggInfoManagement manager) {
		super(manager);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see de.zbit.kegg.io.AbstractKEGGtranslator#isOutputFunctional()
	 */
	@Override
	public boolean isOutputFunctional() {
		return false;
	}

	/* (non-Javadoc)
	 * @see de.zbit.kegg.io.AbstractKEGGtranslator#translateWithoutPreprocessing(de.zbit.kegg.parser.pathway.Pathway)
	 */
	@Override
	protected Object translateWithoutPreprocessing(Pathway p) {
		Sbgn sbgn = new Sbgn();
		Map map = new Map();
		sbgn.setMap (map);
		
		for (de.zbit.kegg.parser.pathway.Entry e : p.getEntries())
		{
			Glyph g = new Glyph();
			int x = e.getGraphics().getX();
			int y = e.getGraphics().getY();
			Bbox bb = new Bbox();
			bb.setX(x);
			bb.setY(y);
			bb.setW(10);
			bb.setH(10);
			g.setClazz("macromolecule");
			g.setBbox(bb);
			Label l = new Label();
			l.setText(e.getName());
			g.setLabel(l);
			
			map.getGlyph().add(g);
		}
		// TODO Auto-generated method stub
		return sbgn;
	}

	/* (non-Javadoc)
	 * @see de.zbit.kegg.io.AbstractKEGGtranslator#writeToFile(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean writeToFile(Object doc, String outFile) {
		try {
			SbgnUtil.writeToFile((Sbgn)doc, new File (outFile));
			return true;
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			return false;
		}
	}

}
