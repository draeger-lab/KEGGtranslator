package de.zbit.kegg.parser.pathway;

import java.util.ArrayList;


import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.zbit.kegg.parser.KeggParser;

/**
 * Corresponding to the Kegg Reaction class (see {@link http://www.genome.jp/kegg/xml/docs/})
 * @author wrzodek
 */
public class Reaction {
  /**
   * 
   */
  String Name;
  /**
   * 
   */
  ReactionType type;
  /**
   * 
   */
  ArrayList<ReactionComponent> substrate = new ArrayList<ReactionComponent>(); // 1..*
  /**
   * 
   */
  ArrayList<ReactionComponent> product = new ArrayList<ReactionComponent>(); // 1..*
  
  /**
   * 
   * @param name
   * @param type
   */
  private Reaction(String name, ReactionType type) {
    super();
    this.Name = name;
    this.type = type;
  }
  
  /**
   * 
   * @param name
   * @param type
   * @param childNodes
   */
  public Reaction(String name, ReactionType type, NodeList childNodes) {
    this(name, type);
    parseSubNodes(childNodes);
  }
  
  /**
   * 
   * @param name
   * @param type
   * @param substrate
   * @param product
   */
  public Reaction(String name, ReactionType type, ReactionComponent substrate, ReactionComponent product) {
    this (name, type);
    addProduct(product);
    addSubstrate(substrate);
  }
  
  /**
   * 
   * @param product
   */
  public void addProduct(ReactionComponent product) {
    this.product.add(product);
  }

  /**
   * 
   * @param substrate
   */
  public void addSubstrate(ReactionComponent substrate) {
    this.substrate.add(substrate);
  }
  
  /**
   * 
   * @return
   */
  public String getName() {
    return Name;
  }
  
  /**
   * 
   * @return
   */
  public ArrayList<ReactionComponent> getProducts() {
    return product;
  }
  
  /**
   * 
   * @return
   */
  public ArrayList<ReactionComponent> getSubstrates() {
    return substrate;
  }
  
  /**
   * 
   * @return
   */
  public ReactionType getType() {
    return type;
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
      if (name.equalsIgnoreCase("substrate")) {
        substrate.add(new ReactionComponent(KeggParser.getNodeValue(att, "name"), node.getChildNodes()));
      } else if(name.equals("product")) {
        product.add(new ReactionComponent(KeggParser.getNodeValue(att, "name"), node.getChildNodes()));
      }
    }
  }

  /**
   * 
   * @param name
   */
  public void setName(String name) {
    Name = name;
  }

  /**
   * 
   * @param type
   */
  public void setType(ReactionType type) {
    this.type = type;
  }

}
