/*
 * Created on Jul 20, 2010
 *
 */
package org.reactome.cytoscape.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.r3.graph.NetworkClusterResult;

/**
 * An interface that is used to do FI related activities.
 * @author gwu
 *
 */
public interface FINetworkService {

    public Integer getNetworkBuildSizeCutoff() throws Exception;

    public Set<String> buildFINetwork(Set<String> selectedGenes,
            boolean useLinkers) throws Exception;

    public Set<String> queryAllFIs() throws IOException;
    
    /**
     * Perform a network clustering. 
     * @param edges
     * @param view
     * @return
     * @throws Exception
     */
    public NetworkClusterResult cluster(List<CyEdge> edges,
                                        CyNetworkView view) throws Exception;
}
