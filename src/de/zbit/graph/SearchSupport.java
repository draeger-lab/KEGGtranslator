/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import y.base.GraphEvent;
import y.base.GraphListener;
import y.base.Node;
import y.base.NodeCursor;
import y.base.NodeList;
import y.view.Drawable;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.NodeRealizer;

/**
 * <b>TODO: WORK IN PROGRESS, does not work yet.</b><p>
 * 
 * <b>This is a stump and currently unused. A demo class taken
 * from yFiles examples to search nodes. Node highlighting is performed
 * by the {@link Marker}.</b><p>
 * Intended is, to provide a search method that helps locating certain
 * genes in a pathway.
 * 
 * <p>
 * Utility class that provides methods for searching for nodes that match
 * a given search criterion and for displaying search results.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @author yFiles GmbH
 * @version $Rev$
 */
public class SearchSupport {
  private static final Object NEXT_ACTION_ID = "SearchSupport.Next";
  private static final Object CLEAR_ACTION_ID = "SearchSupport.Clear";


  // TODO: implement this static method.
//  public static addSearchSupport (TranslatorPanel tp ) {
//    // register keyboard action for "select next match" and "clear search"
//    final LabelTextSearchSupport support = getSearchSupport();
//    final ActionMap amap = support.createActionMap();
//    final InputMap imap = support.createDefaultInputMap();
//    tp.setActionMap(amap);
//    tp.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, imap);
//  }
  
  private Action previous;
  private Action next;
  private Action selectAll;
  private Action clear;

  private SearchResult searchResult;

  private final Graph2DView view;

  public SearchSupport( final Graph2DView view ) {
    this.view = view;
    this.view.addBackgroundDrawable(new Marker());
    final Graph2D graph = this.view.getGraph2D();

    // register a listener that updates search results whenever a node
    // is deleted to prevent stale data in the results
    graph.addGraphListener(new GraphListener() {
      public void onGraphEvent( final GraphEvent e ) {
        if (searchResult != null) {
          if (GraphEvent.POST_NODE_REMOVAL == e.getType() ||
              GraphEvent.SUBGRAPH_REMOVAL == e.getType()) {
            final SearchResult oldResult = searchResult;
            searchResult = new SearchResult();
            for (NodeCursor nc = oldResult.nodes(); nc.ok(); nc.next()) {
              final Node node = nc.node();
              if (node.getGraph() == graph) {
                searchResult.add(node);
              }
            }
          }
        }
      }
    });
  }

  /**
   * Returns the <code>Graph2DView</code> that is associated to the search
   * suuport.
   * @return the <code>Graph2DView</code> that is associated to the search
   * suuport.
   */
  public Graph2DView getView() {
    return view;
  }

  /**
   * Returns the current search result or <code>null</code> if there is none.
   * @return the current search result or <code>null</code> if there is none.
   */
  public SearchResult getSearchResult() {
    return searchResult;
  }

  /**
   * Updates the current search result and the enabled states of the support's
   * clear, next, previous, and select all actions.
   * @param query   specifies which nodes to include in the search result.
   * If the specified query is <code>null</code> the curren search result
   * is reset to <code>null</code>, too.
   * @param incremental   <code>true</code> if the current search result
   * should be refined using the specified criterion; <code>false</code>
   * if all nodes of the support's associated graph view's graph should be
   * checked.
   * @see #getClearAction()
   * @see #getNextAction()
   * @see #getPreviousAction()
   * @see #getSelectAllAction()
   */
  public void search( final SearchCriterion query, final boolean incremental ) {
    boolean resultChanged = false;
    if (query != null) {
      final Graph2D graph = view.getGraph2D();
      final NodeCursor nc =
              searchResult != null && incremental
              ? searchResult.nodes()
              : graph.nodes();
      final HashSet oldResult =
              searchResult == null
              ? new HashSet()
              : new HashSet(searchResult.asCollection());
      final HashMap node2location = new HashMap();
      searchResult = new SearchResult();
      for (; nc.ok(); nc.next()) {
        final Node node = nc.node();
        if (query.accept(graph, node)) {
          searchResult.add(node);
          final NodeRealizer nr = graph.getRealizer(node);
          node2location.put(node, new Point2D.Double(nr.getX(), nr.getY()));
          if (!oldResult.contains(node)) {
            resultChanged = true;
          }
        }
      }
      searchResult.sort(new Comparator() {
        public int compare( final Object o1, final Object o2 ) {
          final Point2D p1 = (Point2D) node2location.get(o1);
          final Point2D p2 = (Point2D) node2location.get(o2);
          if (p1.getY() < p2.getY()) {
            return -1;
          } else if (p1.getY() > p2.getY()) {
            return 1;
          } else {
            if (p1.getX() < p2.getX()) {
              return -1;
            } else if (p1.getX() > p2.getX()) {
              return 1;
            } else {
              return 0;
            }
          }
        }
      });
      resultChanged |= oldResult.size() != searchResult.asCollection().size();
    } else if (searchResult != null) {
      searchResult = null;
      resultChanged = true;
    }

    if (resultChanged) {
      final boolean state =
              searchResult != null &&
              !searchResult.asCollection().isEmpty();
      if (clear != null) {
        clear.setEnabled(state);
      }
      if (previous != null) {
        previous.setEnabled(state);
      }
      if (next != null) {
        next.setEnabled(state);
      }
      if (selectAll != null) {
        selectAll.setEnabled(state);
      }
    }
  }

