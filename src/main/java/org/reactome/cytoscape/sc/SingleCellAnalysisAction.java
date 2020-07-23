package org.reactome.cytoscape.sc;

import java.io.File;

import javax.swing.JOptionPane;

import org.reactome.cytoscape.service.FICytoscapeAction;
import org.reactome.cytoscape.util.PlugInObjectManager;

@SuppressWarnings("serial")
public class SingleCellAnalysisAction extends FICytoscapeAction {

    public SingleCellAnalysisAction() {
        super("Single Cell Analysis");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(10.5f);
    }

    @Override
    protected void doAction() {
        try {
            ScActionDialog gui = new ScActionDialog();
            File file = gui.selectFile();
            if (file == null)
                return ;
            ScAnalysisTask task = new ScAnalysisTask(file.getAbsolutePath(), 
                                                     gui.getSpecies(),
                                                     gui.getFormat(),
                                                     gui.getRegressoutKeys());
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
