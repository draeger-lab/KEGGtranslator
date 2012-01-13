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
package de.zbit.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;

import y.base.DataMap;
import y.base.Edge;
import y.base.EdgeMap;
import y.base.Node;
import y.base.NodeMap;
import y.option.Editor;
import y.option.OptionHandler;
import y.option.TableEditorFactory;
import y.view.DefaultBackgroundRenderer;
import y.view.EditMode;
import y.view.Graph2D;
import y.view.Graph2DSelectionEvent;
import y.view.Graph2DSelectionListener;
import y.view.Graph2DView;
import y.view.HitInfo;
import y.view.NavigationComponent;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import y.view.Overview;
import de.zbit.gui.GUITools;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.ext.GraphMLmaps;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.util.StringUtil;
import de.zbit.util.TranslatorTools;

/**
 * <b>TODO: WORK IN PROGRESS, does not work yet.</b><p>
 * <p><b> Aim of this EditMode is to modify {@link RestrictedEditMode} to
 * display the values not in a simple JTable, but in a yFiles generated
 * {@link OptionHandler} TableFactory. </b><p>
 * 
 * An edit mode for yFiles, that allows no creation of new nodes or edges.
 * <p>Furthermore, a table is displayed on click of a node or edge, that
 * shows various properties of the selected item.
 * <p>A navigation and overview panel is also displayed on the
 * implementing panel.
 * <p>An {@link ActionListener} can be registered, that is fired on
 * double click of a pathway-reference node.  
 * 
 * <p><b>This class is a work-in-progress draft that uses the {@link OptionHandler}s
 * of yFiles that allow a much more improved displaying of node
 * properties!</b>
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
public class RestrictedEditModeV2 extends EditMode implements Graph2DSelectionListener {
 
  /**
   * The scroll pane on which the informations about the
   * current selection are displayed.
   */
  JScrollPane scrollPane=null;
  
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
   * This is the ActionCommand for {@link ActionListener}s to open a new pathway tab.
   */
  public final static String OPEN_PATHWAY = "OPEN_PATHWAY";
  
  public RestrictedEditModeV2() {
    super();
    //setEditNodeMode(null);
    setCreateEdgeMode(null);
    
    // Disallow a few operations
    allowLabelSelection(false);
    allowEdgeCreation(false);
    allowNodeCreation(false);
    
    
    /*
     * The Tooltips of nodes, provding descriptions must be shown
     * longer than the system default. Let's show them 15 seconds!
     */
    if (ToolTipManager.sharedInstance().getDismissDelay()<15000) {
      ToolTipManager.sharedInstance().setDismissDelay(15000);
    }
  }
  
  /**
   * @param listener this is used to fire an action with command {@link #OPEN_PATHWAY}
   * if the user double clicked a pathway reference node.
   */
  public RestrictedEditModeV2(ActionListener listener) {
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
        if (mapDescription==null) continue;
        if (mapDescription.equals(GraphMLmaps.NODE_LABEL)) {
          nodeLabel = "<b>"+c.toString().replace(",", ",<br/>")+"</b><br/>";
        } else if (mapDescription.equals(GraphMLmaps.NODE_DESCRIPTION)) {
          description = "<i>"+c.toString().replace(",", ",<br/>")+"</i><br/>";
        } else if (mapDescription.equals(GraphMLmaps.NODE_KEGG_ID)) {
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
    if (nodeLabel != null) {
      tooltip.append(StringUtil.insertLineBreaks(nodeLabel, StringUtil.TOOLTIP_LINE_LENGTH, "<br/>"));
    }
    if (description != null) {
      tooltip.append(StringUtil.insertLineBreaks(description, StringUtil.TOOLTIP_LINE_LENGTH, "<br/>"));
    }
    if ((image != null) && (image.length() > 0)) {
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
      if (kgId!=null && kgId.toLowerCase().startsWith("path:")) {
      	// TODO: Yes and No are already localized by standard JAVA. Use those!
        int ret = GUITools.showQuestionMessage(null, "Do you want to download and open the referenced pathway in a new tab?", 
          System.getProperty("app.name"), new Object[]{"Yes", "No"});
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
    
    scrollPane = new JScrollPane();
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
   * Creates the GUI.
   */
  private JComponent createComponentForOptionHandler( final OptionHandler handler ) {
//    DefaultEditorFactory defaultFactory = new DefaultEditorFactory();
//    Editor editor1 = defaultFactory.createEditor( handler );
    /*
     * TODO: Decide for a view. Default or tabular?
     * - Make uneditable
     * - generate correct elements (addColor, addDouble, etc. instead of addString).
     * - Group items
     * - resonable default width.
     * 
     */
    TableEditorFactory tableFactory = new TableEditorFactory();
    Editor editor = tableFactory.createEditor(handler);
    
    return editor.getComponent();
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
    if (scrollPane==null) {
      if (eastPanel!=null && eastPanel.isVisible()) {
        eastPanel.setVisible(false);
      }
      return;
    }
    
    if (e.getSubject() instanceof Node) {
      Node node = (Node) e.getSubject();
      if(getGraph2D().isSelected(node)) {
        updatePropertiesTable(node);
      }
//      eastPanel.setVisible((propTable.getModel().getRowCount()>0));
      // TODO: isEmpty?
      eastPanel.setVisible(true);
    } else if (e.getSubject() instanceof Edge) {
      Edge edge = (Edge) e.getSubject();
      if(getGraph2D().isSelected(edge)) {
        updatePropertiesTable(edge);
      }
      
//      eastPanel.setVisible((propTable.getModel().getRowCount()>0));
      // TODO: isEmpty?
      eastPanel.setVisible(true);
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
    List<String> headers = new ArrayList<String>();
    List<Object> content = new ArrayList<Object>();
    if (nodeOrEdge instanceof Node) {
      Node node = (Node) nodeOrEdge;
      
      NodeMap[] nm = graph.getRegisteredNodeMaps();
      if (nm!=null) {
        for (int i=0; i<nm.length;i++) {
          //tm.setValueAt(nm[i].get(node), (index++), 1);
          Object c = nm[i].get(node);
          if (c!=null && c.toString().length()>0) {
            String mapName = mapDescriptionMap.getV(nm[i]);
//            if (mapName==null || mapName.startsWith("_")) {
//              // Maps starting with a "_" are marked invisible.
//              continue;
//            }
            
            String head = getNiceCaption(mapName);
            if (head==null) {
              System.err.println("Please de-register NodeMap " + nm[i]);
              continue;
            }
            headers.add(head);
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
            String mapName = mapDescriptionMap.getV(eg[i]);
            if (mapName==null || mapName.startsWith("_")) {
              // Maps starting with a "_" are marked invisible.
              continue;
            }
            
            headers.add(getNiceCaption(mapName));
            content.add(c.toString().replace(";", "; "));
          }
        }
      
    }
    
    // Apply the new table content
//    DefaultTableModel tm = new DefaultTableModel();
//    tm.addColumn("", headers.toArray());
//    tm.addColumn("", content.toArray());
//    propTable.setModel(tm);
    
    final OptionHandler propTable = new OptionHandler("Grid");
    propTable.useSection("Misc");
    for (int i=0; i<content.size(); i++) {
      propTable.addString(headers.get(i), content.get(i).toString());
    }
    scrollPane.setViewportView(createComponentForOptionHandler(propTable));

  }
  
  /**
   * Automatically adjust node size to fit the node label.
   * TODO: This is currently unsed. Furthermore, a very easy
   * implementation method would be {@link NodeRealizer#getLabel()#getWidth()}!
   * @param realizer
   * @param view
   */
  public static void adjustNodeSize(NodeRealizer realizer, Graph2DView view) {
    NodeLabel label = realizer.getLabel();
    int INDENTATION = 5;
    
    if (view.getGraphics()==null) {
      System.err.println("Cannot adjust node size on unknown graphics object.");
      return;
    }
    
    FontMetrics fm;
    if (label.getFont()!=null) {
      fm = view.getGraphics().getFontMetrics(label.getFont());
    } else {
      fm = view.getGraphics().getFontMetrics();
    }
    
    String labelText = label.getText();

    //find max needed width
    Pattern pat = Pattern.compile("(.*)", Pattern.MULTILINE);
    Matcher matcher = pat.matcher(labelText);
    int maxWidth = 0;
    while (matcher.find()) {
      String currentLine = matcher.group();
      int currentWidth = fm.stringWidth(currentLine);
      if (currentWidth > maxWidth) {
        maxWidth = currentWidth;
      }
    }
    if (maxWidth > 0) {
      realizer.setWidth(maxWidth + 2 * INDENTATION);
    } else {//fallback width if no label is set
      realizer.setWidth(view.getGraph2D().getDefaultNodeRealizer().getWidth());
    }

    //find max needed height
    int lineCount = 1;
    for (Matcher m = Pattern.compile("\n").matcher(labelText); m.find();) {
      lineCount++;
    }
    int lineHeight = fm.getHeight();
    if (labelText.length() > 0) {
      realizer.setHeight(lineHeight * lineCount + 2 * INDENTATION);
    } else {//fallback height if no label is set
      realizer.setHeight(view.getGraph2D().getDefaultNodeRealizer().getHeight());
    }
  }

  /**
   * @param v
   * @return
   */
  private static String getNiceCaption(String v) {
    // description, type, description are being first-uppercased.
    if (v==null) {
      return null;
    } else if (v.equals(GraphMLmaps.NODE_LABEL)) {
      return "All names"; // Synonyms
    } else if (v.equals(GraphMLmaps.NODE_GENE_ID)) {
      return "Entrez id(s)";
    } else if (v.equals(GraphMLmaps.NODE_KEGG_ID)) {
      return "Kegg id(s)";
    } else if (v.equals(GraphMLmaps.NODE_UNIPROT_ID)) {
      return "Uniprot id(s)";
    } else if (v.equals(GraphMLmaps.NODE_ENSEMBL_ID)) {
      return "Ensembl id(s)";
    } else if (v.equals(GraphMLmaps.NODE_URL)) {
      return "URL";
    } else if (v.equals(GraphMLmaps.NODE_COLOR)) {
      return "Node color";
    } else if (v.equals(GraphMLmaps.NODE_NAME)) {
      return "Node name";
    } else if (v.equals(GraphMLmaps.NODE_POSITION)) {
      return "Node position";
    } else if (v.equals(GraphMLmaps.EDGE_TYPE)) {
      return "Interaction type";
      
    } else
      return StringUtil.firstLetterUpperCase(v);
  }
}
