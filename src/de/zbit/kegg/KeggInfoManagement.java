package de.zbit.kegg;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;

import de.zbit.exception.UnsuccessfulRetrieveException;
import de.zbit.util.InfoManagement;
import de.zbit.util.Utils;

/**
 * Retrieve and manage Kegg infos. Once retrieved, they are cached and don't have to
 * be retrieved again. Also a "precache" statement is possible, to quickly cache and download
 * many kegg IDs simultaneously.
 * @author wrzodek
 */
public class KeggInfoManagement extends InfoManagement<String, String> implements Serializable {
  private static final long serialVersionUID = -2621701345149317801L;
  private boolean hasChanged=false;
  
  private KeggAdaptor adap=null;
  
  /**
   * If this flag ist set to true, this class does NOT retrieve any Information, but uses stored information.
   */
  public static boolean offlineMode = false;
      
  /**
   * 
   */
  public KeggInfoManagement () {
    super();
    this.adap = new KeggAdaptor();
  }
  
  /**
   * 
   * @param maxListSize
   * @param adap
   */
  public KeggInfoManagement (int maxListSize, KeggAdaptor adap) {
    super(maxListSize); // Remember maxListSize queries at max.
    this.adap = adap;
  }
  
  /*
   * (non-Javadoc)
   * @see de.zbit.util.InfoManagement#cleanupUnserializableObject()
   */
  @Override  
  protected void cleanupUnserializableObject() {
    adap = null;
  }

  /*
   * (non-Javadoc)
   * @see de.zbit.util.InfoManagement#fetchInformation(java.lang.Comparable)
   */
  @Override
  protected String fetchInformation(String id) throws TimeoutException, UnsuccessfulRetrieveException {
    if (offlineMode) throw new TimeoutException();
    hasChanged=true;
    
    if (adap==null) adap = getKeggAdaptor(); // create new one
    String ret = adap.getWithReturnInformation(id);
    if (ret==null || ret.trim().length()==0) throw new UnsuccessfulRetrieveException(); // Will cause the InfoManagement class to remember this one.
    
    ret = removeUnnecessaryInfos(ret);
    
    return ret; // Successfull and "with data" ;-) 
  }
  
  /*
   * (non-Javadoc)
   * @see de.zbit.util.InfoManagement#fetchMultipleInformations(IDtype[])
   */
  /**
   * Wrapper for {@link fetchMultipleInformationsUpTo100AtATime} because Kegg only supports 100 at a time :)
   */
  @Override
  protected String[] fetchMultipleInformations(String[] ids) throws TimeoutException, UnsuccessfulRetrieveException {
    String[] realRet;
    if (ids.length<=100) {
      realRet = fetchMultipleInformationsUpTo100AtATime(ids);
    } else {
      realRet = new String[ids.length];

      // Instead of requesting all objects at once, splitts Queries to 100 max and concatenates the results... that's it.
      int i=0;
      while (i<ids.length) {
        String[] subArr = new String[Math.min(100, ids.length-i)];
        System.arraycopy(ids, i, subArr, 0, subArr.length);

        String[] ret = fetchMultipleInformationsUpTo100AtATime(subArr);
        System.arraycopy(ret, 0, realRet, i, ret.length);

        i+=subArr.length;
      }
    }
    realRet = removeUnnecessaryInfos(realRet);
    
    return realRet;
  }

