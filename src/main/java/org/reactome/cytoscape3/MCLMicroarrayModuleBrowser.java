/*
 * Created on Apr 12, 2011
 *
 */
package org.reactome.cytoscape3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.cytoscape.service.NetworkModulePanel;

/**
 * This NetworkModuleBrowser is specially designed for MCL network modules
 * generated for microarray data sets.
 * @author wgm
 *
 */
public class MCLMicroarrayModuleBrowser extends NetworkModulePanel {
    
    public MCLMicroarrayModuleBrowser() {
        super("MCL Microarray Browser");
        setVisible(true);
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new MCLModuleTableModel();
    }

    public void showModules(List<Set<String>> clusters,
                            Map<Set<String>, Double> clusterToCorr) {
        MCLModuleTableModel model = (MCLModuleTableModel) contentTable.getModel();
        model.showModules(clusters, clusterToCorr);
    }

    private class MCLModuleTableModel extends NetworkModuleTableModel {
        public MCLModuleTableModel() {
            columnHeaders = new String[] {
                    "Module",
                    "Nodes in Module",
                    "Node Percentage",
                    "Average Correlation",
                    "Node List"
            };
        }
        
        public void showModules(List<Set<String>> clusters,
                                Map<Set<String>, Double> clusterToCorr) {
            // Need to fill data into tableData
            tableData.clear();
            int index = 0;
            // Get a total numbers
            int total = 0;
            for (Set<String> cluster : clusters)
                total += cluster.size();
            for (Set<String> cluster : clusters) {
                String[] row = new String[columnHeaders.length];
                row[0] = index + "";
                index ++;
                row[1] = cluster.size() + "";
                double percent = (double) cluster.size() / total;
                row[2] = String.format("%.4f", percent);
                row[3] = String.format("%.4f", clusterToCorr.get(cluster));
                row[4] = createIDText(cluster);
                tableData.add(row);
            }
            fireTableDataChanged();
        }
        
    }
    
}
