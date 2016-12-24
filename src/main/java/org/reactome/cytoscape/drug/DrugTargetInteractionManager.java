/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.ohsu.bcb.druggability.Interaction;

/**
 * A singleton to manage a list of drug/target interactions so that interactions will not be duplicated after loading
 * from the server.
 * @author gwu
 *
 */
public class DrugTargetInteractionManager {
    private static DrugTargetInteractionManager manager;
    private Map<String, Interaction> idToInteraction;
    // Cached filter
    private InteractionFilter interactionFilter;
    
    /**
     * Default constructor.
     */
    private DrugTargetInteractionManager() {
        this.idToInteraction = new HashMap<>();
        this.interactionFilter = new InteractionFilter();
    }
    
    public static DrugTargetInteractionManager getManager() {
        if (manager == null)
            manager = new DrugTargetInteractionManager();
        return manager;
    }
    
    public InteractionFilter getInteractionFilter() {
        return this.interactionFilter;
    }
    
    public void addInteractions(Collection<Interaction> interactions) {
        if (interactions == null || interactions.size() == 0)
            return;
        for (Interaction interaction : interactions) {
            if (idToInteraction.containsKey(interaction.getId()))
                continue;
            idToInteraction.put(interaction.getId(),
                                interaction);
        }
    }
    
}
