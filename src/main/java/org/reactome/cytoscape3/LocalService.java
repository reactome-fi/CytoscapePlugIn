/*
 * Created on Jul 20, 2010
 *
 */
package org.reactome.cytoscape3;

import java.io.IOException;
import java.util.Set;

import org.reactome.r3.graph.NetworkBuilderForGeneSet;
import org.reactome.r3.util.InteractionUtilities;

/**
 * This class is to create some local service to reduce the burden in the server side
 * to improve the performance. This is a singleton. The use of this class is bound to
 * PlugInScoreObjectManager, which is used to control a dynamic loading of this class, and
 * make sure it is a singleton.
 * Note: currently only constructing FI network with linker genes using local service since 
 * this step is too slow.
 * @author wgm
 *
 */
public class LocalService implements FINetworkService {
    // Cache some fetched information for performance reason.
    // Don't cache anything in case the FI network versio has been changed during different running
    private Set<String> allFIs;
    
    public LocalService() {
    }
    
    public Integer getNetworkBuildSizeCutoff() throws Exception {
        // No limit
        return Integer.MAX_VALUE;
    }

    public Set<String> queryAllFIs() throws IOException {
        RESTFulFIService restService = new RESTFulFIService();
        Set<String> allFIs = restService.queryAllFIs();
        return allFIs;
    }
    
    public Set<String> buildFINetwork(Set<String> selectedGenes,
                                      boolean useLinker) throws Exception {
        Set<String> fis;
        Set<String> allFIs = queryAllFIs();
        if (useLinker) {
            NetworkBuilderForGeneSet networkBuilder = new NetworkBuilderForGeneSet();
            networkBuilder.setAllFIs(allFIs);
            fis = networkBuilder.constructFINetworkForGeneSet(selectedGenes,
                                                              null);
        }
        else {
            fis = InteractionUtilities.getFIs(selectedGenes, allFIs);
        }
        return fis;
    }
}