  /**
   * Ensures that the specified rectangle is visible in the support's
   * associated graph view.
   * @param bnds   a rectangle in world (i.e. graph) coordinates.
   */
  private void focusView( final Rectangle2D bnds ) {
    if (bnds.getWidth() > 0 && bnds.getHeight() > 0) {
      final double minX = bnds.getX() - MARKER_MARGIN;
      final double w = bnds.getWidth() + 2*MARKER_MARGIN;
      final double maxX = minX + w;
      final double minY = bnds.getY() - MARKER_MARGIN;
      final double h = bnds.getHeight() + 2*MARKER_MARGIN;
      final double maxY = minY + h;

      final int canvasWidth = view.getCanvasComponent().getWidth();
      final int canvasHeight = view.getCanvasComponent().getHeight();
      final Point2D oldCenter = view.getCenter();
      final double oldZoom = view.getZoom();
      double newZoom = oldZoom;
      double newCenterX = oldCenter.getX();
      double newCenterY = oldCenter.getY();
      final Rectangle vr = view.getVisibleRect();

      // determine whether the specified rectangle (plus the marker margin)
      // lies in the currently visible region
      // if not, adjust zoom factor and view port accordingly
      boolean widthFits = true;
      boolean heightFits = true;
      if (vr.getWidth() < w) {
        newZoom = Math.min(newZoom, canvasWidth / w);
        widthFits = false;
      }
      if (vr.getHeight() < h) {
        newZoom = Math.min(newZoom, canvasHeight / h);
        heightFits = false;
      }
      if (widthFits) {
        if (vr.getX() > minX) {
          newCenterX -= vr.getX() - minX;
        } else if (vr.getMaxX() < maxX) {
          newCenterX += maxX - vr.getMaxX();
        }
      } else {
                                         // take scroll bars into account
        newCenterX = bnds.getCenterX() + (view.getWidth() - canvasWidth) * 0.5 / newZoom;
      }
      if (heightFits) {
        if (vr.getY() > minY) {
          newCenterY -= vr.getY() - minY;
        } else if (vr.getMaxY() < maxY) {
          newCenterY += maxY - vr.getMaxY();
        }
      } else {
                                         // take scroll bars into account
        newCenterY = bnds.getCenterY() + (view.getHeight() - canvasHeight) * 0.5 / newZoom;
      }

      if (oldZoom != newZoom ||
          oldCenter.getX() != newCenterX ||
          oldCenter.getY() != newCenterY) {
        // animate the view port change
        view.focusView(newZoom, new Point2D.Double(newCenterX, newCenterY), true);
      } else {
        view.updateView();
      }
    }
  }

  /**
   * Ensures that only the specified node is selected and that the specified
   * node is visible in the support's associated graph view.
   * @param node   the node to select and display.
   */
  private void emphasizeNode( final Node node ) {
    final Graph2D graph = view.getGraph2D();
    graph.unselectAll();
    if (node != null) {
      final NodeRealizer nr = graph.getRealizer(node);
      nr.setSelected(true);
      final Rectangle2D.Double bnds = new Rectangle2D.Double(0, 0, -1, -1);
      nr.calcUnionRect(bnds);
      focusView(bnds);
    } else {
      view.updateView();
    }
  }

  /**
   * Returns the support's associated <em>clear search result</em> action.
   * @return the support's associated <em>clearsearch result</em> action.
   * @see #createClearAction()
   */
  public Action getClearAction() {
    if (clear == null) {
      clear = createClearAction();
    }
    return clear;
  }

  /**
   * Creates the support's associated <em>clear search result</em> action.
   * The default implementation resets the support's search result to
   * <code>null</code>.
   * @return the support's associated <em>clear search result</em> action.
   */
  protected Action createClearAction() {
    return new AbstractAction("Clear") {
      {
        setEnabled(searchResult != null);
      }

      public void actionPerformed( final ActionEvent e ) {
        if (searchResult != null) {
          search(null, false);
          view.updateView();
        }
      }
    };
  }

