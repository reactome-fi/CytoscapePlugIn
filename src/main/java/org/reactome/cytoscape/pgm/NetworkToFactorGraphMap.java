/*
 * Created on Mar 5, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.pgm.PGMFactorGraph;

/**
 * This class is used to create a map from a displayed network object to the original
 * factor graph converted from a Reactome pathway diagram.
 * @author gwu
 *
 */
public class NetworkToFactorGraphMap {
    private static NetworkToFactorGraphMap map;
    
    private Map<CyNetwork, PGMFactorGraph> networkToFg;
    
    public static final NetworkToFactorGraphMap getMap() {
        if (map == null)
            map = new NetworkToFactorGraphMap();
        return map;
    }
    
    /**
     * Default constructor.
     */
    private NetworkToFactorGraphMap() {
        networkToFg = new HashMap<CyNetwork, PGMFactorGraph>();
        // If a network is deleted, the cached factor graph should be 
        // removed automatically. Use the following listener, instead of
        // NetworkDestroyedListener so that the Network to be destroyed can be
        // queried directly.
        NetworkAboutToBeDestroyedListener listener = new NetworkAboutToBeDestroyedListener() {
            
            @Override
            public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
                CyNetwork network = e.getNetwork();
                networkToFg.remove(network);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(NetworkAboutToBeDestroyedListener.class.getName(),
                                listener,
                                null);
    }
    
    public void put(CyNetwork network, PGMFactorGraph pfg) {
        networkToFg.put(network, pfg);
    }
    
    public PGMFactorGraph get(CyNetwork network) {
        return networkToFg.get(network);
    }
    
}
