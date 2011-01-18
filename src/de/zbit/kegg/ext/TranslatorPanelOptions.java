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
package de.zbit.kegg.ext;

import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * Contains options for the {@link TranslatorPanel}.
 * @author wrzodek
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
