/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
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

import java.io.BufferedReader;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import de.zbit.io.GeneralFileFilter;
import de.zbit.io.OpenFile;

/**
 * An implementation of {@link java.io.FileFilter} and extension of
 * {@link FileFilter} that recognizes KGML files. It also accepts directories.
 * 
 * @author Andreas Dr&auml;ger
 */
public class FileFilterKGML extends GeneralFileFilter {

    /**
     * The maximal number of lines to check for characteristic identifier in
     * KEGG files. If the first {@link #MAX_LINES_TO_PARSE} do not contain the
     * DOCTYPE entry for KEGG files including a link that start with
     * "http://www.genome.jp/kegg/xml/KGML", the file cannot be recognized as a
     * valid KGML file.
     */
    private static final int MAX_LINES_TO_PARSE = 20;

    /*
     * (non-Javadoc)
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    @Override
    public boolean accept(File f) {
      if (f==null) return false;
	if (f.isDirectory()) {
	    return true;
	}
	if (f.getName().toUpperCase().endsWith(".XML")) {
	    try {
		BufferedReader br = OpenFile.openFile(f.getAbsolutePath());
		String line;
		for (int i = 0; br.ready() && (i < MAX_LINES_TO_PARSE); i++) {
		    line = br.readLine();
		    if (line.toUpperCase().startsWith("<!DOCTYPE")
			    && line
				    .contains("http://www.genome.jp/kegg/xml/KGML")) {
			return true;
		    }
		}
	    } catch (Throwable e) {
		return false;
	    }
	}
	return false;
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    @Override
    public String getDescription() {
	return "KGML files (*.xml)";
    }

}
