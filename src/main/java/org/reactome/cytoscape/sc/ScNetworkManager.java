package org.reactome.cytoscape.sc;

import static org.reactome.cytoscape.service.PathwaySpecies.Homo_sapiens;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellClusterNetwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JOptionPane;

import org.apache.commons.math3.util.Pair;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.pathway.PathwayEnrichmentAnalysisTask;
import org.reactome.cytoscape.pathway.PathwayHierarchyLoadTask;
import org.reactome.cytoscape.sc.diff.DiffExpResult;
import org.reactome.cytoscape.sc.diff.DiffGeneNetworkBuilder;
import org.reactome.cytoscape.sc.diff.DiffGeneNetworkStyle;
import org.reactome.cytoscape.service.PathwayEnrichmentApproach;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.io.JsonEOFException;

import smile.math.MathEx;

/**
 * A singleton to handle single cell network related data process.
 * @author wug
 *
 */
@SuppressWarnings("rawtypes")
public class ScNetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(ScNetworkManager.class);
    private static ScNetworkManager manager;
    private SCNetworkVisualStyle scStyle;
    private CellClusterVisualStyle clusterStyle;
    private DiffGeneNetworkStyle diffGeneStyle;
    private JSONServerCaller serverCaller;
    private TableHelper tableHelper;
    // Cached map for quick performance
    private Map<String, Set<String>> mouse2humanMap;
    // Currently selected species: Default to human
    private PathwaySpecies species = Homo_sapiens;

    private ScNetworkManager() {
        serverCaller = new JSONServerCaller();
        tableHelper = new TableHelper();
    }

    public static ScNetworkManager getManager() {
        if (manager == null)
            manager = new ScNetworkManager();
        return manager;
    }
    
    public JSONServerCaller getServerCaller() {
        return serverCaller;
    }
    
    public PathwaySpecies getSpecies() {
        return species;
    }

    public void setSpecies(PathwaySpecies species) {
        this.species = species;
    }

    public Boolean isEdgeDisplayed() {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        if (view == null)
            return null; // Do nothing
        // Check if edges are shown
        Collection<View<CyEdge>> edgeViews = view.getEdgeViews();
        if (edgeViews == null || edgeViews.size() == 0)
            return null;
        // Pick up for checking the status
        View<CyEdge> edgeView = edgeViews.stream().findAny().get();
        Boolean isVisible = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE);
        return isVisible;
    }
    
    public void setEdgesVisible(Boolean isVisible) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        if (view == null)
            return ; // Do nothing
        setEdgesVisible(isVisible, view);
    }

    public void setEdgesVisible(Boolean isVisible, CyNetworkView view) {
        // Check if edges are shown
        Collection<View<CyEdge>> edgeViews = view.getEdgeViews();
        if (edgeViews == null || edgeViews.size() == 0)
            return;
        if (isVisible)
            PlugInUtilities.showAllEdges(view);
        else 
            edgeViews.forEach(v -> PlugInUtilities.hideEdge(v));
        view.updateView();
    }

    public void loadGeneExp(String gene) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        if (view == null)
            return ; // Do nothing
        try {
            List<Double> values = serverCaller.getGeneExp(gene);
            _loadValues(view, gene, values);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          e.getMessage(),
                                          "Error in Getting Gene Expression",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
        }
    }

    private void _loadValues(CyNetworkView view, 
                             String feature,
                             List<?> values) throws JsonEOFException, IOException {
        List<String> cellIds = serverCaller.getCellIds();
        Map<String, Object> idToValue = IntStream.range(0, cellIds.size())
                .boxed()
                .collect(Collectors.toMap(cellIds::get, values::get));
        ReactomeNetworkType type = tableHelper.getReactomeNetworkType(view.getModel());
        // If the view is for clusters, we need median values.
        if (type == SingleCellClusterNetwork) {
            // Basic requirement for calculating median
            if (!(values.get(0) instanceof Comparable))
                return;
            calculateClusterMedians(cellIds, idToValue);
        }
        tableHelper.storeNodeAttributesByName(view, feature, idToValue);
        getStyle(view).updateNodeColors(view, feature, values.get(0).getClass());
    }

    private void calculateClusterMedians(List<String> cellIds, Map<String, Object> idToValue) throws JsonEOFException, IOException {
        List<Integer> clusters = serverCaller.getCluster();
        Map<Integer, List<Comparable>> clusterToValues = new HashMap<>();
        for (int i = 0; i < clusters.size(); i++) {
            Integer cluster = clusters.get(i);
            Comparable value = (Comparable) idToValue.get(cellIds.get(i));
            clusterToValues.compute(cluster, (key, list) -> {
                if (list == null)
                    list = new ArrayList<>();
                list.add(value);
                return list;
            });
        }
        idToValue.clear();
        for (Integer cluster : clusterToValues.keySet()) {
            String id = SCNetworkVisualStyle.CLUSTER_NODE_PREFIX + cluster;
            List<Comparable> clusterValues = clusterToValues.get(cluster);
            @SuppressWarnings("unchecked")
            Comparable median = MathEx.median(clusterValues.toArray(new Comparable[] {}));
            idToValue.put(id, median);
        }
    }
    
    public void performDPT(String rootCell) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        if (view == null)
            return ; // Do nothing
        try {
            List<Double> dpt = serverCaller.performDPT(rootCell);
            _loadValues(view, SCNetworkVisualStyle.DPT_NAME, dpt);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          e.getMessage(),
                                          "Error in Diffusion Pseudotime Analysis",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
        }
    }
    
    public void loadCellFeature(String featureName) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        if (view == null)
            return ; // Do nothing
        try {
            List<Object> features = serverCaller.getCellFeature(featureName);
            _loadValues(view, featureName, features);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          e.getMessage(),
                                          "Error in Cell Feature",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
        }
    }
    
    public List<String> getCellFeatureNames() {
        try {
            List<String> features = serverCaller.getCellFeatureNames();
            return features;
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          e.getMessage(),
                                          "Error in Getting Cell Features",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
            return null;
        }
    }
    
    private SCNetworkVisualStyle getStyle(CyNetworkView view) {
        ReactomeNetworkType type = tableHelper.getReactomeNetworkType(view.getModel());
        if (type == SingleCellClusterNetwork)
            return getClusterStyle();
        else
            return getScStyle();
    }

    public SCNetworkVisualStyle getScStyle() {
        if (scStyle == null)
            scStyle = new SCNetworkVisualStyle();
        return scStyle;
    }
    
    public SCNetworkVisualStyle getClusterStyle() {
        if (clusterStyle == null)
            clusterStyle = new CellClusterVisualStyle();
        return clusterStyle;
    }
    
    public DiffGeneNetworkStyle getDiffGeneNetworkStyle() {
        if (diffGeneStyle == null)
            diffGeneStyle = new DiffGeneNetworkStyle();
        return diffGeneStyle;
    }
    
    public void doDiffExpAnalysis() {
        try {
            List<Integer> clusters = serverCaller.getCluster();
            List<Integer> distClusters = clusters.stream().distinct().sorted().collect(Collectors.toList());
            DifferentialExpressionAnalyzer helper = new DifferentialExpressionAnalyzer();
            Pair<String, String> selected = helper.getSelectedClusters(distClusters);
            if (selected == null)
                return;
            DiffExpResult result = serverCaller.doDiffGeneExpAnalysis(selected.getFirst(),
                                                                      selected.getSecond());
            if (result == null)
                return; // Just in case.
            result.setResultName(selected.getFirst() + " vs " + selected.getSecond());
            helper.displayResult(result);
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          e.getMessage(),
                                          "Error in Differential Expression Analysis",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
        }
    }
    
    public void buildFINetwork(DiffExpResult result) {
        if (result == null || result.getNames() == null || result.getNames().size() == 0)
            return; // Nothing to do
        try {
            if (mouse2humanMap == null)
                mouse2humanMap = new RESTFulFIService().getMouseToHumanGeneMap();
            DiffGeneNetworkBuilder networkBuilder = new DiffGeneNetworkBuilder();
            networkBuilder.setMouse2humanMap(mouse2humanMap);
            networkBuilder.setStyle(getDiffGeneNetworkStyle());
            networkBuilder.buildNetwork(result);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          e.getMessage(),
                                          "Error in Network Construction",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
        }
    }
    
    public void doPathwayAnalysis(DiffExpResult result,
                                  PathwayEnrichmentApproach approach) {
        if (result == null || result.getNames() == null || result.getNames().size() == 0 || approach == null)
            return;
        // Load the pathway hierarchy if it has not been.
        TaskManager tm = PlugInObjectManager.getManager().getTaskManager();
        if (tm == null)
            return;
        PathwayHierarchyLoadTask hierarchyTask = new PathwayHierarchyLoadTask();
        hierarchyTask.setSpecies(getSpecies());
        // Conduct a pathway enrichment analysis
        PathwayEnrichmentAnalysisTask analysisTask = new PathwayEnrichmentAnalysisTask();
        // This is just for test right now
        String geneList = result.getNames().stream().map(String::toUpperCase).collect(Collectors.joining("\n"));
        analysisTask.setGeneList(geneList);
        analysisTask.setEventPane(PathwayControlPanel.getInstance().getEventTreePane());
        tm.execute(new TaskIterator(hierarchyTask, analysisTask));
    }
    
}
