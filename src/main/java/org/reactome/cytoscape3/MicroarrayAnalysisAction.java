package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.reactome.cytoscape.service.FICytoscapeAction;

public class MicroarrayAnalysisAction extends FICytoscapeAction
{

    private CySwingApplication desktopApp;

    public MicroarrayAnalysisAction(CySwingApplication desktopApp)
    {
        super("Gene Expression Analysis");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(20.0f);
        this.desktopApp = desktopApp;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (!createNewSession())
        {
            return;
        }
        MicroArrayAnalysisDialog gui = new MicroArrayAnalysisDialog();
        gui.setLocationRelativeTo(desktopApp.getJFrame());
        gui.setModal(true);
        gui.setVisible(true);
        if (!gui.isOkClicked())
            return;
        final File file = gui.getSelectedFile();
        if (file == null || !file.exists())
        {
            JOptionPane.showMessageDialog(desktopApp.getJFrame(), 
                    "No file is chosen or the selected file doesn't exist!", 
                    "Error in File", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Thread t = new Thread(new MicroarrayAnalysisTask(gui));
        t.start();
    }

}
