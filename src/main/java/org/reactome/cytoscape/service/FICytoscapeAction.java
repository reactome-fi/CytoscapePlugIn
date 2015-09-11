package org.reactome.cytoscape.service;

/**
 * This class provides some basic functions which
 * most analysis actions require, such as file 
 * validation and testing whether a new network should
 * be created. It is meant to be extended.
 * @author Eric T Dawson
 * @date July 2013
 */
import org.cytoscape.application.swing.AbstractCyAction;
import org.reactome.cytoscape.util.PlugInUtilities;

public abstract class FICytoscapeAction extends AbstractCyAction {
    
    public FICytoscapeAction(String title) {
        super(title);
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(0.5f);
    }

    protected boolean createNewSession() {
        return PlugInUtilities.createNewSession();
    }
    
}
