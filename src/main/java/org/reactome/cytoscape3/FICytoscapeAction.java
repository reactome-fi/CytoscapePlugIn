package org.reactome.cytoscape3;

/**
 * This class provides some basic functions which
 * most analysis actions require, such as file 
 * validation and testing whether a new network should
 * be created. It is meant to be extended.
 * @author Eric T Dawson
 * @date July 2013
 */
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.task.write.SaveSessionTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class FICytoscapeAction extends AbstractCyAction
{
    public FICytoscapeAction(String title)
    {
        super(title);

    }

    protected boolean createNewSession()
    {
        //Checks if a session currently exists and if so whether the user would
        //like to save that session. A new session is then created.
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        ServiceReference desktopAppRef = context.getServiceReference(CySwingApplication.class.getName());
        ServiceReference netManagerRef = context.getServiceReference(CyNetworkManager.class.getName());
        ServiceReference taskManagerRef = context.getServiceReference(TaskManager.class.getName());
        ServiceReference saveAsFactoryRef = context.getServiceReference(SaveSessionAsTaskFactory.class.getName());
        ServiceReference sessionManagerRef = context.getServiceReference(CySessionManager.class.getName());
        if (desktopAppRef != null && netManagerRef != null && taskManagerRef != null && saveAsFactoryRef != null && sessionManagerRef != null)
        {
            CySwingApplication desktopApp = (CySwingApplication) context.getService(desktopAppRef);
            CyNetworkManager networkManager = (CyNetworkManager) context.getService(netManagerRef);
            TaskManager tm = (TaskManager) context.getService(taskManagerRef);
            SaveSessionAsTaskFactory saveAsFactory = (SaveSessionAsTaskFactory) context.getService(saveAsFactoryRef);
            CySessionManager sessionManager = (CySessionManager) context.getService(sessionManagerRef);
            
            int networkCount = networkManager.getNetworkSet().size();
            if (networkCount == 0) return true;
            String msg = new String(
                    "A new session is needed for using Reactome FI plugin.\n"
                            + "Do you want to save your session?");
            int reply = JOptionPane.showConfirmDialog(desktopApp.getJFrame(),
                    msg, "Save Session?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)
            {
                context.ungetService(desktopAppRef);
                context.ungetService(netManagerRef);
                context.ungetService(saveAsFactoryRef);
                context.ungetService(sessionManagerRef);
                context.ungetService(taskManagerRef);
                return false;
            }
            else if (reply == JOptionPane.NO_OPTION)
            {
                CySession.Builder builder = new CySession.Builder();
                sessionManager.setCurrentSession(builder.build(), null);
                context.ungetService(desktopAppRef);
                context.ungetService(netManagerRef);
                context.ungetService(saveAsFactoryRef);
                context.ungetService(sessionManagerRef);
                context.ungetService(taskManagerRef);
                return true;
            }
            else
            {
                tm.execute(saveAsFactory.createTaskIterator());
                if (sessionManager.getCurrentSession() == null) return true;
                CySession.Builder builder = new CySession.Builder();
                sessionManager.setCurrentSession(builder.build(), null);
            }
            context.ungetService(desktopAppRef);
            context.ungetService(netManagerRef);
            context.ungetService(saveAsFactoryRef);
            context.ungetService(sessionManagerRef);
            context.ungetService(taskManagerRef);
            return true;
        }
        return false;
    }

    protected boolean validateFile(JTextField fileTF,
            java.awt.Component parentComp)
    {
        if (fileTF.getText().trim().length() == 0)
        {
            JOptionPane.showMessageDialog(parentComp,
                    "Please enter a file name in the file field",
                    "Empty file name", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String text = fileTF.getText().trim();
        File file = new File(text);
        if (!file.exists())
        {
            JOptionPane
                    .showMessageDialog(
                            parentComp,
                            "The file you entered does no exist. Please enter a valid file name",
                            "Incorrect File Name", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    protected void createFileChooseGUIs(final JTextField fileField,
            final JButton okBtn, final String fileTitle,
            final Collection<FileChooserFilter> fileFilters, JPanel filePanel,
            GridBagConstraints constraints)
    {
        JLabel fileLabel = new JLabel("Choose data file:");
        filePanel.add(fileLabel, constraints);
        fileField.getDocument().addDocumentListener(new DocumentListener()
        {

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                if (fileField.getText().trim().length() > 0)
                {
                    okBtn.setEnabled(true);
                }
                else
                {
                    okBtn.setEnabled(false);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                if (fileField.getText().trim().length() > 0)
                {
                    okBtn.setEnabled(true);
                }
                else
                {
                    okBtn.setEnabled(false);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
        });
        fileField.setColumns(20);
        // fileField.setText("This should be a full path name for a file!");
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

    private void browseDataFile(JTextField tf, String title,
            Collection<FileChooserFilter> fileFilters)
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        ServiceReference desktopAppRef = context.getServiceReference(CySwingApplication.class.getName());
        ServiceReference fileUtilRef = context.getServiceReference(FileUtil.class.getName());
        if (desktopAppRef != null && fileUtilRef != null)
        {
            CySwingApplication desktopApp = (CySwingApplication) context.getService(desktopAppRef);
            FileUtil fileUtil = (FileUtil) context.getService(fileUtilRef);

        // Provide a way to load a MAF file
        File[] files = fileUtil.getFiles(desktopApp.getJFrame(), title,
                FileUtil.LOAD, fileFilters);
        if (files == null || files.length == 0) return;
        File file = files[0];
        tf.setText(file.getAbsolutePath());
        
        context.ungetService(fileUtilRef);
        context.ungetService(desktopAppRef);
        }
    }

}
