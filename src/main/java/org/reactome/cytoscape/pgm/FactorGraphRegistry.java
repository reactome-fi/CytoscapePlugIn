/*
 * Created on Mar 5, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Observation;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;


/**
 * A singleton class that is used to manage FactorGraph related objects.
 * @author gwu
 *
 */
public class FactorGraphRegistry {
    private static FactorGraphRegistry registry;
    // Register converted FactorGraph from a PathwayDiagram
    private Map<CyNetwork, FactorGraph> networkToFg;
    // Register inference results
    private Map<FactorGraph, FactorGraphInferenceResults> fgToResults;
    // For observations
    private Map<FactorGraph, List<Observation>> fgToObservations;
    private Map<FactorGraph, List<Observation>> fgToRandomObservations;
    
    public static final FactorGraphRegistry getRegistry() {
        if (registry == null)
            registry = new FactorGraphRegistry();
        return registry;
    }
    
    /**
     * Default constructor.
     */
    private FactorGraphRegistry() {
        networkToFg = new HashMap<CyNetwork, FactorGraph>();
        fgToResults = new HashMap<FactorGraph, FactorGraphInferenceResults>();
        fgToObservations = new HashMap<FactorGraph, List<Observation>>();
        fgToRandomObservations = new HashMap<FactorGraph, List<Observation>>();
        // If a network is deleted, the cached factor graph should be 
        // removed automatically. Use the following listener, instead of
        // NetworkDestroyedListener so that the Network to be destroyed can be
        // queried directly.
        NetworkAboutToBeDestroyedListener listener = new NetworkAboutToBeDestroyedListener() {
            
            @Override
            public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
                CyNetwork network = e.getNetwork();
                FactorGraph fg = networkToFg.get(network);
                networkToFg.remove(network);
                fgToResults.remove(fg); // Remove them in order to keep the usage of memory as minimum
                fgToObservations.remove(fg);
                fgToRandomObservations.remove(fg);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(NetworkAboutToBeDestroyedListener.class.getName(),
                                listener,
                                null);
        // This should be called only once in the whole session
        PathwayPGMConfiguration config = PathwayPGMConfiguration.getConfig();
        try {
            config.config(getClass().getResourceAsStream("PGM_Pathway_Config.xml"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void registerNetworkToFactorGraph(CyNetwork network, FactorGraph pfg) {
        networkToFg.put(network, pfg);
    }
    
    public FactorGraph getFactorGraph(CyNetwork network) {
        return networkToFg.get(network);
    }
    
    /**
     * Get a saved FactorGraphInferenceResults. If nothing is registered,
     * create a new object and return it.
     * @param factorGraph
     * @return
     */
    public FactorGraphInferenceResults getInferenceResults(FactorGraph factorGraph) {
        FactorGraphInferenceResults fgResults = fgToResults.get(factorGraph);
        if (fgResults == null) {
            fgResults = new FactorGraphInferenceResults();
            fgResults.setFactorGraph(factorGraph);
            fgToResults.put(factorGraph, fgResults);
        }
        return fgResults;
    }
    
    public void setObservations(FactorGraph fg, List<Observation> observations) {
        fgToObservations.put(fg, observations);
    }
    
    public List<Observation> getObservations(FactorGraph fg) {
        return fgToObservations.get(fg);
    }
    
    public void setRandomObservations(FactorGraph fg, List<Observation> observations) {
        fgToRandomObservations.put(fg, observations);
    }
    
    public List<Observation> getRandomObservations(FactorGraph fg) {
        return fgToRandomObservations.get(fg);
    }
}
