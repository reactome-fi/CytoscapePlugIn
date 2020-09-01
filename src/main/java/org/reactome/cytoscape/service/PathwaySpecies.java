package org.reactome.cytoscape.service;

/**
 * A list of supported species for Reactome pathways. Current only two: 
 * Homo sapiens
 * Mus musculus
 * @author wug
 *
 */
public enum PathwaySpecies {

    Homo_sapiens,
    Mus_musculus;
    
    @Override
    public String toString() {
        String rtn = super.toString();
        return rtn.replace("_", " ");
    }
    
    public String getName() {
        return toString();
    }
    
    public String getCommonName() {
        if (this == Mus_musculus)
            return "mouse";
        if (this == Homo_sapiens)
            return "human";
        return null;
    }
    
    public String getURLEncode() {
        return super.toString().replace("_", "+");
    }
    
    public String getDBID() {
        if (this == Mus_musculus)
            return "48892";
        return "48887";
    }
}
