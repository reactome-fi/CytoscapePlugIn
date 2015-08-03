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

import javax.swing.JOptionPane;

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
    // Use diagram id, instead of object for cases a pathway diagram is re-loaded
    private Map<Long, FactorGraph> diagramIdToFg;
    // Register inference results
    private Map<FactorGraph, FactorGraphInferenceResults> fgToResults;
    // For observations
    private Map<FactorGraph, List<Observation<Number>>> fgToObservations;
    private Map<FactorGraph, List<Observation<Number>>> fgToRandomObservations;
    // Cache loaded data
    // Key is a concatenated string based on file name and threshold values
    // Most likely such a key should be unique.
    private Map<String, ObservationData> keyToLoadedData;
    // For random data: there should be only one value at any time
    private Map<String, List<ObservationData>> keyToRandomData;
    private File sampleInfoFile; // The above map should be loaded from this file
    private List<Inferencer> loadedInferencer;
    // Cache escape names to avoid displaying
    private boolean needEscapeNameDialog = true;
    private String escapeNames;
    // Number of permutation
    private Integer numberOfPermtation = 100; // Default is 100
    // To control if a feature warning is needed
    private boolean needFeatureWarning = true;
    
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
        diagramIdToFg = new HashMap<Long, FactorGraph>();
        fgToResults = new HashMap<FactorGraph, FactorGraphInferenceResults>();
        fgToObservations = new HashMap<FactorGraph, List<Observation<Number>>>();
        fgToRandomObservations = new HashMap<FactorGraph, List<Observation<Number>>>();
        keyToLoadedData = new HashMap<String, ObservationFileLoader.ObservationData>();
        keyToRandomData = new HashMap<String, List<ObservationData>>();
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
    
    public Integer getNumberOfPermtation() {
        return numberOfPermtation;
    }

    public void setNumberOfPermtation(Integer numberOfPermtation) {
        this.numberOfPermtation = numberOfPermtation;
    }

    /**
     * Remove all saved data for the passed FactorGraph object.
     * @param fg
     */
    public void clearData(FactorGraph fg) {
        fgToObservations.remove(fg);
        fgToRandomObservations.remove(fg);
        fgToResults.remove(fg);
    }

    public boolean isNeedEscapeNameDialog() {
        return needEscapeNameDialog;
    }

    public void setNeedEscapeNameDialog(boolean needEscapeNameDialog) {
        this.needEscapeNameDialog = needEscapeNameDialog;
    }

    public String getEscapeNames() {
        // escapeNames should not be null 
        if (escapeNames == null)
            escapeNames = PlugInObjectManager.getManager().getProperties().getProperty("fgEscapeNames");
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
        diagramIdToFg.put(getDiagramId(diagram), 
                          fg);
    }
    
    public void registerInferenceResults(FactorGraphInferenceResults fgResults) {
        if (fgToResults == null)
            fgToResults = new HashMap<FactorGraph, FactorGraphInferenceResults>();
        fgToResults.put(fgResults.getFactorGraph(), fgResults);
    }
    
    public void cleanUpCache(RenderablePathway diagram) {
        Long id = getDiagramId(diagram);
        FactorGraph fg = diagramIdToFg.get(id);
        if (fg == null)
            return;
        diagramIdToFg.remove(id);
        fgToObservations.remove(fg);
        fgToRandomObservations.remove(fg);
        fgToResults.remove(fg);
    }
    
    private Long getDiagramId(RenderablePathway diagram) {
        if (diagram.getReactomeDiagramId() != null)
            return diagram.getReactomeDiagramId();
        return diagram.getReactomeId();
    }
    
    public FactorGraph getFactorGraph(CyNetwork network) {
        return networkToFg.get(network);
    }
    
    public FactorGraph getFactorGraph(RenderablePathway diagram) {
        return diagramIdToFg.get(getDiagramId(diagram));
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
        FactorGraph fg = diagramIdToFg.get(getDiagramId(diagram));
        if (fg == null)
            return null;
        FactorGraphInferenceResults fgResults = fgToResults.get(fg);
        return fgResults;
    }
    
    public void setObservations(FactorGraph fg, List<Observation<Number>> observations) {
        fgToObservations.put(fg, observations);
    }
    
    public List<Observation<Number>> getObservations(FactorGraph fg) {
        return fgToObservations.get(fg);
    }
    
    public void setRandomObservations(FactorGraph fg, List<Observation<Number>> observations) {
        fgToRandomObservations.put(fg, observations);
    }
    
    public List<Observation<Number>> getRandomObservations(FactorGraph fg) {
        return fgToRandomObservations.get(fg);
    }
    
    public void cacheRandomData(List<ObservationData> randomData,
                                File dnaFile,
                                double[] dnaThresholdValues,
                                File geneExpFile,
                                double[] geneExpThresholdValues) {
        String key = generateKeyForRandomData(dnaFile,
                                              dnaThresholdValues,
                                              geneExpFile,
                                              geneExpThresholdValues);
        keyToRandomData.clear(); // Make sure only one random data set is cached to control the use of memory
        keyToRandomData.put(key, randomData);
    }
    
    public List<ObservationData> getRandomData(File dnaFile,
                                               double[] dnaThresholdValues,
                                               File geneExpFile,
                                               double[] geneExpThresholdValues) {
        if (dnaFile == null && geneExpFile == null) {
            // Return anything cached
            if (keyToRandomData.size() > 0)
                return keyToRandomData.values().iterator().next();
            return null;
        }
        String key = generateKeyForRandomData(dnaFile, dnaThresholdValues, geneExpFile, geneExpThresholdValues);
        return keyToRandomData.get(key);
    }

    private String generateKeyForRandomData(File dnaFile,
                                            double[] dnaThresholdValues,
                                            File geneExpFile,
                                            double[] geneExpThresholdValues) {
        StringBuilder keyBuilder = new StringBuilder();
        if (dnaFile != null && dnaThresholdValues != null) {
            String key1 = generateKeyForData(dnaFile, dnaThresholdValues);
            keyBuilder.append(key1);
        }
        if (geneExpFile != null && geneExpThresholdValues != null) {
            String key2 = generateKeyForData(geneExpFile, geneExpThresholdValues);
            if (keyBuilder.length() > 0)
                keyBuilder.append("::");
            keyBuilder.append(key2);
        }
        // The number of permutations may be changed. So we need to keep this information too.
        keyBuilder.append("::").append(FactorGraphRegistry.getRegistry().getNumberOfPermtation());
        return keyBuilder.toString();
    }
    
    /**
     * Display a warning dialog. If the user choose cancel button, false will be returned. If data has been loaded
     * before, this dialog will not be displayed, assuming the user has already made a choice.
     */
    public boolean showFeatureWarningDialog() {
        if (!needFeatureWarning)
            return true; // The user has chosen true already.
        // Show a warning
        // Use the JFrame so that the position is the same as other dialog
        int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                  "Features related to probabilistic graphical models are still experimental,\n"
                                                          + "and will be changed in the future. Please use inferred results with \n"
                                                          + "caution. Do you still want to continue?",
                                                          "Experimental Feature Warning",
                                                          JOptionPane.OK_CANCEL_OPTION,
                                                          JOptionPane.WARNING_MESSAGE);
        if (reply == JOptionPane.CANCEL_OPTION)
            return false;
        needFeatureWarning = false;
        return true;
    }
}
