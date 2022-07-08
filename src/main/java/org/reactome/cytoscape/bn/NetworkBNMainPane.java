package org.reactome.cytoscape.bn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.BooleanRelation;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.booleannetwork.HillFunction;
import org.reactome.booleannetwork.IdentityFunction;
import org.reactome.booleannetwork.TransferFunction;
import org.reactome.cytoscape.genescore.GeneSelectionHandler;
import org.reactome.cytoscape.service.GeneLevelSelectionHandler;
import org.reactome.cytoscape.service.NetworkViewSelectionHandler;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

public class NetworkBNMainPane extends BooleanNetworkMainPane {
	
	private CyNetworkView networkView;
	private BooleanNetwork booleanNetwork;
	private ServiceRegistration networkSelectionRegistration;
	private NetworkViewSelectionHandler networkSelectionHandler;
	
	public NetworkBNMainPane() {
		super();
	}
	
	@Override
	protected BooleanNetwork getBooleanNetwork(List<String> targets) {
		BooleanNetwork bn = convertNetworkToBN(networkView);
		this.booleanNetwork = bn;
		return bn;
	}
	
	@Override
	protected BooleanNetworkSamplePane createSamplePane() {
		return new TFPathwaySamplePane();
	}

	@Override
	protected boolean ensureSimulationInSameObject(BooleanNetworkSamplePane pane1, 
	                                               BooleanNetworkSamplePane pane2) {
		// These simulations should be performed in the same network.
		return true;
	}

	@Override
	protected void displayComparison(SimulationTableModel sim1,
	                                 SimulationTableModel sim2,
	                                 PathwayHighlightControlPanel hilitePane) {
		NetworkComparisonPane comparisonPane = new NetworkComparisonPane(sim1.getSimulationName() + " vs. " + sim2.getSimulationName());
        comparisonPane.setNetworkView(networkView);
		CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(comparisonPane.getCytoPanelName());
        int index = cytoPanel.indexOfComponent(comparisonPane);
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
        comparisonPane.setSimulations(sim1, sim2);
	}

	private BooleanNetwork convertNetworkToBN(CyNetworkView view) {
		// Use to get nodes and edges information
		TableHelper tableHelper = new TableHelper();
		// Convert nodes into variables
		List<CyNode> nodeList = view.getModel().getNodeList();
		Map<CyNode, BooleanVariable> node2var = new HashMap<>();
		for (CyNode node : nodeList) {
			String name = tableHelper.getStoredNodeAttribute(view.getModel(),
					node,
					"name",
					String.class);
			BooleanVariable var = new BooleanVariable();
			var.setName(name);
			node2var.put(node, var);
		}
		List<CyEdge> edgeList = view.getModel().getEdgeList();
		BooleanNetwork bn = new BooleanNetwork();
		String networkName = tableHelper.getStoredNetworkAttribute(view.getModel(),
				"name",
				String.class);
		bn.setName(networkName);
		for (CyEdge edge : edgeList ) {
			BooleanRelation relation = convertEdgeToRelation(edge, node2var, view, tableHelper);
			bn.addRelation(relation);
		}
		bn.validateVariables();
		// This is a hack so that it can work with downstream analysis
		bn.getVariables().stream().forEach(var -> var.addProperty("name", var.getName()));
		//System.out.println(bn);
		return bn;
	}
	
	private BooleanRelation convertEdgeToRelation(CyEdge edge,
	                                              Map<CyNode, BooleanVariable> node2var,
	                                              CyNetworkView view,
	                                              TableHelper tableHelper) {
		CyNode source = edge.getSource();
		BooleanVariable sourceVar = node2var.get(source);
		CyNode target = edge.getTarget();
		BooleanVariable targetVar = node2var.get(target);
		BooleanRelation relation = new BooleanRelation();
		// Set up transfer function based on color and value
		String color = tableHelper.getStoredEdgeAttribute(view.getModel(),
				edge, 
				"color", 
				String.class);
		Double value = tableHelper.getStoredEdgeAttribute(view.getModel(),
				edge,
				"value", 
				Double.class);
		boolean isNegated = false;
		if (("#A52A2A").equals(color) || ("#FF0000").equals(color)) // Inhibition for TF/TF and TF/pathway 
			isNegated = true;
		relation.addInputVariable(sourceVar, isNegated);
		relation.setOutputVariable(targetVar);
		TransferFunction func = createTransferFunction(color, value);
		relation.setTransferFunction(func);
		return relation;
	}

