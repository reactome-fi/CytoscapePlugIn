package org.reactome.cytoscape.sc;

import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CELL_NUMBER_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CLUSTER_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CONNECTIVITY_NAME;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellClusterNetwork;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellNetwork;

import java.io.IOException;
import java.util.ArrayList;
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

    public ScAnalysisTask(String file,
                          PathwaySpecies species,
                          String fileFormat) {
        this.file = file;
        this.species = species;
        this.fileFormat = fileFormat;
    }

    @Override
    protected void doAnalysis() {
        ScNetworkManager.getManager().setSpecies(species);
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setTitle("Single Cell Data Analysis");
        JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        parentFrame.setGlassPane(progPane);
        progPane.setVisible(true);
        progPane.setText("Loading data...");
        JSONServerCaller serverCaller = ScNetworkManager.getManager().getServerCaller();
        try {
            String message = serverCaller.openData(file);
            if (!checkMessage(parentFrame, message))
                return;
            progPane.setText("Preprocessing data...");
            message = serverCaller.preprocessData();
            if (!checkMessage(parentFrame, message))
                return;
            progPane.setText("Clustering...");
            message = serverCaller.clusterData();
            if (!checkMessage(parentFrame, message))
                return;
            // Looks everything is fine. Let's build the network
            progPane.setText("Building cluster network...");
            buildClusterNetwork(serverCaller);
            progPane.setText("Building cell network...");
            buildCellNetwork(serverCaller);
        }
        catch(Exception e) {
            showErrorMessage(e.getMessage(), parentFrame);
            e.printStackTrace();
        }
        parentFrame.getGlassPane().setVisible(false);
    }

    private boolean checkMessage(JFrame parentFrame, String message) {
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

    private void buildCellNetwork(JSONServerCaller caller) throws JsonEOFException, IOException {
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
                                              edges, 
                                              nameDelimit,
                                              SingleCellNetwork);
        // Turn off edges as the default
        SwingUtilities.invokeLater(() -> ScNetworkManager.getManager().setEdgesVisible(false, view));
    }

    private void buildClusterNetwork(JSONServerCaller caller) throws Exception {
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
        for (int i = 0; i < positions.size(); i++) {
            String nodeId = SCNetworkVisualStyle.CLUSTER_NODE_PREFIX + i;
            nodeIds.add(nodeId);
            idToPos.put(nodeId, positions.get(i));
            idToCluster.put(nodeId, i);
            idToCellNumber.put(nodeId, clusterToCellNumbers.get(i).intValue());
        }

        // Handle edges
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
            }
        }

        constructNetwork(nodeIds, 
                         idToPos, 
                         idToCluster,
                         idToCellNumber,
                         edgeNameToWeight,
                         edges, 
                         edgeType,
                         SingleCellClusterNetwork);
    }

    private CyNetworkView constructNetwork(List<String> nodeIds,
                                           Map<String, List<Double>> idToPos,
                                           Map<String, Integer> idToCluster,
                                           Map<String, Integer> idToCellNumber,
                                           Map<String, Double> edgeNameToWeight,
                                           Set<String> edges,
                                           String edgeType,
                                           ReactomeNetworkType type) {
        CyNetwork network = new FINetworkGenerator().constructFINetwork(new HashSet<>(nodeIds), 
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
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, coordinates.get(1));
        });
        tableHelper.storeNodeAttributesByName(network, CLUSTER_NAME, idToCluster);
        if (idToCellNumber != null)
            tableHelper.storeNodeAttributesByName(network, CELL_NUMBER_NAME, idToCellNumber);
        tableHelper.storeEdgeAttributesByName(network, CONNECTIVITY_NAME, edgeNameToWeight);
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
