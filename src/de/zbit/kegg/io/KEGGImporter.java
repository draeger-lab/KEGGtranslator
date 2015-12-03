/*
 * $Id: KEGGImporter.java 405 2015-02-12 10:32:23Z roemer $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/kegg/io/KEGGImporter.java $
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2010-2015 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.io;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.SwingWorker;

import de.zbit.gui.GUITools;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.KGMLSelectAndDownload;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.NotifyingWorker;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * A {@link SwingWorker} that handles downloads of KGML-xmls
 * and translations to destination formats.
 * 
 * @author Clemens Wrzodek
 * @version $Rev: 405 $
 */
public class KEGGImporter extends NotifyingWorker<Object> {
  /**
   * 
   */
  private String KEGGpathwayID;
  /**
   * 
   */
  private File inputFile = null;
  /**
   * Alternative to {@link #inputFile}.
   */
  private Pathway inputPathway = null;
  /**
   * 
   */
  private Format outputFormat;
  
  AbstractKEGGtranslator<?> translator;
  
  /**
   * This will download and translate the given pathway.
   * @param KEGGpathwayID
   * @param outputFormat
   */
  public KEGGImporter(String KEGGpathwayID, Format outputFormat) {
    super();
    this.KEGGpathwayID = KEGGpathwayID;
    this.outputFormat = outputFormat;
  }
  
  /**
   * This will only translate the given pathway.
   * @param inputFile
   * @param outputFormat
   */
  public KEGGImporter(File inputFile, Format outputFormat) {
    super();
    this.inputFile = inputFile;
    this.outputFormat = outputFormat;
  }
  
  /**
   * This will only translate the given pathway.
   * @param inputPathway
   * @param outputFormat
   */
  public KEGGImporter(Pathway inputPathway, Format outputFormat) {
    super();
    inputFile = null;
    this.inputPathway = inputPathway;
    this.outputFormat = outputFormat;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  protected Object doInBackground() throws Exception {
    if ((inputFile == null) && (inputPathway == null)) {
      // PART0: Notify that we are goint to download something
      publish(new ActionEvent(this, 1, KEGGpathwayID));
      
      // PART1: Download
      String localFile = null;
      try {
        localFile = KGMLSelectAndDownload.downloadPathway(KEGGpathwayID, false);
      } catch (Exception exc) {
        // Overview pathways are no longer available through KEGG API as of Sept. 2014, must be fetched from FTP server by user
        if (KEGGpathwayID.startsWith("011", 3) || KEGGpathwayID.startsWith("012", 3)) {
          // Show message for overview pathways
          GUITools.showErrorMessage(null, "The overview pathway maps are no longer available for free download! To visualize overview pathways, please acquire the kgml-files from the KEGG FTP server (see http://www.kegg.jp/kegg/download/) and place them in the res/kgml/ folder.");
        } else {
          // Mostly 1) pathway does not exists for organism or 2) no connection to server
          GUITools.showErrorMessage(null, exc);
        }
      }
      
      // PART2: Fire listener that we are done with downloading
      publish(new ActionEvent(this, 2, localFile));
      inputFile = new File(localFile);
    }
    
    // PART3: Translate
    if ((inputFile != null) || (inputPathway != null)) {
      // The order in which the following events happen is important
      translator = (AbstractKEGGtranslator<?>) BatchKEGGtranslator.getTranslator(outputFormat, Translator.getManager());
      
      // The following should also trigger a new progress bar!
      publish(new ActionEvent(translator, 3, null));
      
      translator.setProgressBar(getProgressBar());
      Object result;
      if (inputPathway != null) {
        result = translator.translate(inputPathway);
      } else {
        result = translator.translate(inputFile);
      }
      
      publish(new ActionEvent(result, 4, null));
      
      return result;
    }
    
    // Remove this from list of listeners
    publish(new ActionEvent(this, 5, null));
    
    return null;
  }
  
  @Override
  public void setProgressBar(AbstractProgressBar progress) {
    super.setProgressBar(progress);
    
    if (translator!=null) { // Uh-oh, there is already a translation running!
      translator.setProgressBar(progress);
    }
  }
}
