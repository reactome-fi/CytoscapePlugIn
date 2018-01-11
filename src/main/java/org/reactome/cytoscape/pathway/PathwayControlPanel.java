/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.gk.gkEditor.PathwayOverviewPane;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This customized JPanel, which implements CytoPanelComponent, is used as a control panel for Reactome pathways.
 * The pathway hierarchy and an overview of displayed diagram will be displayed here.
 * @author gwu
 *
 */
public class PathwayControlPanel extends JPanel implements CytoPanelComponent, CytoPanelComponentSelectedListener {
    // Create an overview
    private PathwayOverviewPane overview;
    // Used to hold the overview so that a border can be used
    private JPanel overviewContainer;
    // Create a whole view during the FI network view
    private ControlPathwayView pathwayView;
    private EventTreePane eventPane;
    // Used to hold two parts of views
    private JSplitPane jsp;
    // Have to record this network view in order to do selection
    private CyNetworkView networkView;
    // To set selection direction in order to synchronize
    // selection in two views
    private boolean selectFromPathway;
    private boolean selectFromNetwork;
    
    private static PathwayControlPanel instance;
    
    /** 
     * Default private constrcutor so that this class should be used as a singleton only.
     */
    private PathwayControlPanel() {
        init();
    }
    
    public static PathwayControlPanel getInstance() {
        if (instance == null)
            instance = new PathwayControlPanel();
        return instance;
    }
    
