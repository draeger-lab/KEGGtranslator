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
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class FileFilterKGML extends GeneralFileFilter {

    /**
     * The maximal number of lines to check for characteristic identifier in
     * KEGG files. If the first {@link #MAX_LINES_TO_PARSE} do not contain the
     * DOCTYPE entry for KEGG files, the file cannot be recognized as a
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
      return isKGML(f);
    }

    /**
     * Checks a) if the file endswith XML and b) if the doctype is KGML.
     * @param f
     * @return true if and only if the file is a KGML formatted file.
     */
    public static boolean isKGML(File f) {
      boolean retVal = false;
      if (f.getName().toUpperCase().endsWith(".XML")) {
        try {
          BufferedReader br = OpenFile.openFile(f.getAbsolutePath());
          String line;
          for (int i = 0; br.ready() && (i < MAX_LINES_TO_PARSE); i++) {
            line = br.readLine();
            if (line.toUpperCase().trim().startsWith("<!DOCTYPE")
                && line.contains("KGML")) {
              // Removed: "http://www.genome.jp/kegg/xml/KGML"
              retVal = true;
              break;
            }
          }
          br.close();
        } catch (Throwable e) {
          retVal = false;
        }
      }
      return retVal;
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
