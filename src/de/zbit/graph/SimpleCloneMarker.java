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
 * Copyright (C) 2010-2012 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.graph;

/**
 * The {@link SimpleCloneMarker} is the unlabeled version of the
 * {@link CloneMarker}.
 * 
 * <p>
 * From the official SBGN-PD spec:<br/>
 * <i>The simple (unlabeled) clone marker is a portion of the surface of an EPN
 * that has been modifed visually through the use of a different shade, texture,
 * or color. Figure 2.5 illustrates this. The clone marker occupies the lower
 * part of the EPN. The filled area must be smaller than the unfilled one.</i>
 * </p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface SimpleCloneMarker extends CloneMarker {
  /*
   *  All methods already contained in the parental CloneMarker.
   */
}
