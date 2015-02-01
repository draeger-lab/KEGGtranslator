/*
 * $Id$ $URL:
 * CollapseGroupNodesViewMode.java $
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.graph;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractAction;

import y.base.Node;
import y.base.NodeCursor;
import y.base.NodeList;
import y.geom.YPoint;
import y.layout.LayoutTool;
import y.view.Graph2D;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import y.view.NodeStateChangeEdgeRouter;
import y.view.NodeStateChangeHandler;
import y.view.ProxyShapeNodeRealizer;
import y.view.ViewMode;
import y.view.hierarchy.GroupNodeRealizer;
import y.view.hierarchy.HierarchyManager;

/**
 * <b>TODO: WORK IN PROGRESS, does not work yet.</b><p>
 * View mode that allows to collapse or expand folder / group nodes.<br/>
 * 
 * <i>See yFiles_Demo_Sources\src\demo\view\hierarchy\HierarchyDemo.java
 * for a template implementation.</i>
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class CollapseGroupNodesViewMode extends ViewMode {
  
  Graph2D graph;
  
  HierarchyManager hierarchy;
  
  public CollapseGroupNodesViewMode(Graph2D graph) {
    super();
    this.graph = graph;
    hierarchy = graph.getHierarchyManager();
  }
  
  /* (non-Javadoc)
   * @see y.view.ViewMode#mouseClicked(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseClicked(MouseEvent e) {
    
    if (e.getClickCount() == 2) {
      //      Node v = getHitInfo(e).getHitNode();
      //      if (v != null) {
      //        navigateToInnerGraph(v);
      //      } else {
      //        navigateToParentGraph();
      //      }
    } else {
      Node v = getHitInfo(e).getHitNode();
      if (v != null && !hierarchy.isNormalNode(v)) {
        double x = translateX(e.getX());
        double y = translateY(e.getY());
        Graph2D graph = view.getGraph2D();
        NodeRealizer r = graph.getRealizer(v);
        GroupNodeRealizer gnr = null;
        if (r instanceof GroupNodeRealizer) {
          gnr = (GroupNodeRealizer) r;
        } else if (r instanceof ProxyShapeNodeRealizer
            && ((ProxyShapeNodeRealizer) r).getRealizerDelegate() instanceof GroupNodeRealizer) {
          gnr = (GroupNodeRealizer) ((ProxyShapeNodeRealizer) r)
              .getRealizerDelegate();
        }
        if (gnr != null) {
          NodeLabel handle = gnr.getStateLabel();
          if (handle.getBox().contains(x, y)) {
            if (hierarchy.isFolderNode(v)) {
              openFolder(v);
            } else {
              closeGroup(v);
            }
          }
        }
      }
    }
  }
  
  //////////////////////////////////////////////////////////////////////////////
  // OPERATIONS ////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  /**
   * navigates to the graph inside of the given folder node
   */
  public void navigateToInnerGraph(Node folderNode) {
    if (hierarchy.isFolderNode(folderNode)) {
      Graph2D innerGraph = (Graph2D) hierarchy.getInnerGraph(folderNode);
      Rectangle box = innerGraph.getBoundingBox();
      view.setGraph2D(innerGraph);
      view.setCenter(box.x + box.width / 2, box.y + box.height / 2);
      innerGraph.updateViews();
    }
  }
  
  /**
   * navigates to the parent graph of the graph currently displayed in the graph
   * view.
   */
  public void navigateToParentGraph() {
    Graph2D graph = view.getGraph2D();
    if (!hierarchy.isRootGraph(graph)) {
      Graph2D parentGraph = (Graph2D) hierarchy.getParentGraph(graph);
      view.setGraph2D(parentGraph);
      Node anchor = hierarchy.getAnchorNode(graph);
      view.setZoom(1.0);
      view.setCenter(parentGraph.getCenterX(anchor),
        parentGraph.getCenterY(anchor));
      view.getGraph2D().updateViews();
    }
  }
  
  /**
   * Open a folder node.
   * @param folderNode
   */
  protected void openFolder(Node folderNode) {
    Graph2D graph = view.getGraph2D();
    
    NodeList folderNodes = new NodeList();
    if (folderNode == null) {
      //use selected top level groups
      for (NodeCursor nc = graph.selectedNodes(); nc.ok(); nc.next()) {
        Node v = nc.node();
        if (hierarchy.isFolderNode(v)) {
          folderNodes.add(v);
        }
      }
    } else {
      folderNodes.add(folderNode);
    }
    
    graph.firePreEvent();
    
    NodeStateChangeHandler stateChangeHandler = new NodeStateChangeEdgeRouter();
    
    for (NodeCursor nc = folderNodes.nodes(); nc.ok(); nc.next()) {
      //get original location of folder node
      Graph2D innerGraph = (Graph2D) hierarchy.getInnerGraph(nc.node());
      YPoint folderP = graph.getLocation(nc.node());
      NodeList innerNodes = new NodeList(innerGraph.nodes());
      stateChangeHandler.preNodeStateChange(nc.node());
      hierarchy.openFolder(nc.node());
      
      //get new location of group node
      Rectangle2D.Double gBox = graph.getRealizer(nc.node()).getBoundingBox();
      //move grouped nodes to former location of folder node
      LayoutTool.moveSubgraph(graph, innerNodes.nodes(), folderP.x - gBox.x,
        folderP.y - gBox.y);
      stateChangeHandler.postNodeStateChange(nc.node());
    }
    graph.firePostEvent();
    
    graph.unselectAll();
    for (NodeCursor nc = folderNodes.nodes(); nc.ok(); nc.next()) {
      graph.setSelected(nc.node(), true);
    }
    
    graph.updateViews();
  }
  
  /**
   * Close a group node.
   * @param groupNode
   */
  protected void closeGroup(Node groupNode) {
    Graph2D graph = view.getGraph2D();
    
    NodeList groupNodes = new NodeList();
    if (groupNode == null) {
      //use selected top level groups
      for (NodeCursor nc = graph.selectedNodes(); nc.ok(); nc.next()) {
        Node v = nc.node();
        if (hierarchy.isGroupNode(v) && hierarchy.getLocalGroupDepth(v) == 0) {
          groupNodes.add(v);
        }
      }
    } else {
      groupNodes.add(groupNode);
    }
    
    graph.firePreEvent();
    NodeStateChangeHandler stateChangeHandler = new NodeStateChangeEdgeRouter();
    for (NodeCursor nc = groupNodes.nodes(); nc.ok(); nc.next()) {
      stateChangeHandler.preNodeStateChange(nc.node());
      hierarchy.closeGroup(nc.node());
      stateChangeHandler.postNodeStateChange(nc.node());
    }
    graph.firePostEvent();
    
    graph.unselectAll();
    for (NodeCursor nc = groupNodes.nodes(); nc.ok(); nc.next()) {
      graph.setSelected(nc.node(), true);
    }
    
    graph.updateViews();
  }
  
  /**
   * Action that closes a group node.
   * 
   * @author Clemens Wrzodek
   * @version $Rev$
   */
  public class CloseGroupAction extends AbstractAction {
    private static final long serialVersionUID = 4980453364823774088L;
    Node groupNode;
    
    CloseGroupAction(Node groupNode) {
      super("Close Group");
      this.groupNode = groupNode;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      closeGroup(groupNode);
    }
  }
}
