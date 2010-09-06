package de.zbit.kegg.parser.pathway;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.zbit.kegg.parser.KeggParser;

/**
 * Corresponding to the Kegg ReactionComponent class (see {@link http://www.genome.jp/kegg/xml/docs/})
 * Includes the Kegg "alt" class.
 * @author wrzodek
 */
public class ReactionComponent {
  
  /**
   * 
   */
  String name;
  /**
   * 
   */
  ReactionComponent alt = null;
  
  /**
   * 
   * @param name
   */
  public ReactionComponent(String name) {
    super();
    this.name = name;
  }
  
  /**
   * 
   * @param name
   * @param nl
   */
  public ReactionComponent(String name, NodeList nl) {
    this(name);
    if (nl==null) return;
    
    // Parse child ("Alt's") from nodeList
    for (int i=0; i<nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node==null) continue;
      
      NamedNodeMap att = node.getAttributes();
      if (node.getNodeName().trim().equalsIgnoreCase("alt"))
        alt = new ReactionComponent(KeggParser.getNodeValue(att, "name"), node.getChildNodes());
    }
  }
  
  /**
   * 
   * @return
   */
  public ReactionComponent getAlt() {
    return alt;
  }
  
  /**
   * 
   * @return
   */
  public String getName() {
    return name;
  }
  
  /**
   * 
   * @return
   */
  public boolean hasAlt() {
    return (alt!=null);
  }

  /**
   * 
   * @param alt
   */
  public void setAlt(ReactionComponent alt) {
    this.alt = alt;
  }
  
  /**
   * 
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

}
