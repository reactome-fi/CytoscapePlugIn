/*
 * Created on Jan 21, 2017
 *
 */
package org.reactome.cytoscape.drug;

import org.cytoscape.view.model.CyNetworkView;

/**
 * @author gwu
 *
 */
public class NetworkInteractionFilter extends InteractionFilter {
    private CyNetworkView networkView;
    
    /**
     * Default constructor.
     */
    public NetworkInteractionFilter() {
    }
    
    public void setNetworkView(CyNetworkView networkView) {
        this.networkView = networkView;
    }

    @Override
    public void applyFilter() {
        if (networkView == null)
            return; // Do nothing
        NetworkDrugManager.getManager().applyFilter(networkView);
    }
    
}
