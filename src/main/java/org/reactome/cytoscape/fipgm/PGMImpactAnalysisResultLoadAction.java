/*
 * Created on Oct 19, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import org.reactome.cytoscape.service.FICytoscapeAction;

/**
 * @author gwu
 *
 */
public class PGMImpactAnalysisResultLoadAction extends FICytoscapeAction {
    
    /**
     * @param title
     */
    public PGMImpactAnalysisResultLoadAction() {
        super("Open");
        setPreferredMenu("Apps.Reactome FI.PGM Impact Analysis[20]");
        setMenuGravity(6.0f);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    protected void doAction() {
        PGMImpactResultLoadTask task = new PGMImpactResultLoadTask();
        Thread t = new Thread(task);
        t.start();
    }
    
}