  /**
   * Returns the support's associated <em>find previous match</em> action.
   * @return the support's associated <em>find previous match</em> action.
   * @see #createPreviousAction()
   */
  public Action getPreviousAction() {
    if (previous == null) {
      previous = createPreviousAction();
    }
    return previous;
  }

  /**
   * Creates the support's associated <em>find previous match</em> action.
   * @return the support's associated <em>find previous match</em> action.
   */
  protected Action createPreviousAction() {
    return new AbstractAction("Previous") {
        {
          setEnabled(searchResult != null);
        }

        public void actionPerformed( final ActionEvent e ) {
          if (searchResult != null) {
            searchResult.emphasizePrevious();
            emphasizeNode(searchResult.emphasizedNode());
          }
        }
      };
  }

  /**
   * Returns the support's associated <em>find next match</em> action.
   * @return the support's associated <em>find next match</em> action.
   * @see #createNextAction()
   */
  public Action getNextAction() {
    if (next == null) {
      next = createNextAction();
    }
    return next;
  }

  /**
   * Creates the support's associated <em>find next match</em> action.
   * @return the support's associated <em>find next match</em> action.
   */
  protected Action createNextAction() {
    return new AbstractAction("Next") {
        {
          setEnabled(searchResult != null);
        }

        public void actionPerformed( final ActionEvent e ) {
          if (searchResult != null) {
            searchResult.emphasizeNext();
            emphasizeNode(searchResult.emphasizedNode());
          }
        }
      };
  }

  /**
   * Returns the support's associated <em>select all matches</em> action.
   * @return the support's associated <em>select all matches</em> action.
   * @see #createSelectAllAction()
   */
  public Action getSelectAllAction() {
    if (selectAll == null) {
      selectAll = createSelectAllAction();
    }
    return selectAll;
  }

  /**
   * Creates the support's associated <em>select all matches</em> action.
   * @return the support's associated <em>select all matches</em> action.
   */
  protected Action createSelectAllAction() {
    return new AbstractAction("Select All") {
        {
          setEnabled(searchResult != null);
        }

        public void actionPerformed( final ActionEvent e ) {
          if (searchResult != null) {
            final Graph2D graph = view.getGraph2D();
            graph.unselectAll();
            // clear the result set's emphasis pointer
            searchResult.resetEmphasis();
            // select all matching nodes and en passent calculate the result
            // set's bounding box
            final Rectangle2D.Double bnds = new Rectangle2D.Double(0, 0, -1, -1);
            for (NodeCursor nc = searchResult.nodes(); nc.ok(); nc.next()) {
              final NodeRealizer nr = graph.getRealizer(nc.node());
              nr.setSelected(true);
              nr.calcUnionRect(bnds);
            }

            if (bnds.getWidth() > 0 && bnds.getHeight() > 0) {
              // ensure that all selected nodes are visible
              focusView(bnds);
            } else {
              view.updateView();
            }
          }
        }
      };
  }

  /**
   * Creates a preconfigured action map for the support's
   * <em>find next match</em> and <em>clear result</code> actions.
   * @return a preconfigured action map for the support's
   * <em>find next match</em> and <em>clear result</code> actions.
   * @see #getClearAction()
   * @see #getNextAction()
   */
  public ActionMap createActionMap() {
    final ActionMap amap = new ActionMap();
    amap.put(NEXT_ACTION_ID, getNextAction());
    amap.put(CLEAR_ACTION_ID, getClearAction());
    return amap;
  }

