package org.reactome.cytoscape3;

/**
 * This class provides some basic functions which
 * most analysis actions require, such as file 
 * validation and testing whether a new network should
 * be created. It is meant to be extended.
 * @author Eric T Dawson
 * @date July 2013
 */
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;

public abstract class FICytoscapeAction extends AbstractCyAction {
    
    public FICytoscapeAction(String title) {
        super(title);
    }

    protected boolean createNewSession() {
        //Checks if a session currently exists and if so whether the user would
        //like to save that session. A new session is then created.
        final BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        final ServiceReference desktopAppRef = context.getServiceReference(CySwingApplication.class.getName());
        final ServiceReference netManagerRef = context.getServiceReference(CyNetworkManager.class.getName());
        final ServiceReference taskManagerRef = context.getServiceReference(TaskManager.class.getName());
        final ServiceReference saveAsFactoryRef = context.getServiceReference(SaveSessionAsTaskFactory.class.getName());
        final ServiceReference sessionManagerRef = context.getServiceReference(CySessionManager.class.getName());
        // If any of above essential services is missing, we cannot do anything
        if (desktopAppRef == null ||
                netManagerRef == null ||
                taskManagerRef == null || 
                saveAsFactoryRef == null ||
                sessionManagerRef == null)
            return false;
        CySwingApplication desktopApp = (CySwingApplication) context.getService(desktopAppRef);
        CyNetworkManager networkManager = (CyNetworkManager) context.getService(netManagerRef);
        final CySessionManager sessionManager = (CySessionManager) context.getService(sessionManagerRef);
        
        int networkCount = networkManager.getNetworkSet().size();
        if (networkCount == 0) 
            return true;
        String msg = "A new session is needed for using Reactome FI plugin.\n"
                     + "Do you want to save your session?";
        int reply = JOptionPane.showConfirmDialog(desktopApp.getJFrame(),
                                                  msg, 
                                                  "Save Session?", 
                                                  JOptionPane.YES_NO_CANCEL_OPTION);
        if (reply == JOptionPane.CANCEL_OPTION) {
            ungetServices(context,
                          desktopAppRef,
                          netManagerRef,
                          saveAsFactoryRef,
                          sessionManagerRef,
                          taskManagerRef);
            return false;
        }
        else if (reply == JOptionPane.NO_OPTION) {
            CySession.Builder builder = new CySession.Builder();
            sessionManager.setCurrentSession(builder.build(), null);
            ungetServices(context,
                          desktopAppRef,
                          netManagerRef,
                          saveAsFactoryRef,
                          sessionManagerRef,
                          taskManagerRef);
            return true;
        }
        else {
            //TODO: There is a problem with the following code. If the user clicks "Cancel" in SessionSaveTask
            // FI plug-in method will be executed without stop. This is not good. It will add FI network into
            // the current session since the second newSessionTask will be bypassed. Furthermore, there is a thread
            // issue problem with plug-in method if tasks have not been finished completely.
            // A fix can be done with Cytoscape 3.1 API with TaskObserver. Will do this fix after 3.1 is formally 
            // released.
            final TaskManager tm = (TaskManager) context.getService(taskManagerRef);
            final SaveSessionAsTaskFactory saveAsFactory = (SaveSessionAsTaskFactory) context.getService(saveAsFactoryRef);
            Task newSessionTask = new AbstractTask() {
                @Override
                public void run(TaskMonitor taskMonitor) throws Exception {
                    if (sessionManager.getCurrentSession() == null) 
                        return;
                    CySession.Builder builder = new CySession.Builder();
                    sessionManager.setCurrentSession(builder.build(), null);
                    ungetServices(context,
                                  desktopAppRef,
                                  netManagerRef,
                                  saveAsFactoryRef,
                                  sessionManagerRef,
                                  taskManagerRef);
                }
            };
            TaskIterator tasks = saveAsFactory.createTaskIterator();
            tasks.append(newSessionTask);
            tm.execute(tasks);
//            if (sessionManager.getCurrentSession() == null) 
//                return true;
//            CySession.Builder builder = new CySession.Builder();
//            sessionManager.setCurrentSession(builder.build(), null);
//            ungetServices(context,
//                          desktopAppRef,
//                          netManagerRef,
//                          saveAsFactoryRef,
//                          sessionManagerRef,
//                          taskManagerRef);
            return true;
        }
    }
    
    /**
     * A helper to unget an array of services.
     * @param references
     */
    private void ungetServices(BundleContext context,
                               ServiceReference... references) {
        for (ServiceReference reference : references)
            context.ungetService(reference);
    }
}
