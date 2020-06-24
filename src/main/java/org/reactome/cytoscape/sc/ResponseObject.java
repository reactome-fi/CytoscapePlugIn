package org.reactome.cytoscape.sc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL) 
class ResponseObject extends JSONRPCObject {
    
    // Can be a string or a list of string
    private Object result;
    
    public ResponseObject() {
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
    
    
}