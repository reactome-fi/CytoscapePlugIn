package org.reactome.cytoscape3;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;

import org.reactome.r3.util.InteractionUtilities;

@SuppressWarnings("serial")
public class NetworkModuleBrowser extends NetworkModulePanel
{
    private JLabel modularityLabel;
    
    public NetworkModuleBrowser() {
        super("Network Module Browser");
        init();
    }
    
    private void init() {
        controlToolBar.remove(closeGlue);
        controlToolBar.remove(closeBtn);
        controlToolBar.addSeparator();
        modularityLabel = new JLabel();
        Font font = modularityLabel.getFont();
        modularityLabel.setFont(font.deriveFont(font.getSize() - 1));
        controlToolBar.add(modularityLabel);
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new ModuleTableModel();
    }

    public void showModules(Map<String, Integer> nodeToModule,
                            Map<String, Set<String>> nodeToSamples) {
        ModuleTableModel model = (ModuleTableModel) contentTable.getModel();
        Map<String, Set<String>> sampleToNodes = null;
        if (nodeToSamples != null)
            sampleToNodes = InteractionUtilities.switchKeyValues(nodeToSamples);
        model.setTableData(nodeToModule,
                           sampleToNodes);
    }
    
    public void showModularity(Double modularity) {
        modularityLabel.setText("Modularity: " + 
                                String.format("%.4f", modularity));
    }
    
    private class ModuleTableModel extends NetworkModuleTableModel {
        
        public ModuleTableModel() {
            columnHeaders = new String[] {
                    "Module",
                    "Nodes in Module",
                    "Node Percentage",
                    "Samples in Module",
                    "Sample Percentage",
                    "Node List"
            };
        }
        
        public void setTableData(Map<String, Integer> nodeToModule,
                                 Map<String, Set<String>> sampleToNodes) {
            tableData.clear();
            List<Set<String>> modules = new ArrayList<Set<String>>();
            // Get the total module number
            Set<Integer> moduleSet = new HashSet<Integer>(nodeToModule.values());
            for (int i = 0; i < moduleSet.size(); i++)
                modules.add(new HashSet<String>());
            Set<String> allNodes = new HashSet<String>();
            for (String node : nodeToModule.keySet()) {
                Integer module = nodeToModule.get(node);
                Set<String> set = modules.get(module);
                set.add(node);
                allNodes.add(node);
            }
            for (int i = 0; i < modules.size(); i++) {
                String[] rowData = new String[columnHeaders.length];
                rowData[0] = i + "";
                Set<String> set = modules.get(i);
                rowData[1] = set.size() + "";
                rowData[2] = String.format("%.4f",
                                           (double) set.size() / allNodes.size());
                if (sampleToNodes == null) {
                    rowData[3] = "";
                    rowData[4] = "";
                }
                else {
                    int sampleHit = countSampleInModule(set, sampleToNodes);
                    rowData[3] = sampleHit + "";
                    rowData[4] = String.format("%.4f", (double)sampleHit / sampleToNodes.size());
                }
                rowData[5] = createIDText(set);
                tableData.add(rowData);
            }
            fireTableDataChanged();
        }
        
        private int countSampleInModule(Set<String> moduleNode,
                                        Map<String, Set<String>> sampleToNodes) {
            int count = 0;
            for (String sample : sampleToNodes.keySet()) {
                Set<String> sampleNodes = sampleToNodes.get(sample);
                if (InteractionUtilities.isShared(sampleNodes, moduleNode))
                    count ++;
            }
            return count;
        }

    }

}
