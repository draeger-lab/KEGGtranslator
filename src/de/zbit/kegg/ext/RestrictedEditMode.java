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
 * 
 * There are special restrictions for this file. Each procedure that
 * is using the yFiles API must stick to their license restrictions.
 * Please see the following link for more information
 * <http://www.yworks.com/en/products_yfiles_sla.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.ext;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableModel;

import org.sbml.jsbml.util.StringTools;

import y.base.DataMap;
import y.base.Edge;
import y.base.EdgeMap;
import y.base.Node;
import y.base.NodeMap;
import y.view.DefaultBackgroundRenderer;
import y.view.EditMode;
import y.view.Graph2D;
import y.view.Graph2DSelectionEvent;
import y.view.Graph2DSelectionListener;
import y.view.Graph2DView;
import y.view.HitInfo;
import y.view.NavigationComponent;
import y.view.Overview;
import de.zbit.gui.GUITools;
import de.zbit.gui.SystemBrowser;
import de.zbit.kegg.Translator;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.util.EscapeChars;
import de.zbit.util.StringUtil;

/**
 * An edit mode for y Files, that allow no creating of new Nodes or edges.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class RestrictedEditMode extends EditMode implements Graph2DSelectionListener {
 
  /**
   * A properties table that might be initialized.
   */
  JTable propTable=null;
  
  /**
   * A panel for the {@link #propTable}.
   */
  JPanel eastPanel=null;
  
  /**
   * This is used to fire openPathway events on double clicks on
   * pathway reference nodes.
   */
  ActionListener aListener = null;
  
  /**
   * This is the ActionCommand for actionlisteners to open a new pathway tab.
   * TODO
   */
  public final static String OPEN_PATHWAY = "OPEN_PATHWAY";
  
  public RestrictedEditMode() {
    super();
    //setEditNodeMode(null);
    setCreateEdgeMode(null);
    /*
     * The Tooltips of nodes, provding descriptions must be shown
     * longer than the system default. Let's show them 15 seconds!
     */
    ToolTipManager.sharedInstance().setDismissDelay(15000);
  }
  
  /**
   * @param listener this is used to fire TODO
   */
  public RestrictedEditMode(ActionListener listener) {
    this();
    this.aListener = listener;
  }
  
  
  
  @Override
  protected Node createNode(Graph2D graph, double x, double y) {
    // do nothing
    return null;
  }
  @Override
   protected Node createNode(Graph2D graph, double x, double y, Node parent) {
    // do nothing
    return null;
  }
  
  /*
   * Add some helper methods
   */
  
  /**
   * Adds a background image to the given pane.
   */
  public static void addBackgroundImage(URL imagePath, Graph2DView pane) {
    DefaultBackgroundRenderer renderer = new DefaultBackgroundRenderer(pane);
    renderer.setImageResource(imagePath);
    renderer.setMode(DefaultBackgroundRenderer.CENTERED);
    renderer.setColor(Color.white);
    pane.setBackgroundRenderer(renderer);
  }
  
  /* (non-Javadoc)
   * @see y.view.EditMode#getNodeTip(y.base.Node)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected String getNodeTip(Node n) {
    // Show a nice ToolTipText for every node.
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) n.getGraph().getDataProvider(KEGG2yGraph.mapDescription);
    if (mapDescriptionMap==null) return super.getNodeTip(n);
    
    // Get nodeLabel, description and eventually an image for the ToolTipText
    String nodeLabel = null;
    String description = null;
    String image = "";
    NodeMap[] nm = n.getGraph().getRegisteredNodeMaps();
    if (nm!=null) {
      for (int i=0; i<nm.length;i++) {
        Object c = nm[i].get(n);
        if (c==null || c.toString().length()<1) continue;
        String mapDescription = mapDescriptionMap.getV(nm[i]);
        if (mapDescription.equals("nodeLabel")) {
          nodeLabel = "<b>"+c.toString().replace(",", ",<br/>")+"</b><br/>";
        } else if (mapDescription.equals("description")) {
          description = "<i>"+c.toString().replace(",", ",<br/>")+"</i><br/>";
        } else if (mapDescription.equals("keggIds")) {
          for (String s: c.toString().split(",")) {
            s=s.toUpperCase().trim();
            if (s.startsWith("PATH:")) {
              image+=KEGG2jSBML.getPathwayPreviewPicture(s);
            } else if (s.startsWith("CPD:")) {
              // KEGG provides picture for compounds (e.g., "C00118").
              image+=KEGG2jSBML.getCompoundPreviewPicture(s);
            }
          }
        }
      }
    }
    
    // Merge the three strings to a single tooltip
    StringBuffer tooltip = new StringBuffer();
    if (nodeLabel!=null) {
      tooltip.append(StringUtil.insertLineBreaks(nodeLabel, GUITools.TOOLTIP_LINE_LENGTH, "<br/>"));
    }
    if (description!=null) {
      tooltip.append(StringUtil.insertLineBreaks(description, GUITools.TOOLTIP_LINE_LENGTH, "<br/>"));
    }
    if (image!=null && image.length()>0) {
      tooltip.append("<div align=\"center\">"+image+"</div>");
    }
    
    // Append html and return toString.
    return "<html><body>"+tooltip.toString()+"</body></html>";
  }
  
  /* (non-Javadoc)
   * @see y.view.EditMode#mouseClicked(double, double)
   */
  @Override
  public void mouseClicked(double x, double y) {
    MouseEvent ev = lastClickEvent;
    if (ev.getClickCount() == 2 && aListener!=null) {
      // Get double clicked node
      HitInfo allHitObjects = getGraph2D().getHitInfo(x, y, false);
      Node n = allHitObjects.getHitNode();
      
      // Ask user if he wants to open the pathway and fire an event.
      String kgId = TranslatorTools.getKeggIDs(n);
      if (kgId.toLowerCase().startsWith("path:")) {
        int ret = GUITools.showQuestionMessage(null, "Do you want to download and open the referenced pathway in a new tab?", 
          Translator.APPLICATION_NAME, new Object[]{"Yes", "No"});
        if (ret==0) {
          ActionEvent e = new ActionEvent(kgId.trim().substring(5).toLowerCase(), JOptionPane.OK_OPTION, OPEN_PATHWAY);
          aListener.actionPerformed(e);
        }
      }
      

    } else {
      // let EditMode handle the click event
      super.mouseClicked(x,y);
    }
  }
  
  /**
   * Adds an overview an clickable navigation elements to the pane.
   */
  public static void addOverviewAndNavigation(Graph2DView view) {
    //get the glass pane
    JPanel glassPane = view.getGlassPane();
    //set an according layout manager
    glassPane.setLayout(new BorderLayout());

    JPanel toolsPanel = new JPanel(new GridBagLayout());
    toolsPanel.setOpaque(false);
    toolsPanel.setBackground(null);
    toolsPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 0));

    //create and add the overview to the tools panel
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.insets = new Insets(0, 0, 16, 0);
    Overview overview = createOverview(view);
    toolsPanel.add(overview, gbc);

    //create and add the navigation component to the tools panel
    NavigationComponent navigationComponent = createNavigationComponent(view, 20, 30);
    toolsPanel.add(navigationComponent, gbc);

    //add the toolspanel to the glass pane
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    JViewport viewport = new JViewport();
    viewport.add(toolsPanel);
    viewport.setOpaque(false);
    viewport.setBackground(null);
    JPanel westPanel = new JPanel(new BorderLayout());
    westPanel.setOpaque(false);
    westPanel.setBackground(null);
    westPanel.add(viewport, BorderLayout.NORTH);
    glassPane.add(westPanel, BorderLayout.WEST);
  }
  
  public void addPropertiesTable(Graph2DView view) {
    //get the glass pane
    JPanel glassPane = view.getGlassPane();
    //set an according layout manager
    if (!(glassPane.getLayout() instanceof BorderLayout)) {
      glassPane.setLayout(new BorderLayout());
    }
    Box rightPanel = new Box(BoxLayout.Y_AXIS);
    
    if (propTable==null) {
      propTable = createPropertiesTable();
    }
    JScrollPane scrollPane = new JScrollPane(propTable);
    scrollPane.setPreferredSize(new Dimension(250, 180));
    rightPanel.add(GUITools.createTitledPanel(scrollPane,"Properties"));
    scrollPane.setColumnHeaderView(null);
    
    // Add node-click listener
    getGraph2D().addGraph2DSelectionListener(this);
    
    // Add to glass pane
    eastPanel = new JPanel(new BorderLayout());
    eastPanel.setOpaque(false);
    eastPanel.setBackground(null);
    eastPanel.add(rightPanel, BorderLayout.NORTH);
    eastPanel.setVisible(false);
    
    // Make the background solid, but same color as the graph
    GUITools.setOpaqueForAllElements(eastPanel, false);
    scrollPane.setOpaque(true);
    scrollPane.setBackground(Color.white); 
    
    glassPane.add(eastPanel, BorderLayout.EAST);
  }
  
  /**
   * Creates a JTable based properties view that displays the details of a selected model element.
   */
  private JTable createPropertiesTable() {
    if (propTable==null) {
      propTable = new JTable() {
        private static final long serialVersionUID = -542498752356126673L;
        // Make cells not editable.
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };
      // Make cell values to appear as ToolTip
      propTable.addMouseMotionListener(new MouseMotionAdapter(){
        public void mouseMoved(MouseEvent e){
          Point p = e.getPoint(); 
          int row = propTable.rowAtPoint(p);
          int column = propTable.columnAtPoint(p);
          propTable.setToolTipText(
            StringUtil.toHTML( EscapeChars.forHTML(String.valueOf(propTable.getValueAt(row,column))) , 120)
          );
        }
      });
      propTable.addMouseListener(new MouseAdapter() {
        // Open browser on link double-click
        public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          if (e.getClickCount() == 2) {
            Point p = e.getPoint(); 
            int row = propTable.rowAtPoint(p);
            int column = propTable.columnAtPoint(p);
            String text = String.valueOf(propTable.getValueAt(row,column));
            if (text.toLowerCase().startsWith("http")) {
              SystemBrowser.openURL(text);
            }
          }
        }
      });
      propTable.setOpaque(false);
      propTable.setTableHeader(null);
    }
    
    return propTable;
  }
  
  
  private static Overview createOverview(Graph2DView view) {
    Overview ov = new Overview(view);
    /* customize the overview */
    //animates the scrolling
    ov.putClientProperty("Overview.AnimateScrollTo", Boolean.TRUE);
    //blurs the part of the graph which can currently not be seen
    ov.putClientProperty("Overview.PaintStyle", "Funky");
    //allows zooming from within the overview
    ov.putClientProperty("Overview.AllowZooming", Boolean.TRUE);
    //provides functionality for navigation via keybord (zoom in (+), zoom out (-), navigation with arrow keys)
    ov.putClientProperty("Overview.AllowKeyboardNavigation", Boolean.TRUE);
    //determines how to differ between the part of the graph that can currently be seen, and the rest
    ov.putClientProperty("Overview.Inverse", Boolean.TRUE);
    ov.setPreferredSize(new Dimension(150, 150));
    ov.setMinimumSize(new Dimension(150, 150));

    ov.setBorder(BorderFactory.createEtchedBorder());
    return ov;
  }
  private static NavigationComponent createNavigationComponent(Graph2DView view, double scrollStepSize, int scrollTimerDelay) {
    //create the NavigationComponent itself
    final NavigationComponent navigation = new NavigationComponent(view);
    navigation.setScrollStepSize(scrollStepSize);
    //set the duration between scroll ticks
    navigation.putClientProperty("NavigationComponent.ScrollTimerDelay", new Integer(scrollTimerDelay));
    //set the initial duration until the first scroll tick is triggered
    navigation.putClientProperty("NavigationComponent.ScrollTimerInitialDelay", new Integer(scrollTimerDelay));
    //set a flag so that the fit content button will adjust the viewports in an animated fashion
    navigation.putClientProperty("NavigationComponent.AnimateFitContent", Boolean.TRUE);

    //add a mouse listener that will make a semi transparent background, as soon as the mouse enters this component
    navigation.setBackground(new Color(255, 255, 255, 0));
    MouseAdapter navigationToolListener = new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        Color background = navigation.getBackground();
        //add some semi transparent background
        navigation.setBackground(new Color(background.getRed(), background.getGreen(), background.getBlue(), 196));
      }
      public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        Color background = navigation.getBackground();
        //make the background completely transparent
        navigation.setBackground(new Color(background.getRed(), background.getGreen(), background.getBlue(), 0));
      }
    };
    navigation.addMouseListener(navigationToolListener);

    //add mouse listener to all sub components of the navigationComponent
    for (int i = 0; i < navigation.getComponents().length; i++) {
      Component component = navigation.getComponents()[i];
      component.addMouseListener(navigationToolListener);
    }

    return navigation;
  }
  
  /* (non-Javadoc)
   * @see y.view.Graph2DSelectionListener#onGraph2DSelectionEvent(y.view.Graph2DSelectionEvent)
   */
  public void onGraph2DSelectionEvent(Graph2DSelectionEvent e) {
    //Update the properties table
    if (propTable==null) return;
    if (e.getSubject() instanceof Node) {
      Node node = (Node) e.getSubject();
      if(getGraph2D().isSelected(node)) {
        updatePropertiesTable(node);
      }
      eastPanel.setVisible((propTable.getModel().getRowCount()>0));
    } else if (e.getSubject() instanceof Edge) {
      Edge edge = (Edge) e.getSubject();
      if(getGraph2D().isSelected(edge)) {
        updatePropertiesTable(edge);
      }
      
      eastPanel.setVisible((propTable.getModel().getRowCount()>0));
    } else {
      eastPanel.setVisible(false);
    }
  }

  /**
   * @param node
   */
  @SuppressWarnings("unchecked")
  private void updatePropertiesTable(Object nodeOrEdge) {
    // Get map headings and graph
    Graph2D graph = getGraph2D();
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    if (mapDescriptionMap==null) return;
    
    
    // Generate table headings and content
    ArrayList<String> headers = new ArrayList<String>();
    ArrayList<Object> content = new ArrayList<Object>();
    if (nodeOrEdge instanceof Node) {
      Node node = (Node) nodeOrEdge;
      
      NodeMap[] nm = graph.getRegisteredNodeMaps();
      if (nm!=null) {
        for (int i=0; i<nm.length;i++) {
          //tm.setValueAt(nm[i].get(node), (index++), 1);
          Object c = nm[i].get(node);
          if (c!=null && c.toString().length()>0) {
            headers.add(getNiceCaption(mapDescriptionMap.getV(nm[i])));
            content.add(c.toString().replace(";", "; "));
          }
        }
      }
      
    } if (nodeOrEdge instanceof Edge) {
      Edge edge = (Edge) nodeOrEdge;
      
      EdgeMap[] eg = graph.getRegisteredEdgeMaps();
      if (eg!=null)
        for (int i=0; i<eg.length;i++) {
          //tm.setValueAt(eg[i].get(edge), (index++), 1);
          Object c = eg[i].get(edge);
          if (c!=null && c.toString().length()>0) {
            headers.add(getNiceCaption(mapDescriptionMap.getV(eg[i])));
            content.add(c.toString().replace(";", "; "));
          }
        }
      
    }
    
    // Apply the new table content
    DefaultTableModel tm = new DefaultTableModel();
    tm.addColumn("", headers.toArray());
    tm.addColumn("", content.toArray());
    propTable.setModel(tm);

  }

  /**
   * @param v
   * @return
   */
  private static String getNiceCaption(String v) {
    // description, type, description are being first-uppercased.
    if (v.equals("nodeLabel")) {
      return "All names"; // Synonyms
    } else if (v.equals("entrezIds")) {
      return "Entrez id(s)";
    } else if (v.equals("keggIds")) {
      return "Kegg id(s)";
    } else if (v.equals("uniprotIds")) {
      return "Uniprot id(s)";
    } else if (v.equals("ensemblIds")) {
      return "Ensembl id(s)";
    } else if (v.equals("url")) {
      return "URL";
    } else if (v.equals("nodeColor")) {
      return "Node color";
    } else if (v.equals("nodeName")) {
      return "Node name";
    } else if (v.equals("nodePosition")) {
      return "Node position";
    } else if (v.equals("interactionType")) {
      return "Interaction type";
      
    } else
      return StringTools.firstLetterUpperCase(v);
  }
}
