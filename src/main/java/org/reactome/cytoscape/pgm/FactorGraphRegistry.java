/*
 * Created on Mar 5, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.gk.render.RenderablePathway;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.ObservationFileLoader;
import org.reactome.factorgraph.common.ObservationFileLoader.ObservationData;
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
    // Map from a pathway diagram to a FactorGraph
    private Map<RenderablePathway, FactorGraph> diagramToFg;
    // Register inference results
    private Map<FactorGraph, FactorGraphInferenceResults> fgToResults;
    // For observations
    private Map<FactorGraph, List<Observation>> fgToObservations;
    private Map<FactorGraph, List<Observation>> fgToRandomObservations;
    // Cache loaded data
    // Key is a concatenated string based on file name and threshold values
    // Most likely such a key should be unique.
    private Map<String, ObservationData> keyToLoadedData;
    private File sampleInfoFile; // The above map should be loaded from this file
    private List<Inferencer> loadedInferencer;
    // Cache escape names to avoid displaying
    private boolean needEscapeNameDialog = true;
    private String escapeNames;
    
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
        diagramToFg = new HashMap<RenderablePathway, FactorGraph>();
        fgToResults = new HashMap<FactorGraph, FactorGraphInferenceResults>();
        fgToObservations = new HashMap<FactorGraph, List<Observation>>();
        fgToRandomObservations = new HashMap<FactorGraph, List<Observation>>();
        keyToLoadedData = new HashMap<String, ObservationFileLoader.ObservationData>();
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

    public boolean isNeedEscapeNameDialog() {
        return needEscapeNameDialog;
    }

    public void setNeedEscapeNameDialog(boolean needEscapeNameDialog) {
        this.needEscapeNameDialog = needEscapeNameDialog;
    }

    public String getEscapeNames() {
        return escapeNames;
    }

    public void setEscapeNames(String escapeNames) {
        this.escapeNames = escapeNames;
    }

    public File getTwoCaseSampleInfoFile() {
        return sampleInfoFile;
    }

    public void setTwoCasesSampleInfoFile(File sampleInfoFile) {
        this.sampleInfoFile = sampleInfoFile;
    }
    
    /**
     * Call this method to check if data and algorithm have been set.
     * @return
     */
    public boolean isDataLoaded() {
        return keyToLoadedData.size() > 0 && loadedInferencer != null && loadedInferencer.size() > 0;
    }
    
    public void cacheLoadedData(File file,
                                double[] threshold,
                                ObservationData data) {
        // To save memory, we will cache one data for one DataType only
        for (Iterator<String> it = keyToLoadedData.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            ObservationData tmp = keyToLoadedData.get(key);
            if (tmp.getDataType() == data.getDataType())
                it.remove();
        }
        String key = generateKeyForData(file, threshold);
        keyToLoadedData.put(key, data);
    }
    
    public ObservationData getLoadedData(File file,
                                         double[] threshold) {
        String key = generateKeyForData(file, threshold);
        return keyToLoadedData.get(key);
    }
    
    public List<ObservationData> getAllLoadedData() {
        return new ArrayList<ObservationData>(keyToLoadedData.values());
    }
    
    /**
     * Get the file for cached data.
     * @param dataType
     * @return
     */
    public String getLoadedDataFileName(DataType dataType) {
        for (Iterator<String> it = keyToLoadedData.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            ObservationData tmp = keyToLoadedData.get(key);
            if (tmp.getDataType() == dataType) {
                String[] tokens = key.split("::");
                return tokens[0];
            }
        }
        return null;
    }
    
    /**
     * Get the file for cached data.
     * @param dataType
     * @return
     */
    public double[] getLoadedThresholds(DataType dataType) {
        double[] rtn = null;
        for (Iterator<String> it = keyToLoadedData.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            ObservationData tmp = keyToLoadedData.get(key);
            if (tmp.getDataType() == dataType) {
                String[] tokens = key.split("::");
                rtn = new double[tokens.length - 1];
                for (int i = 1; i < tokens.length; i++)
                    rtn[i - 1] = new Double(tokens[i]);
            }
        }
        return rtn;
    }
    
    private String generateKeyForData(File file, double[] threshold) {
        StringBuilder builder = new StringBuilder();
        builder.append(file.getAbsolutePath());
        for (double value : threshold) {
            builder.append("::").append(value);
        }
        return builder.toString();
    }

    public List<Inferencer> getLoadedAlgorithms() {
        return loadedInferencer;
    }

    public void setLoadedAlgorithms(List<Inferencer> loadedInferencer) {
        this.loadedInferencer = loadedInferencer;
    }

    public void registerNetworkToFactorGraph(CyNetwork network, FactorGraph pfg) {
        networkToFg.put(network, pfg);
    }
    
    public void registerDiagramToFactorGraph(RenderablePathway diagram, FactorGraph fg) {
        diagramToFg.put(diagram, fg);
    }
    
    public void unregisterDiagramToFactorGraph(RenderablePathway diagram) {
        diagramToFg.remove(diagram);
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
    
    /**
     * Get the inference results for a RenderablePathway object. If nothing is done for this
     * diagram, a null will be returned.
     * @param diagram
     * @return
     */
    public FactorGraphInferenceResults getInferenceResults(RenderablePathway diagram) {
        FactorGraph fg = diagramToFg.get(diagram);
        if (fg == null)
            return null;
        FactorGraphInferenceResults fgResults = fgToResults.get(fg);
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
