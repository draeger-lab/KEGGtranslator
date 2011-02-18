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

import de.zbit.gui.prefs.FileHistory;

/**
 * This interface is only needed to distinguish between files that have been
 * loaded by KEGtranslator from those files that have been used by other
 * programs. With the help of this interface the correct keys will be loaded
 * from the user's configuration, because of the specific package in which
 * this extension of {@link FileHistory} is located.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-12-11
 * @since 1.0
 */
public interface KEGGtranslatorHistory extends FileHistory {
	
}
