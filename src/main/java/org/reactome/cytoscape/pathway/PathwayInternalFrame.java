/*
 * Created on Jul 24, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.HyperEdge;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customized JInternalFrame for displaying Reactome pathway diagrams.
 * @author gwu
 *
 */
public class PathwayInternalFrame extends JInternalFrame {
    private final Logger logger = LoggerFactory.getLogger(PathwayInternalFrame.class);
    private CyZoomablePathwayEditor pathwayEditor;
    
    /**
     * Default constructor.
     */
    public PathwayInternalFrame() {
        init();
    }
    
    private void init() {
        pathwayEditor = new CyZoomablePathwayEditor();
        getContentPane().add(pathwayEditor, BorderLayout.CENTER);
        // Fire an event selection
        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                EventSelectionEvent selectionEvent = new EventSelectionEvent();
                List<Long> relatedIds = pathwayEditor.getRelatedPathwaysIds();
                if (relatedIds.size() > 0) {
                    selectionEvent.setParentId(relatedIds.get(0));
                    selectionEvent.setEventId(relatedIds.get(0));
                }
                selectionEvent.setIsPathway(true);
                PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().propageEventSelectionEvent(pathwayEditor,
                                                                                                            selectionEvent);
            }
            
        });
        pathwayEditor.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("convertAsFINetwork"))
                    convertAsFINetwork();
            }
        });
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor.getPathwayEditor();
    }
    
    public CyZoomablePathwayEditor getZoomablePathwayEditor() {
        return this.pathwayEditor;
    }
    
    public RenderablePathway getDisplayedPathway() {
        return (RenderablePathway) pathwayEditor.getPathwayEditor().getRenderable();
    }
    
    /**
     * @param title
     * @param resizable
     * @param closable
     * @param maximizable
     * @param iconifiable
     */
    public PathwayInternalFrame(String title, boolean resizable,
                                boolean closable, boolean maximizable, boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);
        init();
    }
    
    /**
     * Set the pathway diagram in XML to be displayed
     */
    public void setPathwayDiagramInXML(String xml) throws Exception {
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway pathway = reader.openDiagram(xml);
        pathwayEditor.getPathwayEditor().setRenderable(pathway);
    }
    
    /**
     * DB_ID that is used to invoke this PathwayInternalFrame. This may be changed during the life-time 
     * of this object (e.g. a PathwayInternalFrame may be switched to another pathway from a tree 
     * selection)
     * @param pathwayId
     */
    public void setPathwayId(Long pathwayId) {
        Renderable pathway = pathwayEditor.getPathwayEditor().getRenderable();
        if (pathway != null)
            pathway.setReactomeId(pathwayId);
    }
    
    public Long getPathwayId() {
        Renderable pathway = pathwayEditor.getPathwayEditor().getRenderable();
        if (pathway != null)
            return pathway.getReactomeId();
        return null;
    }
    
    /**
     * Set the ids of pathways displayed in this PathwayInternalFrame. A pathway diagram may represent
     * multiple pathways.
     * @param pathwayIds
     */
    public void setRelatedPathwayIds(List<Long> pathwayIds) {
        pathwayEditor.setRelatedPathwayIds(pathwayIds);
    }
    
    private void convertAsFINetwork() {
        //TODO: The RESTFulFIService class should be refactored and moved to other package.
        // Right now it is in the top-level package. Also the version of FI network
        // Used in this place may needs to be changed. 
        try {
            DiagramAndNetworkSwitcher helper = new DiagramAndNetworkSwitcher();
            Set<String> hitGenes = PathwayEnrichmentHighlighter.getHighlighter().getHitGenes();
            helper.convertToFINetwork(getPathwayId(),
                                      (RenderablePathway)pathwayEditor.getPathwayEditor().getRenderable(),
                                      hitGenes);
            
            // Make sure this PathwayInternalFrame should be closed
            setVisible(false);
            dispose();
        }
        catch(Exception e) {
            logger.error("Error in convertAsFINetwork(): " + e.getMessage(), e);
            JOptionPane.showMessageDialog(this, 
                                          "Error in converting a pathway to a FI network: " + e.getMessage(),
                                          "Error in Converting Pathway to FI Network",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
}
