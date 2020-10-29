package org.reactome.cytoscape.sc;

import java.io.File;

import javax.swing.JOptionPane;

import org.reactome.cytoscape.service.FICytoscapeAction;
import org.reactome.cytoscape.util.PlugInObjectManager;

@SuppressWarnings("serial")
public class SingleCellAnalysisAction extends FICytoscapeAction {

    public SingleCellAnalysisAction() {
        super("Analyze");
        setPreferredMenu("Apps.Reactome FI.Single Cell Analysis[10]");
        setMenuGravity(1.0f);
    }

    @Override
    protected void doAction() {
        if (!ScNetworkManager.getManager().isOSSupported())
            return;
        try {
            ScNetworkManager.getManager().reset(); // Reset the status for a new analysis
            ScActionDialog gui = new ScActionDialog();
            File file = gui.selectFile();
            if (file == null)
                return ;
            ScAnalysisTask task = new ScAnalysisTask(file.getAbsolutePath(), 
                                                     gui.getSpecies(),
                                                     gui.getFormat(),
                                                     gui.getRegressoutKeys(),
                                                     gui.getImputationMethod(),
                                                     gui.isForRNAVelocity());
            if (gui.isForRNAVelocity())
                task.setVelocityMode(gui.getVelocityMode());
            Thread thread = new Thread(task);
            thread.start();
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in action: " + e.getMessage(),
                                          "Error in Action",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

}
