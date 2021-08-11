package org.reactome.cytoscape.sc.server;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

// A simple class for sending a JSON query to the server side
@JsonInclude(Include.NON_NULL) class RequestObject extends JSONRPCObject {
    
    String method;
    private List<Object> params;
    
    public RequestObject() {
    }
    
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }

    public void addParams(Object param) {
        if (params == null)
            params = new ArrayList<>();
        params.add(param);
    }
    
    public void resetParams() {
        if (params != null)
            params.clear();
    }
    
}