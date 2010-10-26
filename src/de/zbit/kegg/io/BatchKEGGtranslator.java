package de.zbit.kegg.io;

import java.io.File;
import java.util.ArrayList;

import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.DirectoryParser;


/**
 * 
 * @author Clemens Wrzodek
 */
public class BatchKEGGtranslator {

  /**
   * 
   */
  private String changeOutdirTo = "";
  /**
   * 
   */
  private String orgOutdir = "";
  /**
   * Possible: SBML & GraphML. Default to graphML
   */
  private String outFormat = "GraphML";
  
  /**
   * 
   */
  private KEGGtranslator translator;
  
  /**
   * 
   * @param dir
   * @return
   */
  private String getAndCreateOutDir(String dir) {
    String myDir = dir;
    if (changeOutdirTo!=null && changeOutdirTo.length()>0) {
      myDir = changeOutdirTo + myDir.substring(orgOutdir.length());
      try {
        new File(myDir).mkdirs();
      } catch (Exception e) {} // Gibts schon...
    }
    return myDir;
  }
  
	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		BatchKEGGtranslator batch = new BatchKEGGtranslator();
		if (args != null && args.length > 0) {
			batch.setOrgOutdir(args[0]);
			if (args.length > 1)
				batch.setChangeOutdirTo(args[1]);
			batch.parseDirAndSubDir();
			return;
		}
		System.out.println("Demo Mode:");
		batch.setOrgOutdir(System.getProperty("user.home"));
		batch.setChangeOutdirTo(System.getProperty("user.home"));
		batch.parseDirAndSubDir();
	}
  
  /**
   * 
   * @return
   */
  public String getChangeOutdirTo() {
    return changeOutdirTo;
  }
  
  /**
   * 
   * @return
   */
  public KEGGtranslator getConverter() {
    return translator;
  }
  
  /**
   * 
   * @return
   */
  public String getOrgOutdir() {
    return orgOutdir;
  }
  
  /**
   * 
   * @return
   */
  public String getOutFormat() {
    return outFormat;
  }
  
  /**
   * 
   */
  public void parseDirAndSubDir() {
    parseDirAndSubDir(orgOutdir);
  }
  
  /**
   * 
   * @param dir
   */
  private void parseDirAndSubDir(String dir) {
    if (!dir.endsWith("/") && !dir.endsWith("\\"))
      if (dir.contains("\\")) dir+="\\"; else dir +="/";
    System.out.println("Parsing directory " + dir);
    
    boolean isGraphML = outFormat.equalsIgnoreCase("GraphML");
    if (translator==null) {
      if (outFormat.equalsIgnoreCase("sbml")) {
        translator = new KEGG2jSBML();
      } else if (outFormat.equalsIgnoreCase("GraphML")) {
        translator = new KEGG2GraphML();
      } else {
        System.err.println("Unknwon output Format: '" + outFormat + "'.");
        return;
      }
    }
    
    DirectoryParser dp = new DirectoryParser(dir);
    while (dp.hasNext()) {
      String fn = dp.next();
      
      //if (fn.equals("gml")|| fn.equals("metabolic")) continue;
      
      if (new File(dir+fn).isDirectory()) {
        parseDirAndSubDir(dir + fn);
      } else if (fn.toLowerCase().trim().endsWith(".xml")) {
        // Test if outFile already exists. Assumes: 1 Pathway per file. (should be true for all files... not crucial if assumption is wrong)
        String myDir = getAndCreateOutDir(dir);
        String outFileTemp = myDir + fn.trim().substring(0, fn.trim().length()-4) + (isGraphML?".graphML":".sbml.xml");
        if (new File(outFileTemp).exists()) continue; // Skip already converted files.

        // Parse and convert all Pathways in XML file.
        ArrayList<Pathway> pw=null;
        try {
          pw = de.zbit.kegg.parser.KeggParser.parse(dir+fn);
        } catch (Throwable t) {t.printStackTrace();} // Show must go on...
        if (pw==null || pw.size()<1) continue;
        
        boolean appendNumber=(pw.size()>1);
        for (int i=0; i<pw.size(); i++) {
          String outFile = myDir + fn.trim().substring(0, fn.trim().length()-4) + (appendNumber?"-"+(i+1):"") + (isGraphML?".graphML":".sbml.xml");
          if (new File(outFile).exists()) continue; // Skip already converted files.
          
          // XXX: Main Part
          try {
			translator.translate(pw.get(i), outFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
          
          if (translator.isLastFileWasOverwritten()) { // Datei war oben noch nicht da, sp�ter aber schon => ein anderer prezess macht das selbe bereits.
            System.out.println("It looks like another instance is processing the same files. Going to next subfolder.");
            return; // Function is recursive.
          }
        }
        
        
      }
    }
  }
  
  /**
   * 
   * @param changeOutdirTo
   */
  public void setChangeOutdirTo(String changeOutdirTo) {
    this.changeOutdirTo = changeOutdirTo;
  }

  /**
   * Set the translator you wish to use. This will determine the
   * output format of this class.
   * @param translator
   */
  public void setTranslator(KEGGtranslator translator) {
    this.translator = translator;
  }
  
  /**
   * 
   * @param orgOutdir
   */
  public void setOrgOutdir(String orgOutdir) {
    this.orgOutdir = orgOutdir;
  }

  /**
   * @param outFormat - "graphml" or "sbml".
   */
  public void setOutFormat(String outFormat) {
    this.outFormat = outFormat;
  }
  
}
