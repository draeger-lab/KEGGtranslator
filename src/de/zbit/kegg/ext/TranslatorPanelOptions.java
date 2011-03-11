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
package de.zbit.kegg.ext;

import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * Contains options for the {@link TranslatorPanel}.
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public abstract interface TranslatorPanelOptions extends KeyProvider{
  
  /**
   * If true, shows the KEGGtranslator picture in every graph frame
   * as background image.
   */
  public static final Option<Boolean> SHOW_LOGO_IN_GRAPH_BACKGROUND = new Option<Boolean>("SHOW_LOGO_IN_GRAPH_BACKGROUND",Boolean.class,
      "If true, shows the " + KEGGtranslator.APPLICATION_NAME + " logo in the background of each graph.", false);

  /**
   * Shows an overview and navigation panel in every graph frame.
   */
  public static final Option<Boolean> SHOW_NAVIGATION_AND_OVERVIEW_PANELS = new Option<Boolean>("SHOW_NAVIGATION_AND_OVERVIEW_PANELS",Boolean.class,
      "If true, shows a navigation and overview panel on the left side of each graph.", true);

  /**
   * Show a table with the node/edge properties on the right side.
   */
  public static final Option<Boolean> SHOW_PROPERTIES_TABLE = new Option<Boolean>("SHOW_PROPERTIES_TABLE",Boolean.class,
      "If true, shows a properties table on the right side of each graph.", true);
  
  
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> GRAPH_PANEL_OPTIONS = new OptionGroup<Boolean>(
      "Graph visualization options",
      "Define various options that control the look and feel of GraphML visualizing panels.",
      SHOW_LOGO_IN_GRAPH_BACKGROUND, SHOW_NAVIGATION_AND_OVERVIEW_PANELS, SHOW_PROPERTIES_TABLE);
  
  
  
}
