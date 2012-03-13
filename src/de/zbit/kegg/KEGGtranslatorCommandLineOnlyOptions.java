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
package de.zbit.kegg;

import de.zbit.cache.InfoManagement;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;

/**
 * Options for KEGGtranslator that are supposed only for
 * the command-line.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface KEGGtranslatorCommandLineOnlyOptions extends KeyProvider {
  
  /**
   * Size to take when initializing {@link KeggInfoManagement}.
   */
  public static final Option<Integer> CACHE_SIZE = new Option<Integer>("CACHE_SIZE",Integer.class,
      "Specify the number of API entries from KEGG to keep into cache (default: 10000).", 
      new Range<Integer>(Integer.class, "{[100,1000000]}"), 10000, "-cache");
  
  /**
   * Causes a call to {@link InfoManagement#clearFailCache()} on startup.
   */
  public static final Option<Boolean> CLEAR_FAIL_CACHE = new Option<Boolean>("CLEAR_FAIL_CACHE",Boolean.class,
      "Clear the cache of failed KEGG API queries (cases the application to retry those IDs).",Boolean.FALSE,
      Boolean.FALSE);
  
}
