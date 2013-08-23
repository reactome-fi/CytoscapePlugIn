package org.reactome.cytoscape3;

import java.awt.Component;
import java.awt.Container;
import java.util.Set;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.view.model.events.NetworkViewDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewDestroyedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class FISessionCleanup implements NetworkViewDestroyedListener, SessionAboutToBeSavedListener
{
    @Override
    public void handleEvent(SessionAboutToBeSavedEvent e)
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        ServiceReference visMapManRef = context.getServiceReference(VisualMappingManager.class.getName());
        VisualMappingManager visMapMan = (VisualMappingManager) context.getService(visMapManRef); 
      //Remove FI Visual Style (fix for inability to save
        //continuous mapping of integer values.
        if(visMapMan.getCurrentVisualStyle().getTitle().equals("FI Network"));
        {
            visMapMan.removeVisualStyle(visMapMan.getCurrentVisualStyle());
            visMapMan.setCurrentVisualStyle(visMapMan.getDefaultVisualStyle());
        }
    }
    @Override
    public void handleEvent(NetworkViewDestroyedEvent e)
    {
        // TODO Auto-generated method stub
        // TODO Auto-generated method stub
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(CyNetworkManager.class.getName());
        
        if (servRef != null)
        {
            CyNetworkManager manager = (CyNetworkManager) context.getService(servRef);
            
            Set<CyNetwork> networkSet = manager.getNetworkSet();
            for (CyNetwork network : networkSet)
            {
                boolean isReactomeFINetwork = network.getDefaultNetworkTable().getRow(network.getSUID())
                        .get("isReactomeFINetwork", Boolean.class);
                if (isReactomeFINetwork)
                {
                    // TODO Auto-generated method stub

                    // Destroy module browsers in South Cytopanel,
                    //the NCI Diseases panel in the West Cytopane,
                    //and the Survival Analysis pane in the East Cytopanel.
                    removeAllResultsPanels();
                    
                    // Garbage collect CyTables/JTables

                    // Clean up stored fields and instances.

                    // Dump cytoscape services

                    
                }
            }
        }
    }


    public void removeAllResultsPanels()
    {
        String [] titles = {"MCL Module Browser", "HotNet Module Browser", "Network Module Browser",
                "Pathways in Modules", "Pathways in Network", "GO CC in Network", "GO BP in Network",
                "GO MF in Network", "GO CC in Modules", "GO CC in Modules", "GO BP in Modules"};
        CySwingApplication desktopApp = PlugInScopeObjectManager.getManager()
                .getCySwingApp();
        CytoPanel tableBrowserPane = desktopApp
                .getCytoPanel(CytoPanelName.SOUTH);
        int numComps = tableBrowserPane.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            CytoPanelComponent aComp = (CytoPanelComponent) tableBrowserPane
                    .getComponentAt(i);
            for (String title : titles)
            {
                if (aComp.getTitle().equalsIgnoreCase(title))
                {
                    ((Container) tableBrowserPane).remove((Component) aComp);
                }
            }
        }
        CytoPanel resultsPane = desktopApp.getCytoPanel(CytoPanelName.EAST);
        numComps = resultsPane.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            Component aComp = (Component) resultsPane.getComponentAt(i);
            if ((aComp instanceof CytoPanelComponent) && ((CytoPanelComponent) aComp).getTitle().equalsIgnoreCase("Survival Analysis"))
                ((Container) resultsPane).remove(aComp);
        }
        resultsPane = desktopApp.getCytoPanel(CytoPanelName.WEST);
        numComps = resultsPane.getCytoPanelComponentCount();
        for (int i = 0; i < (numComps + 3); i++)
        {
            Component aComp = (Component) resultsPane.getComponentAt(i);
            if ((aComp instanceof CytoPanelComponent) && ((CytoPanelComponent) aComp).getTitle().equals("NCI Diseases"))
                ((Container) resultsPane).remove(aComp);
        }
    }
    
}

