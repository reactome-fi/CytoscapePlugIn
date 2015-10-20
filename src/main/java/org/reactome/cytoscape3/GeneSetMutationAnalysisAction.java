package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.reactome.cytoscape.service.FICytoscapeAction;

/**
 * This class manages analysis of Gene Set, NCI MAF and Gene Sample / Number
 * Pair data. GUI creation is encapsulated in ActionDialog while network
 * creation is abstracted into a task.
 * 
 * @author Eric T. Dawson
 * 
 */

@SuppressWarnings("serial")
public class GeneSetMutationAnalysisAction extends FICytoscapeAction
{
    private CySwingApplication desktopApp;
    
    // private TaskMonitor taskMonitor;
    public GeneSetMutationAnalysisAction(CySwingApplication desktopApp)
    {
        super("Gene Set/Mutation Analysis");
        this.desktopApp = desktopApp;
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(1.0f);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (!createNewSession())
        {
            return;
        }
        // Create the GUI. The first argument is the GUI context (the type
        // of analysis being performed.
        GeneSetMutationAnalysisDialog gui = new GeneSetMutationAnalysisDialog();
        gui.setLocationRelativeTo(desktopApp.getJFrame());
        gui.setModal(true);
        gui.setVisible(true);
        if (!gui.isOkClicked()) 
            return;
        File file = gui.getSelectedFile();
        String enteredGenes = gui.getEnteredGenes();
        if ((enteredGenes == null) && (file == null || !file.exists())) {
            JOptionPane.showMessageDialog(desktopApp.getJFrame(),
                    "No file is chosen, the selected file doesn't exist or no gene set is entered!",
                    "Error in Specifying Gene Set", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Create and display a network based on the user's input and the
        // data in the FI database.
        Thread t = new Thread(new GeneSetMutationAnalysisTask(gui));
        t.start();
    }

}
