/*
 * Created on Mar 3, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.PGMFactor;
import org.reactome.pgm.PGMFactorGraph;
import org.reactome.pgm.PGMVariable;

/**
 * A similar class to DiagramAndNetworkSwitcher. This class is used to switch between a pathway diagram view and its
 * factor graph view.
 * @author gwu
 *
 */
public class DiagramAndFactorGraphSwitcher {
    
    /**
     * Default constructor.
     */
    public DiagramAndFactorGraphSwitcher() {
    }
    
    public void convertToFactorGraph(final Long pathwayId,
                                     final RenderablePathway pathway,
                                     final PathwayInternalFrame pathwayFrame) throws Exception {
        Task task = new AbstractTask() {
            
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                convertPathwayToFactorGraph(pathwayId,
                                            pathway,
                                            pathwayFrame,
                                            taskMonitor);
            }
        }; 
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        taskManager.execute(new TaskIterator(task)); 
    }
    
    private void convertPathwayToFactorGraph(Long pathwayId,
                                             RenderablePathway pathway,
                                             PathwayInternalFrame pathwayFrame,
                                             TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Convert Pathway to Factor Graph");
        taskMonitor.setStatusMessage("Converting to factor graph...");
        taskMonitor.setProgress(0.0d);
        RESTFulFIService fiService = new RESTFulFIService();
        PGMFactorGraph fg = fiService.convertPathwayToFactorGraph(pathwayId);
        if (fg == null || fg.getFactors() == null || fg.getFactors().size() == 0) {
            JOptionPane.showMessageDialog(PlugInUtilities.getCytoscapeDesktop(),
                                          "Pathway" + "\"" + pathway.getDisplayName() + "\"" + 
                                          " cannot be converted into a factor graph.",
                                          "No Factor Graph",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Make sure this PathwayInternalFrame should be closed
        pathwayFrame.setVisible(false);
        pathwayFrame.dispose();
        
        taskMonitor.setProgress(0.50d);
        // Need to create a new CyNetwork
        FINetworkGenerator generator = new FINetworkGenerator();
        Set<String> interactions = createEdgesFromFG(fg);
        CyNetwork network = generator.constructFINetwork(interactions);
        // Add some meta information
        CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
        row.set("name",
                "Factor Graph for " + pathway.getDisplayName());
        TableHelper tableHelper = new TableHelper();
//        tableHelper.markAsFINetwork(network);
//        tableHelper.storeDataSetType(network, 
//                "PathwayDiagram");
        tableHelper.storeNetworkAttribute(network,
                                          "PathwayId", 
                                          pathwayId);
        
        Map<String, String> nodeTypeInfo = generateNodeTypeInfo(fg);
        tableHelper.storeNodeAttributesByName(network, 
                                              "NodeType",
                                              nodeTypeInfo);
        Map<String, String> nodeLabelInfo = generateNodeLabel(fg);
        tableHelper.storeNodeAttributesByName(network, 
                                              "nodeLabel",
                                              nodeLabelInfo);
        Map<String, String> nodeToolTipInfo = generateNodeToolTip(fg);
        tableHelper.storeNodeAttributesByName(network,
                                              "nodeToolTip",
                                              nodeToolTipInfo);

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
        
        // This FIVisualStyle is not registered as a service to save some coding by
        // avoiding clash with FIVisualStyleImp
        FIVisualStyle visStyler = new FactorGraphVisualStyle();
        visStyler.setVisualStyle(view);
        visStyler.setLayout();
        
        taskMonitor.setProgress(1.0d);
        PropertyChangeEvent event = new PropertyChangeEvent(this, 
                                                            "ConvertPathwayToFactorGraph",
                                                            pathway,
                                                            null);
        PathwayDiagramRegistry.getRegistry().firePropertyChange(event);
    }
    
    private Map<String, String> generateNodeToolTip(PGMFactorGraph fg) {
        Map<String, String> nodeToolTipInfo = new HashMap<String, String>();
        for (PGMFactor factor : fg.getFactors()) {
            String name = factor.getName();
            if (name == null)
                name = factor.getLabel();
            nodeToolTipInfo.put(factor.getLabel(), 
                                "factor: " + name);
        }
        for (PGMVariable variable : fg.getVariables()) {
            String name = variable.getName();
            if (name == null)
                name = variable.getLabel();
            nodeToolTipInfo.put(variable.getLabel(), 
                                "variable: " + name);
        }
        return nodeToolTipInfo;
    }
    
    /**
     * Use this method to generate a customized node labels for a factor graph.
     * @param fg
     * @return
     */
    private Map<String, String> generateNodeLabel(PGMFactorGraph fg) {
        Map<String, String> nodeLabelInfo = new HashMap<String, String>();
        for (PGMFactor factor : fg.getFactors()) {
            // Don't want to display anything for factors
            nodeLabelInfo.put(factor.getLabel(), null);
        }
        for (PGMVariable variable : fg.getVariables()) {
            nodeLabelInfo.put(variable.getLabel(), variable.getLabel());
        }
        return nodeLabelInfo;
    }
    
    private Map<String, String> generateNodeTypeInfo(PGMFactorGraph fg) {
        Map<String, String> nodeTypeInfo = new HashMap<String, String>();
        for (PGMFactor factor : fg.getFactors()) {
            nodeTypeInfo.put(factor.getLabel(), "factor");
        }
        for (PGMVariable variable : fg.getVariables()) {
            nodeTypeInfo.put(variable.getLabel(), "variable");
        }
        return nodeTypeInfo;
    }
    
    /**
     * A helper method to create a set of interactions from a factor graph object.
     * @param fg
     * @return
     */
    private Set<String> createEdgesFromFG(PGMFactorGraph fg) {
        Set<String> edges = new HashSet<String>();
        for (PGMFactor factor : fg.getFactors()) {
            for (PGMVariable var : factor.getVariables()) {
                edges.add(factor.getLabel() + "\t" + var.getLabel());
            }
        }
        return edges;
    }
    
}
