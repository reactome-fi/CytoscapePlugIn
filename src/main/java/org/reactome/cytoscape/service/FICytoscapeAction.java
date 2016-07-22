package org.reactome.cytoscape.service;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * This class provides some basic functions which
 * most analysis actions require, such as file 
 * validation and testing whether a new network should
 * be created. It is meant to be extended.
 * @author Eric T Dawson
 * @date July 2013
 */
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.create.NewSessionTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * It is very difficult to remove an EventListener that is used to listener to new Session creation when
 * the user cancels the task. Here a SynchronizationTaskManager is used.
 * @author gwu
 *
 */
public abstract class FICytoscapeAction extends AbstractCyAction {
    
    public FICytoscapeAction(String title) {
        super(title);
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(0.5f);
    }
    
    /**
     * An empty Session will be treated as a new Session, which can be used for working.
     * @param context
     * @return
     */
    private boolean isNewSession(BundleContext context) {
        ServiceReference networkManagerRef = context.getServiceReference(CyNetworkManager.class.getName());
        CyNetworkManager networkManager = (CyNetworkManager) context.getService(networkManagerRef);
        ungetServices(context, networkManagerRef);
        // If both are true, we don't need a new session.
        if ((networkManager.getNetworkSet().size() == 0 && !PlugInObjectManager.getManager().isPathwaysLoaded())) {
            return true;
        }
        return false;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Checks if a session currently exists.
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        if (isNewSession(context)) {
            doAction();
        }
        else {
            if (createNewSession())
                doAction();
        }
    }
    
    protected abstract void doAction();
    
    protected boolean createNewSession() {
        String msg = "A new session is needed for using ReactomeFIViz.\n"
                + "Do you want to save your session?";
        if (PlugInObjectManager.getManager().isPathwaysLoaded())
            msg += "\nNote: Loaded pathways cannot be saved.";
        int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                  msg, 
                                                  "Save Session?", 
                                                  JOptionPane.YES_NO_CANCEL_OPTION);
        if (reply == JOptionPane.CANCEL_OPTION) {
            return false;
        }
        else {
            //Checks if a session currently exists.
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            // Get the current session
            ServiceReference managerSf = context.getServiceReference(CySessionManager.class.getName());
            CySessionManager manager = (CySessionManager) context.getService(managerSf);
            CySession oldSession = manager.getCurrentSession();
            
            TaskIterator tasks = null;
            if (reply == JOptionPane.YES_OPTION) {
                File file = getSessionFile(context);
                if (file == null)
                    return false; // Cancelled by the user
                // Save session first
                ServiceReference saveSessionSf = context.getServiceReference(SaveSessionAsTaskFactory.class.getName());
                SaveSessionAsTaskFactory sstf = (SaveSessionAsTaskFactory) context.getService(saveSessionSf);
                tasks = sstf.createTaskIterator(); // These tasks for saving
                ungetServices(context, saveSessionSf);
            }
            
            // Create NewSessionTask
            ServiceReference newSessionSf = context.getServiceReference(NewSessionTaskFactory.class.getName());
            NewSessionTaskFactory ntf = (NewSessionTaskFactory) context.getService(newSessionSf);
            // Use true so that new warning dialog will appear.
            // Saving session will be handled in other place since we cannot 
            // catch the session cancelled event, which results in a weird behavior.
            if (tasks != null)
                tasks.append(ntf.createTaskIterator(true));
            else
                tasks = ntf.createTaskIterator(true);
            ServiceReference tmSf = context.getServiceReference(SynchronousTaskManager.class.getName());
            SynchronousTaskManager<?> tm = (SynchronousTaskManager) context.getService(tmSf);
            tm.execute(tasks);
            ungetServices(context, newSessionSf, tmSf, managerSf);
            CySession newSession = manager.getCurrentSession();
            return newSession != oldSession;
        }
    }

    /**
     * The code here is copied from class org.cytoscape.internal.SessionHandler.
     * @param context
     * @return
     */
    private File getSessionFile(BundleContext context) {
        ServiceReference sf = context.getServiceReference(CySessionManager.class.getName());
        CySessionManager sessionMgr = (CySessionManager) context.getService(sf);
        String sessionFileName = sessionMgr.getCurrentSessionFileName();
        ungetServices(context, sf);
        File file;
        if (sessionFileName == null || sessionFileName.isEmpty()) {
            FileChooserFilter filter = new FileChooserFilter("Session File", "cys");
            List<FileChooserFilter> filterCollection = new ArrayList<FileChooserFilter>(1);
            filterCollection.add(filter);
            sf = context.getServiceReference(FileUtil.class.getName());
            FileUtil fileUtil = (FileUtil) context.getService(sf);
            file = fileUtil.getFile(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                    "Save Session File",
                                    FileUtil.SAVE,
                                    filterCollection );
            ungetServices(context, sf);
        } 
        else {
            file = new File(sessionFileName);
        }
        return file;
    }
    
    /**
     * A helper to unget an array of services.
     * @param references
     */
    private static void ungetServices(BundleContext context,
                               ServiceReference... references) {
        for (ServiceReference reference : references)
            context.ungetService(reference);
    }
    
}
