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
}
