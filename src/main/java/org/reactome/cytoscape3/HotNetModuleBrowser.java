/*
 * Created on Mar 19, 2013
 *
 */
package org.reactome.cytoscape3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape3.HotNetAnalysisTask.HotNetModule;
import org.reactome.r3.util.InteractionUtilities;


/**
 * This class is used to display network modules from HotNet.
 * @author gwu
 *
 */
@SuppressWarnings("serial")
public class HotNetModuleBrowser extends NetworkModulePanel {
    
    public HotNetModuleBrowser() {
        super("HotNet Module Browser");
        setVisible(true);
    }
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.NetworkModulePanel#createTableMode()
     */
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new HotNetModuleTableModel();
    }
    
    public void showHotNetModules(List<HotNetModule> modules,
                                  Map<String, Set<String>> sampleToNodes) {
        HotNetModuleTableModel model = (HotNetModuleTableModel) contentTable.getModel();
        model.setTableData(modules, sampleToNodes);
    }
    
    private class HotNetModuleTableModel extends NetworkModuleTableModel {
        
        public HotNetModuleTableModel() {
            columnHeaders = new String[] {
                    "Module",
                    "Nodes in Module",
                    "Node Percentage",
                    "Samples in Module",
                    "Sample Percentage",
                    "pvalue",
                    "FDR",
                    "Node List"
            };
        }
        
        public void setTableData(List<HotNetModule> modules,
                                 Map<String, Set<String>> sampleToNodes) {
            tableData.clear();
            Set<String> allNodes = new HashSet<String>();
            for (HotNetModule module : modules) {
                allNodes.addAll(module.genes);
            }
            for (int i = 0; i < modules.size(); i++) {
                String[] rowData = new String[columnHeaders.length];
                rowData[0] = i + "";
                HotNetModule module = modules.get(i);
                Set<String> set = module.genes;
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
                rowData[5] = String.format("%.2e", module.pvalue);
                rowData[6] = String.format("%.2e", module.fdr);
                rowData[7] = createIDText(set);
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
