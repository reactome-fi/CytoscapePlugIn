package org.reactome.CS.x.internal;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JOptionPane;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;







public class GSMAAction extends FICytoscapeAction
{

    private CySwingApplication desktopApp;
    private TaskManager tm;
    private CyNetworkManager netManager;
    private SaveSessionAsTaskFactory saveSession;
    private FileUtil fileUtil;
    private CySessionManager sessionManager;
    public GSMAAction(TaskManager tm, CyNetworkManager netManager,

	    SaveSessionAsTaskFactory saveSession,
	    FileUtil fileUtil,
	    CySwingApplication desktopApp,
	    CySessionManager sessionManager)
    {
	super(desktopApp, netManager, fileUtil, saveSession, tm, sessionManager, "Gene Set / Mutant Analysis");
	this.desktopApp = desktopApp;
	this.tm = tm;
	this.netManager = netManager;
	this.saveSession = saveSession;
	this.fileUtil = fileUtil;
	this.sessionManager = sessionManager;
	setPreferredMenu("Apps.ReactomeFI");
	
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
	if (createNewSession(netManager, sessionManager)){
	    System.out.println("Session save/no previous session");
	}
	GUIBuilder gui = new GUIBuilder("GSMA", desktopApp, fileUtil);
	gui.setLocationRelativeTo(desktopApp.getJFrame());
	gui.setModal(true);
	gui.setVisible(true);
	if (!gui.isOkClicked())
	    return;
	final File file = gui.getSelectedFile();
	if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(desktopApp.getJFrame(), 
                                          "No file is chosen or the selected file doesn't exit!", 
                                          "Error in File", 
                                          JOptionPane.ERROR_MESSAGE);
            return;
	}
    }
    
   
    
    
    protected boolean createNewSession(CyNetworkManager networkManager, CySessionManager sessionManager)
    {
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
