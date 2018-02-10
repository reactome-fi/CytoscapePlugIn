package org.reactome.cytoscape.rest.tasks;

import java.util.ArrayList;
import java.util.Collections;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class ClusterFINetworkTaskObserver implements TaskObserver {
    private ReactomeFIVizTable table;
    private CIResponse<ReactomeFIVizTable> response;
    
    public ClusterFINetworkTaskObserver() {
    }

    @Override
    public void taskFinished(ObservableTask task) {
        if (task instanceof ObservableClusterFINetworkTask) {
            ObservableClusterFINetworkTask clusterFITask = (ObservableClusterFINetworkTask) task;
            table = clusterFITask.getResults(ReactomeFIVizTable.class);
        }
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {
        response = new CIResponse<>();
        if (finishStatus.getType() == FinishStatus.Type.SUCCEEDED || finishStatus.getType() == FinishStatus.Type.CANCELLED) {
            if (table == null) {
                CIError error = new CIError();
                error.message = "Cannot perform FI network clustering.";
                response.errors = Collections.singletonList(error);
            }
            else {
                response.data = table;
                response.errors = new ArrayList<>();
            }
        }
    }

    public CIResponse<ReactomeFIVizTable> getResponse() {
        return this.response;
    }
}
