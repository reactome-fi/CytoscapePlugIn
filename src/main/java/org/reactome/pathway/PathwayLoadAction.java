/*
 * Created on Jul 18, 2013
 *
 */
package org.reactome.pathway;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;

/**
 * This customized CyAction is used to load a pathway diagram from Reactome via a RESTful API.
 * @author gwu
 */
public class PathwayLoadAction extends AbstractCyAction {
    
    public PathwayLoadAction() {
        super("Load Pathway Diagram");
        setPreferredMenu("Apps.Reactome FI");
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        System.out.println("Load pathway diagram!");
    }
    
}
