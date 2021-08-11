package org.reactome.cytoscape.sc.server;

abstract class JSONRPCObject {
    
    private String jsonrpc = "2.0";
    protected Integer id;
    
    public JSONRPCObject() {
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
}