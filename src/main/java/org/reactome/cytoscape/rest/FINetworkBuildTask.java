package org.reactome.cytoscape.rest;

import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape3.GeneSetMutationAnalysisTask;

public class FINetworkBuildTask extends AbstractTask implements ObservableTask {
    private GeneSetMutationAnalysisTask task;
    private CyNetwork network;
    
    public FINetworkBuildTask(GeneSetMutationAnalysisTask task) {
        this.task = task;
    }
    
    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(CyNetwork.class))
            return (R) network;
        return null;
    }
    
    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(CyNetwork.class);
    }

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        task.run(); // The original task is implemented as a Runnable directly.
        network = PlugInUtilities.getCurrentNetworkView().getModel();
    }

}
