/**
 *
 * @author wrzodek
 */
package de.zbit.kegg.gui;

import y.base.Node;
import y.view.EditMode;
import y.view.Graph2D;

/**
 * An edit mode for y Files, that allow no creating of new Nodes or edges.
 * @author wrzodek
 */
public class RestrictedEditMode extends EditMode {
 
  public RestrictedEditMode() {
    super();
    //setEditNodeMode(null);
    setCreateEdgeMode(null);
  }
  
  @Override
  protected  Node   createNode(Graph2D graph, double x, double y) {
    // do nothing
    return null;
  }
  @Override
   protected  Node   createNode(Graph2D graph, double x, double y, Node parent) {
    // do nothing
    return null;
  }
}
