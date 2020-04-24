package org.reactome.cytoscape.rest.tasks;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to provide the data for the Reactome reacfoam.
 * @author wug
 *
 */
public class PathwayEnrichmentResults {
    private String type = "OVERREPRESENTATION";
    private List<Pathway> pathways;
    
    public PathwayEnrichmentResults() {
    }
    
    public void addPathway(String stId,
                           String name,
                           String pvalue,
                           String total,
                           String found) {
        if (pathways == null)
            pathways = new ArrayList<>();
        Pathway pathway = new Pathway();
        pathway.stId = stId;
        pathway.name = name;
        Entities entities = new Entities();
        entities.pValue = pvalue;
        entities.total = total;
        entities.found = found;
        pathway.entities = entities;
        pathways.add(pathway);
    }
    
    private class Pathway {
        String stId;
        String name;
        Entities entities;
    }
    
    private class Entities {
        String pValue;
        String total;
        String found;
    }

}
