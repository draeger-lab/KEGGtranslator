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
 */
public interface KEGGtranslatorHistory extends FileHistory {
	
}
