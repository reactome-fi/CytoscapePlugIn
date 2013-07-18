package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;

public class MicroarrayAnalysisAction extends FICytoscapeAction
{

    public MicroarrayAnalysisAction(CySwingApplication desktopApp,
            CyNetworkManager cyNetManager, FileUtil fileUtil,
            SaveSessionAsTaskFactory saveAsFactory, TaskManager tm,
            CySessionManager sessionManager, String title)
    {
        super(desktopApp, cyNetManager, fileUtil, saveAsFactory, tm, sessionManager,
                title);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // TODO Auto-generated method stub
        
    }

}
