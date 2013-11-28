/*
 * Created on Jul 24, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
public class PathwayInternalFrame extends JInternalFrame implements EventSelectionListener {
    private final Logger logger = LoggerFactory.getLogger(PathwayInternalFrame.class);
    private CyZoomablePathwayEditor pathwayEditor;
    // A PathwayDiagram may be used by multile pathways (e.g. a disease pathway and a 
    // normal pathway may share a same PathwayDiagram)
    private List<Long> relatedPathwayIds;
    // DB_ID that is used to invoke this PathwayInternalFrame.
    // This may be changed during the life-time of this object (e.g. a PathwayInternalFrame
    // may be switched to another pathway from a tree selection)
    private Long pathwayId;
    
    /**
     * Default constructor.
     */
    public PathwayInternalFrame() {
        init();
    }
    
    private void init() {
        pathwayEditor = new CyZoomablePathwayEditor();
        getContentPane().add(pathwayEditor, BorderLayout.CENTER);
        pathwayEditor.getPathwayEditor().getSelectionModel().addGraphEditorActionListener(new GraphEditorActionListener() {
            @Override
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() != ActionType.SELECTION)
                    return;
                @SuppressWarnings("unchecked")
                List<Renderable> selection = pathwayEditor.getPathwayEditor().getSelection();
                EventSelectionEvent selectionEvent = new EventSelectionEvent();
                selectionEvent.setParentId(relatedPathwayIds.get(0));
                // Get the first selected event
                Renderable firstEvent = null;
                if (selection != null && selection.size() > 0) {
                    for (Renderable r : selection) {
                        if (r.getReactomeId() != null && (r instanceof HyperEdge || r instanceof ProcessNode)) {
                            firstEvent = r;
                            break;
                        }
                    }
                }
                if (firstEvent == null) {
                    selectionEvent.setEventId(relatedPathwayIds.get(0));
                    selectionEvent.setIsPathway(true);
                }
                else {
                    selectionEvent.setEventId(firstEvent.getReactomeId());
                    selectionEvent.setIsPathway(firstEvent instanceof ProcessNode);
                }
                PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().propageEventSelectionEvent(PathwayInternalFrame.this,
                                                                                                            selectionEvent);
            }
        });
        // Fire an event selection
        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                EventSelectionEvent selectionEvent = new EventSelectionEvent();
                selectionEvent.setParentId(relatedPathwayIds.get(0));
                selectionEvent.setEventId(relatedPathwayIds.get(0));
                selectionEvent.setIsPathway(true);
                PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().propageEventSelectionEvent(PathwayInternalFrame.this,
                                                                                                            selectionEvent);
            }
            
        });
        // Add popup menu
        pathwayEditor.getPathwayEditor().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }
            
        });
    }
    
    private void doPopup(MouseEvent e) {
        PathwayEditor wrappedEditor = pathwayEditor.getPathwayEditor();
        List<?> selection = wrappedEditor.getSelection();
        if (selection != null && selection.size() > 1)
            return;
        JPopupMenu popup = null;
        if (selection == null || selection.size() == 0) {
            popup = new JPopupMenu();
            JMenuItem convertToFINetwork = new JMenuItem("Convert as FI Network");
            convertToFINetwork.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    convertAsFINetwork();
                }
            });
            popup.add(convertToFINetwork);
            popup.show(wrappedEditor,
                       e.getX(),
                       e.getY());
        }
    }
    
    /**
     * Set the ids of pathways displayed in this PathwayInternalFrame. A pathway diagram may represent
     * multiple pathways.
     * @param pathwayIds
     */
    public void setRelatedPathwayIds(List<Long> pathwayIds) {
        this.relatedPathwayIds = pathwayIds;
    }
    
    @Override
    public void eventSelected(EventSelectionEvent selectionEvent) {
        if (!relatedPathwayIds.contains(selectionEvent.getParentId())) {
            // Remove any selection
            PathwayEditor editor = pathwayEditor.getPathwayEditor();
            editor.removeSelection();
            editor.repaint(editor.getVisibleRect());
            return;
        }
        try {
            setSelected(true); // Select this PathwayInternalFrame too!
        }
        catch(PropertyVetoException e) {
            logger.error("Error in eventSelected: " + e, e);
        }
        Long eventId = selectionEvent.getEventId();
        PathwayEditor editor = pathwayEditor.getPathwayEditor();
        // The top-level should be selected
        if (relatedPathwayIds.contains(eventId)) {
            editor.removeSelection();
            editor.repaint(editor.getVisibleRect());
            return;
        }
        List<Renderable> selection = new ArrayList<Renderable>();
        for (Object obj : editor.getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (eventId.equals(r.getReactomeId()))
                selection.add(r);
        }
        if (selection.size() > 0 || !selectionEvent.isPathway()) {
            editor.setSelection(selection);
            return;
        }
        // Need to check if any contained events should be highlighted since the selected event cannot
        // be highlighted
        try {
            List<Long> dbIds = ReactomeRESTfulService.getService().getContainedEventIds(eventId);
            for (Object obj : editor.getDisplayedObjects()) {
                Renderable r = (Renderable) obj;
                if (dbIds.contains(r.getReactomeId()))
                    selection.add(r);
            }
            if (selection.size() > 0)
                editor.setSelection(selection);
        }
        catch(Exception e) {
            logger.error("Error in eventSelected: " + e, e);
        }
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
    
    public void setPathwayId(Long pathwayId) {
        this.pathwayId = pathwayId;
    }
    
    public Long getPathwayId() {
        return this.pathwayId;
    }
    
    private void convertAsFINetwork() {
        //TODO: The RESTFulFIService class should be refactored and moved to other package.
        // Right now it is in the top-level package. Also the version of FI network
        // Used in this place may needs to be changed. 
        try {
            DiagramAndNetworkSwitcher helper = new DiagramAndNetworkSwitcher();
            Set<String> hitGenes = pathwayEditor.getHitGenes();
            helper.convertToFINetwork(getPathwayId(),
                                      pathwayEditor.getPathwayEditor().getRenderable(),
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
