package org.reactome.CS.x.internal;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Set.*;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.*;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.*;
import org.cytoscape.work.TaskManager;
import org.cytoscape.task.write.*;




public abstract class FICytoscapeAction extends AbstractCyAction
{
    protected CyNetworkManager cyNetManager;
    protected CySwingApplication desktopApp;
    protected CyFileFilter fileFilter;
    protected FileUtil fileUtil;
    protected TaskManager tm;
    protected SaveSessionAsTaskFactory saveAsFactory;
    protected CySessionManager sessionManager;
    public FICytoscapeAction(CySwingApplication desktopApp,
	    CyNetworkManager cyNetManager,
	    FileUtil fileUtil,
	    SaveSessionAsTaskFactory saveAsFactory,
	    TaskManager tm,
	    CySessionManager sessionManager,
	    String title)
    {
	super(title);
	this.desktopApp = desktopApp;
	this.cyNetManager = cyNetManager;
	this.fileUtil = fileUtil;
	this.saveAsFactory = saveAsFactory;
	this.tm = tm;
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
	}
	else
	{
	    tm.execute(saveAsFactory.createTaskIterator());
	    if (sessionManager.getCurrentSession() == null)
		return true;
	    CySession.Builder builder = new CySession.Builder();
	    sessionManager.setCurrentSession(builder.build(), null);
	}
	return true;
	    
    }
    protected boolean validateFile(JTextField fileTF,
	    java.awt.Component parentComp)
    {
	if (fileTF.getText().trim().length() == 0)
	{
	    JOptionPane.showMessageDialog(parentComp, "Please enter a file name in the file field",
		    "Empty file name", JOptionPane.ERROR_MESSAGE);
	    return false;
	}
	String text = fileTF.getText().trim();
	File file = new File(text);
	if (!file.exists())
	{
	    JOptionPane.showMessageDialog(parentComp,
		    "The file you entered does no exist. Please enter a valid file name",
		    "Incorrect File Name",
		    JOptionPane.ERROR_MESSAGE);
	    return false;
	}
	return true;
    }
    protected void createFileChooseGUIs(final JTextField fileField,
            final JButton okBtn,
            final String fileTitle,
            final Collection<FileChooserFilter> fileFilters,
            JPanel filePanel,
            GridBagConstraints constraints) {
    JLabel fileLabel = new JLabel("Choose data file:");
    filePanel.add(fileLabel, constraints);
    fileField.getDocument().addDocumentListener(new DocumentListener()
    {

	@Override
	public void removeUpdate(DocumentEvent e)
	{
	    if (fileField.getText().trim().length() > 0)
			okBtn.setEnabled(true);
	    else
		okBtn.setEnabled(false);
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
	    if (fileField.getText().trim().length() > 0)
		okBtn.setEnabled(true);
	    else
		okBtn.setEnabled(false);
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
	}
    });
    fileField.setColumns(20);
    //  fileField.setText("This should be a full path name for a file!");
    constraints.gridx = 1;
    constraints.weightx = 0.80;
    filePanel.add(fileField, constraints);
    constraints.weightx = 0.1;
    JButton browseBtn = new JButton("Browse");
    browseBtn.addActionListener(new ActionListener()
    {
      
      @Override
      public void actionPerformed(ActionEvent e) 
      {
          browseDataFile(fileField, fileTitle, fileFilters);
      }
  });
  constraints.gridx = 2;
  filePanel.add(browseBtn);
  // Disable okBtn as default
  okBtn.setEnabled(false);
   }
    private void browseDataFile(JTextField tf,
            String title,
            Collection<FileChooserFilter> fileFilters)
    {
	//FileChooserFilter fileChooserFilter = new FileChooserFilter("Mutation Annotation Format", "maf");
	
	
	// Provide a way to load a MAF file
	File[] files = fileUtil.getFiles((Component) this.desktopApp.getJFrame(),
                     title,
                     fileUtil.LOAD,
                     (Collection) fileFilters);
	if (files == null || files.length == 0)
	    return;
	File file = files[0];
	tf.setText(file.getAbsolutePath());
    }
    
}
