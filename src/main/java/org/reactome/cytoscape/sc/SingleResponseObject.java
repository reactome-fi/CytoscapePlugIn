package org.reactome.cytoscape.sc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL) class SingleResponseObject extends JSONRPCObject {
    
    private String result;
    
    public SingleResponseObject() {
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
    
    
}