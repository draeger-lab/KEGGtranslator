package de.zbit.kegg.parser.pathway;

/**
 * Corresponding to the Kegg Entry class (see {@link http://www.genome.jp/kegg/xml/docs/})
 * @author wrzodek
 */
import java.util.ArrayList;


import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.zbit.kegg.parser.KeggParser;

public class Entry {
  /**
   * 
   */
  int id = 0; // id.type    the ID of this entry in the pathway map
  /**
   * 
   */
  String name = ""; // keggid.type    the KEGGID of this entry
  /**
   * 
   */
  EntryType type; // entry_type.type    the type of this entry
  /**
   * 
   */
  String link = ""; // url.type   the resource location of the information about this entry
  /**
   * 
   */
  String reaction = ""; // keggid.type    the KEGGID of corresponding reaction
  /**
   * 
   */
  Object custom=null; // For Customize purposes (e.g. saving a corresponding node reference)
  /**
   * 
   */
  Graphics graph=null;
  /**
   * 
   */
  ArrayList<Integer> components = new ArrayList<Integer>();
  
  /**
   * 
   * @param id
   * @param name
   * @param type
   */
  public Entry(int id, String name, EntryType type) {
    super();
    this.id = id;
    this.name = name;
    this.type = type;
    //if (type==EntryType.gene) graph = new Graphics(true); else graph = new Graphics(false);
  }
  
  /**
   * 
   * @param id
   * @param name
   * @param type
   * @param link
   * @param reaction
   */
  public Entry(int id, String name, EntryType type, String link, String reaction) {
    this(id, name, type);
    this.link = link;
    this.reaction = reaction;
  }
  
  /**
   * 
   * @param id
   * @param name
   * @param type
   * @param link
   * @param reaction
   * @param childNodes
   */
  public Entry(int id, String name, EntryType type, String link, String reaction, NodeList childNodes) {
    this(id,name,type,link,reaction);
    parseSubNodes(childNodes);
  }
  
  /**
   * 
   * @return
   */
  public ArrayList<Integer> getComponents() {
    return components;
  }
  
  /**
   * 
   * @return
   */
  public boolean hasComponents() {
    return (components!=null&&components.size()>0);
  }
  
  /**
   * 
   * @return
   */
  public Object getCustom() {
    return custom;
  }
  
  /**
   * 
   * @return
   */
  public Graphics getGraphics() {
    return graph;
  }

  /**
   * 
   * @return
   */
  public int getId() {
    return id;
  }

  /**
   * 
   * @return
   */
  public String getLink() {
    return link;
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
  public String getReaction() {
    return reaction;
  }

  /**
   * 
   * @return
   */
  public EntryType getType() {
    return type;
  }

  /**
   * 
   * @return
   */
  public boolean hasGraphics() {
    return (graph!=null);
  }

  /**
   * 
   * @param nl
   */
  private void parseSubNodes(NodeList nl) {
    if (nl==null) return;
    
    for (int i=0; i<nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node==null) return;
      String name = node.getNodeName().trim();
      
      NamedNodeMap att = node.getAttributes();
      if (name.equalsIgnoreCase("component")) { // 0 .. *
        components.add(KeggParser.getNodeValueInt(att, "id"));
      } else if(name.equals("graphics")) { // 0 .. 1
        graph = new Graphics(KeggParser.getNodeValue(att, "name"), KeggParser.getNodeValueInt(att, "x"), KeggParser.getNodeValueInt(att, "y"), GraphicsType.valueOf(KeggParser.getNodeValue(att, "type")), KeggParser.getNodeValueInt(att, "width"), KeggParser.getNodeValueInt(att, "height"), KeggParser.getNodeValue(att, "fgcolor"), KeggParser.getNodeValue(att, "bgcolor"), (type==EntryType.gene));
      }
    }
  }

  /**
   * 
   * @param custom
   */
  public void setCustom(Object custom) {
    this.custom = custom;
  }

  /**
   * 
   * @param id
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * 
   * @param link
   */
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * 
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * 
   * @param reaction
   */
  public void setReaction(String reaction) {
    this.reaction = reaction;
  }

  /**
   * 
   * @param type
   */
  public void setType(EntryType type) {
    this.type = type;
  }

}
