package org.reactome.CS.x.internal;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;
import org.gk.util.DialogControlPane;






public class GSMAAction extends AbstractCyAction
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
	super("Gene Set / Mutant Analysis");
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
	
    }
    protected void getParams(){
	Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
	String [] mafExts = new String [3];
	mafExts[0] = "txt"; mafExts[1] = "protected.maf"; mafExts[2] = "maf";
	FileChooserFilter mafFilter = new FileChooserFilter("NCI MAF Files", mafExts);
	filters.add(mafFilter);
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
