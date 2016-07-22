/*
 * Created on Sep 1, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import org.reactome.cytoscape.service.FICytoscapeAction;

/**
 * @author gwu
 */
public class PGMImpactAnalysisAction extends FICytoscapeAction {
    
    public PGMImpactAnalysisAction() {
        super("Analyze");
        setPreferredMenu("Apps.Reactome FI.PGM Impact Analysis[5]");
        setMenuGravity(5.0f);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    protected void doAction() {
        PGMImpactAnalysisDialog dialog = new PGMImpactAnalysisDialog();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked())
            return;
        PGMImpactAnalysisTask task = new PGMImpactAnalysisTask();
        task.setData(dialog.getSelectedData());
        task.setLbp(dialog.getLBP());
        task.setPGMType(dialog.getPGMType());
        task.setNumberOfPermutation(dialog.getNumberOfPermutation());
        Thread t = new Thread(task);
        t.start();
    }
    
}
