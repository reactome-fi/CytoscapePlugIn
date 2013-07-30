package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.osgi.framework.ServiceReference;

public class MicroarrayAnalysisAction extends FICytoscapeAction
{

    private CySwingApplication desktopApp;

    public MicroarrayAnalysisAction(CySwingApplication desktopApp)
    {
        super("Microarray Analysis");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(2.0f);
        this.desktopApp = desktopApp;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (!createNewSession())
        {
            return;
        }
        ActionDialogs gui = new ActionDialogs("Microarray");
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
