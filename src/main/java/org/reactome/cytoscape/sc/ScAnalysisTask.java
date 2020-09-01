package org.reactome.cytoscape.sc;

import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CELL_NUMBER_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CLUSTER_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CONNECTIVITY_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.EDGE_IS_DIRECTED;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellClusterNetwork;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellNetwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.service.FIAnalysisTask;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.r3.util.InteractionUtilities;

import com.fasterxml.jackson.core.io.JsonEOFException;

public class ScAnalysisTask extends FIAnalysisTask {
    //TODO: To be selected by the user
    private final double EDGE_WEIGHT_CUTOFF = 0.05d;
    
    private String file;
    private PathwaySpecies species;
    private String fileFormat;
    private List<String> regressoutKeys;
    private String imputationMethod;
    protected boolean isForRNAVelocity;
    private ScvVelocityMode velocityMode = ScvVelocityMode.stochastic;
    
    protected ScAnalysisTask() {
    }

    public ScAnalysisTask(String file,
                          PathwaySpecies species,
                          String fileFormat,
                          List<String> regressoutKeys,
                          String imputationMethod,
                          boolean isForRNAVelocity) {
        this.file = file;
        this.species = species;
        this.fileFormat = fileFormat;
        this.regressoutKeys = regressoutKeys;
        this.imputationMethod = imputationMethod;
        this.isForRNAVelocity = isForRNAVelocity;
    }
    
    public void setPathwaySpecies(PathwaySpecies species) {
        this.species = species;
    }
    
    public void setVelocityMode(ScvVelocityMode mode) {
        this.velocityMode = mode;
    }

    @Override
    protected void doAnalysis() {
        ScNetworkManager.getManager().setSpecies(species);
        ScNetworkManager.getManager().setForRNAVelocity(isForRNAVelocity);
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setTitle("Single Cell Data Analysis");
        JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        parentFrame.setGlassPane(progPane);
        progPane.setVisible(true);
        JSONServerCaller serverCaller = ScNetworkManager.getManager().getServerCaller();
        try {
            _doAnalysis(serverCaller, progPane, parentFrame);
        }
        catch(Exception e) {
            showErrorMessage(e.getMessage(), parentFrame);
            e.printStackTrace();
        }
        parentFrame.getGlassPane().setVisible(false);
    }

    protected void _doAnalysis(JSONServerCaller serverCaller, 
                               ProgressPane progPane, 
                               JFrame parentFrame) throws Exception {
        boolean success = false;
        if (isForRNAVelocity)
            success = velocityAnalysis(serverCaller, progPane, parentFrame);
        else
            success = standardAnalysis(serverCaller, progPane, parentFrame);
        if (!success)
            return;
        // Looks everything is fine. Let's build the network
        progPane.setText("Building cluster network...");
        buildClusterNetwork(serverCaller);
        progPane.setText("Building cell network...");
        buildCellNetwork(serverCaller);
    }
    
    private boolean velocityAnalysis(JSONServerCaller serverCaller,
                                     ProgressPane progPane,
                                     JFrame parentFrame) throws JsonEOFException, IOException {
        progPane.setText("Loading data...");
        String message = serverCaller.scvOpenData(file);
        if (!checkMessage(parentFrame, message))
            return false;
        progPane.setText("Preprocessing data...");
        message = serverCaller.scvPreprocessData();
        if (!checkMessage(parentFrame, message))
            return false;
        progPane.setText("Analyzing velocity...");
        message = serverCaller.scvVelocity(velocityMode);
        if (!checkMessage(parentFrame, message))
            return false;
        return true; // Success
    }

    private boolean standardAnalysis(JSONServerCaller serverCaller,
                                     ProgressPane progPane,
                                     JFrame parentFrame) throws JsonEOFException, IOException {
        progPane.setText("Loading data...");
        String message = serverCaller.openData(file);
        if (!checkMessage(parentFrame, message))
            return false;
        progPane.setText("Preprocessing data...");
        message = serverCaller.preprocessData(regressoutKeys, imputationMethod);
        if (!checkMessage(parentFrame, message))
            return false;
        progPane.setText("Clustering...");
        message = serverCaller.clusterData();
        if (!checkMessage(parentFrame, message))
            return false;
        return true; // Success
    }

    protected boolean checkMessage(JFrame parentFrame, String message) {
        if (message.startsWith("error")) {
            showErrorMessage(message, parentFrame);
            parentFrame.getGlassPane().setVisible(false);
            return false; 
        }
        return true;
    }
    
    private void showErrorMessage(String message,
                                  JFrame parentFrame) {
        JOptionPane.showMessageDialog(parentFrame,
                                      "Error in single cell data analysis: " + message, 
                                      "Error in Single Cell Analysis",
                                      JOptionPane.ERROR_MESSAGE);
    }