  /**
   * Do not call this class by yourself.
   * It's just a helper method for {@link fetchMultipleInformations}
   * @param ids
   * @return
   * @throws TimeoutException
   * @throws UnsuccessfulRetrieveException
   */
  private String[] fetchMultipleInformationsUpTo100AtATime(String[] ids) throws TimeoutException, UnsuccessfulRetrieveException {
    if (offlineMode) throw new TimeoutException();
    if (ids == null) return null;
    if (ids.length<1) return new String[0];
    hasChanged=true;
    
    if (adap==null) adap = getKeggAdaptor(); // create new one
    String q = adap.getWithReturnInformation(concatenateKeggIDs(ids));
    if (q==null || q.trim().length()==0) throw new UnsuccessfulRetrieveException(); // Will cause the InfoManagement class to remember all.
    
    String[] splitt = q.split("///");
    
    String[] ret = new String[ids.length];
    for (int i=0; i<ret.length; i++) ret[i] = null; // Initialite all non-successfull ids.
    int numMissing = 0;
    for (int i=0; i<splitt.length; i++) {
      if (splitt[i]==null || splitt[i].trim().length()==0) {splitt[i]=null; continue;}
      
      // Extract Entry id of current dataset
      String aktEntryID = KeggAdaptor.extractInfo(splitt[i], "ENTRY", "  ");
      if (aktEntryID==null || aktEntryID.length()==0) {
        // Sollte NIE vorkommen, da kegg immer einen ENTRY-Eintrag mitgiebt.
        System.err.println("No Entry id found in Text:\n" + splitt[i].substring(0, Math.min(150, splitt[i].length())) + "...\n------------");
        continue;
      }
      
      
      // Look if maybe the indices of entry and return value are the same. This is the case, until the first invalid (or "not found") kegg id.
      /*
       * Folgendes Prinzip: Kegg schickt immer Ergebnisse in der Reihenfolge, in der auch die IDs geschickt wurden. Ist mal kein
       * Ergebnis vorhanden, shiften sich die ids um 1 mehr. ... Rest, siehe implementierung ;-)
       */
      int idIndex = i+numMissing;
      boolean found = false;
      int minNumMissing = numMissing; boolean takeNotSoSureHits=false;
      String aktQueryID;
      while (idIndex<ids.length) {
        idIndex = i+numMissing;
        if (idIndex>= ids.length) {
          // z.B. Query (GN:)"HSA" liefert eine Entry ID "T01001" zur�ck. Das findet man nicht so einfach. Deshalb komplett durchlaufen lassen
          // und spaeter noch mal unschaerfer suchen.
          numMissing = minNumMissing;
          idIndex = i+numMissing;
          if (takeNotSoSureHits) break; // ... should never happen.
          takeNotSoSureHits = true;
        }
        aktQueryID = (ids[idIndex].contains(":")? ids[idIndex].substring(ids[idIndex].indexOf(':')+1):ids[idIndex]).trim().toUpperCase();
        if (aktQueryID.equalsIgnoreCase(aktEntryID) 
            || ("EC " + aktQueryID).equalsIgnoreCase(aktEntryID) // Enzyme werden ohne "EC " gequeried, kommen aber MIT zur�ck... 
            || (takeNotSoSureHits && Utils.isWord(splitt[i].toUpperCase(), aktQueryID))) { // Siehe obiges Beispiel.
          ret[idIndex] = splitt[i]; // Aufpassen. Hier nur i, da index von splitt und id2 hier gleich!
          found = true;
          break;
        }
        numMissing++;
      }
      
      if (!found) {
        System.err.println("No id found for result:\n" + splitt[i].substring(0, Math.min(150, splitt[i].length())) + "...\n-----------------\nThis should not happen!");
      }
      
    }
    
    return ret; // Successfull and "with data" ;-) 
  }

  /**
   * 
   * @return
   */
  public KeggAdaptor getKeggAdaptor() {
    if (adap==null) adap = new KeggAdaptor();
    return adap;
  }
  
  /**
   * This function allows you to extend this class and overwrite this function.
   * Then you can remove all information from the KeggString which you don't need.
   * This may save you a lot of RAM. Please keep this Class as generic as possible.
   * So don't implement this function here directly!
   * 
   * This class must be public, so the user can override it. Please never call this
   * function from outside this class.
   * @return
   */
  public String removeUnnecessaryInfos(String ret) {
    /* Example for content of ret:
ENTRY       8491              CDS       H.sapiens
NAME        MAP4K3
DEFINITION  mitogen-activated protein kinase kinase kinase kinase 3
            (EC:2.7.11.1)
ORTHOLOGY   K04406  mitogen-activated protein kinase kinase kinase kinase 3
                    [EC:2.7.11.1]
PATHWAY     hsa04010  MAPK signaling pathway
CLASS       Metabolism; [...]
     */
    return ret;
  }

  /**
   * This function allows you to extend this class and overwrite this function.
   * Then you can remove all information from the KeggString which you don't need.
   * This may save you a lot of RAM. Please keep this Class as generic as possible.
   * So don't implement this function here directly!
   * @return
   */
  private String[] removeUnnecessaryInfos(String[] realRet) {
    for (int i=0; i<realRet.length; i++)
      realRet[i] = removeUnnecessaryInfos(realRet[i]);
    return realRet;
  }

  /*
   * (non-Javadoc)
   * @see de.zbit.util.InfoManagement#restoreUnserializableObject()
   */
  @Override
  protected void restoreUnserializableObject () {
    adap = getKeggAdaptor();
    hasChanged=false;
  }
  /**
   * 
   * @param adap
   */
  public void setKeggAdaptor(KeggAdaptor adap) {
    this.adap = adap;
  }
  
  /**
   * @return has the content of this class changed, since initilization/ Loading?
   */
  public boolean hasChanged() {
    return hasChanged;
  }
  
  /**
   * Requiered for queries to the KeggDB.
   * @param ids
   * @return every id in the array in one string, separated by a whitespace.
   */
  private static String concatenateKeggIDs(String[] ids) {
    String ret = "";
    for (String s: ids)
      ret+=s.replace(" ", "")+" ";
    return ret.trim();
  }
}
