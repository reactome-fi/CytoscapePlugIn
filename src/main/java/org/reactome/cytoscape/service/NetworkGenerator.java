package org.reactome.cytoscape.service;

import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

public interface NetworkGenerator
{
    public abstract CyNetwork constructFINetwork(Set<String> allNodes, Set<String> fis);
    
    public abstract void addFIs(Set<String> fis, CyNetworkView view);
    
    public abstract void addFIPartners(String target, Set<String> partners, CyNetworkView view);

}
