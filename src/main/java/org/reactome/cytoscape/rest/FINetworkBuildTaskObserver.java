package org.reactome.cytoscape.rest;

import java.util.ArrayList;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class FINetworkBuildTaskObserver implements TaskObserver {
    private CIResponse<Long> response;
    private CyNetwork network;
    
    public FINetworkBuildTaskObserver() {
    }
    
    public CIResponse<Long> getResponse() {
        return response;
    }

    @Override
    public void allFinished(FinishStatus status) {
        response = new CIResponse<>();
        if (status.getType() == FinishStatus.Type.SUCCEEDED || status.getType() == FinishStatus.Type.CANCELLED) {
            if (network != null)
                response.data = network.getSUID();
            else
                response.data = -1L;
            response.errors = new ArrayList<>();
        }
    }

    @Override
    public void taskFinished(ObservableTask task) {
        network = task.getResults(CyNetwork.class);
    }

}
