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
 * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
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

import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Contains options for the {@link TranslatorPanel}.
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public abstract interface KEGGTranslatorPanelOptions extends de.zbit.graph.gui.options.TranslatorPanelOptions {
  
  
  /**
   * If true, shows the KEGGtranslator picture in every graph frame
   * as background image.
   */
	public static final Option<Boolean> SHOW_LOGO_IN_GRAPH_BACKGROUND = new Option<Boolean>(
		"SHOW_LOGO_IN_GRAPH_BACKGROUND", Boolean.class, String.format(
			"If true, shows the %s logo in the background of each graph.", System
					.getProperty("app.name")), false);

  /**
   * If true, shows the original KEGG picture in the background layer of a translated graph.
   */
  public static final Option<Boolean> SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND = new Option<Boolean>("SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND", Boolean.class,
      "If true, shows the original KEGG picture in the background layer of a translated graph.", true, SHOW_LOGO_IN_GRAPH_BACKGROUND, FALSE_RANGE);
  
  /**
   * Select percentage for brightening the KEGG background image.
   */
  public static final Option<Integer> BRIGHTEN_KEGG_BACKGROUND_IMAGE = new Option<Integer>("BRIGHTEN_KEGG_BACKGROUND_IMAGE", Integer.class,
      "Select percentage for brightening the KEGG background image.", new Range<Integer>(Integer.class, "{[0,100]}"), 65, SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND, TRUE_RANGE);

  /**
   * <code>TRUE</code> if the image should be converted to a greyscale image.
   */
  public static final Option<Boolean> GREYSCALE_KEGG_BACKGROUND_IMAGE = new Option<Boolean>("GREYSCALE_KEGG_BACKGROUND_IMAGE", Boolean.class,
      "If true, converts the KEGG background image to a greyscale picture.", true, SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND, TRUE_RANGE);
  
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup GRAPH_BACKGROUND_OPTIONS = new OptionGroup(
      "Graph background image",
      "Control the image or logo that is shown in the graph background.",
      SHOW_LOGO_IN_GRAPH_BACKGROUND, SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND, BRIGHTEN_KEGG_BACKGROUND_IMAGE, 
      GREYSCALE_KEGG_BACKGROUND_IMAGE
      //SHOW_NAVIGATION_AND_OVERVIEW_PANELS, SHOW_PROPERTIES_TABLE, LAYOUT_EDGES
      );
  
  
  
}