	private TransferFunction createTransferFunction(String color, Double value) {
		if (value == null) // Parent-child pathway relationship or TF annotated in a pathway
			return new IdentityFunction(); // Use this for the time being
		HillFunction func = new HillFunction();
		func.setParameter_g(value);
		return func;
	}
	
	public BooleanNetwork getBooleanNetwork() {
		return this.booleanNetwork;
	}
	
	public void setNetworkView(CyNetworkView view) {
		this.networkView = view;
		// Just in case
		if (networkSelectionRegistration != null)
			networkSelectionRegistration.unregister();
		// Do a reset
		SelectionMediator selectionMediator = PlugInObjectManager.getManager().getObservationVarSelectionMediator();
		if (selectionMediator.getSelectables() != null)
			selectionMediator.getSelectables().clear();
		if (networkSelectionHandler == null) {
			networkSelectionHandler = new NetworkViewSelectionHandler();
			selectionMediator.addSelectable(networkSelectionHandler);
		}
		networkSelectionHandler.setNetworkView(view);
		
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        RowsSetListener listener = new RowsSetListener() {
            
            @Override
            public void handleEvent(RowsSetEvent e) {
            	if(!e.containsColumn(CyNetwork.SELECTED))
                    return;
                if (networkView == null || networkView.getModel() == null)
                    return;
                selectionMediator.fireSelectionEvent(networkSelectionHandler);
            }
        };
        networkSelectionRegistration = context.registerService(RowsSetListener.class.getName(),
                                listener,
                                null);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void close() {
		super.close();
		if (networkSelectionHandler != null) {
			List<Selectable> list = PlugInObjectManager.getManager().getObservationVarSelectionMediator().getSelectables();
			if (list != null)
				list.remove(networkSelectionHandler);
		}
		if (networkSelectionRegistration != null)
			networkSelectionRegistration.unregister();
	}
	
	private class TFPathwaySamplePane extends BooleanNetworkSamplePane {

		@Override
		protected void displayTimeCourse(List<BooleanVariable> variables) {
			BooleanNetwork bn = getBooleanNetwork();
			if (bn == null)
				return;
			NetworkTimeCoursePane timeCoursePane = new NetworkTimeCoursePane("BN: " + sampleName);
			timeCoursePane.setNetworkView(networkView);
			CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(timeCoursePane.getCytoPanelName());
			int index = cytoPanel.indexOfComponent(timeCoursePane);
			if (index > -1)
				cytoPanel.setSelectedIndex(index);
			// Not supported yet.
			// timeCoursePane.setHiliteControlPane(hiliteControlPane);
			timeCoursePane.setSimulationResults(new ArrayList<>(bn.getVariables()));
		}

		@Override
		protected void enableSelectionSync() {
	        sampleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	            
	            @Override
	            public void valueChanged(ListSelectionEvent e) {
	                handleTableSelection();
	            }
	        });
	        
	        selectionHandler = new GeneLevelSelectionHandler();
	        ((GeneLevelSelectionHandler)selectionHandler).setGeneLevelTable(sampleTable);
	        SelectionMediator mediator = PlugInObjectManager.getManager().getObservationVarSelectionMediator();
	        mediator.addSelectable(selectionHandler);
		}
		
		@Override
	    protected void handleTableSelection() {
	        SelectionMediator mediator = PlugInObjectManager.getManager().getObservationVarSelectionMediator();
	        mediator.fireSelectionEvent(selectionHandler);
	    }
		
		
	}
	
}