/*
 * Created on Jul 24, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.TableModel;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResults;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.IPAPathwaySummaryPane;
import org.reactome.cytoscape.pgm.InferenceResultsControl;
import org.reactome.cytoscape.service.PathwayHighlightDataType;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
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
    // To be unregsiter
    private ServiceRegistration tableSelectionRegistration;
    // A pathway diagram may be related to several pathways.
    // This value is used to set the start pathway for highlight or other purpose.
    private String pathwayName;

    /**
     * Default constructor.
     */
    public PathwayInternalFrame() {
        init();
    }
    
    public String getPathwayName() {
        return pathwayName;
    }

    public void setPathwayName(String pathwayName) {
        this.pathwayName = pathwayName;
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
                // If pathway inference results existing, we will show them
                // This feature should be used when multiple PathwayInternalFrames are displayed showing inference results
                // and the user wants to choose one.
                RenderablePathway diagram = (RenderablePathway) pathwayEditor.getPathwayEditor().getRenderable();
                FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(diagram);
                if (fgResults != null) {
                    // Show results
                    InferenceResultsControl control = new InferenceResultsControl();
                    control.setHiliteControlPane(pathwayEditor.getHighlightControlPane());
                    try {
                        control.showInferenceResults(fgResults);
                    }
                    catch(Exception e1) {
                        e1.printStackTrace(); // We will not show a dialog here to avoid interferring the user.
                    }
                }
            }

            /**
             * Need to override this method, instead of internalFrameClosing(), since this
             * frame will be closed programmatically.
             */
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                if (tableSelectionRegistration != null)
                    tableSelectionRegistration.unregister();
                PlugInObjectManager.getManager().getDBIdSelectionMediator().getSelectables().remove(pathwayEditor);
            }
            
            /**
             * If the user manually closes this frame, all cached results will be deleted in
             * order to keep the memory use small.
             */
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                FactorGraphRegistry.getRegistry().cleanUpCache((RenderablePathway)pathwayEditor.getPathwayEditor().getRenderable());
                PlugInObjectManager.getManager().getDBIdSelectionMediator().getSelectables().remove(pathwayEditor);
            }
            
        });
        pathwayEditor.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("convertAsFINetwork"))
                    convertAsFINetwork();
                else if (evt.getPropertyName().equals("convertAsFactorGraph"))
                    convertAsFactorGraph();
            }
        });
        
        // This is more like a hack in order to get the selection in a table
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        PropertyChangeListener listener = new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("tableSelection") && 
                    evt.getSource() instanceof JTable) {
                    handleTableSelection((JTable)evt.getSource());
                }
            }
        };
        
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("target", IPAPathwaySummaryPane.class.getSimpleName());
        tableSelectionRegistration = context.registerService(PropertyChangeListener.class.getName(), 
                                                             listener,
                                                             props);
    }
    
    public PathwayHighlightDataType getHighlightDataType() {
        return pathwayEditor.getHighlightDataType();
    }
    
    private IPAPathwaySummaryPane getSummaryPane() {
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        String title = "IPA Pathway Analysis";
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAPathwaySummaryPane outputPane = null;
        if (index > -1)
            outputPane = (IPAPathwaySummaryPane) tableBrowserPane.getComponentAt(index);
        return outputPane;
    }
    
    private void handleTableSelection(JTable table) {
        // Get the displayed summary pane
        IPAPathwaySummaryPane summaryPane = getSummaryPane();
        if (summaryPane == null)
            return; // Nothing needs to be done
        // Check if the factor graph used is converted from this displayed diagram
        FactorGraphInferenceResults fgResults = summaryPane.getFGInferenceResults();
        if (fgResults == null || fgResults.getFactorGraph() == null)
            return;
        RenderablePathway diagram = getDisplayedPathway();
        FactorGraph fg = FactorGraphRegistry.getRegistry().getFactorGraph(diagram);
        if (fgResults.getFactorGraph() != fg)
            return;
        // Get the selected variable labels
        Set<Long> dbIds = new HashSet<Long>();
        if (table.getSelectedRowCount() > 0) {
            TableModel model = table.getModel();
            for (int row : table.getSelectedRows()) {
                int modelIndex = table.convertRowIndexToModel(row);
                String sourceId = (String) model.getValueAt(modelIndex, 0);
                if (sourceId.matches("\\d++"))
                    dbIds.add(new Long(sourceId));
            }
        }
        PlugInUtilities.selectByDbIds(pathwayEditor.getPathwayEditor(), dbIds);
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
        PlugInUtilities.setPathwayDiagramInXML(pathwayEditor.getPathwayEditor(), xml);
        pathwayEditor.recordColors();
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
    
    private void convertAsFactorGraph() {
        try {
            DiagramAndFactorGraphSwitcher helper = new DiagramAndFactorGraphSwitcher();
            helper.convertToFactorGraph(getPathwayId(),
                                        (RenderablePathway)pathwayEditor.getPathwayEditor().getRenderable(),
                                        this);
        }
        catch(Exception e) {
            logger.error("Error in convertAsFactorGraph(): " + e.getMessage(), 
                         e);
            JOptionPane.showMessageDialog(this, 
                                          "Error in converting a pathway to a factor graph: " + e.getMessage(),
                                          "Error in Converting Pathway to Factor Graph",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void convertAsFINetwork() {
        try {
            DiagramAndNetworkSwitcher helper = new DiagramAndNetworkSwitcher();
            Set<String> hitGenes = PathwayEnrichmentHighlighter.getHighlighter().getHitGenes();
            helper.convertToFINetwork(getPathwayId(),
                                      getPathwayName(),
                                      (RenderablePathway)pathwayEditor.getPathwayEditor().getRenderable(),
                                      hitGenes,
                                      this);
//            // Make sure this PathwayInternalFrame should be closed
//            setVisible(false);
//            dispose();
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
