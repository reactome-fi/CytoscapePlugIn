/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.gkEditor.PathwayOverviewPane;
import org.gk.gkEditor.ZoomablePathwayEditor;

/**
 * This customized JPanel, which implements CytoPanelComponent, is used as a control panel for Reactome pathways.
 * The pathway hierarchy and an overview of displayed diagram will be displayed here.
 * @author gwu
 *
 */
public class PathwayControlPanel extends JPanel implements CytoPanelComponent {
    
    private PathwayOverviewPane overview;
    private EventTreePane eventPane;
    
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
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        eventPane, 
                                        overview);
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
    
}