    protected void buildCellNetwork(JSONServerCaller caller) throws JsonEOFException, IOException {
        // Coordinates for cells based on umap
        List<List<Double>> umap = caller.getUMAP();
        List<Integer> clusters = caller.getCluster();
        List<String> cellIds = caller.getCellIds();
        List<List<String>> connectivities = caller.getConnectivities();
        // To be used to set the node views
        Map<String, List<Double>> idToUmap = new HashMap<>();
        Map<String, Integer> idToCluster = new HashMap<>();
        for (int i = 0; i < cellIds.size(); i++) {
            idToUmap.put(cellIds.get(i), umap.get(i));
            idToCluster.put(cellIds.get(i), clusters.get(i));
        }
        Map<String, Double> edgeNameToWeight = new HashMap<>();
        Set<String> edges = new HashSet<>();
        String nameDelimit = "-";
        for (int i = 0; i < connectivities.size(); i++) {
            List<String> connectivity = connectivities.get(i);
            Double weight = new Double(connectivity.get(2));
            if (weight < EDGE_WEIGHT_CUTOFF)
                continue; // To control the total edges
            // The first two are cell ids indices and the last is weight
            String cellId1 = cellIds.get(new Integer(connectivity.get(0)));
            String cellId2 = cellIds.get(new Integer(connectivity.get(1)));
            String pair = InteractionUtilities.generateFIFromGene(cellId1, cellId2);
            edges.add(pair);
            String edgeName = pair.replace("\t", " (" + nameDelimit + ") ");
            edgeNameToWeight.put(edgeName, weight);
        }
        CyNetworkView view = constructNetwork(cellIds,
                                              idToUmap,
                                              idToCluster, 
                                              null,
                                              edgeNameToWeight,
                                              null,
                                              edges, 
                                              "Cell",
                                              nameDelimit,
                                              SingleCellNetwork);
        // Turn off edges as the default
        SwingUtilities.invokeLater(() -> ScNetworkManager.getManager().setEdgesVisible(false, view));
    }

    protected void buildClusterNetwork(JSONServerCaller caller) throws Exception {
        Map<String, List<List<Double>>> paga = caller.getPAGA();
        List<List<Double>> positions = paga.get("pos");
        List<List<Double>> connectivities = paga.get("connectivities");
        List<Integer> clusters = caller.getCluster();
        Map<Integer, Long> clusterToCellNumbers = clusters.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<String> nodeIds = new ArrayList<>();
        Map<String, List<Double>> idToPos = new HashMap<>();
        Map<String, Integer> idToCluster = new HashMap<>();
        Map<String, Double> edgeNameToWeight = new HashMap<>();
        Set<String> edges = new HashSet<>();
        Map<String, Integer> idToCellNumber = new HashMap<>();
        String edgeType = "-";

        // Handle nodes
        double scale = 10.0d; // For position scale so that it is better rendered in Cytoscape
        for (int i = 0; i < positions.size(); i++) {
            String nodeId = SCNetworkVisualStyle.CLUSTER_NODE_PREFIX + i;
            nodeIds.add(nodeId);
            List<Double> pos = positions.get(i);
            idToPos.put(nodeId, Arrays.asList(pos.get(0) * scale, pos.get(1) * scale));
            idToCluster.put(nodeId, i);
            idToCellNumber.put(nodeId, clusterToCellNumbers.get(i).intValue());
        }

        Map<String, Boolean> edgeNameToDirection = new HashMap<>();
        if (isForRNAVelocity) { // This is directed
            for (int i = 0; i < nodeIds.size(); i++) {
                String idi = nodeIds.get(i);
                List<Double> weights = connectivities.get(i);
                for (int j = 0; j < nodeIds.size(); j++) {
                    // Threshold should be handled by the server
                    if (weights.get(j) < Double.MIN_NORMAL)
                        continue;
                    String idj = nodeIds.get(j);
                    // Since we will use directed edge, therefore, we need to fix the edge
                    // ids to make sure source and target are right.
                    String pair = idj + "\t" + idi; // The direction is based on python code
                    edges.add(pair);
                    String edgeName = pair.replace("\t", " (" + edgeType + ") ");
                    edgeNameToWeight.put(edgeName, weights.get(j));
                    edgeNameToDirection.put(edgeName, Boolean.TRUE);
                }
            }
        }
        else { // This is undirected
            for (int i = 0; i < nodeIds.size() - 1; i++) {
                String idi = nodeIds.get(i);
                List<Double> weights = connectivities.get(i);
                for (int j = i + 1; j < nodeIds.size(); j++) {
                    if (weights.get(j) < EDGE_WEIGHT_CUTOFF)
                        continue;
                    String idj = nodeIds.get(j);
                    String pair = InteractionUtilities.generateFIFromGene(idi, idj);
                    edges.add(pair);
                    String edgeName = pair.replace("\t", " (" + edgeType + ") ");
                    edgeNameToWeight.put(edgeName, weights.get(j));
                    edgeNameToDirection.put(edgeName, Boolean.FALSE);
                }
            }
        }
        scalePositions(idToPos, edges);
        
        CyNetworkView networkview = constructNetwork(nodeIds, 
                                                     idToPos, 
                                                     idToCluster,
                                                     idToCellNumber,
                                                     edgeNameToWeight,
                                                     edgeNameToDirection,
                                                     edges, 
                                                     "CellCluster",
                                                     edgeType,
                                                     SingleCellClusterNetwork);
    }
    
