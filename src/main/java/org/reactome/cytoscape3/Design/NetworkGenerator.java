package org.reactome.cytoscape3.Design;

import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;

public interface NetworkGenerator
{
    public abstract void constructFINetwork(Set<String> allNodes, Set<String> fis);
    
    public abstract void addFIs(Set<String> fis, CyNetworkView view);
    
    public abstract void addFIPartners(String target, Set<String> partners, CyNetworkView view);

}
