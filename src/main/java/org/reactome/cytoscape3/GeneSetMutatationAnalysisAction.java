package org.reactome.cytoscape3;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;

import javax.swing.JOptionPane;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.ProgressPane;


/**
 * This class manages analysis of Gene Set,
 * NCI MAF and Gene Sample / Number Pair data.
 * GUI creation is encapsulated in ActionDialog
 * while network creation is abstracted into a task.
 * @author Eric T. Dawson
 *
 */




public class GeneSetMutatationAnalysisAction extends FICytoscapeAction
{

    private CySwingApplication desktopApp;
    private TaskManager tm;
    private CyNetworkManager netManager;
    private SaveSessionAsTaskFactory saveSession;
    private FileUtil fileUtil;
    private CySessionManager sessionManager;
    private CyNetworkFactory networkFactory;
    private CyNetworkViewFactory viewFactory;
    private CyNetworkViewManager viewManager;
   // private TaskMonitor taskMonitor;
    public GeneSetMutatationAnalysisAction(TaskManager tm, CyNetworkManager netManager,

	    SaveSessionAsTaskFactory saveSession,
	    FileUtil fileUtil,
	    CySwingApplication desktopApp,
	    CySessionManager sessionManager,
	    CyNetworkFactory networkFactory,
	    CyNetworkViewFactory viewFactory,
	    CyNetworkViewManager viewManager)
    {
	super(desktopApp, netManager, fileUtil, saveSession, tm, sessionManager, "Gene Set / Mutant Analysis");
	this.desktopApp = desktopApp;
	this.tm = tm;
	this.netManager = netManager;
	this.saveSession = saveSession;
	this.fileUtil = fileUtil;
	this.sessionManager = sessionManager;
	this.networkFactory = networkFactory;
	this.viewFactory = viewFactory;
	this.viewManager = viewManager;
	setPreferredMenu("Apps.ReactomeFI");
	
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
	if (createNewSession(netManager, sessionManager)){
	    System.out.println("Session save/no previous session");
	}
	// Create the GUI. The first argument is the GUI context (the type
	// of analysis being performed.
	ActionDialogs gui = new ActionDialogs("GeneSetMutationAnalysis", desktopApp, fileUtil);
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
	// Create and display a network based on the user's input and the
	// data in the FI database.
	GeneSetMutationAnalysisTaskFactory gsmaFactory = new GeneSetMutationAnalysisTaskFactory(desktopApp,
		gui.getFileFormat(), file, gui.chooseHomoGenes(),
		gui.useLinkers(), gui.getUnlinkedGeneBox().isSelected(),
		gui.getUnlinkedGeneBox().isEnabled(), gui.showFIAnnotationsBeFetched(),
		gui.getSampleCutoffValue(), networkFactory, netManager, viewFactory, viewManager);
	tm.execute(gsmaFactory.createTaskIterator());
	
    }
    
   
    
    
    protected boolean createNewSession(CyNetworkManager networkManager, CySessionManager sessionManager)
    {
        /* Checks if a session currently exists and if so whether the
         * user would like to save that session. A new session is then
         * created.
         */
        int networkCount = networkManager.getNetworkSet().size();
        if (networkCount == 0)
            return true;
        String msg = new String( "A new session is needed for using Reactome FI plugin.\n"
                + "Do you want to save your session?");
        int reply = JOptionPane.showConfirmDialog(this.desktopApp.getJFrame(),
                msg, "Save Session?", JOptionPane.YES_NO_CANCEL_OPTION);
        if (reply == JOptionPane.CANCEL_OPTION)
            return false;
        else if (reply == JOptionPane.NO_OPTION)
        {
            CySession.Builder builder = new CySession.Builder();
            sessionManager.setCurrentSession(builder.build(), null);
            return false;
        }
        else
        {
            tm.execute(saveSession.createTaskIterator());
            if (sessionManager.getCurrentSession() == null)
                return true;
            CySession.Builder builder = new CySession.Builder();
            sessionManager.setCurrentSession(builder.build(), null);
        }
        return true;
    }
    
}
