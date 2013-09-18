package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;

@SuppressWarnings("serial")
public class HotNetAnalysisAction extends FICytoscapeAction
{

    private CySwingApplication desktopApp;

    public HotNetAnalysisAction(CySwingApplication desktopApp)
    {
        super("HotNet Mutation Analysis");
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
        HotNetAnalysisDialog gui = new HotNetAnalysisDialog();
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
        //Initialize a new HotNet task factory.
        //HotNetAnalysisTaskFactory hnTaskFactory = new HotNetAnalysisTaskFactory(gui);
        //Activate the task via the TaskFactory's createIterator method.
//        Map<ServiceReference,  Object> tmRefToTM =  PlugInScopeObjectManager.getManager().getServiceReferenceObject(TaskManager.class.getName());
//        ServiceReference servRef = (ServiceReference) tmRefToTM.keySet().toArray()[0];
//        TaskManager tm = (TaskManager) tmRefToTM.get(servRef);
//        tm.execute(hnTaskFactory.createTaskIterator());
        
 //       PlugInScopeObjectManager.getManager().releaseSingleService(tmRefToTM);
        
        Thread t = new Thread(new HotNetAnalysisTask(gui));
        t.start();
    }

}
