/*
 * Created on Nov 5, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.gk.render.Renderable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.funcInt.FIAnnotation;
import org.reactome.funcInt.Interaction;
import org.reactome.funcInt.ReactomeSource;

/**
 * This class is used to switch between two views: pathway diagram view and FI network view.
 * @author gwu
 *
 */
public class DiagramAndNetworkSwitchHelper {
    
    public DiagramAndNetworkSwitchHelper() {
    }
    
    /**
     * Convert a FI network converted form a pathway diagram back to a pathway diagram.
     * @param networkView
     */
    public void convertToDiagram(CyNetworkView networkView) {
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
        // Do some clean-up
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(CyNetworkManager.class.getName());
        CyNetworkManager mananger = (CyNetworkManager) context.getService(reference);
        mananger.destroyNetwork(networkView.getModel());
        context.ungetService(reference);
    }
    
    public void convertToFINetwork(Long pathwayId,
                                   Renderable pathway) throws Exception {
        //TODO: The RESTFulFIService class should be refactored and moved to other package.
        // Right now it is in the top-level package. Also the version of FI network
        // Used in this place may needs to be changed. 
        RESTFulFIService fiService = new RESTFulFIService();
        List<Interaction> interactions = fiService.convertPathwayToFIs(pathwayId);
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
        
        PropertyChangeEvent event = new PropertyChangeEvent(this, 
                                                            "ConvertDiagramToFIView",
                                                            pathway,
                                                            null);
        PathwayDiagramRegistry.getRegistry().firePropertyChange(event);
        
        //            moveDiagramToResultsPane();
    }
    
    private void moveDiagramToResultsPane(Renderable pathway) {
        //TODO: A big chuck of the following code should be refactored into a common place since
        // it is shared in other classes (e.g. ModuleBasedSurvivalAnalysisResultHelper).
        // Check if a PathwayDiagram has been displayed in the results panel
        // This title is based on class definition of 
        String tabTitle = "Reactome Pathway";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tabbedPane = desktopApp.getCytoPanel(CytoPanelName.EAST);
        CytoPanelState currentState = tabbedPane.getState();
        if (currentState == CytoPanelState.HIDE || currentState == CytoPanelState.FLOAT)
            tabbedPane.setState(CytoPanelState.DOCK);
        int numComps = tabbedPane.getCytoPanelComponentCount();
        int componentIndex = -1;
        for (int i = 0; i < numComps; i++) {
            Component aComp = (Component) tabbedPane.getComponentAt(i);
            if ( (aComp instanceof CytoPanelComponent) && ((CytoPanelComponent) aComp).getTitle().equalsIgnoreCase(tabTitle))
            {
                componentIndex = i;
                break;
            }
        }
        if (componentIndex == -1) {
         // Display PathwayDiagram in the results Panel
            CyZoomablePathwayEditor pathwayEditor = new CyZoomablePathwayEditor();
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            context.registerService(CytoPanelComponent.class.getName(),
                                    pathwayEditor,
                                    new Properties());
            int index = tabbedPane.indexOfComponent(pathwayEditor);
            if (index == -1) 
                return; // Most likely there is something wrong if index == -1. This should not occur.
            componentIndex = index;
        }
 
        // Display PathwayDiagram in the results Panel
        CyZoomablePathwayEditor pathwayEditor = (CyZoomablePathwayEditor) tabbedPane.getComponentAt(componentIndex);
        pathwayEditor.getPathwayEditor().setRenderable(pathway);
    }
    
}
