package de.zbit.kegg.parser.pathway;


/**
 * Corresponding to the Kegg Graphics class (see {@link http://www.genome.jp/kegg/xml/docs/})
 * @author wrzodek
 */
public class Graphics {
  /**
   * 
   */
  String name = "";
  /**
   * 
   */
  int x=0;
  /**
   * 
   */
  int y=0;
  /**
   * 
   */
  String coords="";
  /**
   * 
   */
  GraphicsType type = GraphicsType.rectangle;
  /**
   * 
   */
  int width=0;
  /**
   * 
   */
  int height=0;
  /**
   * 
   */
  String fgcolor="#000000";
  /**
   * for gene products
   */
  String bgcolor = "#FFFFFF"; // "#BFFFBF"; 
  
  /**
   * 
   */
  public Graphics(){
    super();
  }
  
  /**
   * 
   * @param isGeneProduct
   */
  public Graphics( boolean isGeneProduct) {
    this();
    if (isGeneProduct) bgcolor = "#BFFFBF"; // Default for gene product
  }
  
  /**
   * 
   * @param name
   * @param x
   * @param y
   * @param type
   * @param width
   * @param height
   * @param fgcolor
   * @param bgcolor
   * @param isGeneProduct
   */
  public Graphics(String name, int x, int y, GraphicsType type, int width, int height, String fgcolor, String bgcolor, boolean isGeneProduct) {
    this(isGeneProduct);
    this.name = name;
    this.x = x;
    this.y = y;
    this.type = type;
    this.width = width;
    this.height = height;
    this.fgcolor = fgcolor;
    this.bgcolor = bgcolor;
  }
  
  /**
   * 
   * @return
   */
  public String getBgcolor() {
    return bgcolor;
  }
  
  /**
   * 
   * @return
   */
  public String getCoords() {
    return coords;
  }
  
  /**
   * 
   * @return
   */
  public String getFgcolor() {
    return fgcolor;
  }
  
  /**
   * 
   * @return
   */
  public int getHeight() {
    return height;
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
  public GraphicsType getType() {
    return type;
  }
  
  /**
   * 
   * @return
   */
  public int getWidth() {
    return width;
  }
  
  /**
   * 
   * @return
   */
  public int getX() {
    return x;
  }
  
  /**
   * 
   * @return
   */
  public int getY() {
    return y;
  }
  
  /**
   * 
   * @param bgcolor
   */
  public void setBgcolor(String bgcolor) {
    this.bgcolor = bgcolor;
  }
  
  /**
   * 
   * @param coords
   */
  public void setCoords(String coords) {
    this.coords = coords;
  }
  
  /**
   * 
   * @param fgcolor
   */
  public void setFgcolor(String fgcolor) {
    this.fgcolor = fgcolor;
  }
  
  /**
   * 
   * @param height
   */
  public void setHeight(int height) {
    this.height = height;
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
   * @param type
   */
  public void setType(GraphicsType type) {
    this.type = type;
  }
  
  /**
   * 
   * @param width
   */
  public void setWidth(int width) {
    this.width = width;
  }
  
  /**
   * 
   * @param x
   */
  public void setX(int x) {
    this.x = x;
  }
  
  /**
   * 
   * @param y
   */
  public void setY(int y) {
    this.y = y;
  }

}