    /**
     * Scale the edges for cluster nodes based on the shortest edge.
     * @param nodeIdToPos
     * @param edges
     */
    private void scalePositions(Map<String, List<Double>> nodeIdToPos,
                                Set<String> edges) {
        double minDist = Double.MAX_VALUE;
        for (String edge : edges) {
            String[] ids = edge.split("\t");
            List<Double> pos1 = nodeIdToPos.get(ids[0]);
            List<Double> pos2 = nodeIdToPos.get(ids[1]);
            double dx = pos1.get(0) - pos2.get(0);
            double dy = pos1.get(1) - pos2.get(1);
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist)
                minDist = dist;
        }
        double scale = CellClusterVisualStyle.MIN_CLUSTER_DIST / minDist;
        for (String nodeId : nodeIdToPos.keySet()) {
            List<Double> pos = nodeIdToPos.get(nodeId);
            pos.set(0, pos.get(0) * scale);
            pos.set(1, pos.get(1) * scale);
        }
    }

    private CyNetworkView constructNetwork(List<String> nodeIds,
                                           Map<String, List<Double>> idToPos,
                                           Map<String, Integer> idToCluster,
                                           Map<String, Integer> idToCellNumber,
                                           Map<String, Double> edgeNameToWeight,
                                           Map<String, Boolean> edgeNameToDirection,
                                           Set<String> edges,
                                           String nodeType,
                                           String edgeType,
                                           ReactomeNetworkType type) {
        FINetworkGenerator generator = new FINetworkGenerator();
        generator.setNodeType(nodeType);
        generator.setEdgeType("Transition");
        generator.setDirectionInEdgeName(true);
        CyNetwork network = generator.constructFINetwork(new HashSet<>(nodeIds), 
                                                         edges,
                                                         edgeType);
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", 
                                                                       type.toString());
        // Register and display the network
        PlugInObjectManager manager = PlugInObjectManager.getManager();
        CyNetworkManager netManager = manager.getNetworkManager();
        netManager.addNetwork(network);
        // We want to put the scores into the table
        TableHelper tableHelper = new TableHelper();
        //            tableHelper.storeNodeAttributesByName(network,
        //                                                  FIVisualStyle.GENE_VALUE_ATT,
        //                                                  geneToScore);
        // Mark this network before creating a view so that the popup menu can be created correctly
        tableHelper.markAsReactomeNetwork(network, 
                                          type);
        CyNetworkViewFactory viewFactory = manager.getNetworkViewFactory();
        CyNetworkView view = viewFactory.createNetworkView(network);
        CyNetworkViewManager viewManager = manager.getNetworkViewManager();
        viewManager.addNetworkView(view);
        // Need to assign coordinates
        view.getNodeViews().forEach(nodeView -> {
            CyNode node = nodeView.getModel();
            String name = tableHelper.getStoredNodeAttribute(network, node, "name", String.class);
            List<Double> coordinates = idToPos.get(name);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, coordinates.get(0));
            // Python seemingly uses a different coordinate systems: the Y axis is flipped.
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, -coordinates.get(1));
        });
        tableHelper.storeNodeAttributesByName(network, CLUSTER_NAME, idToCluster);
        if (idToCellNumber != null)
            tableHelper.storeNodeAttributesByName(network, CELL_NUMBER_NAME, idToCellNumber);
        tableHelper.storeEdgeAttributesByName(network, CONNECTIVITY_NAME, edgeNameToWeight);
        if (edgeNameToDirection != null)
            tableHelper.storeEdgeAttributesByName(network, EDGE_IS_DIRECTED, edgeNameToDirection);
        FIVisualStyle style = null;
        if (type == ReactomeNetworkType.SingleCellClusterNetwork)
            style = ScNetworkManager.getManager().getClusterStyle();
        else
            style = ScNetworkManager.getManager().getScStyle();
        style.setVisualStyle(view, false);
        view.fitContent();
        view.updateView();
        return view;
    }
}
