package de.zbit.kegg.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import de.zbit.util.DirectoryParser;


/**
 * Small bugfixes in generated graphML files.
 * Much faster than re-generating all files.
 * @author wrzodek
 */
public class FixFiles {
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    DirectoryParser dp = new DirectoryParser(args.length<1?"Y:\\graphML":args[0], ".graphML");
    dp.setRecurseIntoSubdirectories(true);
    
    while (dp.hasNext()) {
      String curFile = dp.getPath() + dp.next();
      String tempFile = curFile+".tmp";
      try {
        BufferedReader in  = new BufferedReader(new FileReader(curFile));
        BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
        String line;
        
        boolean changed = false;
        while ((line=in.readLine())!=null) {
          
          // 1. Compound => Small molecule (aber nur bei nodes, nicht edge labels oder so).
          if (line.contains("<data key=\"d6\">") && line.contains("compound")) {
            line = line.replace("compound", "small molecule");
            changed = true;
          }

          // 2. Fix separation of kegg IDs by " " instead of "," (should be: ",").
          if (line.contains("<data key=\"d4\">") && line.trim().contains(" ")) {
            int spaces = line.indexOf("<data key=\"d4\">");
            line = replicateChar(' ', spaces) + line.trim().replace(" ", ",");
            changed = true;
          }
          
          // 3. number 3 has a bug.
          if (line.contains("<data,key=\"d4\">")) {
            line = line.replace("<data,key=\"d4\">", "<data key=\"d4\">");
            changed = true;
          }
          
          // 4. "gene,gene,gene" -> "gene", "gene" -> "protein"
          if (line.contains("<data key=\"d6\">")) {
            String orgLine = line;
            int spaces = line.indexOf("<data key=\"d6\">");
            
            String eType;
            if (line.contains(",")) {
              eType = line.substring(line.lastIndexOf("[", line.indexOf(","))+1, line.indexOf(","));
            } else {
              eType = line.substring(line.lastIndexOf("[", line.indexOf("]"))+1, line.indexOf("]"));
            }
            if (eType.equalsIgnoreCase("gene")) eType = "protein";
            
            line = replicateChar(' ', spaces) + "<data key=\"d6\"><![CDATA[" + eType + "]]></data>";
            if (!orgLine.equals(line)) changed = true;
          }            
          
          
          out.write(line+"\n");
        }
        in.close();
        out.close();
        
        if (changed) {
          (new File(curFile)).delete();
          (new File(tempFile)).renameTo(new File(curFile));
          System.out.println(curFile + " - Success.");
        } else {
          (new File(tempFile)).delete();
        }
        
      } catch (Exception e) {e.printStackTrace();}
    }
    
  }
  
  /**
   * 
   * @param c
   * @param xTimes
   * @return
   */
  public static String replicateChar(char c, int xTimes) {
    String ret = "";
    for (int i=0; i<xTimes; i++)
      ret+=c;
    return ret;
  }
  
}
