package org.reactome.cytoscape.rest.tasks;

import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
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
        result = new ReactomeFIVizTable();
        result.fillTableFromResults(NetworkModuleBrowser.class, "Network Module Browser");
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(ReactomeFIVizTable.class);
    }
    
}
