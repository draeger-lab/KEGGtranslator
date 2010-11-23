package de.zbit.kegg.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.zbit.kegg.KeggInfoManagement;
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
  @SuppressWarnings("unchecked")
  private void parseDirAndSubDir(String dir) {
    KeggInfoManagement manager = loadCache();
    
    if (!dir.endsWith("/") && !dir.endsWith("\\"))
      if (dir.contains("\\")) dir+="\\"; else dir +="/";
    System.out.println("Parsing directory " + dir);
    
    
    if (translator==null) {
    	translator = getTranslator(outFormat, manager);
    }
    String fileExtension = getFileExtension(translator);
		  
    
    DirectoryParser dp = new DirectoryParser(dir);
    while (dp.hasNext()) {
      String fn = dp.next();
      
      //if (fn.equals("gml")|| fn.equals("metabolic")) continue;
      
      if (new File(dir+fn).isDirectory()) {
        parseDirAndSubDir(dir + fn);
      } else if (fn.toLowerCase().trim().endsWith(".xml")) {
        // Test if outFile already exists. Assumes: 1 Pathway per file. (should be true for all files... not crucial if assumption is wrong)
        String myDir = getAndCreateOutDir(dir);
        String outFileTemp = myDir + fn.trim().substring(0, fn.trim().length()-4) + fileExtension;
        if (new File(outFileTemp).exists()) continue; // Skip already converted files.
        
        // Parse and convert all Pathways in XML file.
        List<Pathway> pw=null;
        try {
          pw = de.zbit.kegg.parser.KeggParser.parse(dir+fn);
        } catch (Throwable t) {t.printStackTrace();} // Show must go on...
        if (pw==null || pw.size()<1) continue;
        
        boolean appendNumber=(pw.size()>1);
        for (int i=0; i<pw.size(); i++) {
          String outFile = myDir + fn.trim().substring(0, fn.trim().length()-4) + (appendNumber?"-"+(i+1):"") + fileExtension;
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
    
    // Remember already queried objects (save cache)
    if ((translator instanceof AbstractKEGGtranslator) &&
        ((AbstractKEGGtranslator) translator).getKeggInfoManager().hasChanged()) {
      KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, 
          ((AbstractKEGGtranslator) translator).getKeggInfoManager());
    }
  }

  /**
   * Returns a KeggTranslater for the given outFormat.
   * @param outFormat
   * @param manager
   * @return
   */
	public static KEGGtranslator getTranslator(String outFormat, KeggInfoManagement manager) {
		KEGGtranslator translator;
		
		if (outFormat.equalsIgnoreCase("sbml")) {
		  translator = new KEGG2jSBML(manager);

    } else if (outFormat.equalsIgnoreCase("LaTeX")) {
      translator = new KEGG2jSBML(manager);
		  
		} else if (outFormat.equalsIgnoreCase("GraphML")) {
		  translator = KEGG2yGraph.createKEGG2GraphML(manager);
		  
		} else if (outFormat.equalsIgnoreCase("GML")) {
		  translator = KEGG2yGraph.createKEGG2GML(manager);

		} else if (outFormat.equalsIgnoreCase("JPG")) {
		  translator = KEGG2yGraph.createKEGG2JPG(manager);
		  
		} else if (outFormat.equalsIgnoreCase("GIF")) {
		  translator = KEGG2yGraph.createKEGG2GIF(manager);
		  
		} else if (outFormat.equalsIgnoreCase("YGF")) {
		  translator = KEGG2yGraph.createKEGG2YGF(manager);
		  
		} else if (outFormat.equalsIgnoreCase("TGF")) {
		  translator = KEGG2yGraph.createKEGG2TGF(manager);
		  
		} else {
		  System.err.println("Unknwon output Format: '" + outFormat + "'.");
		  translator = null;
		}
		
		return translator;
	}
	
	/**
	 * Returns the file extesion (with preceding dot) for the
	 * given KEGGtranslator.
	 * @param translator
	 * @return
	 */
	public static String getFileExtension(KEGGtranslator translator) {
		String fileExtension = ".translated";
    if (translator instanceof KEGG2yGraph) {
      fileExtension = ((KEGG2yGraph)translator).getOutputHandler().getFileNameExtension();
    } else if (translator instanceof KEGG2jSBML) {
		  fileExtension = ".sbml.xml";
    }
		if (!fileExtension.startsWith(".")) fileExtension = "." + fileExtension;
		
		return fileExtension;
	}
  
  /**
   * Load or create a KeggInfoManager
   * @return KeggInfoManagement
   */
  private KeggInfoManagement loadCache() {
    KeggInfoManagement manager=null;
    if (new File(KEGGtranslator.cacheFileName).exists()
        && new File(KEGGtranslator.cacheFileName).length() > 0) {
      try {
        manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(KEGGtranslator.cacheFileName);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    if (manager==null){
      manager = new KeggInfoManagement();
    }
    
    return manager;
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
