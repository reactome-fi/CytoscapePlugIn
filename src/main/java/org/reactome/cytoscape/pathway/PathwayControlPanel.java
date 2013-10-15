/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
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
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.gkEditor.PathwayOverviewPane;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.render.Renderable;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

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
    private CyZoomablePathwayEditor pathwayView;
    private EventTreePane eventPane;
    // Used to hold two parts of views
    private JSplitPane jsp;
    
    /** 
     * Default constructor.
     */
    public PathwayControlPanel() {
        init();
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
    public void loadEventTree() throws Exception {
        eventPane.loadEventTree();
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
            
        };
        PathwayDiagramRegistry.getRegistry().addInternalFrameListener(listener);
        
        PropertyChangeListener propListener = new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propName = evt.getPropertyName();
                if (propName.equals("ConvertDiagramToFIView")) {
                    switchToFullPathwayView((Renderable)evt.getOldValue());
                }
            }
        };
        PathwayDiagramRegistry.getRegistry().addPropertyChangeListener(propListener);
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(CytoPanelComponentSelectedListener.class.getName(),
                                this, 
                                null);
        
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
    }
    
    private void doNetworkViewIsSelected(CyNetworkView networkView) {
        TableHelper tableHelper = new TableHelper();
        if (!tableHelper.isFINetwork(networkView))
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
    }
    
    private void switchToFullPathwayView(Renderable pathway) {
        if (pathwayView == null) {
            pathwayView = new CyZoomablePathwayEditor();
            // Make sure the overview is at the correct place
            pathwayView.addComponentListener(new ComponentAdapter() {
                
                @Override
                public void componentResized(ComponentEvent e) {
                    setOverviewPositionInPathwayView();
                }
                
            });
        }
        // Check if pathwayView has been set already
        if (jsp.getBottomComponent() == pathwayView)
            return; // Don't need to do anything
        // Make sure pathwayView take the original size of overview
        // Note: only preferred size works
        pathwayView.setPreferredSize(overview.getSize());
        pathwayView.getPathwayEditor().setRenderable(pathway);
        overview.syncrhonizeScroll(pathwayView);
        overview.setParentEditor(pathwayView.getPathwayEditor());
        overview.setRenderable(pathway);
        // Replace the overview with the whole pathway diagram view
        jsp.setBottomComponent(pathwayView);
        // Want to keep the original overview still
        // Get the JFrame
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, jsp);
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
        overviewContainer.setPreferredSize(pathwayView.getSize());
        jsp.setBottomComponent(overviewContainer);
        ZoomablePathwayEditor pathwayEditor = pathwayFrame.getZoomablePathwayEditor();
        overview.syncrhonizeScroll(pathwayEditor);
        overview.setParentEditor(pathwayEditor.getPathwayEditor());
        overview.setRenderable(pathwayEditor.getPathwayEditor().getRenderable());
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
    
}
