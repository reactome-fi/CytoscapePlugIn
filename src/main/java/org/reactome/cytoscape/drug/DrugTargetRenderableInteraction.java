/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

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

    @Override
    public JMenuItem createMenuItem() {
        if (interactions == null || interactions.size() == 0)
            return null;
        JMenuItem rtn = null;
        if (interactions.size() > 1) {
            // Need to use submenus
            rtn = new JMenu("Show Details");
            // Do a sorting
            List<Interaction> list = new ArrayList<>(interactions);
            Collections.sort(list, new Comparator<Interaction>() {
                public int compare(Interaction int1, Interaction int2) {
                    return int1.getIntTarget().getTargetName().compareTo(int2.getIntTarget().getTargetName());
                }
            });
            for (Interaction i : list) {
                JMenuItem item = new JMenuItem(i.getIntTarget().getTargetName() + "-" + 
                                               i.getIntDrug().getDrugName());
                final Interaction tmpI = i;
                item.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showDetails(tmpI);
                    }
                });
                rtn.add(item);
            }
        }
        else {
            rtn = new JMenuItem("Show Details");
            rtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showDetails(interactions.iterator().next());
                }
            });
        }
        return rtn;
    }
    
    private void showDetails(Interaction interaction) {
        InteractionView view = new InteractionView();
        view.setInteraction(interaction);
        view.setModal(false);
        view.setVisible(true);
    }
    
}
