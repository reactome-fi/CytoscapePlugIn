package org.reactome.cytoscape.rest.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class RestTaskObserver<T> implements TaskObserver {

    private Class<T> classType;
    private T results;
    private CIResponse<T> response;
    
    public RestTaskObserver(Class<T> cls) {
        this.classType = cls;
    }

    @Override
    public void taskFinished(ObservableTask task) {
        List<Class<?>> resultClasses = task.getResultClasses();
        if (resultClasses.contains(classType))
            results = task.getResults(classType);
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {
        response = new CIResponse<>();
        if (finishStatus.getType() == FinishStatus.Type.SUCCEEDED || finishStatus.getType() == FinishStatus.Type.CANCELLED) {
            if (results == null) {
                createError();
            }
            else {
                response.data = results;
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

    public CIResponse<T> getResponse() {
        return this.response;
    }
    
}