  /**
   * Creates a preconfigured input map for the support's
   * <em>find next match</em> and <em>clear result</code> actions.
   * The default implementation maps the <em>find next match</em> action
   * to the <code>F3</code> function key and the <em>clear search result</em>
   * action to the <code>ESCAPE</code> key.
   * @return a preconfigured input map for the support's
   * <em>find next match</em> and <em>clear result</code> actions.
   * @see #getClearAction()
   * @see #getNextAction()
   */
  public InputMap createDefaultInputMap() {
    final InputMap imap = new InputMap();
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), NEXT_ACTION_ID);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLEAR_ACTION_ID);
    return imap;
  }

  private static final int MARKER_MARGIN = 10;
  private static final Color EMPHASIZE_COLOR = new Color(153,204,0);
  private static final Color HIGHLIGHT_COLOR = new Color(202,227,255);

  /**
   * <code>Drawable</code> that highlights search results by drawing a thick,
   * colored border around search result nodes.
   */
  private final class Marker implements Drawable {
    private final RoundRectangle2D.Double marker;

    Marker() {
      marker = new RoundRectangle2D.Double();
    }

    public void paint( final Graphics2D g ) {
      if (searchResult != null && !searchResult.asCollection().isEmpty()) {
        final Color oldColor = g.getColor();

        final Graph2D graph = view.getGraph2D();
        for (NodeCursor nc = searchResult.nodes(); nc.ok(); nc.next()) {
          final Node node = nc.node();
          if (graph.isSelected(node)) {
            g.setColor(EMPHASIZE_COLOR);
          } else {
            g.setColor(HIGHLIGHT_COLOR);
          }

          final NodeRealizer nr = graph.getRealizer(node);
          marker.setRoundRect(
                  nr.getX() - MARKER_MARGIN,
                  nr.getY() - MARKER_MARGIN,
                  nr.getWidth() + 2* MARKER_MARGIN,
                  nr.getHeight() + 2* MARKER_MARGIN,
                  MARKER_MARGIN,
                  MARKER_MARGIN);
          g.fill(marker);
        }

        g.setColor(oldColor);
      }
    }

    public Rectangle getBounds() {
      if (searchResult == null || searchResult.asCollection().isEmpty()) {
        final Point2D center = view.getCenter();
        return new Rectangle(
                (int) Math.rint(center.getX()),
                (int) Math.rint(center.getY()),
                -1,
                -1);
      } else {
        final Rectangle bnds = new Rectangle(0, 0, -1, -1);
        final Graph2D graph = view.getGraph2D();
        for (NodeCursor nc = searchResult.nodes(); nc.ok(); nc.next()) {
          graph.getRealizer(nc.node()).calcUnionRect(bnds);
        }
        bnds.grow(MARKER_MARGIN, MARKER_MARGIN);
        return bnds;
      }
    }
  }

  /**
   * Stores nodes that make up a <em>search result</em> and manages an
   * emphasis pointer to allow for <em>find next</em> and
   * <em>find previous</em> actions.
   */
  public static final class SearchResult {
    private final NodeList nodes;
    private NodeCursor cursor;
    private Node current;

    SearchResult() {
      nodes = new NodeList();
    }

    /**
     * Add the specified node to the search result set.
     * @param node   the <code>Node</code> to add.
     */
    void add( final Node node ) {
      nodes.add(node);
    }

    /**
     * Returns a cursor over all nodes in the search result set.
     * @return a cursor over all nodes in the search result set.
     */
    public NodeCursor nodes() {
      return nodes.nodes();
    }

    /**
     * Returns the currently emphasized node or <code>null</code> if there is
     * none.
     * @return the currently emphasized node or <code>null</code> if there is
     * none.
     */
    public Node emphasizedNode() {
      return current;
    }

    /**
     * Resets the emphasis cursor, that is calling {@link #emphasizedNode()}
     * afterwards will return <code>null</code>.
     */
    public void resetEmphasis() {
      current = null;
      cursor = null;
    }

    /**
     * Sets the emphasis pointer to the next node in the search result set.
     * If the emphasized node is the last node in the set, this method will
     * set the pointer to the first node in the set.
     */
    public void emphasizeNext() {
      if (cursor == null) {
        if (nodes.isEmpty()) {
          return;
        } else {
          cursor = nodes.nodes();
          cursor.toLast();
        }
      }
      cursor.cyclicNext();
      current = cursor.node();
    }

    /**
     * Sets the emphasis pointer to the previous node in the search result set.
     * If the emphasized node is the first node in the set, this method will
     * set the pointer to the last node in the set.
     */
    public void emphasizePrevious() {
      if (cursor == null) {
        if (nodes.isEmpty()) {
          return;
        } else {
          cursor = nodes.nodes();
          cursor.toFirst();
        }
      }
      cursor.cyclicPrev();
      current = cursor.node();
    }

    /**
     * Sorts the nodes in the search result set according to the order
     * induced by the specified comparator.
     * @param c   the <code>Comparator</code> to sort the nodes in the search
     * result set.
     */
    void sort( final Comparator c ) {
      nodes.sort(c);
    }

    /**
     * Returns a <code>Collection</code> handle for the search result.
     * @return a <code>Collection</code> handle for the search result.
     */
    Collection asCollection() {
      return nodes;
    }
  }

  /**
   * Specifies the contract of search criteria for node searches.
   */
  public static interface SearchCriterion {
    /**
     * Returns <code>true</code> if the specified node should be included
     * in the search result and <code>false</code> otherwise.
     * @param graph   the <code>Graph2D</code> to which the specified node
     * belongs.
     * @param node   the <code>Node</code> to test for inclusion in the
     * search result.
     * @return <code>true</code> if the specified node should be included
     * in the search result and <code>false</code> otherwise.
     */
    public boolean accept( Graph2D graph, Node node );
  }
}