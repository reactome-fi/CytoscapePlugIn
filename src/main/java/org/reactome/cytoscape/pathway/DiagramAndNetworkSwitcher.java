/*
 * Created on Nov 5, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.render.RenderablePathway;
import org.gk.util.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.FIAnnotation;
import org.reactome.funcInt.Interaction;
import org.reactome.funcInt.ReactomeSource;

/**
 * This class is used to switch between two views: pathway diagram view and FI network view.
 * @author gwu
 *
 */
public class DiagramAndNetworkSwitcher {
    
    public DiagramAndNetworkSwitcher() {
    }
    
    /**
     * Convert a FI network converted from a pathway diagram back to a pathway diagram.
     * @param networkView
     */
    public void convertToDiagram(final CyNetworkView networkView) {
        // Just in case
        if (networkView == null)
            return;
        TableHelper helper = new TableHelper();
        Long pathwayId = helper.getStoredNetworkAttribute(networkView.getModel(),
                                                         "PathwayId",
                                                         Long.class);
        if (pathwayId == null)
            return;
        PathwayDiagramRegistry registry = PathwayDiagramRegistry.getRegistry();
        registry.showPathwayDiagram(pathwayId);
        // Need to highlight if any
        PathwayInternalFrame frame = registry.getPathwayFrameWithWait(pathwayId);
        if (frame != null) {
            PathwayEnrichmentHighlighter hiliter = PathwayEnrichmentHighlighter.getHighlighter();
            hiliter.highlightPathway(frame.getZoomablePathwayEditor());
        }
        // If the following code is invoked before the above statment is finished (it is possible since
        // a new thread is going to be used), a null exception may be thrown. So wrap it in an invokeLater method
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Do some clean-up
                BundleContext context = PlugInObjectManager.getManager().getBundleContext();
                ServiceReference reference = context.getServiceReference(CyNetworkManager.class.getName());
                CyNetworkManager mananger = (CyNetworkManager) context.getService(reference);
                mananger.destroyNetwork(networkView.getModel());
                context.ungetService(reference);
            }
        });
    }
    
    public void convertToFINetwork(final Long pathwayId,
                                   final RenderablePathway pathway,
                                   final Set<String> hitGenes,
                                   final PathwayInternalFrame pathwayFrame) throws Exception {
        Task task = new AbstractTask() {
            
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                convertPathwayToFINetwork(pathwayId,
                                          pathway, 
                                          hitGenes,
                                          taskMonitor,
                                          pathwayFrame);
            }
        }; 
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        taskManager.execute(new TaskIterator(task));
    }
    
    private void convertPathwayToFINetwork(final Long pathwayId,
                                           final RenderablePathway pathway,
                                           final Set<String> hitGenes,
                                           TaskMonitor taskMonitor,
                                           PathwayInternalFrame pathwayFrame) throws Exception {
        taskMonitor.setTitle("Convert Pathway to FI Network");
        taskMonitor.setStatusMessage("Converting to FI network...");
        taskMonitor.setProgress(0.0d);
        RESTFulFIService fiService = new RESTFulFIService();
        List<Interaction> interactions = fiService.convertPathwayToFIs(pathwayId);
        if (interactions == null || interactions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInUtilities.getCytoscapeDesktop(),
                                          "There is no FI existing in the selected pathway.",
                                          "No FI",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Make sure this PathwayInternalFrame should be closed
        pathwayFrame.setVisible(false);
        pathwayFrame.dispose();
        
        taskMonitor.setProgress(0.50d);
        // Need to create a new CyNetwork
        FINetworkGenerator generator = new FINetworkGenerator();
        CyNetwork network = generator.constructFINetwork(interactions);
        // Add some meta information
        CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
        row.set("name",
                "FI Nework for " + pathway.getDisplayName());
        TableHelper tableHelper = new TableHelper();
        tableHelper.markAsFINetwork(network);
        tableHelper.storeDataSetType(network, 
                "PathwayDiagram");
        tableHelper.storeNetworkAttribute(network,
                                          "PathwayId", 
                                          pathwayId);
        PathwayEnrichmentHighlighter.getHighlighter().highlightNework(network, 
                                                                      hitGenes);
        // Store Instance ids information
        Map<String, String> edgeToIds = new HashMap<String, String>();
        // Keep annotation information
        Map<String, String> edgeToAnnotation = new HashMap<String, String>();
        Map<String, String> edgeToDirection = new HashMap<String, String>();
        StringBuilder builder = new StringBuilder();
        for (Interaction interaction : interactions) {
            String node1 = interaction.getFirstProtein().getShortName();
            String node2 = interaction.getSecondProtein().getShortName();
            Set<ReactomeSource> sources = interaction.getReactomeSources();
            builder.setLength(0);
            for (ReactomeSource src : sources) {
                if (builder.length() > 0)
                    builder.append(",");
                builder.append(src.getReactomeId());
            }
            String edgeName = node1 + " (FI) " + node2;
            edgeToIds.put(edgeName,
                          builder.toString());
            FIAnnotation annotation = interaction.getAnnotation();
            edgeToAnnotation.put(edgeName, annotation.getAnnotation());
            edgeToDirection.put(edgeName, annotation.getDirection());
        }
        tableHelper.storeEdgeAttributesByName(network, "SourceIds", edgeToIds);
        tableHelper.storeEdgeAttributesByName(network, "FI Annotation", edgeToAnnotation);
        tableHelper.storeEdgeAttributesByName(network, "FI Direction", edgeToDirection);
        
        // Need to query sourceIds for displayed genes
        taskMonitor.setStatusMessage("Fetch genes to ids mapping...");
        Map<String, List<Long>> geneToDBIds = fiService.getGeneToDbIDs(pathway.getReactomeDiagramId());
        // Need to a little format to avoid storing a list
        Map<String, String> geneToIdsAtt = new HashMap<String, String>();
        for (String gene : geneToDBIds.keySet()) {
            List<Long> list = geneToDBIds.get(gene);
            geneToIdsAtt.put(gene, StringUtils.join(",", list));
        }
        tableHelper.storeNodeAttributesByName(network, "SourceIds", geneToIdsAtt);
        
        // Cache the fetched pathway diagram to avoid another slow query
        PathwayDiagramRegistry.getRegistry().registerNetworkToDiagram(network,
                                                                      pathway);
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        
        ServiceReference reference = context.getServiceReference(CyNetworkManager.class.getName());
        CyNetworkManager networkManager = (CyNetworkManager) context.getService(reference);
        networkManager.addNetwork(network);
        networkManager = null;
        context.ungetService(reference);
        
        reference = context.getServiceReference(CyNetworkViewFactory.class.getName());
        CyNetworkViewFactory viewFactory = (CyNetworkViewFactory) context.getService(reference);
        CyNetworkView view = viewFactory.createNetworkView(network);
        viewFactory = null;
        context.ungetService(reference);
        
        reference = context.getServiceReference(CyNetworkViewManager.class.getName());
        CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(reference);
        viewManager.addNetworkView(view);
        viewManager = null;
        context.ungetService(reference);
        
        ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
        FIVisualStyle visStyler = (FIVisualStyle) context.getService(servRef);
        visStyler.setVisualStyle(view);
        visStyler.setLayout();
        visStyler = null;
        context.ungetService(servRef);
        
        taskMonitor.setProgress(1.0d);
        PropertyChangeEvent event = new PropertyChangeEvent(this, 
                                                            "ConvertDiagramToFIView",
                                                            pathway,
                                                            null);
        PathwayDiagramRegistry.getRegistry().firePropertyChange(event);
        
    }
    
}
