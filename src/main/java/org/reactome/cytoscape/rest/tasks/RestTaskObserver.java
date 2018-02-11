package org.reactome.cytoscape.rest.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class RestTaskObserver implements TaskObserver {

    private ReactomeFIVizTable table;
    private CIResponse<ReactomeFIVizTable> response;
    
    public RestTaskObserver() {
    }

    @Override
    public void taskFinished(ObservableTask task) {
        List<Class<?>> resultClasses = task.getResultClasses();
        if (resultClasses.contains(ReactomeFIVizTable.class))
            table = task.getResults(ReactomeFIVizTable.class);
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {
        response = new CIResponse<>();
        if (finishStatus.getType() == FinishStatus.Type.SUCCEEDED || finishStatus.getType() == FinishStatus.Type.CANCELLED) {
            if (table == null) {
                createError();
            }
            else {
                response.data = table;
                response.errors = new ArrayList<>();
            }
        }
        else {
            createError();
        }
    }
    
    private void createError() {
        CIError error = new CIError();
        error.message = "Cannot perform the specified task. See logging for the error details.";
        response.errors = Collections.singletonList(error);
    }

    public CIResponse<ReactomeFIVizTable> getResponse() {
        return this.response;
    }
    
}
