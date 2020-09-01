package org.reactome.cytoscape.sc;

import java.io.File;

import javax.swing.JOptionPane;

import org.reactome.cytoscape.service.FICytoscapeAction;
import org.reactome.cytoscape.util.PlugInObjectManager;

@SuppressWarnings("serial")
public class SingleCellLoadAction extends FICytoscapeAction {

    public SingleCellLoadAction() {
        super("Open");
        setPreferredMenu("Apps.Reactome FI.Single Cell Analysis[10]");
        setMenuGravity(2.0f);
    }

    @Override
    protected void doAction() {
        try {
            ScNetworkManager.getManager().reset(); // Reset the status for a new analysis
            ScLoadActionDialog gui = new ScLoadActionDialog();
            File file = gui.selectFile();
            if (file == null)
                return ;
            ScLoadTask task = new ScLoadTask();
            task.setFile(file.getAbsolutePath());
            task.setPathwaySpecies(gui.getSpecies());
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
