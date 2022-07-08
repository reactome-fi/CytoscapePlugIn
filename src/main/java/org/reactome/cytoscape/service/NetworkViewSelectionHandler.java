package org.reactome.cytoscape.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.Selectable;
import org.reactome.cytoscape.util.PlugInUtilities;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NetworkViewSelectionHandler implements Selectable {
	
	private CyNetworkView networkView;
	
	public NetworkViewSelectionHandler(CyNetworkView view) {
		setNetworkView(view);
	}
	
	public NetworkViewSelectionHandler() {
	}
	
	public void setNetworkView(CyNetworkView view) {
		this.networkView = view;
	}
    
    @Override
    public void setSelection(List selection) {
        TableHelper tableHelper = new TableHelper();
        tableHelper.selectNodes(networkView, 
                                "name",
                                new HashSet<String>(selection));
    }

    @Override
    public List getSelection() {
        Set<String> selectedGenes = PlugInUtilities.getSelectedGenesInNetwork(networkView.getModel());
        return new ArrayList<>(selectedGenes);
    }
}