    private void init() {
        setLayout(new BorderLayout());
        eventPane = new EventTreePane();
        overview = new PathwayOverviewPane();
        overviewContainer = new JPanel();
        overviewContainer.setBorder(BorderFactory.createEtchedBorder());
        overviewContainer.setLayout(new BorderLayout());
        overviewContainer.add(overview, BorderLayout.CENTER);
        jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        eventPane, 
                                        overviewContainer);
        jsp.setDividerLocation(0.67d); // 2/3 for the pathway tree.
        add(jsp, BorderLayout.CENTER);
        installListeners();
    }
    
    /**
     * Call to load the actual event tree from a RESTful API. This method should be called in order to see the tree.
     */
    public void loadFrontPageItems() throws Exception {
        eventPane.loadFrontPageItems();
    }
    
    public void setAllPathwaysInElement(Element root) throws Exception {
        eventPane.setAllPathwaysInElement(root);
    }
    
    private void installListeners() {
        InternalFrameListener listener = new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                if (e.getInternalFrame() instanceof PathwayInternalFrame) {
                    PathwayInternalFrame pathwayFrame = (PathwayInternalFrame) e.getInternalFrame();
                    ZoomablePathwayEditor pathwayEditor = pathwayFrame.getZoomablePathwayEditor();
                    overview.setParentEditor(pathwayEditor.getPathwayEditor());
                    overview.syncrhonizeScroll(pathwayEditor);
                    overview.setRenderable(pathwayFrame.getDisplayedPathway());
                    switchToOverview(pathwayFrame);
                }
            }

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                if(PathwayDiagramRegistry.getRegistry().getSelectedPathwayFrame() == null) {
                    resetOverviewPane();
                }
            }
            
        };
        PathwayDiagramRegistry.getRegistry().addInternalFrameListener(listener);
        
        PropertyChangeListener propListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propName = evt.getPropertyName();
                if (propName.equals("ConvertDiagramToFIView") ||
                    propName.equals("ConvertPathwayToFactorGraph")) {
                    switchToFullPathwayView((Renderable)evt.getOldValue());
                }
            }
        };
        PathwayDiagramRegistry.getRegistry().addPropertyChangeListener(propListener);
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(CytoPanelComponentSelectedListener.class.getName(),
                                this, 
                                null);
        
        // Delete pathway overview if this OSGi plug-in is down
        SynchronousBundleListener bundleListener = new SynchronousBundleListener() {
            
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() == BundleEvent.STOPPING) {
//                    System.out.println("This bundle is stopping! A pathway overview is being removed!");
                    if (overviewContainer.getParent() != null && 
                        overviewContainer.getParent() instanceof JLayeredPane) {
                        overviewContainer.getParent().remove(overviewContainer);
                    }
                }
            }
        };
        context.addBundleListener(bundleListener);
        
        // Catch network view selection event
        SetCurrentNetworkViewListener currentNetworkViewListener = new SetCurrentNetworkViewListener() {
            
            @Override
            public void handleEvent(SetCurrentNetworkViewEvent event) {
                if (event.getNetworkView() == null)
                    return; // This is more like a Pathway view
                doNetworkViewIsSelected(event.getNetworkView());
            }
        };
        context.registerService(SetCurrentNetworkViewListener.class.getName(),
                                currentNetworkViewListener,
                                null);
        
        // Synchronize selection from network to pathway overview
        RowsSetListener selectionListener = new RowsSetListener() {
            
            @Override
            public void handleEvent(RowsSetEvent event) {
                if (!event.containsColumn(CyNetwork.SELECTED) || 
                    networkView == null ||
                    networkView.getModel() == null || 
                    networkView.getModel().getDefaultEdgeTable() == null ||
                    networkView.getModel().getDefaultNodeTable() == null) {
                    return;
                }
                List<CyEdge> edges = CyTableUtil.getEdgesInState(networkView.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
                List<CyNode> nodes = CyTableUtil.getNodesInState(networkView.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
                handleNetworkSelection(edges, nodes);
            }

        };
        context.registerService(RowsSetListener.class.getName(),
                                selectionListener, 
                                null);
        
        NetworkDestroyedListener networkDetroyedListener = new NetworkDestroyedListener() {
            
            @Override
            public void handleEvent(NetworkDestroyedEvent e) {
                if (networkView == null)
                    return; // Do nothing
                CyNetworkManager manager = e.getSource();
                if (!manager.networkExists(networkView.getModel().getSUID())) {
                    // The current view has been destroyed
                    networkView = null;
                }
            }
        };
        context.registerService(NetworkDestroyedListener.class.getName(),
                                networkDetroyedListener,
                                null);
        
        // Showing pathway enrichments
        eventPane.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("showPathwayEnrichments")) {
                    showPathwayEnrichments((Boolean)evt.getNewValue());
                }
            }
        });
        
        // Remove this panel if a new session is created
        SessionLoadedListener sessionListener = new SessionLoadedListener() {
            
            @Override
            public void handleEvent(SessionLoadedEvent e) {
                resetOverviewPane(); // Do this first so that the overviewcontainer can keep correct size
                if (getParent() != null)
                    getParent().remove(PathwayControlPanel.this);
                // Since we are using a singleton, don't want to keep any enrichment results
                eventPane.removeEnrichmentResults();
                eventPane.resetTree();
            }
        };
        context.registerService(SessionLoadedListener.class.getName(),
                                sessionListener,
                                null);
        
    }
    
    private void showPathwayEnrichments(Boolean isShown) {
        if (jsp.getBottomComponent() != pathwayView) {
            return; // Don't need to do anything since it is not displayed
        }
        if (isShown) {
            PathwayEnrichmentHighlighter highlighter = PathwayEnrichmentHighlighter.getHighlighter();
            highlighter.highlightPathway(pathwayView);
        }
        else
            pathwayView.resetColors();
    }
    
    /**
     * A helper method to handle edge selection from a FI network.
     * For some reason, node selection is called by two threads. Therefore,
     * syncrhonized it.
     * @param edges
     */
    private synchronized void handleNetworkSelection(List<CyEdge> edges,
                                        List<CyNode> nodes) {
        if (selectFromPathway)
            return; // Don't do anything
        selectFromNetwork = true;
        Collection<Long> dbIds = new HashSet<Long>();
        TableHelper tableHelper = null;
        String att = null;
        if (edges != null && edges.size() > 0) {
            tableHelper = new TableHelper();
            att = "SourceIds";
            for (CyEdge edge : edges) {
                String sourceIds = tableHelper.getStoredEdgeAttribute(networkView.getModel(),
                                                                      edge, 
                                                                      att, 
                                                                      String.class);
                if (sourceIds == null)
                    continue;
                String[] tokens = sourceIds.split(",");
                for (String token : tokens)
                    dbIds.add(new Long(token));
            }
        }
        if (nodes != null && nodes.size() > 0) {
            if (tableHelper == null)
                tableHelper = new TableHelper();
            if (att == null)
                att = "SourceIds";
            for (CyNode node : nodes) {
                String sourceIds = tableHelper.getStoredNodeAttribute(networkView.getModel(),
                                                                     node, 
                                                                     att, 
                                                                     String.class);
                if (sourceIds == null)
                    continue;
                String[] tokens = sourceIds.split(",");
                for (String token : tokens)
                    dbIds.add(new Long(token));
            }
        }
        PlugInUtilities.selectByDbIds(pathwayView.getPathwayEditor(), dbIds);
        selectFromNetwork = false;
    }
    
    /**
     * A helper method to handle selection generated from the pathway view.
     */
    private void handlePathwayViewSelection() {
        if (networkView == null || selectFromNetwork)
            return;
        selectFromPathway = true;
        @SuppressWarnings("unchecked")
        List<Renderable> selection = pathwayView.getPathwayEditor().getSelection();
        Set<String> dbIds = new HashSet<String>();
        for (Renderable r : selection) {
            if (r.getReactomeId() != null)
                dbIds.add(r.getReactomeId().toString());
        }
        if (pathwayView.showFIsForSelectedOnly()) {
            showWholeNetwork();
        }
        TableHelper tableHelper = new TableHelper();
        // Do selection for edges
        int totalSelected = 0;
        // To be escaped for node checking
        Set<CyNode> checkedNodes = new HashSet<>();
        for (View<CyEdge> edgeView : networkView.getEdgeViews()) {
            // De-select first
            tableHelper.setEdgeSelected(networkView.getModel(),
                                        edgeView.getModel(),
                                        false);
            String sourceIds = tableHelper.getStoredEdgeAttribute(networkView.getModel(),
                                                                  edgeView.getModel(),
                                                                  "SourceIds", 
                                                                  String.class);
            if (sourceIds == null)
                continue;
            for (String token : sourceIds.split(",")) {
                if (dbIds.contains(token)) {
                    // Select it
                    tableHelper.setEdgeSelected(networkView.getModel(),
                                                edgeView.getModel(),
                                                true);
                    totalSelected ++;
                    // We want to select attached nodes too for easy handling
                    CyEdge edge = edgeView.getModel();
                    if (edge.getSource() != null) {
                        tableHelper.setNodeSelected(networkView.getModel(),
                                                    edge.getSource(), 
                                                    true);
                        totalSelected ++;
                        checkedNodes.add(edge.getSource());
                    }
                    if (edge.getTarget() != null) {
                        tableHelper.setNodeSelected(networkView.getModel(),
                                                    edge.getTarget(), 
                                                    true);
                        checkedNodes.add(edge.getTarget());
                        totalSelected ++;
                    }
                    break;
                }
            }
        }
        // Do selection for nodes
        for (View<CyNode> nodeView : networkView.getNodeViews()) {
            if (checkedNodes.contains(nodeView.getModel()))
                continue;
            tableHelper.setNodeSelected(networkView.getModel(),
                                        nodeView.getModel(), 
                                        false);
            String sourceIds = tableHelper.getStoredNodeAttribute(networkView.getModel(),
                                                                  nodeView.getModel(),
                                                                  "SourceIds",
                                                                  String.class);
            if (sourceIds == null)
                continue;
            for (String token : sourceIds.split(",")) {
                if (dbIds.contains(token)) {
                    tableHelper.setNodeSelected(networkView.getModel(),
                                                nodeView.getModel(), 
                                                true);
                    totalSelected ++;
                    break;
                }
            }
        }
        if (!pathwayView.showFIsForSelectedOnly())
            PlugInUtilities.zoomToSelected(networkView, 
                                           totalSelected);
        hideNotSelected(); 
        networkView.updateView();
        selectFromPathway = false;
    }

    private void showWholeNetwork() {
        // Have to call show all nodes first.
        // Otherwise, the edge lines are curved, most likely
        // because there are no nodes can be displayed for some edges.
        PlugInUtilities.showAllNodes(networkView);
        PlugInUtilities.showAllEdges(networkView);
    }

    private void hideNotSelected() {
        // Escape it if the selection is from the network
        if (!pathwayView.showFIsForSelectedOnly() || selectFromNetwork)
            return;
        List<CyEdge> selecedEdges = CyTableUtil.getEdgesInState(networkView.getModel(),
                CyNetwork.SELECTED,
                true);
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(networkView.getModel(),
                CyNetwork.SELECTED,
                true);
//        // Hide all if nothing is selected, which seems better than showing all.
//        if (selecedEdges.size() == 0 && selectedNodes.size() == 0)
//            return; // We want to show all
        networkView.getEdgeViews().forEach(edgeView -> {
            if (selecedEdges.contains(edgeView.getModel()))
                PlugInUtilities.showEdge(edgeView);
            else
                PlugInUtilities.hideEdge(edgeView);
        });
        networkView.getNodeViews().forEach(nodeView -> {
            if (selectedNodes.contains(nodeView.getModel()))
                PlugInUtilities.showNode(nodeView);
            else
                PlugInUtilities.hideNode(nodeView);
        });
    }
    
    private void doNetworkViewIsSelected(CyNetworkView networkView) {
        TableHelper tableHelper = new TableHelper();
        if (!tableHelper.isReactomeNetwork(networkView))
            return;
        // Check if this is a PathwayDiagram view
        String dataSetType = tableHelper.getDataSetType(networkView);
        if (!dataSetType.equals("PathwayDiagram"))
            return;
        // Choose Pathway
        CyNetwork network = networkView.getModel();
        Long pathwayId = tableHelper.getStoredNetworkAttribute(network,
                                                               "PathwayId",
                                                               Long.class);
        // Have to manually select the event for the tree.
        EventSelectionEvent selectionEvent = new EventSelectionEvent();
        selectionEvent.setEventId(pathwayId);
        selectionEvent.setParentId(pathwayId);
        selectionEvent.setIsPathway(true);
        eventPane.eventSelected(selectionEvent);
        // Need to switch to pathway view
        Renderable diagram = PathwayDiagramRegistry.getRegistry().getDiagramForNetwork(network);
        switchToFullPathwayView(diagram);
        this.networkView = networkView;
    }
    
    private void switchToFullPathwayView(Renderable pathway) {
        // In case a network view is loaded from a saved session
        if (getParent() == null || !isVisible())
            return; // Do nothing is this component is not displayed.
        if (pathwayView == null) {
            pathwayView = new ControlPathwayView();
            // Make sure the overview is at the correct place
            pathwayView.addComponentListener(new ComponentAdapter() {
                
                @Override
                public void componentResized(ComponentEvent e) {
                    setOverviewPositionInPathwayView();
                }
            });
            
            // Synchronize selection
            pathwayView.getPathwayEditor().getSelectionModel().addGraphEditorActionListener(new GraphEditorActionListener() {
                
                @Override
                public void graphEditorAction(GraphEditorActionEvent e) {
                    if (e.getID() == ActionType.SELECTION)
                        handlePathwayViewSelection();
                }
            });
            // Synchronize with tree
            // Always register this to avoid an ConcurrentException related to collection.
            PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().addEventSelectionListener(pathwayView);
        }
        // Check if pathwayView has been set already
        if (jsp.getBottomComponent() == pathwayView) {
            // Just in case a new pathway is passed on
            pathwayView.getPathwayEditor().setRenderable(pathway);
            pathwayView.recordColors();
            PathwayEnrichmentHighlighter.getHighlighter().highlightPathway(pathwayView);
            overview.setRenderable(pathway);
            return; // Don't need to do anything
        }
        // Make sure pathwayView take the original size of overview
        // Note: only preferred size works
        pathwayView.setPreferredSize(overview.getSize());
        pathwayView.getPathwayEditor().setRenderable(pathway);
        pathwayView.recordColors();
        PathwayEnrichmentHighlighter.getHighlighter().highlightPathway(pathwayView);
        overview.syncrhonizeScroll(pathwayView);
        overview.setParentEditor(pathwayView.getPathwayEditor());
        overview.setRenderable(pathway);
        // Replace the overview with the whole pathway diagram view
        // For some reason the divider position changed 
        int dividerPos = jsp.getDividerLocation();
        jsp.setBottomComponent(pathwayView);
        jsp.setDividerLocation(dividerPos);
        // Want to keep the original overview still
        // Get the JFrame
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, jsp);
        if (frame == null)
            return;
        JLayeredPane layeredPane = frame.getLayeredPane();
        layeredPane.add(overviewContainer, JLayeredPane.PALETTE_LAYER);
        overviewContainer.setSize(100, 65);
        // Hope to repaint layeredPane
        layeredPane.invalidate();
        layeredPane.validate();
        // Make sure overview has correct location
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                setOverviewPositionInPathwayView();
            }
        });
    }
    
    private void switchToOverview(PathwayInternalFrame pathwayFrame) {
        // Overview is the default view and should be set already
        if (jsp.getBottomComponent() == overviewContainer)
            return; // It has been set.
        // Remove from the original container
        overviewContainer.getParent().remove(overviewContainer);
        // Don't listen to selection
        overviewContainer.setPreferredSize(pathwayView.getSize());
        int dividerPos = jsp.getDividerLocation();
        jsp.setBottomComponent(overviewContainer);
        jsp.setDividerLocation(dividerPos);
        ZoomablePathwayEditor pathwayEditor = pathwayFrame.getZoomablePathwayEditor();
        overview.syncrhonizeScroll(pathwayEditor);
        overview.setParentEditor(pathwayEditor.getPathwayEditor());
        overview.setRenderable(pathwayEditor.getPathwayEditor().getRenderable());
    }
    
    /**
     * Reset the overview part of the panel.
     */
    private void resetOverviewPane() {
        if (jsp.getBottomComponent() != overviewContainer) {
            // Remove from the original container
            overviewContainer.getParent().remove(overviewContainer);
            jsp.setBottomComponent(overviewContainer);
            jsp.setDividerLocation(0.67d); // The original divider position
        }
        overview.setRenderable(new RenderablePathway());
    }
    
    public void validateDividerPosition() {
        jsp.setDividerLocation(0.67d);
    }
    
    public void setFloatedOverviewVisible(boolean visiable) {
        // Overview is not afloat
        if (pathwayView == null || !pathwayView.isVisible())
            return;
        overviewContainer.setVisible(visiable);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public String getTitle() {
        return "Reactome";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void handleEvent(CytoPanelComponentSelectedEvent e) {
        CytoPanel container = e.getCytoPanel();
        // Target to the control panel only
        if(container.getCytoPanelName() != CytoPanelName.WEST)
            return; 
        setFloatedOverviewVisible(container.getSelectedComponent() == this);
    }

    private void setOverviewPositionInPathwayView() {
        Component parentComp = overviewContainer.getParent();
        Point location = SwingUtilities.convertPoint(pathwayView, 
                                                     3, 
                                                     3, 
                                                     parentComp);
        overviewContainer.setLocation(location);
    }
    
    private class ControlPathwayView extends CyZoomablePathwayEditor {
        
        // Used to control if only FIs for selected reactions should be displayed
        private JCheckBox showFIsForSelectedOnly;
        
        public ControlPathwayView() {
            super();
            Font font = getFont();
            // Use a smaller font to save space
            font = font.deriveFont(font.getSize() - 2.0f);
            setFont(font);
        }
        
        @Override
        protected void initColorSpectrumPane() {
            JPanel southPane = getSouthPane();
            if (southPane == null)
                return; // Cannot do anything
            JPanel newSouthPane = new JPanel();
            newSouthPane.setBorder(BorderFactory.createEtchedBorder());
            newSouthPane.setLayout(new BoxLayout(newSouthPane, BoxLayout.Y_AXIS));
            showFIsForSelectedOnly = new JCheckBox("Show FIs Only for Selected");
            showFIsForSelectedOnly.addActionListener(event -> {
                if (networkView == null)
                    return; // Just in case if the network is closed
                if (showFIsForSelectedOnly.isSelected())
                    hideNotSelected();
                else 
                    showWholeNetwork();
                networkView.updateView();
            });
            remove(southPane);
            southPane.setBorder(BorderFactory.createEtchedBorder());
            newSouthPane.add(southPane);
            // Just to get the border for better view
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.add(showFIsForSelectedOnly);
            newSouthPane.add(pane);
            add(newSouthPane, BorderLayout.SOUTH);
        }
        
        public boolean showFIsForSelectedOnly() {
            return showFIsForSelectedOnly.isSelected();
        }
        
        @Override
        protected void addDataAnalysisMenus(JPopupMenu popup) {
        }

        @Override
        protected void addDataAnalysisMenusForObject(Long dbId, String name, JPopupMenu popup) {
        }
        
    }
    
}
