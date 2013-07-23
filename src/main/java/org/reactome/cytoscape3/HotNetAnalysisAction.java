package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;

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
        setMenuGravity(3.0f);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        ActionDialogs gui = new ActionDialogs("Hotnet");
        gui.setLocationRelativeTo(desktopApp.getJFrame());
        gui.setModal(true);
        gui.setVisible(true);
        
        //Initialize a new HotNet task factory.
        
        //Activate the task via the TaskFactory's createIterator method.
        
    }

}
