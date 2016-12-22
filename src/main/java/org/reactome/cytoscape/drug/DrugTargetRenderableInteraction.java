/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.HashSet;
import java.util.Set;

import org.gk.render.RendererFactory;
import org.reactome.cytoscape.service.FIRenderableInteraction;
import org.reactome.cytoscape.service.FIRenderableInteractionRenderer;

import edu.ohsu.bcb.druggability.Interaction;

/**
 * A customized FIRenderableInteraction for cancer drug/target interactions.
 * @author gwu
 *
 */
public class DrugTargetRenderableInteraction extends FIRenderableInteraction {
    
    // A static block so the following statement will be called once only
    static {
        FIRenderableInteractionRenderer renderer = new FIRenderableInteractionRenderer();
        RendererFactory.getFactory().registerRenderer(DrugTargetRenderableInteraction.class,
                                                      renderer);
    }
    
    // The data model behind the renderable object.
    private Set<Interaction> interactions;
    
    public DrugTargetRenderableInteraction() {
    }

    public Set<Interaction> getInteractions() {
        return interactions;
    }

    public void setInteractions(Set<Interaction> interactions) {
        this.interactions = interactions;
    }
    
    public void addInteraction(Interaction interaction) {
        if (this.interactions == null)
            this.interactions = new HashSet<>();
        this.interactions.add(interaction);
    }
    
}
