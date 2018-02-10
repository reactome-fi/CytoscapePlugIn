package org.reactome.cytoscape.rest.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape3.ClusterFINetworkTask;
import org.reactome.cytoscape3.NetworkModuleBrowser;

public class ObservableClusterFINetworkTask extends ClusterFINetworkTask implements ObservableTask {
    private ReactomeFIVizTable result;
    
    public ObservableClusterFINetworkTask(CyNetworkView view, JFrame frame) {
        super(view, frame);
    }

    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(ReactomeFIVizTable.class))
            return (R) result;
        return null;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        super.run(taskMonitor);
        // Need to get the results displayed in the table
        NetworkModuleBrowser browser = PlugInUtilities.getCytoPanelComponent(NetworkModuleBrowser.class,
                                              CytoPanelName.SOUTH,
                                              "Network Module Browser");
        if (browser == null)
            return; // Cannot get anything
        JTable table = browser.getContentTable();
        result = constructRestTable(table);
    }
    
    private ReactomeFIVizTable constructRestTable(JTable table) {
        ReactomeFIVizTable vizTable = new ReactomeFIVizTable();
        TableModel tableModel = table.getModel();
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String header = tableModel.getColumnName(i);
            headers.add(header);
        }
        vizTable.setTableHeaders(headers);
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                Object value = tableModel.getValueAt(i, j);
                row.add(value + "");
            }
            vizTable.addRow(row);
        }
        return vizTable;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(ReactomeFIVizTable.class);
    }
    
}
