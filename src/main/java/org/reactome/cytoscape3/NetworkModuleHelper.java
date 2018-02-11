package org.reactome.cytoscape3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.InteractionUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple helper class to handle some network module related work.
 * @author wug
 *
 */
public class NetworkModuleHelper {
    private static Logger logger = LoggerFactory.getLogger(NetworkModuleHelper.class);
    private boolean avoidGUIs;
    
    public NetworkModuleHelper() {
    }
    
    public void setAvoidGUIs(boolean avoidGUIs) {
        this.avoidGUIs = avoidGUIs;
    }
    
    public Map<String, Integer> extractNodeToModule(CyNetworkView view) {
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        // Check if the network has been clustered
        if (netTable.getRow(netSUID).get("clustering_Type", String.class) == null) {
            if (avoidGUIs)
                logger.error("Clustering has not been performed.");
            else
                PlugInUtilities.showErrorMessage("Error in Annotating Modules",
                        "Please cluster the FI network before annotating modules.");
            return null;
        }
        final Map<String, Integer> nodeToModule = new HashMap<String, Integer>();
        Set<String> linkers = new HashSet<String>();
        for (CyNode node : view.getModel().getNodeList()) {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
            Integer module = nodeTable.getRow(nodeSUID).get("module", Integer.class);
            // Since nodes which are unlinked will have null value for module
            // (as may some other nodes),
            // only use those nodes with value for module.
            if (module != null) {
                nodeToModule.put(nodeName, module);
                Boolean isLinker = nodeTable.getRow(nodeSUID).get("isLinker", Boolean.class);
                if (isLinker != null && isLinker) {
                    linkers.add(nodeName);
                }
            }
        }
        Integer cutoff = null;
        if (avoidGUIs)
            cutoff = 0; // No cutoff
        else
            cutoff = applyModuleSizeFiler(nodeToModule);
        if (cutoff == null)
            return null; // Equivalent to canceling the task.
        if (!avoidGUIs && !linkers.isEmpty()) {
            CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
            int reply = JOptionPane.showConfirmDialog(desktopApp.getJFrame(),
                                                      "Linkers have been used in network construction."
                                                              + " Including linkers\n will bias results. Would you like to exclude them from analysis?",
                                                      "Exclude Linkers?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)
                return null;
            if (reply == JOptionPane.YES_OPTION) {
                nodeToModule.keySet().removeAll(linkers);
                if (nodeToModule.isEmpty()) {
                    JOptionPane.showMessageDialog(desktopApp.getJFrame(),
                                                  "No genes remain after removing linkers. Annotation cannot be performed.",
                                                  "Cannot Annotate Modules", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            }
        }
        return nodeToModule;
    }
    
    private Integer applyModuleSizeFiler(Map<String, Integer> nodeToModule) {
        Map<Integer, Set<String>> clusterToGenes = new HashMap<Integer, Set<String>>();
        for (String node : nodeToModule.keySet()) {
            Integer module = nodeToModule.get(node);
            InteractionUtilities.addElementToSet(clusterToGenes, module, node);
        }
        Set<Integer> values = new HashSet<Integer>();
        for (Set<String> set : clusterToGenes.values()) {
            values.add(set.size());
        }
        List<Integer> sizeList = new ArrayList<Integer>(values);
        Collections.sort(sizeList);
        Integer input = (Integer) JOptionPane.showInputDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                              "Please choose a size cutoff for modules. Modules with sizes equal\n"
                                                                      + "or more than the cutoff will be used for analysis:",
                                                              "Choose Module Size", JOptionPane.QUESTION_MESSAGE, null,
                                                              sizeList.toArray(), sizeList.get(0));
        if (input == null)
            return null; // Cancel has been pressed.
        // Do a filtering based on size
        Set<String> filtered = new HashSet<String>();
        for (Set<String> set : clusterToGenes.values()) {
            if (set.size() < input) {
                filtered.addAll(set);
            }
        }
        nodeToModule.keySet().removeAll(filtered);
        return input;
    }

}
