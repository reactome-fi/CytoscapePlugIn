/*
 * Created on Sep 1, 2015
 *
 */
package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;

import org.reactome.cytoscape.fipgm.PGMImpactAnalysisDialog;

/**
 * @author gwu
 *
 */
public class PGMImpactAnalysisAction extends FICytoscapeAction {
    
    public PGMImpactAnalysisAction() {
        super("PGM Impact Analysis");
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!createNewSession())
            return; // Cannot create a new Cytoscape session. Stop here.
        PGMImpactAnalysisDialog dialog = new PGMImpactAnalysisDialog();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked())
            return;
        System.out.println("Perform PGM Impact Analysis!");
    }
    
}
