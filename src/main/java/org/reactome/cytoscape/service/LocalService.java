/*
 * Created on Jul 20, 2010
 *
 */
package org.reactome.cytoscape.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.graph.GeneClusterPair;
import org.reactome.r3.graph.NetworkBuilderForGeneSet;
import org.reactome.r3.graph.NetworkClusterResult;
import org.reactome.r3.graph.SpectralPartitionNetworkCluster;

/**
 * This FINetworkService was designed to perform a local version of constructing
 * a FI network. Because of the slowness of using linkers, this implementation 
 * fetch all FIs first and then perform a network construction. Otherwise, it uses
 * the server to construct a FI network to avoid downloading a big FI network file,
 * which is a little bit shy of 10 MB.
 * 
 * @author wgm ported July 2013 by Eric T Dawson
 */
public class LocalService implements FINetworkService {
    // Cache some fetched information for performance reasons.
    // Don't cache anything in case the FI network version has
    // changed during a previous session.
    //private Set<String> allFIs;

    public LocalService() {
    }

    @Override
    public Integer getNetworkBuildSizeCutoff() throws Exception {
        // There is no limit to network size yet.
        return Integer.MAX_VALUE;
    }

    @Override
    public Set<String> queryAllFIs() throws IOException {
        RESTFulFIService restService = new RESTFulFIService();
        Set<String> allFIs = restService.queryAllFIs();
        return allFIs;
    }
    
    @Override
    public Set<String> buildFINetwork(Set<String> selectedGenes,
                                      boolean useLinker) throws Exception {
        Set<String> fis;
        if (useLinker) {
            Set<String> allFIs = queryAllFIs();
            NetworkBuilderForGeneSet networkBuilder = new NetworkBuilderForGeneSet();
            networkBuilder.setAllFIs(allFIs);
            fis = networkBuilder.constructFINetworkForGeneSet(selectedGenes,
                                                              null);
        }
        else {
            RESTFulFIService restfulSerive = new RESTFulFIService();
            fis = restfulSerive.buildFINetwork(selectedGenes, false);
        }
        return fis;
    }
    
    /**
     * This method is copied from the server-side code for clients.
     * @param queryFIs
     * @return
     */
    public NetworkClusterResult cluster(List<CyEdge> edges,
                                        CyNetworkView view) throws Exception {
        Set<String> fis = PlugInUtilities.convertEdgesToFIs(edges, view);
        SpectralPartitionNetworkCluster clusterEngine = new SpectralPartitionNetworkCluster();
        List<Set<String>> clusters = clusterEngine.cluster(fis);
        List<GeneClusterPair> geneClusterPairs = new ArrayList<GeneClusterPair>();
        for (int i = 0; i < clusters.size(); i++) {
            for (String gene : clusters.get(i)) {
                GeneClusterPair pair = new GeneClusterPair();
                pair.setGeneId(gene);
                pair.setCluster(i);
                geneClusterPairs.add(pair);
            }
        }
        double modularity = clusterEngine.calculateModualarity(clusters,
                                                               fis);
        NetworkClusterResult rtn = new NetworkClusterResult();
        rtn.setClsName(clusterEngine.getClass().getName());
        rtn.setModularity(modularity);
        rtn.setGeneClusterPairs(geneClusterPairs);
        return rtn;
    }
    
}
