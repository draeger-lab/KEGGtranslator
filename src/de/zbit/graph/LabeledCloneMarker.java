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
 * The labeled clone marker includes an identifying label that can be used to
 * identify equivalent clones elsewhere in the map. This is particularly useful
 * for stateful EPNs, because these can have a large number of state variables
 * displayed and therefore may be dificult to visually identify as being
 * identical.
 * 
 * <p>
 * From the official SBGN-PD spec:<br/>
 * <i>A clone marker is identified by a label placed in an unbordered box
 * containing a string of characters. The characters can be distributed on
 * several lines to improve readability, although this is not mandatory. The
 * label box must be attached to the center of the container. The label may
 * spill outside of the container (the portion of the surface of the EPN that
 * has been modified visually). The font color of the label and the color of the
 * clone marker should contrast with one another. The label on a labeled clone
 * marker is mandatory.</i>
 * </p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface LabeledCloneMarker extends CloneMarker {
  
  /**
   * Set the clone markers label
   * @param label
   */
  public void setCloneMarkerLabel(String label);
  
  /**
   * Get the clone marker label
   * @return
   */
  public String getCloneMarkerLabel();
}
