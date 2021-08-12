package org.reactome.cytoscape.sc.utils;

public enum ScPathwayDataType {
    
    Pathway,
    Transcription_Factor;
    
    @Override
    public String toString() {
        String rtn = super.toString();
        return rtn.replace("_", " ");
    }

}
