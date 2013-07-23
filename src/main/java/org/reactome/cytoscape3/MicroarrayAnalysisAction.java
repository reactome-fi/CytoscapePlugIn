package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.osgi.framework.ServiceReference;

public class MicroarrayAnalysisAction extends FICytoscapeAction
{

    private CySwingApplication desktopApp;

    public MicroarrayAnalysisAction()
    {
        super("Microarray Analysis");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(3);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map<ServiceReference, Object> servRefToService = null;
        try
        {
            List<String> clazzes = new ArrayList<String>();
            clazzes.add(CySwingApplication.class.getName());
            //clazzes.add()
            servRefToService = PlugInScopeObjectManager.getManager().getServiceReferenceObjectList(clazzes);
            for (Object service : servRefToService.entrySet())
            {
                if (service instanceof CySwingApplication)
                    this.desktopApp = (CySwingApplication) service;
            }
        }
        catch (Throwable t)
        {
            JOptionPane.showMessageDialog(null, "An error occurred in retrieving services;\nPlease try again.", "OSGi Service Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!createNewSession())
        {
            return;
        }
        ActionDialogs gui = new ActionDialogs("Microarray");
        gui.setLocationRelativeTo(desktopApp.getJFrame());
        gui.setModal(true);
        gui.setVisible(true);
        PlugInScopeObjectManager.getManager().releaseAllServices(servRefToService);
    }

}
