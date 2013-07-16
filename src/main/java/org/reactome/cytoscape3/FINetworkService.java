/*
 * Created on Jul 20, 2010
 *
 */
package org.reactome.cytoscape3;

import java.io.IOException;
import java.util.Set;

public interface FINetworkService
{

    public Integer getNetworkBuildSizeCutoff() throws Exception;

    public Set<String> buildFINetwork(Set<String> selectedGenes,
            boolean useLinkers) throws Exception;

    public Set<String> queryAllFIs() throws IOException;
}
