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

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.gk.gkEditor.PathwayOverviewPane;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.render.Renderable;
import org.osgi.framework.BundleContext;
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
                }
            }
            
        };
        PathwayDiagramRegistry.getRegistry().addInternalFrameListener(listener);
        
        PropertyChangeListener propListener = new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propName = evt.getPropertyName();
                if (propName.equals("ConvertDiagramToFIView")) {
                    handleDiagramToFIViewConversion((Renderable)evt.getOldValue());
                }
            }
        };
        PathwayDiagramRegistry.getRegistry().addPropertyChangeListener(propListener);
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(CytoPanelComponentSelectedListener.class.getName(),
                                this, 
                                null);
    }
    
    private void handleDiagramToFIViewConversion(Renderable pathway) {
        if (pathwayView == null) {
            pathwayView = new CyZoomablePathwayEditor();
            // Make sure pathwayView take the original size of overview
            // Note: only preferred size works
            pathwayView.setPreferredSize(overview.getSize());
        }
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
        // Make sure the overview is at the correct place
        pathwayView.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                Component parentComp = overviewContainer.getParent();
                Point location = SwingUtilities.convertPoint(pathwayView, 
                                                             3, 
                                                             3, 
                                                             parentComp);
                overviewContainer.setLocation(location);
            }

        });
    }
    
    public void setFloatedOverviewVisible(boolean visiable) {
        // Overview is not afloat
        if (!pathwayView.isVisible())
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
    
}
