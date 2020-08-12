package org.reactome.cytoscape.sc.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.service.FINetworkServiceFactory;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.r3.util.InteractionUtilities;

public class DiffGeneNetworkBuilder {
    private Map<String, Set<String>> mouse2humanMap;
    private DiffGeneNetworkStyle style;
    
    public DiffGeneNetworkBuilder() {
    }
    
    public void setStyle(DiffGeneNetworkStyle style) {
        this.style = style;
    }
    
    public Map<String, Set<String>> getMouse2humanMap() {
        return mouse2humanMap;
    }

    public void setMouse2humanMap(Map<String, Set<String>> mouse2humanMap) {
        this.mouse2humanMap = mouse2humanMap;
    }

    public void buildNetwork(DiffExpResult result) throws Exception {
        Set<String> humanGenes = result.getNames()
                .stream()
                .filter(s -> mouse2humanMap.keySet().contains(s))
                .map(mg -> mouse2humanMap.get(mg))
                .flatMap(s -> s.stream())
                .collect(Collectors.toSet());
        // Check if a local service should be used
        FINetworkService fiService = new FINetworkServiceFactory().getFINetworkService();
        Set<String> fis = fiService.buildFINetwork(humanGenes, false);
        // Build CyNetwork
        FINetworkGenerator generator = new FINetworkGenerator();
        CyNetwork network = generator.constructFINetwork(humanGenes, fis);
        PlugInObjectManager manager = PlugInObjectManager.getManager();
        CyNetworkManager networkManager = manager.getNetworkManager();
        networkManager.addNetwork(network);
        // Build network view
        CyNetworkViewFactory viewFactory = manager.getNetworkViewFactory();
        CyNetworkView view = viewFactory.createNetworkView(network);
        CyNetworkViewManager viewManager = manager.getNetworkViewManager();
        viewManager.addNetworkView(view);
        // Handle network related properties. Do this at the end to avoid null exception because of no view available.
        TableHelper tableHelper = new TableHelper();
        tableHelper.storeFINetworkVersion(network, PlugInObjectManager.getManager().getFiNetworkVersion());
        tableHelper.markAsReactomeNetwork(network);
        tableHelper.storeNetworkAttribute(network, "name", "Network: " + result.getResultName());
        // Save the mapping between mouse and human genes
        storeGeneProperties(humanGenes, network, result, tableHelper);
        if (style == null)
            style = new DiffGeneNetworkStyle();
        style.setVisualStyle(view, false);
        // We need to call these two methods here. Otherwise they will not work since style is not registered during creation.
        style.updateNodeColorsForNumbers(view, "score", BasicVisualLexicon.NODE_FILL_COLOR);
        style.updateNodeColorsForNumbers(view, "logFoldChange", BasicVisualLexicon.NODE_BORDER_PAINT);
        style.doLayout();
        view.updateView();
    }

    private void storeGeneProperties(Set<String> humanGenes,
                                     CyNetwork network,
                                     DiffExpResult result,
                                     TableHelper tableHelper) {
        Map<String, Set<String>> human2mouseMap = InteractionUtilities.switchKeyValues(mouse2humanMap);
        human2mouseMap.keySet().retainAll(humanGenes);
        Map<String, String> mouseGeneAtts = human2mouseMap.keySet()
                                                          .stream()
                                                          .collect(Collectors.toMap(Function.identity(), 
                                                                                    s -> String.join(",", human2mouseMap.get(s))));
        tableHelper.storeNodeAttributesByName(network, "mouseGenes", mouseGeneAtts);
        if (result.isGeneListOnly())
            return;
        String[] attNames = {
                "score",
                "logFoldChange",
                "pValue",
                "FDR"
        };
        String[] selection = {
                "abs",
                "abs",
                "min",
                "min"
        };
        List<?>[] values = {
                result.getScores(),
                result.getLogFoldChanges(),
                result.getPvals(),
                result.getPvalsAdj()
        };
        List<String> mouseGenes = result.getNames();
        for (int i = 0; i < attNames.length; i++) {
            String attName = attNames[i];
            @SuppressWarnings("unchecked")
            List<Double> valueList = (List<Double>) values[i];
            Map<String, Double> attValues = new HashMap<>();
            for (String humanGene : humanGenes) {
                Set<String> mappedMouseGenes = human2mouseMap.get(humanGene);
                for (String mmg : mappedMouseGenes) {
                    int index = mouseGenes.indexOf(mmg);
                    if (index < 0)
                        continue;
                    Double value = valueList.get(index);
                    if (!attValues.containsKey(humanGene)) {
                        attValues.put(humanGene, value);
                        continue;
                    }
                    // We want to have one value only for the displayed human gene even though
                    // a human gene may be mapped to more than one mouse gene. This is to make
                    // the sorter in the node attribute table happy.
                    Double exited = attValues.get(humanGene);
                    if (selection[i].equals("abs")) {
                        if (Math.abs(exited) < Math.abs(value))
                            attValues.put(humanGene, value);
                    }
                    else if (selection[i].equals("min")) {
                        if (exited > value)
                            attValues.put(humanGene, value);
                    }
                }
            }
            tableHelper.storeNodeAttributesByName(network, attName, attValues);
        }
    }

}
